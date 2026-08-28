"""
Query Plan Comparator v6
=========================
Same comparison algorithm as v5 (tree edit distance / structural score,
multiset-Jaccard operator score, overall = structural * operator), but the
parser has been completely replaced.

v5 was built for PostgreSQL `EXPLAIN (FORMAT JSON)` output ("Plan" /
"Node Type" / "Plans" keys). This dataset instead stores PLAIN-TEXT
`EXPLAIN` output as a list of strings under the "execution_plan" key, e.g.:

    {
        "query_number": 7,
        "status": "...",
        "timestamp": "...",
        "original_query": "...",
        "executed_query": "...",
        "execution_plan": [
            "Finalize Aggregate  (cost=1000.55..1000.56 rows=1 width=8)",
            "  ->  Gather  (cost=1000.00..1000.51 rows=2 width=8)",
            "        Workers Planned: 2",
            "        ->  Partial Aggregate  (cost=0.00..0.01 rows=1 width=8)",
            "              ->  Parallel Seq Scan on big_table  (cost=0.00..1000.00 rows=1 width=0)"
        ]
    }

v6 parses this text format into the exact same PNode tree shape v5 used,
with the exact same canonicalization / stripping semantics (Gather,
Gather Merge, Hash, and Parallel Hash are transparent pass-through nodes,
matching how v5 treated them in the JSON parser), so every downstream
scoring function is untouched.

Usage:
    python plan_comparator_v6.py
        (reads ./only_explain_original and ./only_explain_synthetic)

    python plan_comparator_v6.py <original_folder> <synthetic_folder>
"""

import csv
import os
import re
import sys
import json
from collections import Counter
from dataclasses import dataclass, field
from typing import Optional, List


# ── Node canonicalization ─────────────────────────────────────────
# Identical to v5. `None` marks a "transparent" node whose children are
# spliced into its parent instead of being kept as a node themselves.

_NODE_MAP = {
    "Seq Scan":          "SEQ_SCAN",
    "Bitmap Heap Scan":  "BITMAP_SCAN",
    "Bitmap Index Scan": "BITMAP_SCAN",
    "Index Scan":        "INDEX_SCAN",
    "Index Only Scan":   "INDEX_SCAN",
    "Tid Scan":          "TID_SCAN",
    "Function Scan":     "FUNC_SCAN",
    "Values Scan":       "VALUES_SCAN",
    "CTE Scan":          "CTE_SCAN",
    "Hash Join":         "HASH_JOIN",
    "Merge Join":        "MERGE_JOIN",
    "Nested Loop":       "NESTED_LOOP",
    "Aggregate":         "AGGREGATE",
    "Sort":              "SORT",
    "Incremental Sort":  "SORT",
    "Append":            "APPEND",
    "Merge Append":      "MERGE_APPEND",
    "Subquery Scan":     "SUBQUERY",
    "Result":            "RESULT",
    "Unique":            "UNIQUE",
    "SetOp":             "SETOP",
    "Limit":             "LIMIT",
    "LockRows":          "LOCK",
    "Materialize":       "MATERIALIZE",
    "Memoize":           "MEMOIZE",
    "WindowAgg":         "WINDOW_AGG",
    "Group":             "GROUP",
    "ProjectSet":        "PROJECT_SET",
    # stripped — pass children through transparently
    "Gather":            None,
    "Gather Merge":      None,
    "Hash":              None,
}


def _canonical(raw: str) -> Optional[str]:
    """
    Map a raw plan-node label to its canonical operator name.
    Falls back to an UPPER_SNAKE_CASE version of the raw text so that an
    unrecognized node type never resolves to None by accident (None is
    reserved for the deliberately-stripped node types above).
    """
    if raw in _NODE_MAP:
        return _NODE_MAP[raw]
    cleaned = raw.strip()
    if not cleaned:
        return "OTHER"
    return re.sub(r"[^A-Z0-9]+", "_", cleaned.upper()).strip("_") or "OTHER"


# ── Family mapping ────────────────────────────────────────────────
# Used for TED relabel cost and Jaccard. Within a family, substitution
# costs 0. Across families, costs 1. Unchanged from v5.

_FAMILY = {
    "HASH_JOIN":    "JOIN",
    "MERGE_JOIN":   "JOIN",
    "NESTED_LOOP":  "JOIN",
    "INDEX_SCAN":   "SCAN",
    "SEQ_SCAN":     "SCAN",
    "BITMAP_SCAN":  "SCAN",
    "TID_SCAN":     "SCAN",
    "FUNC_SCAN":    "SCAN",
    "CTE_SCAN":     "SCAN",
    "VALUES_SCAN":  "SCAN",
    "AGGREGATE":    "AGG",
    "GROUP":        "AGG",
    "WINDOW_AGG":   "AGG",
    "SORT":         "SORT",
    "MERGE_APPEND": "SORT",
    "LIMIT":        "FILTER",
    "UNIQUE":       "FILTER",
    "SETOP":        "FILTER",
    "RESULT":       "FILTER",
    "APPEND":       "OTHER",
    "SUBQUERY":     "OTHER",
    "MATERIALIZE":  "OTHER",
    "MEMOIZE":      "OTHER",
    "LOCK":         "OTHER",
    "PROJECT_SET":  "OTHER",
}


def _family(op: str) -> str:
    return _FAMILY.get(op, "OTHER")


# ── Plan node ─────────────────────────────────────────────────────

@dataclass
class PNode:
    op: str
    children: list = field(default_factory=list)


# ── Raw text-line node (pre-canonicalization) ─────────────────────

@dataclass
class _RawNode:
    text: str
    indent: int
    children: List["_RawNode"] = field(default_factory=list)


# Matches the "-> " (or "->  ") arrow PostgreSQL prefixes child nodes with.
_ARROW_RE = re.compile(r"^\s*->\s*")

# A line is considered a plan-node line if it contains a top-level
# "(cost=..." annotation. Lines like "Filter: ...", "Sort Key: ...",
# "Workers Planned: 2", "Buffers: ..." etc. never carry this and are
# correctly treated as node detail, not new nodes.
_COST_RE = re.compile(r"\(cost=")


def _split_plan_lines(execution_plan) -> List[str]:
    """
    execution_plan may already be a list of strings (the normal case), or
    occasionally a single newline-joined string. Normalize to a list.
    """
    if isinstance(execution_plan, str):
        return execution_plan.splitlines()
    return list(execution_plan or [])


def _build_raw_tree(plan_lines: List[str]) -> Optional[_RawNode]:
    """
    Turn PostgreSQL plain-text EXPLAIN lines into a raw indentation tree.
    Indentation depth is taken from the column of the first non-space
    character on each node line (whether or not it starts with "->"),
    which is how Postgres consistently nests child plan nodes.
    """
    root: Optional[_RawNode] = None
    stack: List[_RawNode] = []  # ordered shallow -> deep

    for raw_line in plan_lines:
        line = raw_line.rstrip("\n").rstrip()
        if not line.strip():
            continue
        if not _COST_RE.search(line):
            # Detail line (Filter:, Sort Key:, Buffers:, Workers Planned:,
            # Planning Time:, etc.) — not a plan node, skip.
            continue

        indent = len(line) - len(line.lstrip(" "))

        text = line.strip()
        text = _ARROW_RE.sub("", text)          # drop leading "->"
        text = text.split(" (cost=", 1)[0].strip()  # drop cost/rows/width
        text = text.split("(cost=", 1)[0].strip()   # safety net, no space case

        if " on " in text:
            text = text.split(" on ", 1)[0].strip()
        if " using " in text:
            text = text.split(" using ", 1)[0].strip()
        text = re.sub(r"^Parallel\s+", "", text).strip()

        if not text:
            continue

        node = _RawNode(text=text, indent=indent)

        # Pop back to the correct parent: anything at >= this indent is a
        # sibling/cousin, not an ancestor of this node.
        while stack and stack[-1].indent >= indent:
            stack.pop()

        if stack:
            stack[-1].children.append(node)
        else:
            if root is None:
                root = node
            else:
                # A second top-level (indent-0) line — extremely rare, but
                # keep the tree well-formed by nesting it under a synthetic
                # root rather than silently dropping it.
                synthetic = _RawNode(text="RESULT", indent=-1, children=[root, node])
                root = synthetic
                stack = [synthetic]
                continue

        stack.append(node)

    return root


def _convert_raw(raw: _RawNode) -> Optional[PNode]:
    """
    Canonicalize a raw node and splice through transparent nodes (Gather,
    Gather Merge, Hash / Parallel Hash), mirroring exactly what v5's
    `_parse()` did for the JSON "Plans" tree: a transparent node with
    exactly one converted child is replaced by that child; a transparent
    node with zero or multiple children is dropped.
    """
    op = _canonical(raw.text)

    children: List[PNode] = []
    for child in raw.children:
        converted = _convert_raw(child)
        if converted is not None:
            children.append(converted)

    if op is None:  # transparent / stripped node
        return children[0] if len(children) == 1 else None

    return PNode(op=op, children=children)


def parse_text_plan(execution_plan) -> PNode:
    """
    Converts PostgreSQL plain-text EXPLAIN output (list[str]) into a PNode
    tree. Always returns a valid PNode — never None, never an op of None —
    even for degenerate/empty input, so downstream scoring never hits a
    NoneType operator.
    """
    lines = _split_plan_lines(execution_plan)
    raw_root = _build_raw_tree(lines)

    if raw_root is None:
        return PNode(op="OTHER", children=[])

    converted = _convert_raw(raw_root)
    if converted is None:
        # Root itself was a transparent node that stripped away to nothing
        # (e.g. a bare "Gather" with no usable children) — fall back to a
        # placeholder rather than propagate None.
        return PNode(op="OTHER", children=[])

    return converted


# ── JSON EXPLAIN parser (kept for compatibility with FORMAT JSON input) ──

def _parse_json_node(node: dict) -> Optional[PNode]:
    raw = node.get("Node Type", "UNKNOWN")
    op = _canonical(raw)

    children = []
    for child in node.get("Plans", []):
        p = _parse_json_node(child)
        if p is not None:
            children.append(p)

    if op is None:  # stripped node — pass children through
        return children[0] if len(children) == 1 else None

    return PNode(op=op, children=children)


def parse_plan(plan_obj) -> PNode:
    """
    Dispatch on input shape:
      - dict with "execution_plan": text EXPLAIN (this dataset's format)
      - dict with "Plan" key, or list containing such a dict: FORMAT JSON
    """
    if isinstance(plan_obj, dict) and "execution_plan" in plan_obj:
        return parse_text_plan(plan_obj["execution_plan"])

    if isinstance(plan_obj, list):
        plan_obj = plan_obj[0] if plan_obj else {}

    if isinstance(plan_obj, dict) and "Plan" in plan_obj:
        result = _parse_json_node(plan_obj["Plan"])
        if result is None:
            return PNode(op="OTHER", children=[])
        return result

    # Last resort: try treating the dict itself as a "Plan" node.
    if isinstance(plan_obj, dict):
        result = _parse_json_node(plan_obj)
        if result is not None:
            return result

    return PNode(op="OTHER", children=[])


# ── Tree size ─────────────────────────────────────────────────────

def _size(t: Optional[PNode]) -> int:
    if t is None:
        return 0
    return 1 + sum(_size(c) for c in t.children)


# ── Canonical child ordering ──────────────────────────────────────
# Children are treated as a set, not a sequence — child order in the
# plan text is an optimizer artifact, not an algorithmic difference.
# We sort children into a deterministic order before TED so that
# swapped siblings (e.g. SORT/SCAN vs SCAN/SORT under a MERGE_JOIN)
# cost zero edits instead of two.

def _canonical_str(t: PNode) -> str:
    """Deterministic string representation of a subtree (used for sorting only)."""
    if not t.children:
        return t.op
    child_strs = sorted(_canonical_str(c) for c in t.children)
    return f"{t.op}({','.join(child_strs)})"


def _canonicalize(t: PNode) -> PNode:
    """Return a copy of t with children recursively sorted."""
    canonical_children = [_canonicalize(c) for c in t.children]
    canonical_children.sort(key=_canonical_str)
    return PNode(op=t.op, children=canonical_children)


# ── Tree Edit Distance (Zhang-Shasha) ────────────────────────────
# Proper Zhang-Shasha algorithm — finds the true minimum edit script.
# Relabel cost: 0 if same family, 1 if different family.
# Insert / delete cost: 1 per node.
# Always called on canonicalized trees (children sorted).

def _index_tree(node: PNode):
    """
    Post-order index a tree.
    Returns:
        nodes : list of PNode in post-order, 0-indexed (node at position i is nodes[i])
        l     : list where l[i] is the 1-based post-order index of the
                leftmost leaf descendant of the node at position i+1
    """
    nodes: list = []
    l: list = []

    def walk(n: PNode) -> int:
        first_leaf_l = None
        for idx, child in enumerate(n.children):
            child_pos = walk(child)
            if idx == 0:
                first_leaf_l = l[child_pos - 1]

        nodes.append(n)
        my_pos = len(nodes)
        l.append(my_pos if first_leaf_l is None else first_leaf_l)
        return my_pos

    walk(node)
    return nodes, l


def _keyroots(l: list, n: int) -> list:
    seen: dict = {}
    for i in range(1, n + 1):
        seen[l[i - 1]] = i
    return sorted(seen.values())


def _ted(t1: PNode, t2: PNode) -> float:
    """Zhang-Shasha tree edit distance on pre-canonicalized trees."""
    nodes1, l1 = _index_tree(t1)
    nodes2, l2 = _index_tree(t2)
    n1, n2 = len(nodes1), len(nodes2)

    kr1 = _keyroots(l1, n1)
    kr2 = _keyroots(l2, n2)

    def relabel(op1: str, op2: str) -> float:
        return 0.0 if _family(op1) == _family(op2) else 1.0

    td = [[0.0] * (n2 + 1) for _ in range(n1 + 1)]
    fd = [[0.0] * (n2 + 1) for _ in range(n1 + 1)]

    for i in kr1:
        li = l1[i - 1]
        for j in kr2:
            lj = l2[j - 1]

            fd[li - 1][lj - 1] = 0.0

            for i2 in range(li, i + 1):
                fd[i2][lj - 1] = fd[i2 - 1][lj - 1] + 1.0

            for j2 in range(lj, j + 1):
                fd[li - 1][j2] = fd[li - 1][j2 - 1] + 1.0

            for i2 in range(li, i + 1):
                li2 = l1[i2 - 1]
                for j2 in range(lj, j + 1):
                    lj2 = l2[j2 - 1]
                    c = relabel(nodes1[i2 - 1].op, nodes2[j2 - 1].op)

                    if li2 == li and lj2 == lj:
                        fd[i2][j2] = min(
                            fd[i2 - 1][j2] + 1.0,
                            fd[i2][j2 - 1] + 1.0,
                            fd[i2 - 1][j2 - 1] + c,
                        )
                        td[i2][j2] = fd[i2][j2]
                    else:
                        fd[i2][j2] = min(
                            fd[i2 - 1][j2] + 1.0,
                            fd[i2][j2 - 1] + 1.0,
                            fd[li2 - 1][lj2 - 1] + td[i2][j2],
                        )

    return td[n1][n2]


# ── Structural score ──────────────────────────────────────────────

def structural_score(t1: PNode, t2: PNode) -> float:
    c1 = _canonicalize(t1)
    c2 = _canonicalize(t2)
    denom = _size(c1) + _size(c2)
    if denom == 0:
        return 1.0
    return round(1.0 - _ted(c1, c2) / denom, 4)


# ── Operator score (multiset Jaccard on families) ─────────────────

def _op_multiset(t: PNode) -> Counter:
    c = Counter([t.op])
    for child in t.children:
        c += _op_multiset(child)
    return c


def operator_score(t1: PNode, t2: PNode) -> float:
    c1 = _op_multiset(t1)
    c2 = _op_multiset(t2)
    families = set(c1) | set(c2)

    intersection = sum(min(c1[f], c2[f]) for f in families)
    union = sum(max(c1[f], c2[f]) for f in families)

    if union == 0:
        return 1.0
    return round(intersection / union, 4)


# ── Pretty tree ───────────────────────────────────────────────────

def _tree_lines(node: PNode, prefix: str, is_last: bool) -> str:
    connector = "└── " if is_last else "├── "
    child_prefix = prefix + ("    " if is_last else "│   ")
    line = prefix + connector + node.op + "\n"
    for i, child in enumerate(node.children):
        line += _tree_lines(child, child_prefix, i == len(node.children) - 1)
    return line


def pretty_tree(node: PNode) -> str:
    result = node.op + "\n"
    for i, child in enumerate(node.children):
        result += _tree_lines(child, "", i == len(node.children) - 1)
    return result


# ── Compare ───────────────────────────────────────────────────────

def compare_plans(plan1_obj, plan2_obj) -> dict:
    t1 = parse_plan(plan1_obj)
    t2 = parse_plan(plan2_obj)
    s = structural_score(t1, t2)
    o = operator_score(t1, t2)
    return {
        "structural": s,
        "operator": o,
        "overall": round((s * o), 4),
        "_t1": t1,
        "_t2": t2,
    }


# ── Filename matching helpers ──────────────────────────────────────

_QUERY_NUM_RE = re.compile(r"query_(\d+)_explain\.json$")


def _query_number(filename: str) -> int:
    m = _QUERY_NUM_RE.search(filename)
    return int(m.group(1)) if m else -1


def _list_explain_files(folder: str) -> dict:
    """Returns {filename: full_path} for every query_NNN_explain.json in folder."""
    if not os.path.isdir(folder):
        return {}
    out = {}
    for f in os.listdir(folder):
        if _QUERY_NUM_RE.search(f):
            out[f] = os.path.join(folder, f)
    return out


def _load_json(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


# ── Main comparison driver ─────────────────────────────────────────

def compare_folders(original_folder: str, synthetic_folder: str):
    """
    Compares every matching query_NNN_explain.json filename present in
    both folders. Returns:
        results        : list of dicts (query, filename, structural, operator, overall)
        original_trees : list of (filename, PNode) in matched order
        synthetic_trees: list of (filename, PNode) in matched order
    """
    original_files = _list_explain_files(original_folder)
    synthetic_files = _list_explain_files(synthetic_folder)

    common = sorted(set(original_files) & set(synthetic_files), key=_query_number)
    only_orig = set(original_files) - set(synthetic_files)
    only_synth = set(synthetic_files) - set(original_files)

    if only_orig:
        print(f"[WARN] only in original (skipped): {sorted(only_orig, key=_query_number)}")
    if only_synth:
        print(f"[WARN] only in synthetic (skipped): {sorted(only_synth, key=_query_number)}")

    results = []
    original_trees = []
    synthetic_trees = []

    for filename in common:
        print("\n")
        print("=" * 80)
        print(f"Comparing {filename}")
        print("=" * 80)

        try:
            plan_a = _load_json(original_files[filename])
            plan_b = _load_json(synthetic_files[filename])

            result = compare_plans(plan_a, plan_b)
            t1 = result.pop("_t1")
            t2 = result.pop("_t2")

            original_trees.append((filename, t1))
            synthetic_trees.append((filename, t2))

            print("\nPlan A (original)")
            print("------------------")
            print(pretty_tree(t1))

            print("\nPlan B (synthetic)")
            print("-------------------")
            print(pretty_tree(t2))

            print(f"Structural : {result['structural']:.4f}")
            print(f"Operator   : {result['operator']:.4f}")
            print(f"Overall    : {result['overall']:.4f}")

            results.append({
                "query": _query_number(filename),
                "filename": filename,
                "structural": result["structural"],
                "operator": result["operator"],
                "overall": result["overall"],
            })

        except Exception as e:
            print(f"[ERROR] {filename}: {e}")

    return results, original_trees, synthetic_trees


# ── Export helpers ──────────────────────────────────────────────────

def export_trees(trees, out_path: str, label: str):
    with open(out_path, "w", encoding="utf-8") as fh:
        for filename, tree in trees:
            fh.write("=" * 80 + "\n")
            fh.write(f"{label}: {filename}\n")
            fh.write("=" * 80 + "\n")
            fh.write(pretty_tree(tree))
            fh.write("\n")


def export_results_csv(results, out_path: str):
    with open(out_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(
            fh, fieldnames=["query", "filename", "structural", "operator", "overall"]
        )
        writer.writeheader()
        for row in results:
            writer.writerow(row)


# ── CLI ───────────────────────────────────────────────────────────

if __name__ == "__main__":
    original_folder = sys.argv[1] if len(sys.argv) > 1 else "only_explain_original"
    synthetic_folder = sys.argv[2] if len(sys.argv) > 2 else "only_explain_synthetic"

    results, original_trees, synthetic_trees = compare_folders(original_folder, synthetic_folder)

    export_trees(original_trees, "original_trees.txt", "ORIGINAL")
    export_trees(synthetic_trees, "synthetic_trees.txt", "SYNTHETIC")
    export_results_csv(results, "comparison_results.csv")

    print("\n")
    print("=" * 80)
    print(f"Compared {len(results)} query plans.")
    print("Wrote: original_trees.txt, synthetic_trees.txt, comparison_results.csv")
    print("=" * 80)

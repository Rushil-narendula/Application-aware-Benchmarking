"""
Query Plan Comparator v5
========================
Structural score : 1 - TED / (size_A + size_B)
                   TED node cost uses family labels (JOIN / SCAN / AGG / SORT / FILTER / OTHER)
                   so HASH_JOIN vs NESTED_LOOP costs 0, HASH_JOIN vs SEQ_SCAN costs 1.

Operator score   : multiset Jaccard on family labels
                   intersection / union of family counts across both trees.

Overall score    : 0.5 * structural + 0.5 * operator

Output per query : Plan A tree, Plan B tree, three scores.

Usage:
  python plan_comparator_v5.py file_a.json file_b.json
"""

import json, sys,os
from dataclasses import dataclass, field
from typing import Optional
from collections import Counter


# ── Node canonicalization ─────────────────────────────────────────

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
    return _NODE_MAP.get(raw, raw.upper().replace(" ", "_"))


# ── Family mapping ────────────────────────────────────────────────
# Used for TED relabel cost and Jaccard.
# Within a family, substitution costs 0. Across families, costs 1.

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

import re

def parse_text_plan(plan_lines):
    """
    Converts PostgreSQL text EXPLAIN output into a PNode tree.
    """

    stack = []
    root = None

    for line in plan_lines:

        line = line.rstrip()
        if not line:
            continue

        # Ignore non-plan lines
        if "cost=" not in line:
            continue

        # Count indentation
        indent = len(line) - len(line.lstrip())

        # Remove the arrow if present
        text = line.strip()
        if text.startswith("->"):
            text = text[2:].strip()

        # Remove everything after '('
        text = text.split("(", 1)[0].strip()

        # Remove relation names
        if " on " in text:
            text = text.split(" on ")[0].strip()

        # Remove "using ..."
        if " using " in text:
            text = text.split(" using ")[0].strip()

        # Remove Parallel prefix
        text = re.sub(r"^Parallel\s+", "", text)

        op = _canonical(text)

        # Preserve tree structure for stripped nodes in text EXPLAIN
        if op is None:
            op = "OTHER"

        node = PNode(op=op)

        while stack and stack[-1][0] >= indent:
            stack.pop()

        if stack:
            stack[-1][1].children.append(node)
        else:
            root = node

        stack.append((indent, node))

    return root

# ── Parser ────────────────────────────────────────────────────────

def _parse(node: dict) -> Optional[PNode]:
    raw = node.get("Node Type", "UNKNOWN")
    op  = _canonical(raw)

    children = []
    for child in node.get("Plans", []):
        p = _parse(child)
        if p is not None:
            children.append(p)

    if op is None:  # stripped node — pass children through
        return children[0] if len(children) == 1 else None

    return PNode(op=op, children=children)

def parse_plan(plan_json):

    # New format
    if isinstance(plan_json, dict) and "execution_plan" in plan_json:
        return parse_text_plan(plan_json["execution_plan"])

    # Existing JSON EXPLAIN support
    if isinstance(plan_json, list):
        plan_json = plan_json[0]

    root = plan_json.get("Plan", plan_json)

    result = _parse(root)

    if result is None:
        raise ValueError("Could not parse plan")

    return result

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
# Proper Zhang-Shasha algorithm — finds the true minimum edit script,
# unlike the old greedy child-level DP which broke on depth mismatches.
#
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
    l:     list = []

    def walk(n: PNode) -> int:
        """Recursively index; returns 1-based post-order index of n."""
        first_leaf_l = None
        for idx, child in enumerate(n.children):
            child_pos = walk(child)          # 1-based index of child
            if idx == 0:
                first_leaf_l = l[child_pos - 1]   # leftmost leaf of first child

        nodes.append(n)
        my_pos = len(nodes)                  # 1-based
        l.append(my_pos if first_leaf_l is None else first_leaf_l)
        return my_pos

    walk(node)
    return nodes, l


def _keyroots(l: list, n: int) -> list:
    """
    For each unique value in l, keep the node with the largest 1-based index.
    These are the keyroots needed by Zhang-Shasha.
    """
    seen: dict = {}
    for i in range(1, n + 1):
        seen[l[i - 1]] = i          # overwrite → last (largest) index wins
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

    # td[i][j]: TED between subtree rooted at node i and node j (1-based)
    td = [[0.0] * (n2 + 1) for _ in range(n1 + 1)]
    # fd[i][j]: forest edit distance (reused across keyroot pairs)
    fd = [[0.0] * (n2 + 1) for _ in range(n1 + 1)]

    for i in kr1:
        li = l1[i - 1]                       # 1-based leftmost leaf of node i
        for j in kr2:
            lj = l2[j - 1]

            fd[li - 1][lj - 1] = 0.0

            for i2 in range(li, i + 1):
                fd[i2][lj - 1] = fd[i2 - 1][lj - 1] + 1.0   # delete i2

            for j2 in range(lj, j + 1):
                fd[li - 1][j2] = fd[li - 1][j2 - 1] + 1.0   # insert j2

            for i2 in range(li, i + 1):
                li2 = l1[i2 - 1]
                for j2 in range(lj, j + 1):
                    lj2 = l2[j2 - 1]
                    c   = relabel(nodes1[i2 - 1].op, nodes2[j2 - 1].op)

                    if li2 == li and lj2 == lj:
                        # Both subtrees fully contained in current keyroot window:
                        # standard three-way DP recurrence.
                        fd[i2][j2] = min(
                            fd[i2 - 1][j2] + 1.0,          # delete i2
                            fd[i2][j2 - 1] + 1.0,          # insert j2
                            fd[i2 - 1][j2 - 1] + c,        # relabel / match
                        )
                        td[i2][j2] = fd[i2][j2]
                    else:
                        # One or both subtrees extend outside the window:
                        # reuse previously computed subtree TED.
                        fd[i2][j2] = min(
                            fd[i2 - 1][j2] + 1.0,          # delete i2
                            fd[i2][j2 - 1] + 1.0,          # insert j2
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
    union        = sum(max(c1[f], c2[f]) for f in families)

    if union == 0:
        return 1.0
    return round(intersection / union, 4)


# ── Pretty tree ───────────────────────────────────────────────────

def _tree_lines(node: PNode, prefix: str, is_last: bool) -> str:
    connector    = "└── " if is_last else "├── "
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

def compare_plans(plan1_json, plan2_json) -> dict:
    t1 = parse_plan(plan1_json)
    t2 = parse_plan(plan2_json)
    s  = structural_score(t1, t2)
    o  = operator_score(t1, t2)
    return {
        "structural": s,
        "operator":   o,
        "overall":    round((s * o), 4),
        "_t1": t1,
        "_t2": t2,
    }


# ── File loader ───────────────────────────────────────────────────

def compare_files(path_a: str, path_b: str) -> list:
    with open(path_a) as f: plans_a = json.load(f)
    with open(path_b) as f: plans_b = json.load(f)

    common = set(plans_a) & set(plans_b)
    only_a = set(plans_a) - set(plans_b)
    only_b = set(plans_b) - set(plans_a)
    if only_a: print(f"[WARN] only in A (skipped): {sorted(only_a)}")
    if only_b: print(f"[WARN] only in B (skipped): {sorted(only_b)}")

    results = []
    for key in sorted(common, key=lambda k: int(k.split("_")[-1])):
        try:
            r = compare_plans(plans_a[key], plans_b[key])
            results.append({"query": key, "result": r})
        except Exception as e:
            print(f"[ERROR] {key}: {e}")
    return results


# ── CLI ───────────────────────────────────────────────────────────

# ── CLI ───────────────────────────────────────────────────────────

if __name__ == "__main__":

    # Folder paths
    original_folder = "only_explain_original"
    synthetic_folder = "only_explain_synthetic"

    original_files = sorted(
        [f for f in os.listdir(original_folder) if f.endswith(".json")]
    )

    synthetic_files = set(
        f for f in os.listdir(synthetic_folder) if f.endswith(".json")
    )

    total = 0

    for filename in original_files:

        if filename not in synthetic_files:
            print(f"[WARN] {filename} not found in synthetic folder.")
            continue

        path_a = os.path.join(original_folder, filename)
        path_b = os.path.join(synthetic_folder, filename)

        print("\n")
        print("=" * 80)
        print(f"Comparing {filename}")
        print("=" * 80)

        try:
            with open(path_a) as f:
                plan_a = json.load(f)

            with open(path_b) as f:
                plan_b = json.load(f)

            result = compare_plans(plan_a, plan_b)

            t1 = result.pop("_t1")
            t2 = result.pop("_t2")

            print("\nPlan A")
            print("------")
            print(pretty_tree(t1))

            print("\nPlan B")
            print("------")
            print(pretty_tree(t2))

            print(f"Structural : {result['structural']:.4f}")
            print(f"Operator   : {result['operator']:.4f}")
            print(f"Overall    : {result['overall']:.4f}")

            total += 1

        except Exception as e:
            print(f"[ERROR] {filename}: {e}")

    print("\n")
    print("=" * 80)
    print(f"Compared {total} query plans.")
    print("=" * 80)
"""
comparator.py — Structural plan comparison & hashing.

Determines whether two PostgreSQL execution plans are structurally
identical by extracting shape-defining keys, normalizing literals,
and producing an MD5 hash of the canonical JSON representation.
"""

import json
import hashlib
import re

SHAPE_KEYS = [
    "Node Type", "Strategy", "Join Type", "Parent Relationship",
    "Relation Name", "Schema", "Alias", "Index Name",
    "Parallel Aware", "Scan Direction", "Custom Plan Provider",
    "Command", "CTE Name", "Subplan Name", "Function Name",
    "Operation", "Sort Key", "Group Key", "Plans",
]

CONDITION_KEYS = [
    "Filter", "Index Cond", "Hash Cond", "Merge Cond",
    "Recheck Cond", "Join Filter", "One-Time Filter", "TID Cond",
]

_LITERAL_PATTERNS = [
    (re.compile(r"'[^']*'"),         "'?'"),
    (re.compile(r"\b\d+(\.\d+)?\b"), "?"),
]


def normalize_condition(cond: str) -> str:
    for pattern, replacement in _LITERAL_PATTERNS:
        cond = pattern.sub(replacement, cond)
    return cond


def clean_node(node: dict, strict: bool = False) -> dict:
    cleaned = {}
    for key in SHAPE_KEYS:
        if key in node:
            if key == "Plans":
                cleaned[key] = [clean_node(child, strict) for child in node[key]]
            else:
                cleaned[key] = node[key]
    for key in CONDITION_KEYS:
        if key in node:
            cleaned[key] = node[key] if strict else normalize_condition(str(node[key]))
    return cleaned


def structural_hash(plan: dict, strict: bool = False) -> str:
    cleaned = clean_node(plan["Plan"], strict)
    canonical = json.dumps(cleaned, sort_keys=True).encode("utf-8")
    return hashlib.md5(canonical).hexdigest()


def count_nodes(node: dict) -> int:
    return 1 + sum(count_nodes(c) for c in node.get("Plans", []))


def max_depth(node: dict, depth: int = 0) -> int:
    child_depths = [max_depth(c, depth + 1) for c in node.get("Plans", [])]
    return max(child_depths) if child_depths else depth


def short_label(plan: dict) -> str:
    def _node_label(node):
        nt = node.get("Node Type", "?")
        parts = [nt]
        if "Relation Name" in node:
            parts.append(f'on {node["Relation Name"]}')
        if "Index Name" in node:
            parts.append(f'using {node["Index Name"]}')
        if "Join Type" in node:
            parts.append(f'({node["Join Type"]})')
        if node.get("Parallel Aware"):
            parts.insert(0, "Parallel")
        return " ".join(parts)

    def _walk(node):
        labels = []
        nt = node.get("Node Type", "")
        skip = nt in ("Aggregate", "Sort", "Hash", "Materialize",
                       "Incremental Sort", "Memoize")
        if not skip:
            labels.append(_node_label(node))
        for child in node.get("Plans", []):
            labels.extend(_walk(child))
        return labels

    labels = _walk(plan["Plan"])
    if not labels:
        return _node_label(plan["Plan"])
    return " → ".join(labels)


def build_tree_lines(node: dict, prefix: str = "", is_last: bool = True) -> list:
    connector = "└── " if is_last else "├── "
    label = node.get("Node Type", "?")
    extras = []
    if "Relation Name" in node:
        extras.append(f'on {node["Relation Name"]}')
    if "Index Name" in node:
        extras.append(f'using {node["Index Name"]}')
    if "Join Type" in node:
        extras.append(f'({node["Join Type"]})')
    for ck in CONDITION_KEYS:
        if ck in node:
            extras.append(f'{ck}: {node[ck]}')
    detail = "  " + ", ".join(extras) if extras else ""
    lines = [f"{prefix}{connector}{label}{detail}"]
    children = node.get("Plans", [])
    child_prefix = prefix + ("    " if is_last else "│   ")
    for i, child in enumerate(children):
        lines.extend(build_tree_lines(child, child_prefix, i == len(children) - 1))
    return lines


def plan_tree_str(plan: dict) -> str:
    return "\n".join(build_tree_lines(plan["Plan"], prefix="  ", is_last=True))
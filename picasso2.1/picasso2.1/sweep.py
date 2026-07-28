"""
sweep.py — Full grid sweep for switch point detection.

1D queries (QT1, QT6, QT13): sweep p1 across N points
2D queries: sweep p1 × p2 on N×N grid

At each grid point: EXPLAIN (FORMAT JSON) → structural hash → store plan ID.
Then detects all switch boundaries where adjacent grid cells have different plans.

Outputs per query (results/<qt>/):
    sweep_results.json   — complete grid data, phases, switches, plan trees
    switches.json        — raw plan pairs (for analyze.py)
    plan_diagram.json    — 2D grid of plan IDs (for Picasso-style visualization)

Usage:
    python sweep.py qt1                     # single query
    python sweep.py --all                   # all 17 queries
    python sweep.py --all --resolution 30   # 30×30 grid (default 100)
    python sweep.py --all --resolution 100  # full Picasso resolution
"""

import psycopg2
import json
import os
import argparse
from datetime import datetime
from collections import OrderedDict

from comparator import structural_hash, short_label, plan_tree_str, count_nodes, max_depth
from query_registry import QUERIES, QUERY_ORDER

DB_CONFIG = {
    "host": "localhost", "port": 5432,
    "database": "tpchdb_sf1",
    "user": "postgres", "password": "17112003",
}
DEFAULT_RES = 1000
SKIP = {"qt9", "qt20"}


def connect():
    return psycopg2.connect(**DB_CONFIG)

def get_range(cur, table, col):
    cur.execute(f"SELECT MIN({col}), MAX({col}) FROM {table}")
    lo, hi = cur.fetchone()
    return float(lo), float(hi)

def load_sql(sql_file):
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), sql_file)) as f:
        return f.read()


def make_params(fracs, ranges, n_pl):
    """Convert list of fractions [0..1] to parameter values."""
    vals = []
    for i, r in enumerate(ranges):
        f = fracs[i] if i < len(fracs) else fracs[i % len(fracs)]
        vals.append(r["min"] + f * (r["max"] - r["min"]))
    while len(vals) < n_pl:
        vals.append(vals[len(vals) % len(ranges)])
    return tuple(vals)


def get_plan(cur, sql, params):
    cur.execute(f"EXPLAIN (FORMAT JSON) {sql}", params)
    return cur.fetchone()[0][0]


# ═══════════════════════════════════════════════════════════════
#  GRID SWEEP
# ═══════════════════════════════════════════════════════════════

def sweep_query(cur, qname, qinfo, res):
    """Full grid sweep. Returns (grid, plans_catalog, switches, meta)."""
    sql_raw = load_sql(qinfo["sql_file"])
    sql = sql_raw.replace(":p1", "%s").replace(":p2", "%s")
    n_pl = sql.count("%s")
    dims = len(qinfo["params"])

    # Discover ranges
    ranges = []
    for p in qinfo["params"]:
        lo, hi = get_range(cur, p["table"], p["column"])
        ranges.append({"min": lo, "max": hi, "label": p["label"],
                       "table": p["table"], "column": p["column"]})

    # Build grid points
    ticks = [(i + 0.5) / res for i in range(res)]

    # plans_catalog: hash → {plan, label, costs, ...}  (deduplicated)
    plans_catalog = OrderedDict()
    plan_id_map = {}  # hash → integer ID (1-based)

    if dims == 1:
        # 1D: N points
        grid = []  # list of {frac, hash, plan_id}
        for f1 in ticks:
            params = make_params([f1], ranges, n_pl)
            plan = get_plan(cur, sql, params)
            h = structural_hash(plan)
            if h not in plans_catalog:
                pid = len(plans_catalog) + 1
                plan_id_map[h] = pid
                root = plan["Plan"]
                plans_catalog[h] = {
                    "plan_id": pid, "hash": h,
                    "label": short_label(plan),
                    "plan": plan,
                    "total_cost": root.get("Total Cost", 0),
                    "plan_rows": root.get("Plan Rows", 0),
                    "nodes": count_nodes(root),
                    "depth": max_depth(root),
                    "plan_tree": plan_tree_str(plan),
                }
            grid.append({"frac1": f1, "hash": h, "plan_id": plan_id_map[h]})

    else:
        # 2D: N×N grid
        # grid[i][j] = {frac1, frac2, hash, plan_id}
        grid = []
        total = res * res
        done = 0
        for i, f1 in enumerate(ticks):
            row = []
            for j, f2 in enumerate(ticks):
                params = make_params([f1, f2], ranges, n_pl)
                plan = get_plan(cur, sql, params)
                h = structural_hash(plan)
                if h not in plans_catalog:
                    pid = len(plans_catalog) + 1
                    plan_id_map[h] = pid
                    root = plan["Plan"]
                    plans_catalog[h] = {
                        "plan_id": pid, "hash": h,
                        "label": short_label(plan),
                        "plan": plan,
                        "total_cost": root.get("Total Cost", 0),
                        "plan_rows": root.get("Plan Rows", 0),
                        "nodes": count_nodes(root),
                        "depth": max_depth(root),
                        "plan_tree": plan_tree_str(plan),
                    }
                row.append({"frac1": f1, "frac2": f2, "hash": h, "plan_id": plan_id_map[h]})
                done += 1
            grid.append(row)
            # Progress
            if (i + 1) % max(1, res // 10) == 0:
                print(f"    {done}/{total} ({done*100//total}%)")

    # ── Find switches: adjacent cells with different plans ──
    switches = _find_switches(grid, dims, plans_catalog, ranges, n_pl, cur, sql)

    meta = {
        "query": qname, "label": qinfo.get("label", qname),
        "dims": dims, "resolution": res,
        "ranges": ranges, "n_placeholders": n_pl,
        "num_plans": len(plans_catalog),
        "num_switches": len(switches),
        "timestamp": datetime.now().isoformat(),
    }
    return grid, plans_catalog, switches, meta


def _find_switches(grid, dims, catalog, ranges, n_pl, cur, sql):
    """Find ALL plan transitions in the grid (no dedup — every adjacent flip counts).

    1D: every (i-1, i) flip → one switch, with 1D binary-refined boundary.
    2D: right + bottom neighbors → every flipped edge becomes one switch.
    """
    switches = []

    if dims == 1:
        for i in range(1, len(grid)):
            h_prev = grid[i-1]["hash"]
            h_curr = grid[i]["hash"]
            if h_prev == h_curr:
                continue
            # Binary search for exact boundary
            lo_f = grid[i-1]["frac1"]
            hi_f = grid[i]["frac1"]
            exact_f, exact_plan = _refine_1d(cur, sql, n_pl, ranges, lo_f, hi_f, h_prev)

            from_info = catalog[h_prev]
            to_info = catalog.get(h_curr, catalog.get(structural_hash(exact_plan), {}))

            switches.append({
                "switch_num": len(switches) + 1,
                "frac": exact_f,
                "selectivity_pct": exact_f * 100,
                "param_values": list(make_params([exact_f], ranges, n_pl)),
                "from_hash": h_prev, "to_hash": h_curr,
                "from_label": from_info.get("label",""),
                "to_label": to_info.get("label", short_label(exact_plan)),
                "from_cost": from_info.get("total_cost", 0),
                "to_cost": to_info.get("total_cost", 0),
                "from_plan": from_info.get("plan", {}),
                "to_plan": to_info.get("plan", exact_plan),
            })

    else:  # 2D — right + bottom, no dedup
        N = len(grid)
        for i in range(N):
            for j in range(N):
                h = grid[i][j]["hash"]
                # Right neighbor → switch along p2 axis
                if j + 1 < N:
                    h2 = grid[i][j+1]["hash"]
                    if h != h2:
                        _add_2d_switch(switches, grid[i][j], grid[i][j+1],
                                       catalog, ranges, n_pl)
                # Bottom neighbor → switch along p1 axis
                if i + 1 < N:
                    h2 = grid[i+1][j]["hash"]
                    if h != h2:
                        _add_2d_switch(switches, grid[i][j], grid[i+1][j],
                                       catalog, ranges, n_pl)

    # Sort by selectivity (diagonal fraction) and re-number
    switches.sort(key=lambda s: s.get("frac", 0))
    for i, s in enumerate(switches, 1):
        s["switch_num"] = i

    return switches


def _add_2d_switch(switches, cell_a, cell_b, catalog, ranges, n_pl):
    """Add a 2D switch between two adjacent cells."""
    ha, hb = cell_a["hash"], cell_b["hash"]
    info_a = catalog[ha]
    info_b = catalog[hb]

    # Use midpoint between the two cells as the switch location
    mid_f1 = (cell_a["frac1"] + cell_b["frac1"]) / 2
    mid_f2 = (cell_a["frac2"] + cell_b["frac2"]) / 2
    diag_frac = (mid_f1 + mid_f2) / 2  # approximate diagonal position

    switches.append({
        "switch_num": len(switches) + 1,
        "frac": diag_frac,
        "frac1": mid_f1, "frac2": mid_f2,
        "selectivity_pct": diag_frac * 100,
        "selectivity_p1_pct": mid_f1 * 100,
        "selectivity_p2_pct": mid_f2 * 100,
        "param_values": list(make_params([mid_f1, mid_f2], ranges, n_pl)),
        "from_hash": ha, "to_hash": hb,
        "from_label": info_a["label"],
        "to_label": info_b["label"],
        "from_cost": info_a["total_cost"],
        "to_cost": info_b["total_cost"],
        "from_plan": info_a["plan"],
        "to_plan": info_b["plan"],
        "from_plan_id": info_a["plan_id"],
        "to_plan_id": info_b["plan_id"],
    })


def _refine_1d(cur, sql, n_pl, ranges, lo, hi, hash_before):
    """Binary search for exact 1D boundary."""
    new_plan = None
    for _ in range(20):
        if hi - lo < 0.0001: break
        mid = (lo + hi) / 2
        p = make_params([mid], ranges, n_pl)
        plan = get_plan(cur, sql, p)
        h = structural_hash(plan)
        if h == hash_before:
            lo = mid
        else:
            hi = mid; new_plan = plan
    if not new_plan:
        p = make_params([hi], ranges, n_pl)
        new_plan = get_plan(cur, sql, p)
    return hi, new_plan


# ═══════════════════════════════════════════════════════════════
#  OUTPUT
# ═══════════════════════════════════════════════════════════════

def save_results(outdir, qname, qinfo, grid, catalog, switches, meta, elapsed):
    os.makedirs(outdir, exist_ok=True)
    dims = meta["dims"]

    # ── Plan catalog (all unique plans) ──
    cat_export = []
    for h, info in catalog.items():
        cat_export.append({
            "plan_id": info["plan_id"], "hash": info["hash"],
            "label": info["label"],
            "total_cost": info["total_cost"],
            "plan_rows": info["plan_rows"],
            "nodes": info["nodes"], "depth": info["depth"],
            "plan_tree": info["plan_tree"],
        })

    # ── Plan diagram (grid of plan IDs) ──
    if dims == 1:
        diagram = [{"frac": g["frac1"], "sel_pct": g["frac1"]*100,
                     "plan_id": g["plan_id"]} for g in grid]
    else:
        diagram = []
        for i, row in enumerate(grid):
            for j, cell in enumerate(row):
                diagram.append({
                    "i": i, "j": j,
                    "frac1": cell["frac1"], "frac2": cell["frac2"],
                    "sel_p1_pct": cell["frac1"]*100,
                    "sel_p2_pct": cell["frac2"]*100,
                    "plan_id": cell["plan_id"],
                })

    # ── Switches (with plan trees, no raw plan dicts) ──
    sw_export = []
    for sw in switches:
        entry = {
            "switch_num": sw["switch_num"],
            "frac": sw["frac"],
            "selectivity_pct": sw["selectivity_pct"],
            "param_values": sw["param_values"],
            "from_label": sw["from_label"], "to_label": sw["to_label"],
            "from_cost": sw["from_cost"], "to_cost": sw["to_cost"],
            "cost_ratio": sw["to_cost"] / sw["from_cost"] if sw["from_cost"] > 0 else 0,
        }
        if dims == 2:
            entry["selectivity_p1_pct"] = sw.get("selectivity_p1_pct", 0)
            entry["selectivity_p2_pct"] = sw.get("selectivity_p2_pct", 0)
            entry["from_plan_id"] = sw.get("from_plan_id", 0)
            entry["to_plan_id"] = sw.get("to_plan_id", 0)
        sw_export.append(entry)

    # Main results JSON
    export = {
        "meta": meta, "elapsed_seconds": elapsed,
        "num_plans": len(catalog), "num_switches": len(switches),
        "plan_catalog": cat_export,
        "switches": sw_export,
    }
    with open(os.path.join(outdir, "sweep_results.json"), "w") as f:
        json.dump(export, f, indent=2, default=str)

    # Plan diagram JSON (for visualization)
    with open(os.path.join(outdir, "plan_diagram.json"), "w") as f:
        json.dump({"meta": meta, "grid": diagram, "plans": cat_export}, f, indent=2, default=str)

    # Raw switches with plan dicts (for analyze.py)
    sw_raw = []
    for sw in switches:
        sw_raw.append({
            "switch_num": sw["switch_num"],
            "frac": sw["frac"],
            "selectivity_pct": sw["selectivity_pct"],
            "param_values": sw["param_values"],
            "from_hash": sw["from_hash"], "to_hash": sw["to_hash"],
            "from_label": sw["from_label"], "to_label": sw["to_label"],
            "from_cost": sw["from_cost"], "to_cost": sw["to_cost"],
            "from_plan": sw["from_plan"], "to_plan": sw["to_plan"],
        })
    with open(os.path.join(outdir, "switches.json"), "w") as f:
        json.dump(sw_raw, f, indent=2, default=str)

    # Phases JSON (compatibility)
    phases = _extract_phases(grid, catalog, dims)
    with open(os.path.join(outdir, "phases.json"), "w") as f:
        json.dump(phases, f, indent=2)

    # Text report
    W = 85
    lines = ["=" * W]
    lines.append(f"  {meta['label']} — Grid Sweep Report".center(W))
    lines.append("=" * W)
    lines.append(f"  Dimensions  : {dims}D")
    lines.append(f"  Resolution  : {meta['resolution']}{'×'+str(meta['resolution']) if dims==2 else ''}")
    lines.append(f"  Grid points : {meta['resolution']**dims}")
    lines.append(f"  Unique plans: {len(catalog)}")
    lines.append(f"  Switch pairs: {len(switches)}")
    lines.append(f"  Elapsed     : {elapsed:.1f}s")
    lines.append("")
    lines.append("  Parameters:")
    for i, r in enumerate(meta["ranges"]):
        lines.append(f"    p{i+1}: {r['table']}.{r['column']}  [{r['min']:.2f} — {r['max']:.2f}]")
    lines.append("")
    lines.append("  Unique Plans:")
    for info in cat_export:
        lines.append(f"    Plan {info['plan_id']:>2d}: cost={info['total_cost']:>12,.0f}  "
                     f"rows={info['plan_rows']:>8,}  {info['label'][:50]}")
    lines.append("")
    if switches:
        lines.append("  Switch Boundaries:")
        for sw in sw_export[:30]:  # cap at 30 for readability
            lines.append(f"    #{sw['switch_num']:>2d}  sel={sw['selectivity_pct']:5.1f}%  "
                        f"cost {sw['from_cost']:,.0f}→{sw['to_cost']:,.0f} "
                        f"({sw['cost_ratio']:.2f}x)")
        if len(sw_export) > 30:
            lines.append(f"    ... and {len(sw_export)-30} more")
    lines.append("")
    lines.append("=" * W)
    with open(os.path.join(outdir, "sweep_report.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def _extract_phases(grid, catalog, dims):
    """Extract 1D phases (for compatibility with analyze.py)."""
    if dims == 1:
        phases = []
        prev_h = None
        for g in grid:
            h = g["hash"]
            if h != prev_h:
                info = catalog[h]
                phases.append({
                    "start_frac": g["frac1"], "end_frac": g["frac1"],
                    "hash": h, "label": info["label"],
                    "cost": info["total_cost"], "rows": info["plan_rows"],
                    "nodes": info["nodes"], "depth": info["depth"],
                })
            else:
                phases[-1]["end_frac"] = g["frac1"]
            prev_h = h
        if phases:
            phases[-1]["end_frac"] = 1.0
        return phases
    else:
        # For 2D, extract diagonal phases
        N = len(grid)
        phases = []
        prev_h = None
        for i in range(N):
            j = i  # diagonal
            if j >= len(grid[i]): j = len(grid[i]) - 1
            cell = grid[i][j]
            h = cell["hash"]
            frac = cell["frac1"]
            if h != prev_h:
                info = catalog[h]
                phases.append({
                    "start_frac": frac, "end_frac": frac,
                    "hash": h, "label": info["label"],
                    "cost": info["total_cost"], "rows": info["plan_rows"],
                    "nodes": info["nodes"], "depth": info["depth"],
                })
            else:
                phases[-1]["end_frac"] = frac
            prev_h = h
        if phases:
            phases[-1]["end_frac"] = 1.0
        return phases


# ═══════════════════════════════════════════════════════════════
#  MAIN
# ═══════════════════════════════════════════════════════════════

def run_one(cur, qname, qinfo, res, base):
    outdir = os.path.join(base, qname)
    dims = len(qinfo["params"])
    grid_str = f"{res}" if dims == 1 else f"{res}×{res}"

    print(f"\n{'═'*60}")
    print(f"  {qinfo.get('label', qname)}")
    print(f"  {dims}D sweep, {grid_str} = {res**dims} points")
    print(f"{'═'*60}")

    t0 = datetime.now()
    grid, catalog, switches, meta = sweep_query(cur, qname, qinfo, res)
    elapsed = (datetime.now() - t0).total_seconds()

    print(f"\n  {len(catalog)} unique plans, {len(switches)} switch pairs  [{elapsed:.1f}s]")
    for sw in switches[:10]:
        print(f"    #{sw['switch_num']} sel≈{sw['selectivity_pct']:.1f}%  "
              f"{sw['from_label'][:30]}→{sw['to_label'][:30]}")
    if len(switches) > 10:
        print(f"    ... and {len(switches)-10} more")

    save_results(outdir, qname, qinfo, grid, catalog, switches, meta, elapsed)
    print(f"  → {outdir}/")
    return {"query": qname, "dims": dims, "plans": len(catalog),
            "switches": len(switches), "elapsed": elapsed}


def main():
    parser = argparse.ArgumentParser(description="Grid Sweep for Switch Points")
    parser.add_argument("queries", nargs="*")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--resolution", type=int, default=DEFAULT_RES)
    parser.add_argument("--output", default="results")
    args = parser.parse_args()

    if args.all:
        names = [q for q in QUERY_ORDER if q not in SKIP]
    elif args.queries:
        names = args.queries
    else:
        parser.print_help()
        print(f"\nAvailable: {', '.join(QUERY_ORDER)}")
        return

    base = os.path.join(os.path.dirname(os.path.abspath(__file__)), args.output)
    conn = connect(); cur = conn.cursor()

    print(f"\n{'━'*60}")
    print(f"  Grid Sweep — {len(names)} queries, resolution={args.resolution}")
    print(f"{'━'*60}")

    results = []
    for qn in names:
        if qn not in QUERIES: print(f"  Unknown: {qn}"); continue
        try:
            results.append(run_one(cur, qn, QUERIES[qn], args.resolution, base))
        except Exception as e:
            print(f"  ERROR on {qn}: {e}")
            import traceback; traceback.print_exc()

    cur.close(); conn.close()

    print(f"\n{'━'*60}")
    print(f"  {'Query':<8s} {'Dims':>4s} {'Plans':>6s} {'Switches':>8s} {'Time':>8s}")
    for r in results:
        print(f"  {r['query']:<8s} {r['dims']:>4d}D {r['plans']:>6d} {r['switches']:>8d} {r['elapsed']:>7.1f}s")
    tot = sum(r['elapsed'] for r in results)
    print(f"\n  Total: {tot:.0f}s ({tot/60:.1f} min)")
    print(f"{'━'*60}\n")


if __name__ == "__main__":
    main()
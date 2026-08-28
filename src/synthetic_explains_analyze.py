#!/usr/bin/env python3
"""
explain_job_queries.py
======================
Extracts all 113 JOB benchmark SQL queries from the embedded JavaScript RAW
object inside the supplied HTML file, then runs EXPLAIN (ANALYZE, BUFFERS,
VERBOSE, FORMAT JSON) on each query against a PostgreSQL database and saves
the resulting execution plans to disk.

Output modes
------------
  --mode split   → explain_results/query_001.json … query_113.json  (default)
  --mode single  → all_explain_plans.json

Usage example
-------------
  python explain_job_queries.py \\
      --html-file queries.html \\
      --host localhost --port 5432 \\
      --dbname movie_db --user postgres --password secret \\
      --mode split

Requirements
------------
  pip install psycopg2-binary beautifulsoup4
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import re
import sys
import time
from pathlib import Path
from typing import Any

import psycopg2
import psycopg2.extras

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

# EXPLAIN command issued for every query.  FORMAT JSON makes the plan machine-
# readable; ANALYZE actually executes the query; BUFFERS reports I/O hits.
EXPLAIN_PREFIX = "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON)"

# Directory created when running in "split" output mode.
SPLIT_OUTPUT_DIR = Path("explain_results")

# File created when running in "single" output mode.
SINGLE_OUTPUT_FILE = Path("all_explain_plans.json")

# ---------------------------------------------------------------------------
# Logging setup
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# HTML / JS parsing
# ---------------------------------------------------------------------------

def extract_raw_queries(html_path: Path) -> dict[str, str]:
    """
    Parse the HTML file and extract the ``RAW`` JavaScript object.

    The object is declared on a single line as:
        const RAW = {"1": "SELECT ...", "2": "SELECT ...", ...};

    Strategy
    --------
    1. Read the raw HTML text.
    2. Use a non-greedy regex to capture the JSON-serialisable value that
       follows ``const RAW =`` up to the first ``};`` token on the same line.
    3. Pass the captured substring directly to ``json.loads``.  Because the
       JS object uses JSON-compatible syntax (double-quoted keys and values,
       ``\\n`` escape sequences, etc.) this round-trips cleanly.

    Returns
    -------
    dict mapping string query IDs (``"1"`` … ``"113"``) to SQL text.

    Raises
    ------
    ValueError   if the RAW object cannot be located or parsed.
    FileNotFoundError  if the HTML file does not exist.
    """
    if not html_path.exists():
        raise FileNotFoundError(f"HTML file not found: {html_path}")

    html_text = html_path.read_text(encoding="utf-8")

    # Match:  const RAW = { ... };
    # The value is always on a single line in this file, so re.DOTALL is not
    # strictly needed, but we include it for robustness.
    pattern = re.compile(
        r"const\s+RAW\s*=\s*(\{.*?\})\s*;",
        re.DOTALL,
    )
    match = pattern.search(html_text)
    if not match:
        raise ValueError(
            "Could not find 'const RAW = {...};' declaration in the HTML file. "
            "Check that the file contains the expected JavaScript structure."
        )

    raw_json_text = match.group(1)

    try:
        queries: dict[str, str] = json.loads(raw_json_text)
    except json.JSONDecodeError as exc:
        raise ValueError(
            f"Found the RAW object but failed to parse it as JSON: {exc}"
        ) from exc

    if not queries:
        raise ValueError("RAW object parsed successfully but contains no entries.")

    logger.info("Extracted %d queries from HTML file.", len(queries))
    return queries


# ---------------------------------------------------------------------------
# Query normalisation
# ---------------------------------------------------------------------------

def normalise_query(sql: str) -> str:
    """
    Return the SQL text stripped of leading/trailing whitespace and any
    trailing semicolon, ready to be prefixed with EXPLAIN options.
    """
    return sql.strip().rstrip(";").strip()


# ---------------------------------------------------------------------------
# Database connection
# ---------------------------------------------------------------------------

def build_dsn(args: argparse.Namespace) -> dict[str, Any]:
    """Build a psycopg2 connection-keyword dictionary from CLI arguments."""
    dsn: dict[str, Any] = {
        "host": args.host,
        "port": args.port,
        "dbname": args.dbname,
        "user": args.user,
        "connect_timeout": 30,
        # Statement timeout of 0 means no limit — required because some EXPLAIN
        # ANALYZE runs on large JOB queries can take several minutes.
        "options": "-c statement_timeout=0",
    }
    if args.password:
        dsn["password"] = args.password
    return dsn


# ---------------------------------------------------------------------------
# EXPLAIN ANALYZE execution
# ---------------------------------------------------------------------------

def run_explain(
    cursor: "psycopg2.extensions.cursor",
    query_id: str,
    sql: str,
) -> tuple[list[Any] | None, str | None]:
    """
    Execute EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) for *sql*.

    Returns
    -------
    (plan, error_message)
        On success: (list_containing_json_plan, None)
        On failure: (None, error_string)

    Notes
    -----
    * The connection is *not* committed or rolled back here; callers are
      responsible for transaction management.
    * psycopg2 returns the FORMAT JSON plan as a Python list; the first
      element is the top-level plan dict.
    """
    explain_sql = f"{EXPLAIN_PREFIX}\n{sql}"
    try:
        cursor.execute(explain_sql)
        rows = cursor.fetchall()
        # FORMAT JSON → single row, single column; the value is already a
        # Python list (psycopg2 auto-deserialises JSON).
        plan = rows[0][0]
        return plan, None
    except Exception as exc:  # noqa: BLE001
        return None, str(exc)


# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------

def save_split(
    query_id: str,
    payload: dict[str, Any],
    output_dir: Path,
) -> None:
    """Write a single plan to ``output_dir/query_NNN.json``."""
    output_dir.mkdir(parents=True, exist_ok=True)
    filename = output_dir / f"query_{int(query_id):03d}.json"
    filename.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def save_single(all_plans: dict[str, Any], output_file: Path) -> None:
    """Write all plans to a single JSON file."""
    output_file.write_text(
        json.dumps(all_plans, indent=2, ensure_ascii=False), encoding="utf-8"
    )


# ---------------------------------------------------------------------------
# Main execution loop
# ---------------------------------------------------------------------------

def run(args: argparse.Namespace) -> int:
    """
    Main entry point.

    Returns
    -------
    int  exit code: 0 = all queries succeeded, 1 = at least one failed.
    """
    # ── 1. Parse HTML ───────────────────────────────────────────────────────
    html_path = Path(args.html_file)
    try:
        queries = extract_raw_queries(html_path)
    except (FileNotFoundError, ValueError) as exc:
        logger.error("HTML parsing failed: %s", exc)
        return 1

    # Sort keys numerically so progress output is ordered 1 → 113.
    sorted_ids = sorted(queries.keys(), key=lambda k: int(k))
    total = len(sorted_ids)

    # ── 2. Connect to PostgreSQL ─────────────────────────────────────────────
    dsn = build_dsn(args)
    logger.info(
        "Connecting to PostgreSQL  host=%s  port=%s  dbname=%s  user=%s",
        args.host, args.port, args.dbname, args.user,
    )
    try:
        conn = psycopg2.connect(**dsn)
    except psycopg2.OperationalError as exc:
        logger.error("Could not connect to PostgreSQL: %s", exc)
        return 1

    # Use autocommit so each EXPLAIN ANALYZE runs in its own implicit
    # transaction and a failure does not poison subsequent queries.
    conn.autocommit = True

    # ── 3. Prepare output structures ─────────────────────────────────────────
    use_split = (args.mode == "split")
    all_plans: dict[str, Any] = {}  # populated in "single" mode

    success_count = 0
    failure_count = 0
    wall_start = time.perf_counter()

    # ── 4. Execute loop ───────────────────────────────────────────────────────
    with conn:
        with conn.cursor() as cur:
            for idx, query_id in enumerate(sorted_ids, start=1):
                raw_sql = queries[query_id]
                sql = normalise_query(raw_sql)

                q_start = time.perf_counter()
                plan, error = run_explain(cur, query_id, sql)
                elapsed = time.perf_counter() - q_start

                if error:
                    # ── failure path ──────────────────────────────────────────
                    failure_count += 1
                    logger.warning(
                        "[%d/%d]  Query %s  FAILED  (%.2fs)  %s",
                        idx, total, query_id, elapsed, error,
                    )
                    payload: dict[str, Any] = {
                        "query_id": query_id,
                        "status": "error",
                        "error": error,
                        "sql": raw_sql,
                        "elapsed_seconds": round(elapsed, 4),
                    }
                else:
                    # ── success path ──────────────────────────────────────────
                    success_count += 1
                    logger.info(
                        "[%d/%d]  Query %s  OK       (%.2fs)",
                        idx, total, query_id, elapsed,
                    )
                    payload = {
                        "query_id": query_id,
                        "status": "ok",
                        "elapsed_seconds": round(elapsed, 4),
                        "plan": plan,
                        "sql": raw_sql,
                    }

                # ── persist ───────────────────────────────────────────────────
                if use_split:
                    save_split(query_id, payload, SPLIT_OUTPUT_DIR)
                else:
                    all_plans[query_id] = payload

    conn.close()

    # ── 5. Flush single-file output ───────────────────────────────────────────
    if not use_split:
        save_single(all_plans, SINGLE_OUTPUT_FILE)
        logger.info("Saved combined output → %s", SINGLE_OUTPUT_FILE)
    else:
        logger.info("Saved per-query files → %s/", SPLIT_OUTPUT_DIR)

    # ── 6. Summary ────────────────────────────────────────────────────────────
    total_elapsed = time.perf_counter() - wall_start
    print()
    print("=" * 54)
    print(f"  Total queries   : {total}")
    print(f"  Successful      : {success_count}")
    print(f"  Failed          : {failure_count}")
    print(f"  Total runtime   : {total_elapsed:.1f}s  ({total_elapsed/60:.1f} min)")
    print("=" * 54)

    return 0 if failure_count == 0 else 1


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Run EXPLAIN ANALYZE on every JOB benchmark query extracted from "
            "an HTML file and save the resulting execution plans to JSON."
        ),
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )

    # ── Connection arguments ─────────────────────────────────────────────────
    db_group = parser.add_argument_group("Database connection")
    db_group.add_argument("--host",     default="localhost",  help="PostgreSQL host")
    db_group.add_argument("--port",     default=5432, type=int, help="PostgreSQL port")
    db_group.add_argument("--dbname",   default="movie_db",   help="Database name")
    db_group.add_argument("--user",     default="postgres",   help="Database user")
    db_group.add_argument(
        "--password", default=os.environ.get("PGPASSWORD", ""),
        help="Database password (falls back to $PGPASSWORD env var)"
    )

    # ── Input ────────────────────────────────────────────────────────────────
    parser.add_argument(
        "--html-file",
        default="queries.html",
        help="Path to the HTML file containing the embedded RAW JS object",
    )

    # ── Output mode ──────────────────────────────────────────────────────────
    parser.add_argument(
        "--mode",
        choices=["split", "single"],
        default="split",
        help=(
            "split  → write one JSON file per query inside explain_results/; "
            "single → write all plans into all_explain_plans.json"
        ),
    )

    # ── Verbosity ────────────────────────────────────────────────────────────
    parser.add_argument(
        "--quiet", action="store_true",
        help="Suppress per-query INFO log lines (errors are always shown)"
    )

    return parser.parse_args(argv)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    args = parse_args()

    if args.quiet:
        logging.getLogger().setLevel(logging.WARNING)

    sys.exit(run(args))

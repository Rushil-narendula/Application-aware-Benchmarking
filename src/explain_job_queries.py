#!/usr/bin/env python3
"""
explain_job_queries.py
─────────────────────
Run EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON) for every query in the
JOB benchmark file and save the resulting JSON plans to disk.

Query-boundary detection
────────────────────────
Each query in the file starts with a line of the form:

    N.SELECT ...

where N is a positive integer (1 … 113).  The regex  r'(?m)^\d+\.(?=SELECT)'
is used to split the raw text at those boundaries.  The leading "N." prefix is
stripped from each chunk before the SQL is stored or executed.
Blank lines (including the occasional double blank) between queries are handled
naturally by strip().
"""

import argparse
import json
import logging
import re
import sys
import time
from pathlib import Path

import psycopg2

# ──────────────────────────────────────────────────────────────────────────────
# Logging
# ──────────────────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────────────
# Query extraction
# ──────────────────────────────────────────────────────────────────────────────
# The pattern matches the start of a new query: one-or-more digits, a literal
# dot, then SELECT (the lookahead keeps SELECT in the captured text).
_QUERY_BOUNDARY = re.compile(r"(?m)^\d+\.(?=SELECT)")


def extract_queries(text: str) -> list[str]:
    """
    Split *text* on the ``N.SELECT`` boundary pattern and return a list of
    clean SQL strings (no leading label, no trailing semicolons or whitespace).

    The function:
    1. Splits on every occurrence of  ^\d+\.  at the start of a line.
    2. Discards the empty string that precedes the very first delimiter.
    3. Strips each chunk and removes a trailing semicolon if present.
    """
    parts = _QUERY_BOUNDARY.split(text)

    queries: list[str] = []
    for part in parts:
        sql = part.strip()
        if not sql:
            continue
        # Remove trailing semicolon (possibly preceded by whitespace)
        sql = re.sub(r";\s*$", "", sql, flags=re.MULTILINE).rstrip()
        queries.append(sql)

    return queries


# ──────────────────────────────────────────────────────────────────────────────
# Argument parsing
# ──────────────────────────────────────────────────────────────────────────────
def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run EXPLAIN ANALYZE on every JOB benchmark query."
    )
    parser.add_argument(
        "--query-file",
        required=True,
        help="Path to the text file containing all JOB SQL queries.",
    )
    parser.add_argument("--dbname",   required=True, help="PostgreSQL database name.")
    parser.add_argument("--user",     required=True, help="PostgreSQL user.")
    parser.add_argument("--password", required=True, help="PostgreSQL password.")
    parser.add_argument("--host",     default="localhost", help="PostgreSQL host (default: localhost).")
    parser.add_argument("--port",     type=int, default=5432, help="PostgreSQL port (default: 5432).")
    parser.add_argument(
        "--output-dir",
        default="explain_results",
        help="Directory for per-query JSON plan files (default: explain_results).",
    )
    parser.add_argument(
        "--failures-file",
        default="failures.json",
        help="JSON file to collect failed queries (default: failures.json).",
    )
    return parser.parse_args()


# ──────────────────────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────────────────────
def main() -> None:
    args = parse_args()

    # ── Read and parse query file ────────────────────────────────────────────
    query_path = Path(args.query_file)
    if not query_path.exists():
        log.error("Query file not found: %s", query_path)
        sys.exit(1)

    raw_text = query_path.read_text(encoding="utf-8")
    queries = extract_queries(raw_text)

    if not queries:
        log.error("No queries were extracted from %s – check the file format.", query_path)
        sys.exit(1)

    total = len(queries)
    log.info("Extracted %d queries from %s", total, query_path)

    # ── Prepare output directory ─────────────────────────────────────────────
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # ── Connect to PostgreSQL ────────────────────────────────────────────────
    log.info(
        "Connecting to PostgreSQL: host=%s  port=%s  dbname=%s  user=%s",
        args.host, args.port, args.dbname, args.user,
    )
    try:
        conn = psycopg2.connect(
            dbname=args.dbname,
            user=args.user,
            password=args.password,
            host=args.host,
            port=args.port,
            # No connect_timeout: some JOB queries take many minutes.
            options="-c statement_timeout=0",
        )
        # Autocommit so a failed query does NOT abort the transaction and
        # leave subsequent queries unable to execute.
        conn.autocommit = True
    except psycopg2.Error as exc:
        log.error("Could not connect to PostgreSQL: %s", exc)
        sys.exit(1)

    cursor = conn.cursor()

    # Belt-and-suspenders: also issue the SET via SQL in case the connection
    # option was ignored by an older psycopg2 / server combination.
    cursor.execute("SET statement_timeout = 0;")
    log.info("statement_timeout set to 0 (unlimited).")

    # ── Process queries ──────────────────────────────────────────────────────
    successes: int = 0
    failures: list[dict] = []
    wall_start = time.perf_counter()

    for idx, sql in enumerate(queries, start=1):
        query_id = idx
        out_file = output_dir / f"query_{query_id:03d}.json"

        explain_sql = (
            "EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON)\n" + sql
        )

        t0 = time.perf_counter()
        try:
            cursor.execute(explain_sql)
            rows = cursor.fetchall()
            elapsed = time.perf_counter() - t0

            # psycopg2 returns the JSON plan as a Python list already parsed
            # from PostgreSQL's JSON output; rows[0][0] is that list.
            plan = rows[0][0]

            result = {
                "query_id":        query_id,
                "status":          "ok",
                "elapsed_seconds": round(elapsed, 6),
                "sql":             sql,
                "plan":            plan,
            }

            out_file.write_text(
                json.dumps(result, indent=2, ensure_ascii=False),
                encoding="utf-8",
            )

            successes += 1
            print(f"[{query_id}/{total}] Query {query_id} OK ({elapsed:.2f}s)")
            log.info("Query %d saved → %s", query_id, out_file)

        except Exception as exc:          # noqa: BLE001
            elapsed = time.perf_counter() - t0
            error_msg = str(exc).strip()

            failures.append(
                {
                    "query_id":    query_id,
                    "error_message": error_msg,
                    "original_sql": sql,
                }
            )

            print(
                f"[{query_id}/{total}] Query {query_id} FAILED "
                f"({elapsed:.2f}s): {error_msg[:120]}"
            )
            log.warning("Query %d failed: %s", query_id, error_msg)

            # Write a failure stub so the output directory reflects every query
            result = {
                "query_id":        query_id,
                "status":          "error",
                "elapsed_seconds": round(elapsed, 6),
                "sql":             sql,
                "error":           error_msg,
            }
            out_file.write_text(
                json.dumps(result, indent=2, ensure_ascii=False),
                encoding="utf-8",
            )

    # ── Save failures ────────────────────────────────────────────────────────
    failures_path = Path(args.failures_file)
    failures_path.write_text(
        json.dumps(failures, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
    if failures:
        log.info("%d failure(s) written to %s", len(failures), failures_path)

    # ── Cleanup ──────────────────────────────────────────────────────────────
    cursor.close()
    conn.close()

    total_elapsed = time.perf_counter() - wall_start
    h, rem   = divmod(int(total_elapsed), 3600)
    m, s     = divmod(rem, 60)
    time_str = f"{h}h {m}m {s}s" if h else (f"{m}m {s}s" if m else f"{s}s")

    # ── Final summary ────────────────────────────────────────────────────────
    print()
    print("=" * 52)
    print(f"Total Queries : {total}")
    print(f"Successful    : {successes}")
    print(f"Failed        : {len(failures)}")
    print(f"Total Runtime : {time_str}  ({total_elapsed:.2f}s)")
    print("=" * 52)


if __name__ == "__main__":
    main()

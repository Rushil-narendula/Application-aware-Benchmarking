#!/usr/bin/env python3
"""
run_original_explains.py
------------------------
Reads SQL queries from a plain-text file, executes EXPLAIN on each one
against a PostgreSQL database, and writes per-query JSON result files
plus a consolidated error log.
"""

import json
import logging
import os
import re
import sys
import time
import traceback
from datetime import datetime, timezone

import psycopg2
from psycopg2 import OperationalError, ProgrammingError

# ─────────────────────────────────────────────
#  Configuration  ─  edit these values only
# ─────────────────────────────────────────────
TXT_FILE   = "original_.txt"
HOST       = "localhost"
PORT       = 5432
DATABASE   = "movie_db"
USER       = "postgres"
PASSWORD   = ""
OUTPUT_DIR = "execution_plans_original"
# ─────────────────────────────────────────────


# ── Regex that matches the leading "N." prefix (e.g. "1.", "12.", "113.")
_QUERY_PREFIX_RE = re.compile(r"^\s*\d+\.\s*")


def setup_error_logger(log_path: str) -> logging.Logger:
    """Configure and return a file-based logger for query errors."""
    logger = logging.getLogger("query_errors")
    logger.setLevel(logging.ERROR)
    handler = logging.FileHandler(log_path, mode="w", encoding="utf-8")
    handler.setFormatter(
        logging.Formatter("%(asctime)s  %(message)s", datefmt="%Y-%m-%dT%H:%M:%S%z")
    )
    logger.addHandler(handler)
    return logger


def load_queries(txt_file: str) -> list[str]:
    """
    Read *txt_file*, split on semicolons, strip the leading 'N.' prefix,
    and return a list of non-empty SQL strings in document order.
    """
    with open(txt_file, "r", encoding="utf-8") as fh:
        raw = fh.read()

    # Split on semicolons; each chunk is one candidate query
    chunks = raw.split(";")

    queries: list[str] = []
    for chunk in chunks:
        # Strip the leading numeric prefix (e.g. "107.") that appears in this file
        cleaned = _QUERY_PREFIX_RE.sub("", chunk, count=1)
        cleaned = cleaned.strip()
        if cleaned:
            queries.append(cleaned)

    return queries


def connect(host: str, port: int, database: str, user: str, password: str):
    """Open and return a psycopg2 connection."""
    return psycopg2.connect(
        host=host,
        port=port,
        dbname=database,
        user=user,
        password=password,
    )


def explain_query(cursor, sql: str) -> list[str]:
    """
    Execute ``EXPLAIN <sql>`` and return the plan lines as a list of strings.
    Raises psycopg2 exceptions on failure (caller handles them).
    """
    explain_sql = f"EXPLAIN {sql}"
    cursor.execute(explain_sql)
    rows = cursor.fetchall()
    return [row[0] for row in rows]


def write_success(
    output_dir: str,
    query_number: int,
    original_query: str,
    plan_lines: list[str],
) -> None:
    """Persist a successful EXPLAIN result to its JSON file."""
    executed_query = f"EXPLAIN {original_query}"
    payload = {
        "query_number": query_number,
        "status": "success",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "original_query": original_query,
        "executed_query": executed_query,
        "execution_plan": plan_lines,
    }
    _write_json(output_dir, query_number, payload)


def write_failure(
    output_dir: str,
    query_number: int,
    original_query: str,
    error_message: str,
) -> None:
    """Persist a failed EXPLAIN result to its JSON file."""
    executed_query = f"EXPLAIN {original_query}"
    payload = {
        "query_number": query_number,
        "status": "failed",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "original_query": original_query,
        "executed_query": executed_query,
        "error": error_message,
    }
    _write_json(output_dir, query_number, payload)


def _write_json(output_dir: str, query_number: int, payload: dict) -> None:
    filename = f"query_{query_number:03d}_explain.json"
    filepath = os.path.join(output_dir, filename)
    with open(filepath, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, indent=2, ensure_ascii=False)


def process_queries(
    queries: list[str],
    conn,
    output_dir: str,
    error_logger: logging.Logger,
) -> tuple[int, int]:
    """
    Iterate over *queries*, run EXPLAIN for each, and write output files.

    Returns
    -------
    (successes, failures)
    """
    total = len(queries)
    successes = 0
    failures = 0

    for idx, sql in enumerate(queries, start=1):
        print(f"[{idx}/{total}] Running Query {idx}...")

        try:
            # Each query gets its own transaction so a failure doesn't
            # poison the connection for subsequent queries.
            with conn.cursor() as cur:
                plan_lines = explain_query(cur, sql)
            conn.rollback()          # clean up any implicit transaction state

            write_success(output_dir, idx, sql, plan_lines)
            successes += 1
            print("  ✓ Success")

        except (ProgrammingError, Exception) as exc:  # noqa: BLE001
            conn.rollback()          # reset connection after error

            error_msg = str(exc)
            tb = traceback.format_exc()
            ts = datetime.now(timezone.utc).isoformat()

            # Log to errors.log
            error_logger.error(
                "Query %d | %s | %s\n%s",
                idx, ts, error_msg, tb,
            )

            write_failure(output_dir, idx, sql, error_msg)
            failures += 1
            print(f"  ✗ Failed: {error_msg.splitlines()[0]}")

    return successes, failures


def main() -> None:
    wall_start = time.perf_counter()

    # ── 1. Prepare output directory and error log ────────────────────────
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    log_path = os.path.join(OUTPUT_DIR, "errors.log")
    error_logger = setup_error_logger(log_path)

    # ── 2. Parse queries from file ───────────────────────────────────────
    if not os.path.isfile(TXT_FILE):
        print(f"ERROR: Input file not found: {TXT_FILE}", file=sys.stderr)
        sys.exit(1)

    queries = load_queries(TXT_FILE)
    if not queries:
        print("No queries found in the input file. Exiting.")
        sys.exit(0)

    print(f"Loaded {len(queries)} queries from '{TXT_FILE}'")
    print(f"Output directory: {OUTPUT_DIR}\n")

    # ── 3. Connect to PostgreSQL ─────────────────────────────────────────
    try:
        conn = connect(HOST, PORT, DATABASE, USER, PASSWORD)
    except OperationalError as exc:
        print(f"ERROR: Could not connect to PostgreSQL: {exc}", file=sys.stderr)
        sys.exit(1)

    # ── 4. Process every query ───────────────────────────────────────────
    try:
        successes, failures = process_queries(queries, conn, OUTPUT_DIR, error_logger)
    finally:
        conn.close()

    # ── 5. Summary ───────────────────────────────────────────────────────
    elapsed = time.perf_counter() - wall_start
    total = successes + failures

    print("\n" + "─" * 50)
    print(f"  Total queries processed : {total}")
    print(f"  Successful executions   : {successes}")
    print(f"  Failed executions       : {failures}")
    print(f"  Total runtime           : {elapsed:.2f}s")
    print("─" * 50)

    if failures:
        print(f"\nSee '{log_path}' for detailed error traces.")


if __name__ == "__main__":
    main()

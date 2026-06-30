#!/usr/bin/env python3
"""
run_explains.py
---------------
Extracts all SQL queries from the RAW JavaScript object embedded in a
SQLBarber HTML file, runs EXPLAIN (not EXPLAIN ANALYZE) on each one against
a PostgreSQL database, and saves the results as structured JSON files.

Usage:
    python run_explains.py [path/to/file.html]

If no path is given, HTML_FILE (below) is used as the default.
"""

# ─── Standard library ────────────────────────────────────────────────────────
import json
import logging
import os
import re
import sys
import time
import traceback
from datetime import datetime, timezone
from pathlib import Path

# ─── Third-party ─────────────────────────────────────────────────────────────
try:
    import psycopg2
except ImportError:
    sys.exit(
        "psycopg2 is not installed.  Run:  pip install psycopg2-binary"
    )

# ═══════════════════════════════════════════════════════════════════════════════
# CONFIGURATION  –  edit these before running
# ═══════════════════════════════════════════════════════════════════════════════
HTML_FILE  = "Updated_queries_all_with_advanced_features.html"   # default input
HOST       = "localhost"
PORT       = 5432
DATABASE   = "movie_db"          # replace with your actual database name
USER       = "postgres"      # replace with your actual username
PASSWORD   = "Rushil@2007"              # replace with your actual password
OUTPUT_DIR = "execution_plans"
# ═══════════════════════════════════════════════════════════════════════════════


# ─── Logging setup ───────────────────────────────────────────────────────────

def setup_logging(output_dir: Path) -> logging.Logger:
    """
    Configure a logger that writes ERROR-level entries (with stack traces)
    to  <output_dir>/errors.log  and INFO-level progress to stdout.
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    logger = logging.getLogger("run_explains")
    logger.setLevel(logging.DEBUG)

    # File handler – errors only
    fh = logging.FileHandler(output_dir / "errors.log", encoding="utf-8")
    fh.setLevel(logging.ERROR)
    fh.setFormatter(
        logging.Formatter(
            fmt="[%(asctime)s]  Query %(query_num)s  %(levelname)s\n%(message)s\n",
            datefmt="%Y-%m-%dT%H:%M:%SZ",
        )
    )
    logger.addHandler(fh)

    return logger


# ─── HTML / JS extraction ────────────────────────────────────────────────────

def extract_raw_object_text(html_content: str) -> str:
    """
    Locate the JavaScript object literal assigned to  const RAW = { … }
    in the HTML source and return its text (the { … } portion only).

    The regex is anchored at  const RAW =  and captures everything up to the
    matching closing brace, accounting for nested braces via a linear scan.
    """
    # Find the start of the object literal
    match = re.search(r"\bconst\s+RAW\s*=\s*(\{)", html_content, re.DOTALL)
    if not match:
        raise ValueError("Could not find 'const RAW = {' in the HTML file.")

    start = match.start(1)   # position of the opening '{'
    depth = 0
    for idx in range(start, len(html_content)):
        ch = html_content[idx]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return html_content[start : idx + 1]   # inclusive of final '}'

    raise ValueError("RAW object in HTML is not properly closed (unbalanced braces).")


def parse_queries_json(raw_text: str) -> dict[int, str]:
    """
    Primary parser: treat the extracted JS object literal as JSON and decode it
    directly.  JS object literals with string keys and string values are valid
    JSON, so this usually works without modification.

    Returns a dict mapping query_number (int) → SQL string.
    """
    data = json.loads(raw_text)
    return {int(k): v for k, v in data.items()}


def parse_queries_regex(raw_text: str) -> dict[int, str]:
    """
    Fallback parser: use a regex to extract individual  "key": "value"  pairs
    from the raw JS object text.  This handles edge cases where json.loads
    might choke (e.g. trailing commas, JS-style comments).

    The value is a JSON-encoded string so we use json.loads on each match to
    correctly unescape  \\n, \\t, \\'  etc.
    """
    # Match:  "digits" : "... escaped string ..."
    pattern = re.compile(
        r'"(\d+)"\s*:\s*("(?:[^"\\]|\\.)*")',
        re.DOTALL,
    )
    queries: dict[int, str] = {}
    for m in pattern.finditer(raw_text):
        num = int(m.group(1))
        sql = json.loads(m.group(2))   # decode JSON string escapes
        queries[num] = sql
    if not queries:
        raise ValueError("Regex fallback also found zero queries.")
    return queries


def load_queries(html_path: Path) -> dict[int, str]:
    """
    Read the HTML file, extract the RAW JS object, and return an ordered dict
    of  query_number → SQL string.  Tries JSON parsing first; falls back to
    regex extraction if that fails.
    """
    html_content = html_path.read_text(encoding="utf-8")
    raw_text = extract_raw_object_text(html_content)

    try:
        queries = parse_queries_json(raw_text)
        print(f"  Parsed {len(queries)} queries via JSON parser.")
    except (json.JSONDecodeError, ValueError) as exc:
        print(f"  JSON parse failed ({exc}); switching to regex fallback …")
        queries = parse_queries_regex(raw_text)
        print(f"  Parsed {len(queries)} queries via regex fallback.")

    return dict(sorted(queries.items()))   # ascending numeric order


# ─── SQL helpers ─────────────────────────────────────────────────────────────

def build_explain_query(sql: str) -> str:
    """
    Strip surrounding whitespace and trailing semicolons from the original SQL,
    then prepend  EXPLAIN.  This avoids syntax errors such as
    EXPLAIN SELECT …;   (the semicolon terminates the statement prematurely).
    """
    cleaned = sql.strip().rstrip(";").strip()
    return f"EXPLAIN {cleaned}"


# ─── Database connection ──────────────────────────────────────────────────────

def connect() -> "psycopg2.connection":
    """
    Open a single PostgreSQL connection using the module-level credentials.
    Raises psycopg2.OperationalError if the server is unreachable.
    """
    return psycopg2.connect(
        host=HOST,
        port=PORT,
        dbname=DATABASE,
        user=USER,
        password=PASSWORD,
    )


# ─── JSON output helpers ──────────────────────────────────────────────────────

def utc_now_iso() -> str:
    """Return the current UTC time as an ISO-8601 string (Z suffix)."""
    return datetime.now(tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def write_success(
    output_dir: Path,
    query_num: int,
    original_sql: str,
    executed_sql: str,
    plan_rows: list[str],
) -> None:
    """Persist a successful EXPLAIN result as a zero-padded JSON file."""
    payload = {
        "query_number": query_num,
        "status": "success",
        "timestamp": utc_now_iso(),
        "original_query": original_sql,
        "executed_query": executed_sql,
        "execution_plan": plan_rows,
    }
    path = output_dir / f"query_{query_num:03d}_explain.json"
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def write_failure(
    output_dir: Path,
    query_num: int,
    original_sql: str,
    executed_sql: str,
    error_msg: str,
) -> None:
    """Persist a failure record as a zero-padded JSON file."""
    payload = {
        "query_number": query_num,
        "status": "failed",
        "timestamp": utc_now_iso(),
        "original_query": original_sql,
        "executed_query": executed_sql,
        "error": error_msg,
    }
    path = output_dir / f"query_{query_num:03d}_explain.json"
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


# ─── Core execution loop ─────────────────────────────────────────────────────

def run_explains(
    queries: dict[int, str],
    conn: "psycopg2.connection",
    output_dir: Path,
    logger: logging.Logger,
) -> tuple[int, int]:
    """
    Iterate over all queries in ascending order, run EXPLAIN on each, and
    write results.  Errors are caught per-query so processing always continues.

    Returns (success_count, failure_count).
    """
    total      = len(queries)
    successes  = 0
    failures   = 0

    for query_num, original_sql in queries.items():
        executed_sql = build_explain_query(original_sql)

        print(f"\n[{query_num}/{total}] Running Query {query_num}...")

        # Each query gets its own cursor so a failed transaction can be rolled
        # back without killing the whole connection.
        try:
            with conn.cursor() as cur:
                cur.execute(executed_sql)
                rows = cur.fetchall()
                # PostgreSQL returns one row per plan line; extract the text.
                plan_lines = [row[0] for row in rows]

            write_success(output_dir, query_num, original_sql, executed_sql, plan_lines)
            print("  ✓ Success")
            successes += 1

        except Exception as exc:  # noqa: BLE001 – intentionally broad
            # Roll back the failed transaction so the connection stays usable.
            try:
                conn.rollback()
            except Exception:
                pass   # connection may be in a bad state; handled below

            short_msg = str(exc).strip()
            full_trace = traceback.format_exc()

            print(f"  ✗ Failed: {short_msg}")

            # Log detailed error information to errors.log
            logger.error(
                f"Error executing query {query_num}:\n{short_msg}\n\n{full_trace}",
                extra={"query_num": str(query_num)},
            )

            write_failure(output_dir, query_num, original_sql, executed_sql, short_msg)
            failures += 1

    return successes, failures


# ─── Entry point ─────────────────────────────────────────────────────────────

def main() -> None:
    # ── Resolve the HTML input path ──────────────────────────────────────────
    if len(sys.argv) > 1:
        html_path = Path(sys.argv[1])
    else:
        html_path = Path(HTML_FILE)

    if not html_path.exists():
        sys.exit(f"Error: HTML file not found: {html_path}")

    output_dir = Path(OUTPUT_DIR)
    logger = setup_logging(output_dir)

    print("=" * 60)
    print("  SQLBarber EXPLAIN Runner")
    print("=" * 60)
    print(f"  Input  : {html_path}")
    print(f"  Output : {output_dir}/")
    print(f"  DB     : {USER}@{HOST}:{PORT}/{DATABASE}")
    print("=" * 60)

    # ── Extract queries from the HTML ────────────────────────────────────────
    print("\nExtracting queries from HTML …")
    queries = load_queries(html_path)
    total = len(queries)
    print(f"  Found {total} queries.\n")

    # ── Open a single database connection ────────────────────────────────────
    print("Connecting to PostgreSQL …")
    try:
        conn = connect()
        print("  Connection established.\n")
    except psycopg2.OperationalError as exc:
        sys.exit(f"Cannot connect to the database:\n  {exc}")

    # ── Run all EXPLAIN statements ───────────────────────────────────────────
    wall_start = time.perf_counter()

    try:
        successes, failures = run_explains(queries, conn, output_dir, logger)
    finally:
        # Always close the connection, even if an unexpected error bubbles up.
        try:
            conn.close()
        except Exception:
            pass

    elapsed = time.perf_counter() - wall_start

    # ── Summary ──────────────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    print("  Summary")
    print("=" * 60)
    print(f"  Total queries processed : {total}")
    print(f"  Successful              : {successes}")
    print(f"  Failed                  : {failures}")
    print(f"  Elapsed time            : {elapsed:.2f}s")
    print(f"  Results written to      : {output_dir}/")
    if failures:
        print(f"  Error details           : {output_dir}/errors.log")
    print("=" * 60)


if __name__ == "__main__":
    main()

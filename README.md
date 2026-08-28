# Application-Aware Benchmarking: SQLBarber Replication & Structural Fidelity Analysis

Research internship project at **IIT Hyderabad**, replicating and critically evaluating **SQLBarber** — an LLM-driven SQL workload synthesis system ([Lao & Trummer, SIGMOD 2026](https://doi.org/10.1145/3786699)) — on the Join Order Benchmark (JOB/IMDB).

**Live dashboards:** [→ view all interactive results](https://Rushil-narendula.github.io/Application-aware-Benchmarking/)
*(HTML dashboards render directly in-browser via GitHub Pages — no need to download.)*

---

## Core Question

SQLBarber matches a *target execution-cost distribution* using Bayesian Optimization over predicate values. This replication asks: **does matching a cost distribution guarantee the generated queries are structurally or semantically realistic** — or can very different queries land in the same cost bin?

## What I Did

1. **Replicated SQLBarber's pipeline** on JOB/IMDB, generating 113 synthetic queries across 33 base SQL templates.
2. **Profiled cost drift**: found 81.8% (27/33) of templates drifted into unintended cost bins during instantiation — a structural fidelity gap the paper's headline metric doesn't capture.
3. **Built a Python/psycopg2 pipeline** to extract, parse, and diff PostgreSQL `EXPLAIN`/`EXPLAIN ANALYZE` execution plans at scale (`src/explain_job_queries.py`, `src/run_original_explains.py`, `src/synthetic_explain_only.py`).
4. **Designed the Query Relative Ratio (QRR)** metric — `max(original/synthetic, synthetic/original)` — to quantify per-query cost fidelity independent of the aggregate distribution. Found individual outliers (e.g. Query #39) with >2,400% cost error despite the *overall* distribution matching the ground truth exactly.
5. **Extended plan-comparison beyond cost matching**: adapted ideas from the [Picasso query-plan visualizer](https://dsl.cds.iisc.ac.in/projects/PICASSO/) (IISc) to directly compare *query-plan structure*, not just cost. `src/plan_comparator_v5.py` / `v6.py` implement a **Zhang-Shasha tree-edit-distance** score plus a **multiset-Jaccard operator similarity** score between original and synthetic plan trees — parsing both JSON-formatted and plain-text `EXPLAIN` output into a canonicalized tree representation for comparison.
6. **Applied Bayesian Optimization** (Random Forest and Gaussian Process surrogates) to iteratively refine workload-generation parameters across 19 templates, improving post-refinement cost-distribution alignment.

## Key Finding

A workload can match a target cost-distribution histogram *exactly* while still containing queries whose individual costs, and whose plan structures, differ substantially from the real workload. Distribution-matching alone is **not sufficient evidence of realistic SQL semantics**.

---

## Repository Structure

```
├── src/                       # Pipeline scripts
│   ├── explain_job_queries.py         # EXPLAIN extraction over the JOB benchmark
│   ├── run_original_explains.py       # Runs EXPLAIN ANALYZE on original workload
│   ├── synthetic_explain_only.py      # EXPLAIN (no execution) on synthetic queries
│   ├── synthetic_explains_analyze.py  # EXPLAIN ANALYZE on synthetic queries
│   ├── plan_comparator_v5.py          # Plan structural comparison (JSON EXPLAIN input)
│   └── plan_comparator_v6.py          # Plan structural comparison (plain-text EXPLAIN input)
│
├── docs/                      # Interactive HTML dashboards (served via GitHub Pages)
│   └── index.html                     # Dashboard index / landing page
│
├── data/                      # Raw outputs and generated queries
│   ├── explain_analyze_original/      # EXPLAIN ANALYZE, original workload (113 queries)
│   ├── explain_analyze_synthetic/     # EXPLAIN ANALYZE, synthetic workload (113 queries)
│   ├── only_explain_original/         # EXPLAIN only, original workload
│   ├── only_explain_synthetic/        # EXPLAIN only, synthetic workload
│   ├── synthetic_queries/             # Generated SQL (113 .sql files)
│   └── *.json / *.csv / *.txt         # Normalized plans, cost tables, plan trees
│
├── charts/                    # Static chart images referenced by the dashboards
│
└── reference/picasso-tool/    # Attribution: comparator.py from the original Picasso
                                # tool (IISc), which src/plan_comparator_v5/v6.py extend
```

## Tech Stack

Python, `psycopg2`, PostgreSQL `EXPLAIN`/`EXPLAIN ANALYZE`, Bayesian Optimization (Random Forest & Gaussian Process surrogates), Latin Hypercube Sampling, Zhang-Shasha tree edit distance, Chart.js (dashboards).

## Notes

- The `imdb/` dataset directory is git-ignored due to size.
- Dashboards use Chart.js for interactivity — view them via the [GitHub Pages link](https://Rushil-narendula.github.io/Application-aware-Benchmarking/) rather than downloading, or open the `.html` files directly in a browser if working offline.

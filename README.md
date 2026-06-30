# Application-Aware Benchmarking: SQLBarber Analysis

## Overview
This repository contains my internship project at IIT Hyderabad focusing on Application-aware benchmarking. The core of this work involves replicating and analyzing the methodology from the research paper **SQLBarber: A System Leveraging Large Language Models to Generate Customized and Realistic SQL Workloads** (Jiale Lao and Immanuel Trummer). 

My primary goal was to critically evaluate a core assumption: *Does matching a target cost distribution guarantee that the generated queries are structurally realistic or functionally similar to the original workload?*

By utilizing the Join Order Benchmark (JOB) and the IMDB dataset, I followed the paper's methodology to synthetically generate 113 queries. My analysis demonstrates that cost-distribution matching alone is insufficient to claim realistic SQL semantics.

## SQLBarber – Comprehensive Summary

### 1. Motivation
Benchmarking database systems requires large collections of SQL queries. However:
- Real production SQL logs are private and cannot usually be shared.
- Existing generators (e.g., random query generators) create diverse queries but not realistic ones.
- Existing cost-aware generators can target execution costs but cannot generate customized SQL templates.
- Existing workload synthesis methods often reuse benchmark queries instead of creating new ones.

The paper aims to solve this by generating synthetic SQL workloads that resemble production workloads while allowing user customization.

### 2. Main Idea
SQLBarber combines Large Language Models (LLMs) with profiling and Bayesian Optimization to generate SQL workloads.
Users specify:
- SQL structure requirements (joins, aggregations, nested queries, etc.)
- Desired cost distribution (execution cost, cardinality, runtime, etc.)

SQLBarber then generates SQL templates and instantiates them into executable queries whose costs approximately match the requested distribution.

### 3. Key Contributions
- LLM-based generation of customized SQL templates from natural language.
- Self-correction mechanism to repair invalid SQL templates.
- Cost-aware query generation using profiling, refinement, pruning, and Bayesian Optimization.
- Benchmarks derived from real workload statistics from industry datasets.
- Experimental evaluation showing faster generation and better distribution matching than prior methods.

### 4. Overall Architecture
SQLBarber has two major components:
**A. Customized SQL Template Generator:**
- Reads database schema and extracts possible join paths.
- Builds prompts using schema + user instructions.
- Uses an LLM to generate SQL templates.
- Verifies and iteratively rewrites to fix semantic or syntax errors.

**B. Cost-Aware Query Generator:**
- Samples predicate values.
- Profiles resulting query costs.
- Refines templates to cover missing cost ranges.
- Prunes ineffective templates and uses Bayesian Optimization to search predicate values efficiently.

### 5. Template Generation Process
Gathers metadata (table sizes, data types, distinct values, keys, index info) and valid join paths. The LLM receives database context, join path, and user specifications to generate SQL templates with placeholders.

### 6. Self-Correction Mechanism
Repeatedly checks if the template satisfies constraints and uses the DBMS feedback to repair syntax errors until the template is semantically and syntactically valid.

### 7. Profiling Stage
Estimates cost ranges for each template using Latin Hypercube Sampling (LHS) for broad coverage of predicate combinations, recording metrics like estimated plan cost, cardinality, and runtime.

### 8. Template Refinement
Identifies gaps in the target distribution. Promising templates are modified by the LLM (different joins, filters) and profiled again to improve coverage. Ineffective templates are pruned.

### 9. Bayesian Optimization (BO)
Finds predicate values that land in desired cost bins. It identifies intervals with the largest deficit, selects promising templates, and applies BO to search predicate values.

### 10. Benchmarks and Evaluation
Evaluated using TPC-H and IMDB. The authors construct benchmark workloads with uniform, normal, and production-derived distributions (Amazon Redshift/Snowflake) and compare with HillClimbing, LearnedSQLGen, and SQLStorm.

### 11. Reported Results
- Generates templates directly from natural-language constraints.
- Cost distributions align closely with targets.
- Reduces query generation time by 1-2 orders of magnitude.
- Supports arbitrary user-defined cost metrics.

### 12. Strengths
- Natural-language specification.
- Automatic correction of invalid SQL.
- Flexible targeting of cost distributions.
- Efficient predicate search using Bayesian Optimization.

### 13. Potential Limitations & My Hypothesis
- Cost-distribution matching does not necessarily imply realistic SQL semantics. A workload can reproduce execution-cost histograms while still containing queries that differ substantially from those seen in production.
- The refinement process is driven largely by optimizer costs and profiling rather than by semantic similarity to real user workloads.
- Matching distributions alone may overlook other important characteristics, including join patterns, predicate correlations, temporal locality, or business logic.

## Replication Analytics & Dashboard Results
In my replication using the Join Order Benchmark (JOB) and the IMDB database, I synthetically generated 113 queries. To analyze the pipeline's effectiveness, I developed a series of interactive dashboards that break down the workload generation process step-by-step. 

The results from each stage provide critical insights into the limitations of distribution-matching workload generators:

### 1. Template Profiling & Shift Metrics (`sqlbarber_dashboard_v2.html`)
The first step of the pipeline generated 33 base SQL templates. Analyzing their raw execution costs revealed severe inconsistencies:
- **Massive Distribution Drift:** A staggering **81.8% (27 of 33)** of the generated templates drifted into completely different cost bins than intended during instantiation.
- **Perfect Matches:** Only **9.1% (3 templates)** aligned perfectly with their targeted cost bins.
- **Out of Bounds (OOB) Anomalies:** Another **9.1% (3 templates)** generated execution costs entirely outside the defined measurement window (normalized cost range of 0–10,000).
- **Verdict:** The initial instantiation exhibits **low structural fidelity**. The sheer variance in parameter sampling proves that without aggressive refinement, base templates cannot reliably populate a targeted distribution histogram.

### 2. Initial Profiling Stage Distribution (`Final_updated_profiling.html`)
When mapping the initially instantiated templates against the ground-truth JOB distribution (10 bins of 1,000 cost width), major structural gaps were exposed:
- **Over-Concentration:** The LLM heavily favored specific structures. Bins 0 (0–1k) and 5 (5–6k) were massively overrepresented in the synthetic output (e.g., 42 synthetic queries vs 29 ground-truth in Bin 5).
- **Under-Representation:** Mid-to-high cost queries were difficult to generate organically. Bins 3 and 8 had severe deficits.
- **Missing Intervals:** Bin 9 (9k–10k) was entirely empty in the synthetic run, despite the ground truth requiring 10 high-cost queries.
- **Discarded Templates:** Templates 3, 10, and 29 were fully discarded at this stage due to producing out-of-range cost values (cost underflows/overflows).

### 3. Post-Refinement Calibration & Remediation (`Updated_refinement.html`)
To fix the broken initial distribution, the pipeline applied aggressive structural and quota-based refinements. This stage successfully forced the synthetic workload to match the ground truth, but required heavy manual/automated intervention:
- **Calibration Status:** PASSED. All 10 cost bins were forcibly realigned to perfectly match the ground-truth counts (e.g., the 3k–4k peak was nailed at exactly 27 queries).
- **Templates Modified:** **19 of the 33** templates required active modifications (structural rewriting or quota shifting) to achieve this match.
- **OOB Rescues:** The 3 out-of-bounds templates were successfully rescued and reassigned to anchor high/low bins.
- **Pivotal Fix (Template 17 Spotlight):** T17 was initially generating unwanted noise in the bloated 0–1k bin. It was structurally modified to boost its optimizer cost, successfully rerouting it to completely fill the 2k–3k gap with 9 targeted queries.
- **Throttling:** Over-concentration in the 5k–6k bin was resolved by heavily throttling the query quotas of T7, T11, T13, and T15.

### 4. Final Query Comparison & Cost Outliers (`updated_final_query_comparision.html`)
Even after the macroscopic distribution perfectly matched the ground truth, comparing the individual synthetic queries against the original workload revealed massive per-query deviations:
- **Extreme Outliers:** While broad distributions aligned, individual query variance was wild. One outlier (**Query #39**) exhibited a cost prediction error of **2,429%**.
- **Variance Across Cost Buckets:** Queries with original costs over 700K tended to have lower percentage errors (typically 6–35%), suggesting synthetic estimation is somewhat stable at extreme high-end complexities. However, at lower costs, small absolute errors resulted in massive percentage errors (often exceeding 200%).
- **Conclusion:** A matched histogram obscures massive individual query inaccuracies.

### 5. Final Synthetic Workload Repository (`Updated_queries_all_with_advanced_features.html`)
The pipeline successfully output a repository of 113 executable, synthetically generated benchmark queries:
- The queries successfully execute against the IMDB schema.
- They span varied complexity tiers (Low, Medium, High, Very High) and include advanced structural features like deep JOINs, subqueries, `GROUP BY` aggregations, and `ORDER BY` clauses.

### 6. Query Relative Ratio (QRR) Fidelity Analysis (`sqlbarber_qrr_report.html`)
To systematically quantify the per-query deviation between original and synthetic costs, I developed a **Query Relative Ratio (QRR)** fidelity dashboard.
- **The QRR Metric:** Defined as `max(original / synthetic, synthetic / original)`, ensuring the value is always ≥ 1. A QRR of 1.0 represents a perfect match.
- **Fidelity Spread:** Visualizations like the Cumulative Distribution Function (CDF) and Box Plots show the spread of cost agreement. While some queries achieve near-perfect fidelity (QRR ≈ 1.0), others experience wild deviations.
- **Worst Offenders:** The dashboard automatically isolates the worst-performing queries, specifically highlighting massive outliers like Query #39, which has a QRR exceeding 25 (a ~2,429% error).
- **Core Insight:** The QRR analysis visually proves that macroscopic success (matching a histogram) hides microscopic failure (high QRR variance on a per-query basis).

### 7. Execution Plan Logging & Comparison
To enable granular inspection of query execution behavior, the repository now tracks the detailed PostgreSQL `EXPLAIN` and `EXPLAIN ANALYZE` logs for both workloads. 
- Located in `explain_analyze_original/`, `explain_analyze_synthetic/`, `only_explain_original/`, and `only_explain_synthetic/`.
- This permits side-by-side verification of how the query planner interpreted the original versus synthetic structures.
- The Python scripts used to automate and generate these logs are included: `explain_job_queries.py`, `run_original_explains.py`, `synthetic_explain_only.py`, and `synthetic_explains_analyze.py`.
- The final set of generated synthetic SQL query scripts are systematically organized in the `synthetic_total/` directory.

### 8. Advanced Statistical Analysis & Additional Dashboards
Additional advanced dashboards have been included to perform deep-dive statistical adjustments and comparisons across the workloads:
- **`Final_detailed_analysis.html`**: A comprehensive PostgreSQL execution plan comparison report covering all 113 original vs. synthetic query pairs.
- **`Final_detailed_analysis_weighted.html` & `Final_detailed_analysis_weighted_geo.html`**: These apply geometric and weighted smoothing to the execution plan metrics to prevent extreme outliers from skewing the cost analysis.
- **`cdf_relative_error.html`**: A dedicated Cumulative Distribution Function visualization that maps the relative prediction errors dynamically.
- **`Final_templates.html`**: A dashboard detailing the JOB schema SQL templates, table relationships, and parameterized structures used as the baseline for generation.

### 9. Key Quantitative Findings
Based on the advanced statistical tracking and CDF visualizations, several concrete results emerged:
- **Median vs. Mean Error Divergence:** While the median relative error across all 113 queries sits in a moderate range, the Mean Absolute Percentage Error (MAPE) is massively inflated by extreme outliers. This proves that average error metrics are misleading for synthetic workloads.
- **High-Cost Fidelity:** Synthetic cost estimation is notably more accurate for high-complexity queries. Queries with original execution costs exceeding 700K exhibit a much tighter error bound (typically **2% – 35%**). This suggests the generative model performs more reliably when query optimizer costs are naturally large.
- **Low-Cost Variance (The Outlier Problem):** Conversely, queries with very low original costs (≤ 85K) suffer from enormous percentage errors (often **> 200%**, maxing out at **2,429%** for Query #39). In these cases, even minor absolute prediction mistakes cause catastrophic relative skews.
- **Distribution Tail:** The Cumulative Distribution Function (CDF) demonstrates a steep rise up to the ~100% error mark, followed by a long, heavy right tail. This indicates two distinct populations: a core segment of moderately well-predicted queries, and a significant minority that fails completely to mimic original execution costs.

### Final Conclusion on the SQLBarber Hypothesis
Reproducing a target execution-cost distribution is highly valuable for stress-testing DBMS engines (evaluating memory pressure, caching, and execution time under load). However, my analysis strongly proves that **it is not, by itself, evidence that the generated queries are realistic in structure or intent**. 

The heavy reliance on quota-throttling and structural rewriting during the refinement stage forces queries into cost bins artificially. The synthetic queries may execute at the exact same optimizer cost as the production logs, but they perform fundamentally different logic, lack the nuanced semantic patterns of real-world business workloads, and exhibit massive per-query cost variance.

---
**Repository Setup & Presentation:**
- The `imdb/` dataset directory has been git-ignored due to file size constraints.
- All analytical findings are available as interactive HTML dashboards in this repository. 
- **To view the results:** Do not convert the `.html` files to PDF, as you will lose the interactive Chart.js functionality. Simply drag and drop the `.html` files directly into a modern browser (like Chrome) to explore the data dynamically.

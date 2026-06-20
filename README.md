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

## My Analysis and Results
In my replication using the JOB benchmark and IMDB database, I synthetically created 113 queries to test this hypothesis. 
A deep dive into the cost comparison of the original queries versus the synthetic queries reveals significant discrepancies:
- **Extreme Outliers:** While distributions may align broadly, individual query variance can be wild. For example, some queries exhibited an error margin of over 200%, with one outlier (Query #39) having an error of 2,429%.
- **Variance Across Cost Buckets:** Queries with original costs over 700K tended to have lower percentage errors (typically 6–35%), suggesting synthetic estimation improves at higher cost ranges. However, at lower costs, small absolute errors resulted in massive percentage errors.
- **Conclusion:** Reproducing a target cost distribution is valuable for stress-testing DBMS engines (execution cost, memory pressure), but it is not, by itself, evidence that the generated queries are realistic in structure or intent. The synthetic queries may execute at the same cost but perform fundamentally different logic, lacking the nuanced semantics of real-world workloads.

---
**Repository Contents:**
- Interactive HTML dashboards visualizing the comparison between the 113 original JOB queries and the synthetically generated SQLBarber queries.
- Result datasets and refined query scripts.

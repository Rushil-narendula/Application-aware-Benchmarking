# Reference: Picasso Query-Plan Comparator

`comparator.py` in this folder is the original comparison script from the **Picasso Database Query Optimizer Visualizer**, a research tool from the DSL Lab, IISc ([project page](https://dsl.cds.iisc.ac.in/projects/PICASSO/)):

> Haritsa, J. R. *The Picasso Database Query Optimizer Visualizer.* Proc. VLDB Endowment, Vol. 3, No. 2 (2010).

It is included here **only for attribution** — the full Picasso application (Java GUI client/server, JDBC drivers, documentation) is not part of this repository.

The actual work for this project is in [`/src/plan_comparator_v5.py`](../../src/plan_comparator_v5.py) and [`/src/plan_comparator_v6.py`](../../src/plan_comparator_v6.py), which take the plan-comparison idea from this script and re-implement it as a Zhang-Shasha tree-edit-distance + multiset-Jaccard operator similarity scorer, adapted to compare PostgreSQL `EXPLAIN` plan trees between the original JOB workload and the SQLBarber-synthesized workload — independent of the Picasso Java application entirely.

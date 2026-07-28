# Picasso Execution Plan Comparator

## 1. Project Overview

This project is a batch evaluation framework built on top of **Picasso**, designed to systematically compare original PostgreSQL query execution plans against synthetic query execution plans. 

**What is Picasso?**
Picasso is a database query optimizer analysis tool developed by the Database Systems Lab at the Indian Institute of Science (IISc). It visualizes and analyzes execution plans, natively providing a robust tree-differencing methodology that identifies structural alignments between two query trees (e.g., matching a `Nested Loop` in tree A to a `Nested Loop` in tree B) and assigns similarity states and distance metrics.

**What modifications were made?**
The core Picasso matching algorithm (`PicassoMatcher.java`) was kept entirely intact. The primary modification was bypassing the original GUI and database-connection dependencies to introduce a headless batch runner (`PicassoRunner.java`). This custom runner:
- Ingests PostgreSQL query plans exported as JSON rather than connecting directly to a database.
- Recursively translates these JSON objects into native Picasso `TreeNode` structures.
- Iterates over hundreds of query plan pairs (original vs. synthetic) in a single execution.
- Computes custom analytical metrics entirely downstream of the matching process.
- Exports granular matching and similarity metrics to a series of CSV files for further research.

## 2. Prerequisites

To compile and run this project, you need:
- **Java Development Kit (JDK)**: Version 8 or higher is recommended. Both `java` and `javac` must be available in your system's `PATH`.
- **PostgreSQL**: Not strictly required to *run* the batch comparator (as it reads pre-exported JSON files). However, the original execution plans originate from PostgreSQL.
- **Dependencies**: All necessary external libraries are packaged in the `Libraries/` directory. No external package manager (like Maven or Gradle) is strictly required to run the batch runner.
- **Operating System**: Commands provided below use Windows syntax (e.g., semicolons for classpaths). Linux/macOS users should replace the `;` with `:` in the `-cp` argument and use forward slashes `/`.

## 3. Project Structure

- `PicassoClient/`: Contains the client-side logic, including the newly introduced `PicassoRunner` and JSON parsers (`PlanLoader`, `JsonPlanParser`).
- `PicassoServer/` & `PicassoCommon/`: Contain the core Picasso algorithms and data structures, such as `PicassoMatcher` and `TreeNode`.
- `Libraries/`: Contains all dependent JAR files needed for compilation and execution.
- `original_normalized_plans.json` & `synthetic_normalized_plans.json`: The input datasets containing the raw query plan trees.
- `out/`: The default target directory where compiled `.class` files are placed.
- `QUICK_START.md`: A cheat sheet for rapid execution.

## 4. Build Instructions

Navigate to the root directory of the project in your terminal (e.g., `d:\IITH_project\picasso2.1\picasso2.1`) and execute the following command to compile the project from scratch. It compiles the runner and all its dependencies into the `out` directory.

```powershell
javac -d out -sourcepath "PicassoClient;PicassoCommon;PicassoServer" -cp "Libraries/*" PicassoClient\iisc\dsl\picasso\client\frame\PicassoRunner.java
```

## 5. Running the Project

1. **Input Placement**: Ensure your input JSON files (e.g., `original_normalized_plans.json` and `synthetic_normalized_plans.json`) are located in the project's root working directory.
2. **Execution**: Run the `PicassoRunner` class, supplying the original file, the synthetic file, and the diff type (`0`) as arguments.

```powershell
java -cp "out;Libraries/*" iisc.dsl.picasso.client.frame.PicassoRunner original_normalized_plans.json synthetic_normalized_plans.json 0
```

*Note: If you run the command without arguments, it defaults to looking for `original_normalized_plans.json` and `synthetic_normalized_plans.json` in the current directory.*

## 6. Output Files

The batch runner produces four detailed CSV reports in the project root:

- **`picasso_summary.csv`**: Provides a high-level summary for each query pair, including the total node count, boolean similarity states (e.g., count of `T_IS_SIMILAR`, `T_SUB_OP_DIF` nodes), and root similarity indicators.
- **`picasso_node_details.csv`**: A granular, flattened view containing a row for every single node in both trees. It lists depth, similarity state, match number, and parent structure.
- **`picasso_matched_pairs.csv`**: A direct mapping of alignments. It lists the exact node path from the Original tree and matches it alongside its paired node path from the Synthetic tree.
- **`comparison_summary.csv`**: The primary analytical report containing aggregated structural metrics. It exports Jaccard Distance, Matched Pairs, Union Nodes, and the custom `AverageError` metric.

## 7. Explanation of the New Metric

A custom error metric (`AverageError`) was introduced to quantify cost deviations between matching plan components. Picasso internally assigns a unique, positive `matchNumber` to identify aligned node pairs.

The metric is calculated completely after Picasso finishes its structural differencing:

**For every Original Node:**
1. **If the node is matched (`matchNumber > 0`)**:
   We locate the corresponding Synthetic node with the identical `matchNumber` and compute the normalized cost deviation:
   ```text
   error = abs(originalCost - syntheticCost) / max(originalCost, syntheticCost)
   ```
   *(This guarantees the error remains bounded between 0 and 1).*

2. **If the node is unmatched (`matchNumber == 0`)**:
   The node lacks a counterpart in the synthetic plan. It is penalized maximally:
   ```text
   error = 1.0
   ```

**Aggregation**:
Finally, all individual node errors are summed and averaged across the size of the original tree:
```text
TotalError = sum(all node errors)
AverageError = TotalError / OriginalNodeCount
```

**Interpretation**: Lower `AverageError` values indicate better replication, meaning the synthetic plan structurally and mathematically mimics the original plan more closely.

## 8. Example Execution

**Command:**
```powershell
java -cp "out;Libraries/*" iisc.dsl.picasso.client.frame.PicassoRunner original_normalized_plans.json synthetic_normalized_plans.json 0
```

**Expected Console Output:**
```text
Query 1
---------
OriginalNodeCount : 15
SyntheticNodeCount : 15
MatchedPairs : 5
UnionNodes : 25
JaccardDistance : 0.8
AverageError : 0.6667520021235948
T_SUB_OP_DIF : 0
T_LEFT_SIMILAR : 0
T_RIGHT_SIMILAR : 0
T_NO_DIFF_DONE : 10

NO DIFFERENCE : NO

...

Wrote picasso_summary.csv
Wrote picasso_node_details.csv
Wrote picasso_matched_pairs.csv
Wrote comparison_summary.csv
```

## 9. Troubleshooting

- **`javac` or `java` not found**: 
  Ensure you have installed the JDK and added the JDK `bin` folder to your system's `PATH` environment variable.
- **`java.lang.ClassNotFoundException`**: 
  Ensure your classpath (`-cp`) points correctly to the `out` directory and `Libraries/*`. On Linux/macOS, use `-cp "out:Libraries/*"`.
- **Missing input files**: 
  If you see an `IOException` or `FileNotFoundException`, verify that `original_normalized_plans.json` and `synthetic_normalized_plans.json` are placed exactly in the directory from which you are executing the command.
- **Database Connection Issues**: 
  The custom `PicassoRunner` bypasses DB connections to read JSON directly. If you accidentally attempt to run the older Picasso GUI tools, you may encounter missing PG credential errors. Stick to executing `PicassoRunner`.

## 10. Reproducing Results

To fully reproduce the experimental results from a fresh state:

1. Clone or extract the project repository to your local machine.
2. Open a terminal (Command Prompt or PowerShell).
3. Navigate to the project root directory.
4. Compile the codebase:
   `javac -d out -sourcepath "PicassoClient;PicassoCommon;PicassoServer" -cp "Libraries/*" PicassoClient\iisc\dsl\picasso\client\frame\PicassoRunner.java`
5. Place the experimental datasets (`original_normalized_plans.json` and `synthetic_normalized_plans.json`) in the project root.
6. Execute the runner:
   `java -cp "out;Libraries/*" iisc.dsl.picasso.client.frame.PicassoRunner original_normalized_plans.json synthetic_normalized_plans.json 0`
7. Analyze the resulting `comparison_summary.csv` to review the `AverageError` and Jaccard distances for all evaluated queries.

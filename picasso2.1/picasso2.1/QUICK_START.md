# Picasso Batch Runner Quick Start

This guide provides the essential commands to compile and run the Picasso batch comparator.

## Prerequisites
- Java Development Kit (JDK) installed and available in your system `PATH`.
- Ensure you are in the project root directory (`d:\IITH_project\picasso2.1\picasso2.1` or equivalent) containing `original_normalized_plans.json` and `synthetic_normalized_plans.json`.

## 1. Compile the Source Code
Run the following command to compile the `PicassoRunner` class along with its dependencies. Compiled `.class` files will be placed into the `out` directory.

```powershell
javac -d out -sourcepath "PicassoClient;PicassoCommon;PicassoServer" -cp "Libraries/*" PicassoClient\iisc\dsl\picasso\client\frame\PicassoRunner.java
```

## 2. Run the Batch Comparator
Run the compiled `PicassoRunner`, passing the JSON input files and the diff type (`0`) as arguments. 

```powershell
java -cp "out;Libraries/*" iisc.dsl.picasso.client.frame.PicassoRunner original_normalized_plans.json synthetic_normalized_plans.json 0
```

## Outputs
Execution typically completes in under a minute for ~100 queries. The runner will output progress to the console and produce the following CSV files in the same directory:
- `picasso_summary.csv`
- `picasso_node_details.csv`
- `picasso_matched_pairs.csv`
- `comparison_summary.csv` (contains the `AverageError` metric)

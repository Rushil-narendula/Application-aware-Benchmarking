package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.TreeNode;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PicassoRunner {
    private final PlanLoader loader;
    private final PicassoMatcher matcher;
    private final Path outputDirectory;

    public PicassoRunner() {
        this(new PlanLoader(), new PicassoMatcher(), Paths.get(System.getProperty("user.dir")));
    }

    public PicassoRunner(PlanLoader loader, PicassoMatcher matcher) {
        this(loader, matcher, Paths.get(System.getProperty("user.dir")));
    }

    private PicassoRunner(PlanLoader loader, PicassoMatcher matcher, Path outputDirectory) {
        this.loader = loader;
        this.matcher = matcher;
        this.outputDirectory = outputDirectory;
    }

    public static void main(String[] args) throws Exception {
        String originalFile = args.length > 0 ? args[0] : "original_normalized_plans.json";
        String syntheticFile = args.length > 1 ? args[1] : "synthetic_normalized_plans.json";
        int diffType = args.length > 2 ? Integer.parseInt(args[2]) : 0;

        new PicassoRunner().run(originalFile, syntheticFile, diffType);
    }

    public void run(String originalFile, String syntheticFile, int diffType) throws IOException {
        List<TreeNode> original = loader.loadPlans(originalFile, "original");
        List<TreeNode> synthetic = loader.loadPlans(syntheticFile, "synthetic");

        List<String[]> summaryRows = new ArrayList<String[]>();
        List<String[]> nodeRows = new ArrayList<String[]>();
        List<String[]> matchedPairRows = new ArrayList<String[]>();
        List<String[]> comparisonSummaryRows = new ArrayList<String[]>();
        
        int size = Math.min(original.size(), synthetic.size());
        for (int i = 0; i < size; i++) {
            matcher.compare(original.get(i), synthetic.get(i), diffType);
            
            double averageError = computeAverageError(original.get(i), synthetic.get(i));

            summaryRows.add(buildSummaryRow(i + 1, original.get(i), synthetic.get(i)));
            comparisonSummaryRows.add(buildComparisonSummaryRow(i + 1, original.get(i), synthetic.get(i), averageError));
            appendNodeRows(nodeRows, i + 1, "Original", original.get(i));
            appendNodeRows(nodeRows, i + 1, "Synthetic", synthetic.get(i));
            matchedPairRows.addAll(buildMatchedPairRows(i + 1, matcher.getLastMatchedPairs()));

            printQueryReport(i + 1, original.get(i), synthetic.get(i), averageError);
        }

        writeCsv(outputDirectory.resolve("picasso_summary.csv"), buildSummaryHeader(), summaryRows);
        writeCsv(outputDirectory.resolve("picasso_node_details.csv"), buildNodeDetailHeader(), nodeRows);
        writeCsv(outputDirectory.resolve("picasso_matched_pairs.csv"), buildMatchedPairHeader(), matchedPairRows);
        writeCsv(outputDirectory.resolve("comparison_summary.csv"), buildComparisonSummaryHeader(), comparisonSummaryRows);
        
        System.out.println("Wrote " + outputDirectory.resolve("picasso_summary.csv"));
        System.out.println("Wrote " + outputDirectory.resolve("picasso_node_details.csv"));
        System.out.println("Wrote " + outputDirectory.resolve("picasso_matched_pairs.csv"));
        System.out.println("Wrote " + outputDirectory.resolve("comparison_summary.csv"));
    }

    private double computeAverageError(TreeNode originalRoot, TreeNode syntheticRoot) {
        if (originalRoot == null) return 0.0;
        
        List<TreeNode> allOrig = new ArrayList<TreeNode>();
        List<TreeNode> allSynth = new ArrayList<TreeNode>();
        
        flattenTree(originalRoot, allOrig);
        flattenTree(syntheticRoot, allSynth);
        
        double totalError = 0.0;
        int originalNodeCount = allOrig.size();
        
        if (originalNodeCount == 0) return 0.0;
        
        for (int i = 0; i < allOrig.size(); i++) {
            TreeNode origNode = allOrig.get(i);
            int matchNum = origNode.getMatchNumber();
            if (matchNum > 0) {
                TreeNode matchedSynth = null;
                for (int j = 0; j < allSynth.size(); j++) {
                    TreeNode synthNode = allSynth.get(j);
                    if (synthNode.getMatchNumber() == matchNum) {
                        matchedSynth = synthNode;
                        break;
                    }
                }
                
                if (matchedSynth != null) {
                    double origCost = origNode.getNodeValues()[0];
                    double synthCost = matchedSynth.getNodeValues()[0];
                    double maxCost = Math.max(origCost, synthCost);
                    
                    if (maxCost > 0.0) {
                        totalError += Math.abs(origCost - synthCost) / maxCost;
                    }
                } else {
                    totalError += 1.0;
                }
            } else {
                totalError += 1.0;
            }
        }
        
        return totalError / originalNodeCount;
    }

    private void flattenTree(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        list.add(node);
        List children = node.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if (child instanceof TreeNode) {
                    flattenTree((TreeNode) child, list);
                }
            }
        }
    }

    private String[] buildSummaryRow(int queryNumber, TreeNode originalRoot, TreeNode syntheticRoot) {
        Map<Integer, Integer> counts = new TreeMap<Integer, Integer>();
        collectSimilarityCounts(originalRoot, counts);
        collectSimilarityCounts(syntheticRoot, counts);

        boolean noDifferenceDetected = isTreeFullySimilar(originalRoot) && isTreeFullySimilar(syntheticRoot);
        int originalNodeCount = countNodes(originalRoot);
        int syntheticNodeCount = countNodes(syntheticRoot);

        return new String[] {
                String.valueOf(queryNumber),
                String.valueOf(originalNodeCount),
                String.valueOf(syntheticNodeCount),
                String.valueOf(getCount(counts, PicassoConstants.T_IS_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_SUB_OP_DIF)),
                String.valueOf(getCount(counts, PicassoConstants.T_LEFT_EQ_RIGHT)),
                String.valueOf(getCount(counts, PicassoConstants.T_LEFT_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_RIGHT_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_LEFT_EQ)),
                String.valueOf(getCount(counts, PicassoConstants.T_RIGHT_EQ)),
                String.valueOf(getCount(counts, PicassoConstants.T_NO_CHILD_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_NP_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_NP_LEFT_EQ_RIGHT)),
                String.valueOf(getCount(counts, PicassoConstants.T_NP_LEFT_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_NP_RIGHT_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_NP_NOT_SIMILAR)),
                String.valueOf(getCount(counts, PicassoConstants.T_NO_DIFF_DONE)),
                String.valueOf(getCount(counts, PicassoConstants.T_EDIT_NODE)),
                originalRoot == null ? "" : String.valueOf(originalRoot.getSimilarity()),
                originalRoot == null ? "" : String.valueOf(originalRoot.getMatchNumber()),
                noDifferenceDetected ? "Yes" : "No"
        };
    }

    private void appendNodeRows(List<String[]> nodeRows, int queryNumber, String treeLabel, TreeNode root) {
        if (root == null) {
            return;
        }

        appendNodeRows(nodeRows, queryNumber, treeLabel, root, null);
    }

    private void appendNodeRows(List<String[]> nodeRows, int queryNumber, String treeLabel, TreeNode node, TreeNode parent) {
        if (node == null) {
            return;
        }

        List children = node.getChildren();
        int childCount = children == null ? 0 : children.size();
        String parentName = parent == null ? "" : parent.getNodeName();
        nodeRows.add(new String[] {
                String.valueOf(queryNumber),
                treeLabel,
                node.getNodeName(),
                String.valueOf(node.getDepth()),
                String.valueOf(node.getSimilarity()),
                String.valueOf(node.getMatchNumber()),
                parentName,
                String.valueOf(childCount)
        });

        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if (child instanceof TreeNode) {
                    appendNodeRows(nodeRows, queryNumber, treeLabel, (TreeNode) child, node);
                }
            }
        }
    }

    private void printQueryReport(int queryNumber, TreeNode originalRoot, TreeNode syntheticRoot, double averageError) {
        Map<Integer, Integer> counts = new TreeMap<Integer, Integer>();
        collectSimilarityCounts(originalRoot, counts);

        int originalNodeCount = countNodes(originalRoot);
        int syntheticNodeCount = countNodes(syntheticRoot);
        int matchedPairs = countMatchedPairs(originalRoot);
        int unionNodes = originalNodeCount + syntheticNodeCount - matchedPairs;
        double jaccardDistance = unionNodes == 0 ? 0.0 : 1.0 - ((double) matchedPairs / unionNodes);

        int subOpDif = getCount(counts, PicassoConstants.T_SUB_OP_DIF);
        int leftSimilar = getCount(counts, PicassoConstants.T_LEFT_SIMILAR);
        int rightSimilar = getCount(counts, PicassoConstants.T_RIGHT_SIMILAR);
        int noDiffDone = getCount(counts, PicassoConstants.T_NO_DIFF_DONE);
        boolean noDifference = isTreeFullySimilar(originalRoot) && isTreeFullySimilar(syntheticRoot);

        System.out.println("Query " + queryNumber);
        System.out.println("---------");
        System.out.println("OriginalNodeCount : " + originalNodeCount);
        System.out.println("SyntheticNodeCount : " + syntheticNodeCount);
        System.out.println("MatchedPairs : " + matchedPairs);
        System.out.println("UnionNodes : " + unionNodes);
        System.out.println("JaccardDistance : " + jaccardDistance);
        System.out.println("AverageError : " + averageError);
        System.out.println("T_SUB_OP_DIF : " + subOpDif);
        System.out.println("T_LEFT_SIMILAR : " + leftSimilar);
        System.out.println("T_RIGHT_SIMILAR : " + rightSimilar);
        System.out.println("T_NO_DIFF_DONE : " + noDiffDone);
        System.out.println();
        System.out.println("NO DIFFERENCE : " + (noDifference ? "YES" : "NO"));
        System.out.println();
    }

    private int countMatchedPairs(TreeNode node) {
        if (node == null) {
            return 0;
        }

        int count = node.getMatchNumber() > 0 ? 1 : 0;
        List children = node.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if (child instanceof TreeNode) {
                    count += countMatchedPairs((TreeNode) child);
                }
            }
        }
        return count;
    }

    private void collectSimilarityCounts(TreeNode node, Map<Integer, Integer> counts) {
        if (node == null) {
            return;
        }

        int similarity = node.getSimilarity();
        Integer value = counts.get(similarity);
        counts.put(similarity, value == null ? 1 : value + 1);

        List children = node.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if (child instanceof TreeNode) {
                    collectSimilarityCounts((TreeNode) child, counts);
                }
            }
        }
    }

    private int countNodes(TreeNode node) {
        if (node == null) {
            return 0;
        }

        int count = 1;
        List children = node.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if (child instanceof TreeNode) {
                    count += countNodes((TreeNode) child);
                }
            }
        }
        return count;
    }

    private boolean isTreeFullySimilar(TreeNode node) {
        if (node == null) {
            return true;
        }
        if (node.getSimilarity() != PicassoConstants.T_IS_SIMILAR) {
            return false;
        }

        List children = node.getChildren();
        if (children == null) {
            return true;
        }
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (child instanceof TreeNode && !isTreeFullySimilar((TreeNode) child)) {
                return false;
            }
        }
        return true;
    }

    private int getCount(Map<Integer, Integer> counts, int similarity) {
        Integer count = counts.get(similarity);
        return count == null ? 0 : count.intValue();
    }

    private List<String[]> buildMatchedPairRows(int queryNumber, List<PicassoMatcher.MatchedPair> matchedPairs) {
        List<String[]> rows = new ArrayList<String[]>();
        for (PicassoMatcher.MatchedPair pair : matchedPairs) {
            TreeNode originalNode = pair.getNode1();
            TreeNode syntheticNode = pair.getNode2();
            rows.add(new String[] {
                    String.valueOf(queryNumber),
                    buildNodePath(originalNode),
                    buildNodePath(syntheticNode),
                    originalNode == null ? "" : originalNode.getNodeName(),
                    syntheticNode == null ? "" : syntheticNode.getNodeName(),
                    originalNode == null ? "" : String.valueOf(originalNode.getDepth()),
                    syntheticNode == null ? "" : String.valueOf(syntheticNode.getDepth()),
                    originalNode == null ? "" : String.valueOf(originalNode.getSimilarity()),
                    syntheticNode == null ? "" : String.valueOf(syntheticNode.getSimilarity()),
                    originalNode == null ? "" : String.valueOf(originalNode.getMatchNumber()),
                    syntheticNode == null ? "" : String.valueOf(syntheticNode.getMatchNumber())
            });
        }
        return rows;
    }

    private String buildNodePath(TreeNode node) {
        if (node == null) {
            return "";
        }

        List<String> parts = new ArrayList<String>();
        TreeNode current = node;
        while (current != null) {
            parts.add(current.getNodeName());
            current = current.getParent();
        }

        StringBuilder builder = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String[] buildComparisonSummaryRow(int queryNumber, TreeNode originalRoot, TreeNode syntheticRoot, double averageError) {
        int originalNodeCount = countNodes(originalRoot);
        int syntheticNodeCount = countNodes(syntheticRoot);
        int matchedPairs = countMatchedPairs(originalRoot);
        int unionNodes = originalNodeCount + syntheticNodeCount - matchedPairs;
        double jaccardDistance = unionNodes == 0 ? 0.0 : 1.0 - ((double) matchedPairs / unionNodes);
        boolean noDifferenceDetected = isTreeFullySimilar(originalRoot) && isTreeFullySimilar(syntheticRoot);

        return new String[] {
                String.valueOf(queryNumber),
                String.valueOf(originalNodeCount),
                String.valueOf(syntheticNodeCount),
                String.valueOf(matchedPairs),
                String.valueOf(unionNodes),
                String.valueOf(jaccardDistance),
                String.valueOf(averageError),
                noDifferenceDetected ? "Yes" : "No",
                originalRoot == null ? "" : String.valueOf(originalRoot.getSimilarity()),
                originalRoot == null ? "" : String.valueOf(originalRoot.getMatchNumber())
        };
    }

    private String[] buildComparisonSummaryHeader() {
        return new String[] {
                "QueryNumber",
                "OriginalNodeCount",
                "SyntheticNodeCount",
                "MatchedPairs",
                "UnionNodes",
                "JaccardDistance",
                "AverageError",
                "NoDifference",
                "RootSimilarity",
                "RootMatchNumber"
        };
    }

    private String[] buildSummaryHeader() {
        return new String[] {
                "QueryNumber",
                "OriginalNodeCount",
                "SyntheticNodeCount",
                "T_IS_SIMILAR_Count",
                "T_SUB_OP_DIF_Count",
                "T_LEFT_EQ_RIGHT_Count",
                "T_LEFT_SIMILAR_Count",
                "T_RIGHT_SIMILAR_Count",
                "T_LEFT_EQ_Count",
                "T_RIGHT_EQ_Count",
                "T_NO_CHILD_SIMILAR_Count",
                "T_NP_SIMILAR_Count",
                "T_NP_LEFT_EQ_RIGHT_Count",
                "T_NP_LEFT_SIMILAR_Count",
                "T_NP_RIGHT_SIMILAR_Count",
                "T_NP_NOT_SIMILAR_Count",
                "T_NO_DIFF_DONE_Count",
                "T_EDIT_NODE_Count",
                "RootSimilarity",
                "RootMatchNumber",
                "NoDifferenceDetected"
        };
    }

    private String[] buildNodeDetailHeader() {
        return new String[] {
                "QueryNumber",
                "Tree",
                "NodeName",
                "Depth",
                "SimilarityState",
                "MatchNumber",
                "ParentNode",
                "NumberOfChildren"
        };
    }

    private String[] buildMatchedPairHeader() {
        return new String[] {
                "QueryNumber",
                "OriginalNodePath",
                "SyntheticNodePath",
                "OriginalNodeName",
                "SyntheticNodeName",
                "OriginalDepth",
                "SyntheticDepth",
                "OriginalSimilarityState",
                "SyntheticSimilarityState",
                "OriginalMatchNumber",
                "SyntheticMatchNumber"
        };
    }



    private void writeCsv(Path outputPath, String[] header, List<String[]> rows) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath.toFile()), StandardCharsets.UTF_8));
        try {
            writer.write('\uFEFF');
            writer.write(joinCsv(header));
            writer.newLine();
            for (String[] row : rows) {
                writer.write(joinCsv(row));
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    private String joinCsv(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values[i]));
        }
        return builder.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return '"' + escaped + '"';
        }
        return escaped;
    }
}

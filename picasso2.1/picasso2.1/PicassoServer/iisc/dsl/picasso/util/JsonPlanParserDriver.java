package iisc.dsl.picasso.util;

import iisc.dsl.picasso.common.ds.TreeNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class JsonPlanParserDriver {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: JsonPlanParserDriver <path-to-json-file>");
            System.exit(2);
        }

        Path inputPath = Paths.get(args[0]);
        String json = Files.readString(inputPath);
        TreeNode root = new JsonPlanParser().parse(json);

        verifyStructure(root);
        System.out.println("TreeNode structure verified successfully for " + inputPath);
        System.out.println("Root: " + root.getNodeName());
        System.out.println("Children: " + root.getChildren().size());
        printTree(root, 0);
    }

    private static void verifyStructure(TreeNode node) {
        if (node == null) {
            throw new AssertionError("Parsed tree root is null.");
        }

        if (node.getAttributes() == null) {
            throw new AssertionError("Node attributes should not be null.");
        }

        Vector children = node.getChildren();
        if (children == null) {
            throw new AssertionError("Children collection should not be null.");
        }

        for (Object childObj : children) {
            if (!(childObj instanceof TreeNode)) {
                throw new AssertionError("Child entry is not a TreeNode.");
            }
            TreeNode child = (TreeNode) childObj;
            if (child.getParent() != node) {
                throw new AssertionError("Child parent link is incorrect.");
            }
            verifyStructure(child);
        }
    }

    private static void printTree(TreeNode node, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        System.out.println(node.getNodeName());

        Vector children = node.getChildren();
        for (Object child : children) {
            printTree((TreeNode) child, depth + 1);
        }
    }
}

package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.common.ds.TreeNode;

import java.util.Hashtable;
import java.util.Vector;

public class PicassoComparator {
    private final PlanTreeFrame frame;

    public PicassoComparator(PlanTreeFrame frame) {
        this.frame = frame;
    }

    public Hashtable compare(TreeNode root1, TreeNode root2, int diffType) {
        Hashtable matching = frame.getBestMatching(root1, root2, diffType);
        frame.setSimilarity(root1, root2, matching);
        frame.setFetchNodes(root1);
        frame.setFetchNodes(root2);
        return matching;
    }
}

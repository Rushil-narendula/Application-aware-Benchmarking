
/*
 # 
 # 
 # PROGRAM INFORMATION
 # 
 # 
 # Copyright (C) 2006 Indian Institute of Science, Bangalore, India.
 # All rights reserved.
 # 
 # This program is part of the Picasso Database Query Optimizer Visualizer
 # software distribution invented at the Database Systems Lab, Indian
 # Institute of Science (PI: Prof. Jayant R. Haritsa). The software is
 # free and its use is governed by the licensing agreement set up between
 # the copyright owner, Indian Institute of Science, and the licensee.
 # The software is distributed without any warranty; without even the
 # implied warranty of merchantability or fitness for a particular purpose.
 # The software includes external code modules, whose use is governed by
 # their own licensing conditions, which can be found in the Licenses file
 # of the Docs directory of the distribution.
 # 
 # 
 # The official project web-site is
 #     http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html
 # and the email contact address is 
 #     picasso@dsl.serc.iisc.ernet.in
 # 
 #
*/

package iisc.dsl.picasso.common;


import iisc.dsl.picasso.common.ds.TreeNode;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class TreeUtil {

	public static TreeNode getLeftTree(TreeNode tree) {
		Vector children = tree.getChildren();
		if ( children == null || children.size() == 0 )
			return null;
		return((TreeNode)children.elementAt(0));
	}
	
	public static TreeNode getRightTree(TreeNode tree) {
		Vector children = tree.getChildren();
		if ( children == null || children.size() < 2 )
			return null;
		return((TreeNode)children.elementAt(1));
	}
	
	
	public static TreeNode[] getLeaves(TreeNode root) {
		ArrayList leaves = new ArrayList();
		// test
		//initialize the stack to enumerate all nodes
		Stack stack = new Stack();
		stack.push (root);
		
		while (stack.empty() == false)
		{
			TreeNode curr = (TreeNode) stack.pop();
			/*TreeNode left = getLeftTree(curr);
			TreeNode right = getRightTree(curr);*/
			Vector children = curr.getChildren();
			
			/*if ( left == null &&  right == null)
				leaves.add(curr);*/
			if ( children.size() == 0 )
				leaves.add(curr);
			
			/*if (right != null)	stack.add (right);
			if (left  != null)	stack.add (left);*/
			for (int i=0; i < children.size(); i++) {
				stack.add(children.elementAt(i));
			}
		}
		
		TreeNode nodes[] = new TreeNode[leaves.size()];
		for (int i=0; i<nodes.length; i++) {
			nodes[i] = (TreeNode)leaves.get(i);
			//MessageUtil.CPrintToConsole(i + " Leaves :: " + nodes[i].getNodeName());
		}
		return nodes;
	}
	
	/** Get all joins of this tree and return them in ArrayList **/
	public static TreeNode[] getJoinNodes(TreeNode root, String[] treeNames, int[] treeCard)
	{
		ArrayList joins = new ArrayList();
		
		//initialize the stack to enumarate all nodes
		Stack stack = new Stack();
		stack.push (root);
		
		while (stack.empty() == false) {
			TreeNode curr = (TreeNode) stack.pop();
			/*TreeNode left = getLeftTree(curr);
			TreeNode right = getRightTree(curr);*/
			Vector children = curr.getChildren();
			
			String nodeName = curr.getNodeName();
			
			/*if (left != null && right != null  && 
					nodeName.equals ("FETCH") == false)*/
			/*if (children.size() >= 2  
					&& nodeName.equals ("FETCH") == false  // DB2
					&& nodeName.equals("Seq Scan") == false  // Postgres
					&& nodeName.equals ("TABLE ACCESS") == false ) // Oracle*/
			if(curr.getNodeCard(treeNames, treeCard)!=100 && curr.getNodeCard(treeNames, treeCard)>=2 )
				joins.add(curr);
			
			/*if (right != null)	stack.add (right);
			if (left  != null)	stack.add (left);*/
			for (int i=0; i < children.size(); i++) {
				stack.add(children.elementAt(i));
			}
			
		}
		
		TreeNode nodes[] = new TreeNode[joins.size()];
		for (int i=0; i<nodes.length; i++) {
			nodes[i] = (TreeNode)joins.get(i);
			//MessageUtil.CPrintToConsole(i + " JOINS :: " + nodes[i].getNodeName());
		}
		
		return nodes;
	}
	
	/** Get the subtree rooted at given node **/
	public static TreeNode getSubTree(TreeNode root, TreeNode node)
	{
		if (root == node)	return root;
		
		TreeNode tr = null;
		Vector children = root.getChildren();
		for (int i=0; i < children.size(); i++) {
			TreeNode n1 = (TreeNode)(children.elementAt(i));
			tr = getSubTree(n1, node);
			if ( tr != null ) break;
		}
		/*if ( tr != null)
			MessageUtil.CPrintToConsole("Sub Tree :: " + tr.getNodeName());
		else
			MessageUtil.CPrintToConsole("Sub Tree is Null " + node.getNodeName());*/
		return tr;
	}
	
	/** Get all relations used in this tree **/
	public static String[] getRelations(TreeNode root)
	{
		ArrayList rel = new ArrayList();
		
		//initialize the stack to enumarate all nodes
		Stack stack = new Stack();
		if(root!=null)
                    stack.push(root);
		
		while (stack.empty() == false) {
			TreeNode curr = (TreeNode) stack.pop();
			
			/*TreeNode left = getLeftTree(curr);
			TreeNode right = getRightTree(curr);*/
			Vector children = curr.getChildren();
			
			String nodeName = curr.getNodeName();
			
			/*if (left == null && right == null*/
			if ( children.size() == 0)
					/*&& !nodeName.regionMatches(true, 0, "SQL", 0, 3) // For DB2 Fetch...
					&& !nodeName.regionMatches(true, 0, "SYS_", 0, 4)) // For Oracle SYS_*/
				rel.add(nodeName);
			
			/*if (right != null)	stack.add (right);
			if (left  != null /*&& nodeName.equals ("FETCH") == false*?/)	
				stack.add(left);*/
			for (int i=0; i < children.size(); i++) {
				stack.add(children.elementAt(i));
			}
		}
		
		String relNames[] = new String[rel.size()];
		for (int i=0; i<relNames.length; i++) {
			relNames[i] = (String) rel.get(i);
			//MessageUtil.CPrintToConsole(i + " Relation Names :: " + relNames[i]);
		}
		
		return relNames;
	}
	
	public static boolean areTreesEqual(TreeNode t1, TreeNode t2) {
		if ( t1 == null && t2 == null )
			return true;
		if ( t1 == null || t2 == null )
			return false;
		if ( isEquals(t1, t2, OPERATOR_LEVEL) ) {
			TreeNode l1 = getLeftTree(t1);
			TreeNode l2 = getLeftTree(t2);
			TreeNode r1 = getRightTree(t1);
			TreeNode r2 = getRightTree(t2);
			if ( areTreesEqual(l1, l2) == true && areTreesEqual(r1, r2) == true )
				return true;
			else
				return false;
		} else
			return false;
	}
	
	public static final int OPERATOR_LEVEL = 0;
	public static final int SUB_OPERATOR_LEVEL = 1;
	
	public static boolean isEquals(TreeNode t1, TreeNode t2, int diffType) {
		if ( t1.getNodeName().equals(t2.getNodeName()) == false )
			return false;
		
		if ( diffType != SUB_OPERATOR_LEVEL ) // Always compare operator now
			return true;
		  
		Hashtable attr1 = t1.getAttributes();
		Hashtable attr2 = t2.getAttributes();
		
		//MessageUtil.CPrintToConsole(attr1 + " Attr 2 :: " + attr2);
		if ( t1.getAttributes() == null ) {
			if ( t2.getAttributes() == null )
				return true;
			else {
				t1.setShowAttr(true);
				t2.setShowAttr(true);
				return false;
			}
		}
		
		Object[] keys = attr1.keySet().toArray();
		Object[] keys2 = attr2.keySet().toArray();
		if ( keys.length != keys2.length ) {
			t1.setShowAttr(true);
			t2.setShowAttr(true);
			return false;
		}
		for (int i=0; i < keys.length; i++) {
			Object val2 = attr2.get(keys[i]);
			Object val1 = attr1.get(keys[i]);
			if ( val2 == null || !sameValue(val1, val2) ) {
				t1.setShowAttr(true);
				t2.setShowAttr(true);
				return false;
			}
		}
		return true;
	}

	private static boolean sameValue(Object left, Object right) {
		if (left == null || right == null) {
			return left == right;
		}
		if (left instanceof Number && right instanceof Number) {
			return ((Number) left).doubleValue() == ((Number) right).doubleValue();
		}
		return String.valueOf(left).equals(String.valueOf(right));
	}
}

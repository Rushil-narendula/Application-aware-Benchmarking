
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

package iisc.dsl.picasso.common.ds;


import iisc.dsl.picasso.common.PicassoConstants;

import java.awt.Color;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.Hashtable;
import java.util.ListIterator;

public class TreeNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int 		depth;
	private int			type;
	private TreeNode 	parent;
	private Vector		children;
	private int			isSimilar=PicassoConstants.T_NO_DIFF_DONE;
	private int			matchNum=0;
	private boolean		showAttr = false;
	
	private String		nodeName;
	private double		actualCost;
	private double		estimatedCost;
	private double		cardinality;
	private Hashtable	attributes;
	private int			displayType;
	private boolean isDependent; 		// true for dependent nodes
	
	public TreeNode(int d, TreeNode p) {
		parent = p;
		depth = d;
		attributes = new Hashtable();
		displayType = PicassoConstants.SHOW_NONE;
	}
	
	public String toString() {
		//Changed to -1 so that cardinalities can be displayed for relation names
                if(children.size()!=-1)
                {
		DecimalFormat df = new DecimalFormat("0.00E0");
		switch(displayType) {
		case PicassoConstants.SHOW_COST :
			if ( actualCost == 0.0 )
				return(nodeName);
			if ( matchNum == 0 )
				return(nodeName + " | Cost: " + df.format(actualCost));
			else
				return(nodeName + " <" + matchNum + ">" + " | Cost: " + df.format(actualCost));
			
		case PicassoConstants.SHOW_CARD :
			if ( cardinality == 0.0 )
				return(nodeName);
			
			if(matchNum != 0)
				return(nodeName + " <" + matchNum + ">"+ " | Card: " + df.format(cardinality));
			else
				return(nodeName + " | Card: " + df.format(cardinality));
			
		case PicassoConstants.SHOW_BOTH :
			if ( actualCost == 0.0 && cardinality == 0.0 )
				return(nodeName);
			else if ( cardinality == 0.0 )
			{
				if(matchNum == 0)				
					return(nodeName + " | Cost: " + df.format(actualCost));
				else
					return(nodeName + " <" + matchNum + ">" + " | Cost: " + df.format(actualCost));
			}
			else if ( actualCost == 0.0 )
			{
				if(matchNum == 0)
					return(nodeName + " | Card: " + df.format(cardinality));
				else
					return(nodeName + " <" + matchNum + ">" + " | Card: " + df.format(cardinality));
			}
			else
			{
				if(matchNum == 0)
					return(nodeName + " | Cost: " + df.format(actualCost) + " Card: " + df.format(cardinality));
				else
					return(nodeName + " <" + matchNum + ">" + " | Cost: " + df.format(actualCost) + " Card: " + df.format(cardinality));
			}
		case PicassoConstants.SHOW_NONE :
			if ( matchNum != 0 )
				return(nodeName + " <" + matchNum + ">");
			return(nodeName);
		}
                }
		return(nodeName); // + ", EC : " + actualCost);
	}
	
	public void setDisplayType(int dispType) {
		displayType = dispType;
	}
	
	// Get and Set methods..
	public boolean getDependency()
	{
		return isDependent;
	}
	public void setDependency(boolean isDep)
	{
		isDependent = isDep;
	}
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int d) {
		depth = d;
	}

	public int getType() {
		return type;
	}
	
	public void setType(int t) {
		type = t;
	}
	
	public int getSimilarity() {
		return isSimilar;
	}
	
	public void setSimilarity(int sim) {
		isSimilar = sim;
	}
	
	public boolean showAttrs() {
		return showAttr;
	}
	
	public void setShowAttr(boolean sim) {
		showAttr = sim;
	}
	
	public Hashtable getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Hashtable attrib) {
		attributes = attrib;
	}
	
	public double getCardinality() {
		return cardinality;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public void setParent(TreeNode p) {
		parent = p;
	}
	
	public Vector getChildren() {
		return(children);
	}
	
	public void setChildren(Vector c) {
		children = c;
	}
	
	public void setNodeValues(String name, int type, double ac, 
			double ec, double c, Vector argType, Vector argValue) {
		nodeName = name;
		this.type = type; 
		actualCost = ac;
		estimatedCost = ec;
		cardinality = c;
		if(argType == null)
			return;
		ListIterator itt = argType.listIterator();
		ListIterator itv = argValue.listIterator();
		while(itt.hasNext() && itv.hasNext()){
			Object a = itt.next(), b = itv.next();
			if(a !=null && b!=null)
				attributes.put(a, b);
		}
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	public double[] getNodeValues() {
		double[] val = new double[3];
		
		val[0] = actualCost;
		val[1] = estimatedCost;
		val[2] = cardinality;
		return val;
	}
	
	public void setMatchNumber(int mn) {
		matchNum = mn;
	}
	
	public int getMatchNumber() {
		return matchNum;
	}
	
	public Color getNodeColor(String[] treeNames) {
		for (int i=0; i < treeNames.length; i++) {
			if ( nodeName.equalsIgnoreCase(treeNames[i])){//regionMatches(true, 0, treeNames[i], 0, treeNames[i].length())) {
				return(new Color(PicassoConstants.treeColor[i]));
			}
		}
		return(new Color(PicassoConstants.DEFAULT_TREE_NODE_COLOR));
	}
	
	public int getNodeCard(String[] treeNames, int[] treecard)
	{
	    		for (int i=0; i < treeNames.length; i++) {
			    if(nodeName.equalsIgnoreCase(treeNames[i])){
				return(treecard[i]);
			}
		}
		return(100);
	}
}

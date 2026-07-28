/*
 #
 #
 # PROGRAM INFORMATION
 #
 #
 # Copyright (C) 2006 Indian Institute of Science, Bangalore, India.
 # All rights reserved.
 #
 # This program is part of the Picasso Database Query Optimizer
Visualizer
 # software distribution invented at the Database Systems Lab, Indian
 # Institute of Science (PI: Prof. Jayant R. Haritsa). The software is
 # free and its use is governed by the licensing agreement set up
between
 # the copyright owner, Indian Institute of Science, and the licensee.
 # The software is distributed without any warranty; without even the
 # implied warranty of merchantability or fitness for a particular
purpose.
 # The software includes external code modules, whose use is governed
by
 # their own licensing conditions, which can be found in the Licenses
file
 # of the Docs directory of the distribution.
 #
 #
 # The official project web-site is
 #     http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html
 #
and the email contact address is
 #     picasso@dsl.serc.iisc.ernet.in
 #
 #
*/

package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.panel.LogoPanel;
import iisc.dsl.picasso.client.panel.PGraph;
import iisc.dsl.picasso.client.print.PicassoPrint;
import iisc.dsl.picasso.client.print.PicassoSave;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.TreeUtil;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.TreeNode;
//import java.awt.event.WindowAdapter;  
import java.awt.event.WindowEvent; 
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
//import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
//import java.awt.peer.WindowPeer;
//import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JFrame;
//import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class PlanTreeFrame extends JFrame implements ActionListener,WindowListener  {

	private static final long serialVersionUID = -1318220941054132845L;

	PGraph 				myGraphs[];
	JPanel 				centerPanel, northPanel;
	Vector				trees;
	int 				numOfPlans;
	int[][]				sortedPlan;
	ClientPacket		clientPacket;
	String[]			treeNames;
	int[]				treecard;

	JMenuBar				pMenuBar;
	JMenuItem 				about, exit, print, save, contentHelp, preview, colorCode;
	JFrame				cCode;
	Font 				font = new Font("Tahoma", Font.PLAIN, 14);
	String cCodeStr = "\n    Identical Nodes: White Color/Black Links" +
	"\n\n    Similar Nodes (Same Operator): White Fill" +
	"\n       Parameter Different: Green Border/Black Links" +
	"\n       Left/Right Relations Swapped: Orange Border/Blue Links" +
	"\n       Left/Right Relations Different: Orange Border/Red Links" +
	"\n\n    Mapped Nodes (Different Operators): Native Fill/Red Border" +
	"\n       Left/Right Relations Swapped: Blue Links" +
	"\n       Left/Right Relations Different: Red Links" +
	"\n\nUnmapped Nodes: Native Color/Black Links\n";
	
	JTextArea cCodelabel = new JTextArea(cCodeStr);


	String[]				infoStr;
	public int				displayType;
        private final int SUBOP_OFFSET =PicassoConstants.SO_BASE;

	public PlanTreeFrame(ClientPacket cp, int[][] sp, ServerPacket treePacket) {
		super("Schematic Plan Tree");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}

//		Get the number of plan trees sent
		// trees = new Vector(treePacket.trees);
		trees = treePacket.trees;
		numOfPlans = ((Integer)trees.elementAt(0)).intValue();

		pMenuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		//JMenu edit = new JMenu("Edit");
		JMenu help = new JMenu("Help");

		exit = new JMenuItem("Exit");
		print = new JMenuItem("Print Diagram");
		preview = new JMenuItem("Print Diagram Preview");
		save = new JMenuItem("Save");
		cCode = new JFrame();
		file.addSeparator();
		file.add(save);
		file.add(print);
		file.add(preview);
		file.addSeparator();
		file.add(exit);

		about = new JMenuItem("About Picasso");
		contentHelp = new JMenuItem("Usage Guide");
		colorCode = new JMenuItem("Color Code Guide");

		help.add(contentHelp);
		if ( numOfPlans > 1 )
			help.add(colorCode);
		help.add(about);

		pMenuBar.add(file);
		pMenuBar.add(help);
		setJMenuBar(pMenuBar);

		about.addActionListener(this);
		contentHelp.addActionListener(this);
		save.addActionListener(this);
		print.addActionListener(this);
		preview.addActionListener(this);
		exit.addActionListener(this);
		colorCode.addActionListener(this);

		// Set up a north panel and a center panel
		northPanel = new JPanel();
		centerPanel = new JPanel();
		this.setBackground(Color.LIGHT_GRAY);

		sortedPlan = sp;
		clientPacket = cp;
this.addWindowListener(this);
		JScrollPane scroller = new JScrollPane(centerPanel);
		//scroller.setPreferredSize(new Dimension(LEGEND_WIDTH, drawingPane.getHeight()+100));
		scroller.setAutoscrolls(true);

		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		if ( treePacket.messageId == MessageIds.GET_COMPILED_PLAN_TREE ) {
			infoStr = (String[])cp.get("Info");
			setTitle("Compiled Plan Tree");
			displayType = PicassoConstants.SHOW_BOTH;
		} else {
			infoStr = (String[])cp.get("Info");
			displayType = PicassoConstants.SHOW_NONE;
		}

		if ( numOfPlans > 1 && treePacket.messageId != MessageIds.GET_COMPILED_PLAN_TREE )
			setTitle("Plan Difference");

		/*GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.HORIZONTAL;

		northPanel.setLayout(new GridBagLayout());
		northPanel.setBackground(Color.ORANGE);
		northPanel.add(diffLbl, c);

		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.NONE;
		northPanel.add(treeDiffBox, c);
		treeDiffBox.addItemListener(this);

		c.gridx = 2;
		c.gridwidth = 3;
		//JLabel emptyLbl = new JLabel("WHITE : NO DIFF, CYAN : SUB_OP DIFF, YELLOW : LEFT=RIGHT, ORANGE : ONE SIM, RED : PARENT DIFF");
		//northPanel.add(emptyLbl, c);*/
		DBSettings dbSettings = clientPacket.getDBSettings();

		if(!PicassoConstants.IS_PKT_LOADED)
		{
			for(int i=0; i<DBConstants.databases.length; i++){
				DBInfo db = DBConstants.databases[i];
				if ( dbSettings.getDbVendor().equals(db.name)){
					treeNames = db.treeNames;
					treecard = db.treecard;
				}
			}
		}
		else
		{
			DBInfo db = DBConstants.databases[4];
			treeNames = db.treeNames;
			treecard = db.treecard;
		}
		resetMatchingNos(trees);
		treeDiff(numOfPlans, trees, TreeUtil.SUB_OPERATOR_LEVEL);//treeDiffBox.getSelectedIndex());

		//centerPanel.setLocation(20, 50);
		layoutGraphs(treePacket.messageId);

		LogoPanel logoPanel = new LogoPanel();

		getContentPane().setLayout(new BorderLayout());

		if ( treePacket.messageId == MessageIds.GET_COMPILED_PLAN_TREE )
			getContentPane().add(northPanel, BorderLayout.NORTH);
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		getContentPane().add(logoPanel, BorderLayout.SOUTH);

		setSize(800, 600);
		setLocation(0, 0);
	}

	private void resetMatchingNos(Vector treescopy) {
		// This function recursively resets the match numbers of all the nodes in the tree
		for(int i = 0; i < trees.size(); i++)
		{
			TreeNode tn;
			try {
				tn = (TreeNode) treescopy.elementAt(i);
				if(tn == null)
					continue;
				tn.setMatchNumber(0);
				resetMatchingNos(tn.getChildren());
			} catch (Exception e) {
				continue;
			}
		}
	}

	public ClientPacket getClientPacket() {
		return clientPacket;
	}

	public int getTreeCount() {
		return numOfPlans;
	}

	public BufferedImage getTreeImage(int index) {
		return myGraphs[index].getImage();
	}

	public int getPlanNumber(int index) {
		return myGraphs[index].getPlanNumber();
	}

	public void layoutGraphs(int msgId) {
		centerPanel.removeAll();
		centerPanel.setLayout(new GridLayout(1, numOfPlans));

		myGraphs = new PGraph[numOfPlans];
		if ( numOfPlans == 1 ) {
			myGraphs[0] = new PGraph(this, 0, treeNames, treecard, trees, sortedPlan, false, msgId, infoStr);
			//c.fill = GridBagConstraints.BOTH;
			centerPanel.add(myGraphs[0]); //, c);
			myGraphs[0].zoomin();
		} else {
			for (int i=0; i < numOfPlans; i++) {
				myGraphs[i] = new PGraph(this, i, treeNames, treecard, trees, sortedPlan, true, msgId, infoStr);
				//c.gridx = i;
				centerPanel.add(myGraphs[i]); //, c);
			}
		}
	}

	private void treeDiff(int numOfPlans, Vector trees, int diffType) {

		if ( numOfPlans <= 1 )
			return;

		//	The first element is the plan number...
		//int planNum1 = ((Integer)trees.elementAt(1)).intValue();
		TreeNode root1 = (TreeNode)trees.elementAt(2);

		//int planNum2 = ((Integer)trees.elementAt(3)).intValue();
		TreeNode root2 = (TreeNode)trees.elementAt(4);
                /*
                 *Checking if there is one to one mapping or not
                 **/
/*                String relations1[] = TreeUtil.getRelations(root1);
                String relations2[] = TreeUtil.getRelations(root2);
                Arrays.sort(relations1);
                Arrays.sort(relations2);
                for(int i=0;i<relations1.length-1;i++)
                    if(relations1[i].equals(relations1[i+1]))
                    {relations1[i+1]=null;i++;}
                for(int i=0;i<relations2.length-1;i++)
                    if(relations2[i].equals(relations2[i+1]))
                    {relations2[i+1]=null;i++;}
                {
                    int i =0,j=0;
                    while(i<relations1.length && j<relations2.length){
                        if(relations1[i]==null)
                            {i++;continue;}
                        if(relations2[j]==null)
                            {j++;continue;}
                        if(!relations1[i].equalsIgnoreCase(relations2[j])){
                            String Message = "Relations dont match";
                            System.out.println(Message);
                            JOptionPane.showMessageDialog(this, Message);
                            return;
                        }
                        i++;j++;
                    }
                }*/
                Hashtable matching = getBestMatching(root1, root2, diffType);
		setSimilarity(root1, root2, matching);
		setFetchNodes(root1);
		setFetchNodes(root2);
		if ( checkIfTreesSame(root1) == true && checkIfTreesSame(root2) == true) {
			System.out.println("NO DIFFERENCE IN THE PLANS FOUND");
			JOptionPane.showMessageDialog(this, "NO DIFFERENCE IN THE PLANS FOUND");
		}
		//MessageUtil.CPrintToConsole("Done Processing :: Tree");
	}

	int getSingletonTreeLength(TreeNode n1, int len) {
		TreeNode p1 = n1.getParent();

		if ( p1 != null && (p1.getNodeName().equals("FETCH") 
				|| p1.getNodeName().equals("Seq Scan")
				|| p1.getNodeName().equals("TABLE ACCESS"))) // If it is a fetch treat it as a singleton..
		/*if(p1 !=null && p1.getNodeCard(treeNames, treecard)==1)*/
			return(getSingletonTreeLength(p1, len+1));

		if ( p1 == null || TreeUtil.getRightTree(p1) != null) {
			return len;
		} else
			return(getSingletonTreeLength(p1, len+1));
	}

	int getEditDistance(TreeNode n1, TreeNode n2) {
		int len1 = getSingletonTreeLength(n1, 0);
		int len2 = getSingletonTreeLength(n2, 0);

		//MessageUtil.CPrintToConsole(n1.getNodeName() + " Len1 :: " + len1 + " " + n2.getNodeName() + " LEN2 :: " + len2);
		if ( len1 == 0 )
			return len2;
		else if ( len2 == 0 )
			return len1;
		
		int[][] editDistances = new int[len1+1][len2+1];
		TreeNode t1 = n1;
		TreeNode t2 = n2;
		editDistances[0][0] = 0;
		TreeNode[] nodes1 = new TreeNode[len1+1];
		TreeNode[] nodes2 = new TreeNode[len2+1];
		for (int i=0; i <= len1; i++) {
			editDistances[i][0] = i*2;
			//MessageUtil.CPrintToConsole("T1 :: " + t1.getNodeName());
			nodes1[i] = t1;
			t1 = t1.getParent();
		}
		for ( int j=0; j <= len2; j++ ) {
			editDistances[0][j] = j*2;
			//MessageUtil.CPrintToConsole("T2 :: " + t2.getNodeName());
			nodes2[j] = t2;
			t2 = t2.getParent();
		}
		t1 = n1;
		t2 = n2;
		for (int i=1; i <= len1; i++) {
			t1 = t1.getParent();
			t2 = n2;
			for (int j=1; j <= len2; j++) {
				t2 = t2.getParent();
				int val;
				if ( TreeUtil.isEquals(t1, t2, TreeUtil.SUB_OPERATOR_LEVEL) )
					val = 0;
				else
					val = 1;
				editDistances[i][j] = getMin(editDistances[i-1][j]+2, editDistances[i][j-1]+2, editDistances[i-1][j-1]+val);
				//MessageUtil.CPrintToConsole("i " + i + " j " + j + " Edit :: " + editDistances[i][j]);
			}
		}
		return editDistances[len1][len2];
	}
	
	//	 Get the edit distance for non matching nodes with only one child
	// First we need to get the two singleton matching nodes to match
	void setEditNodes(TreeNode n1, TreeNode n2, Hashtable matching) {
		int len1 = getSingletonTreeLength(n1, 0);
		int len2 = getSingletonTreeLength(n2, 0);

		//MessageUtil.CPrintToConsole(n1.getNodeName() + " Len1 :: " + len1 + " " + n2.getNodeName() + " LEN2 :: " + len2);
		if ( len1 == 0 || len2 == 0 ) {
			//MessageUtil.CPrintToConsole(n1.getNodeName() + " Len1 :: " + len1 + " " + n2.getNodeName() + " LEN2 :: " + len2);
			TreeNode t1 = n1;
			for (int i=0; i < len1; i++) {
				t1 = t1.getParent();
				t1.setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
				//MessageUtil.CPrintToConsole(t1.getSimilarity() + " T1 :: " + t1.getNodeName());
			}

			TreeNode t2 = n2;
			for (int i=0; i < len2; i++) {
				t2 = t2.getParent();
				t2.setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
				//MessageUtil.CPrintToConsole(t2.getSimilarity() + " T2 :: " + t2.getNodeName());
			}

			return;
		}

		int[][] editDistances = new int[len1+1][len2+1];
		TreeNode t1 = n1;
		TreeNode t2 = n2;
		editDistances[0][0] = 0;
		TreeNode[] nodes1 = new TreeNode[len1+1];
		TreeNode[] nodes2 = new TreeNode[len2+1];
		for (int i=0; i <= len1; i++) {
			editDistances[i][0] = i*2;
			//MessageUtil.CPrintToConsole("T1 :: " + t1.getNodeName());
			nodes1[i] = t1;
			t1 = t1.getParent();
		}
		for ( int j=0; j <= len2; j++ ) {
			editDistances[0][j] = j*2;
			//MessageUtil.CPrintToConsole("T2 :: " + t2.getNodeName());
			nodes2[j] = t2;
			t2 = t2.getParent();
		}
		t1 = n1;
		t2 = n2;
		for (int i=1; i <= len1; i++) {
			t1 = t1.getParent();
			t2 = n2;
			for (int j=1; j <= len2; j++) {
				t2 = t2.getParent();
				int val;
				if ( TreeUtil.isEquals(t1, t2, TreeUtil.SUB_OPERATOR_LEVEL) )
					val = 0;
				else
					val = 1;
				editDistances[i][j] = getMin(editDistances[i-1][j]+2, editDistances[i][j-1]+2, editDistances[i-1][j-1]+val);
				//MessageUtil.CPrintToConsole("i " + i + " j " + j + " Edit :: " + editDistances[i][j]);
			}
		}

		int m = len1;
		int n = len2;
		int curEdit;
		/*if ( editDistances[m][n] != 0 )
			printEditArray(editDistances);*/
		while ( m != -1 || n != -1 ) {
			curEdit = editDistances[m][n];
			if ( curEdit == 0 ) { // Set everything above to white because they are the same...
				//MessageUtil.CPrintToConsole(m + " Set to Similar from here " + n);
				for (int i=1; i <= m; i++) {
					nodes1[i].setSimilarity(PicassoConstants.T_IS_SIMILAR);
					nodes1[i].setMatchNumber(matchNum++);
					//MessageUtil.CPrintToConsole("Node1 :: " + nodes1[i]);
				}
				matchNum -= (m);
				for (int j=1; j <= n; j++) {
					nodes2[j].setSimilarity(PicassoConstants.T_IS_SIMILAR);
					nodes2[j].setMatchNumber(matchNum++);
					//MessageUtil.CPrintToConsole("Node2 :: " + nodes2[j]);
				}
				break;
			}
			// Check where we got the edit distance from..
			// If it is from m-1, n-1 and the change is 0, set color to white, else it is substitution so set it to red
			// If it is from m-1, n or m, n-1 set it to brown because it is Insert/Delete..
			int val1 = -1, val2 = -1, val3 = -1;
			if ( m != 0 && n != 0 )
				val1 = editDistances[m-1][n-1];
			if ( n != 0 )
				val2 = editDistances[m][n-1];
			if ( m != 0 )
				val3 = editDistances[m-1][n];

			int num = getMinNum(val1, val2, val3, editDistances[m][n]);
			//MessageUtil.CPrintToConsole(m + "," + n + " Edit Number :: " + num);
			switch(num) {
			case 0:
				// Could be a sub operator change..
				if ( !TreeUtil.isEquals(nodes1[m], nodes2[n], TreeUtil.SUB_OPERATOR_LEVEL) ) {
					nodes1[m].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
					nodes2[n].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
					nodes1[m].setMatchNumber(matchNum);
					nodes2[n].setMatchNumber(matchNum);
					matchNum++;
				} else {
					// No Change
					nodes1[m].setSimilarity(PicassoConstants.T_IS_SIMILAR);
					nodes2[n].setSimilarity(PicassoConstants.T_IS_SIMILAR);
					nodes1[m].setMatchNumber(matchNum);
					nodes2[n].setMatchNumber(matchNum);
					matchNum++;
				}
				//setFetchNodes(nodes1[m]);
				//setFetchNodes(nodes2[n]);
				m=m-1; n=n-1;
				break;

			case 1:
				// Labels are changed...
				// Not required, we only need to check the node itself and since it is
				// different it has to be a sub-op difference.
				//int simType = getSimilarityType(nodes1[m], nodes2[n]);
				
				if ( nodes1[m].getNodeName().equals(nodes2[n].getNodeName()) ) { // Sub op has to be diff
					nodes1[m].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
					nodes2[n].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
					nodes1[m].setMatchNumber(matchNum);
					nodes2[n].setMatchNumber(matchNum);
					matchNum++;
				} else {
					nodes1[m].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					nodes2[n].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
				}
				//MessageUtil.CPrintToConsole(nodes1[m].getNodeName() + " Node Change :: " + nodes2[n].getNodeName());
				//setFetchNodes(nodes1[m]);
				//setFetchNodes(nodes2[n]);
				m=m-1; n=n-1;
				break;

			case 2 :
				nodes2[n].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
				//MessageUtil.CPrintToConsole(n + " Node Insert :: " + nodes2[n].getNodeName());
				n=n-1;
				break;

			case 3 :
				nodes1[m].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
				//MessageUtil.CPrintToConsole(m + " Node Delete :: " + nodes1[m].getNodeName());
				m=m-1;
				break;
			}

		}
		//MessageUtil.CPrintToConsole("Edit Distance :: " + editDistances[len1-1][len2-1]);
		return;
	}

	void printEditArray(int[][] editDistances) {
		for (int i=0; i < editDistances.length; i++) {
			System.out.print(i);
			for (int j=0; j < editDistances[i].length; j++) {
				System.out.print(" " + editDistances[i][j]);
			}
			System.out.println("");
		}
	}

	void setFetchNodes(TreeNode node) {
		// Go through the entire and check for fetch nodes and set them..
		if ( node == null )
			return;

/*		if ( node.getNodeName().equalsIgnoreCase("FETCH") ) {
			TreeNode scan = TreeUtil.getRightTree(node);
			scan.setSimilarity(node.getSimilarity());

		}*/
		Vector children = node.getChildren();

		//MessageUtil.CPrintToConsole("In Insert Nodes : " + tree.getNodeName());

		if ( children != null && children.size() != 0 ) {
			//MessageUtil.CPrintToConsole("Children : " + children.size());
			//int i = children.size()-1;
			for (int i=0; i < children.size() ; i++) {
				//while ( i >= 0 ) {
				TreeNode childNode = (TreeNode)children.elementAt(i);
				if ( childNode == null ) {
					MessageUtil.CPrintErrToConsole("Null Child: Error in Server Tree Creation ");
					continue;
				}
				setFetchNodes(childNode);
			}
		}
	}

	boolean checkIfTreesSame(TreeNode root1) {
		if ( root1.getSimilarity() == PicassoConstants.T_IS_SIMILAR ) {
			Vector ch = root1.getChildren();
			for (int i=0; i < ch.size(); i++) {
				if ( checkIfTreesSame((TreeNode)ch.elementAt(i)) == false )
					return false;
			}
			return true;
		} else return false;
	}
	
	int getMin(int val1, int val2, int val3) {
		int min = val1;
		if ( min > val2 ) {
			min = val2;
		}
		if ( min > val3 ) {
			min = val3;
		}
		return min;
	}

	int getMinNum(int val1, int val2, int val3, int orig) {
		int retVal = -1;

		if ( orig == val1 )
			retVal = 0;
		if ( orig == val1+1 )
			retVal = 1;
		if ( orig == (val2+2) ) {
			if ( retVal != -1 ) { // There is a clash, get the minimum value index
				if ( val2 < val1 )
					retVal = 2;
			} else
				retVal = 2;
		}
		if ( orig == (val3+2) ) {
			if ( retVal != -1 ) { // There is a clash, get the minimum value index
				if ( val3 < val1 )
					retVal = 3;
			} else
				retVal = 3;
		}
		if ( retVal == -1 ) {
			MessageUtil.CPrintToConsole("Return is -1 val1 :: " + val1 + " val2 :: " + val2 + " val3 :: " + val3 + " orig :: " + orig);
		}
		return retVal;

		//return minNum;
	}

	int getSimilarityType(TreeNode n1, TreeNode n2, boolean relations) {
		TreeNode l1 = TreeUtil.getLeftTree(n1);
		TreeNode l2 = TreeUtil.getLeftTree(n2);
		TreeNode r1 = TreeUtil.getRightTree(n1);
		TreeNode r2 = TreeUtil.getRightTree(n2);

		boolean ll, rr, lr, rl;
		if ( relations == true ) {
			ll = getRelationMatch(l1, l2);
			rr = getRelationMatch(r1, r2);
			lr = getRelationMatch(l1, r2);
			rl = getRelationMatch(r1, l2);
		} else {
			ll = TreeUtil.areTreesEqual(l1, l2);
			rr = TreeUtil.areTreesEqual(r1, r2);
			lr = TreeUtil.areTreesEqual(l1, r2);
			rl = TreeUtil.areTreesEqual(r1, l2);
		}
		//MessageUtil.CPrintToConsole("LL :: " + ll + " RR :: " + rr + " LR :: " + lr + " RL :: " + rl);

		// Need to set up truth table here to make the code less confusing..

                int subop=0;
                if ( TreeUtil.isEquals(n1, n2, TreeUtil.OPERATOR_LEVEL)) {
			if ( !TreeUtil.isEquals(n1, n2, TreeUtil.SUB_OPERATOR_LEVEL))
				subop = SUBOP_OFFSET; // Sub operators are not the same...
			if ( ll == false ) { // l1, l2 == false
				if ( lr == true ) { // ll is false, but lr is true
					if (rl == true) // lr = true and rl = true
						return(PicassoConstants.T_LEFT_EQ_RIGHT+subop);
					else // lr = true && rl = false
						return(PicassoConstants.T_LR_SIMILAR+subop);
				} else if (rl == true ) // lr is false, rl is true
					return(PicassoConstants.T_RL_SIMILAR+subop);
				if ( rr == false) // l1, l2 == false, r1, r2 == false
					return(PicassoConstants.T_NO_CHILD_SIMILAR+subop);
				else // ll is false, rr is true
					return(PicassoConstants.T_RIGHT_SIMILAR+subop);
			} else { // l1, l2 == true
				if ( rr == false )
					return(PicassoConstants.T_LEFT_SIMILAR+subop); // ll is true, rr is false
				else if ( TreeUtil.isEquals(n1, n2, TreeUtil.SUB_OPERATOR_LEVEL) == false ) // Only sub-operator is diffrent
					return(PicassoConstants.T_SUB_OP_DIF);
				else
					return(PicassoConstants.T_IS_SIMILAR); // Should not come here...
			}
		} else {
			if ( ll == false ) {
				if ( lr == true ) { // l1, l2 == false, l1, r2 == true
					if ( rl == true) // Left and right are swapped
						return(PicassoConstants.T_NP_LEFT_EQ_RIGHT);
					else return(PicassoConstants.T_NP_LR_SIMILAR); // l1, r2 == true but not r1, l2
				} else if (rl == true ) // l1, r2 == false
					return(PicassoConstants.T_NP_RL_SIMILAR);
				if (rr == false)
					return(PicassoConstants.T_NP_NOT_SIMILAR);
				else
					return(PicassoConstants.T_NP_RIGHT_SIMILAR);
			} else { // l1, l2 == true
				if ( rr == false)
					return(PicassoConstants.T_NP_LEFT_SIMILAR);
				else
					return(PicassoConstants.T_NP_SIMILAR);
			}
		}
	}

	void getNonMatchingNodes(Vector entries, TreeNode root) {
		if (root.getSimilarity() != PicassoConstants.T_IS_SIMILAR) {
			entries.add(root);
		}
		Vector children = root.getChildren();
		for (int i=0; i < children.size(); i++) {
			getNonMatchingNodes(entries, (TreeNode)(children.elementAt(i)));
		}
	}

	void setSimilarity(TreeNode root1, TreeNode root2, Hashtable matching)
{
		//show/mark the matching nodes with the help of similarity-box

		Object[] keys = matching.keySet().toArray();
		//boolean[] done = new boolean[keys.length];

		for (int i=0; i < keys.length; i++) {
			TreeNode p1 = (TreeNode)keys[i];
			TreeNode p2 = (TreeNode)matching.get(keys[i]);
			p1.setSimilarity(PicassoConstants.T_IS_SIMILAR);
			p2.setSimilarity(PicassoConstants.T_IS_SIMILAR);
			/*if ( p1.getNodeName().equals("FETCH") )
				MessageUtil.CPrintToConsole((TreeUtil.getLeftTree(p1)).getNodeName() + " FETCH Child " + (TreeUtil.getLeftTree(p2)).getNodeName());*/
			//MessageUtil.CPrintToConsole(p1.getNodeName() + " Tree 2 :: " + p2.getNodeName());
		}
	}

	void setMatchNumber(TreeNode node1, TreeNode node2) {
		node1.setMatchNumber(matchNum);
		node2.setMatchNumber(matchNum);
		matchNum++;
	}

	/** calculate the best matching between two trees **/
	Hashtable getBestMatching (TreeNode tree1, TreeNode tree2, int diffType)
	{
		// initialise the matching table
		Hashtable matching = new Hashtable();
		// find the matching between leaves, here leaves are tables and indexes
		TreeNode[] leaves1 = TreeUtil.getLeaves(tree1);
		TreeNode[] leaves2 = TreeUtil.getLeaves(tree2);

		boolean[] done = new boolean[leaves2.length];
		for (int j=0; j < leaves2.length; j++)
			done[j] = false;
		
		boolean[] idone = new boolean[leaves1.length];
		for (int i=0; i < leaves1.length; i++)
			idone[i] = false;
		
		// Check for duplicates in this and pick the one with the mimimum edit distance
		doDuplicateLeafMatch(matching, leaves1, leaves2, idone, done);
		
		for (int i=0; i<leaves1.length; i++) {
			if ( idone[i] == true )
				continue;
			for (int j=0; j<leaves2.length; j++)
			{
				if ( done[j] == true )
					continue;
				if ( TreeUtil.isEquals(leaves1[i], leaves2[j], diffType) )
				{
					//MessageUtil.CPrintToConsole("I :: " + i + " J :: " + j + leaves2[j].getNodeName() + " Adding :: " + leaves1[i].getNodeName());
					matching.put (leaves1[i], leaves2[j]);
					setMatchNumber(leaves1[i], leaves2[j]);
					done[j] = true;
					break;
				}
			}
		}

		//find the matching between join-nodes. Join nodes are matched if join
		//method and tables getting joined are same.
		TreeNode[] joins1 = TreeUtil.getJoinNodes(tree1,treeNames,treecard);
		TreeNode[] joins2 = TreeUtil.getJoinNodes(tree2,treeNames,treecard);

		done = new boolean[joins2.length];
		for (int j=0; j < joins2.length; j++) {
			done[j] = false;
			//MessageUtil.CPrintToConsole(joins2[j].getNodeName() + " CARD :: " + joins2[j].getCardinality());
		}

		idone = new boolean[joins1.length];
		for (int i=0; i < joins1.length; i++) {
			idone[i] = false;
			//MessageUtil.CPrintToConsole(joins1[i].getNodeName() + " ICARD :: " + joins1[i].getCardinality());
		}
		for (int i=0; i<joins1.length; i++)
			for (int j=0; j<joins2.length; j++)
			{
				if ( done[j] == true )
					continue;

				if (TreeUtil.isEquals(joins1[i], joins2[j], diffType) == false)
					continue;

				TreeNode t1 = TreeUtil.getSubTree(tree1, joins1[i]);
				TreeNode t2 = TreeUtil.getSubTree(tree2, joins2[j]);

				boolean isMatching = true;

				// check for matching in left relation
				TreeNode left1 = TreeUtil.getLeftTree(t1);
				TreeNode left2 = TreeUtil.getLeftTree(t2);
				String[] rel1 = TreeUtil.getRelations(left1);
				String[] rel2 = TreeUtil.getRelations(left2);

				if (rel1.length != rel2.length)
					isMatching = false;

				/*for (int ii=0; isMatching && ii < rel1.length; ii++) {
					MessageUtil.CPrintToConsole("REL 1 :: " + rel1[ii]);
					MessageUtil.CPrintToConsole("REL 2 :: " + rel2[ii]);
				}*/

				for (int ii=0; isMatching && ii < rel1.length; ii++)
				{
					int jj;
					for (jj=0; jj < rel2.length; jj++)
						if (rel1[ii].equals(rel2[jj]))
							break;

					if (jj == rel2.length)
						isMatching = false;
				}

				if (isMatching == false)
					// left relations are not matching
					continue;

				// check for matching in right relations
				TreeNode right1 = TreeUtil.getRightTree(t1);
				TreeNode right2 = TreeUtil.getRightTree(t2);
				rel1 = TreeUtil.getRelations(right1);
				rel2 = TreeUtil.getRelations(right2);

				if (rel1.length != rel2.length)
					isMatching = false;

				for (int ii=0; isMatching && ii < rel1.length; ii++)
				{
					int jj;
					for (jj=0; jj < rel2.length; jj++)
						if (rel1[ii].equals(rel2[jj]))
							break;

					if (jj == rel2.length)
						isMatching = false;
				}


				if (isMatching)
				{
					// both left/right relations and join method matched
					matching.put(joins1[i], joins2[j]);
					setMatchNumber(joins1[i], joins2[j]);
					done[j] = true;
					idone[i] = true;
					break;
				}
			}

		// Get the non match join nodes for both trees
		setJoinSimilarity(joins1, joins2, idone, done, matching);

		if (tree1.getChildren().size()<=1 && matching.containsKey(tree1) == false) {
			matching.put(tree1, tree2);
			setMatchNumber(tree1, tree2);
		}

		Object[] keys = matching.keySet().toArray();
		done = new boolean[keys.length];
		for (int i=0; i < keys.length; i++) {
			done[i] = false;
		}

		/* This is done to get the edit distance for each of the singleton trees */
		for (int i=0; i < keys.length; i++) {
			if (done[i] == true)	continue;

			TreeNode t1 = (TreeNode)keys[i];
			TreeNode t2 = (TreeNode)matching.get(keys[i]);

			setEditNodes(t1, t2, matching);
		}
		Vector entries = new Vector();
		getNonMatchingNodes(entries, tree1);
		if ( entries.size() == 0 ) {
			getNonMatchingNodes(entries, tree1);
			if ( entries.size() == 0 ) {
				MessageUtil.CPrintToConsole("Trees Not Different :: Error in Code");
			}
		}
		return matching;
	}

	boolean getRelationMatch(TreeNode node1, TreeNode node2) {
		boolean isMatching=true;
		String[] rel1 = TreeUtil.getRelations(node1);
		String[] rel2 = TreeUtil.getRelations(node2);
		boolean[] done2 = new boolean[rel2.length];

		if (rel1.length != rel2.length)
			isMatching = false;

		for (int ii=0; isMatching && ii < rel1.length; ii++) {
			//MessageUtil.CPrintToConsole("REL 1 :: " + rel1[ii]);
			//MessageUtil.CPrintToConsole("REL 2 :: " + rel2[ii]);
			//MessageUtil.CPrintToConsole("----------------------------------");
			done2[ii] = false;
		}

		for (int ii=0; isMatching && ii < rel1.length; ii++)
		{
			int jj;
			for (jj=0; jj < rel2.length; jj++) {
				if (rel1[ii].equals(rel2[jj]) && done2[jj] != true) {
					done2[jj] = true;
					break;
				}
			}

			if (jj == rel2.length)
				isMatching = false;
		}
		return isMatching;
	}

	void doDuplicateLeafMatch(Hashtable matching, TreeNode[] leaves1, TreeNode[] leaves2, boolean[] idone, boolean[] jdone) {
		for (int i=0; i<leaves1.length; i++) {
			if ( idone[i] == true )
				continue;
			int[] jdup = new int[leaves2.length];
			int jcount=0;
			for (int j=0; j<leaves2.length; j++) {
				if ( leaves1[i].getNodeName().equals(leaves2[j].getNodeName()) && jdone[j] == false )
					jdup[jcount++] = j;
			}
			if ( jcount > 1 ) {
				int[] idup = new int[leaves1.length];
				int icount = 0;
				// Get all the leaves1 duplicates as weel to get the best match
				for (int j=i; j < leaves1.length; j++) {
					if (leaves1[i].getNodeName().equals(leaves1[j].getNodeName())) {
						idup[icount++] = j;
					}	
				}
				// Compute edit distance for all of them
				int minEditDistance = 100;
				int mj = -1;
				int mi = -1;
				int[][] editDist = new int[icount][jcount];
				for (int l=0; l < icount; l++) {
					for (int j=0; j < jcount; j++) {
						editDist[l][j] = getEditDistance(leaves1[idup[l]], leaves2[jdup[j]]);
						if ( minEditDistance > editDist[l][j] ) {
							minEditDistance = editDist[l][j];
							mi = l; mj = j;
						}
					}
				}
				// Put the first one in and then do the rest again in a similar fashion..
				// Match for this is mj and mi
				while (icount != 0) {
					matching.put (leaves1[idup[mi]], leaves2[jdup[mj]]);
					setMatchNumber(leaves1[idup[mi]], leaves2[jdup[mj]]);
					jdone[jdup[mj]] = true;
					idone[idup[mi]] = true;
					editDist[mi][mj] = icount+jcount; // set to a high number
					minEditDistance = icount+jcount;
					mi = -1; mj = -1;
					for (int l=0; l < icount; l++) {
						for (int j=0; j < jcount; j++) {
							if ( minEditDistance > editDist[l][j] 
							      && idone[idup[l]] == false && jdone[jdup[j]] == false) {
								minEditDistance = editDist[l][j];
								mi = l; mj = j;
							}
						}
					}
					if ( mi == -1 || mj == -1 )
						break;
					// get next i and j
					icount--;
				}
			} // end of k > 1
		}
	}
	
	Vector getAllChildren(TreeNode node, Vector children) {
		if ( node == null )
			return children;

		Vector ch = node.getChildren();
		for (int i=0; i < ch.size(); i++) {
			children.add(ch.elementAt(i));
		}
		for (int i=0; i < ch.size(); i++) {
			getAllChildren((TreeNode)ch.elementAt(i), children);
		}
		return children;
	}

	void getExactRelationMatch(TreeNode[] joins1, TreeNode[] joins2, boolean[] idone, boolean[] jdone, Hashtable matching) {
		for (int i=0; i < joins1.length; i++) {
			if ( idone[i] == true )
				continue;
			
			TreeNode node1 = joins1[i];
			int csimType = PicassoConstants.T_NO_DIFF_DONE;
			// Get an array of all relational # match
			for (int j=0; j < joins2.length; j++) {
				if ( jdone[j] == true )
					continue;
				TreeNode node2 = joins2[j];
				if ( getRelationMatch(node1, node2) == false )
					continue;
				csimType = getSimilarityType(node1, node2, true);
				setJoinSimType(node1, node2, csimType);
				jdone[j] = true;
				idone[i] = true;
				setEditNodes(node1, node2, matching);
				break;
			}
		}
	}
	
	void getCountRelationMatch(TreeNode[] joins1, TreeNode[] joins2, boolean[] idone, boolean[] jdone, Hashtable matching) {
		for (int i=0; i < joins1.length; i++) {
			if ( idone[i] == true )
				continue;
			
			TreeNode node1 = joins1[i];
			int csimType = PicassoConstants.T_NO_DIFF_DONE;
			String[] rel1 = TreeUtil.getRelations(node1);
			int simType;
			TreeNode cnode2 = null;
			int jindex = -1;
			
			// Get an array of all relational # match
			for (int j=0; j < joins2.length; j++) {
				if ( jdone[j] == true )
					continue;
				TreeNode node2 = joins2[j];
				String[] rel2 = TreeUtil.getRelations(joins2[j]);
				if ( rel1.length != rel2.length )
					continue;
				simType = getSimilarityType(node1, node2, true);
				int subop=0;
				if ( !TreeUtil.isEquals(node1, node2, TreeUtil.SUB_OPERATOR_LEVEL))
				    subop =  SUBOP_OFFSET;
				if ( simType < csimType+subop ) {
					csimType = simType;
					cnode2 = node2;
					jindex = j;
				}
			}
			if ( cnode2 != null ) {
				//MessageUtil.CPrintToConsole(node1.getNodeName() + " In Count match :: " + cnode2.getNodeName());
				setJoinSimType(node1, cnode2, csimType);
				jdone[jindex] = true;
				idone[i] = true;
				setEditNodes(node1, cnode2, matching);
			}
		}
	}
	
	void getNonExactJoinMatch(TreeNode[] joins1, TreeNode[] joins2, boolean[] idone, boolean[] jdone, Hashtable matching) {
		for (int i=0; i < joins1.length; i++) {
			if ( idone[i] == true )
				continue;
			
			joins1[i].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
		}

		for (int j=0; j < joins2.length; j++) {
			if ( jdone[j] == true )
				continue;
			joins2[j].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
		}
	}
	
	int matchNum = 1;
	void setJoinSimilarity(TreeNode[] joins1, TreeNode[] joins2, boolean[] idone, boolean[] jdone, Hashtable matching) {
		// Do two passes. In the first pass get exact relation matches, then in the next pass
		// take the unmatched nodes and do the count and group them all together.
		getExactRelationMatch(joins1, joins2, idone, jdone, matching);
		getCountRelationMatch(joins1, joins2, idone, jdone, matching);
		getNonExactJoinMatch(joins1, joins2, idone, jdone, matching);
	}

	void setJoinSimType(TreeNode node1, TreeNode node2, int simType) {
		if ( simType == PicassoConstants.T_NP_LR_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_NP_LEFT_EQ);
			node2.setSimilarity(PicassoConstants.T_NP_RIGHT_EQ);
		} else if (simType == PicassoConstants.T_NP_RL_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_NP_RIGHT_EQ);
			node2.setSimilarity(PicassoConstants.T_NP_LEFT_EQ);
		} else if (simType == PicassoConstants.T_RL_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_RIGHT_EQ);
			node2.setSimilarity(PicassoConstants.T_LEFT_EQ);
		} else if ( simType == PicassoConstants.T_LR_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_LEFT_EQ);
			node2.setSimilarity(PicassoConstants.T_RIGHT_EQ);
		} else if (simType == PicassoConstants.T_SO_RL_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_SO_RIGHT_SIMILAR);
			node2.setSimilarity(PicassoConstants.T_SO_LEFT_SIMILAR);
		} else if ( simType == PicassoConstants.T_SO_LR_SIMILAR ) {
			node1.setSimilarity(PicassoConstants.T_SO_LEFT_SIMILAR);
			node2.setSimilarity(PicassoConstants.T_SO_RIGHT_SIMILAR);
		} else {
			node1.setSimilarity(simType);
			node2.setSimilarity(simType);
		}
		node1.setMatchNumber(matchNum);
		node2.setMatchNumber(matchNum);
		matchNum++;
	}
	
	int getSameCardinality(TreeNode node1, TreeNode[] joins) {
		for (int i=0; i < joins.length; i++) {
			if ( joins[i].getCardinality() == node1.getCardinality() )
				return i;
		}
		return -1;
	}
   
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();
		if ( myGraphs == null )
			return;
		if ( source == print ) {
			new PicassoPrint(this).printPicasso();
		} else if ( source == preview ) {
			new PicassoPrint(this).previewPicasso();
		} else if ( source == save )
			PicassoSave.save(this);
		else if ( source == colorCode )
			{	
			cCodelabel.setSize(50,50);
			cCodelabel.setEditable(false);
			cCodelabel.setFont(font);
			cCodelabel.setBackground(new Color(239,235,231));
			cCode.setTitle("Plan Diff Color Code");
	cCode.setSize(400,250);
	cCode.setAlwaysOnTop(true);
	cCode.setResizable(false);
	//cCode.setLocation(arg0, arg1)
			cCode.add(cCodelabel);
		cCode.setVisible(true);
		}	/*JOptionPane.showMessageDialog(this, "Identical Nodes: White Color/Black Links" +
					"\n\nSimilar Nodes (Same Operator): White Fill" +
					"\n   Parameter Different: Green Border/Black Links" +
					"\n   Left/Right Relations Swapped: Orange Border/Blue Links" +
					"\n   Left/Right Relations Different: Orange Border/Red Links" +
					"\n\nMapped Nodes (Different Operators): Native Fill/Red Border" +
					"\n   Left/Right Relations Swapped: Blue Links" +
					"\n   Left/Right Relations Different: Red Links" +
					"\n\nUnmapped Nodes: Native Color/Black Links", "Plan Diff Color Code",JOptionPane.INFORMATION_MESSAGE);
	*/	else if ( source == about )
			new AboutPicassoFrame().setVisible(true);
		else if ( source == contentHelp )
			PicassoUtil.openURL("file://" + System.getProperty("user.dir") + "/PicassoDoc/usage/PlanTree.htm");
		else if ( source == exit )
		{	cCode.dispose();
			this.dispose();
		}//MessageUtil.CPrintToConsole("Graph Scale === " + graph.getScale());
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
	//cCode.dispose();	
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
cCode.dispose();

		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}


/*
 * # # # PROGRAM INFORMATION # # # Copyright (C) 2006 Indian Institute of
 * Science, Bangalore, India. # All rights reserved. # # This program is part of
 * the Picasso Database Query Optimizer Visualizer # software distribution
 * invented at the Database Systems Lab, Indian # Institute of Science (PI:
 * Prof. Jayant R. Haritsa). The software is # free and its use is governed by
 * the licensing agreement set up between # the copyright owner, Indian
 * Institute of Science, and the licensee. # The software is distributed without
 * any warranty; without even the # implied warranty of merchantability or
 * fitness for a particular purpose. # The software includes external code
 * modules, whose use is governed by # their own licensing conditions, which can
 * be found in the Licenses file # of the Docs directory of the distribution. # # #
 * The official project web-site is #
 * http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html # and the email
 * contact address is # picasso@dsl.serc.iisc.ernet.in # #
 */
//file added for multiplan
package iisc.dsl.picasso.client.panel;

//added for multiplan
import iisc.dsl.picasso.client.frame.PlanTreeFrame_DifEngines;
//addition ends here
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.TreeNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.tree.JGraphTreeLayout;

public class PGraph_DifEngines extends JPanel implements ActionListener,
		MouseListener, ItemListener {

	private static final long serialVersionUID = 2440603754587872956L;

	JGraph graph = null;

	GraphModel model = null;

	GraphLayoutCache view = null;

	JPanel infoPanel, diffPanel;

	String[] treeNames;

	Vector diffLabels;

	boolean isMultiple;

	int msgId;

	int clientPlanNumber, numOfPlans, serverPlanNumber;

	JTextArea operatorLbl;

	JButton zoomin, zoomout;

	JComboBox treeDiffBox;

	JScrollPane scroller = null;

	int planId;

	Vector trees;

	PlanTreeFrame_DifEngines parent;

	DefaultGraphCell[] cells;

public PGraph_DifEngines(PlanTreeFrame_DifEngines ptf, int pid, String[] tn, Vector ts, int[][] sortedPlan, boolean im, int msg, String[] infoStr,String[] optvals,java.util.ArrayList sel_val_attr_arrlist, boolean sameEngine, String engine) {
		treeNames = tn;
		diffLabels = new Vector();
		isMultiple = im;
		msgId = msg;
		planId = pid;
		trees = ts;
		parent = ptf;
		numOfPlans = ((Integer)trees.elementAt(0)).intValue();
		
		serverPlanNumber = ((Integer)trees.elementAt((planId*2)+1)).intValue();
		clientPlanNumber = sortedPlan[0][serverPlanNumber];
		 
//		Zoom In and Zoom Out
		URL zoomInUrl = getClass().getClassLoader().getResource(PicassoConstants.ZOOM_IN_IMAGE);
		URL zoomOutUrl = getClass().getClassLoader().getResource(PicassoConstants.ZOOM_OUT_IMAGE);
		
		if ( zoomInUrl == null || zoomOutUrl == null ) {
			zoomin = new JButton("Zoom In");
			zoomout = new JButton("Zoom Out");
		} else {
			ImageIcon zoomInIcon = new ImageIcon(zoomInUrl);
			zoomin = new JButton(zoomInIcon);
			ImageIcon zoomOutIcon = new ImageIcon(zoomOutUrl);
			zoomout = new JButton(zoomOutIcon);
			zoomin.setPreferredSize(new Dimension(zoomInIcon.getIconWidth(), zoomInIcon.getIconHeight()));
			zoomout.setPreferredSize(new Dimension(zoomOutIcon.getIconWidth(), zoomOutIcon.getIconHeight()));
		}
		zoomin.addActionListener(this);
		zoomout.addActionListener(this);
		
		infoPanel = new JPanel();
		
		JLabel diffLbl = new JLabel("Display Type: ", JLabel.RIGHT);
		treeDiffBox = new JComboBox();
		treeDiffBox.addItem("Both");
		treeDiffBox.addItem("Cost");
		treeDiffBox.addItem("Cardinality");
		treeDiffBox.addItem("None");
		treeDiffBox.addItemListener(this);
		
			diffLbl.setVisible(false);
			
			// The following treediffbox is made visible now to be able to select what all info. (either both cost card or one or none of them can be seen in the trees) 
			treeDiffBox.setVisible(true);
			treeDiffBox.setSelectedIndex(0); // By default, both cost and cardinality are shown
		
		
		JTextField txt = new JTextField("  ");
		txt.setEditable(false);
		JLabel lbl;
		JLabel lbl2 = new JLabel("");
		JLabel lbl12 = new JLabel("");
		String[] selvalues = (String [])sel_val_attr_arrlist.get(pid*4);
		String[] attrnames = (String [])sel_val_attr_arrlist.get(pid*4+1);
		String[] cost = (String [])sel_val_attr_arrlist.get(pid*4+2);
		String[] card = (String [])sel_val_attr_arrlist.get(pid*4+3);
		/*String valnames = "";
		for(int i=0;i<selvalues.length;i++)
		{
			valnames += selvalues[i];
		 	if(i!=selvalues.length-1)
		 	{
		 		valnames+=";";
		 	}
		 		
		}
		String atnames = "";
		for(int i=0;i<attrnames.length;i++)
		{
		 	atnames += attrnames[i];
		 	if(i!=attrnames.length-1)
		 	{
		 		atnames+=";";
		 	}
		 		
		}*/
		//String selecs = "Selectivity("++""++")";
		if(pid==0)
		{
		lbl = new JLabel("Foreign Plan (" + infoStr[pid]+":"+parent.optvals[0] 
				+ ")"  , JLabel.LEFT);
		txt.setVisible(false);
                }
		else
		{//original database plan
		lbl = new JLabel("Plan: P" + (clientPlanNumber+1) 
				+ " (" + infoStr[pid]+ ":" + parent.optvals[1]+")", JLabel.LEFT);
		txt.setBackground(new Color(PicassoConstants.color[clientPlanNumber%PicassoConstants.color.length]));
		}
                JTextArea sInfoLbl = new JTextArea("");
                sInfoLbl.setFont(new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, 12));
                sInfoLbl.setBackground(Color.ORANGE);
                sInfoLbl.setEditable(false);
                // if(sameEngine)
                sInfoLbl.setText(attrnames[0]+" (Selectivity: "+parent.selec11+", Constant: "+selvalues[0]+" )\n"+attrnames[1]+" (Selectivity: "+parent.selec12+", Constant: "+selvalues[1]+" )\n"); 
                					// "Cost (in " + engine + " units): " + cost[0] + "\n" + "Cardinality: " + card[0]);
                /*else
                	sInfoLbl.setText(attrnames[0]+" (Selectivity: "+parent.selec11+", Constant: "+selvalues[0]+" )\n"+attrnames[1]+" (Selectivity: "+parent.selec12+", Constant: "+selvalues[1]+" )");*/
		//lbl12 = new JLabel(attrnames[0]+" ( "+parent.selec11+" , "+selvalues[0]+" )");//(" SELECTIVITY VALUES :("+parent.selec11+" , "+parent.selec12+")");
		//lbl2 =  new JLabel(attrnames[1]+" ( "+parent.selec11+" , "+selvalues[1]+" )");//("("+atnames+")::("+valnames+")");*/
		lbl.setFont(new Font(PicassoConstants.LEGEND_FONT, Font.PLAIN, 14));
//		lbl.setForeground(new Color(Constants.color[planColor]));
		//lbl12.setBackground(Color.RED);
		//lbl12.setForeground(Color.blue);
		
		
		operatorLbl = new JTextArea("");
		operatorLbl.setFont(new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, 10));
		operatorLbl.setForeground(Color.BLACK); //new
												// Color(Constants.color[planColor]));
		operatorLbl.setBackground(Color.ORANGE);
		operatorLbl.setEditable(false);
		
		//JPanel topInfoPanel1 = new JPanel(new FlowLayout());
		JPanel topInfoPanel = new JPanel(new FlowLayout());
		infoPanel.setBackground(Color.ORANGE);
		topInfoPanel.setBackground(Color.ORANGE);
		topInfoPanel.add(diffLbl);
		topInfoPanel.add(treeDiffBox);
		topInfoPanel.add(txt);
		topInfoPanel.add(lbl);
		
		topInfoPanel.add(zoomin);
		topInfoPanel.add(zoomout);
		//topInfoPanel1.add(topInfoPanel);
		//topInfoPanel1.add(lbl2);
		
		infoPanel.setLayout(new BorderLayout());
		JPanel bottomInfoPanel = new JPanel();
		bottomInfoPanel.setBackground(Color.ORANGE);
		
		infoPanel.add(topInfoPanel, BorderLayout.NORTH);
		infoPanel.add(bottomInfoPanel, BorderLayout.CENTER);
		
/*		if ( infoStr[1].length() == 0 ) {
			bottomInfoPanel.setLayout(new GridLayout(3,1));
		} else {
			bottomInfoPanel.setLayout(new GridLayout(4,1));
			bottomInfoPanel.add(sInfoLbl);
		}*/
		bottomInfoPanel.setLayout(new GridLayout(2,1));
                bottomInfoPanel.add(sInfoLbl);
		/*bottomInfoPanel.add(lbl12);
		bottomInfoPanel.add(lbl2);*/
		bottomInfoPanel.add(operatorLbl);
		
		this.setLayout(new BorderLayout());
		this.add(infoPanel, BorderLayout.NORTH);
		drawGraph();
	}	
                
            private void drawGraph() {
		model = new DefaultGraphModel();
		view = new GraphLayoutCache(model, new DefaultCellViewFactory());
		graph = new JGraph(model, view);

		int totalSize = 0;
		TreeNode root = (TreeNode) trees.elementAt((planId * 2) + 2);
		cells = insertTreeData(clientPlanNumber, serverPlanNumber, root, graph);
		totalSize += cells.length;

		boolean selectsAll = graph.getGraphLayoutCache()
				.isSelectsAllInsertedCells();
		boolean selectsLocal = graph.getGraphLayoutCache()
				.isSelectsLocalInsertedCells();
		graph.getGraphLayoutCache().setSelectsAllInsertedCells(false);
		graph.getGraphLayoutCache().setSelectsLocalInsertedCells(false);
		graph.getGraphLayoutCache().insert(cells);
		graph.getGraphLayoutCache().setSelectsAllInsertedCells(selectsAll);
		graph.getGraphLayoutCache().setSelectsLocalInsertedCells(selectsLocal);

		JGraphFacade facade = new JGraphFacade(graph);
                facade.setOrdered(true);
		/*JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();

		if (treeDiffBox.getSelectedIndex() == PicassoConstants.SHOW_BOTH)
			layout.setIntraCellSpacing(250);
		else if (treeDiffBox.getSelectedIndex() == PicassoConstants.SHOW_NONE)
			layout.setIntraCellSpacing(100);
		else
			layout.setIntraCellSpacing(150);

		layout.setInterRankCellSpacing(20);
		layout.isDeterministic();

		layout.setOrientation(SwingConstants.NORTH);

		layout.run(facade);*/
                JGraphTreeLayout clayout = new JGraphTreeLayout();
                clayout.setOrientation(SwingConstants.NORTH);
                clayout.setNodeDistance(50);
                //clayout.setLevelDistance(20);
                clayout.run(facade);
		Map nested = facade.createNestedMap(true, true);
		graph.getGraphLayoutCache().edit(nested);
		graph.setBackground(Color.LIGHT_GRAY);

		graph.addMouseListener(this);
		//int dummyWidth = 400 -
		// graph.getPreferredScrollableViewportSize().width;
		//MessageUtil.CPrintToConsole(this.getWidth() + " Adding dummy Panel ::
		// " + dummyWidth);
		/*
		 * if ( dummyWidth > 0 ) { JPanel dummyPanel = new JPanel();
		 * dummyPanel.setPreferredSize(new Dimension(dummyWidth,
		 * this.getHeight())); dummyPanel.setBackground(Color.LIGHT_GRAY);
		 * this.add(dummyPanel, BorderLayout.WEST); }
		 */
		JPanel centerPanel = new JPanel();
		centerPanel.setBackground(Color.LIGHT_GRAY);
		scroller = new JScrollPane(centerPanel);

		GridBagLayout gb = new GridBagLayout();
		centerPanel.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		//if ( dummyWidth > 0 )
		//c.fill = GridBagConstraints.VERTICAL;
		//else
		//	c.fill = GridBagConstraints.BOTH;
		c.fill = GridBagConstraints.VERTICAL;

		int width = graph.getPreferredScrollableViewportSize().width;
		int height = graph.getPreferredScrollableViewportSize().height;

		//MessageUtil.CPrintToConsole("GRAPH :: " + width + " " + height);

		scroller.setPreferredSize(new Dimension(width + 100, height + 100));
		scroller.setAutoscrolls(true);
		scroller
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroller
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		centerPanel.add(graph, c);
		this.add(scroller, BorderLayout.CENTER);

		int cw = 800;
		int ch = 600;

		/*
		 * if ( numOfPlans > 1 ) { cw = 400; ch = 300; }
		 */
		double wscale = (double) cw / (double) width;
		double hscale = (double) ch / (double) height;
//		MessageUtil.CPrintToConsole(hscale + " Scale :: " + wscale);
		if (msgId == MessageIds.GET_PLAN_TREE) {
			if (wscale > 0.40)
				wscale -= 0.15;
			if (hscale > 0.40)
				hscale -= 0.15;
		}
		if (width > cw)
			graph.setScale(wscale);
		else if (height > ch)
			graph.setScale(hscale);
		else
			graph.setScale(graph.getScale() * 0.75);
		scroller.getHorizontalScrollBar().setValue(cw / 2);
	}

	public void zoomin() {
		graph.setScale(graph.getScale() * 1.3);
		scroller.getHorizontalScrollBar().setValue(graph.getWidth() / 2);
	}

	public void zoomout() {
		graph.setScale(graph.getScale() / 1.3);
		scroller.getHorizontalScrollBar().setValue(graph.getWidth() / 2);
	}

	public BufferedImage getImage() {
		return graph.getImage(null, 1);
	}

	public int getPlanNumber() {
		return clientPlanNumber;
	}

	DefaultGraphCell[] insertTreeData(int planColor, int planNumber,
			TreeNode root, JGraph graph) {
		Vector nodes = new Vector();
		Vector edges = new Vector();
		//DefaultGraphCell cell =
		insertNodesAndEdges(root, nodes, edges, new Color(
				PicassoConstants.color[planColor
						% PicassoConstants.color.length]));

		//MessageUtil.CPrintToConsole("Edges " + edges.size() + " Nodes " +
		// nodes.size());
		DefaultGraphCell[] cells = new DefaultGraphCell[nodes.size()
				+ edges.size()];
		System.arraycopy(nodes.toArray(), 0, cells, 0, nodes.size());
		System.arraycopy(edges.toArray(), 0, cells, nodes.size(), edges.size());
		return (cells);
	}

	String showAttributes(TreeNode tree, Color color) {
		Hashtable table = tree.getAttributes();

		if (tree.getSimilarity() == PicassoConstants.T_IS_SIMILAR)
			return (null);

		if (table == null) {
			table = new Hashtable();
		}

		Object[] keys = table.keySet().toArray();
		if (keys.length == 0)
			return null;

		String str = "";
		for (int i = 0; i < keys.length; i++) {
			str += (keys[i] + "=" + table.get(keys[i]) + "; ");
		}
		Font f = new Font("Courier", Font.BOLD, 12);
		JLabel lbl = new JLabel(str, JLabel.LEFT);
		lbl.setFont(f);
		lbl.setForeground(color);
		diffLabels.add(lbl);
		return str;
	}

	String getAttributeStr(TreeNode tree) {
		Hashtable table = tree.getAttributes();

		if (table == null) {
			table = new Hashtable();
		}

		Object[] keys = table.keySet().toArray();
		if(PicassoConstants.OP_LVL==true)
        	return "";
		if (keys.length == 0)
			return "No Sub-operators";

		String str = "";
		int length = 150;
		for (int i = 0; i < keys.length; i++) {
			str += (keys[i] + "=" + table.get(keys[i]) + "; ");
		}
		if (numOfPlans > 1) {
			length = 50;
		}

		String attrStr = "";
		int begin = 0;
		for (int i = 0; i < str.length() / length; i++) {
			attrStr += str.substring(begin, begin + length) + "\n  ";
			begin += length;
		}
		attrStr += str.substring(begin);
		//MessageUtil.CPrintToConsole(str + " ATTR :: " + attrStr);
		return attrStr;
	}

	void addHighlightNode(String str, Vector nodes, Vector edges,
			DefaultGraphCell parentCell) {
		DefaultGraphCell cell = new DefaultGraphCell(str);

		//GraphConstants.setBounds(cell.getAttributes(), Rectangle2D.(20, 20,
		// 40, 20));
		GraphConstants.setFont(cell.getAttributes(), new Font(
				PicassoConstants.LEGEND_FONT, Font.BOLD, 10));
		GraphConstants.setAutoSize(cell.getAttributes(), true);
		GraphConstants.setInset(cell.getAttributes(), 5);
		GraphConstants.setEditable(cell.getAttributes(), false);
		GraphConstants.setBackground(cell.getAttributes(), Color.WHITE);
		GraphConstants.setForeground(cell.getAttributes(), Color.RED);
		GraphConstants.setOpaque(cell.getAttributes(), true);
		//GraphConstants.setSize(cell.getAttributes(), new Dimension(50, 20));
		GraphConstants.setBorder(cell.getAttributes(), BorderFactory
				.createRaisedBevelBorder());
		GraphConstants.setBorderColor(cell.getAttributes(), Color.RED);

		// Add a Port
		DefaultPort port = new DefaultPort();
		cell.add(port);

		nodes.add(cell);

		DefaultEdge edge = new DefaultEdge();

		int arow = GraphConstants.ARROW_DIAMOND;
		GraphConstants.setLineColor(edge.getAttributes(), Color.CYAN);
		GraphConstants.setLineBegin(edge.getAttributes(), arow);
		GraphConstants.setBeginFill(edge.getAttributes(), true);
		GraphConstants.setLineEnd(edge.getAttributes(), arow);
		GraphConstants.setEndFill(edge.getAttributes(), true);

		edge.setSource(parentCell.getChildAt(0));
		edge.setTarget(cell.getChildAt(0));
		nodes.add(edge);
	}

	DefaultGraphCell insertNodesAndEdges(TreeNode tree, Vector nodes,
			Vector edges, Color c) {
		// First create the vertex and add to the vector
		double[] values = tree.getNodeValues();
		String[] labels = new String[values.length];

		for (int i = 0; i < values.length; i++) {
			labels[0] = "AC: " + values[0];
			labels[1] = "EC: " + values[1];
			labels[2] = "Card: " + values[2];
		}
		//		Get the attribute for similarity..
		Color cellColor, borderColor;
		Color leftEdgeColor, rightEdgeColor;
		int sim = tree.getSimilarity();

		//MessageUtil.CPrintToConsole(tree.getNodeName() + " SIM :: " + sim);
		cellColor = tree.getNodeColor(treeNames);
		Color colorRed = new Color(0xfffa6161);
		Color colorOrange = new Color(0xfffa9700);
		Color colorGreen = Color.GREEN;
		Color colorBlue = Color.BLUE;
		Color colorBlack = Color.BLACK;
		switch (sim) {
		case PicassoConstants.T_IS_SIMILAR:
			borderColor = null;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_SUB_OP_DIF:
			
			if(PicassoConstants.OP_LVL==true)
        		borderColor=null;
        	else
        		borderColor = colorGreen;
			// borderColor = colorGreen;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_LEFT_EQ_RIGHT:
			borderColor = null; //new Color (0xfffa9700+simAdd); //Color.BLUE;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = rightEdgeColor = colorBlue;
			break;
		case PicassoConstants.T_SO_LEFT_EQ_RIGHT :
            // borderColor = colorGreen;  //new Color (0xfffa9700+simAdd); //Color.BLUE;
         	 if(PicassoConstants.OP_LVL==true)
         		 borderColor=null;
          	 else
                  borderColor = colorGreen;
         	cellColor = PicassoConstants.SAME_NODE_COLOR;
             leftEdgeColor = rightEdgeColor = colorBlue;
             break;
		case PicassoConstants.T_SO_LEFT_SIMILAR :
            // borderColor = colorGreen;
         	 if(PicassoConstants.OP_LVL==true)
           	   borderColor=colorOrange;
           	   else
                       borderColor = colorGreen;
         	leftEdgeColor = colorBlack;
             rightEdgeColor = colorRed;
             cellColor = PicassoConstants.SAME_NODE_COLOR;
             break;
		case PicassoConstants.T_LEFT_SIMILAR:
			borderColor = colorOrange;
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = colorRed;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			break;
		case PicassoConstants.T_LEFT_EQ:
			borderColor = colorOrange;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = colorBlue;
			rightEdgeColor = colorRed; //new Color (0xfffa9700+simAdd);
			break;
		case PicassoConstants.T_RIGHT_SIMILAR:
			borderColor = colorOrange;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = colorRed;
			rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_SO_RIGHT_SIMILAR :
            // borderColor = colorGreen;
         	 if(PicassoConstants.OP_LVL==true)
           	   borderColor=colorOrange;
           	 else
               borderColor = colorGreen;
         	cellColor = PicassoConstants.SAME_NODE_COLOR;
             leftEdgeColor = colorRed;
             rightEdgeColor = colorBlack;
             break;
		case PicassoConstants.T_RIGHT_EQ:
			borderColor = colorOrange;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = colorRed; //new Color (0xfffa9700+simAdd);
			rightEdgeColor = colorBlue;
			break;
		case PicassoConstants.T_NO_CHILD_SIMILAR:
			borderColor = colorOrange;
			cellColor = PicassoConstants.SAME_NODE_COLOR;
			leftEdgeColor = colorRed;
			rightEdgeColor = colorRed;
			break;
		 case PicassoConstants.T_SO_NO_CHILD_SIMILAR :
             //  borderColor = colorGreen;
           	 if(PicassoConstants.OP_LVL==true)
             	   borderColor=colorOrange;
             	   else
                         borderColor = colorGreen;
           	cellColor = PicassoConstants.SAME_NODE_COLOR;
               leftEdgeColor = colorRed;
               rightEdgeColor = colorRed;
               break;
		case PicassoConstants.T_NP_SIMILAR:
			borderColor = colorRed;
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_NP_LEFT_EQ_RIGHT:
			borderColor = colorRed;
			leftEdgeColor = rightEdgeColor = colorBlue;
			break;
		case PicassoConstants.T_NP_LEFT_SIMILAR:
			borderColor = colorRed;
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = colorRed;
			break;
		case PicassoConstants.T_NP_RIGHT_SIMILAR:
			borderColor = colorRed;
			leftEdgeColor = colorRed;
			rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_NP_LEFT_EQ:
			borderColor = colorRed;
			leftEdgeColor = colorBlue;
			rightEdgeColor = colorRed; //new Color(0xfffa6161+npAdd);
			break;
		case PicassoConstants.T_NP_RIGHT_EQ:
			borderColor = colorRed;
			leftEdgeColor = colorRed; //new Color(0xfffa6161+npAdd);
			rightEdgeColor = colorBlue;
			break;
		case PicassoConstants.T_NP_NOT_SIMILAR:
			borderColor = colorRed;
			leftEdgeColor = colorRed;
			rightEdgeColor = colorRed;
			break;
		case PicassoConstants.T_EDIT_NODE:
			borderColor = null;
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = Color.BLACK;
			break;
		case PicassoConstants.T_NO_DIFF_DONE:
		default:
			borderColor = null;
			//tree.getNodeColor(treeNames); //new Color(0xFF991111); //
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = Color.BLACK;
			break;
		}

		if (numOfPlans == 1) {
			borderColor = null;
			leftEdgeColor = Color.BLACK;
			rightEdgeColor = Color.BLACK;
			cellColor = tree.getNodeColor(treeNames);
		}
		tree.setDisplayType(treeDiffBox.getSelectedIndex());
		//MessageUtil.CPrintToConsole("Tree :: " + tree.getNodeName() + " " +
		// tree.isSimilar());
		DefaultGraphCell cell = createVertex(tree, labels, cellColor,
				borderColor, Color.BLACK);
		nodes.add(cell);

		/*
		 * if ( tree.getMatchNumber() != 0 ) { //DefaultGraphCell match =
		 * addMatchNode(tree.getMatchNumber()); edges.add(createMatchEdge(cell,
		 * cell, new
		 * Color(PicassoConstants.matchColor[tree.getMatchNumber()])));
		 * //nodes.add(match); }
		 */

		// Create the edges...
		Vector children = tree.getChildren();

		//MessageUtil.CPrintToConsole("In Insert Nodes : " +
		// tree.getNodeName());

		if (children != null && children.size() != 0) {
			//MessageUtil.CPrintToConsole("Children : " + children.size());
			//int i = children.size()-1;
			for (int i = 0; i < children.size(); i++) {
				//while ( i >= 0 ) {
				TreeNode childNode = (TreeNode) children.elementAt(i);
				if (childNode == null) {
					MessageUtil
							.CPrintErrToConsole("Null Child: Error in Server Tree Creation "
									+ cell.toString());
					continue;
				}
				//MessageUtil.CPrintToConsole("Child : " + i + " " +
				// childNode.toString());
				DefaultGraphCell child = insertNodesAndEdges(childNode, nodes,
						edges, c);

				//MessageUtil.CPrintToConsole(cell.toString() + " In Insert
				// Edges : " + child.toString());
				if (i == 0)
					edges.add(createEdge(cell, child, leftEdgeColor));
				else if (i == 1) {
					if (tree.getNodeName().equals("Seq Scan"))
						edges.add(createDashedEdge(cell, child, Color.BLACK));
					else
						edges.add(createEdge(cell, child, rightEdgeColor));
				} else
					edges.add(createDashedEdge(cell, child, Color.BLACK));
				//i--;
			}
		}
		return (cell);
	}

	protected DefaultGraphCell addMatchNode(int matchNumber) {
		DefaultGraphCell cell = new DefaultGraphCell("" + matchNumber);
		GraphConstants.setFont(cell.getAttributes(), new Font(
				PicassoConstants.LEGEND_FONT, Font.BOLD, 10));
		GraphConstants.setAutoSize(cell.getAttributes(), true);
		GraphConstants.setInset(cell.getAttributes(), 5);
		GraphConstants.setEditable(cell.getAttributes(), false);
		GraphConstants.setBackground(cell.getAttributes(), new Color(
				PicassoConstants.matchColor[matchNumber]));
		GraphConstants.setForeground(cell.getAttributes(), Color.BLACK);
		GraphConstants.setOpaque(cell.getAttributes(), true);
		GraphConstants.setBounds(cell.getAttributes(), new Rectangle2D.Double(
				100, 100, 5, 5));
		GraphConstants.setBorder(cell.getAttributes(), BorderFactory
				.createRaisedBevelBorder());

		//		 Add a Port
		DefaultPort port = new DefaultPort();
		cell.add(port);

		return cell;
	}

	protected DefaultEdge createDashedEdge(DefaultGraphCell sourcePort,
			DefaultGraphCell targetPort, Color c) {
		DefaultEdge edge = new DefaultEdge();

		int arow = GraphConstants.ARROW_CLASSIC;
		GraphConstants.setLineBegin(edge.getAttributes(), arow);
		GraphConstants.setBeginFill(edge.getAttributes(), true);
		GraphConstants.setEditable(edge.getAttributes(), false);
		GraphConstants.setLineColor(edge.getAttributes(), c);
		GraphConstants.setDashOffset(edge.getAttributes(), 4.0f);
		float[] values = { 4.0f, 4.0f };
		GraphConstants.setDashPattern(edge.getAttributes(), values);
		GraphConstants.setLineStyle(edge.getAttributes(), 3);
		GraphConstants.setLabelAlongEdge(edge.getAttributes(), true);

		edge.setSource(sourcePort.getChildAt(0));
		edge.setTarget(targetPort.getChildAt(0));

		return edge;
	}

	/**
	 * Method hook to create custom vertices
	 * 
	 * @param userObject
	 *            the user object to pass to the cell
	 * @return the new vertex instance
	 */
	protected DefaultGraphCell createVertex(Object userObject,
			Object[] extraLabels, Color c, Color bc, Color fc) {
		//AttributeMap attributes = new AttributeMap(defaultVertexAttributes);
		//GraphConstants.setBounds(attributes, new Rectangle2D.Double(position
		//		.getX(), position.getY(), 40, 20));
		DefaultGraphCell cell = new DefaultGraphCell(userObject);

		//GraphConstants.setBounds(cell.getAttributes(), Rectangle2D.(20, 20,
		// 40, 20));
		GraphConstants.setFont(cell.getAttributes(), new Font(
				PicassoConstants.LEGEND_FONT, Font.BOLD, 10));
		GraphConstants.setAutoSize(cell.getAttributes(), true);
		GraphConstants.setInset(cell.getAttributes(), 5);
		GraphConstants.setEditable(cell.getAttributes(), false);
		GraphConstants.setBackground(cell.getAttributes(), c);
		GraphConstants.setForeground(cell.getAttributes(), fc);
		GraphConstants.setOpaque(cell.getAttributes(), true);
		GraphConstants.setBounds(cell.getAttributes(), new Rectangle2D.Double(
				100, 100, 40, 20));

		//GraphConstants.setSize(cell.getAttributes(), new Dimension(50, 20));
		if (bc == null)
			GraphConstants.setBorder(cell.getAttributes(), BorderFactory
					.createRaisedBevelBorder());
		else
			GraphConstants.setBorder(cell.getAttributes(), BorderFactory
					.createEtchedBorder(EtchedBorder.RAISED, bc, bc));

		// Add a Port
		DefaultPort port = new DefaultPort();
		cell.add(port);
		return cell;
	}

	/**
	 * Method hook to create custom edges
	 * 
	 * @return the new vertex instance
	 */
	protected DefaultEdge createEdge(DefaultGraphCell sourcePort,
			DefaultGraphCell targetPort, Color c) {
		/*
		 * AttributeMap edgeAttrib = null; if (defaultEdgeAttributes != null) {
		 * edgeAttrib = new AttributeMap(defaultEdgeAttributes); } else {
		 * edgeAttrib = new AttributeMap(6); }
		 */

		DefaultEdge edge = new DefaultEdge();

		int arow = GraphConstants.ARROW_CLASSIC;
		GraphConstants.setLineBegin(edge.getAttributes(), arow);
		GraphConstants.setBeginFill(edge.getAttributes(), true);
		GraphConstants.setEditable(edge.getAttributes(), false);
		GraphConstants.setLineColor(edge.getAttributes(), c);

		edge.setSource(sourcePort.getChildAt(0));
		edge.setTarget(targetPort.getChildAt(0));

		return edge;
	}

	//	A Custom Model that does not allow Self-References
	public static class TreeModel extends DefaultGraphModel {

		private static final long serialVersionUID = -7593843754217797191L;

		// Override Superclass Method
		public boolean acceptsSource(Object edge, Object port) {
			// Source only Valid if not Equal Target
			return (((Edge) edge).getTarget() != port);
		}

		// Override Superclass Method
		public boolean acceptsTarget(Object edge, Object port) {
			// Target only Valid if not Equal Source
			return (((Edge) edge).getSource() != port);
		}
	}

	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		//MessageUtil.CPrintToConsole("In Mouse Clicked " + x + " Y " + y);
		CellView[] views = graph.getGraphLayoutCache().getCellViews();

		for (int i = 0; i < views.length; i++) {
			if (views[i].getBounds().contains(x / graph.getScale(),
					y / graph.getScale())) {
				DefaultGraphCell cell = (DefaultGraphCell) (views[i].getCell());
				TreeNode node = (TreeNode) cell.getUserObject();
				if (node != null) {
					//graph.setToolTipText(node.toString());
					String str = getAttributeStr(node);
					operatorLbl
							.setText("   " + node.getNodeName() + ": " + str);
					//MessageUtil.CPrintToConsole("Found Graph Node " + i + ",
					// " + node.toString());
				}
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == zoomin) {
			this.zoomin();
		} else if (source == zoomout) {
			this.zoomout();
		}
	}

	int prevVal = -1, curVal = -1;

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == treeDiffBox) {
			curVal = treeDiffBox.getSelectedIndex();
			if (prevVal != curVal) {
				prevVal = curVal;
				if (graph != null) {
					parent.displayType = curVal;
					graph.getGraphLayoutCache().remove(cells, true, true);
					TreeNode root = (TreeNode) trees
							.elementAt((planId * 2) + 2);
					cells = insertTreeData(clientPlanNumber, serverPlanNumber,
							root, graph);
					boolean selectsAll = graph.getGraphLayoutCache()
							.isSelectsAllInsertedCells();
					boolean selectsLocal = graph.getGraphLayoutCache()
							.isSelectsLocalInsertedCells();
					graph.getGraphLayoutCache().setSelectsAllInsertedCells(
							false);
					graph.getGraphLayoutCache().setSelectsLocalInsertedCells(
							false);

					graph.getGraphLayoutCache().insert(cells);

					graph.getGraphLayoutCache().setSelectsAllInsertedCells(
							selectsAll);
					graph.getGraphLayoutCache().setSelectsLocalInsertedCells(
							selectsLocal);

					JGraphFacade facade = new JGraphFacade(graph);
                                        facade.setOrdered(true);
					/*JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
					if (treeDiffBox.getSelectedIndex() == PicassoConstants.SHOW_BOTH)
						layout.setIntraCellSpacing(250);
					else if (treeDiffBox.getSelectedIndex() == PicassoConstants.SHOW_NONE)
						layout.setIntraCellSpacing(100);
					else
						layout.setIntraCellSpacing(150);
					layout.setInterRankCellSpacing(20);
					layout.isDeterministic();
					layout.setOrientation(SwingConstants.NORTH);

					layout.run(facade);*/
                                        JGraphTreeLayout clayout = new JGraphTreeLayout();
                                        clayout.setOrientation(SwingConstants.NORTH);
                                        clayout.setNodeDistance(50);
                                        //clayout.setLevelDistance(20);
                                        clayout.run(facade);
					Map nested = facade.createNestedMap(true, true);
					graph.getGraphLayoutCache().edit(nested);
				}
			}
		}
	}

}
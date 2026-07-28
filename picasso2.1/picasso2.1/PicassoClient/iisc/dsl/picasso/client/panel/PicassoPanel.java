
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

package iisc.dsl.picasso.client.panel;

import iisc.dsl.picasso.client.frame.PredicateValuesFrame;
import iisc.dsl.picasso.client.frame.ResolutionFrame;
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.DiagramUtil;
import iisc.dsl.picasso.client.util.Draw1DDiagram;
import iisc.dsl.picasso.client.util.Draw2DDiagram;
import iisc.dsl.picasso.client.util.Draw3DDiagram;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.DataValues;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import visad.DisplayImpl;
import visad.ProjectionControl;
import visad.ScalarMap;


public class PicassoPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -2794920411648209834L;

	protected String panelString = "PicassoPanel";
	private ClientPacket 		clientPacket;
	protected MainPanel 		parent;
	Component   				diagramComponent=null;
	DisplayImpl					display=null;
	ScalarMap[]					maps;
	JPanel						infoPanel;
	JLabel[]					infoLabels;
	JButton						resetButton=null, regenButton=null;
	int							panelType;

	public static final int DEFAULT = 0;
	public static final int PLAN_DIAGRAM = 1;
	public static final int PLAN_COST_DIAGRAM = 2;
	public static final int PLAN_CARD_DIAGRAM = 3;
	public static final int REDUCED_PLAN_DIAGRAM = 4;
	public static final int EXEC_PLAN_COST_DIAGRAM = 5;
	public static final int EXEC_PLAN_CARD_DIAGRAM = 6;
	public static final int SELECTIVITY_LOG = 7;

	//bottom-bar - apa
	private Vector			queryParams = new Vector();
	private JLabel 			dimensionLbl;
	public 	JComboBox		dimensionBox1, dimensionBox2;
	public JButton 		setButton;
	private JLabel			predicateLbl;
	private JLabel 			panelLabel;
	public PredicateValuesFrame predframe;
	//apa
	Hashtable predicateValues;
	
	public PicassoPanel() {
	    this.addPropertyChangeListener(propertyChangeListener);
		this.addPropertyChangeListener(statusListener);
		panelType = DEFAULT;
	}
	public PicassoPanel(MainPanel p) {
		parent = p;
		clientPacket = parent.getClientPacket();
		this.addPropertyChangeListener(propertyChangeListener);
		this.addPropertyChangeListener(statusListener);
		panelType = DEFAULT;
		maps = new ScalarMap[7];
		for (int i=0; i < 4; i++)
			maps[i] = null;
		
	}
	
	public MainPanel getPParent() {
		return(parent);
	}

	protected ClientPacket getClientPacket() {
		clientPacket = parent.getClientPacket();
		return clientPacket;
	}

	public void setStatus(String status) {
		parent.setStatusLabel(status);
	}

	public int getPanelType() {
		return panelType;
	}

	public int[][] getSortedPlan() {
		return parent.getSortedPlan();
	}

	public void setPanelString(String str) {
		panelString = str;
	}

	public String getPanelString() {
		return(panelString);
	}

	public DisplayImpl getDisplayImage() {
		return display;
	}

	public int getVisadY() {
		if ( diagramComponent == null )
			return parent.tabbedPane.getY();
		return parent.tabbedPane.getY() + infoPanel.getY() + diagramComponent.getY();
	}

	protected void setSettingsChanged(boolean value) {
		parent.setSettingsChanged(value);
	}

	protected void setPanelLabel(String str) {
		panelLabel.setText(str);
	}
	public void process(int msgType) {
		//MessageUtil.CPrintToConsole("IN MAIN PANEL PROCESS " + panelString);
	}

	protected void setParamsChanged(boolean value) {
		parent.setParamsChanged(value);
	}
	
	protected void sendProcessToServer(int msgType) {
		//	 Get the fields data structure
		ClientPacket clientPacket = getClientPacket();
		String serverName = parent.getServerName();
		int serverPort = parent.getServerPort();

		// Adding the query packet to the QTDescriptor list. This should be removed if the diagram generation is unsuccessful.
		if(msgType == MessageIds.READ_PICASSO_DIAGRAM)
		{
			parent.getDBSettingsPanel().addQid(getClientPacket().getQueryPacket());
			clientPacket.getQueryPacket().genSuccess = false;
		}
		// Build the message to be sent
		int msgId = msgType;

		clientPacket.setMessageId(msgId);
		if(!PicassoConstants.IS_PKT_LOADED)
		{
			MessageUtil.sendMessageToServer(serverName, serverPort, clientPacket,	(PicassoPanel)this);
			parent.setMsgSent(true);
		}
		else
			drawAllDiagrams(parent.getServerPacket());
//		MessageUtil.sendMessageToServer(serverName, serverPort, clientPacket,	(PicassoPanel)this);
//		parent.setMsgSent(true);
	}

	public void emptyPanel() {
		if ( diagramComponent != null ) {
			remove(diagramComponent);
			display = null;
			this.dimensionBox1.setEnabled(false);
			this.dimensionBox2.setEnabled(false);
			this.setButton.setEnabled(false);
			System.gc();
		}
		nullInfoLabels();
	}

	// Need a data structure which is for putting in the server information.
	// The send and receive thread adds to this queue and the panel thread once
	// it receives the information it removes it from the queue.
	// These need to be synchronized.
	private Vector rcvdServerMsgs = new Vector();

	public synchronized ServerPacket getServerMessage() {
		if ( rcvdServerMsgs.size() != 0 ) {
			ServerPacket msg = (ServerPacket)rcvdServerMsgs.elementAt(0);
			rcvdServerMsgs.removeElement(msg);
			return msg;
		}
		return null;
	}

	public synchronized void addServerMessage(ServerPacket msg) {
		rcvdServerMsgs.addElement(msg);
	}

	public void processServerMessage() {
		ServerPacket serverPacket = getServerMessage();

		int msgId = serverPacket.messageId;
		//MessageUtil.CPrintToConsole("Message "+msgId+" received IN PANEL " + panelString );

		switch (msgId ) {
		case MessageIds.GET_CLIENT_ID :
			clientPacket.setClientId(""+serverPacket.clientId);
			JOptionPane.showMessageDialog(this.getPParent().getParent(), "Connected to Picasso Server\n Machine: " + parent.getServerName() + ", Port: " + parent.getServerPort());
			getQTNames();
			break;

		case MessageIds.ERROR_ID :
			parent.setMsgSent(false);
			if(serverPacket.status != null)
				parent.setStatusLabel("ERROR: " + serverPacket.status);
			
			if(parent.getDBSettingsPanel().getQtDesc()!= null && parent.getDBSettingsPanel().getQtDesc().dummyEntry)
				parent.getDBSettingsPanel().removeQid(parent.getDBSettingsPanel().curQName.getQueryName());
			parent.setProgressBar(0);
			parent.processErrorMessage(serverPacket);
			break;

		case MessageIds.STATUS_ID :
			parent.setStatusLabel("STATUS: " + serverPacket.status);
			parent.setProgressBar(serverPacket.progress);
			//if ( serverMsg.progress != 0 )
			//	parent.showStopButton(true);
			break;

		case MessageIds.PROCESS_QUEUED :
			parent.setMsgSent(false);
			JOptionPane.showMessageDialog(this.getPParent().getParent(), "Your request has been queued and will be processed by the server later","Queued",JOptionPane.INFORMATION_MESSAGE);
			//parent.restoreOldValues();
			parent.processErrorMessage(serverPacket);
			break;

		case MessageIds.DELETE_PICASSO_TABLES :
			parent.setMsgSent(false);
			JOptionPane.showMessageDialog(this.getPParent().getParent(),serverPacket.status,"Status",JOptionPane.INFORMATION_MESSAGE);
			parent.getDBSettingsPanel().emptyQidNames();
			parent.setStatusLabel("STATUS: DONE");
			parent.processErrorMessage(serverPacket);
			break;

		case MessageIds.DELETE_PICASSO_DIAGRAM :
			parent.setMsgSent(false);
			JOptionPane.showMessageDialog(this.getPParent().getParent(),serverPacket.status,"Status",JOptionPane.INFORMATION_MESSAGE);
			parent.processErrorMessage(serverPacket);
			break;

		case MessageIds.GET_QUERYTEMPLATE_NAMES :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
			parent.displayQidNames(serverPacket);
			parent.processErrorMessage(serverPacket);
			parent.getDBSettingsPanel().removeKeyListener(this.parent.getDBSettingsPanel());
			parent.getDBSettingsPanel().setrange(0);
			parent.getDBSettingsPanel().setresbox(0);
			parent.getDBSettingsPanel().addKeyListener(this.parent.getDBSettingsPanel());
			break;

		case MessageIds.READ_PICASSO_DIAGRAM :
			//parent.setStatusLabel("STATUS : Received Graphs from Server");
			//saveGraph(serverMsg);
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
			parent.planPanel.planDiff.setText(PlanPanel.diffLevel1);
			PicassoConstants.OP_LVL=false;
			parent.repaint();
			parent.tabbedPane.setEnabledAt(2, true);
			//drawGraph(serverMsg); // Temp routine.. This should move to the tabbed panels...
			break;

		case MessageIds.TIME_TO_GENERATE :
			//System.out.println("\nDiagram type is: "+PicassoConstants.DIAGRAM_REQUEST_TYPE);
			if(PicassoConstants.DIAGRAM_REQUEST_TYPE == 'E'){
				parent.setMsgSent(false);
				int val = JOptionPane.showConfirmDialog(this.getPParent().getParent(), serverPacket.status, "Status", JOptionPane.YES_NO_OPTION);
				if ( val == 0 ){
					parent.enableCancelButton(true);
					String serverName = parent.getServerName();
					int serverPort = parent.getServerPort();
					clientPacket.setMessageId(MessageIds.GENERATE_PICASSO_DIAGRAM);
					clientPacket.getQueryPacket().setGenDuration(serverPacket.queryPacket.getGenDuration());
					System.err.println("Estimated time to generate for the client is: "+serverPacket.queryPacket.getGenDuration());
					MessageUtil.sendMessageToServer(serverName, serverPort, clientPacket,	(PicassoPanel)this);
					parent.setMsgSent(true);
					//this.process(MessageIds.GENERATE_PICASSO_DIAGRAM);
				}
				else {
					parent.setStatusLabel("STATUS: DONE");
					parent.processErrorMessage(serverPacket);
				}
				break;
			}
			parent.setMsgSent(false);
			parent.estimatedTime = serverPacket.queryPacket.getEstimatedTime();
			parent.getApproxParameters(serverPacket.optClass,serverPacket.status);
			//int val = JOptionPane.showConfirmDialog(this.getPParent().getParent(), serverPacket.status, "Status", JOptionPane.YES_NO_OPTION);
			if ( parent.getPlanDisplayFlag() )
			{
				parent.enableCancelButton(true);
				String serverName = parent.getServerName();
				int serverPort = parent.getServerPort();
				if(parent.approxDiagram)
				{					
					parent.estimatedTime = serverPacket.queryPacket.getEstimatedTime();
					parent.processMessageApprox(MessageIds.GENERATE_APPROX_PICASSO_DIAGRAM);
					if(parent.tabbedPane.getSelectedIndex() == 1)
						setPanelString("Approx Plan Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 2)
						setPanelString("Approx Reduced Plan Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 3)
						setPanelString("Approx Compilation Cost Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 4)
						setPanelString("Approx Compilation Cardinality Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 5)
						setPanelString("Execution Cost Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 6)
						setPanelString("Execution Cardinality Diagram");
					else 
						setPanelString("Approx Plan Diagram");
				}
				else
				{
					clientPacket.setMessageId(MessageIds.GENERATE_PICASSO_DIAGRAM);		
					if(parent.tabbedPane.getSelectedIndex() == 1)
						setPanelString("Plan Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 2)
						setPanelString("Reduced Plan Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 3)
						setPanelString("Compilation Cost Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 4)
						setPanelString("Compilation Cardinality Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 5)
						setPanelString("Execution Cost Diagram");
					else if(parent.tabbedPane.getSelectedIndex() == 6)
						setPanelString("Execution Cardinality Diagram");
					else 
						setPanelString("Plan Diagram");
					
		        	clientPacket.getQueryPacket().setGenDuration(serverPacket.queryPacket.getGenDuration());
					System.err.println("Estimated time to generate for the client is: "+serverPacket.queryPacket.getGenDuration());
					MessageUtil.sendMessageToServer(serverName, serverPort, clientPacket,	(PicassoPanel)this);
					parent.setMsgSent(true);
				}
				
				//this.process(MessageIds.GENERATE_PICASSO_DIAGRAM);
			}
			else {
				parent.setStatusLabel("STATUS: DONE");
				parent.processErrorMessage(serverPacket);
			}
			break;
		case MessageIds.TIME_TO_GENERATE_APPROX:
			parent.setMsgSent(false);
			int val = 0;
			if(serverPacket.queryPacket.getGenDuration() > 0)
			{
				val = JOptionPane.showConfirmDialog(this.getPParent().getParent(), serverPacket.status, "Status", JOptionPane.YES_NO_OPTION);
			}
			if (val == 0)
			{
				// show the user the right parameter while generating
				parent.getDBSettingsPanel().getQtDesc().setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);
				parent.getDBSettingsPanel().repaintQtDesc();
				// send the right info.
				
				parent.enableCancelButton(true);				
				clientPacket.setMessageId(MessageIds.GENERATE_APPROX_PICASSO_DIAGRAM);
				if(parent.tabbedPane.getSelectedIndex() == 1)
					setPanelString("Approx Plan Diagram");
				else if(parent.tabbedPane.getSelectedIndex() == 2)
					setPanelString("Approx Reduced Plan Diagram");
				else if(parent.tabbedPane.getSelectedIndex() == 3)
					setPanelString("Approx Compilation Cost Diagram");
				else if(parent.tabbedPane.getSelectedIndex() == 4)
					setPanelString("Approx Compilation Cardinality Diagram");
				else if(parent.tabbedPane.getSelectedIndex() == 5)
					setPanelString("Execution Cost Diagram");
				else if(parent.tabbedPane.getSelectedIndex() == 6)
					setPanelString("Execution Cardinality Diagram");
				else 
					setPanelString("Approx Plan Diagram");
				
				String serverName = parent.getServerName();
				int serverPort = parent.getServerPort();
				clientPacket.getQueryPacket().setGenDuration(serverPacket.queryPacket.getGenDuration());
				System.err.println("Estimated time to generate for the client is: "+serverPacket.queryPacket.getGenDuration());
				MessageUtil.sendMessageToServer(serverName, serverPort, clientPacket,	(PicassoPanel)this);
				parent.setMsgSent(true);
			}
			else {
				parent.setStatusLabel("STATUS: DONE");
				if(parent.getDBSettingsPanel().getQtDesc().dummyEntry)
					parent.getDBSettingsPanel().removeQid(parent.getDBSettingsPanel().curQName.getQueryName());
				parent.processErrorMessage(serverPacket);
			}
			break;
		case MessageIds.GET_PLAN_TREE :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
			break;

//        case MessageIds.GET_PLAN_TREES :
//			parent.setMsgSent(false);
//			parent.setStatusLabel("STATUS: DONE");
//			((PlanPanel)this).transformToOperatorLevel(serverPacket);
//			break;

		case MessageIds.GET_COMPILED_PLAN_TREE :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
		case MessageIds.GET_ABSTRACT_PLAN :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");

		case MessageIds.GET_PLAN_STRINGS :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
			break;
			
		case MessageIds.GET_PLAN_COSTS :
			parent.setMsgSent(false);
			parent.setStatusLabel("STATUS: DONE");
			break;
			
		default:
			parent.setStatusLabel("STATUS: DONE");
		}
		//parent.setStatusLabel("STATUS: DONE");
		//MessageUtil.CPrintToConsole("Message "+msgId +" is processed");
	}

//	Define PropertyChangeListener
	PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			String property = propertyChangeEvent.getPropertyName();
			if ("msgReceived".equals(property)) {
				processServerMessage();
			}
		}
	};

	PropertyChangeListener statusListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			String property = propertyChangeEvent.getPropertyName();
			if ("statusReceived".equals(property)) {
				processServerMessage();
			}
		}
	};

//	protected void saveDiagram(ServerPacket msg) {
//		parent.setDiagramPacket(msg.diagramPacket);
//	}

	protected DiagramPacket getDiagramPacket() {
		return(parent.getDiagramPacket());
	}

	public void dispWarningMessage(ServerPacket msg) {
		String warning = "";
		boolean show = false;

		int ressum = 0;
		double maxErr=0;
		int res;
		float[] picassoSelec;
		float[] predSelec;
		float psel=0,osel=0;
		DiagramPacket dp = msg.diagramPacket;
		for(int k=0;k<dp.getDimension();k++)//rss
		{
			res = dp.getResolution(k);
			if(k!=0)
				ressum += dp.getResolution(k - 1);
			picassoSelec = dp.getPicassoSelectivity();
			predSelec = dp.getPredicateSelectivity();

		//Get plan selectivity if choose plan...
		//float[] planSelec = dp.getPlanSelectivity();
			maxErr=-1;
			psel=5; osel=5;
		for ( int i=0; i < dp.getRelationNames().length; i++ ) {
			for (int j=0; j < res; j++) {
				double err = -1;
					float abs = Math.abs(picassoSelec[ressum+j] - predSelec[ressum+j]);
					if(predSelec[ressum+j]>0)
						err = (Math.abs(picassoSelec[ressum+j] - predSelec[ressum+j])*100) / predSelec[ressum+j];

				//System.out.println(selecThreshold + " Selec Threshold : " + gdp.getQueryPacket().getSelecThreshold());
				String selecThreshold = dp.getQueryPacket().getSelecThreshold();
				if ( abs > 1 && err >= Double.parseDouble(selecThreshold)) {
					if ( maxErr < err ) {
						maxErr = err;
							psel = picassoSelec[ressum+j];
							osel = predSelec[ressum+j];
						}
					}
				}
			}
		}

		DecimalFormat df = new DecimalFormat("0.00");
		df.setMaximumFractionDigits(2);
		if ( maxErr != -1 ) {
			warning += "Alert: Maximum Selectivity Difference is " + df.format(maxErr) + "%\n"
					+ "Picasso Selectivity=" + df.format(psel) + "% and Optimizer Selectivity=" + df.format(osel) + "%\n";
			show = true;
		}
        if(msg.queryPacket.getExecType().equals(PicassoConstants.COMPILETIME_DIAGRAM)||msg.queryPacket.getExecType().equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM))
		// modified for cdp2
		{
			// if (PicassoUtil.isCDPViolated(msg.diagramPacket)) {
			java.util.ArrayList arrlist = PicassoUtil.isCDPViolated(msg.diagramPacket);
			String viostr = (String) arrlist.get(0);
			String totalptsstr = (String)arrlist.get(2);
			if (viostr.equalsIgnoreCase("true")) {
				String pts_viostr = (String) arrlist.get(1);
				if (show)
					warning += "\n";
				if(!pts_viostr.equals("0"))
				{ 
					warning += "Alert: The Plan Cost Monotonicity (PCM) Principle does not fully hold in the Compilation Cost diagram \n Number of points where PCM is violated (out of "+totalptsstr+"): "+pts_viostr;
				}
				//modification ends here
				show = true;
			}
		}
		// modification for cdp2 ends here
		if ( show ) {
			JOptionPane.showMessageDialog(parent.getParent(), warning,"Status",JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public void drawAllDiagrams(ServerPacket msg) {
		// Here we draw the graphs for all the tabbed panels and then
		// display it in paint routine
		// the following two lines reset the dimension box indexes
		PicassoConstants.a[0]=0;
		PicassoConstants.a[1]=1;
		parent.drawAllDiagrams(msg, this);
		parent.setqp(false);
	}

	public void processErrorMessage(ServerPacket msg) {
		parent.processErrorMessage(msg);
	}

	protected void deleteDiagram() {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.DELETE_PICASSO_DIAGRAM);
		String serverName = parent.getServerName();
		int serverPort = parent.getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, this);

	}

	protected void renameDiagram(String name) {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.RENAME_PICASSO_DIAGRAM);
		values.getQueryPacket().setNewQueryName(name);
		String serverName = parent.getServerName();
		int serverPort = parent.getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, this);

	}

	protected void getQTNames() {
		// Get the fields data structure
		getPParent().getDBSettingsPanel().emptyQidNames();
		ClientPacket values = getClientPacket();
		if ( values.getDBSettings() == null )
			return;

		String serverName = parent.getServerName();
		int serverPort = parent.getServerPort();
		values.setMessageId(MessageIds.GET_QUERYTEMPLATE_NAMES);

		// Build the message to be sent
		//ClientPacket cp = MessageUtil.buildMessage(values, MessageIds.GET_QUERY_NAMES);

		MessageUtil.sendMessageToServer(serverName, serverPort, values, this);
	}

	protected void addBottomPanel()
	{
		Font f = new Font("Courier", Font.PLAIN, 12);

		JPanel bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		GridBagLayout gb = new GridBagLayout();
		bottomPanel.setLayout(gb);

		GridBagConstraints c = new GridBagConstraints();
		
		
		dimensionLbl = new JLabel("Display Dimensions", JLabel.CENTER);
		dimensionLbl.setFont(f);
		predicateLbl = new JLabel("", JLabel.CENTER);
		predicateLbl.setFont(f);
		dimensionBox1 = new JComboBox();
		dimensionBox2 = new JComboBox();
		dimensionBox1.addActionListener(this);
		dimensionBox2.addActionListener(this);
		setButton = new JButton("Set Dim Sel");
		setButton.addActionListener(this);

		
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.ipady = 2;
		c.ipadx = 2;
		c.anchor = GridBagConstraints.LINE_END;
//		c.fill = GridBagConstraints.NONE;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.insets = new Insets(2, 5, 2, 5);
		bottomPanel.add(dimensionLbl, c);
		
		c.gridx = 1;
		bottomPanel.add(dimensionBox1, c);
		
		c.gridx = 2;
		bottomPanel.add(dimensionBox2, c);
		
		c.gridx = 3;
		bottomPanel.add(setButton, c);
		
		c.gridx = 4;
		bottomPanel.add(predicateLbl, c);

	}
	
	protected void addInfoPanel(Color textColor) {
		infoPanel = new JPanel();
		infoPanel.setBackground(PicassoConstants.IMAGE_BACKGROUND);
		this.add(infoPanel, BorderLayout.EAST);

		JPanel topPanel = new JPanel();
		topPanel.setBackground(PicassoConstants.IMAGE_BACKGROUND);
		this.add(topPanel, BorderLayout.NORTH);

                JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(PicassoConstants.IMAGE_BACKGROUND);
		this.add(bottomPanel, BorderLayout.SOUTH);

        infoLabels = new JLabel[27];

		regenButton = new JButton("Regenerate Diagram");
		regenButton.addActionListener(this);
		regenButton.setForeground(Color.BLACK);
		//if ( this instanceof PlanCostPanel || this instanceof PlanCardPanel ) {
		resetButton = new JButton("Reset View");
		resetButton.addActionListener(this);
		resetButton.setForeground(Color.BLACK);
		//resetButton.setBackground(Color.BLACK);
		//}

		GridBagLayout gb = new GridBagLayout();
		infoPanel.setLayout(gb);
		topPanel.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints();
		Font f = new Font("Courier", Font.BOLD, 12);
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		//c.fill = GridBagConstraints.HORIZONTAL;

		for (int i=0; i < infoLabels.length; i++) {
			infoLabels[i] = new JLabel("", JLabel.LEFT);
			infoLabels[i].setFont(f);
			infoLabels[i].setForeground(textColor);
			if ( i == 0 ) {
				c.gridy = 0;
				c.gridwidth = 3;
				c.gridx = 1;
				c.insets = new Insets(1, 5, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			} else if ( i == 1 ) {
				c.gridx = 4;
				c.gridy = 0;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				c.insets = new Insets(1, 1, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			} 
			else if(i==2)
			{
				c.gridx = 4;
				c.gridy = 1;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				c.insets = new Insets(1, 1, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			}else if (i==23)
			{
				c.gridx = 4;
				c.gridy = 2;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				c.insets = new Insets(1, 1, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			}else if (i==24)
			{
				c.gridx = 4;
				c.gridy = 3;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				c.insets = new Insets(1, 1, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			}else if (i==25)
			{
				c.gridx = 4;
				c.gridy = 4;
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				c.insets = new Insets(1, 1, 1, 5);
				topPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			}
			else {
				c.gridx = 0;
				c.gridy = i-2;
				c.anchor = GridBagConstraints.LINE_START;
				if ( i==infoLabels.length-1)
					c.insets = new Insets(1, 1, 50, 1);
				else
					c.insets = new Insets(1, 1, 1, 1);
				infoPanel.add(infoLabels[i], c);
				infoLabels[i].setHorizontalAlignment(JLabel.LEFT);
			}
		}

		c.gridy = 0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.insets = new Insets(1, 5, 1, 5);
		panelLabel = new JLabel(this.panelString, JLabel.LEFT);
		panelLabel.setFont(f);
		panelLabel.setForeground(textColor);
		c.anchor = GridBagConstraints.LINE_START;
		topPanel.add(panelLabel, c);

		c.gridx = 0;
		c.gridy = infoLabels.length;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(100, 1, 1, 1);
		//regenButton.setHorizontalAlignment(JButton.RIGHT);
		infoPanel.add(regenButton, c);
		c.gridx = 0;
		c.gridy = infoLabels.length+1;
		c.gridwidth = 1;
		c.insets = new Insets(1, 1, 1, 1);
		//if ( this instanceof PlanCostPanel || this instanceof PlanCardPanel ) {
		//resetButton.setHorizontalAlignment(JButton.RIGHT);
		infoPanel.add(resetButton, c);
		//}
	}

	protected void setInfoLabels(DiagramPacket gdp, int type, JLabel[] infoLabels) {
		//DiagramPacket gdp = msg.diagramPacket;

		//int dimensions = msg.queryPacket.getDimension();
		double[] values = DiagramUtil.getMinAndMaxValues(gdp);
		
		DataValues []data = null;
		if(gdp.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
			data = parent.getFullExecDiagramPacket().getData();
		else
			data = parent.getFullDiagramPacket().getData();
		
		int num;
		if(gdp.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
			num = parent.getFullExecDiagramPacket().getMaxPlanNumber();
		else
			num = parent.getFullDiagramPacket().getMaxPlanNumber();
		
		boolean []plans = new boolean[num];
		
		for(int i = 0; i < data.length; i++)
		{
			plans[data[i].getPlanNumber()] = true;
		}
		int globplans=0;
		for(int i = 0; i < plans.length; i++)
			if(plans[i])
				globplans++;
		// parent.getFullDiagramPacket().setMaxPlanNumber(globplans);
		//apa
//		if ( gdp.getDimension() > 2 ) {
//			Hashtable attrSelec = getPParent().getClientPacket().getAttributeSelectivities();
//			Object[] keys = attrSelec.keySet().toArray();
//
//			String dimSelec = "";
//			int end = keys.length;
//			if ( keys.length > 4 ) {
//				end = 4;
//			}
//			int start = 20-end;
//			for (int i=0; i < end; i++) {
//				int index = ((Integer)keys[i]).intValue();
//				String attrName = getPParent().getQueryAttrName(index);
//				dimSelec =  "Sel[" + attrName + "]=" + attrSelec.get(keys[i]) + "%";
//				infoLabels[start+i].setText(dimSelec);
//			}
//		}
//apae
		
		DecimalFormat df = new DecimalFormat("0.00E0");
		df.setMaximumFractionDigits(2);

		//infoLabels[0].setText("    QTD: " + parent.getQueryName());//msg.queryPacket.getQueryName());
		String qname = parent.getQueryName();	
		parent.approxDiagram = gdp.approxDiagram;
		
		if(parent.approxDiagram || PicassoConstants.IS_APPROXIMATE_DIAGRAM)
		{
			if(parent.tabbedPane.getSelectedIndex() == 1)
				panelLabel.setText("Approx Plan Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 2)
				panelLabel.setText("Approx Reduced Plan Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 3)
				panelLabel.setText("Approx Compilation Cost Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 4)
				panelLabel.setText("Approx Compilation Cardinality Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 5)
				panelLabel.setText("Execution Cost Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 6)
				panelLabel.setText("Execution Cardinality Diagram");
			else 
				panelLabel.setText("Approx Plan Diagram");
			
			String txt = "    QTD: " + qname;
			/*switch(gdp.getSamplingMode())
			{
				case MessageIds.SRSWOR:txt += "SRS";break;
				case MessageIds.GSPQO:txt += "GSPQO";break;
			}
			if(gdp.isFPC())
			{
				txt += " + FPC";
			}
			txt+="]";*/
			infoLabels[0].setText(txt);
		}
		else {
			if(parent.tabbedPane.getSelectedIndex() == 1)
				panelLabel.setText("Plan Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 2)
				panelLabel.setText("Reduced Plan Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 3)
				panelLabel.setText("Compilation Cost Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 4)
				panelLabel.setText("Compilation Cardinality Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 5)
				panelLabel.setText("Execution Cost Diagram");
			else if(parent.tabbedPane.getSelectedIndex() == 6)
				panelLabel.setText("Execution Cardinality Diagram");
			else 
				panelLabel.setText("Plan Diagram");
			// panelLabel.setText("Plan Diagram");
			infoLabels[0].setText("    QTD: " + qname);
		}
		if(gdp.getDimension()>2)
		{
			infoLabels[1].setForeground(Color.MAGENTA);
			infoLabels[2].setForeground(Color.MAGENTA);
			infoLabels[1].setText("Slice # of Plans: " + new Double(values[4]).intValue());
			infoLabels[2].setText("Total # of Plans: " + new Integer(globplans));
		}
		else
		{
			infoLabels[1].setForeground(Color.MAGENTA);
			infoLabels[1].setText("# of Plans: " + new Double(values[4]).intValue());
		}
		infoLabels[3].setForeground(Color.ORANGE);
		infoLabels[4].setForeground(Color.ORANGE);
		infoLabels[5].setForeground(Color.ORANGE);
		infoLabels[6].setForeground(Color.ORANGE);
		infoLabels[7].setForeground(Color.ORANGE);
		boolean isND = false;
		if(gdp.getDimension()<=2)
		{
			if ( panelType == EXEC_PLAN_CARD_DIAGRAM || panelType == EXEC_PLAN_COST_DIAGRAM ) {
				infoLabels[3].setText("Min Act Time(s): " + df.format(values[0]));
				infoLabels[4].setText("Max Act Time(s): " + df.format(values[1]));
				infoLabels[5].setText("        ");
				infoLabels[6].setText("Min Act Card: " + df.format(values[2]));
				infoLabels[7].setText("Max Act Card: " + df.format(values[3]));
			} else {
				infoLabels[3].setText("Min Est Cost: " + df.format(values[0]));
				infoLabels[4].setText("Max Est Cost: " + df.format(values[1]));
				infoLabels[5].setText("            ");
				infoLabels[6].setText("Min Est Card: " + df.format(values[2]));
				infoLabels[7].setText("Max Est Card: " + df.format(values[3]));
			}
		}
		else
		{
			isND = true;
			if ( panelType == EXEC_PLAN_CARD_DIAGRAM || panelType == EXEC_PLAN_COST_DIAGRAM ) {
				infoLabels[3].setText("Slice Min Act Time(s): " + df.format(values[0]));
				infoLabels[4].setText("Slice Max Act Time(s): " + df.format(values[1]));
				infoLabels[5].setText("Slice Min Act Card: " + df.format(values[2]));
				infoLabels[6].setText("Slice Max Act Card: " + df.format(values[3]));
				infoLabels[7].setText("        ");
				infoLabels[8].setText("Total Min Act Time(s): " + df.format(gdp.getMinCost()));
				infoLabels[9].setText("Total Max Act Time(s): " + df.format(gdp.getMaxCost()));
				infoLabels[10].setText("Total Min Act Card: " + df.format(gdp.getMinCard()));
				infoLabels[11].setText("Total Max Act Card: " + df.format(gdp.getMaxCard()));
			} else {
				infoLabels[3].setText("Slice Min Est Cost: " + df.format(values[0]));
				infoLabels[4].setText("Slice Max Est Cost: " + df.format(values[1]));
				infoLabels[5].setText("Slice Min Est Card: " + df.format(values[2]));
				infoLabels[6].setText("Slice Max Est Card: " + df.format(values[3]));
				infoLabels[7].setText("            ");
				infoLabels[8].setText("Total Min Est Cost: " + df.format(gdp.getMinCost()));
				infoLabels[9].setText("Total Max Est Cost: " + df.format(gdp.getMaxCost()));
				infoLabels[10].setText("Total Min Est Card: " + df.format(gdp.getMinCard()));
				infoLabels[11].setText("Total Max Est Card: " + df.format(gdp.getMaxCard()));
				
				/*if( gdp.getMinCost()>values[0]||(values[1]-gdp.getMaxCost())>0.00001);
					JOptionPane.showMessageDialog (this, "ERROR IN COST ESTIMATION","Warning", JOptionPane.ERROR_MESSAGE);
				
				if( (gdp.getMinCard()-values[2])>0.00001||(values[3]-gdp.getMaxCard())>0.00001);
					JOptionPane.showMessageDialog (this, "ERROR IN CARDINALITY ESTIMATION","Warning", JOptionPane.ERROR_MESSAGE);*/
			}
		}
		DecimalFormat df_approx = new DecimalFormat("0.00");
		if(parent.approxDiagram || PicassoConstants.IS_APPROXIMATE_DIAGRAM)
		{    String txt="";
			switch(gdp.getSamplingMode())
			{
				case MessageIds.SRSWOR:txt = "[SRS]";break;
				case MessageIds.GSPQO:txt = "[GSPQO]";break;
			}
			/*if(gdp.isFPC())
			{
				txt += " + FPC";
			}*/
			//txt+="]";
		//	infoLabels[0].setText(txt);
			infoLabels[24].setText(txt+" Identity Error Tolerance: " + df_approx.format(gdp.getIdentityError())+" %");
			infoLabels[25].setText("Location Error Tolerance: " + df_approx.format(gdp.getAreaError())+ " %");
			if(!isND) {
				infoLabels[8].setText("            ");
			    infoLabels[9].setText("Sample Size : " + df_approx.format(gdp.getSampleSize())+" %");
			
			}else {
				infoLabels[12].setForeground(Color.BLUE);
				infoLabels[13].setForeground(Color.BLUE);
				infoLabels[14].setForeground(Color.BLUE);
				infoLabels[15].setForeground(Color.BLUE);
				infoLabels[12].setText("            ");
			    infoLabels[13].setText("Sample Size : " + df_approx.format(gdp.getSampleSize())+" %");
			    //infoLabels[14].setText("Identity Err: " + df_approx.format(gdp.getIdentityError())+" %");
				//infoLabels[15].setText("Location Err: " + df_approx.format(gdp.getAreaError())+ " %");
			}
		}
	}

	protected void nullInfoLabels() {
		for (int i=0; i < infoLabels.length; i++) {
			infoLabels[i].setText("");
		}
	}

	public void enableRegen(boolean value) {
		if ( regenButton != null )
			regenButton.setVisible(value);
		if ( resetButton != null )
			resetButton.setVisible(value);
	}

	public DisplayImpl drawDiagram(ServerPacket msg, int type) {
		DisplayImpl display1=null;

		//MessageUtil.CPrintToConsole(panelType + "," + type + " In Draw Graph of ::: " + panelString);
		DiagramPacket gdp = msg.diagramPacket;
		if ( gdp == null )
			return null;

		enableRegen(true);
		if ( gdp.getDimension() == 1 ) {
			display1 = Draw1DDiagram.draw(display, getPParent(), gdp, type, maps);
		} else if ( type == PLAN_DIAGRAM ) // called from plan panel
			display1 = Draw2DDiagram.draw(display, getPParent(), gdp, type, maps);
		else
			display1 = Draw3DDiagram.draw(display, getPParent(), gdp, type, maps);

		display = display1;
		diagramComponent = display.getComponent();
		add(diagramComponent, BorderLayout.CENTER);

		return display;
	}


	void redrawDiagram() {
		ServerPacket msg;

		msg = parent.getServerPacket();

		double[] matrix = display.make_matrix(0, 0, 0, 0.4, 0, 0.05, 0);
		if ( panelType == EXEC_PLAN_CARD_DIAGRAM || panelType == EXEC_PLAN_COST_DIAGRAM
				|| panelType == PLAN_CARD_DIAGRAM || panelType == PLAN_COST_DIAGRAM ) {
			if ( display != null ) {
				if ( msg.diagramPacket.getDimension() == 1 ) {
					matrix = display.make_matrix(0, 0, 0, 0.45, 0, 0.05, 0);
				} else {
					matrix = display.make_matrix(48.75,52.5,60.0,0.35,0.0,0.0,0.0);
				}
			}
		} else {
				matrix = display.make_matrix(0, 0, 0, 0.4, 0, 0.05, 0);
		}
		try {
			
				ProjectionControl projCont = display.getProjectionControl();
				projCont.setMatrix(matrix);
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
	
	
	private void setPredicateLabel() {
		Object[] keys = predicateValues.keySet().toArray();
		
		String str = "";
		//MessageUtil.CPrintToConsole("Predicate Values :: " + keys.length);
		
		
		for (int i=0; i < keys.length; i++) {
			int index = ((Integer)keys[i]).intValue();
			//
	//		if(predframe!=null)
	//			str += "Sel["+(queryParams.elementAt(index) + "]=" +this.predframe.textFields[i].getSelectedItem());
				//str += "Sel["+(queryParams.elementAt(index) + "]=" +PicassoConstants.prevselected[i]);
			if(PicassoConstants.first==false && panelType != REDUCED_PLAN_DIAGRAM)
				str += "Sel["+(queryParams.elementAt(index) + "]=" +PicassoConstants.slice[i]);
			else
				str += "Sel["+(queryParams.elementAt(index) + "]=" + predicateValues.get(keys[i]) + "%; ");
		}
		predicateLbl.setText(str);
	}

	public void setPredicateValues(Hashtable values) {
		predicateValues = values;
		setPredicateLabel();
		if(parent.getCurrentTab() instanceof ReducedPlanPanel)
		{
			// setSliceDiagramPacket(true);
			// dothedrawing();
			emptyPanel();
			((ReducedPlanPanel)(this)).setNgdp(parent.getFullReducedDiagramPacket(), getPParent().getServerPacket(),REDUCED_PLAN_DIAGRAM);
		}
		else
		{
			if(parent.getExecDiagramPacket() != null && parent.getExecDiagramPacket().getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM)
					&& (parent.execCardDisplay || parent.execCostDisplay))
				setSliceExecDiagramPacket();
			else
				setSliceDiagramPacket(false);
			dothedrawing();
		}
		//setSettingsChanged(true); //apa
	}

	public Hashtable getPredicateValues() {
		if ( queryParams.size() <= 2 )
			return null;
		
		return predicateValues;
	}

	public Vector getDimensions() {
		Vector vals = new Vector();
		
		vals.add(new Integer(dimensionBox1.getSelectedIndex()));
		vals.add(new Integer(dimensionBox2.getSelectedIndex()));
		PicassoConstants.a[0]=new Integer(dimensionBox1.getSelectedIndex()).intValue();//rss
		PicassoConstants.a[1]=new Integer(dimensionBox2.getSelectedIndex()).intValue();//rss
		return(vals);
	}

	
	
	int prevDim1 = 0, prevDim2 = 1;
//	ActionListener actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if ( e!=null && e.getSource() == resetButton ) 
			{
				redrawDiagram();
			} 
			else if (e!=null && e.getSource() == regenButton) 
			{
				
				if(panelType != REDUCED_PLAN_DIAGRAM)	
				{
					this.parent.getDBSettingsPanel().readonly=false;
					parent.getDBSettingsPanel().regen=true;
					parent.getDBSettingsPanel().oldRangeFlag=parent.getDBSettingsPanel().setRangeFlag;
					parent.getDBSettingsPanel().setRangeFlag=true;
					parent.getDBSettingsPanel().oldResFlag=parent.getDBSettingsPanel().setResFlag;
					parent.getDBSettingsPanel().setResFlag=true;
					parent.getDBSettingsPanel().rangeIndex=parent.getDBSettingsPanel().getRangeType();
					parent.getDBSettingsPanel().resIndex=(parent.getDBSettingsPanel().getResIndex());
					parent.getDBSettingsPanel().setrange(1);
					parent.getDBSettingsPanel().setresbox(5);
					ResolutionFrame	resolutionframe = new ResolutionFrame(parent.getClientPacket(),parent.getDBSettingsPanel().getDistribution(),parent.getQueryBuilderPanel(),parent);
					resolutionframe.setModal(true);
					resolutionframe.pack();
					parent.emptyAllTabs();
					resolutionframe.setVisible(true);
				}
				//		parent.emptyAllTabs();
				//		parent.getResFrame().setVisible(true);
				parent.setqp(true);
				if ( panelType == REDUCED_PLAN_DIAGRAM )
					getPParent().reduceDiagram(2); // 2 is the index of the reduced plan panel
				else
					parent.processMessage(MessageIds.TIME_TO_GENERATE);
			}
			else if (e!=null && e.getSource() == setButton ) {
				String str[] = new String[2];
				
				str[0] = (String)dimensionBox1.getSelectedItem();
				str[1] = (String)dimensionBox2.getSelectedItem();
				PicassoConstants.a[0]=new Integer(dimensionBox1.getSelectedIndex()).intValue();//rss
				PicassoConstants.a[1]=new Integer(dimensionBox2.getSelectedIndex()).intValue();//rss
				predframe=new PredicateValuesFrame(this, queryParams, predicateValues,parent.getClientPacket().getQueryPacket().getResolution(),getClientPacket().getQueryPacket().getStartPoint(),getClientPacket().getQueryPacket().getEndPoint(),parent.getClientPacket().getQueryPacket().getDistribution());
				predframe.setVisible(true);
			}
			else //one of the two dimensionBoxes. 
			{
				//one-dimensional QT
				if ( queryParams.size() == 1 )
					return;

				String str1 = (String)dimensionBox1.getSelectedItem();
				String str2 = (String)dimensionBox2.getSelectedItem();

				int index1 = dimensionBox1.getSelectedIndex();
				int index2 = dimensionBox2.getSelectedIndex();
				PicassoConstants.a[0]=new Integer(dimensionBox1.getSelectedIndex()).intValue();//rss
				PicassoConstants.a[1]=new Integer(dimensionBox2.getSelectedIndex()).intValue();//rss
//				if ( prevDim1 == index1 && prevDim2 == index2 )
//				return;

				//so that the setSelectedIndex below doesn't recursively call this
				dimensionBox1.removeActionListener(this);
				dimensionBox2.removeActionListener(this);
				PicassoConstants.first=true;
				//MessageUtil.CPrintToConsole(" STR 1 :: " + str1 + " STR2 :: " + str2 + " Index :: " + index1 + " Index2 :: " + index2);
				if ( str1 != null && str2 != null && str1.equals(str2) ) {
					if(e.getSource().equals(dimensionBox1)){
//						dimensionBox1.setSelectedIndex(prevDim2);
						//change the other box to what was there in this box earlier
						dimensionBox2.setSelectedIndex(prevDim1);
						index2 = prevDim1;
					}
					else{
//						dimensionBox2.setSelectedIndex(prevDim1);
						//change the other box to what was there in this box earlier
						dimensionBox1.setSelectedIndex(prevDim2);
						index1 = prevDim2;
					}
					PicassoConstants.first=false;
				} 
				//save it for next time (in case both boxes have same label)
				prevDim1 = index1;
				prevDim2 = index2;

				//we removed it above, so put it back
				dimensionBox1.addActionListener(this);
				dimensionBox2.addActionListener(this);
				PicassoConstants.a[0]=new Integer(dimensionBox1.getSelectedIndex()).intValue();//rss
				PicassoConstants.a[1]=new Integer(dimensionBox2.getSelectedIndex()).intValue();//rss


				//MessageUtil.CPrintToConsole("QUERY PARAMS SIZE :: " + queryParams.size());
				for (int i=0; i < queryParams.size(); i++) {
					Integer dimKey = new Integer(i);
					String dimVal = (String)predicateValues.get(dimKey);
					if ( i == index1 || i == index2 ) {
						if ( dimVal != null )
							predicateValues.remove(dimKey);
					} else {
						if ( dimVal == null )
						{
							int[] resolution = parent.getClientPacket().getQueryPacket().getResolution();
							double[] startpoint = parent.getClientPacket().getQueryPacket().getStartPoint();
							double[] endpoint = parent.getClientPacket().getQueryPacket().getEndPoint();
							String distribution = parent.getClientPacket().getQueryPacket().getDistribution();
							for(int j=0;j<PicassoConstants.NUM_DIMS;j++)											//rss
								if(resolution[j]==0) 
									resolution[j]=1;	
							double[][] selectivity=new double [PicassoConstants.NUM_DIMS][];						//rss
							for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
								selectivity[j] = new double [resolution[j]];				//rss
							String[][] sselectivity=new String[PicassoConstants.NUM_DIMS][];						//rss
							for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
								sselectivity[j] = new String [resolution[j]];				//
							for(int j=0;j<PicassoConstants.NUM_DIMS;j++)											//
								sselectivity[j][0]="0";		
							if(distribution!=null)
								PredicateValuesFrame.genSelectivities(selectivity,sselectivity,resolution,startpoint, endpoint, distribution);
							predicateValues.put(dimKey, sselectivity[i][0]);
						}
					}
				}
				setPredicateLabel();
				//setSettingsChanged(true); //apa
				//MessageUtil.CPrintToConsole("Predicate Label Set");
				// the parent.getFullGDP() passes the wrong (startpoint, endpoint) values when a 2nd slice of a reduced diagram is viewed.
				if(parent.getCurrentTab() instanceof ReducedPlanPanel)
				{
					if(parent.getDiagramPacket() == null)
						setSliceDiagramPacket(false);
						
					emptyPanel();
					// setSliceDiagramPacket(true);
					((ReducedPlanPanel)(this)).setNgdp(parent.getFullReducedDiagramPacket(), getPParent().getServerPacket(),REDUCED_PLAN_DIAGRAM);
				}
				else
				{
					if(parent.getExecDiagramPacket() != null && parent.getExecDiagramPacket().getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM) 
							&& (parent.execCardDisplay || parent.execCostDisplay))
						setSliceExecDiagramPacket();
					else 
						setSliceDiagramPacket(false);
					dothedrawing();
					parent.setqp(false);
				}
				dimensionBox1.setVisible(true);
				dimensionBox2.setVisible(true);
				dimensionBox1.setEnabled(true);
				if(PicassoConstants.NUM_DIMS > 1)
				dimensionBox2.setEnabled(true);
			}
		this.parent.getDBSettingsPanel().readonly=true;
	}
	

	void setSliceDiagramPacket(boolean is_reduced)
	{
	//set the new diagram packet dp using fulldp
	DiagramPacket fulldp;
	DiagramPacket dp;

	if(is_reduced)
	{
		fulldp = getPParent().getFullReducedDiagramPacket();	
		dp = new DiagramPacket(getPParent().getFullReducedDiagramPacket());
	}
	else
	{
		fulldp = getPParent().getFullDiagramPacket();
		dp = new DiagramPacket(getPParent().getFullDiagramPacket());
	}
	DataValues fulldpData[] = fulldp.getData();
	
	int[] res = new int [PicassoConstants.NUM_DIMS];	
	double[] startpt = new double[PicassoConstants.NUM_DIMS];
	double[] endpt = new double[PicassoConstants.NUM_DIMS];
	for(int i=0;i<fulldp.getDimension();i++)
	{
		res[i]=fulldp.getResolution(i);
		startpt[i] = fulldp.getQueryPacket().getStartPoint(i);
		endpt[i] = fulldp.getQueryPacket().getEndPoint(i);
	}
	int dim=fulldp.getDimension();
	
	//these are the DataValues of the required slice
	//that are filled and later assigned to dp.
	DataValues myData[];
	
	// added to get the correct size of mydata
	int sliceprod = 1;
	int dim1 = dimensionBox1.getSelectedIndex();
	int dim2 = dimensionBox2.getSelectedIndex();
	PicassoConstants.a[0]=dim1;
	PicassoConstants.a[1]=dim2;
	if(dim1 >= 0 && dim2 >= 0)
		sliceprod = res[dim1]*res[dim2];
	else 
		sliceprod = res[dim1];
	// sliceprod calculated
	if(fulldp.getDimension()==1)
		myData = new DataValues[res[dim1]];
	else
		myData = new DataValues[sliceprod];
	int wi=0; //the write index for myData
	
	//Low to High will be:
	//0 to res in the (upto) two dimensions that are displayed
	//0 to 0 in the dimensions that do not exist
	//x to x in dimensions that exist but whose values are constant for that slice
	int rangeLow[] = new int[6];
	int rangeHigh[] = new int[6];
	
	for(int i=0;i<dim;i++)
	{
		String theval = (String)predicateValues.get(new Integer(i));
		if(theval == null)
		{
			rangeLow[i]=0;
			rangeHigh[i]=res[i];
		}
		else
		{
			double[][] selectivity=new double [PicassoConstants.NUM_DIMS][];						//rss
			String[][] sselectivity=new String[PicassoConstants.NUM_DIMS][];						//rss
			
			for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
				selectivity[j] = new double [res[j]];				//rss
			
			for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
				sselectivity[j] = new String [res[j]];
			
			PredicateValuesFrame.genSelectivities(selectivity,sselectivity,res,startpt, endpt, getPParent().getDiagramPacket().getQueryPacket().getDistribution());
			for(int j=0;j<res[i];j++)
			{
				if(theval.equals(sselectivity[i][j]))
				{
					rangeLow[i] = rangeHigh[i] = j;
				}
			}
		}
	}
	/*System.out.println("BEFORE");
	for(int i = fulldp.getResolution(PicassoConstants.a[1])-1; i >=0; i--)
	{
		for(int j = 0; j < fulldp.getResolution(PicassoConstants.a[0]); j++)
			System.out.print(fulldp.getData()[i*fulldp.getResolution(PicassoConstants.a[0])+j].getPlanNumber() + "  ");
		System.out.println("");
	}*/
	
	for(int i=0;i<fulldpData.length;i++)
	{
		
		int j;
		int resprod = 1;
		for(j=0;j<dim;j++)  //check if need to go down from dim-1 to 0 //apa:TODO
		{
			// made a few changes here. -ma
			int someval=(int)(i/(double)resprod)%res[j];   
			resprod *= res[j];
			if(! (someval>=rangeLow[j] && someval<=rangeHigh[j]))
				break;
		}
		if(j==dim)
			myData[wi++] = fulldpData[i];
	}
	/*System.out.println("BEFORE1");
	for(int i = fulldp.getResolution(1)-1; i >=0; i--)
	{
		for(int j = 0; j < fulldp.getResolution(0); j++)
			System.out.print(myData[i*fulldp.getResolution(0)+j].getPlanNumber() + "  ");
		System.out.println("");
	}*/
	dp.setDataPoints(myData);
	if(dp.getDimension()>1 && dim2<dim1)
		dp.transposeDiagram();
	if(is_reduced)
		parent.setReducedDiagramPacket(dp, parent.getFullSortedPlan());
	else
		parent.setDiagramPacket(dp);
	
	String [] TwoRelNames;
	String [] TwoAttrNames;
	String [] TwoAttrTypes;
	String[] TwoConstants;
	
	if(dp.getDimension() == 1)
	{
		TwoRelNames = new String[1];
		TwoRelNames[0] = fulldp.getRelationNames()[dim1];
		TwoAttrNames = new String[1];
		TwoAttrTypes = new String[1];
		TwoAttrNames[0] = fulldp.getAttributeNames()[dim1];
		TwoConstants = fulldp.getConstants();
		TwoAttrTypes[0] = fulldp.getAttributeTypes()[dim1];
	}
	else //2-d or more, set the required two dimensions depending on dim1 and dim2
	{
		TwoRelNames = new String[2];
		TwoAttrNames = new String[2];
		TwoAttrTypes = new String[2];
		TwoRelNames[0] = fulldp.getRelationNames()[dim1];
		TwoRelNames[1] = fulldp.getRelationNames()[dim2];
		TwoAttrNames[0] = fulldp.getAttributeNames()[dim1];
		TwoAttrNames[1] = fulldp.getAttributeNames()[dim2];
		TwoAttrTypes[0] = fulldp.getAttributeTypes()[dim1];
		TwoAttrTypes[1] = fulldp.getAttributeTypes()[dim2];
		TwoConstants = new String[res[dim1] + res[dim2]];
		for(int i=0;i<fulldp.getDimension();i++)
		res[i] = fulldp.getResolution(i);
		int j=0;
		
		int[] ressum = new int [fulldp.getDimension()];
		for(int i = 1; i < ressum.length; i++)
			ressum[i] += res[i-1] + ressum[i-1];
		
		for(int i=ressum[dim1];i<ressum[dim1]+res[dim1];i++)
		{
			TwoConstants[j++] = fulldp.getConstants()[i];
		}
		for(int i=ressum[dim2];i<ressum[dim2]+res[dim2];i++)
		{
			TwoConstants[j++] = fulldp.getConstants()[i];
		}
		
		
		
		Vector vect = new Vector();
		vect.add(new Integer(dim1));
		vect.add(new Integer(dim2));
		clientPacket.setDimensions(vect);
	}
	dp.setRelationNames(TwoRelNames);
	dp.setAttributeNames(TwoAttrNames);
	dp.setAttrTypes(TwoAttrTypes);
	dp.setConstants(TwoConstants);
	
	Hashtable tmp = getPredicateValues();
	if(tmp!=null) {
		// Set the selectivity values properly..
		Object[] keys = tmp.keySet().toArray();
		Hashtable tmp1 = new Hashtable();
		for (int i=0; i < keys.length; i++) {
			String predValue = (String)tmp.get(keys[i]);
			//int pv = Integer.parseInt(predValue);
			double pv = Double.parseDouble(predValue);
//			if(dist_const.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
//			{
//				if ( pv < 100/(qp.getResolution()*2))
//					pv = 100/(qp.getResolution()*2);
//			}
                            tmp1.put(keys[i], ""+pv);
		}
		clientPacket.setAttributeSelectivities(tmp1);
	}
	else
	{
		clientPacket.setAttributeSelectivities(null);
	}
	
	
	if(is_reduced)
	{
		int temp = dp.getMaxPlanNumber();
		//Set the reduced's max plan number to that of the original so that
		//getSortedPlanArray doesn't crib.
		dp.setMaxPlanNumber(parent.getServerPacket().diagramPacket.getMaxPlanNumber());
		parent.setReducedDiagramPacket(dp, dp.getSortedPlanArray());
		//Put it back.
		dp.setMaxPlanNumber(temp);
		//this will call the (re)drawing function
		// ((ReducedPlanPanel)(this)).setNgdp(dp, getPParent().getServerPacket(),REDUCED_PLAN_DIAGRAM);
	}
	else
	{
		parent.getServerPacket().diagramPacket = dp;
//		parent.setDiagramPacket(dp); //done in drawAllDiagrams
	}

	/*
	this.resolution = p.resolution;
    this.dimension = p.dimension;
    this.maxCard = p.maxCard;
    this.maxCost = p.maxCost;
    this.maxPlans = p.maxPlans;
    this.minCard = p.minCard;
    this.minCost = p.minCost;
    
    this.picsel=copyfarray(p.picsel);
    this.plansel=copyfarray(p.plansel);
    this.predsel=copyfarray(p.predsel);
    this.datasel=copyfarray(p.datasel);
    this.constants=copysarray(p.constants);
    this.attributes=copysarray(p.attributes);
    this.relationNames=copysarray(p.relationNames);
    this.dataPoints=copydata(p.dataPoints);
    this.queryPacket = new QueryPacket(p.queryPacket);
	 */
	
	
}

	void dothedrawing()
	{
		dimensionBox1.removeActionListener(this);
		dimensionBox2.removeActionListener(this);
		parent.setFillBottomBarFlag(false);
		parent.drawAllDiagrams(parent.getServerPacket(),(PicassoPanel)this);
		parent.setFillBottomBarFlag(true);
		parent.repaint();
		dimensionBox1.addActionListener(this);
		dimensionBox2.addActionListener(this);
	}
	
	protected void fillBottomBar() {
		//if ( queryParamsPrevSize == queryParams.size() )
		//	return;
		int[] resolution = parent.getClientPacket().getQueryPacket().getResolution();
		double[] startpoint = parent.getClientPacket().getQueryPacket().getStartPoint();
		double[] endpoint = parent.getClientPacket().getQueryPacket().getEndPoint();
		String distribution = parent.getClientPacket().getQueryPacket().getDistribution();
		for(int i=0;i<PicassoConstants.NUM_DIMS;i++)											//rss
			if(resolution[i]==0) 
				resolution[i]=1;	
		double[][] selectivity;
		String[][] sselectivity;
		selectivity = new double[PicassoConstants.NUM_DIMS][];
		sselectivity = new String[PicassoConstants.NUM_DIMS][];
		for(int i=0 ;i<PicassoConstants.NUM_DIMS;i++)							//rss
		{												//
		selectivity[i] = new double[resolution[i]];		//
		sselectivity[i] = new String[resolution[i]];	//
		}
		if(distribution!=null)
			PredicateValuesFrame.genSelectivities(selectivity,sselectivity,resolution,startpoint, endpoint, distribution);
		
		int qSize = queryParams.size();
		
		String []relNames = null;
		try {
			if(parent.getClientPacket().getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
				relNames = parent.getFullExecDiagramPacket().getRelationNames();
			else
				relNames = parent.getFullDiagramPacket().getRelationNames();
		} catch (Exception e) {
			if(relNames == null)
			{
				relNames = new String [qSize];
			}
		}
		predicateValues.clear();
		dimensionBox1.removeActionListener(this);
		dimensionBox2.removeActionListener(this);

		dimensionBox1.setSelectedIndex(-1);
		dimensionBox2.setSelectedIndex(-1);
	
		dimensionBox1.removeAllItems();
		dimensionBox2.removeAllItems();
		for (int i=0; i < qSize; i++) {
			// The dimension boxes' elements are now preceeded with their table name
			String str = relNames[i] + "." + (String)queryParams.elementAt(i);
			//str = str.substring(0, str.indexOf(':')) + " 0";
			dimensionBox1.addItem(str);
			dimensionBox1.setSelectedIndex(-1);
			dimensionBox2.addItem(str);
			dimensionBox2.setSelectedIndex(-1);
			if ( i > 1 )
			{
				if(predicateValues.get(new Integer(i)) == null)
						predicateValues.put(new Integer(i), sselectivity[i][0]);
			}
		}
		
		if ( qSize <= 2 ) {
			setButton.setEnabled(false);
			if ( predicateValues == null )
				predicateValues = new Hashtable();
			else {
				predicateValues.clear();
				setPredicateLabel();
			}
//			parent.getDBSettingsPanel().setVisibleDisp(false);
		} else {
			setPredicateLabel();
			setButton.setEnabled(true);
//			parent.getDBSettingsPanel().setVisibleDisp(true);
		}
		
		if ( qSize == 0 ) {
			dimensionBox1.setEnabled(false);
			dimensionBox2.setEnabled(false);
		} else if ( qSize == 1 ) {
			dimensionBox1.setEnabled(true);
			dimensionBox2.setEnabled(false);
			dimensionBox1.setSelectedIndex(0);
		} else {
			dimensionBox1.setEnabled(true);
			dimensionBox2.setEnabled(true);
			dimensionBox1.setSelectedIndex(PicassoConstants.a[0]);
			dimensionBox2.setSelectedIndex(PicassoConstants.a[1]);
		}
		dimensionBox1.addActionListener(this);
		dimensionBox2.addActionListener(this);
		/*PicassoConstants.a[0]=new Integer(dimensionBox1.getSelectedIndex()).intValue();//rss
		PicassoConstants.a[1]=new Integer(dimensionBox2.getSelectedIndex()).intValue();*/
	}
	
	public String getQueryAttrName(int index) {
		return((String)queryParams.elementAt(index));
	}
	
	protected void addToQueryParams() {
		
		try {
			queryParams.clear();
			// This code only supports predicate from 1-9.. Need to change the algo
			// if we need to support greater than 9.
			String content=null;
			
			if(PicassoConstants.IS_PKT_LOADED)
				getClientPacket();
			content = clientPacket.getQueryPacket().getQueryTemplate().toLowerCase();
			
			content=content+" ";
			int beginIndex = 0, lastIndex = 0;
			int index = 1;
			
			//MessageUtil.CPrintToConsole("In Highlighting");
			//apa
			//queryParamsPrevSize = queryParams.size();
			//queryParams.removeAllElements();
			//apae
			
			int 		curIndex=0;
			String[]	params = new String[9];
			for (int i=0; i < params.length; i++)
				params[i] = null;
			int NUM_DIMS=0;
			while ( (lastIndex = content.indexOf(":varies", lastIndex)) != -1) {
				lastIndex += 6;
				NUM_DIMS++;
				beginIndex = lastIndex;
				while ( true ) {
					
					//MessageUtil.CPrintToConsole("STR : " + str);
					if ( content.startsWith("and", beginIndex) 
							|| content.startsWith("where", beginIndex)
							|| content.startsWith("or ", beginIndex)
							|| content.startsWith("like ", beginIndex)
							|| beginIndex == 0 )
						break;
					//MessageUtil.CPrintToConsole("content " + content.substring(beginIndex));
					beginIndex--;
				}
				
				String s = content.substring(beginIndex, lastIndex+2);
				s = s.replaceAll("\\s+", " ");
				
				int spIndex = s.indexOf(' ');
				//MessageUtil.CPrintToConsole(spIndex + " SubString :: " + s);
				s = s.substring(spIndex+1);
				int endWord = s.length();
				for (int i=0; i < s.length(); i++) {
					char c = s.charAt(i);
					if ( c == ' ' ) {
						endWord = i;
						break;
					}
				}
				s = s.substring(0, endWord);
				params[curIndex] = s;
				//queryParams.add(s);
				
				
				index++;
				lastIndex+=2;
				curIndex++;
			}
			PicassoConstants.NUM_DIMS=NUM_DIMS;
			for (int i=0; i < 9; i++) {
				if ( params[i] != null )
					queryParams.add(params[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void setSliceExecDiagramPacket()
	{
		//set the new diagram packet dp using fulldp
		DiagramPacket fulldp;
		DiagramPacket dp;
	
		fulldp = getPParent().getFullExecDiagramPacket();
		dp = new DiagramPacket(getPParent().getFullExecDiagramPacket());

		DataValues fulldpData[] = fulldp.getData();
		
		int[] res = new int [PicassoConstants.NUM_DIMS];	
		double[] startpt = new double[PicassoConstants.NUM_DIMS];
		double[] endpt = new double[PicassoConstants.NUM_DIMS];
		for(int i=0;i<fulldp.getDimension();i++)
		{
			res[i]=fulldp.getResolution(i);
			startpt[i] = fulldp.getQueryPacket().getStartPoint(i);
			endpt[i] = fulldp.getQueryPacket().getEndPoint(i);
		}
		int dim=fulldp.getDimension();
		
		//these are the DataValues of the required slice
		//that are filled and later assigned to dp.
		DataValues myData[];
		
		// added to get the correct size of mydata
		int sliceprod = 1;
		int dim1 = dimensionBox1.getSelectedIndex();
		int dim2 = dimensionBox2.getSelectedIndex();
		PicassoConstants.a[0]=dim1;
		PicassoConstants.a[1]=dim2;
		
		/*if(dim1 == -1 || dim2 == -1)
		{
			dim1 = 0;
			dim2 = 1;
		}*/
		
		if(dim1 >= 0 && dim2 >= 0)
			sliceprod = res[dim1]*res[dim2];
		else if (dim1 >= 0) 
			sliceprod = res[dim1];
		else
			System.out.println("This line should never be reached");
		
		// sliceprod calculated
		if(fulldp.getDimension()==1)
			myData = new DataValues[res[dim1]];
		else
			myData = new DataValues[sliceprod];
		int wi=0; //the write index for myData
		
		//Low to High will be:
		//0 to res in the (upto) two dimensions that are displayed
		//0 to 0 in the dimensions that do not exist
		//x to x in dimensions that exist but whose values are constant for that slice
		int rangeLow[] = new int[6];
		int rangeHigh[] = new int[6];
		
		for(int i=0;i<dim;i++)
		{
			String theval = (String)predicateValues.get(new Integer(i));
			if(theval == null)
			{
				rangeLow[i]=0;
				rangeHigh[i]=res[i];
			}
			else
			{
				double[][] selectivity=new double [PicassoConstants.NUM_DIMS][];						//rss
				String[][] sselectivity=new String[PicassoConstants.NUM_DIMS][];						//rss
				
				for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
					selectivity[j] = new double [res[j]];				//rss
				
				for (int j=0;j<PicassoConstants.NUM_DIMS;j++)										//rss
					sselectivity[j] = new String [res[j]];
				
				PredicateValuesFrame.genSelectivities(selectivity,sselectivity,res,startpt, endpt, getPParent().getExecDiagramPacket().getQueryPacket().getDistribution());
				for(int j=0;j<res[i];j++)
				{
					if(theval.equals(sselectivity[i][j]))
					{
						rangeLow[i] = rangeHigh[i] = j;
					}
				}
			}
		}
		/*System.out.println("BEFORE");
		for(int i = fulldp.getResolution(PicassoConstants.a[1])-1; i >=0; i--)
		{
			for(int j = 0; j < fulldp.getResolution(PicassoConstants.a[0]); j++)
				System.out.print(fulldp.getData()[i*fulldp.getResolution(PicassoConstants.a[0])+j].getPlanNumber() + "  ");
			System.out.println("");
		}*/
		
		for(int i=0;i<fulldpData.length;i++)
		{
			
			int j;
			int resprod = 1;
			for(j=0;j<dim;j++)  //check if need to go down from dim-1 to 0 //apa:TODO
			{
				// made a few changes here. -ma
				int someval=(int)(i/(double)resprod)%res[j];   
				resprod *= res[j];
				if(! (someval>=rangeLow[j] && someval<=rangeHigh[j]))
					break;
			}
			if(j==dim)
				myData[wi++] = fulldpData[i];
		}

		dp.setDataPoints(myData);
		if(dp.getDimension()>1 && dim2<dim1)
			dp.transposeDiagram();

		parent.setExecDiagramPacket(dp); // Sets the edp to the correct slice of the full exec diagram 
		
		String [] TwoRelNames;
		String [] TwoAttrNames;
		String [] TwoAttrTypes;
		String[] TwoConstants;
		if(dp.getDimension() == 1)
		{
			TwoRelNames = new String[1];
			TwoRelNames[0] = fulldp.getRelationNames()[dim1];
			TwoAttrNames = new String[1];
			TwoAttrTypes = new String[1];
			TwoAttrNames[0] = fulldp.getAttributeNames()[dim1];
			TwoAttrTypes[0] = fulldp.getAttributeTypes()[dim1];
			TwoConstants = fulldp.getConstants();
		}
		else //2-d or more, set the required two dimensions depending on dim1 and dim2
		{
			TwoRelNames = new String[2];
			TwoAttrNames = new String[2];
			TwoAttrTypes = new String[2];
			TwoRelNames[0] = fulldp.getRelationNames()[dim1];
			TwoRelNames[1] = fulldp.getRelationNames()[dim2];
			TwoAttrNames[0] = fulldp.getAttributeNames()[dim1];
			TwoAttrNames[1] = fulldp.getAttributeNames()[dim2];
			TwoAttrTypes[0] = fulldp.getAttributeTypes()[dim1];
			TwoAttrTypes[1] = fulldp.getAttributeTypes()[dim2];
			TwoConstants = new String[res[dim1] + res[dim2]];
			for(int i=0;i<fulldp.getDimension();i++)
			res[i] = fulldp.getResolution(i);
			int j=0;
			
			int[] ressum = new int [fulldp.getDimension()];
			for(int i = 1; i < ressum.length; i++)
				ressum[i] += res[i-1] + ressum[i-1];
			
			for(int i=ressum[dim1];i<ressum[dim1]+res[dim1];i++)
			{
				TwoConstants[j++] = fulldp.getConstants()[i];
			}
			for(int i=ressum[dim2];i<ressum[dim2]+res[dim2];i++)
			{
				TwoConstants[j++] = fulldp.getConstants()[i];
			}
			Vector vect = new Vector();
			vect.add(new Integer(dim1));
			vect.add(new Integer(dim2));
			clientPacket.setDimensions(vect);
		}
		dp.setRelationNames(TwoRelNames);
		dp.setAttributeNames(TwoAttrNames);
		dp.setConstants(TwoConstants);
		
		Hashtable tmp = getPredicateValues();
		if(tmp!=null) {
			// Set the selectivity values properly..
			Object[] keys = tmp.keySet().toArray();
			Hashtable tmp1 = new Hashtable();
			for (int i=0; i < keys.length; i++) {
				String predValue = (String)tmp.get(keys[i]);
				//int pv = Integer.parseInt(predValue);
				double pv = Double.parseDouble(predValue);
	//			if(dist_const.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
	//			{
	//				if ( pv < 100/(qp.getResolution()*2))
	//					pv = 100/(qp.getResolution()*2);
	//			}
	                            tmp1.put(keys[i], ""+pv);
			}
			clientPacket.setAttributeSelectivities(tmp1);
		}
		else
		{
			clientPacket.setAttributeSelectivities(null);
		}
		parent.getServerPacket().diagramPacket = dp;
	}
}

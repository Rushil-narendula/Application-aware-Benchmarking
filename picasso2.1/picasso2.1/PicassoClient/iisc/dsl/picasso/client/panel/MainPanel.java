
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

import iisc.dsl.picasso.client.Picasso_Applet;
import iisc.dsl.picasso.client.Picasso_Frame;
import iisc.dsl.picasso.client.frame.ApproxPlanGenInfo;
import iisc.dsl.picasso.client.frame.DatabaseInfoFrame;
import iisc.dsl.picasso.client.frame.PicassoSettingsFrame;
import iisc.dsl.picasso.client.frame.PredicateValuesFrame;
import iisc.dsl.picasso.client.frame.ResolutionFrame;
import iisc.dsl.picasso.client.frame.ServerInfoDialog;
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.PicassoSettings;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.client.util.DiagramUtil;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.ds.DataValues;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainPanel extends JPanel implements ChangeListener, ActionListener {

//	Auto Generated Serial ID
	private static final long serialVersionUID = 2881159848139303053L;
	public static boolean IS_APPLET = true;

	private ClientPacket 		message;
	private DiagramPacket		fulldp=null, dp=null, fullrdp=null, rdp=null, edp=null;
	private ServerPacket odp=null;
	//added for multiplan
//	private DiagramPacket dp_array[];
//	private java.util.ArrayList sortedPlanCountList;
	//addition ends here
	//private	Vector				queryNames;
	private int theMaxPlanNumber;
	private ServerPacket		serverPkt;
	public boolean 				haveSettingsChanged = false;//danger
	private PicassoPanel		currentPanel;
	private String 				serverName;
	private int					serverPort;
	private int [][]			fullSortedPlanCount;
	private int	[][]			fullRSortedPlanCount;
	private int [][] 			fullExecSortedPlanCount;
	static private boolean qpset=false;
	public boolean retainSlice; 
	private boolean fillBottomBar;
	public boolean paramsChanged = false;

	/*private static final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	public static final String  FileName="client" + df.format(new Date()).replace(' ', '.') + ".log";
	public static final String  ErrFileName="client" + df.format(new Date()) + ".log";*/

	static final Calendar c = Calendar.getInstance();
	/*public static final String  FileName="..\\Logs\\client" +
		(c.get(Calendar.MONTH)+1) + "." + c.get(Calendar.DAY_OF_MONTH) + "." +
		c.get(Calendar.YEAR) + "." +
		c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) +
		".log";
	public static final String  ErrFileName="client" +
		c.get(Calendar.MONTH) + "." + c.get(Calendar.DAY_OF_MONTH) + "." +
		c.get(Calendar.YEAR) + "." +
		c.get(Calendar.HOUR) + "." + c.get(Calendar.MINUTE) +  ".log";*/

	public static String  FileName="client" +
		c.get(Calendar.DAY_OF_MONTH) + "." + (c.get(Calendar.MONTH)+1) + "." +
		c.get(Calendar.YEAR) + "." +
		c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) +
		".log";

	/*public static String  ErrFileName="client" +
		c.get(Calendar.DAY_OF_MONTH) + "." + (c.get(Calendar.MONTH)+1) + "." +
		c.get(Calendar.YEAR) + "." +
		c.get(Calendar.HOUR) + "." + c.get(Calendar.MINUTE) +  ".log";*/

	//private Vector				queries;

	/* This contains in index 0 : for each plan what is its position(to get the right color..
	 * index 1 : the count for this plan #
	 * index 2 : reverse mapping..
	 * sortedPlan[0][0] contains for plan # 0 the position it occupies
	 * sortedPlan[1][0] plan # 0 covers how many points
	 * sortedPlan[2][0] the plan # that covers the maximum points
	 * Ideally should have been in Graph data packet, but since GDP goes back
	 * and forth between client and server didn't want to put it there.
	 */
	private int[][]					sortedPlanCount, rSortedPlanCount, execSortedPlanCount;
	private DiagramPacket 					fulledp;

	private QueryBuilderPanel 		queryBuilderPanel;
	private PlanCostPanel 			planCostPanel, execPlanCostPanel;
	//made protected so as to read the diff value from PicassoPanel
	protected PlanPanel 				planPanel;
	private ReducedPlanPanel		reducedPlanPanel;
	private PlanCardPanel 			planCardPanel, execPlanCardPanel;
	private SelectivityPanel		selecPanel;
	private DataBaseSettingsPanel 	dbSettingsPanel;
	public 	JTabbedPane 			tabbedPane;
	private LegendPanel				legendPanel;
	private LogoPanel 				logoPanel;
	public ResolutionFrame 		resolutionframe;
	private JLabel					bottomField;
	private JButton					stopButton, pauseButton, resumeButton;
	public int 						slice;
	public int						estimatedTime;
	private double					prevThreshold = -1,
	curThreshold = -1;
	private static double oldthreshold;
	private static int oldredalgo;
	private static boolean oldOpLvl;
	boolean					planDisplay = false,
	costDisplay = false,
	cardDisplay = false,
	execCostDisplay = false,
	execCardDisplay = false,
	reducedDisplay = false,
	//msgRcvd = false,
	msgSent = false,
	execMsgRcvd = false,
	selecDisplay = false;
	public boolean msgRcvd = false;
	private JButton		stopCompileButton; // ADG
	public boolean approxDiagram  = false;// ADG
	//Settings				dbSettingInfo;
	private boolean			firstTime;
	private PicassoSettings		picassoSettings;
	Picasso_Frame			frame=null;
	Picasso_Applet			applet = null;

	public MainPanel(Picasso_Frame p, String serverName, String serverPort) {
		frame = p;
		IS_APPLET = false;

		String sep = System.getProperty("file.separator");
		FileName = p.getCurrentDirectory() + sep + "PicassoRun" + sep + "Logs" + sep + FileName;
		//ErrFileName = p.getCurrentDirectory() + sep + "PicassoRun" + sep + "Logs" + sep + FileName;

		System.out.println("Log File: " + FileName);
		initMsgFields(serverName, serverPort);

		//dbSettingInfo = PicassoUtil.readSettings(Constants.SETTINGS_FILE);
		picassoSettings = new PicassoSettings(PicassoConstants.DB_SETTINGS_FILE);
		createGUI();
		dbSettingsPanel.populateConnectionSettingsBox();
		dbSettingsPanel.setConnectionSettingsBox(null);

		fillBottomBar = true;
		//Uncomment these 2 lines to connect to default PicassoServer at startup.
		//setSettingsChanged(true);
		//sendInitialMessageToServer();
	}

	public MainPanel(String serverName, String serverPort) {

		//Execute a job on the event-dispatching thread:
		//creating this applet's GUI.
		initMsgFields(serverName, serverPort);


		//dbSettingInfo = PicassoUtil.readSettings(Constants.SETTINGS_FILE);
		picassoSettings = new PicassoSettings(PicassoConstants.DB_SETTINGS_FILE);
		createGUI();
		dbSettingsPanel.populateConnectionSettingsBox();
		dbSettingsPanel.setConnectionSettingsBox(null);

		fillBottomBar = true;
		//Uncomment these 2 lines to connect to default PicassoServer at startup.
		//setSettingsChanged(true);
		//sendInitialMessageToServer();
	}
	public void setClientPacket(ClientPacket cp)
	{
		message = cp;
	}
	public void setQueryPlusPlan(ClientPacket cp, String absPlan, int sortedPlan[][]) {
		if (absPlan == null) {
			JOptionPane.showMessageDialog(this,"Sorry! Could not fetch the abstract plan.","No abstract plan!",JOptionPane.ERROR_MESSAGE);
		}
		else {
			int val = JOptionPane.showConfirmDialog(this,"Switching to the query template appended with the selected plan. You can edit the query template or the QTD.\nDo you want to continue?","QueryTemplate with plan",JOptionPane.YES_NO_OPTION); 
			if ( val == 0 ) {
				tabbedPane.setSelectedIndex(0);
				queryBuilderPanel.setQueryPlusPlan(absPlan, cp.getPlanNumbers());
			}
		}
	}
	public void setqp(boolean value)
	{
		qpset=value;
	}
	public String getCurrentDir() {
		return(frame.getCurrentDirectory());
	}

        public String getQueryName() {
		return(queryBuilderPanel.getQueryName());
	}

	public Picasso_Frame getFrame() {
		return frame;
	}

	public void setSettingsChanged(boolean value) {
		haveSettingsChanged = value;
		if ( haveSettingsChanged == true ) 
		{
			enableAllTabs();
			dp = null;
			edp = null;
			rdp = null;
		}
	}

	public void sendInitialMessageToServer() {

		//MessageUtil.CPrintToConsole("In Send First Messge To Server");
		try {
			ClientPacket cp = new ClientPacket();
			cp.setMessageId(MessageIds.GET_CLIENT_ID);
			cp.setClientId("0");
			/*String messageStr = MessageIds.CLIENT_ID + "=0&" +
			 MessageIds.MESSAGE_ID + "=" + MessageIds.GET_CLIENT_ID;*/

			// Send Message to the server and wait for the reply
			MessageUtil.sendMessageToServer(serverName,
					serverPort, cp, queryBuilderPanel);

			//JOptionPane.showMessageDialog(null, "Message Sent to Server ");
		} catch (Exception e) {
			MessageUtil.CPrintErrToConsole("Exception in send Message "  + e);
		}
	}

	public void sendCloseMessage() {
		//  Send a close connection to server..
		// Which essentially means that the server removes the clientId
		MessageUtil.sendCloseMessageToServer(serverName, serverPort, message.getClientId());
	}

	public void displayQidNames(ServerPacket msg) {
		//this.queries = msg.queries;
		dbSettingsPanel.displayQidNames(msg.queries);
	}

	public void changeQueryFields(QueryPacket qp) {
		queryBuilderPanel.changeQueryFields(qp);
	}

	public void emptyQueryFields() {
		queryBuilderPanel.emptyQueryFields();
	}

	public void initMsgFields(String srvrName, String portVal) {
		message = new ClientPacket();
		/*String srvrName = getParameter("ServerName");*/
		if ( srvrName == null )
			srvrName = "localhost";

		//String portVal = getParameter("ServerPort");
		if ( portVal == null )
			portVal = ""+PicassoConstants.SERVER_PORT;

		int srvrPort = new Integer(portVal).intValue();

		MessageUtil.CPrintToConsole("ServerName " + srvrName + " Port " + srvrPort);
		serverName = srvrName;
		serverPort = srvrPort;
	}

	public void createGUI()
	{
		//setSize(800, 500);
		setBackground(Color.WHITE);

		JPanel mainTopPanel = new JPanel(new BorderLayout());

		// Add a text box for showing status
		dbSettingsPanel = new DataBaseSettingsPanel(this);
		dbSettingsPanel.setSize(800, 100);
		dbSettingsPanel.setBackground(Color.orange);

		logoPanel = new LogoPanel();

		mainTopPanel.add(logoPanel, BorderLayout.NORTH);
		mainTopPanel.add(dbSettingsPanel, BorderLayout.CENTER);

		// Add the tabbed pane to this panel.
		tabbedPane = new JTabbedPane (JTabbedPane.TOP);
		tabbedPane.setBackground (Color.WHITE );

		queryBuilderPanel = new QueryBuilderPanel(this);
		planCostPanel = new PlanCostPanel(this, PicassoPanel.PLAN_COST_DIAGRAM);
		planPanel = new PlanPanel(this);
		planCardPanel = new PlanCardPanel(this, PicassoPanel.PLAN_CARD_DIAGRAM);
		reducedPlanPanel = new ReducedPlanPanel(this);
		execPlanCostPanel = new PlanCostPanel(this, PicassoPanel.EXEC_PLAN_COST_DIAGRAM);
		execPlanCardPanel = new PlanCardPanel(this, PicassoPanel.EXEC_PLAN_CARD_DIAGRAM);
		selecPanel = new SelectivityPanel(this);
		execPlanCostPanel.setPanelString("Execution Plan Cost Diagram");
		execPlanCardPanel.setPanelString("Execution Plan Card Diagram");
		resolutionframe = new ResolutionFrame(); 
		//DataBaseSettingsPanel databasePanel = new DataBaseSettingsPanel(message);


		Font f2 = new Font("Arial", Font.BOLD, 12);
		tabbedPane.setFont(f2);

		tabbedPane.addChangeListener(this);
		firstTime = true;
		tabbedPane.addTab("QueryTemplate", queryBuilderPanel);
		tabbedPane.addTab("Plan Diag", planPanel);
		tabbedPane.addTab("Reduced Plan Diag", reducedPlanPanel);
		tabbedPane.addTab("Comp Cost Diag", planCostPanel);
		tabbedPane.addTab("Comp Card Diag", planCardPanel);
		tabbedPane.addTab("Exec Cost Diag", execPlanCostPanel);
		tabbedPane.addTab("Exec Card Diag", execPlanCardPanel);
		tabbedPane.addTab("Sel Log", selecPanel);
		tabbedPane.setForegroundAt(0,Color.BLUE);
		tabbedPane.setForegroundAt(1,PicassoConstants.PLAN_COLOR);
		tabbedPane.setForegroundAt(2,PicassoConstants.PLAN_COLOR);
		tabbedPane.setForegroundAt(3,PicassoConstants.PLAN_COLOR);
		tabbedPane.setForegroundAt(4,PicassoConstants.PLAN_COLOR);
		tabbedPane.setForegroundAt(5,PicassoConstants.EXEC_COLOR);
		tabbedPane.setForegroundAt(6,PicassoConstants.EXEC_COLOR);
		tabbedPane.setForegroundAt(7,new Color(0xFF6E1111));//0xff00faed

		tabbedPane.setEnabledAt(1, false);
		tabbedPane.setEnabledAt(2, false);
		tabbedPane.setEnabledAt(3, false);
		tabbedPane.setEnabledAt(4, false);
		tabbedPane.setEnabledAt(5, false);
		tabbedPane.setEnabledAt(6, false);
		tabbedPane.setEnabledAt(7, false);
		tabbedPane.setToolTipTextAt(0, "Enter QueryTemplate and generate diagrams");
		tabbedPane.setToolTipTextAt(1, "Displays Plan Diagram");
		tabbedPane.setToolTipTextAt(2, "Displays Reduced Plan Diagram");
		tabbedPane.setToolTipTextAt(3, "Displays Compilation Cost Diagram");
		tabbedPane.setToolTipTextAt(4, "Displays Compilation Cardinality Diagram");
		tabbedPane.setToolTipTextAt(5, "Displays Execution Cost Diagram");
		tabbedPane.setToolTipTextAt(6, "Displays Execution Cardinality Diagram");
		tabbedPane.setToolTipTextAt(7, "Shows Selectivity Log for the Diagram");


		//tabbedPane.addTab ("Database Settings", databasePanel);

		// Add a status message bar at the bottom to send all the status and progress bars.
		JPanel mainBottomPanel = new JPanel();
		mainBottomPanel.setLayout(new GridBagLayout());

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());

		stopButton = new JButton("Cancel Processing");
		stopButton.setBackground(PicassoConstants.BUTTON_COLOR);
		stopButton.setVisible(false);
		stopButton.addActionListener(this);
		bottomPanel.add(stopButton);
		
		pauseButton = new JButton("Pause Processing");
		pauseButton.setBackground(PicassoConstants.BUTTON_COLOR);
		pauseButton.setVisible(false);
		pauseButton.addActionListener(this);
		bottomPanel.add(pauseButton);
		
		resumeButton = new JButton("Resume Processing");
		resumeButton.setBackground(PicassoConstants.BUTTON_COLOR);
		resumeButton.setVisible(false);
		resumeButton.addActionListener(this);
		bottomPanel.add(resumeButton);
		
		//stopCompileButton = new JButton("Stop Optimization");
		//stopCompileButton.setBackground(PicassoConstants.BUTTON_COLOR);
		//stopCompileButton.setVisible(false);
		//stopCompileButton.addActionListener(this);
		//bottomPanel.add(stopCompileButton);

		bottomField = new JLabel("STATUS: DONE", JLabel.CENTER);
		//Font f = bottomField.getFont();
		Font f1 = new Font("Arial", Font.BOLD, 16);
		bottomField.setFont(f1);
		bottomField.setForeground(Color.red);
		setStatusLabel("STATUS: DONE");

		mainBottomPanel.setBackground(new Color(0xfffafa9d));
		bottomPanel.setBackground(new Color(0xfffafa9d));
		logoPanel.setBackground(new Color(0xfffafa2d));
		GridBagConstraints c = new GridBagConstraints();

		//c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.HORIZONTAL;
		mainBottomPanel.add(bottomPanel, c);

		c.gridy = 1;
		mainBottomPanel.add(bottomField, c);

		//c.gridy = 2;
		//c.gridheight = 2;
		//mainBottomPanel.add(logoPanel, c);

		// Add a jpanel which is empty now, but will add progress bar to it later..
		//JPanel progressBar = new JPanel();
		//bottomPanel.add(progressBar, BorderLayout.EAST);
		legendPanel = new LegendPanel(this, 1);

		setLayout(new BorderLayout());
		//add(infoPanel, BorderLayout.EAST);
		add(legendPanel, BorderLayout.EAST);
		add(mainTopPanel, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);
		add(mainBottomPanel, BorderLayout.SOUTH);

	//	currentPanel = planPanel;
		firstTime = false;
		dbSettingsPanel.populateConnectionSettingsBox();
		dbSettingsPanel.setConnectionSettingsBox(null);

	}

	public void setPanel() {
		tabbedPane.setSelectedIndex(0);
	}

	public ResolutionFrame getResFrame()
	{
		return (resolutionframe);
	}

	public String getQueryText() {
		return queryBuilderPanel.getQueryText();
	}

	public String getQueryAttrName(int index) {
		return(queryBuilderPanel.getQueryAttrName(index));
	}

	public  ClientPacket getClientPacket() {
		if(haveSettingsChanged && !qpset)
			setClientMsgFields();
		return(message);
	}

	public void setStatusLabel(String status) {
		//char[] blank = new char[Constants.STATUS_LENGTH-status.length()];
		//Arrays.fill(blank, ' ');
		bottomField.setText(status);
	}

	public PicassoPanel getCurrentTab() {
		return currentPanel;
	}

	public LogoPanel getLogoPanel() {
		return logoPanel;
	}


	double req_thresh = -1;
	int req_no_of_plans;

	public double getThreshold() 
	{
		double threshold = -1;
		double estThresh = estimateKnee();

		while ( threshold <= 0 ) {
			double thresh = prevThreshold;
			if ( prevThreshold == -1 )
				thresh = PicassoConstants.PLAN_REDUCTION_THRESHOLD;
			String selecThreshold;
			if(PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_AG)
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "AreaGreedy Reduction:\n"+"Enter Cost Increase Threshold");
			else if(PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CG)
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "[Estimated Knee Threshold = "+estThresh+"%]\n[Estimated "+PicassoConstants.DESIRED_NUM_PLANS+"-plan Threshold = "+req_thresh+"%]\n------------------------------------------\nCostGreedy Reduction:\n"+"Enter Cost Increase Threshold\n", ""+thresh );
			else if(PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CGFPC)
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "CostGreedy (FPC) Reduction:\n"+"Enter Cost Increase Threshold\n[Estimated Knee Threshold = "+estThresh+"%]\n[Estimated "+PicassoConstants.DESIRED_NUM_PLANS+"-plan Threshold = "+req_thresh+"%]", ""+thresh );
			else if(PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_SEER)
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "SEER Reduction:\n"+"Enter Cost Increase Threshold");
			else if(PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CCSEER)
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "CC-SEER Reduction:\n"+"Enter Cost Increase Threshold", "" + thresh);
			else //LiteSEER
				selecThreshold = JOptionPane.showInputDialog(this.getParent(), "LiteSEER Reduction:\n"+"Enter Cost Increase Threshold", "" + thresh);

			if ( selecThreshold == null ) 
			{
				tabbedPane.setSelectedIndex(0); // go back to the query builder panel
				return -1;
			}

			try 
			{
				threshold = Double.parseDouble(selecThreshold);
				if ( threshold <= 0 ) 
				{
					JOptionPane.showMessageDialog(this.getParent(), "Value entered should be a number greater than 0\n Enter Another Value","Error",JOptionPane.ERROR_MESSAGE);
				}
			} 
			catch (Exception e) 
			{
				JOptionPane.showMessageDialog(this.getParent(), "Value entered should be a number greater than 0\n Enter Another Value","Error",JOptionPane.ERROR_MESSAGE);
				threshold = -1;
			}
		}
		return threshold;
	}

	public void resetReducedDiagram() 
	{
		tabbedPane.setSelectedIndex(0);
		setStatusLabel("STATUS: Regenerate Interrupted");
		setFullDiagramPacket(null);
		setReducedDiagramPacket(null, null);
		enableAllTabs();
		stopButton.setVisible(false);
		pauseButton.setVisible(false);
		resumeButton.setVisible(false);
		prevThreshold = 0.0;
		reducedPlanPanel.dimensionBox1.setEnabled(true);
		if(PicassoConstants.NUM_DIMS>1)
		reducedPlanPanel.dimensionBox2.setEnabled(true);
		if(PicassoConstants.NUM_DIMS>2)
			reducedPlanPanel.setButton.setEnabled(true);
	}

	public void enableAllTabs() 
	{
		tabbedPane.setEnabledAt(0, true);
		tabbedPane.setEnabledAt(1, true);
		
		if(PicassoConstants.OP_LVL)
			tabbedPane.setEnabledAt(2, false);
		else
			tabbedPane.setEnabledAt(2, true);
		tabbedPane.setEnabledAt(3, true);
		tabbedPane.setEnabledAt(4, true);
		tabbedPane.setEnabledAt(5, true);
		tabbedPane.setEnabledAt(6, true);
/*		if(!PicassoConstants.IS_APPROXIMATE_DIAGRAM) {
			tabbedPane.setEnabledAt(5, true);
			tabbedPane.setEnabledAt(6, true);
		}else {
			tabbedPane.setEnabledAt(5, false);
			tabbedPane.setEnabledAt(6, false);
		}
*/		tabbedPane.setEnabledAt(7, true);
		if ( frame != null )
			frame.enableMenus(true);
		//legendPanel.enableRegen(true);
		legendPanel.setProgressBar(0);
		currentPanel.enableRegen(true);
	}

	public void emptyAllTabs()
	{
	
		planPanel.emptyPanel();
       // planPanel.dimensionBox1.removeAllItems();
        //planPanel.dimensionBox2.removeAllItems(); 
		planPanel.dimensionBox1.setEnabled(false);
		planPanel.dimensionBox2.setEnabled(false);
		planPanel.setButton.setEnabled(false);
		planCostPanel.emptyPanel();
		planCostPanel.dimensionBox1.setEnabled(false);
		planCostPanel.dimensionBox2.setEnabled(false);
		planCostPanel.setButton.setEnabled(false);
		planCardPanel.emptyPanel();
		planCardPanel.dimensionBox1.setEnabled(false);
		planCardPanel.dimensionBox2.setEnabled(false);
		planCardPanel.setButton.setEnabled(false);
		reducedPlanPanel.emptyPanel();
		reducedPlanPanel.dimensionBox1.setEnabled(false);
		reducedPlanPanel.dimensionBox2.setEnabled(false);
		reducedPlanPanel.setButton.setEnabled(false);
		execPlanCostPanel.emptyPanel();
		execPlanCostPanel.dimensionBox1.setEnabled(false);
		execPlanCostPanel.dimensionBox2.setEnabled(false);
		execPlanCostPanel.setButton.setEnabled(false);
		execPlanCardPanel.emptyPanel();
		execPlanCardPanel.dimensionBox1.setEnabled(false);
		execPlanCardPanel.dimensionBox2.setEnabled(false);
		execPlanCardPanel.setButton.setEnabled(false);
		selecPanel.emptyPanel();
		execCardDisplay = false;
		execCostDisplay = false;
		planDisplay = false;
		costDisplay = false;
		cardDisplay = false;
		selecDisplay = false;
		reducedDisplay = false;
	}

	public void setProgressBar(int value) {
		legendPanel.setProgressBar(value);
		legendPanel.repaint();
	}

	public void setMsgSent(boolean value) {
		msgSent = value;
	}

	public void enableCancelButton(boolean value) {
		stopButton.setVisible(true);
		pauseButton.setVisible(true);
		resumeButton.setVisible(false);
		reducedPlanPanel.dimensionBox1.setEnabled(false);
		reducedPlanPanel.dimensionBox2.setEnabled(false);
		reducedPlanPanel.setButton.setEnabled(false);
		//if(stopCompileButton != null)
			//stopCompileButton.setVisible(true);
	}

	public void stateChanged(ChangeEvent e) 
	{
		if (PicassoConstants.LOW_VIDEO == true)
			emptyAllTabs();
		Component component = tabbedPane.getSelectedComponent();
		currentPanel = (PicassoPanel)component;

		dbSettingsPanel.SetFindqtToBlank();
	//	for(int i=0;i<5;i++)

		if ( currentPanel == queryBuilderPanel ) 
		{
			if(!PicassoConstants.IS_PKT_LOADED)
				dbSettingsPanel.enableFields(true);
			else
				dbSettingsPanel.enableDBConnDesc(true);
//			/dbSettingsPanel.enableFields(true);
			if ( legendPanel != null )
				legendPanel.emptyPanel();
			if(currentPanel.predframe!=null)
				currentPanel.predframe.resetmarkers();
			if(planPanel.predframe!=null)
				planPanel.predframe.resetmarkers();	
			if(reducedPlanPanel.predframe!=null)
				reducedPlanPanel.predframe.resetmarkers();	
			if(planCostPanel.predframe!=null)
				planCostPanel.predframe.resetmarkers();	
			if(planCardPanel.predframe!=null)
				planCardPanel.predframe.resetmarkers();	
			if(execPlanCardPanel.predframe!=null)
				execPlanCardPanel.predframe.resetmarkers();
			if(execPlanCostPanel.predframe!=null)
				execPlanCostPanel.predframe.resetmarkers();
			planPanel.setPanelLabel(" ");
			return;
		}
		else 
		{
			dbSettingsPanel.enableFields(false);
			legendPanel.setEmptyLegend(false);
			if(currentPanel!=queryBuilderPanel&&currentPanel!=selecPanel)
			currentPanel.setButton.setEnabled(false);
		//	if(currentPanel.predframe!=null)
		//		currentPanel.predframe.resetmarkers();
		}
		if ( haveSettingsChanged == true ) 
		{
			planPanel.setPanelLabel(" ");
			oldthreshold=-1;
			haveSettingsChanged = false;
			setClientMsgFields();
			if(this.getDBSettingsPanel().iscustom()&&!this.getDBSettingsPanel().isreadonly())
			{	
				if ( PicassoConstants.NUM_DIMS <= 0 ) 
				{
					JOptionPane.showMessageDialog(this, "No Picasso Selectivity Predicates selected. Please Enter a valid query template.","Error",JOptionPane.ERROR_MESSAGE);
					tabbedPane.setSelectedIndex(0);
					return;
				}
				else
				{	
					resolutionframe = new ResolutionFrame(message,dbSettingsPanel.getDistribution(),queryBuilderPanel, this);
					this.getResFrame().setModal(true);
					this.getResFrame().pack();
					this.emptyAllTabs();
					this.emptyLegendPanel();
					this.getResFrame().setVisible(true);
				}
			}
			else
			{
				this.getResFrame().setModal(true);
				this.getResFrame().setVisible(false);				
			}
			msgRcvd = false;
			execMsgRcvd = false;
			emptyAllTabs();
			legendPanel.emptyPanel();

			if ( !firstTime ) {
				processMessage(component, MessageIds.READ_PICASSO_DIAGRAM);
			}
			return;
		} 
		else if ( msgRcvd == true && msgSent == false) 
		{
			if ( component == planPanel && planDisplay == false ) 
			{
				fulldp.getSortedPlanArray(); // Done so that the global colour mapping is set correctly after viewing a reduced diagram. Necessary because reduced diagrams global colouring is diff.
				planPanel.emptyPanel(); //--ma
				planDisplay = true;
				planPanel.addToQueryParams();
				planPanel.fillBottomBar();
				// planPanel.actionPerformed(null); // Commented out so that planpanel, plancostpanel, plancardpanel are in sync always in terms of their content
				planPanel.planDiff.setVisible(false);
				planPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_DIAGRAM);
				planPanel.planDiff.setVisible(true);
			}
			else if ( component == planCostPanel && costDisplay == false ) 
			{
				fulldp.getSortedPlanArray(); // Done so that the global colour mapping is set correctly after viewing a reduced diagram. Necessary because reduced diagrams global colouring is diff.
				planCostPanel.emptyPanel();
				costDisplay = true;
				planCostPanel.addToQueryParams();
				planCostPanel.fillBottomBar();
				// planCostPanel.actionPerformed(null); // Commented out so that planpanel, plancostpanel, plancardpanel are in sync always in terms of their content
				planCostPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_COST_DIAGRAM);
			} 
			else if ( component == planCardPanel && cardDisplay == false ) 
			{
				fulldp.getSortedPlanArray(); // Done so that the global colour mapping is set correctly after viewing a reduced diagram. Necessary because reduced diagrams global colouring is diff.
				planCardPanel.emptyPanel();
				cardDisplay = true;
				planCardPanel.addToQueryParams();
				planCardPanel.fillBottomBar();
				// planCardPanel.actionPerformed(null); // Commented out so that planpanel, plancostpanel, plancardpanel are in sync always in terms of their content
				planCardPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_CARD_DIAGRAM);
			} 
			else if ( component == selecPanel ) 
			{
				selecDisplay = true;
				serverPkt.diagramPacket = fulldp;
				int tab = selecPanel.setSelectivityLog(serverPkt);
				if (tab == -1) tabbedPane.setSelectedIndex(0);
			}
			else if ( component == reducedPlanPanel ) 
			{
				// reducedPlanPanel.emptyPanel();
				reducedDisplay = true;
				reducedPlanPanel.addToQueryParams();
				reducedPlanPanel.fillBottomBar();
				// reducedPlanPanel.actionPerformed(null);
				serverPkt.diagramPacket = fulldp;
				reduceDiagram(0);
				//haveSettingsChanged = true;
			}
			else if (edp == null && (component == execPlanCardPanel ||component == execPlanCostPanel)) 
			{
				execPlanCostPanel.emptyPanel();
				execPlanCardPanel.emptyPanel();
				message.getQueryPacket().setExecType(PicassoConstants.RUNTIME_DIAGRAM);
				processMessage(component, MessageIds.READ_PICASSO_DIAGRAM);
			}
			else if ( component == execPlanCardPanel && execCardDisplay == false ) 
			{
				execPlanCardPanel.emptyPanel();
				execCardDisplay = true;
				execPlanCardPanel.addToQueryParams();
				execPlanCardPanel.fillBottomBar();
				execPlanCardPanel.drawDiagram(serverPkt, PicassoPanel.EXEC_PLAN_CARD_DIAGRAM);
			}
			else if ( component == execPlanCostPanel && execCostDisplay == false ) 
			{
				execPlanCostPanel.emptyPanel();
				execCostDisplay = true;
				execPlanCostPanel.addToQueryParams();
				execPlanCostPanel.fillBottomBar();
				execPlanCostPanel.drawDiagram(serverPkt, PicassoPanel.EXEC_PLAN_COST_DIAGRAM);
			}
		} 
		else if ( execMsgRcvd == true && msgSent == false) 
		{
			if ( component == execPlanCardPanel && execCardDisplay == false ) 
			{
				fulledp.getSortedPlanArray();
				execPlanCardPanel.emptyPanel();
				planCostPanel.emptyPanel();
				planCardPanel.emptyPanel();
				execCardDisplay = true;
				execPlanCardPanel.addToQueryParams();
				execPlanCardPanel.fillBottomBar();
				execPlanCardPanel.drawDiagram(serverPkt, PicassoPanel.EXEC_PLAN_CARD_DIAGRAM);
			}
			else if ( component == execPlanCostPanel && execCostDisplay == false ) 
			{
				fulledp.getSortedPlanArray();
				execPlanCostPanel.emptyPanel();
				planCostPanel.emptyPanel();
				planCardPanel.emptyPanel();
				execCostDisplay = true;
				execPlanCostPanel.addToQueryParams();
				execPlanCostPanel.fillBottomBar();
				execPlanCostPanel.drawDiagram(serverPkt, PicassoPanel.EXEC_PLAN_COST_DIAGRAM);
			}
			else if ( dp == null &&(component == planPanel || component == selecPanel ||component == reducedPlanPanel ||component == planCostPanel || component == planCardPanel)) 
			{
				planCostPanel.emptyPanel();
				planCardPanel.emptyPanel();
				planPanel.emptyPanel();
                reducedPlanPanel.emptyPanel();
                execPlanCostPanel.emptyPanel();
                execPlanCardPanel.emptyPanel();
                if(approxDiagram)                	
                	message.getQueryPacket().setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);
                else
                	message.getQueryPacket().setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
				processMessage(component, MessageIds.READ_PICASSO_DIAGRAM);
			} else if ( component == planPanel && planDisplay == false ) {
				planDisplay = true;
				planPanel.addToQueryParams();
				planPanel.fillBottomBar();
				planPanel.actionPerformed(null);
				planPanel.planDiff.setVisible(false);
				planPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_DIAGRAM);
				planPanel.dimensionBox1.setEnabled(true);
				if(PicassoConstants.NUM_DIMS>1)
				planPanel.dimensionBox2.setEnabled(true);
				if(PicassoConstants.NUM_DIMS > 2)
					planPanel.setButton.setEnabled(true);
				planPanel.planDiff.setVisible(true);
			} else if ( component == selecPanel ) {
				selecDisplay = true;
				int tab = selecPanel.setSelectivityLog(serverPkt);
				if (tab == -1) tabbedPane.setSelectedIndex(0);
			} else if ( component == reducedPlanPanel ) {
				reducedPlanPanel.emptyPanel(); 
				reducedPlanPanel.addToQueryParams();
				reducedPlanPanel.fillBottomBar();
				//reducedPlanPanel.actionListener.actionPerformed(null);
				serverPkt.diagramPacket = fulldp;
				reduceDiagram(0);
			//	reducedPlanPanel.dimensionBox1.setEnabled(true);
				//reducedPlanPanel.dimensionBox2.setEnabled(true);
			} else if ( component == planCostPanel && costDisplay == false ) {
				
				costDisplay = true;
				planDisplay = false;
				cardDisplay = false;
				reducedDisplay = false;
				execCardDisplay = false;
				execCostDisplay = false;
				selecDisplay = false;
				
				planCostPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_COST_DIAGRAM);
				if(PicassoConstants.NUM_DIMS > 2)
					planCostPanel.setButton.setEnabled(true);
				planCostPanel.dimensionBox1.setEnabled(true);
				if(PicassoConstants.NUM_DIMS>1)
				planCostPanel.dimensionBox2.setEnabled(true);
			} else if ( component == planCardPanel && cardDisplay == false ) {
				cardDisplay = true;
				costDisplay = false;
				planDisplay = false;
				reducedDisplay = false;
				execCardDisplay = false;
				execCostDisplay = false;
				selecDisplay = false;
				planCardPanel.drawDiagram(serverPkt, PicassoPanel.PLAN_CARD_DIAGRAM);
				if(PicassoConstants.NUM_DIMS > 2)
					planCardPanel.setButton.setEnabled(true);
				planCardPanel.dimensionBox1.setEnabled(true);
				planCardPanel.dimensionBox2.setEnabled(true);
			}
		} else if (component != queryBuilderPanel && msgSent == true){
			currentPanel.enableRegen(false);
			JOptionPane.showMessageDialog(this.getParent(), "Server is busy, Please wait","Server busy",JOptionPane.INFORMATION_MESSAGE);
		}
		else if (msgSent == false && msgRcvd == false)
		{
			if ( PicassoConstants.NUM_DIMS <= 0 ) 
			{
				JOptionPane.showMessageDialog(this, "No Picasso Selectivity Predicates selected. Please Enter a valid query template.","Error",JOptionPane.ERROR_MESSAGE);
				tabbedPane.setSelectedIndex(0);
				return;
			}
		}
		if(fulldp != null)
			fulldp.getSortedPlanArray(); // Done so that the global colour mapping is set correctly after viewing a reduced diagram. Necessary because reduced diagrams global colouring is diff.
		if(fulledp != null)
			fulledp.getSortedPlanArray();
		legendPanel.resetScrollBar();
		repaint();
	}

	public void reduceDiagram(int index) {
		
		if(PicassoConstants.IS_PKT_LOADED && PicassoConstants.REDUCTION_ALGORITHM != PicassoConstants.REDUCE_CG)
		{
			JOptionPane.showMessageDialog(this.getParent(), "The requested reduction algorithm cannot be applied on loaded packets.","Can't reduce",JOptionPane.ERROR_MESSAGE);	
			tabbedPane.setSelectedIndex(0);
			return;
		}
		
		if(!PicassoConstants.IS_PKT_LOADED)
		{
			if((!(message.getDBSettings().getDbVendor().equalsIgnoreCase(DBConstants.MSSQL) || 
				  message.getDBSettings().getDbVendor().equalsIgnoreCase(DBConstants.SYBASE))) && 
				 (
				   PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CGFPC ||
				   PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_SEER ||
				   PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CCSEER ||
				   PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_LITESEER))
			{
				JOptionPane.showMessageDialog(this.getParent(), "The requested reduction algorithm cannot be applied to the given database.","Can't reduce",JOptionPane.ERROR_MESSAGE);	
				tabbedPane.setSelectedIndex(index);
				return;
			}
		}
			
		
		double thresh = getThreshold();
		if (oldthreshold==thresh && oldredalgo == PicassoConstants.REDUCTION_ALGORITHM && oldOpLvl==PicassoConstants.OP_LVL
				&& PicassoConstants.LOW_VIDEO!=true)
		{
			return;
		}
		oldthreshold=thresh;
		oldredalgo=PicassoConstants.REDUCTION_ALGORITHM;
	oldOpLvl=PicassoConstants.OP_LVL;
		if ( thresh == -1 ) {
			tabbedPane.setSelectedIndex(index);
			return;
		}
		curThreshold = thresh;

		reducedDisplay = false;
		if ( curThreshold != prevThreshold || ((msgRcvd == true || execMsgRcvd == true ) && reducedDisplay == false)) {
			reducedDisplay = true;
			rdp = null; rSortedPlanCount = null;
			reducedPlanPanel.drawDiagram(serverPkt, PicassoPanel.REDUCED_PLAN_DIAGRAM, curThreshold);
			tabbedPane.setEnabledAt(0, false);
			tabbedPane.setEnabledAt(1, false);
			tabbedPane.setEnabledAt(3, false);
			tabbedPane.setEnabledAt(4, false);
			tabbedPane.setEnabledAt(5, false);
			tabbedPane.setEnabledAt(6, false);
			tabbedPane.setEnabledAt(7, false);
			if ( frame != null )
				frame.enableMenus(false);
			stopButton.setVisible(true);
			pauseButton.setVisible(false);
			resumeButton.setVisible(false);
			reducedPlanPanel.dimensionBox1.setEnabled(false);
			reducedPlanPanel.dimensionBox2.setEnabled(false);
			reducedPlanPanel.setButton.setEnabled(false);
			prevThreshold = curThreshold;
		}
	}


//@pd///////////////////////////////////////////////////////

	class Plan
	{
		double cost;
		double min;
		double max;
		int ct;
	}

	double maxThreshold;

	public int estimateKnee() 
	{
		DiagramPacket gdp = serverPkt.diagramPacket;
		if (gdp != null) 
		{
			int n = gdp.getMaxPlanNumber();
			req_no_of_plans = PicassoConstants.DESIRED_NUM_PLANS;
			req_thresh = -1;
			if (req_no_of_plans >= n) 
			{
				req_thresh = 0;
			}
			int[] ct = new int[n];
			double[] avg = new double[n];
			double[] max = new double[n];
			double[] min = new double[n];
			DataValues[] val=null;
			      if(gdp!=null)
			    	   val = gdp.getData();
			for (int i = 0; i < n; i++) 
			{
				max[i] = Double.MIN_VALUE;
				min[i] = Double.MAX_VALUE;
			}
			
			int dimen = gdp.getDimension();
			int d = 0;
			/*int ncol = gdp.getResolution(gdp.getResolution(PicassoConstants.a[0]));
			int nrow = dimen>1?gdp.getResolution(PicassoConstants.a[1]):1;*/
			boolean[][] cols = new boolean[gdp.getMaxResolution()][n];
	
			int x = 1;
			for (int y = 0; y < val.length; y++)
			{
				int i = y;
				int p = val[i].getPlanNumber();
				double c = val[i].getCost();
				ct[p]++;
				avg[p] += c;
				if (max[p] < c) {
					max[p] = c;
				}
				if (min[p] > c) {
					min[p] = c;
				}
			}
			Plan[] p = new Plan[n];
			for (int i = 0; i < n; i++) {
				p[i] = new Plan();
				avg[i] /= ct[i];
				p[i].cost = avg[i];
				p[i].ct = ct[i];
				p[i].max = max[i];
				p[i].min = min[i];
			}
			double maxdiff = val[val.length - 1].getCost() - val[0].getCost();
			maxdiff /= val[0].getCost();
			maxThreshold = maxdiff * 100;

			int knee = -1;
			int old = -1;
			int dist = -1;
			int k = 0;
			int dd = gdp.getMaxPlanNumber();
			int pp;
			do {
				do {
					knee++;
					pp = estimate(p, knee);
				} while (old == pp && knee < dd);
				d = pp * pp + knee * knee;
				old = pp;
				if (pp <= req_no_of_plans && req_thresh == -1) {
					req_thresh = knee;
				}
					if(dist == -1)
					{
					dist = d;
					k = knee;
					dd = (int) Math.sqrt(dist);
				} else {
					if(d < dist) {
						dist = d;
						k = knee;
						dd = (int) Math.sqrt(dist);
					}
				}
			} while (knee < dd);
			int bound = 0; 
			while(req_thresh == -1 && bound < 1000) {
				pp = estimate(p, knee);
				knee ++;
				if(pp <= req_no_of_plans) {
					req_thresh = knee;
				}
				bound++;
			}
			return k;
		}
		return -1;
}

	class Set
	{
		// HashSet<Integer> elements = new HashSet<Integer>();
		HashSet elements = new HashSet();
	}

	private int estimate(Plan[] p, double d) // AmmEst
	{
		int no = 0;
		Set univ = new Set();
		Set[] pl = new Set[p.length];
		d /= 100;
		for (int i = 0; i < p.length; i++) {
			univ.elements.add(new Integer(i));
			if (pl[i] == null) {
				pl[i] = new Set();
			}
			pl[i].elements.add(new Integer(i));
			double c = p[i].cost * (1 + d);
			double cmin = p[i].min * (1 + d);
			double cmax = p[i].max * (1 + d);
			for (int j = 0; j < p.length; j++)
			{
				if (pl[j] == null)
				{
					pl[j] = new Set();
				}
				if (p[j].cost <= c && p[j].cost > p[i].cost && p[j].min <= cmin && p[j].max <= cmax)
				{
					pl[j].elements.add(new Integer(i));
				}
			}
		}
		while (univ.elements.size() > 0) {
			int max = -1;
			int j = 0;
			for (int i = 0; i < pl.length; i++) {
				if (pl[i] != null) {
					if (pl[i].elements.size() > max) {
						max = pl[i].elements.size();
						j = i;
					}
				}
			}
			univ.elements.removeAll(pl[j].elements);
			for (int i = 0; i < pl.length; i++) {
				if (i != j && pl[i] != null) {
					pl[i].elements.removeAll(pl[j].elements);
					if (pl[i].elements.size() == 0) {
						pl[i] = null;
					}
				}
			}
			pl[j] = null;
			no++;
		}
		return no;
	}



/////////////////////////////////////////


	private void processMessage(Component component, int messageid)
	{
		if ( currentPanel == queryBuilderPanel )
			return;
		msgSent = true;
		tabbedPane.setEnabledAt(0, false);
		emptyAllTabs();
		emptyLegendPanel();
		prevThreshold = -1;
		if(!PicassoConstants.IS_PKT_LOADED)
			serverPkt = null;
		//serverPkt = null;
		int sindex = tabbedPane.getSelectedIndex();
		if ( currentPanel == execPlanCostPanel || currentPanel == execPlanCardPanel){
			/*tabbedPane.setEnabledAt(0, false);
			tabbedPane.setEnabledAt(1, false);
			tabbedPane.setEnabledAt(2, false);
			tabbedPane.setEnabledAt(3, false);
			tabbedPane.setEnabledAt(4, false);
			tabbedPane.setEnabledAt(7, false);*/
			PicassoConstants.DIAGRAM_REQUEST_TYPE = 'E';//SB - diagram type
			message.getQueryPacket().setExecType(PicassoConstants.RUNTIME_DIAGRAM);
			edp = null;
		}
		if ( currentPanel != execPlanCostPanel && currentPanel != execPlanCardPanel ){
			/*tabbedPane.setEnabledAt(0, false);
			tabbedPane.setEnabledAt(5, false);
			tabbedPane.setEnabledAt(6, false);*/
			if(approxDiagram) {
				PicassoConstants.DIAGRAM_REQUEST_TYPE = 'A';//SB - diagram type
				message.getQueryPacket().setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);
			}
			else {
				PicassoConstants.DIAGRAM_REQUEST_TYPE = 'C';//SB - diagram type
				message.getQueryPacket().setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
			}
			dp = null; sortedPlanCount = null;
		}
		for (int i=0; i < tabbedPane.getComponentCount(); i++) {
			if ( i != sindex )
				tabbedPane.setEnabledAt(i, false);
		}
		//legendPanel.enableRegen(false);
		if ( frame != null )
			frame.enableMenus(false);
		currentPanel.enableRegen(false);
		((PicassoPanel)component).process(messageid);
	}
	public void processMessage(int messageId) {
		Component component = tabbedPane.getSelectedComponent();
		currentPanel = (PicassoPanel)component;
		System.out.println("Current Panel: " + currentPanel.panelString + "," + currentPanel.panelType);
		if ( currentPanel == queryBuilderPanel ) {
			dbSettingsPanel.enableFields(true);
			return;
		} else {
			dbSettingsPanel.enableFields(false);
		}
		if(!qpset)
		setClientMsgFields();
		msgRcvd = false;
		execMsgRcvd = false;
		processMessage(currentPanel, MessageIds.TIME_TO_GENERATE);
	}
	public void processMessageApprox(int messageId) {
		Component component = tabbedPane.getSelectedComponent();
		currentPanel = (PicassoPanel)component;
		System.out.println("Current Panel: " + currentPanel.panelString + "," + currentPanel.panelType);
		if ( currentPanel == queryBuilderPanel ) {
			dbSettingsPanel.enableFields(true);
			return;
		} else {
			dbSettingsPanel.enableFields(false);
		}
		if(!qpset)
		setClientMsgFields();
		msgRcvd = false;
		execMsgRcvd = false;
		processMessage(currentPanel, MessageIds.TIME_TO_GENERATE_APPROX);
	}	
	private void setClientMsgFields() {
		String val;
		DBSettings dbSettings = dbSettingsPanel.getCurrentDBSettings();
		QueryPacket qp = message.getQueryPacket();
		if ( dbSettings == null ) {
			message.setDbType("");
			qp.setOptLevel("");
			for(int i=0;i<PicassoConstants.NUM_DIMS;i++)			//rss
			qp.setResolution(0,i);
		} else 
		{
			message.setDbType(dbSettingsPanel.getDbVendor());
			message.setDBSettings(dbSettings);
			val = dbSettingsPanel.getOptLevel();
			if ( val != null )
				qp.setOptLevel(val);
			else
				qp.setOptLevel("DEFAULT");
		}
		qp.setDimension(PicassoConstants.NUM_DIMS);
if(!getDBSettingsPanel().isreadonly()){
		val = dbSettingsPanel.getResolution();					//
			if ( val != null && val !="Custom Per Dimension" )//
				for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
				qp.setResolution(Integer.parseInt(val),i);			//
		else
				for(int i=0;i<PicassoConstants.NUM_DIMS;i++)		//
				qp.setResolution(10,i);								//
}
else 		for(int i=0;i<PicassoConstants.NUM_DIMS;i++)		//
	qp.setResolution(getDBSettingsPanel().locres[i],i);	
if(!getDBSettingsPanel().isreadonly()){
for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
		{
			qp.setStartPoint(0,i);								//
			qp.setEndPoint(1.0,i);		
		}														//
}
else
	for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
	{
		qp.setStartPoint(getDBSettingsPanel().locrange[0][i],i);								//
		qp.setEndPoint(getDBSettingsPanel().locrange[1][i],i);		
	}														//

if ( currentPanel == execPlanCostPanel || currentPanel == execPlanCardPanel){
			qp.setExecType(PicassoConstants.RUNTIME_DIAGRAM);
		} else if ( currentPanel != execPlanCostPanel && currentPanel != execPlanCardPanel ){
			if(approxDiagram)                	
				qp.setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);
			else
				qp.setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
		}
                String dist_const = dbSettingsPanel.getDistribution();
                if(dist_const.equals(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
                {
                	for(int i=0;i<PicassoConstants.NUM_DIMS;i++)					//rss
                    {
                		switch(qp.getResolution(i))
                		{
                		case 10:
                			dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_10;
                			break;
                		case 30:
                			dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_30;
                			break;
                		case 100:
                			dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_100;
                			break;
                		case 300:
                			dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_300;
                			break;
                		case 1000:
                			dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_1000;
                			break;
                		}
                    }	
                }
		qp.setDistribution(dist_const);
		qp.setPlanDiffLevel(dbSettingsPanel.getPlanDiffLevel());
		qp.setQueryTemplate(queryBuilderPanel.getQueryText());
		qp.setQueryName(queryBuilderPanel.getQueryName());
		//apa
//		Hashtable tmp = queryBuilderPanel.getPredicateValues();
//		if(tmp!=null) {
//			// Set the selectivity values properly..
//			Object[] keys = tmp.keySet().toArray();
//			Hashtable tmp1 = new Hashtable();
//			for (int i=0; i < keys.length; i++) {
//				String predValue = (String)tmp.get(keys[i]);
//				//int pv = Integer.parseInt(predValue);
//                                double pv = Double.parseDouble(predValue);
//				if(dist_const.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
//				{
//					if ( pv < 100/(qp.getResolution()*2))
//						pv = 100/(qp.getResolution()*2);
//				}
//                                tmp1.put(keys[i], ""+pv);
//			}
//			message.setAttributeSelectivities(tmp1);
//		}
//		else
//		{
//			message.setAttributeSelectivities(null);
//		}

		//	Vector dimensions = queryBuilderPanel.getDimensions();
	//	message.setDimensions(dimensions);
		// TODO: What to do about this?
		message.setSelecErrorThreshold(""+PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD);
		message.setQueryPacket(qp);
	}
	
	// The following set of functions are used for global colouring of plans.

	public void setParamSP(ServerPacket sp) 
	{
		odp = sp;
	}
	public ServerPacket getParamSP() 
	{
		return odp;
	}
	// ------------------------------------------------------------- SET/GET FUNCTIONS FOR (NORMAL) DIAGRAM PACKET -----------------------------------------------------
	public void setFullDiagramPacket(DiagramPacket gdp) 
	{
		fulldp = gdp;
		fullSortedPlanCount = gdp.getSortedPlanArray();
	}
	
	public DiagramPacket getFullDiagramPacket() 
	{
		return(fulldp);
	}

	public DiagramPacket getDiagramPacket() {
		return(dp);
	}
	
	public void setDiagramPacket(DiagramPacket gdp) {
		dp = gdp;
		sortedPlanCount = gdp.getSortedPlanArray();
	}
	
	public int[][] getFullSortedPlan() 
	{
		return fullSortedPlanCount;
	}

	public int[][] getSortedPlan() {
		return sortedPlanCount;
	}

	public PlanPanel getPlanPanel() {
		return planPanel;
		
	}
	// ------------------------------------------------------------- SET/GET FUNCTIONS FOR REDUCED DIAGRAM PACKET -----------------------------------------------------
	
	public void setFullReducedDiagramPacket(DiagramPacket gdp) 
	{
		fullrdp = gdp;
		fullRSortedPlanCount = gdp.getSortedPlanArray();
	}
	
	public DiagramPacket getFullReducedDiagramPacket() 
	{
		return(fullrdp);
	}
	
	public int[][] getFullRSortedPlan() 
	{
		return fullRSortedPlanCount;
	}
	
	public void setReducedDiagramPacket(DiagramPacket gdp, int[][] sortedPlans) 
	{
		rdp = gdp;
		rSortedPlanCount = sortedPlans;
	}

	public DiagramPacket getReducedDiagramPacket() 
	{
		return(rdp);
	}
	
	public int[][] getRSortedPlan() {
		return rSortedPlanCount;
	}

	// ------------------------------------------------------------- SET/GET FUNCTIONS FOR EXEC DIAGRAM PACKET -----------------------------------------------------


	public void setExecDiagramPacket(DiagramPacket gdp) {
		edp = gdp;
		execSortedPlanCount = gdp.getSortedPlanArray();
	}
	public int[][] getExecSortedPlan() {
		return execSortedPlanCount;
	}
	public DiagramPacket getExecDiagramPacket() {
		return(edp);
	}
	public void setFullExecDiagramPacket(DiagramPacket gdp) 
	{
		fulledp = gdp;
		fullExecSortedPlanCount = gdp.getSortedPlanArray();
	}
	
	public DiagramPacket getFullExecDiagramPacket() 
	{
		return(fulledp);
	}
	
	public int[][] getFullExecSortedPlan() 
	{
		return fullExecSortedPlanCount;
	}
	
	public ServerPacket getServerPacket() {
		return serverPkt;
	}
	
	public Color getReducedPlanColor(int i) {
		// Get the plan # for index i
		int planNo = rSortedPlanCount[2][i];

		// For this plan # get the color in the original plan
		Color c = new Color(PicassoConstants.color[sortedPlanCount[0][planNo]%PicassoConstants.color.length]);
		return(c);

	}

	public int getOriginalPlanNumber(int i) {
//		Get the plan # for index i
		int planNo = rSortedPlanCount[2][i];

		return(sortedPlanCount[0][planNo]);
	}

	public void processErrorMessage(ServerPacket packet) {
		//msgRcvd = true;
		msgRcvd = false;
		msgSent = false;
		execMsgRcvd = false;
		planDisplay = true;
		costDisplay = true;
		cardDisplay = true;
		reducedDisplay = true;
		execCostDisplay = true;
		execCardDisplay = true;
		selecDisplay = true;
		enableAllTabs();
		tabbedPane.setSelectedIndex(0);
		setSettingsChanged(true);
		//haveSettingsChanged = true;
		stopButton.setVisible(false);
		pauseButton.setVisible(false);
		resumeButton.setVisible(false);
		reducedPlanPanel.dimensionBox1.setEnabled(true);
		if(PicassoConstants.NUM_DIMS>1)
		reducedPlanPanel.dimensionBox2.setEnabled(true);
		if(PicassoConstants.NUM_DIMS > 2)
			reducedPlanPanel.setButton.setEnabled(true);
	}

	private void processExecution(ServerPacket packet) {
		serverPkt = packet;
		setFullExecDiagramPacket(packet.diagramPacket);
		if(edp == null)
			setExecDiagramPacket(packet.diagramPacket); // Patch work. may need to be removed.
		if ( currentPanel == execPlanCostPanel ) {
			execPlanCostPanel.drawDiagram(packet, PicassoPanel.EXEC_PLAN_COST_DIAGRAM);
			execCardDisplay = false;
			execCostDisplay = true;
		} else if ( currentPanel == execPlanCardPanel ) {
			execPlanCardPanel.drawDiagram(packet, PicassoPanel.EXEC_PLAN_CARD_DIAGRAM);
			execCardDisplay = true;
			execCostDisplay = false;
		}
	}

	public void emptyLegendPanel()
	{
		if(legendPanel != null)
			legendPanel.emptyPanel();
	}

	public void drawAllDiagrams(ServerPacket packet, PicassoPanel displayPanel) {
		// Save the packet for display later...
		legendPanel.setEmptyLegend(false);
		msgSent = false;
		dbSettingsPanel.addQid(packet.queryPacket);
		enableAllTabs();
		stopButton.setVisible(false);
		pauseButton.setVisible(false);
		resumeButton.setVisible(false);
		
		if ( packet.queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
		{
			execMsgRcvd = true;
			msgRcvd = false;
			packet.diagramPacket = fulledp;
			processExecution(packet);
		} 
		else 
		{
			msgRcvd = true;
			execMsgRcvd = false;
			serverPkt = packet;
			setDiagramPacket(packet.diagramPacket);
		}
		if ( packet.diagramPacket.getMaxPlanNumber() == 0 ) 
		{
			setStatusLabel("ERROR: Number of Plans is 0");
			return;
		} 
		else
			setStatusLabel("STATUS: Displaying Diagrams");

		if ( currentPanel == planPanel ) 
		{
			planDisplay = true;
			cardDisplay = false;
			costDisplay = false;
			reducedDisplay = false;
			execCardDisplay = false;
			execCostDisplay = false;
			selecDisplay = false;
			planPanel.emptyPanel();
			planPanel.planDiff.setVisible(false);
			if(fillBottomBar)
			{
				planPanel.addToQueryParams();
				if(!retainSlice)
					planPanel.fillBottomBar();
				else
					retainSlice = false;
				planPanel.actionPerformed(null);
			}
	//		planPanel.planDiff.setVisible(false);
			planPanel.drawDiagram(packet, PicassoPanel.PLAN_DIAGRAM);
			planPanel.planDiff.setVisible(true);
		currentPanel.dimensionBox1.setEnabled(true);
		if(PicassoConstants.NUM_DIMS>1)
		currentPanel.dimensionBox2.setEnabled(true);
		if(PicassoConstants.NUM_DIMS > 2)
			currentPanel.setButton.setEnabled(true);
		} 
		else if ( currentPanel == planCostPanel ) 
		{
			costDisplay = true;
			planDisplay = false;
			cardDisplay = false;
			reducedDisplay = false;
			execCardDisplay = false;
			execCostDisplay = false;
			selecDisplay = false;
			planCostPanel.emptyPanel();
			if(fillBottomBar)
			{
				planCostPanel.addToQueryParams();
				if(!retainSlice)
					planCostPanel.fillBottomBar();
				else
					retainSlice = false;				
				planCostPanel.actionPerformed(null);
			}
			planCostPanel.drawDiagram(packet, PicassoPanel.PLAN_COST_DIAGRAM);
			currentPanel.dimensionBox1.setEnabled(true);
			if(PicassoConstants.NUM_DIMS>1)
			currentPanel.dimensionBox2.setEnabled(true);
			if(PicassoConstants.NUM_DIMS > 2)
				currentPanel.setButton.setEnabled(true);
		} 
		else if ( currentPanel == planCardPanel ) 
		{
			cardDisplay = true;
			planDisplay = false;
			costDisplay = false;
			reducedDisplay = false;
			execCardDisplay = false;
			execCostDisplay = false;
			selecDisplay = false;
			planCardPanel.emptyPanel();
			if(fillBottomBar)
			{
				planCardPanel.addToQueryParams();
				if(!retainSlice)
					planCardPanel.fillBottomBar();
				else
					retainSlice = false;
				planCardPanel.actionPerformed(null);
			}
			planCardPanel.drawDiagram(packet, PicassoPanel.PLAN_CARD_DIAGRAM);
			currentPanel.dimensionBox1.setEnabled(true);
			currentPanel.dimensionBox2.setEnabled(true);
			if(PicassoConstants.NUM_DIMS > 2)
				currentPanel.setButton.setEnabled(true);
		} 
		else if ( currentPanel == reducedPlanPanel ) 
		{
			cardDisplay = false;
			planDisplay = false;
			costDisplay = false;
			reducedDisplay = true;
			execCardDisplay = false;
			execCostDisplay = false;
			selecDisplay = false;
			reducedPlanPanel.emptyPanel();
			if(fillBottomBar)
			{
				reducedPlanPanel.addToQueryParams();
				reducedPlanPanel.fillBottomBar();
				// reducedPlanPanel.actionPerformed(null);
			}
			packet.diagramPacket = fulldp;
			// planPanel.drawDiagram(packet, PicassoPanel.REDUCED_PLAN_DIAGRAM);
			reduceDiagram(0);
			
			if(currentPanel != queryBuilderPanel)
			{
				currentPanel.dimensionBox1.setEnabled(true);
				if(PicassoConstants.NUM_DIMS>1)
				currentPanel.dimensionBox2.setEnabled(true);
				if(PicassoConstants.NUM_DIMS > 2)
					currentPanel.setButton.setEnabled(true);
			}
		} 
		else if ( currentPanel == selecPanel ) 
		{
			cardDisplay = false;
			planDisplay = false;
			costDisplay = false;
			reducedDisplay = false;
			execCardDisplay = false;
			execCostDisplay = false;
			selecDisplay = true;
			packet.diagramPacket = fulldp;
			int tab = selecPanel.setSelectivityLog(packet);
			if ( tab == -1 ) tabbedPane.setSelectedIndex(0);
		}
		else if ( currentPanel == execPlanCardPanel ) 
		{
			cardDisplay = false;
			planDisplay = false;
			costDisplay = false;
			reducedDisplay = false;
			execCardDisplay = true;
			execCostDisplay = false;
			selecDisplay = false;
			// planCostPanel.emptyPanel();
			// planCardPanel.emptyPanel();
			execPlanCardPanel.emptyPanel();
			if(fillBottomBar)
			{
				execPlanCardPanel.addToQueryParams();
				execPlanCardPanel.fillBottomBar();
				execPlanCardPanel.actionPerformed(null);
			}
			execPlanCardPanel.drawDiagram(packet, PicassoPanel.EXEC_PLAN_CARD_DIAGRAM);
			currentPanel.dimensionBox1.setEnabled(true);
			if(PicassoConstants.NUM_DIMS>1)
			currentPanel.dimensionBox2.setEnabled(true);
			if(PicassoConstants.NUM_DIMS > 2)
				currentPanel.setButton.setEnabled(true);
		} 
		else if ( currentPanel == execPlanCostPanel ) 
		{
			cardDisplay = false;
			planDisplay = false;
			costDisplay = false;
			reducedDisplay = false;
			execCardDisplay = false;
			execCostDisplay = true;
			selecDisplay = false;
			// planCostPanel.emptyPanel();
			// planCardPanel.emptyPanel();
			execPlanCostPanel.emptyPanel();
			if(fillBottomBar)
			{
				execPlanCostPanel.addToQueryParams();
				execPlanCostPanel.fillBottomBar();
				execPlanCostPanel.actionPerformed(null);
			}
			execPlanCostPanel.drawDiagram(packet, PicassoPanel.EXEC_PLAN_COST_DIAGRAM);
			currentPanel.dimensionBox1.setEnabled(true);
			if(PicassoConstants.NUM_DIMS>1)
			currentPanel.dimensionBox2.setEnabled(true);
			if(PicassoConstants.NUM_DIMS > 2)
				currentPanel.setButton.setEnabled(true);
		}
        setStatusLabel("STATUS: DONE");
	}

	public DataBaseSettingsPanel getDBSettingsPanel() {
		return dbSettingsPanel;
	}
	public QueryBuilderPanel getQueryBuilderPanel(){
		return queryBuilderPanel;
		
	}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == stopButton ) {
			if ( currentPanel != reducedPlanPanel )
				// Send Message to server to stop processing..
				stopProcessing();
			else {
				reducedPlanPanel.stopReduction();
			}
		}
		else if (e.getSource() == pauseButton)
		{
			pauseProcessing();
			pauseButton.setVisible(false);
			resumeButton.setVisible(true);
		}
		else if (e.getSource() == resumeButton)
		{
			resumeProcessing();
			resumeButton.setVisible(false);
			pauseButton.setVisible(true);
			
		}
		//else if(e.getSource() == stopCompileButton) //ADG
			//stopCompiling();
	}

	private void stopProcessing() {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.STOP_PROCESSING);
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);

	}
	private void pauseProcessing()
	{
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.PAUSE_PROCESSING);
		
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}
	private void resumeProcessing()
	{
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.RESUME_PROCESSING);
		
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}
	
	private void stopCompiling()//Approximation related
	{
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.STOP_COMPILING);
		String serverName = getServerName();
		int serverPort = getServerPort();
		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}
	public String getServerName() {
		return serverName;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void help() {
		//new HelpFrame().show();
		//HelpFrame.showIEHelp();
		String marker="usage.htm";
		//System.out.println(System.getProperty("user.dir"));
		if(currentPanel==planPanel)
			marker="controls.htm/";//#plan_diag";
		else if(currentPanel==reducedPlanPanel)
			marker="controls.htm/";//#reduced_diag";
		else if (currentPanel==planCardPanel)
			marker="controls.htm/";//#card";
		else if (currentPanel==planCostPanel)
			marker="controls.htm/";//#cost";
		else if (currentPanel==execPlanCardPanel)
			marker="controls.htm/";//#ecard";
		else if(currentPanel==execPlanCostPanel)
			marker="controls.htm/";//#ecost";
			PicassoUtil.openURL("file://" + System.getProperty("user.dir") + "/PicassoDoc/Usage/"+marker);
	}

	public void docHome() {
		PicassoUtil.openURL("file://" + System.getProperty("user.dir") + "/PicassoDoc/index.htm");
	}
	
	public void keymap(){
		PicassoUtil.openURL("file://" + System.getProperty("user.dir") + "/../../PicassoDoc/Usage/controls.htm#keymap");
	}

	public void changeDBSettings(int opType, String prevInstance, DBSettings dbSettings) {
		if ( opType == PicassoConstants.NEW_DB_INSTANCE ) {
			picassoSettings.add(dbSettings);
			dbSettingsPanel.populateConnectionSettingsBox();
			//dbSettingsPanel.setConnectionSettingsBox(dbSettings.getInstanceName());
		} else if (opType == PicassoConstants.EDIT_DB_INSTANCE ) {
			picassoSettings.edit(prevInstance, dbSettings);
			dbSettingsPanel.populateConnectionSettingsBox();
			//dbSettingsPanel.setConnectionSettingsBox(dbSettings.getInstanceName());
		} else if (opType == PicassoConstants.DELETE_DB_INSTANCE ) {
			picassoSettings.delete(dbSettings.getInstanceName());
			dbSettingsPanel.setConnectionSettingsBox(null);
			dbSettingsPanel.populateConnectionSettingsBox();
			dbSettingsPanel.setConnectionSettingsBox(null);
		}
	}

	public PicassoSettings getDBSettings() {
		return picassoSettings;
	}

	public void showStopButton(boolean value) {
		stopButton.setVisible(value);
	}

	public void doMenuAction(int type) {
		switch (type) {
		case PicassoConstants.NEW_DB_INSTANCE :
		case PicassoConstants.EDIT_DB_INSTANCE :
		case PicassoConstants.DELETE_DB_INSTANCE :
			new DatabaseInfoFrame(this, dbSettingsPanel, type).setVisible(true);
			break;

		case PicassoConstants.GET_DIAGRAM_LIST :
			dbSettingsPanel.getQTNames();
			break;

		case PicassoConstants.DELETE_DIAGRAM :
			dbSettingsPanel.deleteDiagram();
			break;

		case PicassoConstants.RENAME_DIAGRAM:
			dbSettingsPanel.renameDiagram();
			break;
			
		case PicassoConstants.CONNECT_PICASSO:
			reconnectToPServer();
			break;

		case PicassoConstants.CHECK_SERVER :
			ClientPacket values = getClientPacket();
			values.setMessageId(MessageIds.GET_SERVER_STATUS);
			String serverName = getServerName();
			int serverPort = getServerPort();

			MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
			break;

		case PicassoConstants.ABOUT_SERVER :
			JOptionPane.showMessageDialog(this.getParent(), "Server Name : " + getServerName() + "\nServer Port : " + getServerPort(),"About Picasso Server",JOptionPane.INFORMATION_MESSAGE);
			break;

		case PicassoConstants.CLEAN_PROCESSES :
			cleanupPicassoTables();
			break;

		case PicassoConstants.DELETE_PICDB :
			int val = JOptionPane.showConfirmDialog(this, "All Picasso Diagrams will be permanently destroyed if you click 'Yes' (You won't be able to view these diagrams unless you regenerate them). Are you sure you want to proceed?", "Destroy", JOptionPane.YES_NO_OPTION);
			if ( val == 0 )
				deletePicassoTables();
			break;

		case PicassoConstants.SHUTDOWN_SERVER :
			shutdownServer();
			break;
			
		case PicassoConstants.SHOW_PICASSO_SETTINGS:
			new PicassoSettingsFrame(this).setVisible(true);
			break;

		}
		//MessageUtil.CPrintToConsole("In Here " + type);

	}

	void cleanupPicassoTables() {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.CLEANUP_PICASSO_TABLES);
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}

	void deletePicassoTables() {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.DELETE_PICASSO_TABLES);
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}

	void shutdownServer() {
		ClientPacket values = getClientPacket();
		values.setMessageId(MessageIds.SHUTDOWN_SERVER);
		String serverName = getServerName();
		int serverPort = getServerPort();

		MessageUtil.sendMessageToServer(serverName, serverPort, values, planPanel);
	}


	public void reconnectToPServer() {
		dbSettingsPanel.setConnectionSettingsBox(null);
		if ( message != null && message.getDBSettings() != null ) {
			// Set the message db settings to null
			message.setDBSettings(null);
		}
		new ServerInfoDialog(this).setVisible(true);;
	}

	public void setServerName (String sname){
		serverName = sname;

	}
	public void setPort (int port){
		serverPort = port;
	}
	
	public void setFillBottomBarFlag (boolean val)
	{
		fillBottomBar = val;
	}
	public void getApproxParameters(int optClass,String  status) {		// ADG
		//Checking whether the diagram is not bigger than what integer can index - SB
		long total = 1;
		int dim = message.getQueryPacket().getDimension();
		boolean isPossible = true;
		for(int i = 0;i < dim;i++) 
			total *= (long)message.getQueryPacket().getResolution(i);
		System.out.println("Diagram size: "+total+" query points");
		if(total > (long)Integer.MAX_VALUE)
			isPossible = false;
		//Checking done - now display error if diagram is too big
		if(!isPossible) {
			JOptionPane.showMessageDialog(null, "Diagram size exceeds integer capacity. Please reduce dimensions or resolutions.","Information",JOptionPane.INFORMATION_MESSAGE);
			planDisplay = false;
		}
		//Else start generating diagram
		else {
			new ApproxPlanGenInfo(this,optClass,status).setVisible(true);	
			//stopCompileButton.setVisible(false);
		}
		if(planDisplay)
		{
			tabbedPane.setEnabledAt(0, false);
			tabbedPane.setEnabledAt(1, true);
			tabbedPane.setEnabledAt(2, false);
			tabbedPane.setEnabledAt(3, false);
			tabbedPane.setEnabledAt(4, false);
			tabbedPane.setEnabledAt(5, false);
			tabbedPane.setEnabledAt(6, false);
			tabbedPane.setEnabledAt(7, false);
			getClientPacket().approxDiagram = approxDiagram;
			stopButton.setVisible(true);
			getClientPacket().getQueryPacket().setEstimatedTime(estimatedTime);
		}
		else
		{
			tabbedPane.setEnabledAt(0, true);
			tabbedPane.setEnabledAt(1, false);
			tabbedPane.setEnabledAt(2, false);
			tabbedPane.setEnabledAt(3, false);
			tabbedPane.setEnabledAt(4, false);
			tabbedPane.setEnabledAt(5, false);
			tabbedPane.setEnabledAt(6, false);
			tabbedPane.setEnabledAt(7, false);
			tabbedPane.setSelectedIndex(0);
			enableAllTabs();			
		}
	}
	
	public boolean getPlanDisplayFlag()
	{
		return planDisplay;
	}
	
	public void setPlanDisplayFlag(boolean val)
	{
		planDisplay = val;
	}
	public void setParamsChanged(boolean value) {
		paramsChanged = value;
	}
	public void createNewServerPacket() {
		serverPkt = new ServerPacket();
	}
	public void loadPacket(DiagramPacket dp) {
		QueryPacket qp = dp.getQueryPacket();
		
//		if(qp.getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
//			getDBSettingsPanel().setdistribution(1);
//		else
//			getDBSettingsPanel().setdistribution(0);
		
		getDBSettingsPanel().setLoadedQtDescItem(qp);
	}
}

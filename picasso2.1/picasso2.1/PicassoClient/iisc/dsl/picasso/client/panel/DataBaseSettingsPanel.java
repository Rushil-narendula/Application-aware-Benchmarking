
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

import iisc.dsl.picasso.client.frame.ResolutionFrame;
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.PicassoSettings;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.QueryPacket;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

import java.util.regex.Pattern;
import java.util.regex.Matcher; 

public class DataBaseSettingsPanel extends PicassoPanel implements ActionListener, KeyListener, DocumentListener, ItemListener {
	
	// Auto generated ID
	private static final long serialVersionUID = -1233128796965240316L;
	
	private DBSettings	curDB;
	private PicassoSettings	picassoSettings;
	private static boolean customflag;
	private JComboBox  rangeBox;
public boolean readonly=false;
	private JTextField  findqt;
	private JComboBox 	dbConnDesc, qtDesc, resBox, optlvlBox,distribution,selecType;
	public JComboBox	qtDescAll; //for search of qtDesc list., 
								   //this will contain all qtDesc's, and the other contains the filtered list
	
	//try to remove
	private JComboBox   planDiffLevel;
	public int goBackTo;


	private JLabel		dbInformation;
	public static boolean 		setResFlag,setRangeFlag;
	public static boolean		oldResFlag,oldRangeFlag;
	public static int 			resIndex,rangeIndex;
	public static boolean 		regen=false;
	public static int[] locres;
	public static double[][] locrange;
	public boolean		getQTNamesFlag;
	private boolean 	notFromFindBoxFlag;
	
	
	public DataBaseSettingsPanel(MainPanel app) 
	{
		super(app);
		createDBGUI();
		populateConnectionSettingsBox();
		setConnectionSettingsBox(null);
		
		if ( curDB != null )
			optlvlBox.setSelectedItem(curDB.getOptLevel());
		
		findqt.getDocument().addDocumentListener(this);

		dbConnDesc.addActionListener(this);
		qtDesc.addKeyListener(this);
		qtDesc.addItemListener(this);
		resBox.addItemListener(this);
		rangeBox.addItemListener(this);
		optlvlBox.addItemListener(this);
		distribution.addItemListener(this);
		selecType.addItemListener(this);
	}
	public void setresbox(int value)
	{
		resBox.setSelectedIndex(value);
	}
	public void setrange(int value)
	{
		rangeBox.setSelectedIndex(value);
	}
	public void setdistribution(int value)
	{
		distribution.setSelectedIndex(value);
	}
	public void enableFields(boolean value) 
	{
		dbConnDesc.setEnabled(value);
		qtDesc.setEnabled(value);
		resBox.setEnabled(value);
		optlvlBox.setEnabled(value);
		distribution.setEnabled(value);
		selecType.setEnabled(value);
		rangeBox.setEnabled(value);
		planDiffLevel.setEnabled(value);
	}
	
	private void createDBGUI() 
	{
		setBackground(Color.orange);
		setPanelString("Database Settings");
		setBorder(BorderFactory.createEtchedBorder());
			
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		
		//-----------------------zeroth row------------------------------------------
		
		JLabel settingsLbl = new JLabel("Settings", JLabel.LEFT);
		settingsLbl.setFont(new Font("Arial", Font.BOLD, 16));
		
		Font f = new Font("Arial", Font.PLAIN, 12);
		
		//---------------------------first row---------------------------------------
		
		JLabel dbConnDesLbl = new JLabel("DBConnection Descriptor: ", JLabel.RIGHT);
		dbConnDesLbl.setFont(f);
		dbConnDesc = new JComboBox();
		dbConnDesc.setMinimumSize(new Dimension(100,20));
		dbConnDesc.setToolTipText("Select database connection idenfitier");
		
		JLabel qtdLbl = new JLabel("QueryTemplate Descriptor: ", JLabel.RIGHT);
		qtdLbl.setFont(f);
		qtDesc = new JComboBox();
		qtDesc.setMinimumSize(new Dimension(100,20));
		qtDesc.setSelectedIndex(-1);
		qtDesc.setToolTipText("Display a previously generated diagram");
		qtDescAll = new JComboBox();

		JLabel resLbl = new JLabel("Plot Resolution: ", JLabel.RIGHT);
		resLbl.setFont(f);
		resBox = new JComboBox();
		resBox.setMinimumSize(new Dimension(100,20));
		resBox.addItem("10");
		resBox.addItem("30");
		resBox.addItem("100");
		resBox.addItem("300");
		resBox.addItem("1000");
		resBox.addItem("Custom Per Dimension");
		resBox.setSelectedItem("10");
		resBox.setToolTipText("Number of query points per dimension");

		//---------------------------second row----------------------------------------------
		
		JLabel optlvlLbl = new JLabel("Optimization Level: ", JLabel.RIGHT);
		optlvlLbl.setFont(f);
		optlvlBox = new JComboBox();
		optlvlBox.setMinimumSize(new Dimension(100,20));
		optlvlBox.addItem("Default");
		optlvlBox.setToolTipText("Set Optimization Level For Diagram Generation");
		
		
		
		JLabel distriLbl = new JLabel("Query Distribution: ", JLabel.RIGHT);
		distriLbl.setFont(f);
		distribution = new JComboBox();
		distribution.setMinimumSize(new Dimension(100,20));
		distribution.addItem(PicassoConstants.UNIFORM_DISTRIBUTION);
		distribution.addItem(PicassoConstants.EXPONENTIAL_DISTRIBUTION);
		distribution.setToolTipText("Select Query Point Distribution");
		
		JLabel selecLbl = new JLabel("Plot Selectivity: ", JLabel.RIGHT);
		JLabel rangeLbl = new JLabel("Plot Range:",JLabel.RIGHT);
		selecLbl.setFont(f);
		rangeLbl.setFont(f);
		selecType = new JComboBox();
		selecType.setMinimumSize(new Dimension(100,20));
		selecType.addItem("Picasso");
		selecType.addItem("Engine");
		selecType.setToolTipText("Select Plot Selectivity");
		
		rangeBox = new JComboBox();
		rangeBox.setMinimumSize(new Dimension(100,20));
		rangeBox.addItem(" Default (0-100)");
		rangeBox.addItem("Custom Per Dimension");
		rangeBox.setToolTipText("Select Range Constant / Custom");
		
		//need to keep these 3 for now
		planDiffLevel = new JComboBox();
		planDiffLevel.addItem(PicassoConstants.SUBOPERATORLEVEL);
		planDiffLevel.addItem(PicassoConstants.OPERATORLEVEL);
		
		
		
		//-----------------------ADDITION TO PANEL BEGINS---------------------------------------
		
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		
		c.gridheight = 1;
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.HORIZONTAL;

		//------------------------zero'th row-----------------------
		
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		add(settingsLbl, c);
		
		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth=1;
		findqt = new JTextField();
		findqt.setVisible(false);
		add(findqt,c);
		notFromFindBoxFlag = true;
		c.gridx = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(rangeLbl, c);
		c.gridx = 5;
		add(rangeBox,c);

		//---------------------first row------------------------------------
		c.gridy = 1;
		
		c.gridx = 0;
		add(dbConnDesLbl, c);
		c.gridx = 1;
		add(dbConnDesc, c);
		
		c.gridx = 2;
		add(qtdLbl, c);
		c.gridx = 3;
		add(qtDesc, c);
		
		c.gridx = 4;
		add(resLbl, c);
		c.gridx = 5;
		add(resBox, c);
		
		//------------------------------second row----------------------------
		
		c.gridy = 2;

		c.gridx = 0;
		add(optlvlLbl, c);
		c.gridx = 1;
		add(optlvlBox, c);
		
		c.gridx = 2;
		add(distriLbl,c);
		c.gridx = 3;
		add(distribution,c);

		c.gridx = 4;
		add(selecLbl, c);
		c.gridx = 5;
		add(selecType,c);
		
}
	
	
//=======================GET FUNCTIONS==============================
	//first all the DB settings,
	//then resolution,
	//then optlevel, querypt distribution, selectivity type
	
	public boolean iscustom()
	{
		customflag = false;
		if(getRangeStr()=="Custom Per Dimension"||getResolution()=="Custom Per Dimension")
			customflag=true;
		return customflag;
	}

	public boolean isreadonly()
	{
		return readonly;
	}
	public void setcustom (boolean val)
	{
		customflag=val;
	}
	
	public String getDbVendor() {
		if ( curDB == null )
			return null;
		return(curDB.getDbVendor());
	}
	
	public DBSettings getCurrentDBSettings() {
		return curDB;
	}
	
	//each of the fields in the DBConnSettings
	public String getDBInstance() {
		return (String)dbConnDesc.getSelectedItem();
	}
	public String getDbServer() {
		return curDB.getServerName();
	}	
	public String getDbPort() {
		return curDB.getServerPort();
	}
	public String getDbName() {
		return curDB.getDbName();
	}
	public String getDbSchema() {
		return curDB.getSchema();
	}
	public String getDbUserName() {
		return curDB.getUserName();
	}
	public String getDbPassword() {
		return curDB.getPassword();
	}
	
	//all the fields of the DB Connection string formatted properly
	public String getDbInfoStr() {
		return PicassoUtil.getDBInfoString(curDB);
	}
	
	public String getResolution() {
		return((String)resBox.getSelectedItem());
	}
	public int getResIndex() {
		return((Integer)resBox.getSelectedIndex());
	}
	//redundant, remove one
	public String getOptStr() {
		return((String)optlvlBox.getSelectedItem());
	}
	public String getOptLevel() {
		return(curDB.getOptLevel());
	}
			public String getRangeStr() {
		return((String)rangeBox.getSelectedItem());
	}
	public String getDistribution() {
		return (String)(distribution.getSelectedItem());
	}
	
	public QueryPacket getQtDesc()
	{
		return ((QueryPacket)(qtDesc.getSelectedItem()));
	}
	public void repaintQtDesc()
	{
		qtDesc.repaint();
	}
	public void setQtDescItem(QueryPacket qp)
	{
		int current = qtDesc.getSelectedIndex();
		
		qtDesc.removeKeyListener(this);
		qtDesc.removeItemListener(this);
		
		qtDesc.removeItemAt(current);
		qtDesc.insertItemAt(qp, current);
		qtDesc.setSelectedIndex(current);
		
		qtDesc.addKeyListener(this);
		qtDesc.addItemListener(this);
	}
	public JComboBox getQtDescAll()
	{
		return qtDescAll;
	}
	//called from PrintDiagramPanel
	public String getSelecStr() {
		return((String)selecType.getSelectedItem());
	}
	//make it a string where it's read and remove this function
	public int getSelecType() {
		return(selecType.getSelectedIndex());
	}
	public int getRangeType() {
		return(rangeBox.getSelectedIndex());
	}
	
	//remove one of them - redundant
	public String getPlanDiffLevel() {
		return (String)(planDiffLevel.getSelectedItem());
	}
	public String getPlanDiffStr() {
		return((String)planDiffLevel.getSelectedItem());
	}
	public String getCurrentQTDesc() {
		//int index = qtDesc.getSelectedIndex();
		return(qtDesc.getSelectedItem().toString());
	}

	//=================end of get functions========================
	
	public void populateConnectionSettingsBox() {
		getQTNamesFlag = false;
		picassoSettings = parent.getDBSettings();
		dbConnDesc.removeAllItems();
		Object[] insts = picassoSettings.getAllInstances();
		for (int i=0; i < insts.length; i++)
			dbConnDesc.addItem((String)insts[i]);
		getQTNamesFlag = true;
	}
	
	//only called with null, make this clearConnectionSettingsBox
	public void setConnectionSettingsBox(String cur) {
			qtDesc.removeAllItems();
			qtDescAll.removeAllItems();
			curDB = null;
			prevDbInst = null;
			dbConnDesc.setSelectedIndex(-1);
			getPParent().getFrame().enableDBMenus(false);
	}
	
	public void removeQid(QueryPacket qp)
	{
		qtDesc.removeItem(qp);
		qtDescAll.removeItem(qp);
	}
	
	public void removeQid(String qname)
	{
		for(int i = 0; i < qtDesc.getItemCount(); i++)
		{
			QueryPacket qp = (QueryPacket)qtDesc.getItemAt(i);
			if(qp.getQueryName().equals(qname))
			{
				qtDesc.removeItem(qp);
				qtDescAll.removeItem(qp);
				qtDesc.setSelectedIndex(goBackTo);
				return;
			}
		}
		
	}
	
	public void addQid(QueryPacket qp) {
		if ( qp == null)
			return;
		
		int count = qtDesc.getItemCount();
		QueryPacket []dupList = new QueryPacket [count];
		goBackTo = qtDesc.getSelectedIndex();
		
		// creating a duplicate list so that when the diagram is generated it is showed at the top of the list
		for(int i = 0; i < count; i++)
		{
			dupList[i] = (QueryPacket)qtDesc.getItemAt(i);
		}
		
		String qName = qp.getQueryName();
		QueryPacket lqp = null;
		
		int inx = 0;
		boolean addFlag = true;
		for (int i=0; i < count; i++) {
			lqp = (QueryPacket)qtDesc.getItemAt(i);
			if (lqp.getQueryName().equals(qName)) {
				// dupList[i] = qp; // return;
				inx = i; 
				addFlag = false;
			}	
		}
		qtDesc.removeKeyListener(this);
		qtDesc.removeItemListener(this);
		
		qtDesc.removeAllItems();
		qtDescAll.removeAllItems();
		
		if(addFlag)
		{
			qp.dummyEntry = true;
			qtDesc.addItem(qp);
			qtDescAll.addItem(qp);
		}
		
		prevQName = curQName = qp;
		
		for(int i = 0; i < count; i++)
		{
			qtDesc.addItem(dupList[i]);
			qtDescAll.addItem(dupList[i]);
		}
		
		qtDesc.addKeyListener(this);
		qtDesc.addItemListener(this);
		
		if(!isCustomRes(qp))
		{
			resBox.setSelectedItem(Integer.toString(curQName.getResolution(0)));
			setResFlag=false;
		}
		else
		{
			locres =new int [PicassoConstants.NUM_DIMS];	
			locres=qp.getResolution();
			setResFlag=true;
			resBox.setSelectedItem("Custom Per Dimension");
		}
		if(!isCustomRange(qp))
		{
			rangeBox.setSelectedIndex(0);
			setRangeFlag=false;
		}
		else
		{
			locrange=new double[2][PicassoConstants.NUM_DIMS];
			locrange[0]=qp.getStartPoint();
			locrange[1]=qp.getEndPoint();
			setRangeFlag=true;
			rangeBox.setSelectedItem("Custom Per Dimension");
		}
		
		if(curQName.getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
			distribution.setSelectedItem(PicassoConstants.EXPONENTIAL_DISTRIBUTION);
		
		optlvlBox.setSelectedItem(curQName.getOptLevel());
		planDiffLevel.setSelectedItem(curQName.getPlanDiffLevel());
		
		qtDesc.removeKeyListener(this);
		qtDesc.removeItemListener(this);
		
		if(addFlag)
			qtDesc.setSelectedItem(qp);
		else
			qtDesc.setSelectedIndex(inx);
		
		qtDesc.addKeyListener(this);
		qtDesc.addItemListener(this);
	}
	
	private boolean isCustomRes (QueryPacket qp)
	{
		boolean flag =false;
		for(int i =0;i<qp.getDimension()-1;i++)
			if(qp.getResolution(i)!=qp.getResolution(i+1))
			{
				flag=true;
				break;
			}
		return flag;
	}
	
	private boolean isCustomRange (QueryPacket qp)
	{
		boolean flag =false;
		for(int i =0;i<qp.getDimension();i++)
			if(qp.getEndPoint(i) != 1.0 || qp.getStartPoint(i)!=0.0)
			{
				flag=true;
				break;
			}
		return flag;
	}	
	public void emptyQidNames() 
	{
		qtDesc.removeAllItems();
		qtDescAll.removeAllItems();
	}
	
	public void displayQidNames(Vector qids) 
	{
		QueryPacket qp ;
		
		qtDesc.removeAllItems();
		qtDescAll.removeAllItems();
		
		for (int i=0; i < qids.size(); i++) 
		{
			qp = (QueryPacket)qids.elementAt(i);
			qp.dummyEntry = false; //
			qtDesc.addItem(qp);
			qtDescAll.addItem(qp);
		}
		
		if ( qids.size() > 0 ) 
		{
			qtDesc.setSelectedIndex(-1);
		}
	}
	
	
	void setOptlvlBox(DBSettings db) 
	{
		String dbType = curDB.getDbVendor();
		if(dbType == null)
			return;
		optlvlBox.removeAllItems();
		for(int i=0; i<DBConstants.databases.length; i++) 
		{
			DBInfo dbinfo = DBConstants.databases[i];
			if ( dbType.equals(dbinfo.name))
			{
				for(int j=0; j<dbinfo.optLevels.length; j++)
				{
					optlvlBox.addItem(dbinfo.optLevels[j]);
				}
			}
		}
		optlvlBox.setSelectedItem(curDB.getOptLevel());
	}

	String prevDbInst=null, curDbInst=null;
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if ( source == dbConnDesc && getQTNamesFlag) 
		{
			PicassoConstants.IS_PKT_LOADED = false;
			curDbInst = (String)dbConnDesc.getSelectedItem();
			if ( curDbInst != null) 
			{
				prevDbInst = curDbInst;
				curDB = parent.getDBSettings().get(curDbInst);
				parent.getDBSettings().setCurrentInstance(curDbInst);
				setOptlvlBox(curDB);
				getQTNames();
				getPParent().getFrame().enableDBMenus(true);
			
			}
			SetFindqtToBlank();
		}
		enableFields(true);
	}
	QueryPacket oldqp;
	public static QueryPacket prevQName=null, curQName=null;
	
	public void itemStateChanged(ItemEvent e) 
	{
		Object source = e.getSource();
			if (source == rangeBox)
			{
				readonly=false;
				if(rangeBox.getSelectedIndex()==0)
				{
					setRangeFlag=false;	
					for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
					{
						if(parent.getResFrame().range[0][i]!=null)
						parent.getResFrame().range[0][i].setText("0");
						if(parent.getResFrame().range[0][i]!=null)
						parent.getResFrame().range[1][i].setText("100");
						locrange[0][i]=0;
						locrange[1][i]=1;
					}
				}else if (rangeBox.getSelectedIndex()==1)
					setRangeFlag=true;
			}
			else if(source == resBox)
			{	
				readonly=false;
				if(resBox.getSelectedIndex()==5)
					setResFlag=true;
				else
					setResFlag=false;
			}
			else if (source == qtDesc && notFromFindBoxFlag) 
			{
				curQName = (QueryPacket)qtDesc.getSelectedItem();
				//System.out.println("Exec Type: "+curQName.getExecType());
				//oldop=curQName;
				if ( curQName != null && curQName != prevQName ) 
				{
					resBox.removeItemListener(this);
					rangeBox.removeItemListener(this);
					PicassoConstants.first=true;
					prevQName = curQName;
					if(!isCustomRes(curQName))//may2008
					{
						resBox.setSelectedItem(Integer.toString(curQName.getResolution(0)));
						setResFlag=false;
					}
					else
					{
						setResFlag=true;
						resBox.setSelectedItem("Custom Per Dimension");
					}				
					
					locres =new int [PicassoConstants.NUM_DIMS];	
					locres=curQName.getResolution();
					
					if(!isCustomRange(curQName))
					{
						rangeBox.setSelectedIndex(0);
						setRangeFlag=false;
					}
					else
					{
						setRangeFlag=true;
						rangeBox.setSelectedItem("Custom Per Dimension");
					}
					locrange=new double[2][PicassoConstants.NUM_DIMS];
					locrange[0]=curQName.getStartPoint();
					locrange[1]=curQName.getEndPoint();
	
					optlvlBox.setSelectedItem(curQName.getOptLevel());
					if(curQName.getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
						distribution.setSelectedItem(PicassoConstants.EXPONENTIAL_DISTRIBUTION);
					else
						distribution.setSelectedItem(PicassoConstants.UNIFORM_DISTRIBUTION);
					parent.changeQueryFields(curQName);
	
					planDiffLevel.setSelectedItem(curQName.getPlanDiffLevel());
					locrange[0]=curQName.getStartPoint();
					locrange[1]=curQName.getEndPoint();
					readonly=true;
					String qName = getCurrentQTDesc();
					if(qName.indexOf("(A)") != -1 && qName.lastIndexOf("(A)") == qName.length()-3) {
						PicassoConstants.IS_APPROXIMATE_DIAGRAM = true;
						parent.approxDiagram = true;
					}else {
						PicassoConstants.IS_APPROXIMATE_DIAGRAM = false;
						parent.approxDiagram = false;
					}
					
					resBox.addItemListener(this);
					rangeBox.addItemListener(this);
				}
				else if( curQName == null)
				{
					prevQName = null;
					resBox.setSelectedIndex(0);
	
					optlvlBox.setSelectedIndex(0);
					distribution.setSelectedIndex(0);
	
					planDiffLevel.setSelectedIndex(0);
					parent.emptyQueryFields(); //qtid and querytemplate SQL
					readonly=false;
				}
			} 
			else if ( source == optlvlBox ) 
			{
				curDB.setOptLevel((String)optlvlBox.getSelectedItem());
				readonly=false;
			} 

		//This is done for change in qtdesc, resolution, optlvl, querypt distri, and selectivity type
		setSettingsChanged(true);
		setParamsChanged(true);
	}

	public void deleteDiagram() {
		int val = JOptionPane.showConfirmDialog(this.getPParent(), "Delete QueryTemplate: " + qtDesc.getSelectedItem() + "?", "Delete", JOptionPane.YES_NO_OPTION);
		
		if ( val == 0 ) {
			super.deleteDiagram();
			
			Object mytemp = qtDesc.getSelectedItem(); //so that we can delete after this from qtDescAll too
			qtDesc.removeItem(mytemp);
			qtDescAll.removeItem(mytemp);
		}
	}
	
	public void renameDiagram()
	{
		Object mytemp = qtDesc.getSelectedItem(); //so that we can delete after this from qtDescAll too
		int index1 = qtDesc.getSelectedIndex();
		int index2 = qtDescAll.getSelectedIndex();
		
		
		String s = (String)JOptionPane.showInputDialog(
		                    this.getPParent(),
		                    "Rename as:\n",
		                    "Rename Query Template",
		                    JOptionPane.PLAIN_MESSAGE,
		                    null,
		                    null,
		                    ((QueryPacket)mytemp).getQueryName());

		if(s == null)
			return;
		if(s.equals(((QueryPacket)mytemp).getQueryName()))
			return;

		// Check if the given name already exists.
		int count = qtDescAll.getItemCount();
		for(int i = 0; i < count; i++)
			if(s.equals(((QueryPacket)qtDesc.getItemAt(i)).getQueryName()))
			{
				JOptionPane.showMessageDialog(this, "A diagram already exists with this name. Please enter another name.","Sorry",JOptionPane.ERROR_MESSAGE);
				return;
			}
				
		//TODO
		super.renameDiagram(s);
		qtDesc.removeItem(mytemp);
		qtDescAll.removeItem(mytemp);
		
		((QueryPacket)mytemp).setQueryName(s);
		qtDesc.insertItemAt(mytemp, index1);
		qtDescAll.insertItemAt(mytemp, index2);
		
		qtDesc.setSelectedIndex(index1);
		qtDescAll.setSelectedIndex(index2);
	}
	
	//rest of the code in this file is for find functionality in QTID list	
	public void keyTyped(KeyEvent e) {
		Object source = e.getSource();

		if (source == qtDesc) 
		{
			// Show it (fresh).
			if ( ! findqt.isVisible() && e.getKeyChar()!='\b')
			{
				findqt.setText("" + e.getKeyChar());
				findqt.requestFocus();
				fillWithMatches();
			}
		} 
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void insertUpdate(DocumentEvent e) {
		update(e);
	}
	public void removeUpdate(DocumentEvent e) {
		update(e);
	}
	public void changedUpdate(DocumentEvent e) {
		// we won't ever get this with a PlainDocument
	}
	private void update(DocumentEvent e) 
	{
		if (findqt.getText().equals("")) 
		{
			findqt.setVisible(false);

			qtDesc.removeAllItems();
			for(int i=0;i<qtDescAll.getItemCount();i++)
			{
				qtDesc.addItem(qtDescAll.getItemAt(i));
			}
			qtDesc.setSelectedIndex(-1);
			qtDesc.requestFocus();
		}
		else
		{
			fillWithMatches();
		}
	}
	
	void fillWithMatches()
	{
		findqt.setVisible(true);
		Pattern p;
		try
		{
			p = Pattern.compile(".*" + findqt.getText() + ".*",java.util.regex.Pattern.CASE_INSENSITIVE);
		}
		catch(java.util.regex.PatternSyntaxException pse)
		{
			p = Pattern.compile(".*",java.util.regex.Pattern.CASE_INSENSITIVE);
		}

		Matcher m;

		qtDesc.removeAllItems();
		for(int i=0;i<qtDescAll.getItemCount();i++)
		{
			m = p.matcher(qtDescAll.getItemAt(i).toString()); 
			if(m.matches())
				qtDesc.addItem(qtDescAll.getItemAt(i));
		}
		qtDesc.setSelectedIndex(-1);
	}
	
	public void SetFindqtToBlank()
	{
		findqt.setVisible(false);
		
		//Save the selectedItem
		notFromFindBoxFlag = false;
		Object temp = qtDesc.getSelectedItem();
		
		qtDesc.removeItemListener(this);
		qtDesc.removeAllItems();
		
		for(int i=0;i<qtDescAll.getItemCount();i++)
		{
			qtDesc.addItem(qtDescAll.getItemAt(i));
		}
		
		//Put it back as selected
		qtDesc.setSelectedItem(temp);
		qtDesc.addItemListener(this);
		notFromFindBoxFlag = true;
	}
	public void setLoadedQtDescItem(QueryPacket qp) {
		if(qtDesc.getItemAt(0) == null)
		{
			qtDesc.insertItemAt(qp, 0);
			qtDesc.setSelectedIndex(0);
		}
		else
		{
			qtDesc.removeItemAt(0);
			qtDesc.insertItemAt(qp, 0);
			qtDesc.setSelectedIndex(0);
		}
		enableFields(false);
		enableDBConnDesc(true);
		dbConnDesc.removeActionListener(this);
		dbConnDesc.setSelectedIndex(-1);
		dbConnDesc.addActionListener(this);
	}
	public void enableDBConnDesc(boolean b) {
		dbConnDesc.setEnabled(b);
	}
}

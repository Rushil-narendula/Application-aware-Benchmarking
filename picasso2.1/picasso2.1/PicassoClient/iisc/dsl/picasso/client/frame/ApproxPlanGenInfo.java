package iisc.dsl.picasso.client.frame;
import iisc.dsl.picasso.client.panel.MainPanel;

import iisc.dsl.picasso.client.panel.DataBaseSettingsPanel;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.ApproxParameters;
import iisc.dsl.picasso.common.ds.QueryPacket;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;


public class ApproxPlanGenInfo extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	private  ButtonGroup picType;
	private  JRadioButton origDiag;
	private  JRadioButton approxDiag;
	private  JComboBox userType;
	private  JComboBox samplingType;
	private  JTextField errThreshold_I;
	private  JTextField errThreshold_L;
	private  JButton   okButton, cancelButton;
	private  MainPanel	mainPanel;	
	private  JPanel user;
	/*Developer specific inputs*/
	private  DataBaseSettingsPanel dPanel;	
	/*private  JTextField sampleSize;*/	
	private  JComboBox  compareQueryList;
	//private long time;
	private int optClass ;
	private JLabel /*sampleSizeLbl,*/ compareWithLbl;
	private JTextArea statusLbl;
	private JScrollPane scrollPane;
	private JCheckBox FPCMode;
	private Font font;
	private boolean previousDiagramType;
	
	public ApproxPlanGenInfo(MainPanel mp,int optimizerClass,String status) {
		super(mp.getFrame(), "Approximate Plan Diagram Generation:", true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		optClass = optimizerClass;
		mainPanel = mp;
		previousDiagramType = mainPanel.approxDiagram;
		mainPanel.approxDiagram = false;
		PicassoConstants.IS_APPROXIMATE_DIAGRAM = false;
		dPanel = mainPanel.getDBSettingsPanel();
		font = new Font("Tahoma", Font.PLAIN, 14);
		statusLbl = new JTextArea(status,7,50);
		statusLbl.setSize(400, 100);
		statusLbl.setBackground(new Color(240,240,240));
		statusLbl.setFont(font);
		statusLbl.setEditable(false);
		scrollPane = new JScrollPane(statusLbl,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		origDiag = new JRadioButton("Generate Exact Diagram");
		origDiag.setMnemonic(KeyEvent.VK_B);
		origDiag.setActionCommand("PD");
		origDiag.setSelected(true);
		origDiag.setMargin(new Insets(0, 100, 0 ,0));
		approxDiag = new JRadioButton("Generate Approximate Diagram");
		approxDiag.setMnemonic(KeyEvent.VK_B);
		approxDiag.setActionCommand("AD");
		approxDiag.setMargin(new Insets(0, 100, 0 ,0));
		picType = new ButtonGroup();	
		picType.add(origDiag);
		picType.add(approxDiag);		
		if(PicassoConstants.IS_CLASS2_OPT_ENABLED && optClass == MessageIds.Class2)
		{		
			FPCMode = new JCheckBox("Enable FPC");
			FPCMode.setMnemonic(KeyEvent.VK_C);
			FPCMode.setSelected(false);
			FPCMode.setHorizontalAlignment(SwingConstants.RIGHT);
		}		
		//time = mainPanel.getServerPacket().queryPacket.getGenDuration();
		try {
			createGUI(mp.getServerName(), mp.getServerPort());
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		userType.addActionListener(this);	
		origDiag.addItemListener(new SelectItemListener());
		approxDiag.addItemListener(new SelectItemListener());
	}
	private void createGUI(String sName, int sPort) {
		mainPanel.tabbedPane.setEnabledAt(2, true);	
		setTitle("Please enter the following information:");
		setSize(550, 360);
		setLocation(250, 250);
		GridBagLayout gb = new GridBagLayout();
		FlowLayout fl = new FlowLayout();
		getContentPane().setLayout(gb);
		user = new JPanel();	
		user.setLayout(gb);	
		GridBagConstraints c = new GridBagConstraints();
		/*sampleSize = new JTextField("");*/	
		userType = new JComboBox();	
		samplingType = new JComboBox();		
		compareQueryList = new JComboBox();		
		errThreshold_I = new FixedLengthTextField("10", 2);
		errThreshold_L = new FixedLengthTextField("10", 2);
		errThreshold_I.setSize(3, 10);
		errThreshold_L.setSize(3, 10);
		userType.addItem("User");
		userType.addItem("Developer");
		//MessageIds.SRS
		samplingType.addItem("Sampling Algorithm (RS_NN)");
		//MessageIds.GSPQO		
		samplingType.addItem("Grid Algorithm (GS_PQO)"); 	
		//errThreshold_I.setText("10");
		//errThreshold_L.setText("10");
		compareQueryList.setMinimumSize(new Dimension(70,20));
		compareQueryList.addItem("--Select a template--");	
		try{
			int qCnt = compareQueryList.getItemCount();
			for(int i = qCnt;i>0;i--)
			{
				compareQueryList.remove(i);				
			}
			qCnt = dPanel.qtDescAll.getItemCount();
			for(int i = 0;i<qCnt;i++)
			{
				compareQueryList.addItem(dPanel.qtDescAll.getItemAt(i));				
			}
		}
		catch(Exception qe)	{}
		Font f = new Font("Tahoma", Font.PLAIN, 11);		
		JLabel userTypeLbl = new JLabel("Mode: ", JLabel.RIGHT);
		userTypeLbl.setFont(f);	
		userTypeLbl.setLabelFor(userType);
		JLabel samplingModeLbl = new JLabel("Approximation Technique: ", JLabel.RIGHT);
		samplingModeLbl.setFont(f);
		samplingModeLbl.setLabelFor(samplingType);
		/*sampleSizeLbl = new JLabel("Sample Size: ", JLabel.RIGHT);
		sampleSizeLbl.setFont(f);
		sampleSizeLbl.setLabelFor(sampleSize);*/
		compareWithLbl = new JLabel("Select diagram to compare: ", JLabel.RIGHT);
		compareWithLbl.setFont(f);
		compareWithLbl.setLabelFor(compareQueryList);
		JLabel errThresholdLbl_I = new JLabel("Identity Error Threshold (%)", JLabel.RIGHT);
		JLabel errThresholdLbl_L = new JLabel("Location Error Threshold (%)", JLabel.RIGHT);
		JLabel dummyLBL = new JLabel(" ",JLabel.RIGHT);
		JLabel dummyFld = new JLabel(" ",JLabel.LEFT);
		JLabel dummyFld2 = new JLabel(" ",JLabel.LEFT);
		dummyFld.setVisible(false);
		errThresholdLbl_I.setFont(f);
		errThresholdLbl_L.setFont(f);
		errThresholdLbl_I.setLabelFor(errThreshold_I);
		errThresholdLbl_L.setLabelFor(errThreshold_L);
		okButton = 		new JButton("     OK     ");
		cancelButton = 	new JButton("   Cancel   ");
		okButton.setHorizontalAlignment(SwingConstants.RIGHT);
		cancelButton.setHorizontalAlignment(SwingConstants.LEFT);
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;		
		c.insets = new Insets(10, 5, 5 ,10);		
		int rowCount = 0,userRowCount = 0;
		//*******************************Generation Time
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 0;
		c.gridy = rowCount++;
		scrollPane.setPreferredSize(new Dimension(450, 150));
		gb.setConstraints(scrollPane, c);
		getContentPane().add(scrollPane);
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH; 	
		//*******************************Picture Type			
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 0;
		c.gridy = rowCount++;
		gb.setConstraints(origDiag, c);
		getContentPane().add(origDiag);		
		c.gridx = 0;
		c.gridy = rowCount++;
		gb.setConstraints(approxDiag, c);
		getContentPane().add(approxDiag);			
		if(PicassoConstants.IS_DEVELOPER)
		{
		//***************************************User Mode
			c.gridwidth = 1;		
			c.gridx = 0;
			c.gridy = userRowCount;
			user.add(userTypeLbl, c);
			c.gridwidth = GridBagConstraints.REMAINDER;		
			c.gridx = 1;
			c.gridy = userRowCount++;
			user.add(userType, c); 
		}
		//***************************************Sampling Mode
		c.gridwidth = 1;		
		c.gridx = 0;
		c.gridy = userRowCount;
		user.add(samplingModeLbl, c);
		c.gridwidth = GridBagConstraints.REMAINDER;		
		c.gridx = 1;
		c.gridy = userRowCount++;
		user.add(samplingType, c);		
		/*c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 2;
		c.gridy = userRowCount-1;
		user.add(dummyLBL, c);*/
		//****************************************************Panel 0 - Error		
		c.gridwidth = 1;		
		c.gridx = 0;
		c.gridy = userRowCount;
		user.add(errThresholdLbl_I, c);
		c.gridwidth = 1;		
		c.gridx = 1;
		c.gridy = userRowCount++;
		user.add(errThreshold_I, c);
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = userRowCount-1;
		user.add(dummyLBL, c);	
		c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = userRowCount-1;
		user.add(dummyFld, c);		
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = userRowCount-1;
		user.add(dummyFld, c);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = userRowCount;
		user.add(errThresholdLbl_L, c);
		c.gridwidth = 1;//GridBagConstraints.REMAINDER;		
		c.gridx = 1;
		c.gridy = userRowCount++;
		user.add(errThreshold_L, c);
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = userRowCount-1;
		user.add(dummyLBL, c);
		c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = userRowCount-1;
		user.add(dummyFld, c);
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = userRowCount-1;
		user.add(dummyFld2, c);
		//****************************************************Panel 0 - SampleSize (Dev Mode)
		/*c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 5;
		user.add(sampleSizeLbl, c);	
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 1;
		c.gridy = 5;
		user.add(sampleSize, c);
		sampleSizeLbl.setVisible(false);
		sampleSize.setVisible(false);*/
		//***************************************Compare Query List	(Dev Mode)	
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = userRowCount;		
		user.add(compareWithLbl, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 1;
		c.gridy = userRowCount++;			
		user.add(compareQueryList,c);	
		compareWithLbl.setVisible(false);
		compareQueryList.setVisible(false);		
		//***************************************FPC Mode
		if(PicassoConstants.IS_CLASS2_OPT_ENABLED && optClass == MessageIds.Class2)
		{
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridx = 0;
			c.gridy = userRowCount;			
			user.add(FPCMode, c);
		}
		//Add user panel to contentpane
		c.gridwidth = 2;//GridBagConstraints.REMAINDER;	
		c.gridx = 0;
		c.gridy = userRowCount++;
		getContentPane().add(user, c);
		//***************************************Buttons
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.gridheight = 1;			
		c.gridx = 0;
		c.gridy = userRowCount;	
		getContentPane().add(okButton, c);
		c.gridx = 1;
		c.gridy = userRowCount++;
		getContentPane().add(cancelButton, c);
		user.setVisible(false);
		TitledBorder title;
		title = BorderFactory.createTitledBorder("Approximate Diagram Parameters");		
		title.setTitleJustification(TitledBorder.RIGHT);
		user.setBorder(title);
		samplingType.setSelectedIndex(1);
	}
	public void actionPerformed(ActionEvent e) {
		int ind = (short)samplingType.getSelectedIndex();
		mainPanel.setPlanDisplayFlag(false);
		if(e.getSource() == userType)
		{
			if(userType.getSelectedIndex()==1)
			{
				setSize(550,610);
				compareWithLbl.setVisible(true);
				compareQueryList.setVisible(true);		
				/*sampleSizeLbl.setVisible(true);
				sampleSize.setVisible(true);*/
			}
			else
			{
				setSize(550,540);
				compareWithLbl.setVisible(false);
				compareQueryList.setVisible(false);		
				/*sampleSizeLbl.setVisible(false);
				sampleSize.setVisible(false);*/
			}
		}		
		else if ( e.getSource() == okButton ) {	
			if(mainPanel.approxDiagram == true)
			{
				ApproxParameters ap = new ApproxParameters(ind);			
				ap.setValue("UserMode", userType.getSelectedIndex());
				float err_I = -1;
				float err_L = -1;
				try {
					err_I = Float.parseFloat(errThreshold_I.getText());
				} catch (NumberFormatException e2) {
					JOptionPane.showMessageDialog(this.getParent(),"Please enter valid error threshold %");
					return;
				}
				if(err_I < 1.0 || err_I > 99.0)
				{
					JOptionPane.showMessageDialog(this.getParent(),"Please enter error threshold between 1 and 99");
					return;
				}
				ap.setValue("IError", err_I );
				try {
					err_L = Float.parseFloat(errThreshold_L.getText());
				} catch (NumberFormatException e2) {
					JOptionPane.showMessageDialog(this.getParent(),"Please enter valid error threshold %");
					return;
				}
				if(err_L < 1.0 || err_L > 99.0)
				{
					JOptionPane.showMessageDialog(this.getParent(),"Please enter error threshold between 1 and 99");
					return;
				}
				ap.setValue("LError", err_L );
				if(userType.getSelectedIndex()==1)
				{
					/*int sSize = -1;
					try {
						sSize = Integer.parseInt(sampleSize.getText());						
					} catch (NumberFormatException e1) {
						;
					}	
					finally{
						if(sSize>=0)
							ap.setValue("SampleSize",sSize);
						else
						{
							ap.setValue("SampleSize","0");
							//JOptionPane.showMessageDialog(this.getParent(),"Sample size must be a positive integer");
							//return;
						}
					}*/
					ap.setValue("SampleSize","0");
					int cQueryIndex = compareQueryList.getSelectedIndex();			
			    	String cQuery =  "-1";
			    	if(cQueryIndex>0)
			    	{
			    		QueryPacket qp = (QueryPacket)compareQueryList.getItemAt(cQueryIndex); 
			    		cQuery = qp.getQueryName();
			    	}
			    	ap.setValue("CompareQuery",cQuery);		
				}
				ap.optClass = optClass;
				mainPanel.getClientPacket().setApproxParameters(ap);
				if(PicassoConstants.IS_CLASS2_OPT_ENABLED && optClass == MessageIds.Class2)
				{
					ap.FPCMode = FPCMode.isSelected();
				}
			}
			else // if its an exact diagram
			{
				mainPanel.getDBSettingsPanel().getQtDesc().setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
				mainPanel.getDBSettingsPanel().repaintQtDesc();
			}
			mainPanel.setPlanDisplayFlag(true);
			this.dispose();			
		} else if ( e.getSource() == cancelButton ) {
			mainPanel.approxDiagram = previousDiagramType;
			mainPanel.setPlanDisplayFlag(false);
			mainPanel.getDBSettingsPanel().curQName=mainPanel.getDBSettingsPanel().prevQName;
			if(mainPanel.getDBSettingsPanel().getQtDesc().dummyEntry)
			{
				mainPanel.getDBSettingsPanel().removeQid(mainPanel.getDBSettingsPanel().prevQName.getQueryName());
				// mainPanel.paramsChanged = false;
			}
			this.dispose();				
		}
	}	
	class SelectItemListener implements ItemListener{
		public void itemStateChanged(ItemEvent e){
			AbstractButton sel = (AbstractButton)e.getItemSelectable();
			if(e.getStateChange() == ItemEvent.SELECTED){								
				if (sel.getActionCommand().equals("AD"))
				{
					setSize(550,540);
					user.setVisible(true);
					mainPanel.approxDiagram = true;
					PicassoConstants.IS_APPROXIMATE_DIAGRAM = true;
				}
				else
				{
					user.setVisible(false);
					userType.setSelectedIndex(0);
					mainPanel.approxDiagram = false;
					setSize(550,360);
					PicassoConstants.IS_APPROXIMATE_DIAGRAM = false;
				}
			}
		}
	}
	class FixedLengthPlainDocument extends PlainDocument 
	{
		static final long serialVersionUID = -98989898989898L;
		private int maxLength;
		public FixedLengthPlainDocument(int maxLength) {
			this.maxLength = maxLength;			
		}
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			if(getLength() + str.length() > maxLength) {
				Toolkit.getDefaultToolkit().beep();
			}
			else if(!str.matches("[0-9]*")) {
				Toolkit.getDefaultToolkit().beep();
			}
			else {
				super.insertString(offset, str, a);
			}
		}
	}

	class FixedLengthTextField extends JTextField 
	{
		static final long serialVersionUID = -89898989898989L;
		public FixedLengthTextField(int length){
			this(null, length);	
			this.setSize(2, 10);
		}
		public FixedLengthTextField(String text, int length) {
			super(new FixedLengthPlainDocument(length), text,length);
			this.setSize(2, 10);
		}
	}
}

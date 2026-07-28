
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
//import iisc.dsl.picasso.client.frame.PredicateValuesFrame;

import iisc.dsl.picasso.client.util.QueryLoader;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.ds.QueryPacket;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
//import java.util.Hashtable;
import java.util.*;
import javax.swing.JButton;
//import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

class FixedLengthPlainDocument extends PlainDocument
{ 
	                
	                private int maxLength; 
	                 
	                public FixedLengthPlainDocument(int maxLength) { 
	                          this.maxLength = maxLength; 
	                } 
	                public void insertString(int offset, String str,
AttributeSet a) 
	                        throws BadLocationException { 
	                        if(getLength() + str.length() > maxLength) {

	                                Toolkit.getDefaultToolkit().beep(); 
	                        } 
	                        else { 
	                                 super.insertString(offset, str, a);

	                        } 
	                }                
	                
	    } 
	        
	        class FixedLengthTextField extends JTextField { 
	                
	                public FixedLengthTextField(int length) 
	                { 
	                        this(null, length); 
	                } 
	                
	                public FixedLengthTextField(String text, int length)
{ 
	                        super(new FixedLengthPlainDocument(length),
text, length); 
	                } 

	        }
	        public class QueryBuilderPanel extends PicassoPanel implements ActionListener,KeyListener {
	// Auto Generated Serial ID
	private static final long serialVersionUID = 2L;
	private JButton 		queryButton, /*setButton,*/ clearButton;
	private JTextArea 		queryText;
	private JTextField 		queryName;
	private JLabel 			qNameLbl;
	private Document		doc;
	private DefaultHighlighter hl;
	private String			prevQueryText, prevQueryName, queryFileName;
	
	public QueryBuilderPanel (MainPanel app) {
		super(app);
		setPanelString("QueryPanel");
		createQueryGUI();
		doc = queryText.getDocument();
		doc.addDocumentListener(documentListener);
		Document doc1 = queryName.getDocument();
		doc1.addDocumentListener(documentListener);
	}
	//This function is called when a query template is appended with a plan
	public void setQueryPlusPlan(String plan, String planno) {
		if (getPParent().getDBSettingsPanel().getDbVendor().equals(DBConstants.MSSQL))
			queryText.setText(queryText.getText() + "--Picasso_Abstract_Plan\noption (use plan N'" + plan + "')\n");
		else if (getPParent().getDBSettingsPanel().getDbVendor().equals(DBConstants.SYBASE))
			queryText.setText(queryText.getText() + "--Picasso_Abstract_Plan\nplan '" + plan + "'\n");
		int planNumber = getPParent().getSortedPlan()[0][Integer.parseInt(planno)]+1;
		queryName.setText(queryName.getText() + "_P" + planNumber);
	}
	public void createQueryGUI() {
		BorderLayout bl = new BorderLayout();
		bl.setHgap(10);
		bl.setVgap(10);
		setLayout(bl);
		setBackground(Color.lightGray);

		JPanel topPanel = new JPanel();
		add(topPanel, BorderLayout.NORTH);
		GridBagLayout gb = new GridBagLayout();
		topPanel.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints();
		Font f = new Font("Courier", Font.PLAIN, 12);
		qNameLbl = new JLabel("QueryTemplate Descriptor: ", JLabel.CENTER);
		qNameLbl.setFont(f);

		queryName = new FixedLengthTextField("",PicassoConstants.QTNAME_LENGTH);
		queryName.setForeground(Color.BLUE);
		queryName.setToolTipText("Enter QueryTemplate Descriptor");
		queryName.setMinimumSize(new Dimension(200,20));
		queryName.setMaximumSize(new Dimension(250,20));
		queryName.addKeyListener(this);
	
		queryButton = new JButton("Load QueryTemplate");
		queryButton.setBackground(PicassoConstants.BUTTON_COLOR);
		queryButton.addActionListener(this);
		queryButton.setToolTipText("Loads QueryTemplate from file");
		clearButton = new JButton("Clear QueryTemplate");
		clearButton.setBackground(PicassoConstants.BUTTON_COLOR);
		clearButton.addActionListener(this);
		clearButton.setToolTipText("Clears QueryTemplate");
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.ipady = 2;
		c.ipadx = 2;
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.NONE;
		topPanel.add(qNameLbl, c);
		
		c.gridx = 1;
		c.insets = new Insets(2, 0, 2, 0);
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(queryName, c);
		c.gridx = 2;
		c.insets = new Insets(2, 60, 2, 10);
		c.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(queryButton, c);
		
		c.gridx = 3;
		c.insets = new Insets(2, 10, 2, 10);
		topPanel.add(clearButton, c);
		c.gridy = 0;
		c.gridx = 0;

		queryText = new JTextArea();
		hl = new DefaultHighlighter();
		queryText.setHighlighter(hl);
		queryText.setForeground(Color.BLUE);
		queryText.addKeyListener(this);
		JScrollPane jsp = new JScrollPane(queryText);
		add(jsp, BorderLayout.CENTER);
	}
	private void highlightParamText(JTextArea textArea) {
		try {
			// This code only supports predicate from 1-9.. Need to change the algo
			// if we need to support greater than 9.
			String content = doc.getText(0, doc.getLength()).toLowerCase();
			content=content+" ";
			int beginIndex = 0, lastIndex = 0;
			int index = 1;
			hl.removeAllHighlights();
			int 		curIndex=0;
			PicassoConstants.params = new String[9];
			for (int i=0; i < PicassoConstants.params.length; i++)
				PicassoConstants.params[i] = null;
			int NUM_DIMS=0;
			while ( (lastIndex = content.indexOf(":varies", lastIndex)) != -1) 
			{
				NUM_DIMS++;
				lastIndex += 6;
				beginIndex = lastIndex;
				while ( true ) 
				{
					if ( content.startsWith("and", beginIndex) 
						|| content.startsWith("where", beginIndex)
						|| content.startsWith("or ", beginIndex)
						|| content.startsWith("like ", beginIndex)
						|| beginIndex == 0 )
						break;
					beginIndex--;
				}
				String s = content.substring(beginIndex, lastIndex+2);
				s = s.replaceAll("\\s+", " ");
				int spIndex = s.indexOf(' ');
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
				PicassoConstants.params[curIndex] = s;

				int hlIndex = content.indexOf(s, beginIndex);
				hl.addHighlight(hlIndex, lastIndex+2, new DefaultHighlighter.DefaultHighlightPainter(PicassoConstants.HIGHLIGHT_COLOR));
				index++;
				lastIndex+=2;
				curIndex++;
			}		
			PicassoConstants.NUM_DIMS=NUM_DIMS;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
//	this function is used to handle events related to buttons
	public void actionPerformed (ActionEvent evt)  {
		Object source = evt.getSource ();
		if (source == queryButton) 
		{
			String query = QueryLoader.getText(getPParent());
			if ( query.length() > 0 ) {
				
				QueryPacket backup = null;
				if(parent.getDBSettingsPanel().getQtDesc() != null)
					backup = new QueryPacket(parent.getDBSettingsPanel().getQtDesc());
				
				Font f = new Font("Arial", Font.PLAIN, 14);
				queryText.setFont(f);
				queryText.setText(query);
				queryText.setCaretPosition(0);
				/*parent.getDBSettingsPanel().getQtDesc().setSelectedIndex(-1);
				parent.getDBSettingsPanel().getQtDescAll().setSelectedIndex(-1);*/
				parent.getDBSettingsPanel().setrange(0);
				parent.getDBSettingsPanel().setresbox(0);
				parent.getDBSettingsPanel().setdistribution(0);
				// a = parent.getDBSettingsPanel().getQtDesc();
				setPrefix();
				for(int i=0;i<5;i++)
				{
					parent.getDBSettingsPanel().locrange[0][i]=0;
					parent.getDBSettingsPanel().locrange[1][i]=1;
					parent.getDBSettingsPanel().locres[i]=10;
				}
// 				a = parent.getDBSettingsPanel().getQtDesc();
				//getPParent().emptyAllTabs();//danger
				if(backup != null)
					parent.getDBSettingsPanel().setQtDescItem(backup);
				setSettingsChanged(true);
				this.parent.getDBSettingsPanel().readonly=false;
			}
			
		}
		else if ( source == clearButton ) {
			queryText.setText("");
			queryName.setText("");
			getPParent().emptyLegendPanel();
			setPrefix();
			//getPParent().emptyAllTabs();//daner
			setSettingsChanged(true);
		} else if (source == queryName){
			getPParent().getDBSettingsPanel().readonly=false;
			setSettingsChanged(true);
		}
		repaint();
	}
	
	private void setPrefix()
	{
		// Save previous query text and query name..
		prevQueryText = queryText.getText().trim();
		prevQueryName = (String)queryName.getText().trim();
		String dbVendor = getPParent().getDBSettingsPanel().getDbVendor();
		String resolution = getPParent().getDBSettingsPanel().getResolution();
		String range = getPParent().getDBSettingsPanel().getRangeStr();
		String optimization = getPParent().getDBSettingsPanel().getOptLevel();
        String distribution = getPParent().getDBSettingsPanel().getDistribution();
		if(dbVendor == null)
			return;
		highlightParamText(queryText);
		String distStr = null;
		String planDiffStr = null;
		if(distribution.equalsIgnoreCase(PicassoConstants.UNIFORM_DISTRIBUTION))
			distStr = "_U_";
		else
			distStr = "_E_";

		planDiffStr="";
		if(prevQueryName==null || prevQueryName.equals("") || prevQueryName.startsWith(dbVendor)){
			prevQueryName="";
			String queryNameStr = dbVendor + "_" + optimization + planDiffStr + distStr; 
			queryName.setText(queryNameStr);
			queryName.setCaretPosition(queryNameStr.length());
			queryName.setSelectionStart(0);
			queryName.setSelectionEnd(queryNameStr.length());
			queryName.requestFocus();
		}
	}

	public void process() {
		// This should not go to the server, it should do nothing
		//sendMessageToServer(buildMessages()); We don't nedd to talk to the server here...
	}
	public void changeQueryFields(QueryPacket qp) {
		queryName.setText(qp.getQueryName());
		queryText.setText(qp.getQueryTemplate());
		parent.emptyLegendPanel();
	}
	public void emptyQueryFields() {
		queryName.setText("");
		queryText.setText("");
		parent.emptyLegendPanel();
	}
	public String getQueryText() {
		String txt = queryText.getText();
		return(txt);
	}
	public String getQueryName() {
		return(queryName.getText());
	}
	public void restorePrevQuery() {
		queryText.setText(prevQueryText);
		queryName.setText(prevQueryName);
	}
	DocumentListener documentListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) {
			highlightParamText(queryText);
			setSettingsChanged(true);
		}
		public void insertUpdate(DocumentEvent e) {
			highlightParamText(queryText);
			setSettingsChanged(true);
		}
		public void removeUpdate(DocumentEvent e) {
			highlightParamText(queryText);
			setSettingsChanged(true);
		}
	};

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
//	parent.getDBSettingsPanel().readonly=false;	
	}
	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
	
	}
	@Override
	public void keyTyped(KeyEvent e) {
		parent.getDBSettingsPanel().readonly=false;
	}
}

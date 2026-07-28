
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

package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.panel.DataBaseSettingsPanel;
import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class DatabaseInfoFrame extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = -8088437034960288910L;
	
	private JTextField 	dbNameText, dbUserNameText,
						dbServerText, dbPortText, dbSchemaText, settingsNameText;
	private JPasswordField dbPasswordText;
	private JComboBox   dbVendor;
	private JButton		okButton, cancelButton;
	
	private String		settingsName, prevSettings;
	private DataBaseSettingsPanel	dbPanel;
	private MainPanel	mainPanel;
	private int			opType;
	
	public DatabaseInfoFrame(MainPanel mp, DataBaseSettingsPanel panel, int op) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		mainPanel = mp;
		opType = op;
		dbPanel = panel;
		
		if ( opType != PicassoConstants.NEW_DB_INSTANCE ) {
		    settingsName = dbPanel.getDBInstance();
                }
                else {
		    settingsName = "";
                }
		prevSettings = settingsName;
		createDBGUI();
		dbNameText.addFocusListener(focusListener);
		dbUserNameText.addFocusListener(focusListener);
		dbServerText.addFocusListener(focusListener);
		dbPortText.addFocusListener(focusListener);
		dbSchemaText.addFocusListener(focusListener);
		settingsNameText.addFocusListener(focusListener);
		dbPasswordText.addFocusListener(focusListener);
		
		dbVendor.addItemListener(itemListener);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
	}
	
	private void createDBGUI() {
		setBackground(Color.LIGHT_GRAY);
		setTitle("DB Connection Settings");
		setSize(900, 200);
		setLocation(100,300);
		setResizable(false);
		
		// Should take it from a client settings file if it exists...
		//String dbVersion = "Oracle 10g";
		//MessageUtil.CPrintToConsole("DB INSTANCE NAME :: " + settingsName);
		DBSettings dbSettings = mainPanel.getDBSettings().get(settingsName);
		String dbName, dbServerName, dbUserName, dbPassword, dbServerPort, dbSchema, dbType;
		if ( dbSettings != null ) {
			dbName = dbSettings.getDbName();
			dbServerName = dbSettings.getServerName();
			dbUserName = dbSettings.getUserName();
			dbPassword = dbSettings.getPassword();
			dbServerPort = dbSettings.getServerPort();
			dbSchema = dbSettings.getSchema();
			dbType = dbSettings.getDbVendor();
		} else {
			dbName = "";
			dbServerName = "localhost";
			dbUserName = "";
			dbPassword = "";
			dbServerPort = "";
			dbSchema = "";
			dbType = "";
		}
		
		Font f = new Font("Courier", Font.BOLD, 12);
		JLabel dbInstLbl = new JLabel("DBConnection Descriptor: ", JLabel.RIGHT);
		dbInstLbl.setFont(f);
		
		settingsNameText = new JTextField(settingsName);
		JLabel srvrLbl = new JLabel("Machine: ", JLabel.RIGHT);
		srvrLbl.setFont(f);
		
		dbServerText = new JTextField(dbServerName);
		JLabel portLbl = new JLabel("Port: ", JLabel.RIGHT);
		portLbl.setFont(f);
		
		dbPortText = new JTextField(dbServerPort);
		JLabel nameLbl = new JLabel("Database: ", JLabel.RIGHT);
		nameLbl.setFont(f);
		dbNameText = new JTextField(dbName);
		
		JLabel dbUserLbl = new JLabel("User: ", JLabel.RIGHT);
		dbUserLbl.setFont(f);
		dbUserNameText = new JTextField(dbUserName);
		
		JLabel dbPasswordLbl = new JLabel("Password: ", JLabel.RIGHT);
		dbPasswordLbl.setFont(f);
		dbPasswordText = new JPasswordField(dbPassword);
		dbPasswordText.setEchoChar('*');
		
		JLabel schLbl = new JLabel("Schema: ", JLabel.RIGHT);
		schLbl.setFont(f);
		
		JLabel dbLbl = new JLabel("Engine: ", JLabel.RIGHT);
		dbLbl.setFont(f);
		dbVendor = new JComboBox();
		// Do not change this order, if changed please change the ids in constants too..
		for(int i=0; i<DBConstants.databases.length; i++)
			dbVendor.addItem(DBConstants.databases[i].name);
		if(dbType.equals(""))
			dbVendor.setSelectedIndex(-1);
		dbVendor.setSelectedItem(dbType);
		dbSchemaText = new JTextField(dbSchema);

		if (opType == PicassoConstants.DELETE_DB_INSTANCE ) {
			settingsNameText.setEditable(false);
			dbNameText.setEditable(false);
			dbVendor.setEnabled(false);
			dbUserNameText.setEditable(false);
			dbPortText.setEditable(false);
			dbSchemaText.setEditable(false);
			dbServerText.setEditable(false);
			dbPasswordText.setEditable(false);
		}
		
		if ( opType == PicassoConstants.NEW_DB_INSTANCE ) 
			okButton = new JButton("Save");
		else if ( opType == PicassoConstants.EDIT_DB_INSTANCE )
			okButton = new JButton("Save");
		else if ( opType == PicassoConstants.DELETE_DB_INSTANCE )
			okButton = new JButton("Delete");
		
		cancelButton = new JButton("Cancel");
		
		getContentPane().setLayout(new GridLayout(4, 8, 5, 25));

//first row	
		JLabel dbLbl1 = new JLabel("Connection", JLabel.RIGHT);
		dbLbl1.setFont(f);
		JLabel dbLbl2 = new JLabel("Descriptor:", JLabel.LEFT);
		dbLbl2.setFont(f);
		
//		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		getContentPane().add(dbLbl1);
		getContentPane().add(dbLbl2);
		getContentPane().add(settingsNameText);
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		
//second row
		getContentPane().add(srvrLbl);
		getContentPane().add(dbServerText);
		getContentPane().add(dbLbl);
		getContentPane().add(dbVendor);
		getContentPane().add(portLbl);
		getContentPane().add(dbPortText);
		getContentPane().add(new JLabel(""));
  		getContentPane().add(new JLabel(""));
		
// third row
		getContentPane().add(nameLbl);
		getContentPane().add(dbNameText);
		getContentPane().add(schLbl);
		getContentPane().add(dbSchemaText);
		getContentPane().add(dbUserLbl);
		getContentPane().add(dbUserNameText);
		getContentPane().add(dbPasswordLbl);
		getContentPane().add(dbPasswordText);

//fourth row
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		getContentPane().add(okButton);
		getContentPane().add(new JLabel(""));
		getContentPane().add(cancelButton);
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
		getContentPane().add(new JLabel(""));
	}
	
	boolean checkFieldEmpty() {
		if ( dbNameText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "DB Name is empty", "Error",JOptionPane.ERROR_MESSAGE);
			dbNameText.setFocusable(true);
			return true;
		}
		if ( dbUserNameText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "DB User Name is empty","Error",JOptionPane.ERROR_MESSAGE);
			dbUserNameText.setFocusable(true);
			return true;
		}
		if ( dbPortText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "Port is empty","Error",JOptionPane.ERROR_MESSAGE);
			dbPortText.setFocusable(true);
			return true;
		}
		if ( settingsNameText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "DBConnection Descriptor is empty","Error",JOptionPane.ERROR_MESSAGE);
			settingsNameText.setFocusable(true);
			return true;
		}
		if ( dbServerText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "Server Name is empty","Error",JOptionPane.ERROR_MESSAGE);
			dbServerText.setFocusable(true);
			return true;
		}
		if ( dbSchemaText.getText().length() == 0 ) {
			JOptionPane.showMessageDialog(this, "DB Schema Name is empty","Error",JOptionPane.ERROR_MESSAGE);
			dbSchemaText.setFocusable(true);
			return true;
		}
		return false;
	}
	
	public void actionPerformed(ActionEvent e) {
		DBSettings dbSettings = new DBSettings();
		dbSettings.setDbName(dbNameText.getText());
		dbSettings.setDbVendor((String)dbVendor.getSelectedItem());
		dbSettings.setUserName(dbUserNameText.getText());
		dbSettings.setServerPort(dbPortText.getText());
		dbSettings.setInstanceName(settingsNameText.getText());
		dbSettings.setServerName(dbServerText.getText());
		dbSettings.setOptLevel("DEFAULT"); //(String)optBox.getSelectedItem();
		dbSettings.setPassword(new String(dbPasswordText.getPassword()));
		dbSettings.setSchema(dbSchemaText.getText());
		
		if ( e.getSource() == okButton ) {
			if ( opType != PicassoConstants.DELETE_DB_INSTANCE ) {
				if (checkFieldEmpty() == false) {
					mainPanel.changeDBSettings(opType, prevSettings, dbSettings);
					this.dispose();
				}
			} else {
				mainPanel.changeDBSettings(opType, prevSettings, dbSettings);
				this.dispose();
			}
		} else if ( e.getSource() == cancelButton )
			this.dispose();
	}
	
	FocusListener focusListener = new FocusListener() {

		public void focusGained(FocusEvent e) {
			Object evt = e.getSource();
			if ( evt.getClass() == dbNameText.getClass() ) {
				JTextField tf = (JTextField)evt;
				tf.setSelectionStart(0);
				tf.setSelectionEnd(tf.getText().length());
			}
		}

		public void focusLost(FocusEvent e) {
		}
	};
	
	ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object source = e.getSource();
			DBSettings dbSettings = new DBSettings();
			
			if ( source == dbVendor ) {
				String msg = (String)dbVendor.getSelectedItem();
				if(msg == null)
					return;
				for(int i=0; i<DBConstants.databases.length; i++){
					DBInfo dbinfo = DBConstants.databases[i];
					if(msg.equals(dbinfo.name)){
                                                //dbSettings.setServerName("localhost");
                                                dbSettings.setServerName(dbServerText.getText());
						dbSettings.setServerPort(dbinfo.defaultPort);
						dbSettings.setDbName("");
						dbSettings.setSchema("");
						dbSettings.setUserName("");
						dbSettings.setPassword("");
					}
				}
			
				dbServerText.setText(dbSettings.getServerName());
				dbPortText.setText(dbSettings.getServerPort());
				dbNameText.setText(dbSettings.getDbName());
				dbSchemaText.setText(dbSettings.getSchema());
				dbUserNameText.setText(dbSettings.getUserName());
				dbPasswordText.setText(dbSettings.getPassword());
			}
		}
	};
}

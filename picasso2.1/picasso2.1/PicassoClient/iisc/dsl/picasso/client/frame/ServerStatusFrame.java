
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

import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.ServerPacket;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;

public class ServerStatusFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = -169108145725667922L;

	JComboBox 	qidNames;
	Vector		procs;
	JButton 	deleteButton, cancelButton;
	String		serverName;
	int			serverPort;
	JLabel		progress;
	
	public ServerStatusFrame(String name, int port, ClientPacket cp, ServerPacket sp) {
		setSize(400, 200);
		setTitle("Server( " + name + ":" + port + ") Status");
		setLocation(100, 100);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		serverName = name;
		serverPort = port;
		procs = sp.queries;
		
		JLabel serverInfo = new JLabel("Server is operational(Name: "+serverName
					+", Port: " + serverPort + ")");
		JLabel qidLbl = new JLabel("QueryTemplate Descriptor: ", JLabel.RIGHT);
		qidNames = new JComboBox();
		for (int i=0; i < procs.size(); i++) {
			ClientPacket p = (ClientPacket)procs.elementAt(i);
			qidNames.addItem(p.getQueryPacket().getQueryName());
		}
		ClientPacket dp = (ClientPacket)procs.elementAt(0);
		JLabel dbInfoLbl = new JLabel("DB Information", JLabel.RIGHT);
		String dbInfoStr = PicassoUtil.getDBInfoString(dp.getDBSettings());
		JLabel dbInfo = new JLabel(dbInfoStr, JLabel.LEFT);
		//JLabel msgLbl = new JLabel("Command Sent : ", JLabel.RIGHT);
		JLabel msgLbl = new JLabel("", JLabel.RIGHT);
		JLabel msgText = new JLabel(dp.getMessageId(), JLabel.RIGHT);
		JLabel progressLbl = new JLabel("Progress: ", JLabel.RIGHT);
		progress = new JLabel(dp.getProgress()+"% Done", JLabel.LEFT);
		
		deleteButton = new JButton("Delete");
		cancelButton = new JButton("Close");
		deleteButton.addActionListener(this);
		cancelButton.addActionListener(this);
		qidNames.addActionListener(this);
		
		getContentPane().setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		c.insets = new Insets(10, 10, 10, 10);
		c.fill = GridBagConstraints.NONE;
		
		getContentPane().add(serverInfo, c);
		
		c.gridy = 1;
		c.gridx = 0;
		getContentPane().add(qidLbl, c);
		
		c.gridx = 1;
		getContentPane().add(qidNames, c);
		
		c.gridy = 2;
		c.gridx = 0;
		getContentPane().add(dbInfoLbl, c);
		
		c.gridx = 1;
		getContentPane().add(dbInfo, c);
		
		c.gridy = 3;
		c.gridx = 0;
		getContentPane().add(progressLbl, c);
		
		c.gridx = 1;
		getContentPane().add(progress, c);
		
		c.gridy = 4;
		c.gridx = 0;
		getContentPane().add(deleteButton, c);
		
		c.gridx = 1;
		getContentPane().add(cancelButton, c);
		
		c.gridy = 5;
		c.gridx = 0;
		getContentPane().add(msgLbl, c);
		
		c.gridx = 1;
		//c.gridheight = 3;
		c.fill = GridBagConstraints.BOTH;
		getContentPane().add(msgText, c);
	}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == deleteButton ) {
			int index = qidNames.getSelectedIndex();
			if ( index < 0 )
				return;
			ClientPacket p = (ClientPacket)procs.elementAt(index);
			MessageUtil.sendDeleteProcessToServer(serverName, serverPort, p);
			qidNames.removeItemAt(index);
			procs.remove(index);
		} else if ( e.getSource() == cancelButton ) {
			this.dispose();
		} else if ( e.getSource() == qidNames ) {
			int index = qidNames.getSelectedIndex();
			if ( index < 0 )
				return;
			ClientPacket p = (ClientPacket)procs.elementAt(index);
			progress.setText(p.getProgress() + "% done");
		}
	}
}

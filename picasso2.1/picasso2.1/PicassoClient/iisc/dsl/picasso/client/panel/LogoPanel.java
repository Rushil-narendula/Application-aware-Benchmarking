
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

import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class LogoPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	ImageIcon 	dslLogo, iiscLogo;
	URL 		url, iiscurl;
	JButton		dslButton, iiscButton, header;
	
	public LogoPanel() {
		// initialise logo image	
		try
		{
			url = getClass().getClassLoader().getResource(PicassoConstants.MINI_LOGO);
			dslLogo = new ImageIcon(url);
			
			iiscurl = getClass().getClassLoader().getResource(PicassoConstants.IISC_LOGO);
			iiscLogo = new ImageIcon(iiscurl);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this.getParent(), "Logo of DSL is missing!",
					"Missing", JOptionPane.ERROR_MESSAGE);	
		}
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		
		dslButton = new JButton("", dslLogo);
		iiscButton = new JButton("", iiscLogo);
		dslButton.addActionListener(this);
		iiscButton.addActionListener(this);
		dslButton.setToolTipText("Go To DSL Home Page");
		iiscButton.setToolTipText("Go To IISc Home Page");
		dslButton.setPreferredSize(new Dimension(dslLogo.getIconWidth(), dslLogo.getIconHeight()));
		iiscButton.setPreferredSize(new Dimension(iiscLogo.getIconWidth(), iiscLogo.getIconHeight()));
		
		//button.setEnabled(false);
		Font font = new Font("Arial", Font.BOLD, 14);
		header = new JButton();
		header.setText("Picasso Database Query Optimizer Visualizer 2.1");
		header.setFont(font);
		header.addActionListener(this);
		//header.setBackground(Color.LIGHT_GRAY);
		header.setForeground(Color.RED);
		header.setBorderPainted(false);
		header.setToolTipText("Go To Picasso Home Page");
		
		JLabel copyright = new JLabel("       Copyright \u00A9 Indian Institute of Science, Bangalore, India", JLabel.CENTER);
		copyright.setForeground(Color.BLACK);
		font = new Font("Arial", Font.PLAIN, 12);
		copyright.setFont(font);
		
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		c.insets = new Insets(0, 0, 0, 0);
		//c.fill = GridBagConstraints.BOTH;
		
		c.gridheight = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		add(dslButton, c);
		
		c.gridx = 3;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		add(header, c);
		
		c.gridx = 3;
		c.gridy = 1;
		add(copyright,c);
		
		c.gridx = 4;
		c.gridy = 0;
		c.gridheight = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		add(iiscButton, c);
	}
	
	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == dslButton ) {
			try {
				PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in");
			} catch (Exception ex) {}
		} else if ( e.getSource() == iiscButton ) {
			try {
				PicassoUtil.openURL("http://www.iisc.ernet.in");
			} catch (Exception ex) {}
		} else if ( e.getSource() == header ) {
			try {
				PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html");
			} catch (Exception ex) {}
		}
	}
}

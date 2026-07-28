
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

import iisc.dsl.picasso.client.Picasso_Frame;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.text.AttributedString;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class AboutFrame extends JFrame implements MouseListener, MouseMotionListener, ActionListener {

	private static final long serialVersionUID = -3157705397756034506L;

	//	location of url string
	int urlX1=85, urlX2=415, urlY1=165, urlY2=175;
	
	//logo
	Image 	logo;
	URL 	url;
	JPanel 	dispPanel;
	JButton okBtn;
	boolean	source;
	String serverName, serverPort, currentDir;
	Frame sourceFrame;
	Image offscreenImage;
	Graphics offscreenGraphics;
	JButton enterButton; 
	
	public AboutFrame(boolean src) {
		CreateGUI("", "", "", src);
	}
	
	public AboutFrame(String sn, String sp, String cwd, boolean src) {
		CreateGUI(sn, sp, cwd, src);
	}
	
	void CreateGUI(String sn, String sp, String cwd, boolean src) {
		serverName = sn;
		serverPort = sp;
		currentDir = cwd;
		source = src;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		if(source == true)
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		else
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//	initialise logo image
		try
		{
			url=ClassLoader.getSystemResource(PicassoConstants.IMAGE_URL);
			logo = new javax.swing.ImageIcon(url).getImage();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Logo of DSL is missing!",
					"Missing", JOptionPane.ERROR_MESSAGE);	
		}
		setTitle("Picasso Database Query Optimizer Visualizer 2.1");
		addMouseListener(this);
		setSize(500, 375);
		setResizable(false);
		setLocation(260, 200);
        try {
        	setUndecorated(true);
        } catch(Exception e) {
        	
        }
		
		dispPanel = new JPanel();
		okBtn = new JButton("OK");
                getContentPane().setLayout(null);
		//getContentPane().setLayout(new java.awt.BorderLayout());
		//getContentPane().add(dispPanel, BorderLayout.CENTER);
		
		/*JPanel LOGO = new JPanel();
                getContentPane().add(LOGO, BorderLayout.CENTER);
                Image 	logo1 = new javax.swing.ImageIcon("images/tarunlogo.jpg").getImage();
                LOGO.getGraphics().drawImage(logo1,0,70,this);*/
                
                /*sourceFrame = new Frame(); 
		sourceFrame.addNotify();
		offscreenImage = sourceFrame.createImage(640, 400);
		offscreenGraphics = offscreenImage.getGraphics();*/
	
		if ( source == true ){
			enterButton = new JButton("Enter");
			enterButton.setToolTipText ("Click here to know more about Picasso");
		}
		else
			enterButton = new JButton("OK");
		
                enterButton.setBounds(20,330,460,25);
                getContentPane().add(enterButton);
		enterButton.addActionListener(this);
		enterButton.setEnabled(true);
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public void paint(Graphics g)
        {
			super.paint(g);
            g.drawImage(logo,0,0,500,375,this);
            enterButton.repaint();
        }
	
	public void mouseClicked( MouseEvent e)
	{
		int x=e.getX(),y=e.getY();
		
		if(urlX1<=x && urlX2>=x && urlY1<=y && urlY2>=y)
		{
			try {
				//Runtime.getRuntime().exec("explorer http://dsl.serc.iisc.ernet.in/projects/PICASSO");
				PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html");
			} catch (Exception ex) {}
		}
	}
	
	public void mouseMoved( MouseEvent e)
	{
		int x=e.getX(),y=e.getY();
		
		if(urlX1<=x && urlX2>=x && urlY1<=y && urlY2>=y)
			setCursor(new Cursor(Cursor.HAND_CURSOR));
		//setCursor(new Cursor(Cursor.TEXT_CURSOR));
		else
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void mouseDragged(MouseEvent e) {}
	
	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}
		
	public void mousePressed(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == enterButton ) {
			if ( source == true )
				new Picasso_Frame(serverName, serverPort, currentDir).setVisible(true);
			this.dispose();
		}
	}

}

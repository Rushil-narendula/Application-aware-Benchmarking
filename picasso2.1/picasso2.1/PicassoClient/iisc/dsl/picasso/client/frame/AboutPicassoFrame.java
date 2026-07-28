package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.util.PicassoUtil;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;

public class AboutPicassoFrame extends JFrame implements MouseListener, MouseMotionListener {
	public static String reldate="April 01, 2008";

	private static final long serialVersionUID = -1844594094107158514L;

	public AboutPicassoFrame() {
		setSize(425, 350);
		setLocation(330, 300);
		setTitle("About Picasso");
		this.setResizable(false);
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	//The places clicking where any of the 7 URLs should open
	int picx, picy, picl;
	int dslx, dsly, dsll;
	int iiscx, iiscy, iiscl;
	int copyx, copyy, copyl;
	int licx, licy, licl;
	int mailx, maily, maill;
	int okx, oky, okl;
	int yHeight = 15;
	
	public void paint(Graphics g) {

		int yPos = 50;
		int xPos = 10;

		super.paint(g);
		Font font = new Font("Arial", Font.PLAIN, 18);
		g.setFont(font);
		g.setColor(Color.RED);
		g.drawString("Picasso Database Query Optimizer Visualizer", xPos+50-40, yPos);
		picx = 60-40; picy = 35; picl = 420-40; 
		yPos += 25;
		
		font = new Font("Arial", Font.PLAIN, 12);
		g.setFont(font);
		g.setColor(Color.BLACK);
		
		g.drawString("Version: 2.1 (February 2011)   Build: " + reldate, xPos+60, yPos);
		yPos += 2*yHeight;
		
		g.drawString("Developed by:", xPos, yPos);
		yPos += 2*yHeight;
		
		g.setColor(Color.BLUE);
		g.drawString("Database Systems Lab ", xPos, yPos);
		dslx = 10; dsly = 125; dsll = 140;
		yPos += yHeight;
		
		g.drawString("Indian Institute of Science ", xPos, yPos);
		iiscx = 10; iiscy = 140; iiscl = 150;
		yPos += yHeight;
		
		g.setColor(Color.BLACK);
		g.drawString("Bangalore 560012, INDIA.", xPos, yPos);
		yPos += 3*yHeight;
		
		g.drawString("Copyright:", xPos, yPos);
		
		g.setColor(Color.BLUE);
		g.drawString("Indian Institute of Science",xPos+60, yPos);
		copyx = 70; copyy = 200; copyl = 210;
		yPos += 2*yHeight;
		
		g.setColor(Color.BLACK);
		g.drawString("Distribution: The software is distributed as freeware under a", xPos, yPos);
		
		g.setColor(Color.BLUE);
		g.drawString("licensing agreement",xPos, yPos+yHeight);
		licx = 350-330-10; licy = 230+yHeight; licl = 465-330-10;
		yPos += 3*yHeight;
		
		g.setColor(Color.BLACK);
		g.drawString("Email:", xPos, yPos);
		
		g.setColor(Color.BLACK);
		g.drawString("picasso@dsl.serc.iisc.ernet.in",xPos+40, yPos);
		mailx = 50; maily = 260; maily = 215;
		yPos += 2*yHeight;
		
		/*g.setColor(Color.BLACK);
		g.draw3DRect(200-40, yPos, 40, 25, true);
		g.drawString("OK", 212-40, yPos+17);
		okx = 200-40; oky = yPos; okl = 240-40;*/
	}

	public void mouseClicked(MouseEvent e) {
		//MessageUtil.CPrintToConsole("X :: " + e.getX() + " Y:: " + e.getY());
		if ( e.getX() >= okx && e.getX() <= okl && e.getY() >= oky && e.getY() <= oky+20 )
			this.dispose();
		if ( e.getX() >= picx && e.getX() <= picl && e.getY() >= picy && e.getY() <= picy+20 )
			PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html");
		if ( e.getX() >= dslx && e.getX() <= dsll && e.getY() >= dsly && e.getY() <= dsly+yHeight)
			PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in");
		if ( e.getX() >= iiscx && e.getX() <= iiscl && e.getY() >= iiscy && e.getY() <= iiscy+yHeight)
			PicassoUtil.openURL("http://www.iisc.ernet.in");
		if (e.getX() >= copyx && e.getX() <= copyl && e.getY() >= copyy && e.getY() <= copyy+yHeight)
			PicassoUtil.openURL("http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso-copyright.pdf");
		if (e.getX() >= licx && e.getX() <= licl && e.getY() >= licy && e.getY() <= licy+yHeight)
			PicassoUtil.openURL("http://bhairav.serc.iisc.ernet.in/license.htm");
		else if (e.getX() >= mailx && e.getX() <= mailx+maill && e.getY() >= maily && e.getY() <= maily+yHeight)
			try {
				Runtime.getRuntime().exec("explorer mailto:picasso@dsl.serc.iisc.ernet.in");
			}catch (Exception ex) {}
		
	}

	public void mouseMoved( MouseEvent e)
	{	
		Cursor myCursor = new Cursor(Cursor.HAND_CURSOR);
		
		if (/*( e.getX() >= okx && e.getX() <= okl && e.getY() >= oky && e.getY() <= oky+20 )
		||*/ ( e.getX() >= picx && e.getX() <= picl && e.getY() >= picy && e.getY() <= picy+20 )
		|| (e.getX() >= dslx && e.getX() <= dsll && e.getY() >= dsly && e.getY() <= dsly+yHeight)
		|| (e.getX() >= iiscx && e.getX() <= iiscl && e.getY() >= iiscy && e.getY() <= iiscy+yHeight)
		|| (e.getX() >= copyx && e.getX() <= copyl && e.getY() >= copyy && e.getY() <= copyy+yHeight)
		|| (e.getX() >= licx && e.getX() <= licl && e.getY() >= licy && e.getY() <= licy+yHeight)
		|| (e.getX() >= mailx && e.getX() <= maill && e.getY() >= maily && e.getY() <= maily+yHeight)) {
			setCursor(myCursor);
		} else
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		
	}
	
	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
}


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

import iisc.dsl.picasso.client.frame.PlanTreeFrame;
import iisc.dsl.picasso.client.print.ImageGenerator;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.client.print.PicassoPrint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;

import javax.swing.JButton;
import javax.swing.JPanel;

public class PrintTreePanel extends JPanel {
	
	private static final long serialVersionUID = 4428917937409598565L;
	PlanTreeFrame	treeFrame;
	public static double 			paperwidth, paperheight;
	int				pageIndex;
	double			scale;
	
	public PrintTreePanel(PlanTreeFrame ptf, int index, double s, double w, double h) {
		treeFrame = ptf;
		paperwidth = w;
		paperheight = h;
		pageIndex = index;
		scale = s;
		
		this.setLayout(new BorderLayout());
	}
	
	public void setPaperSettings(int index, double s, double w, double h) {
		paperwidth = w;
		paperheight = h;
		pageIndex = index;
		scale = s;
	}
	
int count = PicassoPrint.pageCount;
int dummy_index = pageIndex;
public static boolean pgFlag;
int flag =0;	
	public void paint(Graphics g) {
		//MessageUtil.CPrintToConsole("In Tree Frame Print :: " + pageIndex);

		int planNum = treeFrame.getPlanNumber(dummy_index);
		BufferedImage image = treeFrame.getTreeImage(dummy_index);
		
		int planNum1=0;
		BufferedImage image1 = image;
		//int w = image1.getWidth();
		
		if ( scale == 1.2 )
			FONT_SIZE = 10;
		else if ( scale == 1.4 )
			FONT_SIZE = 12;
		else
			FONT_SIZE = 8;
		
		g.setFont(new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, FONT_SIZE));
		g.setColor(Color.red);
		g.drawString("Picasso Database Query Optimizer Visualizer 2.1", 150, 10);
		g.drawString("Copyright \u00A9 Indian Institute of Science, Bangalore, India", 150, 20);
		g.setColor(Color.BLACK);
		g.drawLine(0, 30, (int)paperwidth, 30);
		g.setColor(Color.BLUE);
		
		
	
	
		if((2*image.getWidth())+ 100 < paperwidth && count > 1){
					
			pgFlag = true;
			pageIndex = 0;
			planNum1 = treeFrame.getPlanNumber(dummy_index + 1);
			image1 = treeFrame.getTreeImage(dummy_index + 1);
			
			int width = (int)paperwidth-10;  //(int)(image.getWidth()*0.60);
			int height = (int)paperheight-150;
			if ( paperwidth > paperheight ) {
				width = image.getWidth()*height/image.getHeight();//height = (int)paperheight-100; //(int)(image.getHeight()*0.60);
				if ( width > paperwidth ) {
					width = (int)paperwidth - 10;
				}
			} else {
				height = image.getHeight()*width/image.getWidth();//height = (int)paperheight-150; // need to change this...
				if ( height > (paperheight-150) ) {
					width = (int)paperheight - 150;
				}
			}
			
			// The scaling will be nice smooth with this filter
			AreaAveragingScaleFilter scaleFilter =
				new AreaAveragingScaleFilter(width, height);
			ImageProducer producer = new FilteredImageSource(image.getSource(),
					scaleFilter);
			ImageGenerator generator = new ImageGenerator();
			producer.startProduction(generator);
			BufferedImage scaled = generator.getImage();
			
		
			
			int width1 = (int)paperwidth-10;  //(int)(image.getWidth()*0.60);
			int height1 = (int)paperheight-150;
			if ( paperwidth > paperheight ) {
				width1 = image1.getWidth()*height1/image1.getHeight();//height = (int)paperheight-100; //(int)(image.getHeight()*0.60);
				if ( width1 > paperwidth ) {
					width1 = (int)paperwidth - 10;
				}
			} else {
				height1 = image1.getHeight()*width1/image1.getWidth();//height = (int)paperheight-150; // need to change this...
				if ( height1 > (paperheight-150) ) {
					width1 = (int)paperheight - 150;
				}
			}
			
			// The scaling will be nice smooth with this filter
			AreaAveragingScaleFilter scaleFilter1 =
				new AreaAveragingScaleFilter(width1, height1);
			ImageProducer producer1 = new FilteredImageSource(image1.getSource(),
					scaleFilter1);
			ImageGenerator generator1 = new ImageGenerator();
			producer1.startProduction(generator1);
			BufferedImage scaled1 = generator1.getImage();
				
						
			int xPos = 10;
			int yPos = 55;
	
			int x1Pos = scaled.getWidth() + 50;
			int y1Pos = 55;
			
			
			//MessageUtil.CPrintToConsole(width + "::" + height + " IMAGE " + image.getWidth() + " In Print Paint " + image.getHeight());
			g.setColor(new Color(PicassoConstants.color[planNum%PicassoConstants.color.length]));
			g.fillRect(xPos, 40, 10, 10);
			g.setColor(Color.BLUE);
			g.drawString("Plan : " + (planNum+1), xPos+20, 50);
			g.drawImage(scaled, xPos, yPos, this);
			
			g.setColor(new Color(PicassoConstants.color[planNum1%PicassoConstants.color.length]));
			g.fillRect(x1Pos, 40, 10, 10);
			g.setColor(Color.RED);
			g.drawString("Plan : " + (planNum1+1), x1Pos + 20, 50);
			g.drawImage(scaled1, x1Pos, y1Pos, this);
			
			
			yPos += (scaled.getHeight()+15);
			ClientPacket cp = treeFrame.getClientPacket();
			QueryPacket qp = cp.getQueryPacket();
			String qName = qp.getQueryName();
			g.drawString("QueryTemplate Descriptor: " + qName, xPos, yPos+15);
			
			
			String dbInfoStr = "DBInfo: " + PicassoUtil.getDBInfoString(cp.getDBSettings());
			g.drawString(dbInfoStr, xPos, yPos+30);
			
			flag=1;
			}
		else {
			PicassoPrint.addNavButtons();
			
		int planNum2 = treeFrame.getPlanNumber(pageIndex);
		BufferedImage image2 = treeFrame.getTreeImage(pageIndex);
		
		int width = (int)paperwidth-10;  //(int)(image.getWidth()*0.60);
		int height = (int)paperheight-150;
		if ( paperwidth > paperheight ) {
			width = image2.getWidth()*height/image2.getHeight();//height = (int)paperheight-100; //(int)(image.getHeight()*0.60);
			if ( width > paperwidth ) {
				width = (int)paperwidth - 10;
			}
		} else {
			height = image2.getHeight()*width/image2.getWidth();//height = (int)paperheight-150; // need to change this...
			if ( height > (paperheight-150) ) {
				width = (int)paperheight - 150;
			}
		}
		
		// The scaling will be nice smooth with this filter
		AreaAveragingScaleFilter scaleFilter =
			new AreaAveragingScaleFilter(width, height);
		ImageProducer producer = new FilteredImageSource(image2.getSource(),
				scaleFilter);
		ImageGenerator generator = new ImageGenerator();
		producer.startProduction(generator);
		BufferedImage scaled = generator.getImage();
		
		int xPos = 10;
		int yPos = 55;
		//MessageUtil.CPrintToConsole(width + "::" + height + " IMAGE " + image.getWidth() + " In Print Paint " + image.getHeight());
		g.setColor(new Color(PicassoConstants.color[planNum%PicassoConstants.color.length]));
		g.fillRect(150, 40, 10, 10);
		g.setColor(Color.BLUE);
		g.drawString("Plan : " + (planNum2+1), 170, 45);
		g.drawImage(scaled, xPos, yPos, this);
		
		yPos += (scaled.getHeight()+15);
		ClientPacket cp = treeFrame.getClientPacket();
		QueryPacket qp = cp.getQueryPacket();
		String qName = qp.getQueryName();
		g.drawString("QueryTemplate Descriptor: " + qName, xPos, yPos);
		
		
		String dbInfoStr = "DBInfo: " + PicassoUtil.getDBInfoString(cp.getDBSettings());
		g.drawString(dbInfoStr, xPos, yPos+15);
		}	
	}
	
	int FONT_SIZE = 8;
	int RECT_SIZE = 20;
}

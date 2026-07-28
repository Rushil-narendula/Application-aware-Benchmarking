
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
import iisc.dsl.picasso.client.print.ImageGenerator;
import iisc.dsl.picasso.client.util.DiagramUtil;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

import javax.swing.JPanel;

public class PrintDiagramPanel extends JPanel {

	private static final long serialVersionUID = -3071943413427429102L;

	MainPanel 		mainPanel;
	BufferedImage 	image;
	double 			paperwidth, paperheight;
	Insets			insets = new Insets(1, 1, 1, 1);
	double			scale;

	public PrintDiagramPanel(MainPanel mp, BufferedImage img, double s, double w, double h) {
		mainPanel = mp;
		image = img;
		paperwidth = w;
		paperheight = h;
		scale = s;

		this.setLayout(new BorderLayout());
		//this.add(panel, BorderLayout.CENTER);

		//createGUI(img);
	}

	public void setPaperSettings(double s, double w, double h) {
		paperwidth = w;
		paperheight = h;
		scale = s;
	}

	public void setInsets(Insets i) {
		insets = i;
	}

	public void paint(Graphics g) {

		PicassoPanel p = mainPanel.getCurrentTab();

		if ( scale == 1.2 )
			FONT_SIZE = 10;
		else if ( scale == 1.4 )
			FONT_SIZE = 12;
		else
			FONT_SIZE = 8;

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.setFont(new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, FONT_SIZE));
		g.setColor(Color.red);
		g.drawString("Picasso Database Query Optimizer Visualizer 2.1", 150, 10);
		g.drawString("Copyright \u00A9 Indian Institute of Science, Bangalore, India", 150, 20);
		g.drawString(p.getPanelString(), (int)paperwidth/2, 40);
		g.setColor(Color.BLACK);
		g.drawRect(insets.left, 30, (int)paperwidth-5-insets.right,
				(int)paperheight-35-insets.bottom);
		//g.drawLine(0, 30, (int)paperwidth, 30);
		//g.drawLine(0, 30, 0, (int)paperheight);

		g.setColor(Color.BLUE);

		if ( image == null ) {
			g.setFont(new Font("Courier", Font.PLAIN, FONT_SIZE));
			g.drawString(mainPanel.getQueryText(), 15, 35);
		} else {
			int width = (int)paperwidth-120;  //(int)(image.getWidth()*0.60);
			int height = (int)paperheight-150;
			if ( paperwidth > paperheight ) {
				width = image.getWidth()*height/image.getHeight();
				if ( width > paperwidth ) {
					width = (int)paperwidth - 120;
				}//height = (int)paperheight-120; //(int)(image.getHeight()*0.60);
			} else {
				height = image.getHeight()*width/image.getWidth();
				if ( height > (paperheight-150) ) {
					width = (int)paperheight - 150;
				}
				//height = (int)paperheight-150; // need to change this...
			}

//			The scaling will be nice smooth with this filter
			AreaAveragingScaleFilter scaleFilter =
				new AreaAveragingScaleFilter(width, height);
			ImageProducer producer = new FilteredImageSource(image.getSource(),
					scaleFilter);
			ImageGenerator generator = new ImageGenerator();
			producer.startProduction(generator);
			BufferedImage scaled = generator.getImage();

			//MessageUtil.CPrintToConsole(width + "::" + height + " IMAGE " + image.getWidth() + " In Print Paint " + image.getHeight());
			//MessageUtil.CPrintToConsole(" Scale " + scale + " width " + paperwidth + " height " + paperheight);
			int xPos = insets.left+5;
			int yPos = 45;
			g.drawImage(scaled, xPos, yPos, this);

			DiagramPacket gdp, fullgdp = null;
			int sortedPlan[][], fullsortedPlan[][] = null;

			int panelType = p.getPanelType();

			if ( p instanceof ReducedPlanPanel ) {
				gdp = mainPanel.getReducedDiagramPacket();
				fullgdp = mainPanel.getFullReducedDiagramPacket();
				sortedPlan = mainPanel.getRSortedPlan();
				fullsortedPlan = mainPanel.getFullRSortedPlan();
				//ReducedPlanPanel rpd = (ReducedPlanPanel)p;
			} else if ( panelType == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM
					|| panelType == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) {
				fullgdp = mainPanel.getFullExecDiagramPacket();
				fullsortedPlan = mainPanel.getFullExecSortedPlan();
				gdp = mainPanel.getExecDiagramPacket();
				sortedPlan = mainPanel.getExecSortedPlan();
			} else {
				gdp = mainPanel.getDiagramPacket();
				fullgdp = mainPanel.getFullDiagramPacket();
				sortedPlan = mainPanel.getSortedPlan();
				fullsortedPlan = mainPanel.getFullSortedPlan();
			}

			String[] infoStr = drawLegend(g, panelType, gdp, sortedPlan, fullgdp, fullsortedPlan);
			int yAdd = 15;
			if ( paperheight > paperwidth ) {// In Potrait Mode
				yPos += (height+60);
				yAdd = 30;
			} else {
				yPos += (height+10);
			}
			g.setColor(Color.blue);

			g.drawString("QueryTemplate Descriptor: " + mainPanel.getServerPacket().queryPacket.getQueryName(), xPos, yPos);
			//g.drawString("Plans : " + gdp.getMaxPlan(), (int)paperwidth-170, yPos);

			if ( p instanceof ReducedPlanPanel ) {
				g.drawString(((ReducedPlanPanel)p).getInfoStr(), xPos, yPos+yAdd);
			} else {
				g.drawString(infoStr[0], xPos, yPos+yAdd);
				g.drawString(infoStr[1], xPos, yPos+(yAdd*2));
			}

			DataBaseSettingsPanel dbs = mainPanel.getDBSettingsPanel();
			ResolutionFrame rf = mainPanel.getResFrame();
			String dbStr = "DBInfo: " + dbs.getDbInfoStr();
			g.drawString(dbStr, xPos, yPos+(yAdd*3));
			String PlanDiffStrnew;
			if(p instanceof PlanPanel)
			{
				if(((PlanPanel)p).planDiff.getText().equals(((PlanPanel)p).diffLevel1))
				{
						PlanDiffStrnew = "PARAMETER";
				}
				else
				{
						PlanDiffStrnew = "OPERATOR";
				}
					
			}
			else
			{
				if(dbs.getPlanDiffStr().equals(PicassoConstants.SUBOPERATORLEVEL))
				{
					PlanDiffStrnew = "PARAMETER";
				}
				else
				{
					PlanDiffStrnew = "OPERATOR";
				}
			}
			dbStr="{Resolution, StartPoint, EndPoint}: ";
			for (int i=0;i<PicassoConstants.NUM_DIMS;i++)									//rss
			{
				dbStr = dbStr+ " Dim. " +i+": {" + gdp.getResolution(i) +", " + gdp.getQueryPacket().getStartPoint(i)*100 + ", " + gdp.getQueryPacket().getEndPoint(i)*100 + "}";		//rss
				if(i < PicassoConstants.NUM_DIMS - 1)
					dbStr =dbStr + "  |  ";
			}
			/*for (int i=0;i<PicassoConstants.NUM_DIMS;i++)									//rss
				dbStr = dbStr+" Higher Range "+ i+": " + gdp.getQueryPacket().getEndPoint(i)*100;	
			dbStr =dbStr + "  |  ";
			for (int i=0;i<PicassoConstants.NUM_DIMS;i++)									//rss
				dbStr = dbStr+" Lower Range "+ i +": "+ gdp.getQueryPacket().getStartPoint(i)*100;*/
			g.drawString(dbStr, xPos, yPos+(yAdd*4));
			dbStr="";
			dbStr=dbStr	+ "Optimization: " + dbs.getOptStr()		//rss
			+ ", Plan-Diff: " + PlanDiffStrnew
			+ ", Selectivity: " + dbs.getSelecStr()
			+ ", Distribution: " + dbs.getDistribution();
			g.drawString(dbStr, xPos, yPos+(yAdd*5));
		}
		//MessageUtil.CPrintToConsole("Done Print Paint");
	}

	int FONT_SIZE = 8;
	int RECT_SIZE = 20;

	public String[] drawLegend(Graphics g, int panelType, DiagramPacket gdp, int[][] sortedPlan, DiagramPacket fullgdp, int [][]fullsortedPlan) {
		double[] values = null;
		double[] globalvalues =null;
		
		if(!gdp.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
		{
			values = DiagramUtil.getMinAndMaxValues(gdp);
			globalvalues = DiagramUtil.getMinAndMaxValues(fullgdp);
		}
		else
		{
			values = DiagramUtil.getMinAndMaxValues(gdp);
			globalvalues = DiagramUtil.getMinAndMaxValues(fullgdp);
		}
		
		DecimalFormat df = new DecimalFormat("0.00E0");
		df.setMaximumFractionDigits(2);

		String[] infoStr = new String[2];
		infoStr[0] = "# of Plans: " + new Double(values[4]).intValue();

		// TODO: What to do for exec diagrams? theres not enough space to put all the info for >=3D diagrams. 
		// leaving it as it was before for now
		if ( panelType == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM || panelType == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) {
			infoStr[0] += ", Min Act Time(s): " + df.format(values[0]);
			infoStr[0] += ", Max Act Time(s): " + df.format(values[1]);
			infoStr[1] = "Min Act Card: " + df.format(values[2]);
			infoStr[1] += ", Max Act Card: " + df.format(values[3]);
		} else {
			infoStr[0] += ", Min Est Cost: " + df.format(values[0]);
			infoStr[0] += ", Max Est Cost: " + df.format(values[1]);
			infoStr[1] = "Min Est Card: " + df.format(values[2]);
			infoStr[1] += ", Max Est Card: " + df.format(values[3]);
		}

		if ( gdp == null || sortedPlan == null || fullsortedPlan == null || fullgdp == null) {
			return null;
		}

		int legX = (int)paperwidth - 120;
		int legY = 50;

		int maxPlans = fullgdp.getMaxPlanNumber();
		int[] yPlanPos = new int[maxPlans];

		int yPos = legY;
		int xPos = legX;
		int rPos = legX;
		int total = gdp.getData().length;
		int globaltotal = fullgdp.getData().length;
		int rectSize = RECT_SIZE;

		boolean scaleupflag = false;
		for(int i = 0; i < gdp.getDimension(); i++)
		{
			if(gdp.getQueryPacket().getEndPoint(i) - gdp.getQueryPacket().getStartPoint(i) < 0.05 && gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
				scaleupflag = true;
		}
		
		int numlocplans = 0;
		int loctotal = 0;
		for(int i = 0; i < sortedPlan[1].length; i++)
		{
			if(sortedPlan[1][i] != 0)
				numlocplans++;
			if(scaleupflag)
				loctotal += (sortedPlan[1][i]/100);
			else
				loctotal += (sortedPlan[1][i]);
		}
		
		if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
		{
			globaltotal = 0;
			for(int i = 0; i < fullsortedPlan[1].length; i++)
			{
				if(!scaleupflag)
					globaltotal += fullsortedPlan[1][i];
				else
					globaltotal += (fullsortedPlan[1][i]/Math.pow(10, gdp.getDimension()));
			}
		}
		
		g.setFont (new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, FONT_SIZE));
		g.setColor(Color.GRAY);
		if(gdp.getDimension() >= 3)
		{
			g.drawString("               G(%)      L(%)", xPos, yPos);
			g.drawString("\n", xPos, yPos+2);
		}
		
		for (int i=0; i < 10; i++) 
		{
			if ( i >= maxPlans )
				break;

			int value = sortedPlan[1][fullsortedPlan[2][i]];
			int globalvalue = fullsortedPlan[1][fullsortedPlan[2][i]];
//			apexp
			double areasum = 1;
			double volsum = 1;
			double value2=0, value3=0;
			if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION)) 
			{
				/*value2=value/total;
				value3=globalvalue/globaltotal;*/
				areasum = 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
				if(gdp.getDimension()>1)
					areasum *= 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
				
				for(int j = 0; j < gdp.getDimension(); j++)
				{
					volsum *= 100*(gdp.getQueryPacket().getEndPoint(j) - gdp.getQueryPacket().getStartPoint(j));
				}
				
				if(scaleupflag)
				{
					areasum *= 100;
					volsum *= Math.pow(10, gdp.getDimension());
				}
				
				// value2 corresponds to the global value. value3 to the local.
				if(gdp.getDimension() ==1)
				{
					value2=(double)value/areasum;
					value3=(double)globalvalue/volsum;
				}
				else /*if(gdp.getDimension() ==2)*/
				{
					/*value2=(double)localvalue/10000;
					value3=(double)globalvalue/1000000;*/
					value2=(double)value/areasum;
					value3=(double)globalvalue/volsum;
				}
			}
			//end apexp
			yPlanPos[i] = yPos;
			//apexp
			//the below code tests whether for the machine precision,
			//the value will be 0 (regardless of accuracy specified.
			/*if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
			{
				//end apexp
				if ( ((value * 100.0)/total) == 0 )
					continue;
				//apexp
			}*/
			//end apexp

			Color c = new Color(PicassoConstants.color[i%PicassoConstants.color.length]);//Constants.color[pos]);

			// in reduced plan : rSortedPlan[2][i] gives plan #
			// sortedPlan[0][plan #] = count (Color(i))
			int displayPlanNumber = i+1;
			if ( mainPanel.getCurrentTab() instanceof ReducedPlanPanel ) {
				c = mainPanel.getReducedPlanColor(i);
				displayPlanNumber = mainPanel.getOriginalPlanNumber(i)+1;
			}

			int accLen = 15;
			String accu;
			int[] resolution = new int [gdp.getDimension()]; 
			int maxres =gdp.getMaxResolution();

			for(int k=0;k<gdp.getDimension();k++)
				resolution[k]=gdp.getResolution(k);
			//commented by apexp//String accu = PicassoUtil.getAccuracy(sortedPlan[1][sortedPlan[2][i]], total, gdp.getResolution(), accLen);
			if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
			{
				accu = PicassoUtil.getAccuracy(globalvalue, globaltotal, maxres, accLen);
			}
			else
			{
				accu = PicassoUtil.getAccuracy2(value3, globaltotal, maxres, accLen);
			}
			
			String laccu;
			if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
			{
				laccu = PicassoUtil.getAccuracy(value, loctotal, maxres, accLen);
			}
			else
			{
				laccu = PicassoUtil.getAccuracy2(value2, loctotal, maxres, accLen);
			}
			
			g.setColor(c);
			g.fillRect(rPos, yPos, rectSize, rectSize);
			yPlanPos[i] = yPos;
			
			g.setColor(Color.black);
			StringTokenizer st = new StringTokenizer(accu, "%");
			
			String accu2="";
			if(st.hasMoreTokens())
				accu2 = st.nextToken();
			if(numlocplans == 1 && Double.parseDouble(accu2) < 100.0)
				accu = "  100.000%";
			
			StringTokenizer st2 = new StringTokenizer(laccu, "%");
			String accu3="";
			if(st2.hasMoreTokens())
				accu3 = st2.nextToken();
			if(maxPlans == 1 && Double.parseDouble(accu3) < 100.0)
				laccu = "  100.000%";
			
			df = new DecimalFormat("0.000");
			df.setMaximumFractionDigits(3);
			String glob="";
			
			if(gdp.getQueryPacket().getDimension() > 2)
			{
				if(accu3.trim().equals("0.00"))
					g.drawString("P"+(displayPlanNumber)+accu2 + "   --- ", xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
				else
					g.drawString("P"+(displayPlanNumber)+accu2 + "  [" + accu3.trim()+ "]", xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
			}
			else
				g.drawString("P"+(displayPlanNumber)+laccu, xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
			
			yPos += (5+RECT_SIZE);
			//planCount[pos] = -1;
		}
		if ( maxPlans > 10 ) {
			g.drawString("...", xPos, yPos+RECT_SIZE-6);
			yPos += (5+RECT_SIZE);
			g.setColor(Color.black);
			//commented by apexp: String accu = PicassoUtil.getAccuracy(sortedPlan[1][sortedPlan[2][maxPlans-1]], total, gdp.getResolution(), 14);
			//apexp
			String accu,accu2;
			if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
			{
				accu = PicassoUtil.getAccuracy(sortedPlan[1][fullsortedPlan[2][maxPlans-1]], total, gdp.getMaxResolution(), 14);//rss
				accu2 = PicassoUtil.getAccuracy(fullsortedPlan[1][fullsortedPlan[2][maxPlans-1]], globaltotal, gdp.getMaxResolution(), 14);//rss
			}
			else
			{
				//if(gdp.getDimension()<=2)
					//accu = PicassoUtil.getAccuracy2(sortedPlan[1][sortedPlan[2][maxPlans-1]]/Math.pow(100, gdp.getDimension()+1), total, gdp.getMaxResolution(), 14);//rss
				accu = PicassoUtil.getAccuracy(sortedPlan[1][fullsortedPlan[2][maxPlans-1]], total, gdp.getMaxResolution(), 14);//rss
				accu2 = PicassoUtil.getAccuracy(fullsortedPlan[1][fullsortedPlan[2][maxPlans-1]], globaltotal, gdp.getMaxResolution(), 14);//rss
					/*
				else
				{
					double value2 = sortedPlan[1][sortedPlan[2][maxPlans-1]]/(1000000.0);
						int q=1; //gdp.getDimension()-2;
						while(q>0)
						{
							value2/=gdp.getResolution();
							q--;
						}
					accu = PicassoUtil.getAccuracy2( value2, total, gdp.getResolution(), 14);
				}*/
			}
			//end apexp
			Color c = new Color(PicassoConstants.color[(maxPlans-1)%PicassoConstants.color.length]);
			g.setColor(c);
			g.fillRect(rPos, yPos, rectSize, rectSize);
			g.setColor(Color.black);
			// g.drawString("P"+(maxPlans)+accu, xPos, yPos+RECT_SIZE-6);
			if(gdp.getDimension() < 3)
				g.drawString("P"+(maxPlans)+accu, xPos, yPos+RECT_SIZE-6);
			else
			{
				if(accu.trim().equals("0.00"))
					g.drawString("P"+(maxPlans)+accu2 + "      ---", xPos, yPos+RECT_SIZE-6);
				else
					g.drawString("P"+(maxPlans)+accu2 + "    " + accu.trim(), xPos, yPos+RECT_SIZE-6);
			}
		}
		return infoStr;
	}
}

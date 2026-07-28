
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
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

public class LegendPanel extends JPanel { //implements ActionListener {
	
	private static final long serialVersionUID = 5528463892274232069L;
	
	int 			panelType;
	int 			maxPlans, maxCost, maxCardinality;
	MainPanel		parent;
	
	JProgressBar	progressBar;
	DrawingPane 	drawingPane;
	int 			realMaxPlans;
	int				rectanglePos=0;
	int[]			yPlanPos=null;
	int 			firstPlanNum=-1, secondPlanNum=-1;
	int				maxUnitIncrement = 1;
	int				LEGEND_WIDTH = 200;
	JScrollPane 	scroller;
	Hashtable rep_Points = null;
    DiagramPacket gdp = null;
    static boolean firstflag = true;
    
	public LegendPanel(MainPanel p, int type) {
		parent = p;
		panelType = type;
		
		//realMaxPlans=parent.getFullDiagramPacket().getMaxPlanNumber();
		//System.out.println("RealMaxPlans "+realMaxPlans);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1,1));
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		setLayout(new BorderLayout());
		drawingPane = new DrawingPane();
		
		//Put the drawing area in a scroll pane.
		scroller = new JScrollPane(drawingPane);
		scroller.setPreferredSize(new Dimension(LEGEND_WIDTH, drawingPane.getHeight()+100));
		scroller.setAutoscrolls(true);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		add(buttonPanel, BorderLayout.NORTH);
		add(scroller, BorderLayout.CENTER);
		add(progressBar, BorderLayout.SOUTH);
	}
	
	public void setProgressBar(int value) {
		progressBar.setValue(value);
	}
	
	int rectSize = PicassoConstants.LEGEND_SIZE;
	int legendGap = 5;
	public void setLegendPreferredSize(int maxPlans) 
	{
		System.out.println("MAX PLANS IN LEGEND: "+maxPlans);
		int legendHeight = (rectSize+legendGap)*maxPlans + (PicassoConstants.LEGEND_SIZE*2);
		drawingPane.setPreferredSize(new Dimension(LEGEND_WIDTH, legendHeight));
	}
	
	boolean emptyLegend = true;
	public void setEmptyLegend(boolean val) 
	{
		emptyLegend = val;
	}
	
	int height = PicassoConstants.LEGEND_HEIGHT;
	//int height = (rectSize+legendGap)*realMaxPlans + (PicassoConstants.LEGEND_SIZE*2);
	public void resetScrollBar() 
	{
		// scroller.setVisible(true);
		scroller.getVerticalScrollBar().setValue(0);
		scroller.getHorizontalScrollBar().setValue(0);
//		System.out.println("MaxPlans reset are :" +realMaxPlans);
//		height = (rectSize+legendGap)*realMaxPlans + (PicassoConstants.LEGEND_SIZE*2);
		this.setPreferredSize(new Dimension(LEGEND_WIDTH, height));
	}
	
	public void emptyPanel() 
	{
		setEmptyLegend(true);
		drawingPane.repaint();
		scroller.getVerticalScrollBar().setValue(0);
		scroller.getHorizontalScrollBar().setValue(0);
	}
	
	/** The component inside the scroll pane. */
	public class DrawingPane extends JPanel	implements MouseListener 
	{
		private static final long serialVersionUID = -7796716099459482983L;
		
		boolean firstTime = true;
		
		public DrawingPane() 
		{
			this.addMouseListener(this);
		}
		
		// check this function below --ma
		protected void paintComponent(Graphics g) 
		{
			if(!firstflag)
			{
				DiagramPacket gdp = null;
				super.paintComponent(g);
				
				if ( parent.getCurrentTab() instanceof ReducedPlanPanel ) 
				{
	                gdp = parent.getReducedDiagramPacket();
					drawLegend(parent.getFullReducedDiagramPacket(), parent.getFullRSortedPlan(), parent.getRSortedPlan(), g);
				}
				else if ( parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM 
						 || parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) 
				{
					gdp = parent.getExecDiagramPacket();
					// TODO: make use of the new drawlegend function even for exec diagrams.
					drawLegend(parent.getFullExecDiagramPacket(), parent.getFullExecSortedPlan(), parent.getExecSortedPlan(), g);
				} 
				else if ( ! (parent.getCurrentTab() instanceof SelectivityPanel))
				{
					gdp = parent.getFullDiagramPacket();
					drawLegend(gdp, parent.getFullSortedPlan(), parent.getSortedPlan(), g);
				}
			}
			else
			{
				DiagramPacket gdp = null;
				super.paintComponent(g);
				
				//MessageUtil.CPrintToConsole("Current Tab :: " + parent.getCurrentTab());
				if ( parent.getCurrentTab() instanceof ReducedPlanPanel ) 
				{
	                gdp = parent.getReducedDiagramPacket();
					drawLegend(parent.getReducedDiagramPacket(), parent.getRSortedPlan(), g);
				} 
				else if ( parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM 
						 || parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) 
				{
					gdp = parent.getExecDiagramPacket();
					drawLegend(parent.getExecDiagramPacket(), parent.getExecSortedPlan(), g);
				} 
				else if ( ! (parent.getCurrentTab() instanceof SelectivityPanel))
				{
					gdp = parent.getDiagramPacket();
					drawLegend(gdp, parent.getSortedPlan(), g);
				}
				firstflag = false;
			}
				
		}
		
		/*protected void paintComponent(Graphics g) {
			//MessageUtil.CPrintToConsole("In LEGEND Paint");
			
			DiagramPacket gdp = null;
			super.paintComponent(g);
			
			//MessageUtil.CPrintToConsole("Current Tab :: " + parent.getCurrentTab());
			if ( parent.getCurrentTab() instanceof ReducedPlanPanel ) 
			{
                gdp = parent.getReducedDiagramPacket();
				drawLegend(parent.getReducedDiagramPacket(), parent.getRSortedPlan(), g);
			} 
			else if ( parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM 
					 || parent.getCurrentTab().panelType == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) 
			{
				gdp = parent.getExecDiagramPacket();
				drawLegend(parent.getExecDiagramPacket(), parent.getExecSortedPlan(), g);
			} 
			else if ( ! (parent.getCurrentTab() instanceof SelectivityPanel))
			{
				gdp = parent.getDiagramPacket();
				drawLegend(gdp, parent.getSortedPlan(), g);
			}
		}*/
		
		public void drawLegend(DiagramPacket i_gdp,int [][]fullsortedplan, int[][] sortedPlan, Graphics g) 
		{
			int maxPlans = 0;
			
			// resetScrollBar();
			// setVisible(true);
			gdp = i_gdp;
			if ( gdp == null || sortedPlan == null ) {
				return;
			}
			realMaxPlans=gdp.getMaxPlanNumber();
			if(fullsortedplan == null)
				return;
			if(gdp!=null)
			{
				maxPlans = gdp.getMaxPlanNumber();
				rep_Points =  new Hashtable();
				DataValues[] dataValues = gdp.getData();
				for(int i=0;i<dataValues.length;i++) 
				{
					if(rep_Points.get(new Integer(dataValues[i].getPlanNumber()))==null)
						rep_Points.put(new Integer(dataValues[i].getPlanNumber()),new Integer(i));
				}
			}
			//height = (rectSize+legendGap)*realMaxPlans + (PicassoConstants.LEGEND_SIZE*2);
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
			
			double sumofperc=0.0;
			int planstocover80=-1;
			int mymaxplan;
			
//			height = (rectSize+legendGap)*realMaxPlans*4 + (PicassoConstants.LEGEND_SIZE*2);
//			System.out.println("MaxPlans drawlegend are :" +realMaxPlans+" "+height);

			g.drawLine(0, height, 0, height+4);
			this.setPreferredSize(new Dimension(LEGEND_WIDTH, height));
			
			if ( gdp == null && (sortedPlan == null || fullsortedplan == null)) {
				return;
			}
			
			if ( emptyLegend ) {
				setBackground(this.getBackground());
				return;
			}
			
			int legX = 0; //this.getX();
			int legY = 0;
			
			int xi=0;
			maxPlans=0;
			for (xi=0; xi < fullsortedplan[1].length; xi++)
			{ 
				if(fullsortedplan[1][xi] != 0)
					maxPlans++;
			}
			
			yPlanPos = new int[maxPlans];
			int yPos = PicassoConstants.LEGEND_MARGIN_Y + legY;
			int xPos = PicassoConstants.LEGEND_MARGIN_X + rectSize + 5 + legX;
			int rPos = rectanglePos = PicassoConstants.LEGEND_MARGIN_X + legX;
			int total = 0;
			
			if(gdp != null)
				total=gdp.getData().length;
			else return;
			
			if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
			{
				total = 0;
				for(int i = 0; i < fullsortedplan[1].length; i++)
					if(!scaleupflag)
						total += fullsortedplan[1][i];
					else
						total += (fullsortedplan[1][i]/Math.pow(10, gdp.getDimension()));
			}
			
			DecimalFormat df = new DecimalFormat("0.00");
			df.setMaximumFractionDigits(2);
			
			g.setFont (new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, PicassoConstants.LEGEND_FONT_SIZE));
			double gini = PicassoUtil.computeGiniIndex(fullsortedplan, maxPlans, total, gdp, parent, false);
			double localgini = PicassoUtil.computeGiniIndex(sortedPlan, numlocplans, loctotal, gdp, parent, true);
			
			/*// remove this patchy code.
			if(gini < 0.0)
				gini = Math.round(gini);
			if(gini == -1.0)
				gini = 1;
			
			if(localgini < 0.0)
				localgini = Math.round(localgini);
			if(localgini == -1.0)
				localgini = 1;*/
			
			if(gdp.getDimension() > 2)
			{
				yPos = 0;
				g.setColor(Color.GRAY);
				g.drawString("            G(%)   L(%)     ", xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
				yPos += 40;
			}
			mymaxplan = maxPlans;
			
			g.setColor(Color.BLUE);
			if(gdp.getDimension() > 2)
				g.drawString("Gini Coeff: " + df.format(gini) + "  [" + df.format(localgini) + "] ", rPos, yPos);
			else
				g.drawString("Gini Coeff: " + df.format(localgini), rPos, yPos);
			
			yPos += 10;
			
			for (int i=0; i < maxPlans; i++) 
			{
				int localvalue = sortedPlan[1][fullsortedplan[2][i]];
				int globalvalue = fullsortedplan[1][fullsortedplan[2][i]];
				
				double areasum = 1;
				double volsum = 1;
				
				
				double value2=0;
				double value3=0;
				yPlanPos[i] = yPos;
				
				if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION)) 
				{
					// areasum is the slice (local) area
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
						value2=(double)localvalue/areasum;
						value3=(double)globalvalue/volsum;
					}
					else /*if(gdp.getDimension() ==2)*/
					{
						/*value2=(double)localvalue/10000;
						value3=(double)globalvalue/1000000;*/
						value2=(double)localvalue/areasum;
						value3=(double)globalvalue/volsum;
					}
					/*else if(gdp.getDimension() ==3)
					{
						value2=(double)localvalue/10000;
						value3=(double)globalvalue/1000000;
						value2=100*(double)localvalue/areasum;
						value3=100*(double)globalvalue/volsum;
					}
					else if(gdp.getDimension() ==4)
					{
						value2=(double)localvalue/10000;
						value3=(double)globalvalue/1000000;
						value2=100*(double)localvalue/areasum;
						value3=100*(double)globalvalue/volsum;
					}*/
				}

				//the below code tests whether for the machine precision,
				//the value will be 0 (regardless of accuracy specified.
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					if (((localvalue * 100.0)/total) == 0 )
					{
						mymaxplan--;
						// continue;
					}
				}
				else
				{
					if(localvalue==0) 
					{
						mymaxplan--;
						// continue;
					}
				}
				
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					if (((globalvalue * 100.0)/total) == 0 )
					{
						mymaxplan--;
						// continue;
					}
				}
				else
				{
					if(globalvalue==0) 
					{
						mymaxplan--;
						// continue;
					}
				}
				
				Color c = new Color(PicassoConstants.color[i%PicassoConstants.color.length]);
				
				// in reduced plan : rSortedPlan[2][i] gives plan #
				// sortedPlan[0][plan #] = count (Color(i))
				int displayPlanNumber = i+1;
				if ( parent.getCurrentTab() instanceof ReducedPlanPanel ) 
				{
					c = parent.getReducedPlanColor(i);
					displayPlanNumber = parent.getOriginalPlanNumber(i)+1;
				}
				
				int accLen = 15;
				if ( displayPlanNumber > 9 )
					accLen = 13;
				//value is the same as sortedPlan[1][...] above, just reusing
				//this is the "   45.00%" etc.
				String accu;
				int[] resolution = new int [gdp.getDimension()]; 
				int maxres =gdp.getMaxResolution();

				for(int k=0;k<gdp.getDimension();k++)
					resolution[k]=gdp.getResolution(k);
				
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					accu = PicassoUtil.getAccuracy(globalvalue, total, maxres, accLen);
					sumofperc+=(globalvalue * 100.0)/total;
					
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
				}
				else
				{
					accu = PicassoUtil.getAccuracy2(value3, total, maxres, accLen);
					sumofperc+=value3;
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
				}
				
				String laccu;
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					laccu = PicassoUtil.getAccuracy(localvalue, loctotal, maxres, accLen);
					sumofperc+=(localvalue * 100.0)/loctotal;
					
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
				}
				else
				{
					laccu = PicassoUtil.getAccuracy2(value2, loctotal, maxres, accLen);
					sumofperc+=value2;
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
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
				yPos += (legendGap+PicassoConstants.LEGEND_SIZE);
			}
			//height = (rectSize+legendGap)*gdp.getMaxPlanNumber() + (PicassoConstants.LEGEND_SIZE*2);
			if ( maxPlans != 0 ) {
				int panelHeight = yPos+rectSize+PicassoConstants.LEGEND_SIZE;
				if ( panelHeight > height )
					this.setPreferredSize(new Dimension(LEGEND_WIDTH, panelHeight+200));
				else
					this.setPreferredSize(new Dimension(LEGEND_WIDTH, height));
			}
		}
		public void drawLegend(DiagramPacket i_gdp, int[][] sortedPlan, Graphics g) 
		{
			// old version of this function. draws local legend with global percentages in parenthesis
			// retained for now. has to be eliminated.
			int maxPlans = 0;
			gdp = i_gdp;
			if(gdp!=null)
			{
				maxPlans = gdp.getMaxPlanNumber();
				realMaxPlans=gdp.getMaxPlanNumber();
				rep_Points =  new Hashtable();
				DataValues[] dataValues = gdp.getData();
				for(int i=0;i<dataValues.length;i++) 
				{
					if(rep_Points.get(new Integer(dataValues[i].getPlanNumber()))==null)
						rep_Points.put(new Integer(dataValues[i].getPlanNumber()),new Integer(i));
				}
			}
//			height = (rectSize+legendGap)*realMaxPlans*4 + (PicassoConstants.LEGEND_SIZE*2);
//			System.out.println("MaxPlans drawlegend 2 are :" +realMaxPlans+" "+height);


			double sumofperc=0.0;
			int planstocover80=-1;
			int mymaxplan;
			g.drawLine(0, height, 0, height+4);
			this.setPreferredSize(new Dimension(LEGEND_WIDTH, height));
			
			if ( gdp == null && sortedPlan == null ) {
				return;
			}
			
			if ( emptyLegend ) {
				setBackground(this.getBackground());
				return;
			}
			
			int legX = 0; //this.getX();
			int legY = 0;
			
			int xi;
			for (xi=maxPlans-1; xi >0; xi--)
			{ int value = sortedPlan[1][sortedPlan[2][xi]];
				if(value!=0) break;
			}
			maxPlans = xi+1;
			
			yPlanPos = new int[maxPlans];
			int yPos = PicassoConstants.LEGEND_MARGIN_Y + legY;
			int xPos = PicassoConstants.LEGEND_MARGIN_X + rectSize + 5 + legX;
			int rPos = rectanglePos = PicassoConstants.LEGEND_MARGIN_X + legX;
			int total = 0;
			if(gdp != null)
					total=gdp.getData().length;
			else return;
			
			DecimalFormat df = new DecimalFormat("0.00");
			df.setMaximumFractionDigits(2);
			
			g.setFont (new Font(PicassoConstants.LEGEND_FONT, Font.BOLD, PicassoConstants.LEGEND_FONT_SIZE));
			double gini = PicassoUtil.computeGiniIndex(sortedPlan, maxPlans, total,gdp,parent, false);
			
			if(gini < 0.0)
				gini = Math.round(gini);
			if(gini == -1.0)
				gini = 1;
			
			g.setColor(Color.BLUE);
			g.drawString("Gini Coeff: " + df.format(gini), rPos, yPos);
			yPos += 15;
			mymaxplan = maxPlans;
			
			for (int i=0; i < maxPlans; i++) 
			{
				int value = sortedPlan[1][sortedPlan[2][i]];
				int areasum = 0;
				
				for(int j = 0; j < maxPlans; j++)
					areasum += sortedPlan[1][j];
				
				double value2=0;
				yPlanPos[i] = yPos;
				
				if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION)) 
				{
					if(gdp.getDimension() ==1)
					{
						value2=(double)value/100;
					}
					else if(gdp.getDimension() ==2)
					{
						value2=(double)value/10000;
					}
					else if(gdp.getDimension() ==3)
					{
						value2=(double)value/10000;
					}
					if(gdp.getDimension() ==4)
					{
						value2=(double)value/10000;
					}
				}

				//the below code tests whether for the machine precision,
				//the value will be 0 (regardless of accuracy specified.
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					if (((value * 100.0)/total) == 0 )
					{
						mymaxplan--;
						continue;
					}
				}
				else
				{
					if(value==0) 
					{
						mymaxplan--;
						continue;
					}
				}
				
				Color c = new Color(PicassoConstants.color[i%PicassoConstants.color.length]);
				
				// in reduced plan : rSortedPlan[2][i] gives plan #
				// sortedPlan[0][plan #] = count (Color(i))
				int displayPlanNumber = i+1;
				if ( parent.getCurrentTab() instanceof ReducedPlanPanel ) 
				{
					c = parent.getReducedPlanColor(i);
					displayPlanNumber = parent.getOriginalPlanNumber(i)+1;
				}
				
				int accLen = 15;
				if ( displayPlanNumber > 9 )
					accLen = 13;
				//value is the same as sortedPlan[1][...] above, just reusing
				//this is the "   45.00%" etc.
				String accu;
				int[] resolution = new int [gdp.getDimension()]; 
				int maxres =gdp.getMaxResolution();

				for(int k=0;k<gdp.getDimension();k++)
					resolution[k]=gdp.getResolution(k);
				if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					accu = PicassoUtil.getAccuracy(value, total, maxres, accLen);
					sumofperc+=(value * 100.0)/total;
					
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
				}
				else
				{
					accu = PicassoUtil.getAccuracy2(value2, total, maxres, accLen);
					sumofperc+=value2;
					if(sumofperc>=80.0)
					{
						planstocover80=i+1;
						sumofperc=-1000; //so that it doesn't make planstocover80 change in the future
					}
				}
				
				g.setColor(c);
				g.fillRect(rPos, yPos, rectSize, rectSize);
				yPlanPos[i] = yPos;
				
				g.setColor(Color.black);
				StringTokenizer st = new StringTokenizer(accu, "%");
				
				String accu2="";
				if(st.hasMoreTokens())
					accu2 = st.nextToken();
				if(maxPlans == 1 && Double.parseDouble(accu2) < 100.0)
					accu = "  100.000%";
				
				df = new DecimalFormat("0.000");
				df.setMaximumFractionDigits(3);
				String glob="";
				if(gdp.getQueryPacket().getDimension() > 2)
					glob = df.format(PicassoConstants.sgpercs[i]*100) + "%";
				if(gdp.getQueryPacket().getDimension() > 2)
					g.drawString("P"+(displayPlanNumber)+accu + " (" + glob +")", xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
				else
					g.drawString("P"+(displayPlanNumber)+accu, xPos, yPos+PicassoConstants.LEGEND_SIZE-6);
					yPos += (legendGap+PicassoConstants.LEGEND_SIZE);
			}
			//height = (rectSize+legendGap)*gdp.getMaxPlanNumber() + (PicassoConstants.LEGEND_SIZE*2);
			if ( maxPlans != 0 ) {
				int panelHeight = yPos+rectSize+PicassoConstants.LEGEND_SIZE;
				if ( panelHeight > height )
					this.setPreferredSize(new Dimension(LEGEND_WIDTH, panelHeight+200));
				else
					this.setPreferredSize(new Dimension(LEGEND_WIDTH, height));
			}
		}
		public void mouseDragged(MouseEvent arg0) {
		}
		
		public void mouseMoved(MouseEvent arg0) {
		}
		
		public void mouseClicked(MouseEvent e) {
		}
		
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseExited(MouseEvent e) {}
		
		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			
			firstPlanNum = checkRange(x, y);
		}
		
		public void mouseReleased(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			secondPlanNum = checkRange(x, y);
			int mods = e.getModifiers();
			if (e.getButton() == MouseEvent.BUTTON3 && (mods & InputEvent.ALT_MASK) != 0 && PicassoConstants.ENABLE_COST_MODEL) 
			{
				String dbVendor = parent.getDBSettingsPanel().getCurrentDBSettings().getDbVendor();
				if (!(dbVendor.equalsIgnoreCase(DBConstants.MSSQL)) && !(dbVendor.equalsIgnoreCase(DBConstants.SYBASE))) 
				{
					JOptionPane.showMessageDialog(parent,"The Abstract Plan function is supported only for MS SQLServer 2005 and Sybase 15","Function Not Supported",JOptionPane.INFORMATION_MESSAGE);
				} 
				else
				{
					int resolution [] = new int [gdp.getDimension()];
					for(int i=0;i<gdp.getDimension();i++)
						resolution[i] = gdp.getResolution(i);
					Integer tmp_point = (Integer)rep_Points.get(new Integer(secondPlanNum));
					int rep_point=0;
					if(tmp_point!=null)
						rep_point=tmp_point.intValue();
					int yselec = rep_point/resolution[1];
					int xselec = rep_point - (yselec*resolution[0]);
					float[] pselec = gdp.getPicassoSelectivity();
					double xDSelec = pselec[xselec];
					double yDSelec = pselec[yselec];
					String infoStr[] = {"","","",""};
					PicassoUtil.getAbsPlan(parent, parent.getCurrentTab(), infoStr, resolution[0], xDSelec, yDSelec, secondPlanNum);
				}
			}
			else if ( firstPlanNum != -1 && secondPlanNum != -1) 
			{
				if ( firstPlanNum == secondPlanNum )
					PicassoUtil.displayTree(parent, parent.getCurrentTab(), firstPlanNum+"");
				else	
					PicassoUtil.displayTree(parent, parent.getCurrentTab(), firstPlanNum+","+secondPlanNum);
			}
			firstPlanNum = secondPlanNum = -1;
		}
		
		int checkRange(int x, int y) 
		{
			if(yPlanPos == null)
				return -1;
			int numOfPlans = yPlanPos.length;
			int[][] sPlans = parent.getSortedPlan();
			if ( parent.getCurrentTab() instanceof ReducedPlanPanel )
				sPlans = parent.getRSortedPlan();
			
			if ( sPlans== null || y < yPlanPos[0] || y > yPlanPos[numOfPlans-1]+PicassoConstants.LEGEND_SIZE) {
				return -1;
			}
			
			int planNum = -1;
			if ( x >= rectanglePos && x < rectanglePos+PicassoConstants.LEGEND_SIZE) {
				for (int i=0; i < yPlanPos.length; i++) {
					if ( y > yPlanPos[i] && y < yPlanPos[i]+PicassoConstants.LEGEND_SIZE) {
						planNum = sPlans[2][i];
						break;
					}
				}
			}
			return planNum;
		}
	}
}

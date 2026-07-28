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

import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.DiagramUtil;
import iisc.dsl.picasso.client.util.Draw1DDiagram;
import iisc.dsl.picasso.client.util.Draw2DDiagram;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.MouseBehavior;
import visad.VisADException;

public class ReducedPlanPanel extends PicassoPanel implements DisplayListener {

	private static final long serialVersionUID = -3303786673752106571L;

	double lamda = 10;

	ReducePlan reduceThread;

	boolean stopThread = false;

	// private DataValues[][] newValues;

	//Note: Lots of variables defined below!
	
	public ReducedPlanPanel(MainPanel parent) {
		super(parent);
		
		panelType = REDUCED_PLAN_DIAGRAM;
		String redalgo="";
	/*	if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_AG) redalgo="Area Greedy";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CG) redalgo="Cost Greedy";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CGFPC) redalgo="Cost Greedy with FPC";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_SEER) redalgo="SEER";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CCSEER) redalgo="CCSEER";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_LITESEER) redalgo="LiteSEER";
		*/
		String str ="Reduced Plan Diagram    "+ redalgo;
		setPanelString(str);
    	BorderLayout bl = new BorderLayout();
    	//	bl.setHgap(20);
    		bl.setVgap(40);
    		this.setBackground(Color.WHITE);
    		setLayout(bl);
		addInfoPanel(PicassoConstants.PLAN_COLOR);
		
		//apa
        predicateValues = new Hashtable();
        addBottomPanel();	
        //apae
	}

	public void process(int msgType) {
		// MessageUtil.CPrintToConsole("In Plan Panel " + getPanelString());
		sendProcessToServer(msgType);
	}

	DiagramPacket ngdp;

	public void drawDiagram(ServerPacket msg, int type, double threshold) {
		emptyPanel();

		if (threshold == -1)
			return;

		if (msg == null)
			return;

		DiagramPacket gdp = msg.diagramPacket;
		if (gdp == null)
			return;

		// PicassoUtil.checkCDP(gdp);

		lamda = threshold;
		enableRegen(false);
		nullInfoLabels();
		parent.setStatusLabel("STATUS: Reducing Plan Diagram");
		stopThread = false;
		reduceThread = new ReducePlan(this, msg, type, threshold);
		// this.getPParent().setReducedDiagramPacket(ngdp, setSortedPlans(ngdp,
		// gdp.getMaxPlan()));
	}

	public synchronized void stopReduction() {
		stopThread = true;
	}

	protected void setInfoLabels(ServerPacket msg, DiagramPacket gdp, int type, JLabel[] infoLabels) {
		// int dimensions = msg.queryPacket.getDimension();
		double[] values = DiagramUtil.getMinAndMaxValues(gdp);
		DecimalFormat df = new DecimalFormat("0.00E0");
		df.setMaximumFractionDigits(2);

		DecimalFormat df1 = new DecimalFormat();
		df1.setMaximumFractionDigits(2);

		if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_AG)
		{
			if (numPlansChanged != 0)
				resultantIncrease = (efficiency / numPlansChanged) * 100;
			else {
				minInc = 0.0;
				resultantIncrease = 0.0;
			}
		}

		// minInc = ((1.0*(minNewCost - minOldCost))/minOldCost);
		/*
		 * double giniIndex = 100.0; for ( int i=0; i < numPlansChanged; i++) {
		 * double val = ((Double)increaseList.get(i)).doubleValue(); double diff =
		 * (val*val*100.0)/(efficiency*efficiency);
		 * MessageUtil.CPrintToConsole(val + " total Inc " + efficiency + " Val /
		 * eff = " + diff); giniIndex -= diff; }
		 */

		if (parent.planPanel.planDiff.getText().equals(PlanPanel.diffLevel1))
			infoLabels[0].setText("    QTD: " + msg.queryPacket.getQueryName() + " | Parameter");
		else
			infoLabels[0].setText("    QTD: " + msg.queryPacket.getQueryName() + " | Operator");
	String redalgo="";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_AG) redalgo="{Area Greedy}";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CG) redalgo="{Cost Greedy}";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CGFPC) redalgo="{Cost Greedy with FPC}";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_SEER) redalgo="{SEER}";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CCSEER) redalgo="{CC-SEER}";
		if(PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_LITESEER) redalgo="{LiteSEER}";
		infoLabels[1].setText("# of Plans: " + new Double(values[4]).intValue());
		infoLabels[2].setText(redalgo +" @ Cost Inc Thresh: " + lamda + "%");
		
		if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_SEER || 
				PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CCSEER || 
				PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_LITESEER)
		{
			// Min info labels commented as it doesn't make much sense
			infoLabels[4].setText("Avg Cost Inc: N/A");
			//infoLabels[5].setText("Min Cost Inc: N/A");
			infoLabels[5].setText("Max Cost Inc: N/A");
		}
		else
		{
		/*	if(!((resultantIncrease>=minInc*100)&&(resultantIncrease<=maxInc*100)))
			{
				MessageUtil.CPrintToConsole("ERROR 1422 has been encountered , please mail this ocurance to picasso@dsl.serc.iisc.ernet.in");	
			}*/
			// Min info labels commented as it doesn't make much sense
			infoLabels[4].setText("Avg Cost Inc \u2264 " + df1.format(resultantIncrease) + "%");
			//infoLabels[5].setText("Min Cost Inc \u2264 " + df1.format(minInc * 100) + "%");
			infoLabels[5].setText("Max Cost Inc \u2264 " + df1.format(maxInc * 100) + "%");
		}
		
	
		infoLabels[7].setText(" ");
		infoLabels[8].setText(" ");
		infoLabels[9].setText(" ");
	//	infoLabels[23].setText(redalgo);
		//infoLabels[23].setForeground(Color.BLACK);
		//apa
//		if (gdp.getDimension() > 2) {
//			Hashtable attrSelec = getPParent().getClientPacket().getAttributeSelectivities();
//			Object[] keys = attrSelec.keySet().toArray();
//
//			String dimSelec = "";
//			int end = keys.length;
//			if (keys.length > 4) {
//				end = 4;
//			}
//			int start = 11 - end;
//			for (int i = 0; i < end; i++) {
//				int index = ((Integer) keys[i]).intValue();
//				String attrName = getPParent().getQueryAttrName(index);
//				dimSelec += "Sel[" + attrName + "]=" + attrSelec.get(keys[i]) + "%";
//				infoLabels[start + i].setText(dimSelec);
//			}
//		}
		//apae
	}

	public String getInfoStr() {
		return (infoLabels[1].getText() + ", RPD Threshold = " + lamda + ", "
				+ infoLabels[6].getText() + ", " + infoLabels[5].getText() + ", " + infoLabels[4]
				.getText());
	}

	double efficiency, maxInc, minInc;

	double resultantIncrease = 0.0;

	double maxOldCost, minOldCost;

	double maxNewCost, minNewCost;

	DataCoords[] dataCoords;

	int numPlansChanged;

	private void computeCosts(double oldCost, double newCost) {
		double curInc = ((newCost - oldCost) / oldCost);
		efficiency += curInc;
		if(curInc<0)return;
		
			if (maxInc < curInc) {
				maxNewCost = newCost;
				maxOldCost = oldCost;
				maxInc = curInc;
			}
			if (minInc > curInc) {
				minInc = curInc;
				minNewCost = newCost;
				minOldCost = oldCost;
			}
		
		numPlansChanged++;
	}

	

	private void comparePlans(double maxCost, double threshold, Vector costBucket1,
			Vector costBucket2, Vector xy1, Vector xy2) {
		DataValues dataValue2;
		double cost1;
		// double newMaxCost=0.0;
		// double newMinCost = maxCost;
		// boolean inThreshold;
		double[] returnVals = new double[2];

		returnVals[0] = -1;
		returnVals[1] = -1;
		for (int i = 0; i < costBucket1.size(); i++) {
			// dataValue1 = (DataValues)(costBucket1.elementAt(i));
			// cost1 = dataValue1.getCost();
			DataCoords coords1 = (DataCoords) xy1.elementAt(i);
			cost1 = coords1.oldCost;
			// inThreshold = false;
			for (int j = 0; j < costBucket2.size(); j++) {
				dataValue2 = (DataValues) (costBucket2.elementAt(j));
				DataCoords coords2 = (DataCoords) xy2.elementAt(j);
				// float cost2 = dataValue2.getCost();
				double cost2 = coords2.oldCost;
				if ((coords1.i > coords2.i) || (coords1.j > coords2.j)) {
					/*
					 * MessageUtil.CPrintToConsole("(" + coords1.i + "," +
					 * coords1.j + ") In Continue (" + coords2.i + "," +
					 * coords2.j + ")");
					 */
					continue;
				}

				if (inThreshold(cost2, cost1, threshold)) {
					/*
					 * if ( dataValue1.getPlanNumber() == 1 ) {
					 * MessageUtil.CPrintToConsole("(" + coords1.i + "," +
					 * coords1.j + ")" + cost2 + " InThresh :: " + cost1); }
					 */
					if (coords1.changed == true) {
						if (cost2 < coords1.newCost) {
							coords1.newCost = cost2;
							coords1.newI = coords2.i;
							coords1.newJ = coords2.j;
							coords1.newPlan = dataValue2.getPlanNumber();
						} else if (cost2 == coords1.newCost) {
							int pi = coords1.newI - coords1.i;
							int ni = coords2.i - coords1.i;
							int pj = coords1.newJ - coords1.j;
							int nj = coords2.j - coords1.j;

							if ((ni < pi) || (nj < pj)) { // the newer point
															// is closer, so
															// merge with that..
								coords1.newCost = cost2;
								coords1.newI = coords2.i;
								coords1.newJ = coords2.j;
								coords1.newPlan = dataValue2.getPlanNumber();
								// MessageUtil.CPrintToConsole("(" + pi + "," +
								// pj + ")" + "(" + ni + "," + nj + ")" + "In
								// this change of plan... ");
							}
						}
					} else {
						coords1.changed = true;
						coords1.newCost = cost2;
						coords1.newPlan = dataValue2.getPlanNumber();
						coords1.newI = coords2.i;
						coords1.newJ = coords2.j;
					}
				} /*
					 * else { /*if ( dataValue1.getPlanNumber() == 1 ) {
					 * MessageUtil.CPrintToConsole("(" + coords1.i + "," +
					 * coords1.j + ")" + cost2 + " Not in Thresh :: " + cost1);
					 * }*?/ }
					 */
			}
			/*
			 * if (inThreshold == false) return;
			 */
		}
	}

	/*
	 * private static double getMinCostForPlan(int planNumber, double maxCost,
	 * Vector costBucket) { double minCost = maxCost;
	 * 
	 * for (int i=0; i < costBucket.size(); i++) { DataValues data =
	 * (DataValues)costBucket.elementAt(i); if (data.getCost() < minCost)
	 * minCost = data.getCost(); } return minCost; }
	 * 
	 * private static double getMaxCostForPlan(int planNumber, Vector
	 * costBucket) { double maxCost = 0;
	 * 
	 * for (int i=0; i < costBucket.size(); i++) { DataValues data =
	 * (DataValues)costBucket.elementAt(i); if (data.getCost() > maxCost)
	 * maxCost = data.getCost(); } return maxCost; }
	 */

	private static boolean inThreshold(double cost1, double cost2, double threshold) {
		double val = ((cost1 - cost2) * 100 / cost2);
		if (val <= threshold && val >= 0)
			return true;

		return false;
	}

	/*
	 * private static double changeCostAndPlan(Vector costBucket, int newPlan,
	 * double newCost) { double eff = 0.0; for (int i=0; i < costBucket.size();
	 * i++) { DataValues data = (DataValues)costBucket.elementAt(i); eff +=
	 * (1.0*(newCost - data.getCost()))/data.getCost(); data.setCost(newCost);
	 * data.setPlanNumber(newPlan); } return eff; }
	 */

	private class DataCoords {
		public int i;

		public int j;

		boolean changed;

		double newCost;

		double oldCost;

		int newI;

		int newJ;

		int newPlan;

		public DataCoords(int x, int y, double cost) {
			i = x;
			j = y;
			oldCost = cost;
			changed = false;
			newCost = -1;
			newPlan = -1;
		}
	}

	int mousePressedPlanNum = -1;

	public void displayChanged(DisplayEvent e) throws VisADException, RemoteException {


		MouseBehavior mb = display.getDisplayRenderer().getMouseBehavior();

		double[] position1 = new double[] { 1.0, 1.0, 1.0 };
		double[] position2 = new double[] { -1.0, -1.0, -1.0 };

		int[] screen1 = mb.getScreenCoords(position1);
		int[] screen2 = mb.getScreenCoords(position2);

		// Compute i and j here..
		// screen2[0] gives the bottom x coord
		// screen2[1] gives the bottom y coord
		// screen1[0] gives the top x coord
		// screen1[1] gives the top y coord
		DiagramPacket gdp = getPParent().getReducedDiagramPacket();
		DiagramPacket oldgdp = getPParent().getDiagramPacket();
      QueryPacket qp;
	try {
		qp = gdp.getQueryPacket();
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		//e1.printStackTrace();
		return;
	}
		DataValues[] data = gdp.getData();
		DataValues[] olddata = oldgdp.getData();

    //  int res = gdp.getResolution();rss
      int [] res = new int[gdp.getDimension()];//
      for(int i=0;i<gdp.getDimension();i++)//
      	res[i]=gdp.getResolution(i);//
     
    //  if ( gdp.getDimension() == 1 ) 
    	//res[0] = 0;//DEMO
     int ressum[] = new int [gdp.getDimension()];
     for(int i=1;i<gdp.getDimension();i++)//
     	ressum[i]=res[i-1]+ ressum[i-1];

		// 0.8 is the aspect ratio...
		double diffX = (screen1[0] - screen2[0]) * ((1.0 - PicassoConstants.ASPECT_2D_X) / 2);
		double diffY = (screen2[1] - screen1[1]) * ((1.0 - PicassoConstants.ASPECT_2D_Y) / 2);

		double[] newScreen1 = new double[2];
		newScreen1[0] = screen1[0] - diffX;
		newScreen1[1] = screen1[1] + diffY;

		double[] newScreen2 = new double[2];
		newScreen2[0] = screen2[0] + diffX;
		newScreen2[1] = screen2[1] - diffY;
      //rss
      double xRes = (newScreen1[0] - newScreen2[0])/gdp.getResolution(PicassoConstants.a[0]);//rss
      xRes=xRes*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
      
      double yRes = 1;
      if(gdp.getDimension() != 1)
      {	
      	yRes = (newScreen2[1] - newScreen1[1])/gdp.getResolution(PicassoConstants.a[1]);//rss
      	yRes = yRes*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
      }

      else
     	{
      	yRes = (newScreen2[1] - newScreen1[1])/gdp.getResolution(PicassoConstants.a[0]);//rss
      	yRes = yRes*0.3;
     	}
  //rssend    
//      double xRes = (newScreen1[0] - newScreen2[0])/gdp.getResolution(0);//rss
//      double yRes = (newScreen2[1] - newScreen1[1])/gdp.getResolution(1);//rss

		double xValue = (e.getX() - newScreen2[0]) / xRes;
		double yValue = (e.getY() - newScreen1[1]) / yRes;

    //  int i = gdp.getResolution(1) - (int)yValue;//rss
      int i;
      double iTrans; 
      if(gdp.getDimension() != 1)
       	iTrans = (1-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]))*gdp.getResolution(PicassoConstants.a[1])/(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
      else
      	//iTrans = (1-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]))*gdp.getResolution(PicassoConstants.a[0])/(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
      	//iTrans=100;
     iTrans=gdp.getResolution(PicassoConstants.a[0])/0.3;
      	i = (int)(iTrans-yValue);//rss
      
      if ( i == gdp.getResolution(PicassoConstants.a[1]) ) i--;//rss

		// Hack..
      /*if ( gdp.getResolution(PicassoConstants.a[1]) <= 30 )//rss
			i--;*/
  	double jTrans=gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])*gdp.getResolution(PicassoConstants.a[0])/(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
    int j = (int)(xValue-jTrans);
    //  int j = (int)xValue;
    //  if ( j == gdp.getResolution(0) ) j--;//rss

		int planNumber = -1;
		DataValues curData = data[0];
		/*
		 * if ( i >= 0 && i < gdp.getResolution() && j >=0 && j <
		 * gdp.getResolution() ) planNumber = data[i*res+j].getPlanNumber();
		 */
		// apexp
		if (gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION)) 
		{
			// end apexp
			if (gdp.getDimension() != 1) 
			{
				if ( i >= 0 && i < gdp.getResolution(PicassoConstants.a[1]) && j >=0 && j < gdp.getResolution(PicassoConstants.a[0]) )
	        		curData = data[i*res[PicassoConstants.a[0]]+j];
		else return;//DEMO
    } 
				else {
					if ( i >= 0 && i < gdp.getResolution(PicassoConstants.a[0]) && j >=0 && j < gdp.getResolution(PicassoConstants.a[0]) )//rss
		        		curData = data[j];
		        	else 
		        	{
                //display.getDisplayRenderer().setForegroundColor(PicassoConstants.IMAGE_BACKGROUND);
		        		return;
		        	}
			}
			// apexp

		}

		else // exponential
		{
			double ires;
			if(gdp.getDimension() != 1)
			{
			ires=gdp.getResolution(PicassoConstants.a[1])/(qp.getEndPoint(PicassoConstants.a[1])-qp.getStartPoint(PicassoConstants.a[1]));
			}
			else
			{
			ires=gdp.getResolution(PicassoConstants.a[0])/0.3;	
			}
			
double jres=gdp.getResolution(PicassoConstants.a[0])/(qp.getEndPoint(PicassoConstants.a[0])-qp.getStartPoint(PicassoConstants.a[0]));
			
			double fi=(ires-yValue)*100.0/ires;
			double fj=xValue*100.0/ jres;//rss

			if(gdp.getDimension()==1)
			{
				if ( !(fi >= 0 && fi <=30 && fj>=qp.getStartPoint(0)*100 && fj<=qp.getEndPoint(0)*100))
				{
					
					return;
				}
			}
			double filow,fihigh;
			if(qp.getDimension()==1)
			{
				filow=0;
				fihigh=30;
			}
			else
			{
				filow=qp.getStartPoint(PicassoConstants.a[1])*100;
				fihigh=qp.getEndPoint(PicassoConstants.a[1])*100;
			}
			//do for all dims
				if ( fi >= filow && fi <=fihigh && fj >=qp.getStartPoint(PicassoConstants.a[0])*100 && fj <=100*qp.getEndPoint(PicassoConstants.a[0]) )
				{	
				float[] selvals = gdp.getPicassoSelectivity();
				int iflag = 0, jflag = 0;
					/*if(fi<selvals[0]) { i=0; iflag=1; }
					if(fj<selvals[0]) { j=0; jflag=1; }
					if(fi>=(selvals[gdp.getResolution(1)-1]+selvals[gdp.getResolution(1)-2])/2) //rss
					{ i=gdp.getResolution(1)-1; iflag=1; }//rss
					if(fj>=(selvals[gdp.getResolution(0)-1]+selvals[gdp.getResolution(0)-2])/2)//rss 
					{ j=gdp.getResolution(0)-1; jflag=1; }//rss//DEMO
*/					if(gdp.getDimension()!=1)										//L B
					{	if(fi<selvals[ressum[PicassoConstants.a[1]]]) 
						{ 
						i=0; 
						iflag=1; 
						}															//O O
					}																//W U
					else															//E N
						if(fi<selvals[0]) { i=0; iflag=1; }							//R D
					if(fj<selvals[ressum[PicassoConstants.a[0]]]) { j=0; jflag=1; }	
				//UPPER BOUND
					if(gdp.getDimension()!=1)
					{
						int offset=ressum[PicassoConstants.a[1]]+res[PicassoConstants.a[1]];
						if(fi>=(selvals[offset-1]+selvals[offset-2])/2) 
							{ 
								i=gdp.getResolution(PicassoConstants.a[1])-1;
								iflag=1; 
							}
					}
					int offset=ressum[PicassoConstants.a[0]]+res[PicassoConstants.a[0]];
					if(fj>=(selvals[offset-1]+selvals[offset-2])/2) 
					{ j=gdp.getResolution(PicassoConstants.a[0])-1; jflag=1; }
					float prev=selvals[ressum[PicassoConstants.a[0]]];
					/*for(int k=0;k<gdp.getMaxResolution()-1;k++)//rss
					{
						if(iflag==1 && jflag==1) break;
						
						if(iflag==0)
						if(fi>prev && fi<=(selvals[k+1]+selvals[k])/2)
						{
							i=k;
					iflag = 1;
				}
						if(jflag==0)
						if(fj>prev && fj<=(selvals[k+1]+selvals[k])/2)
						{
							j=k;
					jflag = 1;
				}
						prev=(selvals[k+1]+selvals[k])/2;
					}DEMO*/
					for(int k=ressum[PicassoConstants.a[0]];k<(ressum[PicassoConstants.a[0]]+res[PicassoConstants.a[0]]);k++)//rss
					{
						if(jflag==1) break;
						
						if(jflag==0)
						if(fj>prev && fj<=(selvals[k+1]+selvals[k])/2)
						{
							j=k;
							jflag=1;
							j=j-ressum[PicassoConstants.a[0]];
						}
						prev=(selvals[k+1]+selvals[k])/2;
					}
					if(gdp.getDimension()!=1)
					{
					prev=selvals[ressum[PicassoConstants.a[1]]];
					for(int k=ressum[PicassoConstants.a[1]];k<ressum[PicassoConstants.a[1]]+res[PicassoConstants.a[1]];k++)
					{
						if(iflag==1)break;
						if(iflag==0)
						
						if(fi>prev && fi<=(selvals[k+1]+selvals[k])/2)
						{
							i=k;
							iflag=1;
							i=i-ressum[PicassoConstants.a[1]];
						//	i=i-gdp.getResolution(PicassoConstants.a[0]);
						}
						prev=(selvals[k+1]+selvals[k])/2;
					}
					}
					else i=0;
					curData = data[i*res[PicassoConstants.a[0]]+j];
				}
			
				//clicking outside the graph range 
				else {
					//display.getDisplayRenderer().setForegroundColor(PicassoConstants.IMAGE_BACKGROUND);
					return;
				}
		}

		// end apexp
		// display.getDisplayRenderer().setForegroundColor(PicassoConstants.PLAN_COLOR);
		planNumber = curData.getPlanNumber();
		InputEvent ie = e.getInputEvent();
		int mods = ie.getModifiers();
		String[] rel = gdp.getRelationNames();
		String[] attr = gdp.getAttributeNames();
		String[] constants = gdp.getConstants();

		// apexp
		DecimalFormat df;
		if (gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION)) {
			df = new DecimalFormat("0.00");
			df.setMaximumFractionDigits(2);
		} else {
			df = new DecimalFormat("0.00000");
			df.setMaximumFractionDigits(5);
		}

		/*float[] pselec = gdp.getPicassoSelectivity();
		float xDSelec = pselec[j];
	float yDSelec = pselec[res[0]+i];//rss */
		/*int ressum[] = new int [gdp.getDimension()];
        for(int k=1;k<gdp.getDimension();k++)//
        	ressum[k]=res[k-1]+ ressum[k-1];
        */
		float[] pselec = gdp.getPicassoSelectivity();
		  float xDSelec = j>0?pselec[ressum[PicassoConstants.a[0]]+j]:0;
	        float yDSelec = 0;
	        if(gdp.getDimension() != 1)
	        	yDSelec = (ressum[PicassoConstants.a[1]]+i>0 && ressum[PicassoConstants.a[1]]+i<pselec.length)?pselec[ressum[PicassoConstants.a[1]]+i]:0;//rss
	       
		String xSelec = "" + df.format(xDSelec);
		String ySelec = "" + df.format(yDSelec);

	//	String[] infoStr = new String[3];
		//infoStr[0] = "";
		//infoStr[1] = rel[PicassoConstants.a[0]] + "." + attr[PicassoConstants.a[0]] + " [" + "Selectivity: " + xSelec + ", Constant: "
		//		+ constants[j] + "]";
		//if (gdp.getDimension() == 1)
		//	infoStr[2] = "";
	//	else
		//  infoStr[2] = rel[PicassoConstants.a[1]] + "." + attr[PicassoConstants.a[1]] + " [" + "Selectivity: " + ySelec + ", Constant: " + constants[res[PicassoConstants.a[0]]+i] + "]";
	 //    infoStr[2] = rel[1] + "." + attr[1] + " [" + "Selectivity: " + ySelec + ", Constant: " + constants[/*ressum[PicassoConstants.a[1]]+*/i] + "]";
		if (e.getId() == DisplayEvent.MOUSE_PRESSED_RIGHT) {
			mousePressedPlanNum = planNumber;
			// MessageUtil.CPrintToConsole("xRes :: " + xRes + " yRes :: " +
			// yRes);
			// MessageUtil.CPrintToConsole("xValue :: " + xValue + " yValue :: "
			// + yValue);
			// System.out.println("screen1 = (" + screen1[0] + ", " + screen1[1]
			// +"), new :: (" + newScreen1[0] + ", " + newScreen1[1]);
			// System.out.println("screen2 = (" + screen2[0] + ", " + screen2[1]
			// +"), new :: (" + newScreen2[0] + ", " + newScreen2[1]);
			// MessageUtil.CPrintToConsole(e.getId() + " X :: " + e.getX() + " Y
			// :: " + e.getY() + " Plan # " + planNumber);
		} else if (e.getId() == DisplayEvent.MOUSE_RELEASED_RIGHT) {
			// if ((mods & InputEvent.SHIFT_MASK) != 0) {
			// PicassoUtil.displayCompiledTree(parent, this, infoStr,
			// gdp.getResolution(), xDSelec, yDSelec, planNumber);
			// }
			String showString = "";
			if ((mods & InputEvent.ALT_MASK) != 0 || (mods & InputEvent.CTRL_MASK) != 0) {
				showString = "This mouse-key control not valid in Reduced Plan Diagram";
				JOptionPane.showMessageDialog(this, showString, "QueryInfo",
						JOptionPane.INFORMATION_MESSAGE);
			} else if ((mods & InputEvent.SHIFT_MASK) != 0) {
				display.getDisplayRenderer().setCursorOn(false);
				DataValues d, od;
				if (gdp.getDimension() == 1) {
					od = olddata[j];
					d = data[j];
				} else {
    			  od = olddata[i*res[PicassoConstants.a[0]]+j];//rss
    			  d = data[i*res[PicassoConstants.a[0]]+j];//rss
				}

				df = new DecimalFormat("0.00E0");
				if (od.getPlanNumber() != d.getPlanNumber()) {
					if (gdp.getDimension() == 1) {
						
						if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_SEER || 
								PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CCSEER || 
								PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_LITESEER)
							{
								ArrayList point = new ArrayList();
								if(planStrSet)
								{
									int curplan = d.getPlanNumber();
									
									costSet = false;
									point.add(new Integer(j));
									PicassoUtil.getPlanCosts(getPParent(), this, planStrs[curplan], point);
									while(costSet != true)
										;
									d.setCost(cost[0]);
								}
								else
								{
									// The code should never enter this else part. But even if it does, the code is ready to handle it :]
									PicassoUtil.getAllPlanStrings(getPParent(), this, point);
									
									int curplan = d.getPlanNumber();
									
									costSet = false;
									point.add(new Integer(j));
									PicassoUtil.getPlanCosts(getPParent(), this, planStrs[curplan], point);
									while(costSet != true)
										;
									d.setCost(cost[0]);
								}
							}
						if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CG || PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_AG)
							showString = "Selectivity = (" + xSelec + ")\nPlan = P"
									+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
									+ ", Original Plan = P"
									+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
									+ "\nCost \u2264 " + df.format(d.getCost()) + ", Original Cost = "
									+ df.format(od.getCost()) + "\nCardinality = "
									+ df.format(od.getCard());
						else 
							showString = "Selectivity = (" + xSelec + ")\nPlan = P"
							+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
							+ ", Original Plan = P"
							+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
							+ "\nCost = " + df.format(d.getCost()) + ", Original Cost = "
							+ df.format(od.getCost()) + "\nCardinality = "
							+ df.format(od.getCard());
					} else {
						// 2-D or more case
						// Need to do some processing here in case of the FPC based algorithms.
						// Required to set the cost values correctly.
						if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_SEER || 
							PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CCSEER || 
							PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_LITESEER)
						{
							ArrayList point = new ArrayList();
							if(planStrSet)
							{
								int curplan = d.getPlanNumber();
								
								costSet = false;
								point.add(new Integer(i*res[PicassoConstants.a[0]]+j));
								PicassoUtil.getPlanCosts(getPParent(), this, planStrs[curplan], point);
								while(costSet != true)
									;
								d.setCost(cost[0]);
							}
							else
							{
								// The code should never enter this else part. But even if it does, the code is ready to handle it :]
								PicassoUtil.getAllPlanStrings(getPParent(), this, point);
								
								int curplan = d.getPlanNumber();
								
								costSet = false;
								point.add(new Integer(i*res[PicassoConstants.a[0]]+j));
								PicassoUtil.getPlanCosts(getPParent(), this, planStrs[curplan], point);
								while(costSet != true)
									;
								d.setCost(cost[0]);
							}
							/*if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CG || PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_AG)
								showString = "Selectivities = (" + xSelec + "," + ySelec + ")\nPlan = P"
								+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
								+ ", Original Plan = P"
								+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
								+ "\nCost \u2264 " + df.format(d.getCost()) + ", Original Cost = "
								+ df.format(od.getCost()) + "\nCardinality = "
							+ df.format(od.getCard());
							else */
							showString = "Selectivities = (" + xSelec + "," + ySelec + ")\nPlan = P"
							+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
							+ ", Original Plan = P"
							+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
							+ "\nCost = " + df.format(d.getCost()) + ", Original Cost = "
							+ df.format(od.getCost()) + "\nCardinality = "
							+ df.format(od.getCard());
						}
						else if( PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_CG || PicassoConstants.REDUCTION_ALGORITHM==PicassoConstants.REDUCE_AG)
							showString = "Selectivities = (" + xSelec + "," + ySelec + ")\nPlan = P"
							+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
							+ ", Original Plan = P"
							+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
							+ "\nCost \u2264 " + df.format(d.getCost()) + ", Original Cost = "
							+ df.format(od.getCost()) + "\nCardinality = "
							+ df.format(od.getCard());
						/*else
						{
							showString = "Selectivities = (" + xSelec + "," + ySelec + ")\nPlan = P"
								+ (getPParent().getSortedPlan()[0][d.getPlanNumber()] + 1)
								+ ", Original Plan = P"
								+ (getPParent().getSortedPlan()[0][od.getPlanNumber()] + 1)
								+ "\nCost = " + df.format(d.getCost()) + ", Original Cost = "
								+ df.format(od.getCost()) + "\nCardinality = "
								+ df.format(od.getCard());
						}*/
					}

				} else {
					if (gdp.getDimension() == 1) {
						showString = "Selectivity = (" + xSelec + ")\nPlan = P"
								+ (getPParent().getSortedPlan()[0][planNumber] + 1) + "\nCost = "
								+ df.format(od.getCost()) + "\nCardinality = "
								+ df.format(od.getCard());
					} else {
						showString = "Selectivity = (" + xSelec + "," + ySelec + ")\nPlan = P"
								+ (getPParent().getSortedPlan()[0][planNumber] + 1) + "\nCost = "
								+ df.format(od.getCost()) + "\nCardinality = "
								+ df.format(od.getCard());
					}
				}

				JOptionPane.showMessageDialog(this, showString, "QueryInfo",
						JOptionPane.INFORMATION_MESSAGE);
				// MessageUtil.CPrintToConsole("Cost = " +
				// data[i*gdp.getResolution()+j].getCost());
				// MessageUtil.CPrintToConsole("Cardinality = " +
				// data[i*gdp.getResolution()+j].getCard());
			} else {
				String planStr = "";
				if (planNumber != -1 && mousePressedPlanNum != -1) {
					if (mousePressedPlanNum == planNumber) {
						planStr += planNumber;
						if ( gdp.getDimension() ==1 )
	    				{
							if(data[j].getPlanNumber() != planNumber )//rss
	    				  		planStr += "," + data[i].getPlanNumber();//rss
	    				}
						else if(data[i*res[PicassoConstants.a[0]]+j].getPlanNumber() != planNumber )//rss
	    				  		planStr += "," + data[i*res[PicassoConstants.a[0]]+j].getPlanNumber();//rss
					} else
						planStr = mousePressedPlanNum + "," + planNumber;
					// MessageUtil.CPrintToConsole("PlanStr :: " + planStr);
					PicassoUtil.displayTree(parent, this, planStr);
				}
			}
			// MessageUtil.CPrintToConsole("xRes :: " + xRes + " yRes :: " +
			// yRes);
			// MessageUtil.CPrintToConsole("xValue :: " + xValue + " yValue :: "
			// + yValue);
			// System.out.println("screen1 = (" + screen1[0] + ", " + screen1[1]
			// +"), new :: (" + newScreen1[0] + ", " + newScreen1[1]);
			// System.out.println("screen2 = (" + screen2[0] + ", " + screen2[1]
			// +"), new :: (" + newScreen2[0] + ", " + newScreen2[1]);
			// MessageUtil.CPrintToConsole("In Mouse Released Right :: " +
			// e.getX() + " Y :: " + e.getY() + " Plan # " + planNumber);
		}
	}

	/*
	 * private void displayCompiledTree(String[] infoStr, int resolution, int
	 * selec1, int selec2, int planNumber) { // Get the fields data structure
	 * ClientPacket values = getClientPacket(); String serverName =
	 * parent.getServerName(); int serverPort = parent.getServerPort(); double
	 * s1 = (double)selec1*100/resolution; double s2 =
	 * (double)selec2*100/resolution;
	 *  // Build the message to be sent //String sendString =
	 * MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE); //sendString +=
	 * "&" + MessageIds.PLAN_NUMBER + "=" + planStr;
	 * 
	 * values.setMessageId(MessageIds.GET_COMPILED_PLAN_TREE); Vector v =
	 * values.getDimensions(); values.setPlanNumbers(""+planNumber);
	 * 
	 * Hashtable table = new Hashtable(); table.put(v.elementAt(0),
	 * Double.toString(s1)); table.put(v.elementAt(1), Double.toString(s2));
	 *  // Hack need to actually rebuild it... values.put("Info", infoStr);
	 * values.setCompileTreeValues(table);
	 * 
	 * MessageUtil.CPrintToConsole("VALUES :: " + selec1 + " " + selec2);
	 * MessageUtil.CPrintToConsole("DIMENSIONS :: " + v.elementAt(0) + " " +
	 * v.elementAt(1));
	 * 
	 * //values.setPlanNumbers(planStr);
	 * MessageUtil.sendMessageToServer(serverName, serverPort, values, this); }
	 * 
	 * private void displayTree(String[] infoStr, String planStr) { // Get the
	 * fields data structure ClientPacket values = getClientPacket(); String
	 * serverName = parent.getServerName(); int serverPort =
	 * parent.getServerPort();
	 *  // Build the message to be sent //String sendString =
	 * MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE); //sendString +=
	 * "&" + MessageIds.PLAN_NUMBER + "=" + planStr;
	 * 
	 * values.setMessageId(MessageIds.GET_PLAN_TREE);
	 * values.setPlanNumbers(planStr); for (int i=0; i < infoStr.length; i++) {
	 * infoStr[i] = ""; } values.put("Info", infoStr);
	 * MessageUtil.sendMessageToServer(serverName, serverPort, values, this); }
	 */

	public void setNgdp(DiagramPacket gdp, ServerPacket msg, int type) {
		DisplayImpl display1 = null;

		if (display != null)
			display.removeDisplayListener(this);

		DiagramPacket globgdp = gdp;
		ngdp = gdp;
//		this.getPParent().setReducedDiagramPacket(ngdp, ngdp.getSortedPlanArray());
											//	setSortedPlans(ngdp, gdp.getMaxPlan()));
		this.getPParent().setFullReducedDiagramPacket(gdp);
		setSliceDiagramPacket(false); //so that the same slice is used when the sortedPlans is read while drawing
		parent.setDiagramPacket(parent.getServerPacket().diagramPacket); //so as to calculate that sorted array
		
		setSliceDiagramPacket(true);
		gdp = parent.getReducedDiagramPacket();
		ngdp = gdp; //see if needed here
		
		if (gdp.getDimension() == 1)
			display1 = Draw1DDiagram.draw(display, getPParent(), gdp, type, maps);
		else
			display1 = Draw2DDiagram.draw(display, getPParent(), gdp, type, maps);

		display = display1;
		diagramComponent = display.getComponent();
		remove(diagramComponent);
		try 
		{
			System.gc();
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		add(diagramComponent, BorderLayout.CENTER);
		validate();

		setInfoLabels(msg, globgdp, type, infoLabels);

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		/*
		 * double resultantIncrease=0; if(numPlansChanged != 0) {
		 * resultantIncrease = (efficiency/numPlansChanged)*100; minInc =
		 * ((1.0*(minNewCost - minOldCost))/minOldCost); String message =
		 * "Average cost increase: "+ df.format(resultantIncrease)+"%\n\n" +
		 * "Maximum cost increase: "+df.format(maxInc*100)+"% (from "+
		 * (int)maxOldCost+ " to " + (int)(maxNewCost) +")\n\n" +"Minimum cost
		 * increase: "+df.format(minInc*100)+"% (from "+ (int)(minOldCost) + "
		 * to " + (int)(minNewCost) +")\n\n";
		 * 
		 * JOptionPane.showMessageDialog (null, message,"Information",
		 * JOptionPane.INFORMATION_MESSAGE); }
		 */

		enableRegen(true);
		display.addDisplayListener(this);
		parent.setStatusLabel("STATUS: DONE");
		parent.enableAllTabs();
		parent.showStopButton(false);
		parent.repaint();
		dimensionBox1.setEnabled(true);
		if(PicassoConstants.NUM_DIMS > 1)
		dimensionBox2.setEnabled(true);
		if(PicassoConstants.NUM_DIMS > 2)
			setButton.setEnabled(true);
	}

	class ReducePlan implements Runnable {
		double threshold;

		ReducedPlanPanel panel;

		ServerPacket msg;

		int type;

		public ReducePlan(ReducedPlanPanel p, ServerPacket m, int pt, double t) {
			threshold = t;
			panel = p;
			msg = m;
			type = pt;

			Thread th = new Thread(this);
			th.start();
		}

		// @pd////////////////////////////////
		public void run() {
			DiagramPacket gdp;
			int maxPlans;

			gdp = msg.diagramPacket;
			gdp.setQueryPacket(msg.queryPacket);
			if(gdp.resolution == null) //-ma
				gdp.resolution = new int[gdp.getDimension()];
			
			if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_AG) {
				maxPlans = gdp.getMaxPlanNumber();

				MessageUtil.CPrintToConsole("In AreaGreedy Reduce Method");
				DataValues[] origData = gdp.getData();
				// double threshold = panel.getPParent().getThreshold();

				int[][] sortedPlans = panel.getPParent().getSortedPlan();

			int nrows = gdp.getResolution(1);//rss
			int ncols = gdp.getResolution(0);//rss
				// int maxPlans = gdp.getMaxPlan();
				// int newMaxPlan = maxPlans;

			DiagramPacket ngdp = new DiagramPacket(gdp);
			/* ngdp.setDimension(gdp.getDimension());
			if(ngdp.resolution == null) //-ma
				ngdp.resolution = new int[gdp.getDimension()]; */

				if (gdp.getDimension() == 1)
					nrows = 1;

				DataValues[] newData = new DataValues[nrows * ncols];
			/* ngdp.setMaxCard(gdp.getMaxCard());
				ngdp.setMinCard(gdp.getMinCard());
				ngdp.setResolution(gdp.getResolution());
				ngdp.setMaxConditions(gdp.getDimension());
				ngdp.setMaxCost(gdp.getMaxCost());
				ngdp.setMinCost(gdp.getMinCost());
				ngdp.setRelationNames(gdp.getRelationNames());
				ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
				ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
				ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
				ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
				ngdp.setRelationNames(gdp.getRelationNames());
				ngdp.setAttributeNames(gdp.getAttributeNames());
				ngdp.setConstants(gdp.getConstants());
				ngdp.setQueryPacket(gdp.getQueryPacket()); */

				for (int i = 0; i < nrows; i++) {
					for (int j = 0; j < ncols; j++) {
					newData[i*ncols+j] = origData[i*ncols+j].copy();
					}
				}
				// panel.setNewValues(newData);

				// ///////////////////////////////////////////////////
				//
				// Following Code Finds the Boundary Points of the contiguous
				// Regions and
				// Puts them in Buckets.
				// Added on: 20th oct 2006

				Vector[] bordercostBucket = new Vector[maxPlans];
				Vector[] bordercoords = new Vector[maxPlans];
				for (int i = 0; i < maxPlans; i++) {
					bordercostBucket[i] = new Vector();
					bordercoords[i] = new Vector();
				}
				try {
					int Boundaries[][] = new int[nrows][ncols];
					for (int i = nrows - 2; i >= 0; i--) {// for (int i=0; i <
						// nrows-1; i++) {
						for (int j = 0; j < ncols - 1; j++) {

							int Gx = newData[i * ncols + j].getPlanNumber()
									- newData[(i + 1) * ncols + j + 1].getPlanNumber();
							int Gy = newData[(i) * ncols + j + 1].getPlanNumber()
									- newData[(i + 1) * ncols + j].getPlanNumber();
							if (java.lang.Math.sqrt(Gx * Gx + Gy * Gy) > 0) {
								Boundaries[i][j] = 1;
							} else
								Boundaries[i][j] = 0;
						}

					}

					for (int i = nrows - 2; i >= 0; i--) {
						for (int j = 0; j < ncols - 1; j++) {
							if (Boundaries[i][j] == 1) {
								for (int i1 = -1; i1 <= 1; i1++)
									for (int j1 = -1; j1 <= 1; j1++) {
										int i2 = i + i1;
										int j2 = j + j1;
					if((i2 >=0 && j2>=0) &&( (i1==0 && j1==0) || Boundaries[i2][j2]==0)){
					int curPlan1 = newData[(i2)*ncols+(j2)].getPlanNumber();
					bordercostBucket[curPlan1].add(newData[(i2)*ncols+(j2)]);
					DataCoords dc = new DataCoords(i2, j2, newData[(i2)*ncols+(j2)].getCost());
											bordercoords[curPlan1].add(dc);
					if(Boundaries[i2][j2]==0)Boundaries[i2][j2]=-1;
										}
									}
							}
						}

					}

					for (int i = 0; i < nrows; i++) {
						int j = ncols - 1;
						int curPlan1 = newData[(i) * ncols + (j)].getPlanNumber();
						bordercostBucket[curPlan1].add(newData[(i) * ncols + (j)]);
						DataCoords dc = new DataCoords(i, j, newData[(i) * ncols + (j)].getCost());
						bordercoords[curPlan1].add(dc);
					}

					for (int j = 0; j < ncols; j++) {
						int i = nrows - 1;
						int curPlan1 = newData[(i) * ncols + (j)].getPlanNumber();
						bordercostBucket[curPlan1].add(newData[(i) * ncols + (j)]);
						DataCoords dc = new DataCoords(i, j, newData[(i) * ncols + (j)].getCost());
						bordercoords[curPlan1].add(dc);

					}

				} catch (Exception e) {
					System.out.println("Exception in PlanReduction Border find code");
				}
				// ////////////////////////////////////

				// Starting from plans with most area.. Check for plans with
				// least area if
				// the cost is within the threshold value, if so you can replce
				// them..
				// First put all the points and their cost with the same plan
				// into their respective buckets.
				Vector[] costBucket = new Vector[maxPlans];
				Vector[] coords = new Vector[maxPlans];
				for (int i = 0; i < maxPlans; i++) {
					costBucket[i] = new Vector();
					coords[i] = new Vector();
				}

				// store coords and set changes to the end like the previous
				// algo will be
				// changed later
				dataCoords = new DataCoords[origData.length];
				for (int i = 0; i < nrows; i++) {
					for (int j = 0; j < ncols; j++) {
						int curPlan = newData[i * ncols + j].getPlanNumber();
						costBucket[curPlan].add(newData[i * ncols + j]);
						DataCoords dc = new DataCoords(i, j, newData[i * ncols + j].getCost());
						dataCoords[i * ncols + j] = dc;
						coords[curPlan].add(dc);
					}
				}

				double maxCost = gdp.getMaxCost();

				efficiency = 0.0;
				maxInc = 0.0;
				minInc = 100.0;
				maxOldCost = 0.0;
				minOldCost = maxCost;
				maxNewCost = 0.0;
				minNewCost = maxCost;
				numPlansChanged = 0;

				// For plans with min area, check their cost and see if they are
				// within the
				// threshold of plans with max area covered. If so, change the
				// plan
				// If not, do not change them.
				boolean planChanged[] = new boolean[maxPlans];
				int changedPlan[] = new int[maxPlans];

				for (int i = 0; i < maxPlans; i++) {
					planChanged[i] = false;
					changedPlan[i] = sortedPlans[2][i];
				}

				for (int i = maxPlans - 1; i >= 0; i--) {
					int iPlan = sortedPlans[2][i];
					for (int a = 0; a < coords[iPlan].size(); a++) {
						((DataCoords) coords[iPlan].elementAt(a)).changed = false;
					}

					parent.setProgressBar(((maxPlans - i) * 100) / maxPlans);
					parent.setStatusLabel("STATUS: Reducing Plan Diagram");
					for (int j = 0; j < maxPlans; j++) {

						int jPlan = sortedPlans[2][j];
						if (i == j || iPlan == jPlan || costBucket[iPlan] == null
								|| costBucket[jPlan] == null)
							continue;
						// NEW Border Checking Code
						comparePlans(maxCost, threshold, costBucket[iPlan],
								bordercostBucket[jPlan], coords[iPlan], bordercoords[jPlan]);
						// OLD Whole Area checking Code
						// comparePlans(maxCost, threshold, costBucket[iPlan],
						// costBucket[jPlan], coords[iPlan], coords[jPlan]);
					}
					if (stopThread == true) {
						parent.resetReducedDiagram();
						return;
					}

					// If the iplan can all be changed change them
					boolean change = true;
					for (int a = 0; a < coords[iPlan].size(); a++) {
						DataCoords dc = ((DataCoords) coords[iPlan].elementAt(a));
						if (dc.changed == false) {
							change = false;
							break;
						}
					}
					if (change == true) {

						// Change this plan to the appropriate plan chosen
						// MessageUtil.CPrintToConsole(i + " Removing Plan # ::
						// " + iPlan);
						for (int a = 0; a < costBucket[iPlan].size(); a++) {
							DataValues d1 = (DataValues) costBucket[iPlan].elementAt(a);
							DataCoords dc1 = (DataCoords) coords[iPlan].elementAt(a);

							double oldCost = dc1.oldCost; // d1.getCost();
							d1.setCost(dc1.newCost);
							d1.setPlanNumber(dc1.newPlan);
							computeCosts(oldCost, dc1.newCost);
							// Add the change plan to the new cost bucket
							costBucket[dc1.newPlan].add(d1);
							coords[dc1.newPlan].add(dc1);
						}

						// TSTART updating Borderbuckets
						bordercostBucket[iPlan].removeAllElements(); // Not
						// needed
						// anymore...
						bordercostBucket[iPlan] = null;
						bordercoords[iPlan].removeAllElements();
						bordercoords[iPlan] = null;
						// TEND updating Borderbuckets

						costBucket[iPlan].removeAllElements(); // Not needed
						// anymore...
						costBucket[iPlan] = null;
						coords[iPlan].removeAllElements();
						coords[iPlan] = null;

					}
				}
				parent.setProgressBar(100);
				ngdp.setDataPoints(newData);
				panel.setNgdp(ngdp, msg, type);
				ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
				validateCostIncrease(ngdp, gdp, threshold);
				for (int i = 0; i < maxPlans; i++) {
					if (costBucket[i] != null) {
						costBucket[i].removeAllElements();
						costBucket[i] = null;
						coords[i].removeAllElements();
						coords[i] = null;
					}
				}

			} else if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CG) {
				MessageUtil.CPrintToConsole("In CostGreedy Reduce Method");
				if (stopThread == true) {
					parent.resetReducedDiagram();
					return;
				}

				DiagramPacket newgdp = new DiagramPacket();
				int dim = gdp.getDimension();
				if (dim == 1) {
					newgdp = reduce1D(threshold, gdp);
				} else if (dim == 2) {
					newgdp = reduce2D(threshold, gdp);
				} else if (dim == 3) {
					newgdp = reduce3D(threshold, gdp);
				} else if (dim == 4) {
					newgdp = reduce4D(threshold, gdp);
				}
				parent.setProgressBar(100);
				panel.setNgdp(newgdp, msg, type);

				ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
				validateCostIncrease(ngdp, gdp, threshold);
			} else {
				// TODO These algos require FPC. Check if the database is complaint
				if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CGFPC) {
					MessageUtil.CPrintToConsole("In CG-FPC"); 
					if (stopThread == true) {
						parent.resetReducedDiagram();
						return;
					}

					DiagramPacket newgdp = cgFpc(threshold, gdp);
					
					parent.setProgressBar(100);
					panel.setNgdp(newgdp, msg, type);
					ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
					// validateCostIncrease(ngdp, gdp, threshold);
				} else if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_SEER) {
					MessageUtil.CPrintToConsole("In Seer");
					if (stopThread == true) {
						parent.resetReducedDiagram();
						return;
					}

					DiagramPacket newgdp = seer(threshold, gdp);
					parent.setProgressBar(100);
					panel.setNgdp(newgdp, msg, type);
					ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
					// validateCostIncrease(ngdp, gdp, threshold);
				} else if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_CCSEER) {//CC-SEER Reduction invoked
					MessageUtil.CPrintToConsole("In CC-Seer");
					if (stopThread == true) {
						parent.resetReducedDiagram();
						return;
					}

					DiagramPacket newgdp = hcSeer(threshold, gdp);
					parent.setProgressBar(100);
					panel.setNgdp(newgdp, msg, type);
					ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
					// validateCostIncrease(ngdp, gdp, threshold);
				} else if (PicassoConstants.REDUCTION_ALGORITHM == PicassoConstants.REDUCE_LITESEER) {
					MessageUtil.CPrintToConsole("In LiteSeer");
					if (stopThread == true) {
						parent.resetReducedDiagram();
						return;
					}

					DiagramPacket newgdp = liteSeer(threshold, gdp);
					parent.setProgressBar(100);
					panel.setNgdp(newgdp, msg, type);
					ngdp.setMaxPlanNumber(PicassoUtil.getMaxPlans(ngdp));
					// validateCostIncrease(ngdp, gdp, threshold);
				}
			}
		}

		class Set {
			// HashSet<Integer> elements = new HashSet<Integer>();
			HashSet elements = new HashSet();
		}

		class Plan {
			double cost;

			double min;

			double max;

			int rx;

			int ry;

			int tx;

			int ty;

			int ct;
		}

		class Belong {
			double[] a;

			public Belong(int maxPlans) {
				a = new double[maxPlans];
				for (int i = 0; i < maxPlans; i++) {
					a[i] = -1;
				}
			}
		}

		public DiagramPacket reduce2D(double threshold, DiagramPacket gdp) {

			DiagramPacket ngdp = new DiagramPacket(gdp);

		/*	ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setMinCard(gdp.getMinCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setMinCost(gdp.getMinCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

		int n = gdp.getMaxPlanNumber();
		int r = gdp.getResolution(1);//rss
		int c = gdp.getResolution(0);//rss
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			// double maxx = 0;
			// double minx = Double.MAX_VALUE;
		double[][] xcost = new double[gdp.getMaxResolution()][n]; //-ma
			boolean[] notSwallowed = new boolean[n];
			Set[] s = new Set[n];

			int pb = 0;
			for (int i = r - 1; i >= 0; i--) // for each row
			{
			for (int j = c - 1; j >= 0; j--)	//for each column //rss
				{
				int x = (i * c + j);			//for each point, starting from topmost, rightmos
					Integer xI = new Integer(x);

					// belong[x] = new Belong(n);
					int p = data[x].getPlanNumber();

					if (s[p] == null) {
						s[p] = new Set(); // create a set for each plan
					}
					s[p].elements.add(xI); // add the point to the set

					double cost = data[x].getCost(); // orig cost of x
					// maxx = Math.max(maxx, cost);
					// minx = Math.min(minx, cost);
					double lt = cost * (1 + threshold / 100);
					if (xcost[j][p] != 0) {
						xcost[j][p] = Math.min(cost, xcost[j][p]);
					} else {
						xcost[j][p] = cost;
					}
					for (int k = 0; k < j; k++) {
						if (xcost[k][p] != 0) {
							xcost[k][p] = Math.min(cost, xcost[k][p]);
						} else {
							xcost[k][p] = cost;
						}
					}
					if (notSwallowed[p]) {
						continue;
					}
					// int jj = j + 1;
					boolean flag = false;
					for (int xx = 0; xx < n; xx++) {
						if (xx != p && xcost[j][xx] != 0
								&& /* xcost[j][k][xx] >= c && */xcost[j][xx] <= lt
								&& xcost[j][xx] >= cost) {
							// belong[x].a[xx] = xcost[j][xx];
							if (s[xx] == null) {
								s[xx] = new Set();
							}
							s[xx].elements.add(xI);
							flag = true;
						}
					}
					// dont think this is required
					// if (jj < r) {
					// for (int xx = 0; xx < n; xx++) {
					// if (xx != p && xcost[jj][xx] != 0
					// && /* xcost[j][k][xx] >= c && */xcost[jj][xx] <= lt) {
					// // belong[x].a[xx] = xcost[jj][xx];
					// if (s[xx] == null) {
					// s[xx] = new Set();
					// }
					// s[xx].elements.add(xI);
					// flag = true;
					// }
					// }
					// }
					if (!flag) {
						notSwallowed[p] = true;
					}

					parent.setProgressBar(pb);
					if (i % (0.2 * r) == 0 && pb <= 30) {
						pb++;
					}
				}
			}

			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
					parent.setProgressBar(pb);
					if (i % (n / 3) == 0 && pb <= 70) {
						pb++;
					}
				}
				s[p] = null;
			}

			double costinc = 0;
			minInc = Double.MAX_VALUE;
			maxInc = 0;
			resultantIncrease = 0;
			int noOfPoints=0;
			// repeat the whole process to get the values for the new data
		xcost = new double[gdp.getMaxResolution()][n];
		for (int i = r - 1; i >= 0; i--)
		{
			for (int j = c - 1; j >= 0; j--)
			{

				int x = (i * c + j);

					newData[x] = new DataValues();

					int p = data[x].getPlanNumber();
					Integer pI = new Integer(p);
					newData[x].setCard(data[x].getCard());

					double cost = data[x].getCost();
					// maxx = Math.max(maxx, cost);
					// minx = Math.min(minx, cost);
					double lt = cost * (1 + threshold / 100);
					if (xcost[j][p] != 0) {
						xcost[j][p] = Math.min(cost, xcost[j][p]);
					} else {
						xcost[j][p] = cost;
					}
					for (int k = 0; k < j; k++) {
						if (xcost[k][p] != 0) {
							xcost[k][p] = Math.min(cost, xcost[k][p]);
						} else {
							xcost[k][p] = cost;
						}
					}

					if (soln.contains(pI)) {
						newData[x].setPlanNumber(p);
						newData[x].setCost(data[x].getCost());
					} else {
						int plan = -1;
						double newcost = Double.MAX_VALUE;
						for (int xx = 0; xx < n; xx++) {
							if (soln.contains(new Integer(xx)) && xx != p && xcost[j][xx] != 0
									&& xcost[j][xx] <= lt && xcost[j][xx] >= cost) {
								// another redundant check for xx != p
								if (xcost[j][xx] <= newcost) {
									costinc = ((xcost[j][xx] - cost) / cost);
									// what r the chances of being equal ??
									
									// resultantIncrease += costinc;
									newcost = xcost[j][xx];
									plan = xx;
								}
							}
						}
						noOfPoints++;
						costinc = ((xcost[j][plan] - cost) / cost);
						if (minInc > costinc && costinc > 0)
							minInc = costinc;
						if (maxInc < costinc && costinc > 0)
							maxInc = costinc;
						resultantIncrease += costinc;
						newData[x].setPlanNumber(plan);
						newData[x].setCost(newcost);
					}

					parent.setProgressBar(pb);
					if (i % (0.2 * r) == 0 && pb <= 100) {
						pb++;
					}

				}
			}
			if(noOfPoints==0)
				noOfPoints=1;
			if (minInc == Double.MAX_VALUE)
				minInc = 0;
			//System.out.println("NoOFPOints is..." +noOfPoints);
			//resultantIncrease = (resultantIncrease / (r * r)) * 100;
			resultantIncrease = (resultantIncrease / (noOfPoints)) * 100;
			// ngdp.setMaxPlanNumber(soln.size());
			ngdp.setDataPoints(newData);
			System.out.println("Exiting CG method");
			return ngdp;
		}

		private DiagramPacket reduce4D(double th, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n = gdp.getMaxPlanNumber();
			int r[] = gdp.getResolution();
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			Set[] s = new Set[n];

			double[][][][] xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			boolean[] notSwallowed = new boolean[n];

			th /= 100;
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) 
				{
					int jj = j + 1;
					if (jj < r[1]) {
						for (int k = r[2] - 1; k >= 0; k--) {
							for (int l = 0; l < r[3]; l++) {
								for (int m = 0; m < n; m++) {
									if (xcost[j][k][l][m] != 0) {
										if (xcost[jj][k][l][m] != 0) {
											xcost[j][k][l][m] = Math.min(xcost[j][k][l][m],
													xcost[jj][k][l][m]);
										}
									} else {
										xcost[j][k][l][m] = xcost[jj][k][l][m];
									}
								}
							}
						}
					}

					for (int k = r[2] - 1; k >= 0; k--) {
						int kk = k + 1;
						if (kk < r[2]) {
							for (int l = 0; l < r[3]; l++) {
								for (int m = 0; m < n; m++) {
									if (xcost[j][k][l][m] != 0) {
										if (xcost[j][kk][l][m] != 0) {
											xcost[j][k][l][m] = Math.min(xcost[j][k][l][m],
													xcost[j][kk][l][m]);
										}
									} else {
										xcost[j][k][l][m] = xcost[j][kk][l][m];
									}
								}
							}
						}

						for (int l = r[3] - 1; l >= 0; l--) {
							int x = l*r[0]*r[1]*r[2]+ k*r[0]*r[1] + j*r[0] + i;
							Integer xI = new Integer(x);

							double c = data[x].getCost();
							int p = data[x].getPlanNumber();
							double t = c * (1 + th);

							if (s[p] == null) {
								s[p] = new Set();
							}
							s[p].elements.add(xI);

							if (xcost[j][k][l][p] != 0) {
								xcost[j][k][l][p] = Math.min(xcost[j][k][l][p], c);
							} else {
								xcost[j][k][l][p] = c;
							}

							for (int b = 0; b <= l; b++) {
								if (xcost[j][k][b][p] == 0) {
									xcost[j][k][b][p] = c;
								} else {
									xcost[j][k][b][p] = Math.min(xcost[j][k][b][p], c);
								}
							}
							if (notSwallowed[p]) {
								continue;
							}
							boolean flag = false;
							for (int xx = 0; xx < n; xx++) {

								if (xx != p && xcost[j][k][l][xx] != 0
										&& /* xcost[j][k][l][xx] >= c && */xcost[j][k][l][xx] <= t) {
									if (s[xx] == null) {
										s[xx] = new Set();
									}
									s[xx].elements.add(xI);
									flag = true;
								}
							}
							if (!flag) {
								notSwallowed[p] = true;
							}
						}
					}
				}
			}

			// Now we have a reduced Universal set. apply the log n
			// approximation
			// univ is the Universal set and s is the subsets
			HashSet soln = new HashSet();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i]) {
					soln.add(new Integer(i));
				}
			}
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i] = null;
				}
			}
			Set t1 = new Set();
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
					t1.elements.addAll(s[i].elements);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}

			xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) {

					int jj = j + 1;
					if (jj < r[1]) {
						for (int k = r[2] - 1; k >= 0; k--) {
							for (int l = 0; l < r[3]; l++) {
								for (int m = 0; m < n; m++) {
									if (xcost[j][k][l][m] != 0) {
										if (xcost[jj][k][l][m] != 0) {
											xcost[j][k][l][m] = Math.min(xcost[j][k][l][m],
													xcost[jj][k][l][m]);
										}
									} else {
										xcost[j][k][l][m] = xcost[jj][k][l][m];
									}
								}
							}
						}
					}

					for (int k = r[2] - 1; k >= 0; k--) {
						int kk = k + 1;
						if (kk < r[2]) {
							for (int l = 0; l < r[3]; l++) {
								for (int m = 0; m < n; m++) {
									if (xcost[j][k][l][m] != 0) {
										if (xcost[j][kk][l][m] != 0) {
											xcost[j][k][l][m] = Math.min(xcost[j][k][l][m],
													xcost[j][kk][l][m]);
										}
									} else {
										xcost[j][k][l][m] = xcost[j][kk][l][m];
									}
								}
							}
						}

						for (int l = r[3] - 1; l >= 0; l--) {
							int x = l*r[0]*r[1]*r[2]+ k*r[0]*r[1] + j*r[0] + i;
							
							newData[x] = new DataValues();
							double c = data[x].getCost();
							int p = data[x].getPlanNumber();
							Integer pI = new Integer(p);
							double t = c * (1 + th);

							if (xcost[j][k][l][p] != 0) {
								xcost[j][k][l][p] = Math.min(xcost[j][k][l][p], c);
							} else {
								xcost[j][k][l][p] = c;
							}

							for (int b = 0; b <= l; b++) {
								if (xcost[j][k][b][p] == 0) {
									xcost[j][k][b][p] = c;
								} else {
									xcost[j][k][b][p] = Math.min(xcost[j][k][b][p], c);
								}
							}
							if(soln.contains(pI)) {
								newData[x].setPlanNumber(p);
								newData[x].setCost(data[x].getCost());
								newData[x].setCard(data[x].getCard());
							}
							else {
								double cheap = Double.MAX_VALUE;
								int plan = -1;
								for (int xx = 0; xx < n; xx++) {
									if (soln.contains(new Integer(xx))) {
										if (xcost[j][k][l][xx] != 0 && xcost[j][k][l][xx] <= t
												&& xcost[j][k][l][xx] <= cheap) {
											plan = xx;
											cheap = xcost[j][k][l][xx];
										}
									}
								}
								
								newData[x].setCard(data[x].getCard());
								newData[x].setPlanNumber(plan);
								newData[x].setCost(cheap);
							}
						}
					}
				}
			}
			ngdp.setDataPoints(newData);
//			ngdp.setMaxPlanNumber(soln.size());
			setInfoValues(data, newData);
			return ngdp;
		}

		private DiagramPacket reduce3D(double th, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n = gdp.getMaxPlanNumber();
			int r[] = gdp.getResolution();
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			Set[] s = new Set[n];

			boolean[] notSwallowed = new boolean[n];

			th /= 100;
			double[][][] xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) {
					for (int k = r[2] - 1; k >= 0; k--) {
						int jj = j + 1;
						if (jj < r[1]) {
							for (int l = 0; l < n; l++) {
								if (xcost[j][k][l] != 0) {
									if (xcost[jj][k][l] != 0) {
										xcost[j][k][l] = Math.min(xcost[j][k][l], xcost[jj][k][l]);
									}
								} else {
									xcost[j][k][l] = xcost[jj][k][l];
								}
							}
						}
					}
					for (int k = r[2] - 1; k >= 0; k--) {
						int x = k*r[0]*r[1] + j*r[0] + i;

						Integer xI = new Integer(x);

						double c = data[x].getCost();
						int p = data[x].getPlanNumber();
						double t = c * (1 + th);

						if (s[p] == null) {
							s[p] = new Set();
						}
						s[p].elements.add(xI);

						if (xcost[j][k][p] != 0) {
							xcost[j][k][p] = Math.min(xcost[j][k][p], c);
						} else {
							xcost[j][k][p] = c;
						}
						for (int b = 0; b <= k; b++) {
							if (xcost[j][b][p] == 0) {
								xcost[j][b][p] = c;
							} else {
								xcost[j][b][p] = Math.min(xcost[j][b][p], c);
							}
						}
						if (notSwallowed[p]) {
							continue;
						}
						boolean flag = false;
						for (int xx = 0; xx < n; xx++) {
							if (xx != p && xcost[j][k][xx] != 0
									&& /* xcost[j][k][xx] >= c && */xcost[j][k][xx] <= t) {
								if (s[xx] == null) {
									s[xx] = new Set();
								}
								s[xx].elements.add(xI);
								flag = true;
							}
						}
						if (!flag) {
							notSwallowed[p] = true;
						}
					}
				}
			}

			// Now we have a reduced Universal set. apply the log n
			// approximation
			// univ is the Universal set and s is the subsets
			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}

			xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) {
					for (int k = r[2] - 1; k >= 0; k--) {
						int jj = j + 1;
						if (jj < r[1]) {
							for (int l = 0; l < n; l++) {
								if (xcost[j][k][l] != 0) {
									if (xcost[jj][k][l] != 0) {
										xcost[j][k][l] = Math.min(xcost[j][k][l], xcost[jj][k][l]);
									}
								} else {
									xcost[j][k][l] = xcost[jj][k][l];
								}
							}
						}
					}
					for (int k = r[2] - 1; k >= 0; k--) {
						// int x = (j * r[0] + i) * r[1] + k;
						int x = k*r[0]*r[1] + j*r[0] + i;
						
						newData[x]=new DataValues();
						double c = data[x].getCost();
						int p = data[x].getPlanNumber();
						Integer pI = new Integer(p);
						double t = c * (1 + th);

						if (xcost[j][k][p] != 0) {
							xcost[j][k][p] = Math.min(xcost[j][k][p], c);
						} else {
							xcost[j][k][p] = c;
						}
						for (int b = 0; b <= k; b++) {
							if (xcost[j][b][p] == 0) {
								xcost[j][b][p] = c;
							} else {
								xcost[j][b][p] = Math.min(xcost[j][b][p], c);
							}
						}
						
						if(soln.contains(pI)) {
							newData[x].setPlanNumber(p);
							newData[x].setCost(data[x].getCost());
							newData[x].setCard(data[x].getCard());
						}
						double cheap = Double.MAX_VALUE;
						int plan = -1;
						for (int xx = 0; xx < n; xx++) {
							if (soln.contains(new Integer(xx))) {
								if (xcost[j][k][xx] != 0 && xcost[j][k][xx] <= t
										&& xcost[j][k][xx] <= cheap) {
									plan = xx;
									cheap = xcost[j][k][xx];
								}
							}
						}
						//newData[x] = new DataValues();
						newData[x].setCard(data[x].getCard());
						newData[x].setPlanNumber(plan);
						newData[x].setCost(cheap);
					}
				}
			}
			for (Iterator it = soln.iterator(); it.hasNext();) {
				Integer ii = (Integer) it.next();
				// System.out.println(ii);
			}
			ngdp.setDataPoints(newData);
//			ngdp.setMaxPlanNumber(soln.size());
			setInfoValues(data, newData);
			return ngdp;
		}

		public DiagramPacket reduce1D(double threshold, DiagramPacket gdp) {

			DiagramPacket ngdp = new DiagramPacket();

			ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setMinCard(gdp.getMinCard());
		// int dims=2;
		for (int i=0;i<PicassoConstants.NUM_DIMS;i++)
			ngdp.setResolution(gdp.getResolution(i),i);
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setMinCost(gdp.getMinCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setAttrTypes(gdp.getAttributeTypes());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());

			int n = gdp.getMaxPlanNumber();
			int r = 1;
		if(gdp.getDimension()!=1)
			r = gdp.getResolution(1);//rss
		int c =gdp.getResolution(0);//rss
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			// double maxx = 0;
			// double minx = Double.MAX_VALUE;
		double[][] xcost = new double[c][n];
			boolean[] notSwallowed = new boolean[n];
			Set[] s = new Set[n];

			int pb = 0;
			// for (int i = r - 1; i >= 0; i--) //for each row; 1D only one row
			// {
			int i = 1;
			for (int j = c - 1; j >= 0; j--)	//for each column
			{
				// int x = (i * r + j); //for each point, starting from topmost,
				// rightmos
				int x = j;
				Integer xI = new Integer(x);

				// belong[x] = new Belong(n);
				int p = data[x].getPlanNumber();

				if (s[p] == null)
				{
					s[p] = new Set(); // create a set for each plan
				}
				s[p].elements.add(xI); // add the point to the set

				double cost = data[x].getCost(); // orig cost of x
				// maxx = Math.max(maxx, cost);
				// minx = Math.min(minx, cost);
				double lt = cost * (1 + threshold / 100);
				if (xcost[j][p] != 0) {
					xcost[j][p] = Math.min(cost, xcost[j][p]);
				} else {
					xcost[j][p] = cost;
				}
				for (int k = 0; k < j; k++) {
					if (xcost[k][p] != 0) {
						xcost[k][p] = Math.min(cost, xcost[k][p]);
					} else {
						xcost[k][p] = cost;
					}
				}
				if (notSwallowed[p]) {
					continue;
				}
				// int jj = j + 1;
				boolean flag = false;
				for (int xx = 0; xx < n; xx++) {
					if (xx != p && xcost[j][xx] != 0
							&& /* xcost[j][k][xx] >= c && */xcost[j][xx] <= lt
							&& xcost[j][xx] >= cost) {
						// belong[x].a[xx] = xcost[j][xx];
						if (s[xx] == null) {
							s[xx] = new Set();
						}
						s[xx].elements.add(xI);
						flag = true;
					}
				}
				// dont think this is required
				// if (jj < r) {
				// for (int xx = 0; xx < n; xx++) {
				// if (xx != p && xcost[jj][xx] != 0
				// && /* xcost[j][k][xx] >= c && */xcost[jj][xx] <= lt) {
				// // belong[x].a[xx] = xcost[jj][xx];
				// if (s[xx] == null) {
				// s[xx] = new Set();
				// }
				// s[xx].elements.add(xI);
				// flag = true;
				// }
				// }
				// }
				if (!flag) {
					notSwallowed[p] = true;
				}

				parent.setProgressBar(pb);
				if (j % (0.2 * r) == 0 && pb <= 30) {
					pb++;
				}
			}
			// }

			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			for (i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
					parent.setProgressBar(pb);
					if (i % (n / 3) == 0 && pb <= 70) {
						pb++;
					}
				}
				s[p] = null;
			}

			double costinc = 0;
			minInc = Double.MAX_VALUE;
			maxInc = 0;
			resultantIncrease = 0;
			int noOfPoints=0;
			// repeat the whole process to get the values for the new data
		xcost = new double[c][n];
			// for (int i = r - 1; i >= 0; i--)
			// {
			i = 1;
			for (int j = c - 1; j >= 0; j--)
			{

				// int x = (i * r + j);
				int x = j;
				newData[x] = new DataValues();

				int p = data[x].getPlanNumber();
				Integer pI = new Integer(p);
				newData[x].setCard(data[x].getCard());

				double cost = data[x].getCost();
				// maxx = Math.max(maxx, cost);
				// minx = Math.min(minx, cost);
				double lt = cost * (1 + threshold / 100);
				if (xcost[j][p] != 0) {
					xcost[j][p] = Math.min(cost, xcost[j][p]);
				} else {
					xcost[j][p] = cost;
				}
				for (int k = 0; k < j; k++) {
					if (xcost[k][p] != 0) {
						xcost[k][p] = Math.min(cost, xcost[k][p]);
					} else {
						xcost[k][p] = cost;
					}
				}

				if (soln.contains(pI)) {
					newData[x].setPlanNumber(p);
					newData[x].setCost(data[x].getCost());
				} else {
					int plan = -1;
					double newcost = Double.MAX_VALUE;
					for (int xx = 0; xx < n; xx++) {
						if (soln.contains(new Integer(xx)) && xx != p && xcost[j][xx] != 0
								&& xcost[j][xx] <= lt && xcost[j][xx] >= cost) {
							// another redundant check for xx != p
							if (xcost[j][xx] <= newcost) {
								costinc = ((xcost[j][xx] - cost) / cost);
								// what r the chances of being equal ??
								
								// resultantIncrease += costinc;
								newcost = xcost[j][xx];
								plan = xx;
							}
						}
					}
					noOfPoints++;
					costinc = ((xcost[j][plan] - cost) / cost);
					if (minInc > costinc && costinc > 0)
						minInc = costinc;
					if (maxInc < costinc && costinc > 0)
						maxInc = costinc;
					resultantIncrease += costinc;
					newData[x].setPlanNumber(plan);
					newData[x].setCost(newcost);
				}
				
				parent.setProgressBar(pb);
				if (j % (0.2 * r) == 0 && pb <= 100) {
					pb++;
				}

			}
			// }
			if(noOfPoints==0)
				noOfPoints=1;
			if (minInc == Double.MAX_VALUE)
				minInc = 0;
			//resultantIncrease = (resultantIncrease / (r * r)) * 100;
			//Dividing by the number of poits swallowed instead of all points
			resultantIncrease = (resultantIncrease / (noOfPoints )) * 100;

			ngdp.setDataPoints(newData);
			// ngdp.setMaxPlanNumber(soln.size());
			System.out.println("Exiting CG method");
			return ngdp;
		}

		// ////////////////////////////////////////

		void validateCostIncrease(DiagramPacket newGdp, DiagramPacket gdp, double threshold) {
			int nrows = gdp.getResolution(1);//rss
			int dim = gdp.getDimension();
			int ncols = gdp.getResolution(0);//rss
			if (dim == 1)
				nrows = 1;
			DataValues[] ndv = newGdp.getData();
			DataValues[] dv = gdp.getData();
			for (int i = 0; i < nrows; i++) {
				for (int j = 0; j < ncols; j++) {
					double oldCost = dv[i*ncols+j].getCost();
					double newCost = ndv[i*ncols+j].getCost();
					if (!inThreshold(newCost, oldCost, threshold)) {
						MessageUtil.CPrintToConsole("Points not in Threshold :: (" + i + "," + j + ")" +
								"Cost :: (" + newCost + "," + oldCost + ")");
					}
				}
			}

			// Ratio of Max to Min for getting a single picture..
			double[] vals = DiagramUtil.getMinAndMaxValues(gdp);
			double cost2 = vals[0];
			double cost1 = vals[1];
			double ratio = ((cost1 - cost2) * 100 / cost2);
			MessageUtil.CPrintToConsole("Ratio to get single plan :: " + ratio);
			if (threshold > ratio) {
				if (newGdp.getMaxPlanNumber() != 1) {
					JOptionPane
							.showMessageDialog(
									parent,
									"Cannot be reduced to a single plan because the Plan Cost Monotonicity (PCM) does not hold.\nCheck the Comp Cost Diag for violation of PCM",
									"Warning", JOptionPane.WARNING_MESSAGE);
				}
			} else {
				/*
				 * if ( newGdp.getMaxPlan() == 1 ) {
				 * MessageUtil.CPrintToConsole("No of Plans Should not be 1, Bug
				 * in the code, " + threshold + " Ratio " + ratio); }
				 */
			}
		}

		float[] sel;

		public DiagramPacket seer(double threshold, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setMinCard(gdp.getMinCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setMinCost(gdp.getMinCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n = gdp.getMaxPlanNumber();
			// System.out.println(n);
			Set[] s = new Set[n];
			Integer[] x = new Integer[n];
			boolean[] notSwallowed = new boolean[n];
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			boolean[][] swallow = new boolean[n][n];
			int r[] = gdp.getResolution();
			sel = gdp.getPicassoSelectivity();
			int d = gdp.getDimension();

			minInc = Double.MAX_VALUE;
			maxInc = Double.MIN_VALUE;
			resultantIncrease = 0;
			
			getPlanStrings(data,n);
			
			costs = new double[n][data.length];
			wedge = new boolean[n];
			perimeter = new boolean[n];
			for (int i = 0; i < n; i++) {
				s[i] = new Set();
				x[i] = new Integer(i);
				s[i].elements.add(x[i]);
				Arrays.fill(costs[i], -1);
			}

			for (int i = 0; i < n; i++) {
				boolean flag = false;
				for (int j = 0; j < n; j++) {
					// j -> swallower & i -> swallowee
					if (i != j) {
						if (ndCheck(j, i, (float) threshold, d, r)) {
							s[j].elements.add(x[i]);
							flag = true;
							swallow[j][i] = true;
						}
					}
				}
				if (!flag) {
					notSwallowed[i] = true;
				}
			}
			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}

			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
			// soln has the required soln
			System.out.println("# plans in Reduced Diagram : " + soln.size());
			int[] plan = new int[n];
			for (int i = 0; i < n; i++) {
				if (soln.contains(x[i])) {
					plan[i] = i;
				} else {
					for (Iterator it = soln.iterator(); it.hasNext();) {
						Integer xx = (Integer) it.next();
						if (swallow[xx.intValue()][i]) {
							plan[i] = xx.intValue();
							break;
						}
					}
				}
			}
			for (int i = 0; i < data.length; i++) {
				int pi = data[i].getPlanNumber();
				newData[i] = new DataValues();
				newData[i].setCard(data[i].getCard());
				newData[i].setPlanNumber(plan[pi]);
				// what to do????
				// newData[i].setCost(getCost(plan[pi], i));
			}
			ngdp.setDataPoints(newData);
			setInfoValues(data, newData);
			return ngdp;
		}

		/************************** CC-SEER ********************************/
		double[][] AllPlanCosts;
		int[] offset;
		int[] base;
		int [] virtualBase;
		
		public DiagramPacket hcSeer(double threshold, DiagramPacket gdp) {
			
			long  start = System.currentTimeMillis();
			DiagramPacket ngdp = new DiagramPacket(gdp);

			
			int n = gdp.getMaxPlanNumber();
			HashSet[] s = new HashSet[n];
			Integer[] x = new Integer[n];
			boolean[] notSwallowed = new boolean[n];
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			boolean[][] swallow = new boolean[n][n];
			int r[] = gdp.getResolution();
			sel = gdp.getPicassoSelectivity();
			int d = gdp.getDimension();
			
			getPlanStrings(data,n);

			AllPlanCosts = new double[n][(int) Math.pow(4, d)];
			offset = new int[d];
			base = new int[d];
			virtualBase = new int[d];
			
			//initialize offset
			offset[0] = 0;
			for (int i = 1; i < d; i++)
				offset[i] = offset[i - 1] + r[i- 1];
			
			//initialize base
			base[0] = 1;
			for (int i = 1; i < d; i++)
				base[i] = base[i - 1] * r[i - 1];
			
			//initialize the virtual base
			virtualBase[0] = 1;
			for (int i = 1; i < d; i++)
				virtualBase[i] = virtualBase[i - 1] * 4;
			
			
			ArrayList points = new ArrayList();
			ArrayList virtualPts = new ArrayList();
			int[] selPt = new int[d];
			AddHyperCubePts(d, selPt, r, 0, points, virtualPts);
			
			
			//Cost all the plans at all the points conatained in $points$ i.e. the "hypercube" points
			for (int planNum = 0; planNum < n; planNum++) {
				costSet = false;
				PicassoUtil.getPlanCosts(getPParent(), panel, planStrs[planNum], points);
				while (!costSet) {
					try {
					Thread.sleep(1000);
					} catch (InterruptedException e) {
					e.printStackTrace();
					}
				}
				// System.out.printf("plan # %d     ", planNum + 1);
				int k = 0;
				for(Iterator it = virtualPts.iterator();it.hasNext();) {
					int pt = ((Integer)it.next()).intValue();
					AllPlanCosts[planNum][pt] = cost[k];
					k++;
				}
			}
			
			
			
			for (int i = 0; i < n; i++) {
				s[i] = new HashSet();
				x[i] = new Integer(i);
				s[i].add(x[i]);
			}
		
			ArrayList slopeDim = new ArrayList();
			ArrayList slopeDimVal = new ArrayList();
			int[] dim = new int[d];
			for (int i = 0; i < d; i++)
				dim[i] = i;
			
			
			for (int i = 0; i < n; i++) {
				boolean flag = false;
				for (int j = 0; j < n; j++) {
					// j -> swallower & i -> swallowee
					if (i != j) {
						if (hctest(j, i, d, selPt, d, dim, r, threshold, slopeDim, slopeDimVal)) {
							s[j].add(x[i]);
							flag = true;
							swallow[j][i] = true;
						}
					}
				}
				if (!flag) {
					notSwallowed[i] = true;
				}
			}
			
			ArrayList soln = new ArrayList();
			HashSet temp = new HashSet();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.addAll(s[i]);
					s[i].clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}

			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].removeAll(temp);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].removeAll(s[p]);
						if (s[i].size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			} 
		
			// soln has the required soln
			long finish = System.currentTimeMillis();
			long timeElapsed = (finish - start) / 1000;
			System.out.println("Total time elapsed :" + timeElapsed + "seconds");
			int[] plan = new int[n];
			for (int i = 0; i < n; i++) {
				if (soln.contains(x[i])) {
					plan[i] = i;
				} else {
					for (Iterator it = soln.iterator(); it.hasNext();) {
						Integer xx = (Integer) it.next();
						if (swallow[xx.intValue()][i]) {
							plan[i] = xx.intValue();
							break;
						}
					}
				}
			}
			for (int i = 0; i < data.length; i++) {
				int pi = data[i].getPlanNumber();
				newData[i] = new DataValues();
				newData[i].setCard(data[i].getCard());
				newData[i].setPlanNumber(plan[pi]);
			}
			ngdp.setDataPoints(newData);
			return ngdp;

		}

		/* diagDim : dimensionality of the plan diagram
		 * 
		 * nDim : describes the dimensionality of the hyperplane (which is a slice of the original plan diagram).
		 * For eg.: its value would be 2 if the concerned hyperplane is a 2 dimensonal slice of the original plan diagram.
		 * 
		 * dim : it is an array of size $numOfDim$ decribing the orientation of the
		 * hyperplane of the original diagram. For eg.: say the original diagram is 
		 * 3 dimensional with X, Y, Z being the dimensions 1, 2 and 3 respectively. 
		 * Then a XZ hyperplane of the original diagram would be described by sending
		 * {1, 3} as the value of the array $dim$.
		 * 
		 *  selPt : it is an array containing the $d$ coordinates of a selectivity point in the original diagram space.
		 *  
		 *  
		 */
		
		//Adds the selectivity points required for hypercube test to the arraylist $points$
		//This is done so that the FPCs can be done in bulk.
		public void AddHyperCubePts(int numOfDim, int[] selPt, int[] r, int depth, ArrayList points, ArrayList virtualPts) {
			if (depth == numOfDim - 1) {
				selPt[depth] = 0;
				points.add(new Integer(PtIndex(selPt, r, numOfDim)));
				virtualPts.add(new Integer(VirtualPtIndex(selPt, r, numOfDim)));
				
				selPt[depth] = 1;
				points.add(new Integer(PtIndex(selPt, r, numOfDim)));
				virtualPts.add(new Integer(VirtualPtIndex(selPt, r, numOfDim)));
				
				selPt[depth] = r[depth] - 2;
				points.add(new Integer(PtIndex(selPt, r, numOfDim)));
				virtualPts.add(new Integer(VirtualPtIndex(selPt, r, numOfDim)));
				
				selPt[depth] = r[depth] - 1;
				points.add(new Integer(PtIndex(selPt, r, numOfDim)));
				virtualPts.add(new Integer(VirtualPtIndex(selPt, r, numOfDim)));
				
			}
			
			else {
				selPt[depth] = 0;
				AddHyperCubePts(numOfDim, selPt, r, depth + 1, points, virtualPts);
				
				selPt[depth] = 1;
				AddHyperCubePts(numOfDim, selPt, r, depth + 1, points, virtualPts);
			
				selPt[depth] = r[depth] - 2;
				AddHyperCubePts(numOfDim, selPt, r, depth + 1, points, virtualPts);
				
				selPt[depth] = r[depth] - 1;
				AddHyperCubePts(numOfDim, selPt, r, depth + 1, points, virtualPts);
				
			}
			
		}
		

		//Determines the index of a selectivity point
		public int PtIndex(int[] selPt, int[] r, int diagDim)
		{
			int ptIndex = 0;
			for (int i = 0; i < diagDim; i++) 
				ptIndex += selPt[i] * base[i];
			return ptIndex;
		}
			
		//determines the virtual index of a "hypercube" pt
		public int VirtualPtIndex(int[] selPt, int[] r, int diagDim) {
			int ptIndex = 0;
			for (int i = 0; i < diagDim; i++) {
				if (selPt[i] > 1) {
					if (selPt[i] == r[i] - 2)
						ptIndex += virtualBase[i]* 2;
					else
						ptIndex += virtualBase[i] * 3;
				}
				else
					ptIndex += virtualBase[i] * selPt[i];					
			}
			return ptIndex;
		}
		
		final int SEC_DER_POSITIVE = 0;
		final int SEC_DER_NEG = 1;
		final int SEC_DER_INCONCLUSIVE = 2;
		//Sanity constant made to 0, can be changed if necessary
		final int SANITY_CONST = 0;	
		public boolean hctest(int pEr, int pEe, int diagDim, int[] selPt, int nDim, int[] dim, int[] r, double th, ArrayList slopeDim, ArrayList slopeDimVal) {
		
			
			if (nDim == 0) {
				ArrayList slopeDimClone = new ArrayList();
				ArrayList slopeDimValClone = new ArrayList();
				
				slopeDimClone = (ArrayList) slopeDim.clone();
				slopeDimValClone = (ArrayList) slopeDimVal.clone();
				if (ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th) <= 0)
					return true;
				else
					return false;
			}
			
			int[] newDim = new int[nDim - 1];
			
			for (int i = 0; i < nDim; i++) {
				ConstructArrayDim(nDim, dim, newDim, dim[i]);
				
				ArrayList slopeDimClone = new ArrayList();
				ArrayList slopeDimValClone = new ArrayList();
				
				slopeDimClone = (ArrayList) slopeDim.clone();
				slopeDimValClone = (ArrayList) slopeDimVal.clone();
				selPt[dim[i]] = 0;
				boolean frontSafety = hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th, slopeDimClone, slopeDimValClone);
				
				slopeDimClone = (ArrayList) slopeDim.clone();
				slopeDimValClone = (ArrayList) slopeDimVal.clone();
				selPt[dim[i]] = r[dim[i]] - 1;
				boolean backSafety = hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th, slopeDimClone, slopeDimValClone);
				
				if (!(frontSafety && backSafety)) 
					continue;
				
				//compute second derivative
				ArrayList hPlaneDim = new ArrayList();
				for (int j = 0; j < nDim - 1; j++)
					hPlaneDim.add(new  Integer(newDim[j]));
				slopeDimClone = (ArrayList) slopeDim.clone();
				slopeDimValClone = (ArrayList) slopeDimVal.clone();
				int secDerSign = ComputeSecDer(pEr, pEe, hPlaneDim, selPt, dim[i], slopeDimClone, slopeDimValClone, r, diagDim, th); 
				
				if (secDerSign == SEC_DER_POSITIVE)
					return true;
				else if (secDerSign == SEC_DER_INCONCLUSIVE)
					continue;
					
				ArrayList newSlopeDim = new ArrayList();
				ArrayList newSlopeDimVal  = new ArrayList();
				
				newSlopeDim = (ArrayList) slopeDim.clone();
				newSlopeDimVal = (ArrayList) slopeDimVal.clone();
				newSlopeDim.add(new Integer(dim[i]));
				newSlopeDimVal.add(new Integer(1));
				
				if (hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th, newSlopeDim, newSlopeDimVal))
					return true;
				
				
				newSlopeDim = (ArrayList) slopeDim.clone();
				newSlopeDimVal = (ArrayList) slopeDimVal.clone();
				newSlopeDim.add(new Integer(dim[i]));
				newSlopeDimVal.add(new Integer(r[dim[i]] - 1));
				
				if (hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th, newSlopeDim, newSlopeDimVal))
					return true;
				
			}
			
			return false;
		}
		
		public double ComputeVal(int pEr, int pEe, int[] selPt, ArrayList slopeDim, ArrayList slopeDimVal, int[] r, int diagDim, double th)
		{
			
			if (slopeDim.isEmpty()) {
				return SFVal(pEr, pEe, selPt, r, diagDim, th);
			}	
			Integer curDim = (Integer)slopeDim.get(0);
			Integer curDimVal = (Integer)slopeDimVal.get(0);
			
			ArrayList slopeDimClone = new ArrayList();
			ArrayList slopeDimValClone = new ArrayList();
			
			slopeDimClone = (ArrayList)slopeDim.clone();
			slopeDimValClone = (ArrayList)slopeDimVal.clone();
			slopeDimClone.remove(0);
			slopeDimValClone.remove(0);
			
			
			selPt[curDim.intValue()] = curDimVal.intValue();
			double val1 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);

			slopeDimClone = (ArrayList)slopeDim.clone();
			slopeDimValClone = (ArrayList)slopeDimVal.clone();
			slopeDimClone.remove(0);
			slopeDimValClone.remove(0);
			selPt[curDim.intValue()]--;
			double val2 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);
			
			double finalVal = val2 - val1;
			
			if (curDimVal.intValue() == r[curDim.intValue()] - 1)
				finalVal /= (sel[offset[curDim.intValue()] + r[curDim.intValue()] - 1] - 
						     sel[offset[curDim.intValue()] + r[curDim.intValue()] - 2]);
			else
				finalVal /= (sel[offset[curDim.intValue()] + 0] - sel[offset[curDim.intValue()] + 1]);
			
			return finalVal;
		}
		
		
		
		//computes the value of the safety function at the given selectivity point.
		double SFVal(int pEr, int pEe, int[] selPt, int[] r, int diagDim, double th) {
			th = 1 + th / 100;
			double sfVal = Cost(pEr, VirtualPtIndex(selPt, r, diagDim)) - (th * Cost(pEe, VirtualPtIndex(selPt, r, diagDim))) - SANITY_CONST;
			return sfVal;
		}
		
		public double Cost(int planNum, int ptIndex) {
			return AllPlanCosts[planNum][ptIndex];
		}


		public void ConstructArrayDim(int nDim, int[] dim, int[] newDim, int currDim){
			int k = 0;
			for (int i = 0; i < nDim; i++) {
				if (dim[i] != currDim)
					newDim[k++] = dim[i];
				
			}
		}
		
		public int ComputeSecDer(int pEr, int pEe, ArrayList hPlaneDim, int[] selPt, int secDerDim, ArrayList slopeDim, ArrayList slopeDimVal, int[] r, int diagDim, double th) {
			if (hPlaneDim.isEmpty()) {
				ArrayList slopeDimClone = new ArrayList();
				ArrayList slopeDimValClone = new ArrayList();
				
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
								
				selPt[secDerDim] = 0;
				double val1 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
				
				selPt[secDerDim] = 1;
				double val2 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
				
				selPt[secDerDim] = r[secDerDim] - 2;
				double val3 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
				
				selPt[secDerDim] = r[secDerDim] - 1;
				double val4 = ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				double firstDer1 = (val1 - val2) / (sel[offset[secDerDim] + 0] - sel[offset[secDerDim] + 1]);
				double firstDer2 = (val3 - val4) / (sel[offset[secDerDim] + r[secDerDim] - 2] - sel[offset[secDerDim] + r[secDerDim] - 1]);
				
				if (firstDer1 <= firstDer2)
					return SEC_DER_POSITIVE;
				else
					return SEC_DER_NEG;
			}
			else {
				ArrayList hPlaneDimClone = new ArrayList();
				ArrayList slopeDimClone = new ArrayList();
				ArrayList slopeDimValClone = new ArrayList();
				
				hPlaneDimClone = (ArrayList)hPlaneDim.clone();
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
				
				Integer curDim = (Integer)hPlaneDimClone.get(hPlaneDimClone.size() - 1);
				hPlaneDimClone.remove(hPlaneDimClone.size() - 1);
				
				selPt[curDim.intValue()] = 0;
				int secDerSign1 = ComputeSecDer(pEr, pEe, hPlaneDimClone, selPt, secDerDim, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				hPlaneDimClone = (ArrayList)hPlaneDim.clone();
				hPlaneDimClone.remove(hPlaneDimClone.size() - 1);
				slopeDimValClone = (ArrayList)slopeDimVal.clone();
				slopeDimClone = (ArrayList)slopeDim.clone();
				
				selPt[curDim.intValue()] = r[curDim.intValue()] - 1;
				int secDerSign2 = ComputeSecDer(pEr, pEe, hPlaneDimClone, selPt, secDerDim, slopeDimClone, slopeDimValClone, r, diagDim, th);
				
				if (secDerSign1 == SEC_DER_POSITIVE && secDerSign2 == SEC_DER_POSITIVE)
					return SEC_DER_POSITIVE;
				else if (secDerSign1 == SEC_DER_NEG && secDerSign2 == SEC_DER_NEG)
					return SEC_DER_NEG;
				else 
					
					return SEC_DER_INCONCLUSIVE;
			}
			
		}
	
	
/************************** CC-SEER ********************************/

		
		public DiagramPacket liteSeer(double threshold, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setMinCard(gdp.getMinCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setMinCost(gdp.getMinCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n = gdp.getMaxPlanNumber();
			// System.out.println(n);
			Set[] s = new Set[n];
			Integer[] x = new Integer[n];
			boolean[] notSwallowed = new boolean[n];
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			boolean[][] swallow = new boolean[n][n];
			int r[] = gdp.getResolution();
			sel = gdp.getPicassoSelectivity();
			int d = gdp.getDimension();
			minInc = Double.MAX_VALUE;
			maxInc = Double.MIN_VALUE;
			resultantIncrease = 0;

			getPlanStrings(data,n);
			
			int noPts = (int) Math.pow(2, d);
			costs = new double[n][noPts];
			ArrayList points = new ArrayList();
			for (int i = 0; i < noPts; i++) {
				int[] in = new int[d];
				int xx = i;
				for (int j = 0; j < d; j++) {
					in[j] = xx & 1;
					xx >>= 1;
					in[j] *= (r[j] - 1);
				}
				int c = 0;
				for (int j = d - 1; j >= 0; j--) {
					c = c * r[j] + in[j];
				}
				points.add(new Integer(c));
			}

			for (int i = 0; i < n; i++) {
				s[i] = new Set();
				x[i] = new Integer(i);
				s[i].elements.add(x[i]);

				costSet = false;
				PicassoUtil.getPlanCosts(getPParent(), panel, planStrs[i], points);
				while (!costSet) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				for (int xx = 0; xx < noPts; xx++) {
					costs[i][xx] = cost[xx];
				}
			}
			
			double finalsumr = 0, finalsume = 0;
			double th = 1 + threshold / 100;
			for (int i = 0; i < n; i++) {
				boolean flag = false;
				for (int j = 0; j < n; j++) {
					// j -> swallower & i -> swallowee
					if (i != j) {
						boolean sw = true;
						double min = Double.MAX_VALUE;
						double max = Double.MIN_VALUE;
						double sumr = 0, sume = 0;
						for (int xx = 0; xx < noPts; xx++) {
							min = Double.MAX_VALUE;
							max = Double.MIN_VALUE;
							double ccc = costs[j][xx] - costs[i][xx];
							double cc = costs[j][xx] - th * costs[i][xx];
							if (cc > 0) {
								sw = false;
							}
							if(ccc > 0.0 && ccc/costs[i][xx] < min && sw)
								min = ccc/costs[i][xx];
							if(ccc > 0.0 && ccc/costs[i][xx] > max && sw)
								max = ccc/costs[i][xx];
							if(sw & ccc > 0.0)
							{
								sumr += costs[j][xx];
								sume += costs[i][xx];
							}
						}
						if (sw) {
							s[j].elements.add(x[i]);
							
							if(min < minInc)
								minInc = min;
							else if(max > maxInc)
								maxInc = max;
							// resultantIncrease += sumr;
							finalsumr += sumr;
							finalsume += sume;
							
							flag = true;
							swallow[j][i] = true;
						}
						sume = sumr = 0;
					}
				}
				if (!flag) {
					notSwallowed[i] = true;
				}
			}
			
			resultantIncrease = 100* (finalsumr - finalsume)/finalsume;
			if(minInc == Double.MAX_VALUE)
				minInc = 0;
			if(maxInc == Double.MIN_VALUE)
				maxInc = 0;
			if(maxInc == 0 && minInc == 0)
				resultantIncrease = 0;
			
			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}

			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
			// soln has the required soln
			System.out.println("# plans in Reduced Diagram : " + soln.size());
			int[] plan = new int[n];
			for (int i = 0; i < n; i++) {
				if (soln.contains(x[i])) {
					plan[i] = i;
				} else {
					for (Iterator it = soln.iterator(); it.hasNext();) {
						Integer xx = (Integer) it.next();
						if (swallow[xx.intValue()][i]) {
							plan[i] = xx.intValue();
							break;
						}
					}
				}
			}
			
			
			for (int i = 0; i < data.length; i++) {
				int pi = data[i].getPlanNumber();
				newData[i] = new DataValues();
				newData[i].setCard(data[i].getCard());
				newData[i].setPlanNumber(plan[pi]);
				// what to do????
				// newData[i].setCost(getCost(plan[pi], i));
			}
			ngdp.setDataPoints(newData);
			
			
			// Just checking the compression ratio for a reduced plan diagram
			
			ByteArrayOutputStream baos;
			ServerPacket packet = new ServerPacket();
			baos = new ByteArrayOutputStream();
			
			try
			{
				GZIPOutputStream gos = new GZIPOutputStream(baos);
				ObjectOutputStream oos = new ObjectOutputStream(gos);
				oos.writeObject(ngdp);
				oos.flush();
				gos.flush();
				gos.finish();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			packet.compressedDiagramPacket = baos.toByteArray();
			
			// End compression check
			
			return ngdp;
		}

		private DiagramPacket cgFpc(double th, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setMinCard(gdp.getMinCard());
			ngdp.setResolution(gdp.getResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setMinCost(gdp.getMinCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n = gdp.getMaxPlanNumber();
			// System.out.println(n);
			Set[] s = new Set[n];
			boolean[] notSwallowed = new boolean[n];
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			sel = gdp.getPicassoSelectivity();

			getPlanStrings(data,n);
			
			allPlanCosts = false;
			PicassoUtil.getAllPlanCosts(getPParent(), panel, planStrs);
			while (!allPlanCosts) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			
			for(int i = 0;i < data.length;i ++) {
				Integer xI = new Integer(i);
				int p = data[i].getPlanNumber();
				if (s[p] == null) {
					s[p] = new Set();
				}
				s[p].elements.add(xI);
				if (notSwallowed[p]) {
					continue;
				}
				
				double cost = costs[p][i];
				double lt = cost * (1 + threshold / 100);
				boolean flag = false;
				for(int j = 0;j < n;j ++) {
					if(p != j) {
						double cst = costs[j][i];//getCost(j,i);
						if(cst <= lt) {
							if(s[j] == null) {
								s[j] = new Set();
							}
							s[j].elements.add(xI);
							flag = true;
						}
					}
				}
				if (!flag) {
					notSwallowed[p] = true;
				}
			}
			
			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			int cct = 0;
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
					if(s[i].elements.size() != 0) {
						cct ++;
					}
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
			for (int i = 0; i < data.length; i++) {
				int p = data[i].getPlanNumber();
				Integer pI = new Integer(p);
				newData[i] = new DataValues();
				newData[i].setCard(data[i].getCard());
				
				double cost = costs[p][i];
				double lt = cost * (1 + threshold / 100);
				
				if(soln.contains(pI)) {
					newData[i].setPlanNumber(p);
					newData[i].setCost(data[i].getCost());
				} else {
					int plan = -1;
					double newcost = Double.MAX_VALUE; 
					for (int xx = 0; xx < n; xx++) {
						double cst = costs[xx][i];//getCost(xx,i);
						if (soln.contains(new Integer(xx)) && xx != p && cst <= lt) {
							// another redundant check for xx != p
							if(cst <= newcost) {
								newcost = cst;
								plan = xx;
							}
						}
					}
					newData[i].setPlanNumber(plan);
					newData[i].setCost(newcost);
				}
			}
			ngdp.setDataPoints(newData);
			setInfoValues(data, newData);
			return ngdp;
		}


		private void getPlanStrings(DataValues [] data, int n) {
			int[] pts = new int[n];
			Arrays.fill(pts, -1);
			for (int i = 0; i < data.length; i++) {
				int p = data[i].getPlanNumber();
				if (pts[p] == -1) {
					pts[p] = i;
				}
			}
			ArrayList points = new ArrayList();
			for (int i = 0; i < n; i++) {
				points.add(new Integer(pts[i]));
			}
			planStrSet = false;
			PicassoUtil.getAllPlanStrings(getPParent(), panel, points);
			while (!planStrSet) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		int dim;

		int[] index;

		private boolean ndCheck(int pEr, int pEe, float th, int d, int []r) {
			dim = d;
			index = new int[d];
			return check(pEr, pEe, th, d, r);
		}

		private boolean check(int er, int ee, float th, int d, int r[]) {
			if (d == 2) {
				return wptest(er, ee, th, r);
			} else {
				boolean ret = true;
				for (int i = 0; i < r[d-1]; i++) {
					index[d - 1] = i;
					ret = ret & check(er, ee, th, d - 1, r);
				}
				return ret;
			}
		}

		// Wedge followed by perimeter test
		private boolean wptest(int pEr, int pEe, float th, int r[]) {

			th = 1 + th / 100;

			// wedge test
			double lb = cost(pEr, 0, 0, r) - th * cost(pEe, 0, 0, r);
			double lt = cost(pEr, 0, r[1] - 1, r) - th * cost(pEe, 0, r[1] - 1, r);
			double rb = cost(pEr, r[0] - 1, 0, r) - th * cost(pEe, r[0] - 1, 0, r);
			double rt = cost(pEr, r[0] - 1, r[1] - 1, r) - th * cost(pEe, r[0] - 1, r[1] - 1, r);
			
			/*double lb1 = (cost(pEr, 0, 0, r) - cost(pEe, 0, 0, r))/cost(pEr, 0, 0, r);
			double lt1 = (cost(pEr, 0, r[1] - 1, r) - cost(pEe, 0, r[1] - 1, r))/cost(pEr, 0, r[1]-1, r);
			double rb1 = (cost(pEr, r[0] - 1, 0, r) - cost(pEe, r[0] - 1, 0, r))/cost(pEr, r[0]-1, 0, r);
			double rt1 = (cost(pEr, r[0] - 1, r[1] - 1, r) - cost(pEe, r[0] - 1, r[1] - 1, r))/cost(pEr, r[0]-1, r[1]-1, r);*/

			short ct = 0;
			if (lb <= 0) {
				ct++;
			}
			if (lt <= 0) {
				ct++;
			}
			if (rb <= 0) {
				ct++;
			}
			if (rt <= 0) {
				ct++;
			}
			if (ct != 4) {
				return false;
			}

			double ml1 = cost(pEr, 0, 1, r) - th * cost(pEe, 0, 1, r);
			double mr1 = cost(pEr, r[0] - 1, 1, r) - th * cost(pEe, r[0] - 1, 1, r);
			double mb1 = cost(pEr, 1, 0, r) - th * cost(pEe, 1, 0, r);
			double mt1 = cost(pEr, 1, r[1] - 1, r) - th * cost(pEe, 1, r[1] - 1, r);

			double ml2 = cost(pEr, 0, r[1] - 2, r) - th * cost(pEe, 0, r[1] - 2, r);
			double mr2 = cost(pEr, r[0] - 1, r[1] - 2, r) - th * cost(pEe, r[0] - 1, r[1] - 2, r);
			double mb2 = cost(pEr, r[0] - 2, 0, r) - th * cost(pEe, r[0] - 2, 0, r);
			double mt2 = cost(pEr, r[0] - 2, r[1] - 1, r) - th * cost(pEe, r[0] - 2, r[1] - 1, r);

			double sl1 = (ml1 - lb) / (sel[r[0] + 1] - sel[r[0] + 0]);
			double sl2 = (lt - ml2) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);

			double sr1 = (mr1 - rb) / (sel[r[0] + 1] - sel[r[0] + 0]);
			double sr2 = (rt - mr2) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);

			double st1 = (mt1 - lt) / (sel[1] - sel[0]);
			double st2 = (rt - mt2) / (sel[r[0] - 1] - sel[r[0] - 2]);

			double sb1 = (mb1 - lb) / (sel[1] - sel[0]);
			double sb2 = (rb - mb2) / (sel[r[0] - 1] - sel[r[0] - 2]);

			boolean incL = false;
			boolean incR = false;
			boolean incT = false;
			boolean incB = false;

			if (sl2 >= sl1) {
				incL = true;
			}
			if (sr2 >= sr1) {
				incR = true;
			}
			if (st2 >= st1) {
				incT = true;
			}
			if (sb2 >= sb1) {
				incB = true;
			}

			boolean lr = false;
			if (incL && incR) {
				lr = true;
			}

			boolean tb = false;
			if (incT && incB) {
				tb = true;
			}

			boolean top = false;
			boolean bottom = false;
			boolean left = false;
			boolean right = false;

			if (lb <= 0 && lt <= 0) {
				if (incL) {
					left = true;
				} else {
					if (sl1 <= 0 || sl2 >= 0) {
						left = true;
					}
				}
			}

			if (rb <= 0 && rt <= 0) {
				if (incR) {
					right = true;
				} else {
					if (sr1 <= 0 || sr2 >= 0) {
						right = true;
					}
				}
			}

			if (lb <= 0 && rt <= 0) {
				if (incB) {
					bottom = true;
				} else {
					if (sb1 <= 0 || sb2 >= 0) {
						bottom = true;
					}
				}
			}

			if (lt <= 0 && rt <= 0) {
				if (incT) {
					top = true;
				} else {
					if (st1 <= 0 || st2 >= 0) {
						top = true;
					}
				}
			}

			if (lr && top && bottom) {
				return true;
			}

			if (tb && left && right) {
				return true;
			}

			// Perimeter test
			if (!left) {
				for (int i = 0; i < r[1]; i++) {
					double c = cost(pEr, 0, i, r) - th * cost(pEe, 0, i, r);
					if (c > 0) {
						return false;
					}
				}
			}
			if (!right) {
				for (int i = 0; i < r[1]; i++) {
					double c = cost(pEr, r[0] - 1, i, r) - th * cost(pEe, r[0] - 1, i, r);
					if (c > 0) {
						return false;
					}
				}
			}
			if (!bottom) {
				for (int i = 0; i < r[0]; i++) {
					double c = cost(pEr, i, 0, r) - th * cost(pEe, i, 0, r);
					if (c > 0) {
						return false;
					}
				}
			}
			if (!top) {
				for (int i = 0; i < r[0]; i++) {
					double c = cost(pEr, i, r[1] - 1, r) - th * cost(pEe, i, r[1] - 1, r);
					if (c > 0) {
						return false;
					}
				}
			}
			if (lr || tb) {
				return true;
			}
			// lr
			boolean flag = true;
			if (!incL && !incR) {
				for (int i = 0; i < r[0]; i++) {
					double c1 = cost(pEr, i, 0, r) - th * cost(pEe, i, 0, r);
					double c2 = cost(pEr, i, 1, r) - th * cost(pEe, i, 1, r);
					double c3 = cost(pEr, i, r[1] - 2, r) - th * cost(pEe, i, r[1] - 2, r);
					double c4 = cost(pEr, i, r[1] - 1, r) - th * cost(pEe, i, r[1] - 1, r);

					double s1 = (c2 - c1) / (sel[r[0] + 1] - sel[r[0] + 0]);
					double s2 = (c4 - c3) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);
					if (!(s1 <= 0 || s2 >= 0)) {
						flag = false;
						break;
					}
				}
			}
			if (flag) {
				return true;
			}

			if (!incT && !incB) {
				for (int i = 0; i < r[1]; i++) {
					double c1 = cost(pEr, 0, i, r) - th * cost(pEe, 0, i, r);
					double c2 = cost(pEr, 1, i, r) - th * cost(pEe, 1, i, r);
					double c3 = cost(pEr, r[0] - 2, i, r) - th * cost(pEe, r[0] - 2, i, r);
					double c4 = cost(pEr, r[0] - 1, i, r) - th * cost(pEe, r[0] - 1, i, r);

					double s1 = (c2 - c1) / (sel[1] - sel[0]);
					double s2 = (c4 - c3) / (sel[r[0] - 1] - sel[r[0] - 2]);
					if (!(s1 <= 0 || s2 >= 0)) {
						return false;
					}
				}
			}
			return true;
		}

		boolean[] wedge;

		boolean[] perimeter;

		private double cost(int p, int i, int j, int r[]) {
			int ct = 0;
			index[1] = i;
			index[0] = j;
			/*for(int x = dim-1;x >= 0;x --) {
				ct = ct * r + index[x];
			}*/
			ct = index[0] * r[0] + index[1]; 
			if (costs[p][ct] != -1) {
				return costs[p][ct];
			}else {
				int ct1 = 0;
				for(int x = dim-1;x >= 2;x --) {
					ct1 = ct1 * r[x] + index[x];
				}
				ct1 *= r[0] * r[1];
				if(costs[p][ct1] == -1) {
					// wedge
					ArrayList points = new ArrayList();
					ct1 = 0;
					for(int x = dim-1;x >= 2;x --) {
						ct1 = ct1 * r[x] + index[x];
					}
					ct1 *= r[0] * r[1];
					// 4 corners
					int pp = ct1;
					points.add(new Integer(pp));
					pp = ct1 + r[0] - 1;
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 1);
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 1) + r[0] - 1;
					points.add(new Integer(pp));
					
					// left bottom wedge
					pp = ct1 + 1;
					points.add(new Integer(pp));
					pp = ct1 + r[0];
					points.add(new Integer(pp));
					
					// right bottom wedge
					pp = ct1 + r[0] - 2;
					points.add(new Integer(pp));
					pp = ct1 + r[0] + r[0] - 1;
					points.add(new Integer(pp));

					// left top wedge
					pp = ct1 + r[0] * (r[1] - 2);
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 1) + 1;
					points.add(new Integer(pp));

					// right top wedge
					pp = ct1 + r[0] * (r[1] - 2) + r[0] - 1;
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 1) + r[0] - 2;
					points.add(new Integer(pp));

					ArrayList tempp = new ArrayList();
					for(Iterator it = points.iterator();it.hasNext();) {
						Integer iii = ((Integer)it.next());
						int ii = iii.intValue();
						if(costs[p][ii] == -1) {
							tempp.add(iii);
						}
					}
					points.clear();
					points.addAll(tempp);

					
					costSet = false;
					PicassoUtil.getPlanCosts(getPParent(), panel, planStrs[p], points);
					while(!costSet) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					int x = 0;
					for(Iterator it = points.iterator();it.hasNext();) {
						int ii = ((Integer)it.next()).intValue();
						costs[p][ii] = cost[x];
						x ++;
					}
				} else {
					// perimeter
					ArrayList points = new ArrayList();
					ct1 = 0;
					for(int x = dim-1;x >= 2;x --) {
						ct1 = ct1 * r[x] + index[x];
					}
					ct1 *= r[0] * r[1];
					
					// Outer perimeter
					for(int x = 2;x < r[0]-2;x ++) {
						// bottom
						int pp = ct1 + x;
						points.add(new Integer(pp));
						// top
						pp = ct1 + x + r[0] * (r[1] - 1);
						points.add(new Integer(pp));
					}
					for(int x = 2;x < r[1]-2;x ++) {
						// left
						int pp = ct1 + x * r[0];
						points.add(new Integer(pp));
						// right
						pp = ct1 + r[0] * x + (r[0] - 1);
						points.add(new Integer(pp));
					}
					
					// Inner perimeter
					int pp = ct1 + r[0] + 1;
					points.add(new Integer(pp));
					pp = ct1 + r[0] + (r[0] - 2);
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 2) + 1;
					points.add(new Integer(pp));
					pp = ct1 + r[0] * (r[1] - 2) + r[0] - 2;
					points.add(new Integer(pp));
					
					for(int x = 2;x < r[0]-2;x ++) {
						// bottom
						pp = ct1 + r[0] + x;
						points.add(new Integer(pp));
						// top
						pp = ct1 + x + r[0] * (r[1] - 2);
						points.add(new Integer(pp));
					}
					for(int x = 2;x < r[1]-2;x ++) {
						// left
						pp = ct1 + x * r[0] + 1;
						points.add(new Integer(pp));
						// right
						pp = ct1 + r[0] * x + (r[0] - 2);
						points.add(new Integer(pp));
					}
					ArrayList tempp = new ArrayList();
					for(Iterator it = points.iterator();it.hasNext();) {
						Integer iii = ((Integer)it.next());
						int ii = iii.intValue();
						if(costs[p][ii] == -1) {
							tempp.add(iii);
						}
					}
					points.clear();
					points.addAll(tempp);
					costSet = false;
					PicassoUtil.getPlanCosts(getPParent(), panel, planStrs[p], points);
					while(!costSet) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					int x = 0;
					for(Iterator it = points.iterator();it.hasNext();) {
						int ii = ((Integer)it.next()).intValue();
						costs[p][ii] = cost[x];
						x ++;
					}
				}					
			}
			if(costs[p][ct] == -1) {
				System.out.println("Error. Please report this error to picasso@dsl.serc.iisc.ernet.in");
			}
			return costs[p][ct];
		}
	}

	// TO support SEER

	boolean costSet;

	double[] cost;
	double[][] costs;


	public void setCosts(ServerPacket fromServer) {
		ArrayList cst = (ArrayList) fromServer.hashmap.get("PlanCosts");
		cost = new double[cst.size()];
		int ct = 0;
		for (Iterator it = cst.iterator(); it.hasNext();) {
			cost[ct] = ((Double) it.next()).doubleValue();
			ct++;
		}
		costSet = true;
	}

	String[] planStrs;

	boolean planStrSet;
	boolean allPlanCosts;
	
	public void setPlans(ServerPacket fromServer) {
		ArrayList plans = (ArrayList) fromServer.hashmap.get("PlanStrings");
		planStrs = new String[plans.size()];
		int ct = 0;
		for (Iterator it = plans.iterator(); it.hasNext();) {
			planStrs[ct] = (String) it.next();
			ct++;
		}
		planStrSet = true;
	}
	
	public void setAllPlanCosts(ServerPacket fromServer) {
		costs = (double [][]) fromServer.hashmap.get("AllPlanCosts");
		allPlanCosts = true;
	}
	public void setInfoValues(DataValues []data, DataValues []newdata)
	{
		double min, max, avg;
		min = Double.MAX_VALUE;
		max = Double.MIN_VALUE;
		avg = 0;
		int noOfPoints=0;
		for(int i = 0; i < data.length; i++)
		{
			if(data[i].getPlanNumber() == newdata[i].getPlanNumber())
				continue;
			else
			{
				noOfPoints++;
				if((newdata[i].getCost() - data[i].getCost())/data[i].getCost() < min && (newdata[i].getCost() - data[i].getCost()) > 0.0)
					min = (newdata[i].getCost() - data[i].getCost())/data[i].getCost();
				else if((newdata[i].getCost() - data[i].getCost())/data[i].getCost() > max && (newdata[i].getCost() - data[i].getCost()) > 0.0)
					max = (newdata[i].getCost() - data[i].getCost())/data[i].getCost();
				if((newdata[i].getCost() - data[i].getCost())/data[i].getCost() > 0)
					avg += (newdata[i].getCost() - data[i].getCost())/data[i].getCost();
			}
		}
		if(noOfPoints==0)
			noOfPoints=1;
		//Dividing by number of points which were actually swallowed.
		avg /=noOfPoints;
		//avg /= data.length;
		if(min == Double.MAX_VALUE)
			min = 0;
		if(max == Double.MIN_VALUE)
			max = 0;
		
		minInc = min;
		maxInc = max;
		resultantIncrease = avg;		
		
	}
}



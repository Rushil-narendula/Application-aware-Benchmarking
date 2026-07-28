
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

package iisc.dsl.picasso.client.util;

import iisc.dsl.picasso.client.frame.PlanTreeFrame;
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.client.panel.ReducedPlanPanel;
import iisc.dsl.picasso.client.print.PicassoSave;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.RepaintManager;

//All the utility functions should be kept here...

public class PicassoUtil {

	/* Utility method to transform a Java color in
	 an array of rgb components between 0 and 1*/

	public static float[] colorToFloats(Color c){

		float[] rgb = new float[]{0.5f,0.5f,0.5f};  //init with gray
		if(c != null)
		{

			rgb[0] = (float) c.getRed()/255.0f;
			rgb[1] = (float) c.getGreen()/255.0f;
			rgb[2] = (float) c.getBlue()/255.0f;
		}

		return rgb;
	}
	/* original code
	public static boolean isCDPViolated(DiagramPacket gdp) {
		DataValues[] data = gdp.getData();

		double prevCost = data[0].getCost();
		int ncols = gdp.getResolution();
		int nrows;
		if ( gdp.getDimension() == 1 )
			nrows = 1;
		else
			nrows = ncols;
		//int previ=0;
		int prevj=0;
		for (int i=0; i < nrows; i++) {
			//Set the prevcost to the begin of the previous row..
			if ( i != 0 ) {
				prevCost = data[(i-1)*nrows].getCost();
				//previ = i-1;
				prevj = 0;
			}
			for (int j=0; j < nrows; j++) {
				if ( data[i*nrows+j].getCost() < prevCost * (PicassoConstants.COST_DOMINATION_THRESHOLD/100)) {
					return(true);
				}
				if ( i != 0 && data[i*nrows+j].getCost() < data[(i-1)*nrows+j].getCost() * (PicassoConstants.COST_DOMINATION_THRESHOLD/100) ) {
					return(true);
				}
				prevCost = data[i*nrows+j].getCost();
				//previ = i;
				prevj = j;
			}
		}
		return(false);
	}	  original code of isCDPViolated */
	//modified code which checks southwards, westwards and diagonally
	public static java.util.ArrayList isCDPViolated(DiagramPacket gdp) {
	//public static boolean isCDPViolated(DiagramPacket gdp) {
		java.util.ArrayList arrlist = new java.util.ArrayList();
		//first element gives true or false
		//second element given number of points violated
		arrlist.add("false");
		arrlist.add("0");
		if(gdp.getDimension() == 2)
			arrlist.add((gdp.getResolution(0)*gdp.getResolution(1))+"");//rss
		else if (gdp.getDimension() == 1)
			arrlist.add((gdp.getResolution(0))+"");//rss
		boolean vio_flag = false;
		int points_violated = 0;
	//change for cdp2 ends here	
		try{
			DataValues[] data = gdp.getData();

			double prevCost = data[0].getCost();
			int ncols = gdp.getResolution(0);
			
			
			float[] newselarr = gdp.getPicassoSelectivity();
			
			int nrows;
			if ( gdp.getDimension() == 1 )
				nrows = 1;
			else
				nrows = gdp.getResolution(1);//rss

			//added for debugging
			//try{
			//if(gdp.getDimension() > 1)
			//{
			//java.io.FileWriter tempfilewriter = new java.io.FileWriter("../debugtempfile.txt",true);
			//tempfilewriter.write(" \nwriting to the file query name:"+gdp.getQueryPacket().getQueryName()+"\n");
			//for(int i=0;i<nrows;i++)
			//{
			//	for(int j=0;j<nrows;j++)
			//	{
			//		double tempcost = data[i*nrows+j].getCost();
			//		tempfilewriter.write(tempcost+" ");
			//	}
			//	tempfilewriter.write("\n");
			//}
			//tempfilewriter.close();
			//}//end if(gdp.getdimension)...
			//}catch(Exception e)
			//{
			//	e.printStackTrace();
			//}
			//addition ends here
			for (int i=0; i < nrows; i++) {
				//Set the prevcost to the begin of the previous row..
				if ( i != 0 ) {
					prevCost = data[(i-1)*ncols].getCost();
					//previ = i-1;
					//commented 
					//prevj = 0;
					//commenting  ends here
				}
				for (int j=0; j < ncols; j++) {//rss

						//added 
					//if ( data[i*nrows+j].getCost() < prevCost * (PicassoConstants.COST_DOMINATION_THRESHOLD/100)) {
					//	System.out.println(" cdp: old algorithm cdp violated");
					//}
						Float xselecD ;
						Float yselecD ;
						float sucdiff = 0;
						sucdiff = (float)100.0/(float)nrows;
						xselecD = new Float((float)(i+1)/(float)nrows *(float)(100.0)-sucdiff/2.0);
						yselecD = new Float(((float)(j+1))/(float)ncols *(100.0)-sucdiff/2.0);
						 //int xselec = xselecD.intValue();
						 //int yselec = yselecD.intValue();
						float xselec = newselarr[i];
						float yselec = newselarr[nrows-1+j];//rss
						int flag_for_cost_violation = 0;
						double presentCost = data[i*ncols+j].getCost();
						//System.out.println("presentCost is "+presentCost);
						int count =1;
						double newprevCostdiagonal = 0.0;
						double newprevCostSouth = 0.0;
						double newprevCostWest = 0.0;
						if(i==0&&j==0)
							continue;

						if(i>0&&j>0)
						{
						newprevCostdiagonal = data[(i-1)*ncols+(j-1)].getCost();
						}
						if(i>0)
						{
						newprevCostSouth = data[(i-1)*ncols+(j)].getCost();
						}
						if(j>0)
						{
						newprevCostWest = data[(i)*ncols+(j-1)].getCost();
						}
						double relpercent = (PicassoConstants.COST_DOMINATION_THRESHOLD/100);
						//check diagonally
						boolean local_flag1 = false;
						while((i>0&&j>0&&newprevCostdiagonal > presentCost))
						{
							
							if((presentCost < (newprevCostdiagonal * relpercent)))
							{
								flag_for_cost_violation = 1;
								// System.out.println("CLIENT :: ");
								//In the diagram, xselec will become y selectivity and yselec will become x selectivity
								// System.out.println(" Cost("+yselec+","+xselec+") = "+presentCost);
								// System.out.println(" Cost("+newselarr[nrows+j-count]+","+newselarr[i-count] + ") = "+newprevCostdiagonal);
								//return(true);
								local_flag1 = true;
								vio_flag = true;
								points_violated++;
								break;
							}
							
							count++; //first execution becomes 2
							//System.out.println("count :"+count+", newprevCost is :"+newprevCostdiagonal+" ,i is :"+i+",j is :"+j);
							if((i-count) >0 && (j-count)>0)
							{
								newprevCostdiagonal = data[(i-count)*ncols+(j-count)].getCost();
							}
							else
							{
								break;
							}


						}
						if(local_flag1==true)
						{
							continue;
						}
						//end while for the diagonal case

						//check southwards
						boolean local_flag2 = false;
						count = 1;
						if(i>0)
						{
							//newprevCostdiagonal = data[(i-1)*nrows+(j-1)].getCost();
							//newprevCostWest = data[(i-1)*nrows+(j)].getCost();
							newprevCostSouth = data[(i-1)*ncols+(j)].getCost();
						}
						while((i>0&&newprevCostSouth> presentCost))
						{

							if((presentCost < (newprevCostSouth * relpercent)))
							{
								flag_for_cost_violation = 1;
								// System.out.println("CLIENT :: ");
//								In the diagram, xselec will become y selectivity and yselec will become x selectivity
								// System.out.println(" Cost("+yselec+","+xselec+") = "+presentCost);
								// System.out.println(" Cost("+yselec+","+newselarr[i-count]+") = "+newprevCostSouth);
								//return(true);
								local_flag2 = true;
								vio_flag = true;
								points_violated++;
								break;
							}
							count++; //first execution becomes 2
							//System.out.println("count :"+count+", newprevCost is :"+newprevCostdiagonal+" ,i is :"+i+",j is :"+j);
							if((i-count) >0 )
							{
								newprevCostSouth = data[(i-count)*ncols+(j)].getCost();
							}
							else
							{
								break;
							}
						}
						if(local_flag2==true)
						{
							continue;
						}
						//end while for south case
						//	check westwards
						boolean local_flag3 = false;
						count = 1;
						if(j>0)
						{
							//newprevCostdiagonal = data[(i-1)*nrows+(j-1)].getCost();
							newprevCostWest = data[(i)*ncols+(j-1)].getCost();
							
						}
						while((j>0&&newprevCostWest > presentCost))
						{


							if((presentCost < (newprevCostWest * relpercent)))
							{
								flag_for_cost_violation = 1;
								// System.out.println("CLIENT :: ");
//								In the diagram, xselec will become y selectivity and yselec will become x selectivity
								// System.out.println(" Cost("+yselec+","+xselec+") = "+presentCost);
								// System.out.println(" Cost("+newselarr[nrows+j-count]+","+(xselec)+") = "+newprevCostWest);
								//return(true);
								local_flag3 = true;
								vio_flag = true;
								points_violated++;
								break;
							}
							count++; //first execution becomes 2
							//System.out.println("count :"+count+", newprevCost is :"+newprevCostdiagonal+" ,i is :"+i+",j is :"+j);
							if((j-count) >0 )
							{
								newprevCostWest = data[(i)* ncols+(j-count)].getCost();
							}
							else
							{
								break;
							}
						}
						if(local_flag3==true)
						{
							continue;
						}//end west check case
					//addition ends here
					}//end inner for loop

			}//end outer for loop
			arrlist.clear();
			String viostr = "false";
			if(vio_flag)
			{
				viostr = "true";
			}
			String pts_viostr = points_violated+"";
			arrlist.add(viostr);
			arrlist.add(pts_viostr);
			if(gdp.getDimension() != 1)
				arrlist.add((gdp.getResolution(0)*gdp.getResolution(1))+"");//rss
			else if(gdp.getDimension() == 1)
				arrlist.add((gdp.getResolution(0))+"");//rss
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
			return(arrlist);
	}

	public static void checkCDP(DiagramPacket gdp) {
		DataValues[] data = gdp.getData();

		double prevCost = data[0].getCost();
		int ncols = gdp.getResolution(PicassoConstants.a[0]);//rss
		int nrows;
		if ( gdp.getDimension() == 1 )
			nrows = 1;
		else
			nrows = gdp.getResolution(PicassoConstants.a[1]);//rss
		int previ=0, prevj=0;

		float[] picSel = gdp.getPicassoSelectivity();

		for (int i=0; i < nrows; i++) {
			//Set the prevcost to the begin of the previous row..
			if ( i != 0 ) {
				prevCost = data[(i-1)*ncols].getCost();
				previ = i-1; prevj = 0;
			}
			for (int j=0; j < ncols; j++) {
				float xSelec = picSel[i];
				float ySelec = 0.0f;
				float pxSelec = picSel[previ];
				float pySelec = 0.0f;
				if ( gdp.getDimension() != 1 ) {
					ySelec = picSel[ncols+i]; //ma
					pySelec = picSel[ncols+previ];
				}
				if ( data[i*ncols+j].getCost() < prevCost ) {
					if ( gdp.getDimension() != 1 ) {
						//System.out.println("CDP Violation :: (" + xSelec + ", "
							//	+ ySelec + ")" + " CurCost: " + data[i*nrows+j].getCost()
							//	+ ", Prev Points: (" + pxSelec + "," + pySelec + ") PrevCost: " + prevCost);
					} else {
						//System.out.println("CDP Violation :: (" + xSelec + ")" + " CurCost: " + data[i*nrows+j].getCost()
							//	+ ", Prev Points: (" + pxSelec + ") PrevCost: " + prevCost);

					}
				}
				if ( i != 0 && data[i*ncols+j].getCost() < data[(i-1)*ncols+j].getCost() ) {
					pxSelec = picSel[i-1];
					if ( gdp.getDimension() != 1 ) {
						//System.out.println("CDP Violation :: (" + xSelec + ", "
							//+ ySelec + ")" + " CurCost: " + data[i*nrows+j].getCost()
							//+ ", Prev Points: (" + pxSelec + "," + ySelec + ") PrevCost: " + data[(i-1)*nrows+j].getCost());
					} else {
						//System.out.println("CDP Violation :: (" + xSelec + ")" + " CurCost: " + data[i*nrows+j].getCost()
								//+ ", Prev Points: (" + pxSelec + ") PrevCost: " + prevCost);
					}
				}
				prevCost = data[i*ncols+j].getCost();
				previ = i;
				prevj = j;
			}
		}
	}

	public static String getDBInfoString(DBSettings curDB) {
		if ( curDB == null )
			return("");

		return (curDB.getDbVendor() + ":" + curDB.getServerName() + ":" + curDB.getServerPort() +
				":" + curDB.getDbName()+ ":" + curDB.getSchema() + ":" + curDB.getUserName());
	}

	public static int getMaxPlans(DiagramPacket gdp) {
		int maxPlans = gdp.getMaxPlanNumber();
		int[] plans = new int[500];
		int curPlans = 0;

		DataValues[] data = gdp.getData();
		for (int i=0; i < plans.length; i++)
			plans[i] = 0;
		for (int i=0; i < data.length; i++) {
			int planNumber = data[i].getPlanNumber();
			if ( planNumber >= maxPlans )
				MessageUtil.CPrintErrToConsole("Error in max plans setting");
			else {
				plans[planNumber]++;
			}
		}
		for (int i=0; i < plans.length; i++) {
			if ( plans[i] != 0 )
				curPlans++;
		}
		return curPlans;
	}

	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(String s) {
		String ext = "";
		//String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	public static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = ClassLoader.getSystemResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			System.out.println("Couldn't find file: " + path);
			return null;
		}
	}

	public static void openURL(String url) {
		String osName = System.getProperty("os.name");
		try {
			if (osName.startsWith("Mac OS")) {
				Class fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL",
						new Class[] {String.class});
				openURL.invoke(null, new Object[] {url});
			}
			else if (osName.startsWith("Windows"))
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			else { //assume Unix or Linux
				String[] browsers = {
						"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (Runtime.getRuntime().exec(
							new String[] {"which", browsers[count]}).waitFor() == 0)
						browser = browsers[count];
				if (browser == null)
					throw new Exception("Could not find web browser");
				else
					Runtime.getRuntime().exec(new String[] {browser, url});
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getAccuracy(int value, int total, int res, int maxLength) {
		DecimalFormat df = new DecimalFormat("0.00");
		if ( res <= 100 )
			df.setMaximumFractionDigits(2);
		else {
			df = new DecimalFormat("0.000");
			df.setMaximumFractionDigits(3);
		}
		String accu = df.format((value * 100.0)/total);

		//This snippet is used for right-aligning the text
		int remainLength = maxLength - accu.length() - 2;
		if (remainLength > 0)
		{
			char [] blank = new char [remainLength];
			Arrays.fill(blank, ' ');
			accu = String.valueOf(blank) + accu;
		}
		return accu;
	}

	//apexp
	public static String getAccuracy2(double value, int total, int res, int maxLength) {
		DecimalFormat df;
		df = new DecimalFormat("0.000");
		df.setMaximumFractionDigits(3);
		String accu = df.format(value);

		//This snippet is used for right-aligning the text
		int remainLength = maxLength - accu.length() - 2;
		if (remainLength > 0)
		{
			char [] blank = new char [remainLength];
			Arrays.fill(blank, ' ');
			accu = String.valueOf(blank) + accu;
		}
		return accu;
	}
	//end apexp
	
	
	//commented by apexp//public static double computeGiniIndex(int[][] sortedPlan, int maxPlans, int total) {
	//added by apexp
	/*public static double computeGiniIndex(int[][] sortedPlan, int maxPlans, int total, DiagramPacket gdp, MainPanel parent, boolean local) {
		String distri=gdp.getQueryPacket().getDistribution();
		//int res=gdp.getResolution(); commented bcoz nowhere is it read
	//end apexp
		// Formula used for this is 2/n(1*P1 + 2*P2 + 3*P3...)
		if(maxPlans == 1)
			return 0.0;
		double summation = 0.0;
		//apexp
		
		//set maxPlans correctly - remove 0 sized plans from the maxPlan count; 
		//at least 1 plan will be there; so upto i>0
		
		int i;
		if(distri.startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION) && parent.getCurrentTab() instanceof iisc.dsl.picasso.client.panel.ReducedPlanPanel)
		{
			for(i=maxPlans-1;i>0;i--)
			{
				if(sortedPlan[1][sortedPlan[2][i]]!=0) break;
			}
			maxPlans=i+1;
		//System.out.println(maxPlans);
		}
		
		//end apexp
		
		for (int i=0; i < maxPlans; i++) {
			//apexp
			int numPlans;
			double val=0.0;
			
			double areasum = 0;
			for(int j = 0; j < sortedPlan[1].length; j++)
				areasum += sortedPlan[1][j];
			
			double areasum = 1;
			double volsum = 1;
			areasum = 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
			if(gdp.getDimension()>1)
				areasum *= 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
			
			for(int j = 0; j < gdp.getDimension(); j++)
			{
				volsum *= 100*(gdp.getQueryPacket().getEndPoint(j) - gdp.getQueryPacket().getStartPoint(j));
			}
			
			if(distri.startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION)) // exponential
			{	
			//	if(gdp.getDimension()<=2 || parent.getCurrentTab() instanceof iisc.dsl.picasso.client.panel.ReducedPlanPanel )
					val= sortedPlan[1][sortedPlan[2][i]]/Math.pow(100, gdp.getDimension());//(100.0 * gdp.getResolution(0) * gdp.getResolution(1));
					val/=100;
					if(gdp.getDimension() ==1)
					{
						val=(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
					}
					else if(gdp.getDimension() ==2)
					{
						if(local)
							val=(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
						else
							val=(double)sortedPlan[1][sortedPlan[2][i]]/volsum;
					}
					summation += (val/100) * (val/100);
					if(i == maxPlans -1)
						return 1-summation;
					else
						continue;
					else if(gdp.getDimension() ==3)
					{
						if(local)
							val=100*(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
						else
							val=100*(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
					}
					if(gdp.getDimension() ==4)
					{
						if(local)
							val=100*(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
						else
							val=100*(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
					}
				else
				{
					val=sortedPlan[1][sortedPlan[2][i]]/(1000000.0);
					int q=1; //gdp.getDimension()-2;
					while(q>0)
					{
						val/=gdp.getResolution();
						q--;
					}
				}
			}
			
			else
			{	
				numPlans= sortedPlan[1][sortedPlan[2][i]];
				val = (double)numPlans*100/(double)total;
			}
			//end apexp
			summation += ((val*(maxPlans-i))/100);
			//MessageUtil.CPrintToConsole(summation + ", " + numPlans + " VAL :: " + val + ", " + total);
		}
		summation -= ((maxPlans+1)*0.5);
		double gini = (double)2*summation/(double)maxPlans;
		return gini;
	}*/
	public static double computeGiniIndex(int[][] sortedPlan, int maxPlans, int total, DiagramPacket gdp, MainPanel parent, boolean local) 
	{
		// Totally rewritten by ma
		String distri=gdp.getQueryPacket().getDistribution();
		// Formula used for this is 1-(p1*p1 + p2*p2 + ...)
		
		
		if(maxPlans == 1)
			return 0.0;
		double summation = 0.0;
		
		for (int i=0; i < maxPlans; i++) {
			int numPlans;
			double val=0.0;
			
			double areasum = 1;
			double volsum = 1;
			areasum = 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
			if(gdp.getDimension()>1)
				areasum *= 100*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1]) - gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
			
			for(int j = 0; j < gdp.getDimension(); j++)
			{
				volsum *= 100*(gdp.getQueryPacket().getEndPoint(j) - gdp.getQueryPacket().getStartPoint(j));
			}
			
			if(distri.startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
			{	
				boolean scaleupflag = false;
				for(int k = 0; k < gdp.getDimension(); k++)
				{
					if(gdp.getQueryPacket().getEndPoint(k) - gdp.getQueryPacket().getStartPoint(k) < 0.05 && gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
						scaleupflag = true;
				}
				
				if(scaleupflag)
				{
					areasum *= 100;
					volsum *= Math.pow(10, gdp.getDimension());
				}
				
				if(gdp.getDimension() ==1)
				{
					val=(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
				}
				else 
				{
					if(local)
						val=(double)sortedPlan[1][sortedPlan[2][i]]/areasum;
					else
						val=(double)sortedPlan[1][sortedPlan[2][i]]/volsum;
				}
			}
			else
			{	
				numPlans= sortedPlan[1][sortedPlan[2][i]];
				val = (double)numPlans*100/(double)total;
			}
			summation += (val/100) * (val/100);
		}
		return 1-summation;
	}

	/** The speed and quality of printing suffers dramatically if
	 *  any of the containers have double buffering turned on.
	 *  So this turns if off globally.
	 *  @see enableDoubleBuffering
	 */
	public static void disableDoubleBuffering (Component c)
	{
		RepaintManager currentManager = RepaintManager.currentManager (c);
		currentManager.setDoubleBufferingEnabled (false);
	}

	/** Re-enables double buffering globally. */

	public static void enableDoubleBuffering (Component c)
	{
		RepaintManager currentManager = RepaintManager.currentManager (c);
		currentManager.setDoubleBufferingEnabled (true);
	}

	public static void displayCompiledTree(MainPanel mPanel, PicassoPanel panel, String[] infoStr, int resolution, double selec1, double selec2, int planNumber) {
		//Get the fields data structure
		ClientPacket values = mPanel.getClientPacket();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();
		/*double s1 = (double)selec1*100/resolution;
		double s2 = (double)selec2*100/resolution;*/

		// Build the message to be sent
		//String sendString = MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE);
		//sendString += "&" + MessageIds.PLAN_NUMBER + "=" + planStr;

		values.setMessageId(MessageIds.GET_COMPILED_PLAN_TREE);
		values.setPlanNumbers(""+planNumber);
		Vector v = values.getDimensions();

		Hashtable table = new Hashtable();
		table.put(v.elementAt(0), Double.toString(selec1));
		table.put(v.elementAt(1), Double.toString(selec2));

		// Hack need to actually rebuild it...
		values.put("Info", infoStr);
		values.setCompileTreeValues(table);

		//MessageUtil.CPrintToConsole(infoStr[1] + " VALUES :: " + selec1 + " " + selec2);
		//MessageUtil.CPrintToConsole("DIMENSIONS :: " + v.elementAt(0) + " " + v.elementAt(1));

		//values.setPlanNumbers(planStr);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	public static void getAbsPlan(MainPanel mPanel, PicassoPanel panel, String[] infoStr, int resolution, double selec1, double selec2, int planNumber) {
		//Get the fields data structure
		ClientPacket values = mPanel.getClientPacket();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();

		values.setMessageId(MessageIds.GET_ABSTRACT_PLAN);
		values.setPlanNumbers(""+planNumber);
		Vector v = values.getDimensions();

		Hashtable table = new Hashtable();
		table.put(v.elementAt(0), Double.toString(selec1));
		table.put(v.elementAt(1), Double.toString(selec2));

		// Hack need to actually rebuild it...
		values.put("Info", infoStr);
		values.setCompileTreeValues(table);

		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	public static void displayTreeAD(MainPanel mPanel, PicassoPanel panel, String planStr) {
		
		//create a fake ServerPacket and fill up trees and messageID as these are the only fields read in PlanTreeFrame
		
		Vector alltrees = mPanel.getServerPacket().trees;
		
		
		//put the required tree from alltrees to tree
		String imageDir = PicassoConstants.INPUT_IMAGE_FOLDER;
		JFileChooser chooser = new JFileChooser(new File(imageDir));
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);		
		chooser.setAcceptAllFileFilterUsed(false);
		    
	    if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) {	    
	    	return;
	    }		
		
		//String path = chooser.getCurrentDirectory() + "";
		String fName = chooser.getSelectedFile().getAbsolutePath() + System.getProperty("file.separator");
		int sp[][] = panel.getSortedPlan();
		
		for(int planNum = 0;planNum<sp[0].length;planNum++)
		{
			ServerPacket mySvrPkt = new ServerPacket();
			Vector tree = new Vector();
			planStr = planNum +"";
			StringTokenizer st = new StringTokenizer(planStr, ",");
			int[] planNumbers = new int[st.countTokens()];
			int i=0;
			while ( st.hasMoreTokens() ) {
				String str = st.nextToken(); 
				planNumbers[i++] = Integer.parseInt(str);
			}
	
			tree.add(new Integer(planNumbers.length));
	
			try {
				for (i=0; i < planNumbers.length; i++) {
					tree.add(new Integer(planNumbers[i]));
					tree.add(alltrees.get(2+planNumbers[i]*2));
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			mySvrPkt.trees = tree;
			
			mySvrPkt.messageId = MessageIds.GET_PLAN_TREE;
			
			//create blank infoStr[3] and put it in ClientPacket as Pgraph reads it and will crib if it is null.
			String infoStr[] = new String[3];
			infoStr[0] = "";
			infoStr[1] = "";
			infoStr[2] = "";
			mPanel.getClientPacket().put("Info", infoStr);
			PlanTreeFrame planTree = new PlanTreeFrame(mPanel.getClientPacket(), panel.getSortedPlan(), mySvrPkt);
			planTree.setVisible(true);
			//planTree.setAlwaysOnTop(true);
			planTree.setSize(1920, 1200);			
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
			PicassoSave.saveAD(planTree,sp[0][planNum],fName);
		
		}
		
//		ClientPacket values = mPanel.getClientPacket();
//		String serverName = mPanel.getServerName();
//		int serverPort = mPanel.getServerPort();
//
//		// Build the message to be sent
//		//String sendString = MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE);
//		//sendString += "&" + MessageIds.PLAN_NUMBER + "=" + planStr;
//		String[] infoStr = new String[3];
//
//		values.setMessageId(MessageIds.GET_PLAN_TREE);
//		values.setPlanNumbers(planStr);
//		for (int i=0; i < infoStr.length; i++) {
//			infoStr[i] = "";
//		}
//		values.put("Info", infoStr);
//		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	public static void displayTree(MainPanel mPanel, PicassoPanel panel, String planStr) {
		
		//create a fake ServerPacket and fill up trees and messageID as these are the only fields read in PlanTreeFrame
		ServerPacket mySvrPkt = new ServerPacket();
		Vector alltrees = mPanel.getServerPacket().trees;
		Vector tree = new Vector();
		
		//put the required tree from alltrees to tree
		
		StringTokenizer st = new StringTokenizer(planStr, ",");
		int[] planNumbers = new int[st.countTokens()];
		int i=0;
		while ( st.hasMoreTokens() ) {
			String str = st.nextToken();
			planNumbers[i++] = Integer.parseInt(str);
		}

		tree.add(new Integer(planNumbers.length));

		for (i=0; i < planNumbers.length; i++) {
			tree.add(new Integer(planNumbers[i]));
			tree.add(alltrees.get(2+planNumbers[i]*2));
		}
		mySvrPkt.trees = tree;
		
		mySvrPkt.messageId = MessageIds.GET_PLAN_TREE;
		
		//create blank infoStr[3] and put it in ClientPacket as Pgraph reads it and will crib if it is null.
		String infoStr[] = new String[3];
		infoStr[0] = "";
		infoStr[1] = "";
		infoStr[2] = "";
		mPanel.getClientPacket().put("Info", infoStr);
		PlanTreeFrame planTree = new PlanTreeFrame(mPanel.getClientPacket(), panel.getSortedPlan(), mySvrPkt);
		planTree.setVisible(true);

//		ClientPacket values = mPanel.getClientPacket();
//		String serverName = mPanel.getServerName();
//		int serverPort = mPanel.getServerPort();
//
//		// Build the message to be sent
//		//String sendString = MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE);
//		//sendString += "&" + MessageIds.PLAN_NUMBER + "=" + planStr;
//		String[] infoStr = new String[3];
//
//		values.setMessageId(MessageIds.GET_PLAN_TREE);
//		values.setPlanNumbers(planStr);
//		for (int i=0; i < infoStr.length; i++) {
//			infoStr[i] = "";
//		}
//		values.put("Info", infoStr);
//		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	//added for multiplan tree
	public static void displayMultiPlanTree(MainPanel mPanel, PicassoPanel panel, String[] messageStrArr,double selec1,double selec2,int plannumber,String optionValue,String optLevelValue,String otherDBQueryText) {
		ClientPacket values = mPanel.getClientPacket();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();

		// Build the message to be sent
		//String sendString = MessageUtil.buildMessage(values, MessageIds.GET_PLAN_TREE);
		//sendString += "&" + MessageIds.PLAN_NUMBER + "=" + planStr;
		String[] infoStr = new String[3];
		
		// AP: will be 0,1 or 1,0 depending upon which order is
		// chosen in the Display Dimensions in the Query template tab
		java.util.Vector v = values.getDimensions();

		Hashtable table = new Hashtable();
		//AP: assign the selectivities to the required dimensions
		table.put(v.elementAt(0), Double.toString(selec1));
		table.put(v.elementAt(1), Double.toString(selec2));
		
		values.setMessageId(MessageIds.GET_MULTI_PLAN_TREES);
		values.setPlanNumbers(messageStrArr[0]);
		values.put("messageStrArr",messageStrArr);
		values.setCompileTreeValues(table);
		DBSettings tempdbsetsnew = new DBSettings();
		tempdbsetsnew.setDbName(mPanel.getDBSettings().get(optionValue).getDbName());
		tempdbsetsnew.setDbVendor(mPanel.getDBSettings().get(optionValue).getDbVendor());
		tempdbsetsnew.setInstanceName(mPanel.getDBSettings().get(optionValue).getInstanceName());
		tempdbsetsnew.setOptLevel(mPanel.getDBSettings().get(optionValue).getOptLevel());
		tempdbsetsnew.setPassword(mPanel.getDBSettings().get(optionValue).getPassword());
		tempdbsetsnew.setSchema(mPanel.getDBSettings().get(optionValue).getSchema());
		tempdbsetsnew.setServerPort(mPanel.getDBSettings().get(optionValue).getServerPort());
		tempdbsetsnew.setServerName(mPanel.getDBSettings().get(optionValue).getServerName());
		tempdbsetsnew.setUserName(mPanel.getDBSettings().get(optionValue).getUserName());
		
		//DBSettings tempdbsets = mPanel.getDBSettings().get(optionValue);
		tempdbsetsnew.setOptLevel(optLevelValue);
		values.put("origdbsettings",values.getDBSettings());
		values.put("otherdbsettings",tempdbsetsnew);
		values.put("otherdbquerytext",otherDBQueryText);
		
		for (int i=0; i < infoStr.length; i++) {
			infoStr[i] = "";
		}
		values.put("Info", infoStr);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	//addition for multi plan tree ends here
	
	// ADDED for SEER
	public static void getPlanCosts(MainPanel mPanel, PicassoPanel panel, String plan, ArrayList points) {
		DiagramPacket gdp = mPanel.getDiagramPacket();
		int no = gdp.getMaxPlanNumber();

		ClientPacket values = mPanel.getClientPacket();
		values.attrTypes = gdp.getAttributeTypes();
		values.constants = gdp.getConstants();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();

		// Build the message to be sent
		String[] infoStr = new String[3];

		String planStr = Integer.toString(no); 
		values.setMessageId(MessageIds.GET_PLAN_COSTS);
		values.setPlanNumbers(planStr);
		for (int i=0; i < infoStr.length; i++) {
			infoStr[i] = "";
		}
		values.put("Info", infoStr);
		values.put("PlanPoints",points);
		values.put("PlanString",plan);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}

	public static void getAllPlanStrings(MainPanel mPanel, PicassoPanel panel, ArrayList points) {
		DiagramPacket gdp = mPanel.getDiagramPacket();
		int no = gdp.getMaxPlanNumber();

		ClientPacket values = mPanel.getClientPacket();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();

		// Build the message to be sent
		String[] infoStr = new String[3];

		String planStr = Integer.toString(no); 
		values.setMessageId(MessageIds.GET_PLAN_STRINGS);
		values.setPlanNumbers(planStr);
		for (int i=0; i < infoStr.length; i++) {
			infoStr[i] = "";
		}
		values.put("Info", infoStr);
		values.put("PlanPoints",points);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	
	public static void setTheSettingInServer(MainPanel mPanel, PicassoPanel panel, String settingName, String settingValue) {
		ClientPacket values = mPanel.getClientPacket();
		//ClientPacket values = new ClientPacket();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();
		values.setMessageId(MessageIds.SET_SERVER_SETTING);
		values.put("SettingName", settingName);
		values.put("SettingValue",settingValue);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, mPanel.getPlanPanel());
	}
	public static void setTheSettingInServer2(MainPanel mPanel, PicassoPanel panel, String settingName, String settingValue) {
		//ClientPacket values = mPanel.getClientPacket();
		ClientPacket values = new ClientPacket();
		values.setClientId("0");
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();
		values.setMessageId(MessageIds.SET_SERVER_SETTING);
		values.put("SettingName", settingName);
		values.put("SettingValue",settingValue);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, mPanel.getPlanPanel());
	}
	public static void getAllPlanCosts(MainPanel mPanel, ReducedPlanPanel panel, String[] planStrs) {
		DiagramPacket gdp = mPanel.getDiagramPacket();
		int no = gdp.getMaxPlanNumber();

		ClientPacket values = mPanel.getClientPacket();
		values.attrTypes = gdp.getAttributeTypes();
		values.constants = gdp.getConstants();
		String serverName = mPanel.getServerName();
		int serverPort = mPanel.getServerPort();

		// Build the message to be sent
		String[] infoStr = new String[3];

		String planStr = Integer.toString(no); 
		values.setMessageId(MessageIds.GET_ALL_PLAN_COSTS);
		values.setPlanNumbers(planStr);
		for (int i=0; i < infoStr.length; i++) {
			infoStr[i] = "";
		}
		values.put("Info", infoStr);
		values.put("PlanStrings",planStrs);
		MessageUtil.sendMessageToServer(serverName, serverPort, values, panel);
	}
	public static int[] slicePlans(DiagramPacket dp)
	{
		int maxplanno=0;
		boolean []map = new boolean[1000]; // assuming number of plans in a slice is never more than 1000
		DataValues data[] = dp.getData();
		for(int i = 0; i < data.length; i++)
		{
			map[data[i].getPlanNumber()] = true;
		}
		for(int i = 0; i < map.length; i++)
			if(map[i] == true)
				maxplanno++;
		
		int k = 0;
		int a[] = new int[maxplanno];
		for(int i = 0; i < map.length; i++)
			if(map[i] == true)
				a[k++] = i;	
		
		return a;
	}
	
}

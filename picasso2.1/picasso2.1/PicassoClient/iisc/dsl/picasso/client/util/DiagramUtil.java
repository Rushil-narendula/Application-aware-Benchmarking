
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

import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.Hashtable;

import visad.AxisScale;
import visad.ColorControl;
import visad.ConstantMap;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.Set;
import visad.VisADException;

public class DiagramUtil {

	public static double[][] getSamplesForPlan(int type, MainPanel panel, DiagramPacket gdp) {
//   	 Create a flat array for plans
		double[][] flatSamples = null;
		int NROWS=1;
		int maxConditions = gdp.getDimension();
		if(maxConditions!=1)
			NROWS = gdp.getResolution(PicassoConstants.a[1]);//rss
		int NCOLS = gdp.getResolution(PicassoConstants.a[0]); //rss
		int index = 0;
		int[][] sortedPlan;
		
		if ( type == 1 )
			sortedPlan = panel.getExecSortedPlan();
		else
			sortedPlan = panel.getSortedPlan();
		
		DataValues[] data = gdp.getData();
		
		if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
			if ( maxConditions == 1 ) {
				//1d fix added by modifiying addValue:1st Feb 2011
				/*if(NCOLS <= 100)
				{
					flatSamples = new double[1][NCOLS*2 + 4];
					
					for (int i=0; i < 2; i++)
						for (int j=0; j < NCOLS + 2; j++) {
							if(j == 0)
								flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
							else if(j == NCOLS + 1)
								flatSamples[0][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
							else
								flatSamples[0][index] = sortedPlan[0][data[j - 1].getPlanNumber()];
							index++;
						}
				}
				else*/ 
				{
					flatSamples = new double[1][NCOLS*2];
					
					for (int i=0; i < 2; i++)
						for (int j=0; j < NCOLS; j++) {
							flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
							index++;
						}
				}
			}
			else
			{
				flatSamples = new double[1][(NCOLS + 2) * (NROWS + 2)];
				for (int i=0; i < NROWS + 2; i++)
					for (int j=0; j < NCOLS + 2; j++) {
						if(j > 0 && j <= NCOLS && i > 0 && i <= NROWS)
							flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS+j-1].getPlanNumber()];
						else if(j == 0)
						{
							if(i == 0)
								flatSamples[0][index] = sortedPlan[0][data[0].getPlanNumber()];
							else if(i == NROWS + 1)
								flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS].getPlanNumber()];
							else 
								flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS].getPlanNumber()];
						}
						else if(j == NCOLS + 1)
						{
							if(i == 0)
								flatSamples[0][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
							else if(i == NROWS + 1)
								flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS + j - 2].getPlanNumber()];
							else 
								flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS + j - 2].getPlanNumber()];
						}
						else if(i == 0)
						{
							flatSamples[0][index] = sortedPlan[0][data[j-1].getPlanNumber()];
						}
						else if(i == NROWS + 1)
						{
							flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS+j-1].getPlanNumber()];
						}
						index++;
					}
			}
		}
		else
		{
			// EXPO
			if ( maxConditions == 1 ) {
				flatSamples = new double[1][NCOLS*2];
				for (int i=0; i < 2; i++)
					for (int j=0; j < NCOLS; j++) {
						flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
						index++;
					}
			}
			
			else
			{
				/*for(int i = NROWS -1; i >=0; i--)
				{
					for(int j = 0; j < NCOLS; j++)
						System.out.print(data[i*NCOLS+j].getPlanNumber() + "  ");
					System.out.println("");
				}*/
				flatSamples = new double[1][(NCOLS) * (NROWS)];
				for (int i=0; i < NROWS; i++)
					for (int j=0; j < NCOLS; j++) {
						flatSamples[0][index] = sortedPlan[0][data[i*NCOLS+j].getPlanNumber()];
						index++;
					}
			}
		}
		return flatSamples;
   }
   
   public static double[][] getSamplesForReducedPlan(MainPanel panel, DiagramPacket gdp) {
   	double[][] flatSamples;
   	int NROWS=1;
   	int NCOLS;
   	if(gdp.getDimension()!=1)
  	 	NROWS = gdp.getResolution(PicassoConstants.a[1]);
   	NCOLS = gdp.getResolution(PicassoConstants.a[0]);
   	DiagramPacket ngdp = panel.getReducedDiagramPacket();
   	
   	//MessageUtil.CPrintToConsole(gdp.getMaxPlan() + " In Reduced Panel :: " + ngdp.getMaxPlan());
   	DataValues[] data = null;
	try {
		data = ngdp.getData();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
	}
	if(ngdp == null)
		return null;
	
   	int index = 0;
	int[][] sortedPlan = panel.getSortedPlan();
	int maxConditions = gdp.getDimension();
		
	/*	if ( maxConditions == 1 ) {
			flatSamples = new double[1][NCOLS * 2];
			for (int i=0; i < 2; i++)
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
				//MessageUtil.CPrintToConsole("Val : " + index + " " + flatSamples[0][index]);
					index++;
				}
		} else {
			flatSamples = new double[1][NCOLS * NROWS];
			for (int i=0; i < NROWS; i++)
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = sortedPlan[0][data[i*NCOLS+j].getPlanNumber()];
				//MessageUtil.CPrintToConsole("Val : " + index + " " + flatSamples[0][index]);
					index++;
				}
		}*/
	if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.UNIFORM_DISTRIBUTION))
	{
		if ( maxConditions == 1 ) 
		{
			if(NCOLS <= 100)
			{
				flatSamples = new double[1][NCOLS*2 + 4];
				
				for (int i=0; i < 2; i++)
					for (int j=0; j < NCOLS + 2; j++) {
						if(j == 0)
							flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
						else if(j == NCOLS + 1)
							flatSamples[0][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
						else
							flatSamples[0][index] = sortedPlan[0][data[j - 1].getPlanNumber()];
						index++;
					}
			}
			else 
			{
				flatSamples = new double[1][NCOLS*2];
				
				for (int i=0; i < 2; i++)
					for (int j=0; j < NCOLS; j++) {
						flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
						index++;
					}
			}
		}
		else
		{
			flatSamples = new double[1][(NCOLS + 2) * (NROWS + 2)];
			for (int i=0; i < NROWS + 2; i++)
				for (int j=0; j < NCOLS + 2; j++) {
					if(j > 0 && j <= NCOLS && i > 0 && i <= NROWS)
						flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS+j-1].getPlanNumber()];
					else if(j == 0)
					{
						if(i == 0)
							flatSamples[0][index] = sortedPlan[0][data[0].getPlanNumber()];
						else if(i == NROWS + 1)
							flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS].getPlanNumber()];
						else 
							flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS].getPlanNumber()];
					}
					else if(j == NCOLS + 1)
					{
						if(i == 0)
							flatSamples[0][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
						else if(i == NROWS + 1)
							flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS + j - 2].getPlanNumber()];
						else 
							flatSamples[0][index] = sortedPlan[0][data[(i-1)*NCOLS + j - 2].getPlanNumber()];
					}
					else if(i == 0)
					{
						flatSamples[0][index] = sortedPlan[0][data[j-1].getPlanNumber()];
					}
					else if(i == NROWS + 1)
					{
						flatSamples[0][index] = sortedPlan[0][data[(i-2)*NCOLS+j-1].getPlanNumber()];
					}
					index++;
				}
		}
	}
	else
	{
		// EXPO
		if ( maxConditions == 1 ) {
			flatSamples = new double[1][NCOLS*2];
			for (int i=0; i < 2; i++)
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = sortedPlan[0][data[j].getPlanNumber()];
					index++;
				}
		}
		
		else
		{
			flatSamples = new double[1][(NCOLS) * (NROWS)];
			for (int i=0; i < NROWS; i++)
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = sortedPlan[0][data[i*NCOLS+j].getPlanNumber()];
					index++;
				}
		}
	}
	return flatSamples;
   	}
   
   public static double[][] getExtraPlanSamples(DiagramPacket gdp, double[][] flatSamples) {
		int NROWS=1;
	    if(gdp.getDimension()!=1)
	   		NROWS = gdp.getResolution(PicassoConstants.a[1]);//rss
		int NCOLS = gdp.getResolution(PicassoConstants.a[0]);//rss
		double[][] newSamples;
		int index = 0;
		
		int CmultipleFactor = 100/NCOLS;
		if ( gdp.getDimension() == 1 ) {
			newSamples = new double[1][(NCOLS*CmultipleFactor)*2];
			for (int k=0; k < 2; k++) {
				for (int j=0; j < NCOLS; j++) {
					for (int i=0; i < CmultipleFactor; i++) {
						newSamples[0][index++] = flatSamples[0][k*NCOLS+j];
					}
				}
			}
		} else {
			int RmultipleFactor = 100/NROWS;
			newSamples = new double[1][NROWS*RmultipleFactor*NROWS*CmultipleFactor];
			
			for (int i=0; i < NROWS; i++) {
				for (int l=0; l < RmultipleFactor; l++) {
					for (int j=0; j < NCOLS; j++) {
						for (int k=0; k < CmultipleFactor; k++) {
							newSamples[0][index++] = flatSamples[0][i*NCOLS+j];
						}
					}
				}
			}
		}
		return newSamples;
   }
   
   public static double[][] getExtra3DSamples(DiagramPacket gdp, double[][] flatSamples) {
		int NROWS = gdp.getResolution(1);//rss
		int NCOLS = gdp.getResolution(0);
		double[][] newSamples;
		int index = 0;
		
		int RmultipleFactor = 100/NROWS;
		int CmultipleFactor = 100/NCOLS;
		newSamples = new double[2][NROWS*RmultipleFactor*NCOLS*CmultipleFactor];
		
		for (int i=0; i < NROWS; i++) {
			for (int l=0; l < RmultipleFactor; l++) {
				for (int j=0; j < NCOLS; j++) {
					for (int k=0; k < CmultipleFactor; k++) {
						newSamples[0][index] = flatSamples[0][i*NCOLS+j];
						newSamples[1][index] = flatSamples[1][i*NCOLS+j];
						index++;
					}
				}
			}
		}
		return newSamples;
	}
   
   public static double[][] getSamplesForCost(MainPanel panel, int[][] sortedPlan, DiagramPacket gdp) {
//   	 Create a flat array for plans
		double[][] flatSamples = null;
		
		int NROWS=1;
		if(gdp.getDimension()!=1)
			NROWS = gdp.getResolution(PicassoConstants.a[1]);//rss
		int NCOLS = gdp.getResolution(PicassoConstants.a[0]);//rss
		DataValues[] data = gdp.getData();
		
		double maxCost = gdp.getMaxCost();
		
		int index = 0;
		//int[][] sortedPlan = panel.getSortedPlan();
		int maxConditions = gdp.getDimension();
		if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
			if ( maxConditions == 1 ) 
			{
				flatSamples = new double[2][NCOLS*2 + 4];
				
				for (int i=0; i < 2; i++)
				{
					for (int j=0; j < NCOLS + 2; j++) 
					{
						if(j == 0)
						{
							if(i == 0)
								flatSamples[0][index] = data[j].getCost()/maxCost;
							else
								flatSamples[0][index] = 0.05;
							flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
						}
						else if(j == NCOLS + 1)
						{
							if(i == 0)
								flatSamples[0][index] = data[j-2].getCost()/maxCost;
							else
								flatSamples[0][index] = 0.05;
							flatSamples[1][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
						}
						else
						{
							if(i == 0)
								flatSamples[0][index] = data[j-1].getCost()/maxCost;
							else
								flatSamples[0][index] = 0.05;
						
							flatSamples[1][index] = sortedPlan[0][data[j - 1].getPlanNumber()];
						}
						index++;
					}
				}
			}
			else
			{
				flatSamples = new double[2][(NCOLS + 2) * (NROWS + 2)];
				for (int i=0; i < NROWS + 2; i++)
				{
					for (int j=0; j < NCOLS + 2; j++) 
					{
						if(j > 0 && j <= NCOLS && i > 0 && i <= NROWS)
						{
							flatSamples[0][index] = data[(i-1)*NCOLS+j-1].getCost()/maxCost;
							flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS+j-1].getPlanNumber()];
						}
						else if(j == 0)
						{
							if(i == 0)
							{
								flatSamples[0][index] = data[0].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[0].getPlanNumber()];
							}
							else if(i == NROWS + 1)
							{
								flatSamples[0][index] = data[(i-2)*NCOLS].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS].getPlanNumber()];
							}
							else
							{
								flatSamples[0][index] = data[(i-1)*NCOLS].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS].getPlanNumber()];
							}
						}
						else if(j == NCOLS + 1)
						{
							if(i == 0)
							{
								flatSamples[0][index] = data[j - 2].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
							}
							else if(i == NROWS + 1)
							{
								flatSamples[0][index] = data[(i-2)*NCOLS + j - 2].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS + j - 2].getPlanNumber()];
							}
							else
							{
								flatSamples[0][index] = data[(i-1)*NCOLS + j - 2].getCost()/maxCost;
								flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS + j - 2].getPlanNumber()];
							}
						}
						else if(i == 0)
						{
							flatSamples[0][index] = data[j-1].getCost()/maxCost;
							flatSamples[1][index] = sortedPlan[0][data[j-1].getPlanNumber()];
						}
						else if(i == NROWS + 1)
						{
							flatSamples[0][index] = data[(i-2)*NCOLS+j-1].getCost()/maxCost;
							flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS+j-1].getPlanNumber()];
						}
						index++;
					}
				}
			}
		}
		else
		{
			// EXPO
			if ( maxConditions == 1 ) 
			{
				flatSamples = new double[2][NCOLS*2];
				
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = data[j].getCost()/maxCost;
					flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
					index++;
				}
				
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = 0.05;
					flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
					index++;
				}
				
				return flatSamples;
			}
				
			flatSamples = new double[2][NCOLS * NROWS];
			for (int i=0; i < NROWS; i++)
			{
				for (int j=0; j < NCOLS; j++) 
				{
		            if(maxCost==0.0)
		                flatSamples[0][index] = 1.0;
		            else
		                flatSamples[0][index] = data[i*NCOLS+j].getCost()/maxCost;
					flatSamples[1][index] = sortedPlan[0][data[i*NCOLS+j].getPlanNumber()];
					if ( flatSamples[0][index] > 1.0 ) {
						MessageUtil.CPrintErrToConsole(">>>>>> 1.0 Val : " + index + " " + flatSamples[0][index]);
					}
					index++;
				}
			}
		}
		return flatSamples;
   }
   
   public static double[][] getSamplesForCard(MainPanel panel, int[][] sortedPlan, DiagramPacket gdp) {
	   double[][] flatSamples;
   	
   		DataValues[] data = gdp.getData();
		
   		double maxCardinality = gdp.getMaxCard();
		int NROWS=0;
   		if(gdp.getDimension()!=1)
   			NROWS = gdp.getResolution(PicassoConstants.a[1]);//rss
		int NCOLS = gdp.getResolution(PicassoConstants.a[0]);//rss
		
		int index = 0;
		//int[][] sortedPlan = panel.getSortedPlan();
		int maxConditions = gdp.getDimension();
		
		if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
			if ( maxConditions == 1 ) 
			{
				flatSamples = new double[2][NCOLS*2 + 4];
				
				for (int i=0; i < 2; i++)
				{
					for (int j=0; j < NCOLS + 2; j++) 
					{
						if(j == 0)
						{
							if(i == 0)
								flatSamples[0][index] = data[j].getCard()/maxCardinality;
							else
								flatSamples[0][index] = 0.05;
							flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
						}
						else if(j == NCOLS + 1)
						{
							if(i == 0)
								flatSamples[0][index] = data[j-2].getCard()/maxCardinality;
							else
								flatSamples[0][index] = 0.05;
							flatSamples[1][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
						}
						else
						{
							if(i == 0)
								flatSamples[0][index] = data[j-1].getCard()/maxCardinality;
							else
								flatSamples[0][index] = 0.05;
						
							flatSamples[1][index] = sortedPlan[0][data[j - 1].getPlanNumber()];
						}
						index++;
					}
				}
			}
			else
			{
				flatSamples = new double[2][(NCOLS + 2) * (NROWS + 2)];
				for (int i=0; i < NROWS + 2; i++)
				{
					for (int j=0; j < NCOLS + 2; j++) 
					{
						if(j > 0 && j <= NCOLS && i > 0 && i <= NROWS)
						{
							flatSamples[0][index] = data[(i-1)*NCOLS+j-1].getCard()/maxCardinality;
							flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS+j-1].getPlanNumber()];
						}
						else if(j == 0)
						{
							if(i == 0)
							{
								flatSamples[0][index] = data[0].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[0].getPlanNumber()];
							}
							else if(i == NROWS + 1)
							{
								flatSamples[0][index] = data[(i-2)*NCOLS].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS].getPlanNumber()];
							}
							else
							{
								flatSamples[0][index] = data[(i-1)*NCOLS].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS].getPlanNumber()];
							}
						}
						else if(j == NCOLS + 1)
						{
							if(i == 0)
							{
								flatSamples[0][index] = data[j - 2].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[j - 2].getPlanNumber()];
							}
							else if(i == NROWS + 1)
							{
								flatSamples[0][index] = data[(i-2)*NCOLS + j - 2].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS + j - 2].getPlanNumber()];
							}
							else
							{
								flatSamples[0][index] = data[(i-1)*NCOLS + j - 2].getCard()/maxCardinality;
								flatSamples[1][index] = sortedPlan[0][data[(i-1)*NCOLS + j - 2].getPlanNumber()];
							}
						}
						else if(i == 0)
						{
							flatSamples[0][index] = data[j-1].getCard()/maxCardinality;
							flatSamples[1][index] = sortedPlan[0][data[j-1].getPlanNumber()];
						}
						else if(i == NROWS + 1)
						{
							flatSamples[0][index] = data[(i-2)*NCOLS+j-1].getCard()/maxCardinality;
							flatSamples[1][index] = sortedPlan[0][data[(i-2)*NCOLS+j-1].getPlanNumber()];
						}
						index++;
					}
				}
			}
		}
		else
		{
			// EXPO
			if ( maxConditions == 1 ) 
			{
				flatSamples = new double[2][NCOLS*2];
				
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = data[j].getCard()/maxCardinality;
					flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
					index++;
				}
				
				for (int j=0; j < NCOLS; j++) {
					flatSamples[0][index] = 0.05;
					flatSamples[1][index] = sortedPlan[0][data[j].getPlanNumber()];
					index++;
				}
				
				return flatSamples;
			}
				
			flatSamples = new double[2][NCOLS * NROWS];
			for (int i=0; i < NROWS; i++)
			{
				for (int j=0; j < NCOLS; j++) 
				{
		            if(maxCardinality==0.0)
		                flatSamples[0][index] = 1.0;
		            else
		                flatSamples[0][index] = data[i*NCOLS+j].getCard()/maxCardinality;
					flatSamples[1][index] = sortedPlan[0][data[i*NCOLS+j].getPlanNumber()];
					if ( flatSamples[0][index] > 1.0 ) {
						MessageUtil.CPrintErrToConsole(">>>>>> 1.0 Val : " + index + " " + flatSamples[0][index]);
					}
					index++;
				}
			}
		}
		return flatSamples;
   }
   
   public static void drawBackground(DisplayImpl display, int dimension, ScalarMap[] maps) 
   throws RemoteException, VisADException {
   	RealType longitude, latitude, altitude;
		RealTupleType zdomain_tuple, zrange_tuple;
		RealTupleType xdomain_tuple, xrange_tuple;
		RealTupleType ydomain_tuple, yrange_tuple;
		FunctionType func_zdomain_range;
		FunctionType func_xdomain_range;
		FunctionType func_ydomain_range;
		Set zdomain_set, xdomain_set, ydomain_set;
		FlatField zvals_ff, xvals_ff, yvals_ff;
		DataReferenceImpl zdata_ref, xdata_ref, ydata_ref;
		ScalarMap latMap, lonMap, altMap;
		
		latitude = RealType.getRealType("latitude",null,null);
		longitude = RealType.getRealType("longitude",null,null);
		altitude = RealType.getRealType("altitude",null,null);
		
		zdomain_tuple = new RealTupleType(latitude, longitude);
		zrange_tuple = new RealTupleType( altitude );
		xdomain_tuple = new RealTupleType(longitude,altitude);
		xrange_tuple = new RealTupleType( latitude);
		ydomain_tuple = new RealTupleType(altitude, latitude);
		yrange_tuple = new RealTupleType(longitude);
		
		func_zdomain_range = new FunctionType( zdomain_tuple, zrange_tuple);
		func_xdomain_range = new FunctionType( xdomain_tuple, xrange_tuple);
		func_ydomain_range = new FunctionType( ydomain_tuple, yrange_tuple);
		
		zdomain_set = new Linear2DSet(zdomain_tuple, 0.0, 1.0, 2, 0.0, 1.0, 2);
		xdomain_set = new Linear2DSet(xdomain_tuple, 0.0, 1.0, 2, 0.0, 1.0, 2);
		ydomain_set = new Linear2DSet(ydomain_tuple, 0.0, 1.0, 2, 0.0, 1.0, 2);
		
		float[][] zflat_samples = new float[1][4];
		float[][] xflat_samples = new float[1][4];
		float[][] yflat_samples = new float[1][4];
		for(int i=0;i<4;i++){
			zflat_samples[0][i] = 0.0f;
			xflat_samples[0][i] = 0.0f;
			yflat_samples[0][i] = 1.0f;
		}
		zvals_ff = new FlatField( func_zdomain_range, zdomain_set);
		zvals_ff.setSamples( zflat_samples , false );
		xvals_ff = new FlatField( func_xdomain_range, xdomain_set);
		xvals_ff.setSamples( xflat_samples , false );
		yvals_ff = new FlatField( func_ydomain_range, ydomain_set);
		yvals_ff.setSamples( yflat_samples , false );
		
		latMap = new ScalarMap( latitude,    Display.XAxis );
		latMap.setRange(0.0,1.0);
		latMap.setScaleEnable(false);
		lonMap = new ScalarMap( longitude, Display.YAxis );
		lonMap.setRange(0.0,1.0);
		lonMap.setScaleEnable(false);
		altMap = new ScalarMap( altitude,  Display.ZAxis );
		altMap.setRange(0.0,1.0);
		altMap.setScaleEnable(false);
		display.addMap( latMap );
		maps[4] = latMap;
		maps[5] = lonMap;
		maps[6] = altMap;
		
		if ( dimension != PicassoConstants.ONE_D )
			display.addMap( lonMap );
		
		if ( dimension == PicassoConstants.THREE_D)
			display.addMap( altMap );
		
		zdata_ref = new DataReferenceImpl("zdata_ref");
		zdata_ref.setData( zvals_ff );
		xdata_ref = new DataReferenceImpl("xdata_ref");
		xdata_ref.setData( xvals_ff );
		ydata_ref = new DataReferenceImpl("ydata_ref");
		ydata_ref.setData( yvals_ff );
		
		
		ConstantMap[] zconstMap = {new ConstantMap(0.5f, Display.Red ),
				new ConstantMap(0.5f, Display.Green ),
				new ConstantMap(0.0f, Display.Blue)};
		ConstantMap[] xconstMap = {new ConstantMap(0.1f, Display.Red ),
				new ConstantMap(0.5f, Display.Green ),
				new ConstantMap(0.4f, Display.Blue)};
		ConstantMap[] yconstMap = {new ConstantMap(0.0f, Display.Red ),
				new ConstantMap(0.4f, Display.Green ),
				new ConstantMap(0.0f, Display.Blue)};
		
		if ( dimension == PicassoConstants.THREE_D)
			display.addReference( zdata_ref, zconstMap );
		
		display.addReference( xdata_ref, xconstMap );
		
		if ( dimension != PicassoConstants.ONE_D )
			display.addReference( ydata_ref, yconstMap );	
   }
   
   public static float[] setColorMap(int maxNumber, ScalarMap RGBMap) 
 								throws RemoteException, VisADException {
 		float[][] myColorTable = new float[3][maxNumber+1];
 		
 		for(int r = 0; r < maxNumber+1; r++)
 		{	
 			float[] rgb = PicassoUtil.colorToFloats(new Color(PicassoConstants.color[r%PicassoConstants.color.length]));
 			for(int c = 0 ; c < 3; c++)
 				myColorTable[c][r] = rgb[c];
 		}
 		
 		/*float[] rgb = PicassoUtil.colorToFloats(Color.BLACK);
		for(int c = 0 ; c < 3; c++)
			myColorTable[c][maxNumber+1] = rgb[c];*/
			
 		// Get the ColorControl from the altitude RGB map
 		ColorControl colCont1 = (ColorControl)RGBMap.getControl();
 		
 		// Set the table
 		colCont1.setTable(myColorTable);
 		
 		
 		float[] cr = PicassoUtil.colorToFloats(Color.black);
 		return(cr);
 	}
   
   public static double[] getMinAndMaxValues(DiagramPacket gdp) {
   	double minCost = gdp.getMaxCost();
   	double minCard = gdp.getMaxCard();
   	double maxCost = 0.0;
   	double maxCard = 0.0;
   	int[]	plans = new int[gdp.getMaxPlanNumber()];
   	
   	int maxPlans = gdp.getMaxPlanNumber();
   	for (int i=0; i < maxPlans; i++) {
   		plans[i] = 0;
   	}
   	
   	DataValues[] data = gdp.getData();
   	for (int i=0; i < data.length; i++) {
   		double cost = data[i].getCost();
   		double card = data[i].getCard();
   		if ( minCost > cost )
   			minCost = cost;
   		if ( maxCost < cost )
   			maxCost = cost;
   		if ( minCard > card )
   			minCard = card;
   		if ( maxCard < card )
   			maxCard = card;
   		if ( data[i].getPlanNumber() < maxPlans )
   			plans[data[i].getPlanNumber()]++;
   	}
   	int totalPlans = 0;
   	for (int i=0; i < maxPlans; i++) {
   		if (plans[i] != 0 )
   			totalPlans++;
   	}
   	double[] vals = new double[5];
   	vals[0] = minCost;
   	vals[1] = maxCost;
   	vals[2] = minCard;
   	vals[3] = maxCard;
   	vals[4] = totalPlans;
   	return vals;
   }
  
   public static void setScaleTickValues(AxisScale scale, DiagramPacket gdp, int axis) {
   	float[] selecValues = gdp.getPicassoSelectivity();
		if ( selecValues == null || selecValues.length < 1 )
			scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
		else {
			int res = gdp.getMaxResolution();//rss
			Hashtable tickLabels = new Hashtable();
			int increment = 20;
			double curValue = 0.0;
			String dispValue;
			int index;
			for (int i=0; i < 5; i++) {
				index = (axis*res) + (i*(res/5));//(i*(res/20) + ((res-1)/20));
				dispValue = "" + (int)(selecValues[index]+0.5);
				tickLabels.put(new Double(curValue), new String(dispValue));
				curValue += increment;
			}
			dispValue = "100"; //+ (int)(selecValues[(axis*res)+res-1]+0.5);
			tickLabels.put(new Double(curValue), new String(dispValue));
			//MessageUtil.CPrintToConsole("Tick :: " + curValue + "," + dispValue);
			try {
				scale.setLabelTable(tickLabels);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
   }
   
   public static boolean checkEngineSelectivity(int selecType, DiagramPacket gdp) {
		float[] picassoSelec = gdp.getPicassoSelectivity();
		float[] selecValues = gdp.getPicassoSelectivity();
		int dimension = gdp.getDimension();
		
		if ( selecType == PicassoConstants.PREDICATE_SELECTIVITY )
			selecValues = gdp.getPredicateSelectivity();
		else if ( selecType == PicassoConstants.PLAN_SELECTIVITY )
			selecValues = gdp.getPlanSelectivity();
		else if ( selecType == PicassoConstants.DATA_SELECTIVITY )
			selecValues = gdp.getDataSelectivity();
		int nrows =1;
		if(gdp.getDimension()!=1)
			nrows = gdp.getResolution(1);//rss
		int length = selecValues.length;
		double err1 = (Math.abs(picassoSelec[0] - selecValues[0])*100) / selecValues[0];
		double err2 = (Math.abs(picassoSelec[nrows-1] - selecValues[nrows-1])*100) / selecValues[nrows-1];
		
		double err3 = 0.0;
		double err4 = 0.0;
		if ( dimension > 1 ) {
			err3 = (Math.abs(picassoSelec[nrows] - selecValues[nrows])*100) / selecValues[nrows];
			err4 = (Math.abs(picassoSelec[length-1] - selecValues[length-1])*100) / selecValues[length-1];
		}
		
		if ( err1 <= 10.0 && err2 <= 10.0
				&& err3 <= 10.0 && err4 <= 10.0 )
			return true;
		else
			return false;
	}
}

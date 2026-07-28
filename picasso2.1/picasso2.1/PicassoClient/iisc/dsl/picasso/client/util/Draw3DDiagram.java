
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

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.Font;
import java.util.Hashtable;

import javax.swing.JOptionPane;

import visad.AxisScale;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.ProjectionControl;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.java3d.DisplayImplJ3D;

public class Draw3DDiagram {
	public static DisplayImpl draw(DisplayImpl display, MainPanel panel, DiagramPacket gdp, int type, ScalarMap[] maps) {
		DisplayImpl display1=null;
		ScalarMap	latitudeMap, longitudeMap, altitudeMap, planRGBMap;

		//System.out.println(" In 3d Diagram " + panel.getCurrentTab());
		try {
			if ( display != null ) {
				display.removeAllReferences();
				for (int i=0; i < maps.length; i++) {
					if ( maps[i] != null )
						display.removeMap(maps[i]);
				}
				display1 = display;
			} else
				display1 = new DisplayImplJ3D("display1");
			String[] relations = gdp.getRelationNames();
			String[] attributes = gdp.getAttributeNames();
			String[] relNames = new String[2];

			relNames[0] = relations[0];

			if ( gdp.getDimension() == 1 )
				relNames[1] = "";
			else
				relNames[1] = relations[1];

			// Draw the 3D Graph
			RealType latitude = RealType.getRealType(relNames[0]);
			RealType longitude = RealType.getRealType(relNames[1]);
			RealType altitude = null;
			String altName = "";
			if ( type == PicassoPanel.PLAN_CARD_DIAGRAM) {
				altitude = RealType.getRealType("CompiledCard");
				altName = "Compiled Card (N)";
			} else if ( type == PicassoPanel.PLAN_COST_DIAGRAM ) {
				altitude = RealType.getRealType("CompiledCost");
				altName = "Compiled Cost (N)";
			} else if ( type == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM ) {
				altitude = RealType.getRealType("ExecCard");
				altName = "Exec Card (N)";
			} else if (type == PicassoPanel.EXEC_PLAN_COST_DIAGRAM) {
				altitude = RealType.getRealType("ExecCost");
				altName = "Exec Time (N)";
			}
			RealType planNumber = RealType.getRealType("plan");

			RealTupleType domainTuple = new RealTupleType(latitude, longitude);
			RealTupleType rangeTuple = new RealTupleType(altitude, planNumber);


			// Create a FunctionType (domain_tuple -> plan_tuple)
			// Use FunctionType(MathType domain, MathType range)
			FunctionType funcType = null;
			funcType = new FunctionType(domainTuple, rangeTuple);

			double[][] flatSamples = null;
			if ( type == PicassoPanel.PLAN_COST_DIAGRAM ) {
				flatSamples = DiagramUtil.getSamplesForCost(panel, panel.getSortedPlan(), gdp);
			} else if ( type == PicassoPanel.EXEC_PLAN_COST_DIAGRAM ) {
				flatSamples = DiagramUtil.getSamplesForCost(panel, panel.getExecSortedPlan(), gdp);
			} else if ( type == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM ) {
				flatSamples = DiagramUtil.getSamplesForCard(panel, panel.getExecSortedPlan(), gdp);
			} else if ( type == PicassoPanel.PLAN_CARD_DIAGRAM ) {
				flatSamples = DiagramUtil.getSamplesForCard(panel, panel.getSortedPlan(), gdp);
			}

			// Create a flat array for plans

			latitudeMap = new ScalarMap( latitude, Display.XAxis );
			longitudeMap = new ScalarMap( longitude, Display.YAxis );
			altitudeMap = new ScalarMap( altitude, Display.ZAxis );
			planRGBMap = new ScalarMap( planNumber,  Display.RGB );
			
			maps[0] = latitudeMap;
			maps[1] = longitudeMap;
			maps[2] = planRGBMap;
			maps[3] = altitudeMap;
			
			int selectivity = panel.getDBSettingsPanel().getSelecType();
			boolean addExtra = false;
			//apexp
			if ( gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION) && /*selectivity == PicassoConstants.PICASSO_SELECTIVITY && */gdp.getMaxResolution() < 100 ) {//rss
				// flatSamples = DiagramUtil.getExtra3DSamples(gdp, flatSamples);
				addExtra = false;//true;
				
			}
			//end apexp
			
			
			//apexp
			//flatSamples are plan numbers.
			//change flatSamples from 100 to 121 (or something else, dep. on res)
			if(gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
			{
				double[][] nflatSamples = new double[2][(gdp.getResolution(PicassoConstants.a[0])+1)*(gdp.getResolution(PicassoConstants.a[1])+1)];
				int k=0,l=0;
				for(int i=0;i<gdp.getResolution(PicassoConstants.a[1]);i++)
				{ 	for(int j=0;j<gdp.getResolution(PicassoConstants.a[0]);j++)
					{
						nflatSamples[0][l++]=flatSamples[0][k++];
						nflatSamples[1][l-1]=flatSamples[1][k-1];
					}
					nflatSamples[0][l++]=flatSamples[0][k-1];
					nflatSamples[1][l-1]=flatSamples[1][k-1];
				}
				//copy uppermost row
				k-=gdp.getResolution(PicassoConstants.a[0]);
				for(int j=0;j<gdp.getResolution(PicassoConstants.a[0]);j++)
				{
					nflatSamples[0][l++]=flatSamples[0][k++];
					nflatSamples[1][l-1]=flatSamples[1][k-1];
				}
				nflatSamples[0][l++]=flatSamples[0][k-1];
				nflatSamples[1][l-1]=flatSamples[1][k-1];
				
				flatSamples=nflatSamples;
			}
			
			//paste from 2d diag
			//int fac=5;
			//flatsamples are plan numbers
			//get indices
			//end apexp
			
			FlatField flatFieldValues = Draw2DDiagram.getFlatFieldValues(domainTuple, funcType, gdp, selectivity, addExtra,true,panel);

			//	False being array won't be copied
			//FlatField flatFieldValues = new FlatField(funcType, domainSet);
			flatFieldValues.setSamples( flatSamples , false );

			//		 Add maps to display
			display1.addMap( latitudeMap );
			display1.addMap( longitudeMap );
			display1.addMap( altitudeMap );
			display1.addMap( planRGBMap );

			// Set maps ranges
			latitudeMap.setRange(0.0f, 100.0f);
			longitudeMap.setRange(0.0f, 100.0f);
			altitudeMap.setRange(0.0f, 1.0f);

			int maxPlanNumber = gdp.getMaxPlanNumber();
			planRGBMap.setRange(0, maxPlanNumber);

			//float[] colorMap =
			DiagramUtil.setColorMap(maxPlanNumber, planRGBMap);

			latitudeMap.setScalarName(relNames[0]);
			longitudeMap.setScalarName(relNames[1]);

			//HersheyFont hf = new HersheyFont("cursive");
			Font hf = new Font(PicassoConstants.SCALE_FONT, Font.PLAIN, 10);

			AxisScale scale = latitudeMap.getAxisScale();
			//apexp
			
			/* if ( gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION)) { */
				scale.setSide(AxisScale.PRIMARY);
				scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
			//apexp
			/* } else
				DiagramUtil.setScaleTickValues(scale, gdp, 0); */

			scale.setFont( hf );
			scale.setTitle(relations[0] + "." + attributes[0]+" ["+Double.toString((int) (gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])*100))+","+Double.toString((int) (gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])*100))+"]@ "+Integer.toString((int)gdp.getResolution(PicassoConstants.a[0])));
			scale.setSnapToBox(true);
			scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);

			scale = longitudeMap.getAxisScale();
			scale.setSide(AxisScale.SECONDARY);
			//apexp
			/* if ( gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION)) { */
			//end apexp
				scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
//				apexp
				/*
				}
			else
				DiagramUtil.setScaleTickValues(scale, gdp, 1);
				*/ 
			scale.setFont( hf );
			scale.setSnapToBox(true);
			scale.setTitle(relations[1] + "." + attributes[1]+" ["+Double.toString((int) (gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1])*100))+","+Double.toString((int) (gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])*100))+"]@ "+Integer.toString((int)gdp.getResolution(PicassoConstants.a[1])));
			
			scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);

			scale = altitudeMap.getAxisScale();
			altitudeMap.setScalarName(altName);
			Hashtable labelTable = new Hashtable();
			labelTable.put(new Double(0.0), "0.0");
			labelTable.put(new Double(0.2), "0.2");
			labelTable.put(new Double(0.4), "0.4");
			labelTable.put(new Double(0.6), "0.6");
			labelTable.put(new Double(0.8), "0.8");
			labelTable.put(new Double(1.0), "1.0");
			scale.setLabelTable(labelTable);
			scale.setFont( hf );
			scale.setSnapToBox(true);
			scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);

			ProjectionControl projCont = display1.getProjectionControl();
			DisplayRenderer dRenderer = display1.getDisplayRenderer();
			dRenderer.setBoxOn( false );
			dRenderer.setBackgroundColor(PicassoConstants.IMAGE_BACKGROUND);

			//		 Get display's graphics mode control and draw scales
			GraphicsModeControl dispGMC1 = (GraphicsModeControl)display1.getGraphicsModeControl();

			dispGMC1.setProjectionPolicy(DisplayImplJ3D.PARALLEL_PROJECTION);
			dispGMC1.setScaleEnable(true);

			DataReferenceImpl dataRef = new DataReferenceImpl("dataReference");
			dataRef.setData(flatFieldValues);
			display1.addReference(dataRef);

			double[] aspect;

			aspect = new double[]{PicassoConstants.ASPECT_X, PicassoConstants.ASPECT_Y, PicassoConstants.ASPECT_Z};
			DiagramUtil.drawBackground(display1, PicassoConstants.THREE_D, maps);

//			 double[] 	make_matrix(double rotx, double roty, double rotz, double scale, double transx, double transy, double transz) 
			projCont.setMatrix(display1.make_matrix(48.75,52.5,60.0,0.35,0.0,0.0,0.0)); //0.5 normal

			projCont.setAspectCartesian( aspect );
		} catch (OutOfMemoryError bounded) {
			JOptionPane.showMessageDialog(panel, "Out Of Memory Error, Please Restart PicassoClient","Error",JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return display1;
    }
}

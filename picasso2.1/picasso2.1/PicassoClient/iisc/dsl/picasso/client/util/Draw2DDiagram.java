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
import java.awt.Font;

import javax.swing.JOptionPane;

import visad.AxisScale;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.Gridded2DSet;
import visad.ProjectionControl;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.SetException;
import visad.java3d.DirectManipulationRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.DisplayRendererJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;

public class Draw2DDiagram {

	public static DisplayImpl draw(DisplayImpl display, MainPanel panel,
			DiagramPacket gdp, int type, ScalarMap[] maps) {
		DisplayImpl display1 = null;
		ScalarMap latitudeMap, longitudeMap, planRGBMap;

		try {
			if (display != null) {
				display.removeAllReferences();
				for (int i = 0; i < maps.length; i++) {
					if (maps[i] != null)
						display.removeMap(maps[i]);
				}
				display1 = display;
			} else
				display1 = new DisplayImplJ3D("display1",
						new TwoDDisplayRendererJ3D());

			String[] relNames = new String[2];
			String[] relations = gdp.getRelationNames();
			String[] attributes = gdp.getAttributeNames();

			relNames[0] = relations[0];

			if (gdp.getDimension() == 1)
				relNames[1] = "";
			else
				relNames[1] = relations[1];

			// Draw the 2D Graph
			RealType latitude = RealType.getRealType(relNames[0]);
			RealType longitude;

			longitude = RealType.getRealType(relNames[1]);

			// altitude = RealType.getRealType("Plan");
			RealType planNumber = RealType.getRealType("plan");

			RealTupleType domainTuple = new RealTupleType(latitude, longitude);
			// RealTupleType rangeTuple = new RealTupleType(altitude,
			// planNumber);

			// Create a FunctionType (domain_tuple -> plan_tuple)
			// Use FunctionType(MathType domain, MathType range)
			FunctionType funcType = new FunctionType(domainTuple, planNumber);

			Real[] reals3;
			/*
			 * reals3 = new Real[] {new Real(longitude, 1.0), new Real(latitude,
			 * 1.0), new Real(planNumber, 1.0)}; //1.0)};
			 */
			DataValues[] data = gdp.getData();
			float[] sValues = gdp.getPredicateSelectivity();
			float lat = sValues[0];
			float lon = sValues[gdp.getResolution(0)];//rss
			float plan = data[0].getPlanNumber();
			reals3 = new Real[] { new Real(longitude, lon),
					new Real(latitude, lat), new Real(planNumber, plan) };
			RealTuple direct_tuple = new RealTuple(reals3);

			// Create a flat array for plans (colors)
			//the first index is always 1, that's why it's flat
			double[][] flatSamples;
			if (type == PicassoPanel.REDUCED_PLAN_DIAGRAM)
				flatSamples = DiagramUtil.getSamplesForReducedPlan(panel, gdp);
			else
				flatSamples = DiagramUtil.getSamplesForPlan(0, panel, gdp);

			latitudeMap = new ScalarMap(latitude, Display.XAxis);
			longitudeMap = new ScalarMap(longitude, Display.YAxis);
			planRGBMap = new ScalarMap(planNumber, Display.RGB);
			maps[0] = latitudeMap;
			maps[1] = longitudeMap;
			maps[2] = planRGBMap;

			// FlatField flatFieldValues = new FlatField(funcType, domainSet);
			int selectivity = panel.getDBSettingsPanel().getSelecType();

			boolean addExtra = false;
			boolean doExtra = true;
			// apexp
			// doExtra will be false only if there's a 10+% difference
			// between Picasso selectivity values and engine sel values
			// end apexp
			if (selectivity != PicassoConstants.PICASSO_SELECTIVITY) {
				doExtra = DiagramUtil.checkEngineSelectivity(selectivity, gdp);
			}
			// apexp: make them 10,000 samples if they aren't already more than
			// that,
			// and set addExtra so that getFlatfieldValues can see it..
			/*if (doExtra == true
					&& gdp.getResolution(0)*gdp.getResolution(1) < 100//rss
					&& gdp.getQueryPacket().getDistribution().equals(
							PicassoConstants.UNIFORM_DISTRIBUTION)) {
				// end apexp
				flatSamples = DiagramUtil.getExtraPlanSamples(gdp, flatSamples);
				addExtra = true;
			}*/
			// apexp
			// flatSamples are plan numbers.
			// change flatSamples from 100 to 121 (or something else, dep. on
			// res)
			double [][] onflatSamples; 
			if (gdp.getQueryPacket().getDistribution().startsWith(
					PicassoConstants.EXPONENTIAL_DISTRIBUTION)) {
				double[][] nflatSamples = new double[1][(gdp.getResolution(PicassoConstants.a[0]) + 1)
						* (gdp.getResolution(PicassoConstants.a[1]) + 1)];//rss
				int k = 0, l = 0;
				for (int i = 0; i < gdp.getResolution(PicassoConstants.a[1]); i++) {			//rss
					for (int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++) {		//rss
						nflatSamples[0][l++] = flatSamples[0][k++];
					}
					nflatSamples[0][l++] = flatSamples[0][k - 1];
				}
				// copy uppermost row
				k -= gdp.getResolution(PicassoConstants.a[0]);//rss
				for (int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++) {		//rss
					nflatSamples[0][l++] = flatSamples[0][k++];
				}
				nflatSamples[0][l++] = flatSamples[0][k - 1];

				flatSamples = nflatSamples;
				// onflatSamples = nflatSamples;
			}

			// put fac points between every 2 points.

			int fac;
			if (gdp.getMaxResolution() < 100)//rss
				fac = 10;
			else
				fac = 5;
			// flatsamples are plan numbers
			if (gdp.getQueryPacket().getDistribution().startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION)
					&& gdp.getMaxResolution() <= 100) //rss
			{
				// multiple duplication code
				double[][] nflatSamples = new double[1][(gdp.getResolution(PicassoConstants.a[0]) + 1+ fac* gdp.getResolution(PicassoConstants.a[0]))
						* (gdp.getResolution(PicassoConstants.a[1]) + 1 + fac * gdp.getResolution(PicassoConstants.a[1]))];//rss

				int l = 0, m = 0;
				for (int i = 0; i < gdp.getResolution(PicassoConstants.a[1]); i++) {//rss
					for (int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++) {//rss
						for (int k = 0; k <= fac; k++) {
							nflatSamples[0][m++] = flatSamples[0][l];
						}
						l++;
					} // end for j
					nflatSamples[0][m++] = flatSamples[0][l++];

					// vertical
					for (int k = 1; k <= fac; k++) {
						l -= (gdp.getResolution(PicassoConstants.a[0]) + 1);//rss

						for (int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++) {//rss
							for (int k2 = 0; k2 <= fac; k2++) {
								nflatSamples[0][m++] = flatSamples[0][l];
							}
							l++;
						}
						nflatSamples[0][m++] = flatSamples[0][l++];
					}
				} // end for i

				// TOP ROW
				for (int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++) {//rss
					for (int k = 0; k <= fac; k++) {
						nflatSamples[0][m++] = flatSamples[0][l];
					}
					l++;
				}
				nflatSamples[0][m++] = flatSamples[0][l++];
				flatSamples = nflatSamples;
			}
			// get indices
			// end apexp
			FlatField flatFieldValues = getFlatFieldValues(domainTuple,
					funcType, gdp, selectivity, addExtra, false, panel);
			try {
				flatFieldValues.setSamples(flatSamples, false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

			// Add maps to display
			display1.addMap(latitudeMap);
			display1.addMap(longitudeMap);
			display1.addMap(planRGBMap);

			// Set maps ranges
			latitudeMap.setRange(0.0f, 100.0f);
			longitudeMap.setRange(0.0f, 100.0f);

			int maxPlanNumber = gdp.getMaxPlanNumber(); //PicassoConstants.slicePlans.length;
			planRGBMap.setRange(0, maxPlanNumber);

			// float[] colorMap =
			DiagramUtil.setColorMap(maxPlanNumber, planRGBMap);

			// latitudeMap.setScaleColor( colorMap );
			// longitudeMap.setScaleColor( colorMap );

			latitudeMap.setScalarName(relNames[0]);
			longitudeMap.setScalarName(relNames[1]);

			// HersheyFont hf = new HersheyFont("cursive");
			Font hf = new Font(PicassoConstants.SCALE_FONT, Font.PLAIN, 12);

			AxisScale scale = latitudeMap.getAxisScale();
			// if ( gdp.getQueryPacket().getScaleType() ==
			// Constants.UNIFORMDISTRIBUTION ) {
			scale.setSide(AxisScale.PRIMARY);
			scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
			/*
			 * } else setScaleTickValues(scale, gdp, 0);
			 */
			scale.setFont(hf);
		//	scale.setTitle(relations[0] + "." + attributes[0]);
			scale.setTitle(relations[0] + "." + attributes[0]+" ["+Double.toString((int) (gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])*100))+","+Double.toString((int) (gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])*100))+"]@ "+Integer.toString((int)gdp.getResolution(PicassoConstants.a[0])));
			scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);
			// scale.setSnapToBox(true);

			if (gdp.getDimension() != 1) {
				scale = longitudeMap.getAxisScale();
				scale.setFont(hf);
				// if ( gdp.getQueryPacket().getScaleType() ==
				// Constants.UNIFORMDISTRIBUTION ) {
				scale.setSide(AxisScale.PRIMARY);
				scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
		//		scale.setTitle(relations[1] + "." + attributes[1]);
				scale.setTitle(relations[1] + "." + attributes[1]+" ["+Double.toString((int) (gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1])*100))+","+Double.toString((int) (gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])*100))+"]@ "+Integer.toString((int)gdp.getResolution(PicassoConstants.a[1])));
				scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);
				/*
				 * } else setScaleTickValues(scale, gdp, 1);
				 */
			} else {
				longitudeMap.setScaleColor(PicassoUtil
						.colorToFloats(Color.black));
			}

			// scale.setSnapToBox(true);

			ProjectionControl projCont = display1.getProjectionControl();
			DisplayRenderer dRenderer = display1.getDisplayRenderer();
			dRenderer.setBoxOn(false);
			dRenderer.setBackgroundColor(PicassoConstants.IMAGE_BACKGROUND);
			dRenderer.setCursorOn(false);
			// dRenderer.setForegroundColor(Color.BLACK);

			// Get display's graphics mode control and draw scales
			GraphicsModeControl dispGMC1 = (GraphicsModeControl) display1
					.getGraphicsModeControl();

			dispGMC1.setProjectionPolicy(DisplayImplJ3D.PARALLEL_PROJECTION);

			if (gdp.getQueryPacket().getDistribution().equals(
					PicassoConstants.UNIFORM_DISTRIBUTION)
					&& doExtra) {
				// dispGMC1.setPointSize(400/gdp.getResolution());
				dispGMC1.setPointSize(1.0f);
				dispGMC1.setPointMode(false);
			} else {
				dispGMC1.setPointSize(1.0f);
				dispGMC1.setPointMode(false);
			}

			dispGMC1.setScaleEnable(true);

			DisplayRendererJ3D dr = (DisplayRendererJ3D) display1
					.getDisplayRenderer();
			dr.setClip(0, true, 1.0f, 0.0f, 0.0f, -1.0f);
			dr.setClip(1, true, -1.0f, 0.0f, 0.0f, -1.0f);
			dr.setClip(2, true, 0.0f, 1.0f, 0.0f, -1.0f);
			dr.setClip(3, true, 0.0f, -1.0f, 0.0f, -1.0f);
			dr.setClip(4, true, 0.0f, 0.0f, 1.0f, -1.0f);
			dr.setClip(5, true, 0.0f, 0.0f, -1.0f, -1.0f);

			DataReferenceImpl ref_direct_tuple;
			ref_direct_tuple = new DataReferenceImpl("ref_direct_tuple");
			ref_direct_tuple.setData(direct_tuple);
			DataReference[] refs2 = new DataReference[] { ref_direct_tuple };
			display1.addReferences(new DirectManipulationRendererJ3D(), refs2,
					null);

			DataReferenceImpl dataRef = new DataReferenceImpl("dataReference");
			dataRef.setData(flatFieldValues);
			// This line actually shows the figure, but the clicking on
			// it is handled elsewhere.
			display1.addReference(dataRef);

			double[] aspect;

			aspect = new double[] { PicassoConstants.ASPECT_2D_X,
					PicassoConstants.ASPECT_2D_Y };
//			 double[] 	make_matrix(double rotx, double roty, double rotz, double scale, double transx, double transy, double transz) 
			projCont.setMatrix(display1.make_matrix(0, 0, 0, 0.4, 0, 0.05, 0)); //0.70 normal
			projCont.setAspectCartesian(aspect);
		} catch (OutOfMemoryError bounded) {
			JOptionPane.showMessageDialog(panel,
					"Out Of Memory Error, Please Restart PicassoClient",
					"Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage().equals("Gridded2DSet exception")
					&& panel.getDBSettingsPanel().getSelecType() != PicassoConstants.PICASSO_SELECTIVITY) {
				JOptionPane
						.showMessageDialog(
								panel,
								"The engine selectivity values are not monotonically non-decreasing. The plan diagram will not be shown.");
				panel.tabbedPane.setSelectedIndex(0);
			}
		}
		return display1;
	}

	public static FlatField getFlatFieldValues(RealTupleType domainTuple,
			FunctionType funcType, DiagramPacket gdp, int selecType,
			boolean addExtra, boolean is3d, MainPanel panel) throws Exception {
		// float[] sValue = gdp.getPicassoSelectivity();
		float[] selecValues = gdp.getPicassoSelectivity();// = new
															// float[sValue.length];

		int NROWS=1;
		//int a[] = panel.getPicassoPanel().getDisplayedDimensions();
		if(gdp.getDimension()!=1)
			NROWS = gdp.getResolution(PicassoConstants.a[1]);//rss
		int NCOLS = gdp.getResolution(PicassoConstants.a[0]);//rss

		// MessageUtil.CPrintToConsole("Selec :: " + selecType);
		if (selecType == PicassoConstants.PICASSO_SELECTIVITY) {
			selecValues = gdp.getPicassoSelectivity();
		} else if (selecType == PicassoConstants.PREDICATE_SELECTIVITY)
			selecValues = gdp.getPredicateSelectivity();
		else if (selecType == PicassoConstants.PLAN_SELECTIVITY)
			selecValues = gdp.getPlanSelectivity();
		else if (selecType == PicassoConstants.DATA_SELECTIVITY)
			selecValues = gdp.getDataSelectivity();

		// This and getExtra samples for data go together..
		// This is done so that the data is shown properly in resolutions of 10
		// and 30
		// Here the two extra bands are copied (the 0th row and the 0th column)
		if (addExtra) {
			int multipleFactor = (int) (100.0 / NROWS);
			double addValue = 100.0 / (multipleFactor * NROWS);
			int length = multipleFactor * NROWS;
			float[] sValues = new float[length * 2];
			int index = 0;
			for (int k = 0; k < 2; k++) {
				double pointValue = 0;
				for (int i = 0; i < multipleFactor; i++) {
					for (int j = 0; j < NROWS; j++) {
						sValues[index++] = new Float(pointValue).floatValue();// selecValues[k*NROWS+j];
						pointValue += addValue;
					}
				}
			}
			sValues[length - 1] = 100.0f;
			sValues[(length * 2) - 1] = 100.0f;
			selecValues = sValues;
			NCOLS = multipleFactor * NROWS;
			NROWS = NCOLS;
		}

		// Set the boundary conditions properly
		// apexp
		if (gdp.getQueryPacket().getDistribution().equals(
				PicassoConstants.UNIFORM_DISTRIBUTION)
				&& panel.getDBSettingsPanel().getSelecType() == PicassoConstants.PICASSO_SELECTIVITY) {
			// validateSelecValues(NROWS, NCOLS, selecValues, selecType);
			// apexp
		}
		// end apexp

		// commented by apexp//float[][] sampleSet = new
		// float[2][(NROWS)*(NCOLS)];
		// apexp
		float[][] osampleSet = null;
		float[][] sampleSet;
		int index = 0;
		int fac;
		if (gdp.getMaxResolution() < 100)
			fac = 10;
		else
			fac = 5;
		
		int ressum[] = new int[gdp.getDimension()];
		for(int i = 1; i < ressum.length; i++)
			ressum[i] = ressum[i-1] + gdp.getResolution(i-1);

		if (gdp.getQueryPacket().getDistribution().equals(
				PicassoConstants.UNIFORM_DISTRIBUTION)) {
			sampleSet = new float[2][(NROWS + 2) * (NCOLS + 2)];
			// end apexp
			for (int i = 0; i < NROWS + 2; i++) {
				// System.out.println(i + " :: " + selecValues[NROWS+i] + " ::
				// ");
				for (int j = 0; j < NCOLS + 2; j++) {
					
					if(j == 0)
					{
						sampleSet[0][index] = new Double(gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])).floatValue()*100;
					}
					else if(j == NCOLS + 1)
						sampleSet[0][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])).floatValue()*100;
					else
						sampleSet[0][index] = new Double(selecValues[ressum[PicassoConstants.a[0]]+j - 1]).floatValue();

					if (selecValues.length == NCOLS) // a complex way write 1-D.
						sampleSet[1][index] = new Double(selecValues[i]).floatValue();
					else if(i == 0)
						sampleSet[1][index] = new Double(gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1])).floatValue()*100;
					else if (i == NROWS + 1)
						sampleSet[1][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])).floatValue()*100;
					else
						sampleSet[1][index] = new Double(selecValues[ressum[PicassoConstants.a[1]]+ i - 1]).floatValue();
					// System.out.print("(" + sampleSet[0][index] + "," +
					// sampleSet[1][index] + ")");
					index++;
				}
				int z = 0;
			}
			// apexp
		}

		else // exponential distri
		{

			osampleSet = new float[2][(NROWS + 1) * (NCOLS + 1)];

			/*
			 * 
			 * set the points directly, not lowers
			 */
			if (is3d) {
				for (int i = 0; i <= NROWS; i++) {
					for (int j = 0; j <= NCOLS; j++) {

						int k;

						/*if(i==0)
						{
							osampleSet[1][index] = new Double(gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1])).floatValue() * 100;
						}*/
						if (i == NROWS) {
							osampleSet[1][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])).floatValue() * 100;
						} else {
							osampleSet[1][index] = selecValues[ressum[PicassoConstants.a[1]] + i];
						}

						/*if(j==0)
						{
							osampleSet[0][index] = new Double(gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])).floatValue() * 100;
						}*/
						if (j == NCOLS) {
							osampleSet[0][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])).floatValue() * 100;
						} else {
							osampleSet[0][index] = selecValues[ressum[PicassoConstants.a[0]]+j];
						}

						index++;
					} // end for j
				} // end for i

				sampleSet = osampleSet;
			}

			else // 2d
			{
				// set the lowers
				for (int i = 0; i <= NROWS; i++) {
					for (int j = 0; j <= NCOLS; j++) {

						int k;

						if (i == NROWS) {
							osampleSet[1][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])).floatValue() * 100; // 100.0f;
						} else {
							if (i != 0)
								osampleSet[1][index] = selecValues[ressum[PicassoConstants.a[1]] + i] - (selecValues[ressum[PicassoConstants.a[1]] + i] - selecValues[ressum[PicassoConstants.a[1]]+ i - 1]) / 2;
							else
								osampleSet[1][index] = selecValues[ressum[PicassoConstants.a[1]]];
						}

						if (j == NCOLS) {
							osampleSet[0][index] = new Double(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])).floatValue() * 100;
						} else {
							if (j != 0)
								osampleSet[0][index] = selecValues[ressum[PicassoConstants.a[0]]+j]
										- (selecValues[ressum[PicassoConstants.a[0]]+j] - selecValues[ressum[PicassoConstants.a[0]]+j - 1])
										/ 2;
							else
								osampleSet[0][index] = selecValues[ressum[PicassoConstants.a[0]]];
						}

						index++;
					} // end for j
				} // end for i

				// multiple duplication interpolation code
				if (gdp.getMaxResolution() <= 100 && !is3d) {//rss
					sampleSet = new float[2][(NROWS + 1 + fac * NROWS)
							* (NCOLS + 1 + fac * NCOLS)];
					int k;
					index = 0;
					int oi = 0;

					for (int i = 0; i < NROWS; i++) {

						for (int j = 0; j < NCOLS; j++) {
							for (k = 0; k <= fac; k++) {
								sampleSet[0][index] = osampleSet[0][oi]
										+ (osampleSet[0][oi + 1] - osampleSet[0][oi])
										* k / (fac + 1);
								sampleSet[1][index] = osampleSet[1][oi];
								index++;
							}
							oi++;
						}

						sampleSet[0][index] = osampleSet[0][oi];
						sampleSet[1][index++] = osampleSet[1][oi++];

						// vertical
						for (int k2 = 1; k2 <= fac; k2++) {
							oi -= (NCOLS + 1);
							for (int j = 0; j < NCOLS; j++) {
								for (k = 0; k <= fac; k++) {
									sampleSet[0][index] = osampleSet[0][oi]
											+ (osampleSet[0][oi + 1] - osampleSet[0][oi])
											* k / (fac + 1);
									;
									sampleSet[1][index] = osampleSet[1][oi]
											+ (osampleSet[1][oi + NCOLS + 1] - osampleSet[1][oi])
											* k2 / (fac + 1);
									index++;
								}
								oi++;
							}
							sampleSet[0][index] = osampleSet[0][oi];
							sampleSet[1][index] = osampleSet[1][oi]
									+ (osampleSet[1][oi + NCOLS + 1] - osampleSet[1][oi])
									* k2 / (fac + 1);
							index++;
							oi++;
						}

					} // end for i

					// TOP ROW
					for (int j = 0; j < NCOLS; j++) {
						for (k = 0; k <= fac; k++) {
							sampleSet[0][index] = osampleSet[0][oi]
									+ (osampleSet[0][oi + 1] - osampleSet[0][oi])
									* k / (fac + 1);
							sampleSet[1][index] = osampleSet[1][oi];
							index++;
						}
						oi++;
					}
					sampleSet[0][index] = osampleSet[0][oi];
					sampleSet[1][index++] = osampleSet[1][oi++];
				} else {
					sampleSet = osampleSet;
				}
			} // end of else (2d)
		} // end of else (expo)
		// end apexp
		try {
			// apexp
			Gridded2DSet domainSet;
			if (gdp.getQueryPacket().getDistribution().equals(
					PicassoConstants.UNIFORM_DISTRIBUTION)) {
				domainSet = new Gridded2DSet(domainTuple, sampleSet, NCOLS + 2,
						NROWS + 2);
				// domainSet = new Gridded2DSet(domainTuple, sampleSet, (NCOLS+1)*(100/NCOLS), (100/NROWS) * (NROWS+1));
			} 
			else 
			{ //EXPONENTIAL
				if (gdp.getMaxResolution() <= 100 && !is3d)//rss
					sampleSet = validateSelecValues2(sampleSet, NROWS + 1 + fac	* NROWS, NCOLS + 1 + fac * NCOLS);
				else
					sampleSet = validateSelecValues2(sampleSet, NROWS + 1, NCOLS + 1);
				//sampleSet = validateSelecValues2(sampleSet, NROWS + 1 + fac*NROWS, NCOLS + 1+fac*NCOLS); //-ma
				// normal
				// domainSet = new Gridded2DSet(domainTuple, sampleSet, NROWS+1,
				// NCOLS+1);
				// multiple duplication
				if (gdp.getMaxResolution() <= 100 && !is3d)//rss
					domainSet = new Gridded2DSet(domainTuple, sampleSet, NCOLS
							+ 1 + fac * NCOLS, NROWS + 1 + fac * NROWS);
				else
					domainSet = new Gridded2DSet(domainTuple, sampleSet,
							NCOLS + 1, NROWS + 1);
				
				//the osampleset is of size (ncols+1) * (nrows+1)
				//domainSet = new Gridded2DSet(domainTuple, osampleSet, NCOLS + 1, NROWS + 1); //use for originial expo diagram.
				// end duplication
				// domainSet = new Gridded2DSet(domainTuple, sampleSet, NROWS*2,
				// NCOLS*2);
				// end and multiple duplication
				// domainSet = new Gridded2DSet(domainTuple, sampleSet,
				// NROWS*(2+fac), NCOLS*(2+fac));
			}
			// end apexp
			FlatField flatFieldValues = new FlatField(funcType, domainSet);
			return flatFieldValues;
		} catch (SetException se) {

			/*index = 0;
			for (int i = 0; i < NROWS; i++) {
				System.out.println(i + " :: " + selecValues[i] + " :: ");
				System.out.println("");
			}
			for (int j = 0; j < NCOLS; j++) {
				System.out.println(j + " :: " + selecValues[NROWS + j] + ")");
				// index++;
			}*/
			se.printStackTrace();
			throw new Exception("Gridded2DSet exception");
		} catch (Exception e) {
			MessageUtil.CPrintErrToConsole("Exception in Gridded2DSet");
			e.printStackTrace();
			throw new Exception("Gridded2DSet exception");
		}
		// return null;
	}

	private static float[] validateSelecValues(int NROWS, int NCOLS, float[] selecValues,
			int selecType) {
		float epsilon = 0.000001F;
		// System.out.print("ROW VALUES (" + selecValues[0] + ",");
		for (int i = 1; i < NROWS; i++) {
			if (selecValues[i - 1] >= selecValues[i])
				selecValues[i] = selecValues[i - 1] + epsilon;
			// System.out.print(selecValues[i] + ",");

		}
		if (selecValues.length == NROWS) // it is 1-D
			return selecValues;

		// System.out.println(")");
		// System.out.print("COL VALUES (" + selecValues[NROWS] + ",");
		for (int i = 1; i < NROWS; i++) {
			if (selecValues[NCOLS + i - 1] >= selecValues[NCOLS + i])
				selecValues[NCOLS + i] = selecValues[NCOLS + i - 1] + epsilon;
			// System.out.print(selecValues[NROWS+i] + ",");
		}
		// System.out.println(")");
		return selecValues;
	}

	private static float[][] validateSelecValues2(float[][] s, int rows,
			int cols) {
		int i, j, k;
		float c, d = 1;
		double maxval, diff, theval;
		int cnt;

		for (i = 0; i < rows; i++) {
			c = 1;
			for (j = 0; j < cols; j++) {
				if (s[0][i * cols + j] < 1e-6f
						|| (j != 0 && s[0][i * cols + j] < s[0][i * cols + j
								- 1])) {
					s[0][i * cols + j] = c * 1e-6f;
					c++;
				}
				if (s[1][i * cols + j] < 1e-6f
						|| (i != 0 && s[1][i * cols + j] < s[1][(i - 1) * cols
								+ j])) {
					s[1][i * cols + j] = d * 1e-6f;
				}
			}
			d++;
		}

		// This part tries to create a small epsilon such that suppose your
		// selvalues were
		// 0.0005 0.0005 0.0005 0.00500001, your epsilon couldn't be such that
		// 0.0005 + epsilon >(=) 0.00500001
		for (i = 0; i < rows; i++) {
			for (j = 0; j < cols; j++) {
				cnt = 1;
				k = j;
				while (k != 0 && k != cols
						&& s[0][i * cols + k] == s[0][i * cols + k - 1]) {
					cnt++;
					k++;
				}
				if (cnt != 1) {
					if (k == cols) {
						maxval = 0.999999999999999;
					} else {
						maxval = s[0][i * cols + k]; // the greater one after
														// the equal set
					}
					diff = maxval - s[0][i * cols + j]; // the difference
														// between the bigger
														// value and the equal
														// set

					diff /= 2;
					diff /= cnt;
					// now diff (epsilon) is such that after adding it to all
					// these same valued points, the final point will
					// still be before the 1st point with a different
					// selectivity value.
					if (diff > 1e-6)
						diff = 1e-6;

					theval = s[0][i * cols + j];
					k = j;
					while (k != 0 && k != cols && s[0][i * cols + k] == theval) {
						s[0][i * cols + k] = s[0][i * cols + k - 1]
								+ (float) diff; // add our epsilon to it
						k++;
					}
				} // end if(cnt!=1)
			} // end for j
		} // end for i

		for (i = 1; i < rows; i++) {
			cnt = 1;
			k = i;
			// check 0th column only (rest will be same w.r.t. y co-ord)
			while (s[1][k * cols + 0] == s[1][(k - 1) * cols + 0]) {
				cnt++;
				k++;
			}
			if (cnt != 1) {
				if (k == rows) {
					maxval = 0.999999999999999;
				} else {
					maxval = s[1][k * cols + 0]; // the greater one after the
													// equal set
				}
				diff = maxval - s[1][i * cols + 0]; // the difference between
													// the bigger value and the
													// equal set

				diff /= 2;
				diff /= cnt;
				if (diff > 1e-6)
					diff = 1e-6;
				theval = s[1][i * cols + 0];
				k = i;
				while (k != 0 && k != rows && s[1][k * cols + 0] == theval) {

					for (j = 0; j < cols; j++)
						s[1][k * cols + j] = s[1][(k - 1) * cols + j]
								+ (float) diff; // add our epsilon to it
					k++;
				}
			}
		}
		return s;
	}
}

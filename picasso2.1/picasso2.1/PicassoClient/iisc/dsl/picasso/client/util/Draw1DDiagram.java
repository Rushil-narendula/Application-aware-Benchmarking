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

import java.awt.Font;
import java.util.Hashtable;

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

public class Draw1DDiagram {

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
			relNames[1] = "";

			double[][] flatSamples = null;
			boolean isLongitudeScale = false;

			DataValues[] data = gdp.getData();
			double maxCost = gdp.getMaxCost();
			double maxCard = gdp.getMaxCard();

			int NCOLS = gdp.getResolution(0);
			double[] actualSamples = new double[NCOLS];

			switch (type) {
			case PicassoPanel.PLAN_COST_DIAGRAM:
				relNames[1] = "Compiled Cost (N)";
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = data[j].getCost() / maxCost;
				}
				flatSamples = DiagramUtil.getSamplesForPlan(0, panel, gdp);
				isLongitudeScale = true;
				break;

			case PicassoPanel.EXEC_PLAN_COST_DIAGRAM:
				relNames[1] = "Exec Time (N)";
				if (maxCost == 0)
					maxCost = 1;
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = data[j].getCost() / maxCost;
				}
				flatSamples = DiagramUtil.getSamplesForPlan(1, panel, gdp);
				isLongitudeScale = true;
				break;

			case PicassoPanel.PLAN_CARD_DIAGRAM:
				relNames[1] = "Compiled Card (N)";
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = data[j].getCard() / maxCard;
				}
				flatSamples = DiagramUtil.getSamplesForPlan(0, panel, gdp);
				isLongitudeScale = true;
				break;

			case PicassoPanel.EXEC_PLAN_CARD_DIAGRAM:
				relNames[1] = "Exec Card (N)";
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = data[j].getCard() / maxCard;
				}
				flatSamples = DiagramUtil.getSamplesForPlan(1, panel, gdp);
				isLongitudeScale = true;
				break;

			case PicassoPanel.PLAN_DIAGRAM:
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = 0.30;
				}
				flatSamples = DiagramUtil.getSamplesForPlan(0, panel, gdp);
				break;

			case PicassoPanel.REDUCED_PLAN_DIAGRAM:
				for (int j = 0; j < NCOLS; j++) {
					actualSamples[j] = 0.30;
				}
				flatSamples = DiagramUtil.getSamplesForReducedPlan(panel, gdp);
				break;

			default:
				MessageUtil.CPrintToConsole("Wrong TYPE !!!!! " + type);
				relNames[1] = "";
				// xyTuple = null;
				break;
			}

			RealType latitude = RealType.getRealType(relNames[0]);
			RealType planNumber = RealType.getRealType("plan");
			RealType longitude = RealType.getRealType("Cost");
			RealTupleType domainTuple = new RealTupleType(latitude, longitude);
			FunctionType funcType = new FunctionType(domainTuple, planNumber);

			// Draw the 2D Graph
			Real[] reals3;
			reals3 = new Real[] { new Real(longitude, 5.0),
					new Real(latitude, 5.0),
					new Real(planNumber, gdp.getMaxPlanNumber() + 1) }; // 1.0)};
			RealTuple direct_tuple = new RealTuple(reals3);

			
			latitudeMap = new ScalarMap(latitude, Display.XAxis);
			longitudeMap = new ScalarMap(longitude, Display.YAxis);
			planRGBMap = new ScalarMap(planNumber, Display.RGB);
			maps[0] = latitudeMap;
			maps[1] = longitudeMap;
			maps[2] = planRGBMap;

			
			int selectivity = panel.getDBSettingsPanel().getSelecType();
			boolean addExtra = false;
			boolean doExtra = true;
			if (selectivity != PicassoConstants.PICASSO_SELECTIVITY)
				doExtra = DiagramUtil.checkEngineSelectivity(selectivity, gdp);
			// apexp
			if (doExtra == true
					&& gdp.getResolution(0) <= 100//rss
					&& gdp.getQueryPacket().getDistribution().equals(
							PicassoConstants.UNIFORM_DISTRIBUTION)) {
				// end apexp
				flatSamples = DiagramUtil.getExtraPlanSamples(gdp, flatSamples);
				addExtra = true;
			}
			// apexp
			int fac1d = 10;
			if (gdp.getQueryPacket().getDistribution().startsWith(
					PicassoConstants.EXPONENTIAL_DISTRIBUTION)) {
				double[][] nflatSamples;
				
				
				if(gdp.getResolution(0)<10)//rss
				{
					nflatSamples= new double[1][(gdp.getResolution(0)//rss
							* fac1d + 1) * 2];
					int i, j;
					int k = 0, l = 0, m;
					for (i = 0; i < 2; i++) {
						for (j = 0; j < gdp.getResolution(0); j++) {//rsss
							for (m = 0; m < fac1d; m++)
							{
								nflatSamples[0][l++] = flatSamples[0][k];
							}
							k++;
						}
						nflatSamples[0][l++] = flatSamples[0][k - 1];
					}
				}
				else
				{
					nflatSamples= new double[1][(gdp.getResolution(0) + 1) * 2];//rss
					int i, j;
					int k = 0, l = 0;
					for (i = 0; i < 2; i++) {
						for (j = 0; j < gdp.getResolution(0); j++) {//rss
								nflatSamples[0][l++] = flatSamples[0][k++];
						}
						nflatSamples[0][l++] = flatSamples[0][k - 1];
					}
				}
				flatSamples = nflatSamples;

			}
			// end apexp
			FlatField flatFieldValues = get1DFlatFieldValues(domainTuple,
					funcType, gdp, selectivity, actualSamples, addExtra);
			// MessageUtil.CPrintToConsole(flatFieldValues.getLength() + "
			// Lengths :: " + flatSamples[0].length + "," +
			// flatSamples[1].length);
			flatFieldValues.setSamples(flatSamples, false);

			// Add maps to display
			display1.addMap(latitudeMap);
			display1.addMap(longitudeMap);
			display1.addMap(planRGBMap);

			// Set maps ranges
			latitudeMap.setRange(0.0f, 100.0f);
			longitudeMap.setRange(0.0f, 1.0f);

			int maxPlanNumber = gdp.getMaxPlanNumber();
			planRGBMap.setRange(0, maxPlanNumber);

			DiagramUtil.setColorMap(maxPlanNumber, planRGBMap);

			latitudeMap.setScalarName(relNames[0]);
			longitudeMap.setScalarName(relNames[1]);

			Font hf = new Font(PicassoConstants.SCALE_FONT, Font.PLAIN, 10);

			AxisScale scale = latitudeMap.getAxisScale();
			scale.setSide(AxisScale.PRIMARY);
			scale.createStandardLabels(100.0, 0.0, 0.0, 20.0);
			scale.setFont(hf);
			scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);
			scale.setSnapToBox(true);
			//scale.setTitle(relations[0] + "." + attributes[0]);
			scale.setTitle(relations[0] + "." + attributes[0]+" ["+Double.toString((int) (gdp.getQueryPacket().getStartPoint(0)*100))+","+Double.toString((int) (gdp.getQueryPacket().getEndPoint(0)*100))+"]@ "+Integer.toString((int)gdp.getResolution(0)));
			if (isLongitudeScale) {
				scale = longitudeMap.getAxisScale();
				scale.setFont(hf);
				scale.setColor(PicassoConstants.IMAGE_TEXT_COLOR);
				scale.setSide(AxisScale.PRIMARY);
				// scale.createStandardLabels(1.0, 0.0, 0.0, 0.20);
				Hashtable labelTable = new Hashtable();
				labelTable.put(new Double(0.0), "0.0");
				labelTable.put(new Double(0.2), "0.2");
				labelTable.put(new Double(0.4), "0.4");
				labelTable.put(new Double(0.6), "0.6");
				labelTable.put(new Double(0.8), "0.8");
				labelTable.put(new Double(1.0), "1.0");
				scale.setLabelTable(labelTable);
				scale.setSnapToBox(true);
			} else {
				longitudeMap.setScaleEnable(false);
				// longitudeMap.setScaleColor(PicassoUtil.colorToFloats(PicassoConstants.IMAGE_BACKGROUND));
			}

			ProjectionControl projCont = display1.getProjectionControl();
			DisplayRenderer dRenderer = display1.getDisplayRenderer();
			dRenderer.setBoxOn(false);
			dRenderer.setBackgroundColor(PicassoConstants.IMAGE_BACKGROUND);
			// dRenderer.setForegroundColor(Color.BLACK);

			// Get display's graphics mode control and draw scales
			GraphicsModeControl dispGMC1 = (GraphicsModeControl) display1
					.getGraphicsModeControl();

			dispGMC1.setProjectionPolicy(DisplayImplJ3D.PARALLEL_PROJECTION);

			// dispGMC1.setPointSize(400/gdp.getResolution());
			dispGMC1.setPointSize(1.0f);
			dispGMC1.setPointMode(false);
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
			display1.addReference(dataRef);

			double[] aspect;
			aspect = new double[] { PicassoConstants.ASPECT_2D_X,
					PicassoConstants.ASPECT_2D_Y };
			projCont.setMatrix(display1.make_matrix(0, 0, 0, 0.4, 0, 0.1, 0)); //0.70 normal
			projCont.setAspectCartesian(aspect);
		} catch (OutOfMemoryError bounded) {
			JOptionPane.showMessageDialog(panel,
					"Out Of Memory Error, Please Restart PicassoClient",
					"Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			if(e.getMessage().equals("Gridded2DSet exception") && panel.getDBSettingsPanel().getSelecType()!=PicassoConstants.PICASSO_SELECTIVITY)
			{
				JOptionPane.showMessageDialog(panel, "The engine selectivity values are not monotonically non-decreasing. The plan diagram will not be shown.");
				panel.tabbedPane.setSelectedIndex(0);
			}
			e.printStackTrace();
		}
		return display1;
	}

	private static FlatField get1DFlatFieldValues(RealTupleType domainTuple,
			FunctionType funcType, DiagramPacket gdp, int selecType,
			double[] actualSamples, boolean addExtra) throws Exception {
		float[] selecValues = gdp.getPicassoSelectivity();
		int NCOLS = gdp.getResolution(0);//rss
		//apexp
		int fac1d = 10;
		//end apexp
		
		if (selecType == PicassoConstants.PICASSO_SELECTIVITY)
			selecValues = gdp.getPicassoSelectivity();
		else if (selecType == PicassoConstants.PLAN_SELECTIVITY)
			selecValues = gdp.getPlanSelectivity();
		else if (selecType == PicassoConstants.PREDICATE_SELECTIVITY)
			selecValues = gdp.getPredicateSelectivity();
		else if (selecType == PicassoConstants.DATA_SELECTIVITY)
			selecValues = gdp.getDataSelectivity();
		//1d fix added by modifiying addValue:1st Feb 2011
		if (addExtra) {
			int multipleFactor = (int) (100 / NCOLS);
			float[] sValues = new float[multipleFactor * NCOLS];
			double[] eas = new double[multipleFactor * NCOLS];
			int index = 0;
			float pointValue = 100*((float)gdp.getQueryPacket().getStartPoint(0));
			double addValue = (gdp.getQueryPacket().getEndPoint(0)-gdp.getQueryPacket().getStartPoint(0));// / (multipleFactor * NCOLS);
			for (int j = 0; j < NCOLS; j++) {
				for (int i = 0; i < multipleFactor; i++) {
					sValues[index] = (new Float(pointValue).floatValue());// selecValues[k*NCOLS+j];
					eas[index++] = actualSamples[j];
					// MessageUtil.CPrintToConsole(addValue + " Print Value :: "
					// + pointValue);
					pointValue +=addValue;
				}
			}
			sValues[sValues.length - 1] = (float)gdp.getQueryPacket().getEndPoint(0)*100;
			selecValues = sValues;
			actualSamples = eas;
			NCOLS = multipleFactor * NCOLS;
			/*
			 * float[] sValues = new float[NCOLS+1]; sValues[0] = 0;
			 * System.arraycopy(selecValues, 0, sValues, 1, NCOLS); NCOLS++;
			 * selecValues = sValues; double[] eas = new
			 * double[actualSamples.length+1]; eas[0] = actualSamples[0];
			 * System.arraycopy(actualSamples, 0, eas, 1, actualSamples.length);
			 * actualSamples = eas;
			 */
		}

		int index = 0;
		// apexp
		float[][] sampleSet;

		if (gdp.getQueryPacket().getDistribution().equals(
				PicassoConstants.UNIFORM_DISTRIBUTION)) {
			sampleSet = new float[2][NCOLS * 2];
			// end apexp
			for (int i = 0; i < NCOLS; i++) {
				sampleSet[0][index] = new Double(selecValues[i]).floatValue();
				sampleSet[1][index] = new Double(0.0).floatValue();
				// System.out.print("(" + sampleSet[0][index] + "," +
				// sampleSet[1][index] + ")");
				index++;
			}

			for (int i = 0; i < NCOLS; i++) {
				sampleSet[0][index] = new Double(selecValues[i]).floatValue();
				sampleSet[1][index] = new Double(actualSamples[i]).floatValue();
				// System.out.print("(" + sampleSet[0][index] + "," +
				// sampleSet[1][index] + ")");
				index++;
			}
			// System.out.println("");
			// apexp
		} else {

			if(gdp.getResolution(0)<10)//rss
			{
				//set the lowers
				float[] osampleSet = new float[(NCOLS + 1) * 2];
				for ( int i = 0; i < 2; i++ ) {
					for ( int j = 0; j <= NCOLS; j++ ) {
						if(j==NCOLS)
						{
							osampleSet[index]=(float)gdp.getQueryPacket().getEndPoint(0);
						}
						else
						{
							if(j!=0)
								osampleSet[index] = selecValues[j] - (selecValues[j]-selecValues[j-1])/2;
							else
								osampleSet[index] = selecValues[j]/2;
						}

						index++;
					} //end for j
				} //end for i 

				sampleSet = new float[2][(NCOLS * fac1d + 1) * 2];

				//bottom row
				index=0;
				for (int i = 0; i < NCOLS; i++) 
				{
					for (int j = 0; j < fac1d; j++) 
					{
						sampleSet[0][index] = osampleSet[i] + j*(osampleSet[i+1]-osampleSet[i])/(fac1d+1);
						sampleSet[1][index] = new Double(0.0).floatValue();
						// System.out.print("(" + sampleSet[0][index] + "," +
						// sampleSet[1][index] + ")");
						index++;
					}
				}
				sampleSet[0][index] = 100.0f;
				sampleSet[1][index] = 0.0f;
				index++;

				//other row
				for (int i = 0; i < NCOLS; i++) 
				{
					for (int j = 0; j < fac1d; j++) 
					{
						sampleSet[0][index] = osampleSet[i] + j*(osampleSet[i+1]-osampleSet[i])/(fac1d+1);

						sampleSet[1][index] = new Double(actualSamples[i])
						.floatValue();
						// System.out.print("(" + sampleSet[0][index] + "," +
						// sampleSet[1][index] + ")");
						index++;
					}
				}
				sampleSet[0][index] = (float)gdp.getQueryPacket().getEndPoint(0);
				sampleSet[1][index] = new Double(actualSamples[NCOLS - 1])
				.floatValue();
				index++;
			}
			else
			{
				float[][] osampleSet = new float[2][(NCOLS + 1) * 2];
				int k;
				for ( int i = 0; i < 2; i++ ) {
					k=0;
					for ( int j = 0; j <= NCOLS; j++ ) {
		
						if(j==NCOLS)
						{
							osampleSet[0][index]=(float)gdp.getQueryPacket().getEndPoint(0)*100;
							if(i==1)
								osampleSet[1][index] = (float)actualSamples[k-1];
							else
								osampleSet[1][index] =0.0f;
						}
						else
						{
							//if(j!=1)
							osampleSet[0][index] = selecValues[j];
							if(i==1)
								osampleSet[1][index] = (float)actualSamples[k];
							else
								osampleSet[1][index] =0.0f;
							k++;
							
							//else
							//osampleSet[1][index] = new Double(selecValues[j]/2).floatValue();
						}

						index++;
					} //end for j
				} //end for i 
				sampleSet = osampleSet;
			}
		}
		try {

			Gridded2DSet domainSet;
			if (gdp.getQueryPacket().getDistribution().equals(
					PicassoConstants.UNIFORM_DISTRIBUTION)) {
				sampleSet = validateSelecValues2(sampleSet, 2, NCOLS);
				domainSet = new Gridded2DSet(domainTuple, sampleSet, NCOLS, 2);
			} else {
				if(gdp.getResolution(0)<10)//rss
				{
					sampleSet = validateSelecValues2(sampleSet, 2, NCOLS*fac1d + 1);
					domainSet = new Gridded2DSet(domainTuple, sampleSet, NCOLS
						* fac1d + 1, 2);
				}
				else
				{
					sampleSet = validateSelecValues2(sampleSet, 2, NCOLS + 1);
					domainSet = new Gridded2DSet(domainTuple, sampleSet, NCOLS + 1, 2);
				}
			}
			// end apexp
			FlatField flatFieldValues = new FlatField(funcType, domainSet);
			return flatFieldValues;
		} catch (SetException se) {
			index = 0;
			for (int i = 0; i < NCOLS; i++) {
				/*System.out.println(i + " :: " + selecValues[i] + " :: ");
				System.out.println("");*/
			}
			se.printStackTrace();
			throw new Exception("Gridded2DSet exception");
			
		} catch (Exception e) {
			MessageUtil.CPrintToConsole("Exception in Gridded2DSet");
			e.printStackTrace();
			throw new Exception("Gridded2DSet exception");
		}
		//return null;
	}

	private static float[][] validateSelecValues2(float[][] s, int rows,
			int cols) {
		int i, j;
		float c, d = 1;

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
		
		int cnt,k;
		double maxval,diff,theval;
		//This part tries to create a small epsilon such that suppose your selvalues were
		//0.0005 0.0005 0.0005 0.00500001, your epsilon couldn't be such that 0.0005 + epsilon >(=) 0.00500001 		
		for(i=0;i<rows;i++)
		{
			for(j=0;j<cols;j++)
			{
				cnt=1;
				k=j;
				while(k!=0 && k!=cols && s[0][i*cols+k]==s[0][i*cols+k-1])
				{
					cnt++;
					k++;
				}
				if(cnt!=1)
				{
					if(k==cols)
					{
						maxval=0.999999999999999;
					}
					else 
					{
						maxval=s[0][i*cols+k]; //the greater one after the equal set
					}
					diff=maxval-s[0][i*cols+j]; //the difference between the bigger value and the equal set

					diff/=2;
					diff/=cnt; 
					//now diff (epsilon) is such that after adding it to all these same valued points, the final point will
					//still be before the 1st point with a different selectivity value.
					if(diff>1e-6) diff=1e-6;

					theval=s[0][i*cols+j];
					k=j;
					while(k!=0 && k!=cols && s[0][i*cols+k]==theval)
					{
						s[0][i*cols+k]=s[0][i*cols+k-1]+(float)diff; //add our epsilon to it
						k++;
					}
				} //end if(cnt!=1)
			} //end for j
		} //end for i
		
		
		return s;
	}

}

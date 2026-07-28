
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
//
package iisc.dsl.picasso.common.ds;

import java.io.Serializable;

//import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
//import java.util.ArrayList;
//import java.util.Vector;

import iisc.dsl.picasso.common.PicassoConstants;

public class DiagramPacket implements Serializable {
	private static final long serialVersionUID = 8449356788103077907L; //2L
	
	//The resolution of the plan diagram
	public int				resolution[];
	
	//Number of dimensions (:varies clauses) in the plan diagram
	int				dimension;

	//The total number of plans in the plan diagram
	int 			maxPlans;
	
	//The maximum and minimum costs and cardinalities of the points in the plan diagram
	double 			maxCost;
	double 			maxCard;
	double 			minCost;
	double 			minCard;
	
	
	//Picasso Selectivities
	float[]		picsel;
	
	//Plan Selectivities
	float[]		plansel;
	
	//Predicate Selectivities
	float[]		predsel;
	
	//Reserved
	float[]		datasel;
	
	//Constants corresponding to Picasso selectivities
	String[]		constants;
	
	//Attributes on which :varies clauses exist
	String[]		attributes;
	String []		attrTypes;
	
	//Relation names on whose attributes :varies clauses exist
	String[] 		relationNames;
	
	//For each point in the plan diagram:
	//1) PlanNumber (Note that this number is not the same as that displayed in the front end. There, 
	// the number is based on the decreasing order of the area covered by the plan).  
	//2) Cost
	//3) Cardinality
	//(These points are available in row-major order with the first point being that with the lowest
	// selectivity vector and the last point being that with the highest selectivity vector).
	DataValues[] 	dataPoints;
	
	//The query packet consists of:
	//The query template, the query template descriptor, diagram resolution, number of dimensions, 
	// execution type - compile-time or run-time, query point distribution - uniform or exponential,
	// plan difference level, database engine optimization level,
	// time at which the diagram generation was started, time that the generation took,
	// and the selectivity error threshold.
	QueryPacket		queryPacket;
	
	//ADG
//	ArrayList 		PlanTrees;
	public boolean  approxDiagram = false;
	double sampleSize;
	double areaError;
	double identityError;
	int samplingMode;
	boolean FPCMode;
	
	//Routines to manipulate the information in the DiagramPacket follow.
	
    public DiagramPacket()
    {
    }
    
    public DiagramPacket(DiagramPacket p)
    {
        this.dimension = p.dimension;
        resolution = new int[dimension];
        for(int i = 0; i < dimension; i++)
    		this.resolution[i] = p.resolution[i];
        this.maxCard = p.maxCard;
        this.maxCost = p.maxCost;
        this.maxPlans = p.maxPlans;
        this.minCard = p.minCard;
        this.minCost = p.minCost;
        
        this.picsel=copyfarray(p.picsel);
        this.plansel=copyfarray(p.plansel);
        this.predsel=copyfarray(p.predsel);
        if(p.datasel!=null)
        	this.datasel=copyfarray(p.datasel);
        this.constants=copysarray(p.constants);
        this.attributes=copysarray(p.attributes);
        this.attrTypes = copysarray(p.attrTypes);
        this.relationNames=copysarray(p.relationNames);
        this.dataPoints=copydata(p.dataPoints);
        this.queryPacket = new QueryPacket(p.queryPacket);
        this.approxDiagram = p.approxDiagram;
        this.sampleSize = p.sampleSize;
        this.areaError = p.areaError;
        this.identityError = p.identityError;
        this.samplingMode = p.samplingMode;
        this.FPCMode = p.FPCMode;
    }
    
    public float[] copyfarray(float[] source)
    {
        float[] target = new float[source.length];
        for(int i = 0 ;i<source.length;i++)
            target[i]=source[i];
        return target;
    }
    public String[] copysarray(String[] source)
    {
        String[] target = new String[source.length];
        for(int i = 0 ;i<source.length;i++){
            if(source[i]!=null)
                target[i]=new String(source[i]);
            else
                target[i]=null;
        }
        return target;
    }
        
    public DataValues[] copydata(DataValues[] source)
    {
        DataValues[] target = new DataValues[source.length];
        for(int i = 0 ;i<source.length;i++){
            target[i] = new DataValues();
            target[i].setPlanNumber(source[i].getPlanNumber());
            target[i].setCard(source[i].getCard());
            target[i].setCost(source[i].getCost());
        }
        return target;            
    }
                
     /* Function: createDiagramPacket
	 * This function takes the data required to draw a diagram, creates a packet 
	 * and sends it to the client. The packet format is as follows :
	 * 
	 * Max = <# of Axis>,<Max Plans><Max Cost><Max Cardinality>
	 * AxisName = <All the names to be given to the diagrams>
	 * HeaderData =  <Interval Values for all the headers>
	 * DataPoints = Tuple1&Tuple2...TupleN
	 * Tuple1 = <X,Y,Z..., Plan#, Cost, Cardinality
	 * 
	 * I think ideally we should just send it as an object and retreive it
	 * in the client later as an object..
	 */
	public static DiagramPacket createDiagramPacket(int dimension, int resolution[],
						String[] relationNames, String attrNames[], DataValues[] dataPoints, DataValues maxValues, DataValues minValues,
						float picsel[], float plansel[], float predsel[], float datasel[],String constants[], QueryPacket qp,boolean approxFlag) 
	{
		
		// Create the packet...
		DiagramPacket packet = new DiagramPacket();
		
		packet.resolution = new int [dimension]; // -ma
    	for(int i = 0; i < dimension; i++)
    		packet.setResolution(resolution[i], i);

		packet.setMaxConditions(dimension);
		packet.setMaxPlanNumber(maxValues.getPlanNumber());
		packet.setMaxCost(maxValues.getCost());
		packet.setMaxCard(maxValues.getCard());
		packet.setMinCost(minValues.getCost());
		packet.setMinCard(minValues.getCard());
		packet.setRelationNames(relationNames);
		packet.setAttributeNames(attrNames);
		packet.setDataPoints(dataPoints);
		packet.setPicassoSelectivity(picsel);
		packet.setPlanSelectivity(plansel);
		packet.setPredicateSelectivity(predsel);
		packet.setDataSelectivity(datasel);
		packet.setConstants(constants);
		packet.setQueryPacket(qp);
		packet.approxDiagram = approxFlag;				
		return(packet);
	}
	
	public static DiagramPacket createDiagramPacket(int dimension, int resolution[],
			String[] relationNames, String attrNames[], String attrTypes[], DataValues[] dataPoints, DataValues maxValues, DataValues minValues,
			float picsel[], float plansel[], float predsel[], float datasel[],String constants[], QueryPacket qp,boolean approxFlag) 
	{
	
		// Create the packet...
		DiagramPacket packet = new DiagramPacket();
		
		packet.resolution = new int [dimension]; // -ma
		for(int i = 0; i < dimension; i++)
		packet.setResolution(resolution[i], i);
		
		packet.setMaxConditions(dimension);
		packet.setMaxPlanNumber(maxValues.getPlanNumber());
		packet.setMaxCost(maxValues.getCost());
		packet.setMaxCard(maxValues.getCard());
		packet.setMinCost(minValues.getCost());
		packet.setMinCard(minValues.getCard());
		packet.setRelationNames(relationNames);
		packet.setAttributeNames(attrNames);
		packet.setAttrTypes(attrTypes);
		packet.setDataPoints(dataPoints);
		packet.setPicassoSelectivity(picsel);
		packet.setPlanSelectivity(plansel);
		packet.setPredicateSelectivity(predsel);
		packet.setDataSelectivity(datasel);
		packet.setConstants(constants);
		packet.setQueryPacket(qp);
		packet.approxDiagram = approxFlag;				
		return(packet);
	}
	

	/*
	 * Transposes a 2D Picasso Diagram
	 */
	public void transposeDiagram()
	{
		int i,j;
		if(dimension<2)
			return;
		String tmp;
		tmp = relationNames[1];
		relationNames[1] = relationNames[0];
		relationNames[0] = tmp;
		DataValues [] tdata = new DataValues [resolution[PicassoConstants.a[0]] * resolution[PicassoConstants.a[1]]];
		tmp =attributes[1];
		attributes[1] = attributes[0];
		attributes[0] = tmp;
		
		tmp =attrTypes[1];
		attrTypes[1] = attrTypes[0];
		attrTypes[0] = tmp;
		
		if(dimension > 2)
		{
			int k = 0;
			for(j = 0; j < resolution[PicassoConstants.a[1]]; j++)
				for(i = j; i < resolution[PicassoConstants.a[1]]*resolution[PicassoConstants.a[0]]; i+=resolution[PicassoConstants.a[1]])
					tdata[k++] = dataPoints[i];
			dataPoints = tdata;
		}
		else // 2D
		{
			int k = 0;
			
			for(j = 0; j < resolution[PicassoConstants.a[1]]; j++)
				for(i = j; i < resolution[PicassoConstants.a[1]]*resolution[PicassoConstants.a[0]]; i+=resolution[PicassoConstants.a[1]])
					tdata[k++] = dataPoints[i];
			dataPoints = tdata;
			
			/*System.out.println("BEFORE2");

			for(i = getResolution(PicassoConstants.a[1])-1; i >=0; i--)
			{
				for(j = 0; j < getResolution(PicassoConstants.a[0]); j++)
					System.out.print(tdata[i*getResolution(PicassoConstants.a[0])+j].getPlanNumber() + "  ");
				System.out.println("");
			}
			int yy = 0;
			System.out.println("AFTER");
			for(i = getResolution(PicassoConstants.a[1])-1; i >=0; i--)
			{
				for(j = 0; j < getResolution(PicassoConstants.a[0]); j++)
					System.out.print(dataPoints[i*getResolution(PicassoConstants.a[0])+j].getPlanNumber() + "  ");
				System.out.println("");
			}
			int xx = 0;*/
		}
		// rotating the arrays now
		/*k = 0;
		String [] newconstants = new String [resolution[0] + resolution[1]];
		for(i = resolution[0]; i < resolution[0] + resolution[1]; i++)
			newconstants[k++] = constants[i];
		for(i = 0; i < resolution[0]; i++)
			newconstants[k++] = constants[i];
		
		for(i = 0; i < newconstants.length; i++)
			constants[i] = newconstants[i];
		
		k = 0;
		float [] newfloats = new float [resolution[0] + resolution[1]];
		for(i = resolution[0]; i < resolution[0] + resolution[1]; i++)
			newfloats[k++] = picsel[i];
		for(i = 0; i < resolution[0]; i++)
			newfloats[k++] = picsel[i];
		for(i = 0; i < constants.length; i++)
			picsel[i] = newfloats[i];
		
		k = 0;
		for(i = resolution[0]; i < resolution[0] + resolution[1]; i++)
			newfloats[k++] = plansel[i];
		for(i = 0; i < resolution[0]; i++)
			newfloats[k++] = plansel[i];
		for(i = 0; i < constants.length; i++)
			plansel[i] = newfloats[i];
		
		k = 0;
		for(i = resolution[0]; i < resolution[0] + resolution[1]; i++)
			newfloats[k++] = predsel[i];
		for(i = 0; i < resolution[0]; i++)
			newfloats[k++] = predsel[i];
		for(i = 0; i < constants.length; i++)
			predsel[i] = newfloats[i];
		
		k = 0;
		for(i = resolution[0]; i < resolution[0] + resolution[1]; i++)
			newfloats[k++] = datasel[i];
		for(i = 0; i < resolution[0]; i++)
			newfloats[k++] = datasel[i];
		for(i = 0; i < constants.length; i++)
			datasel[i] = newfloats[i];
		int res;
		res = resolution[0];
		resolution[0] = resolution[1];
		resolution[1] = res;*/
		
		// int x = 0;
	}
	
	public void setQueryPacket(QueryPacket qp)
	{
		queryPacket = qp;
	}

	public QueryPacket getQueryPacket()
	{
		return queryPacket;
	}
	
	public void setResolution(int num, int dim) {
		if(resolution == null)
		{
			resolution = new int[PicassoConstants.NUM_DIMS];
			resolution[dim] = num;
		}
		if(resolution != null && dim < resolution.length)
			resolution[dim] = num;
	}
	
	public void setMaxConditions(int num) {
		dimension = num;
	}
	
	public void setMaxPlanNumber(int num) {
		maxPlans = num;
	}
	
	public void setMaxCost(double num) {
		maxCost = num;
	}
	
	public void setMaxCard(double num) {
		maxCard = num;
	}
	
	public void setMinCost(double num) {
		minCost = num;
	}
	
	public void setMinCard(double num) {
		minCard = num;
	}
	
	public void setRelationNames(String[] names) {
		relationNames = names;;
	}
	
	public void setAttributeNames(String[] names){
		attributes = names;
	}
	public void setDataPoints(DataValues[] data) {
		dataPoints = data;
	}
	public void setAttrTypes(String []types)
	{
		attrTypes = types;
	}
	public int getResolution(int dim) {
		if(resolution == null)
			return 1;
		if(dim < 0)
			return 1;
		if(dim < resolution.length)
			return resolution[dim];
		return 1;
	}
	public int [] getResolution()
	{
		return resolution;
	}
	
	public int getMaxPlanNumber() {
		return(maxPlans);
	}
	public int getMaxResolution()
	{
		if(resolution == null)
		{
			resolution = new int [1];
			return 1;
		}
		int maxres = resolution[0];
    	for(int i = 1; i < dimension; i++)
    		if(resolution[i] > maxres)
    			maxres = resolution[i];
    	return maxres;
	}
	public double getMaxCost() {
		return(maxCost);
	}
	
	public int getDimension() {
		return(dimension);
	}
	
	public double getMaxCard() {
		return(maxCard);
	}
	
	public double getMinCost() {
		return(minCost);
	}
	
	public double getMinCard() {
		return(minCard);
	}
	
	public String[] getRelationNames() {
		return(relationNames);
	}
	
	public String[] getAttributeNames() {
		return(attributes);
	}
	
	public String[] getAttributeTypes() {
		return(attrTypes);
	}
	
	public DataValues[] getData() {
		return(dataPoints);
	}
	
	public int[] getPlanCountArray() 
	{
		int[] planCount = new int[getMaxPlanNumber()];

		//Set dim depending on whether we are dealing with full packet or slice
		int dim;
		if(getDimension()==1) 
			dim = 1;
		else if(dataPoints.length > getResolution(0) * getResolution(1)) //full packet
		{
			dim = getDimension(); //set to actual number of dimensions
		}
		else //slice
		{
			if(getDimension()==1) dim = 1; 
			else dim = 2; //if actual dimension >= 2
		}
		
		float[] selvals;
   		selvals=picsel;
   		boolean scaleupflag = false;
   		
		if(queryPacket.getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
			for (int i=0; i < dataPoints.length; i++)
				planCount[dataPoints[i].getPlanNumber()]++;
		} 
		else // Exponential
		{
			for(int i = 0; i < getDimension(); i++)
			{
				if(getQueryPacket().getEndPoint(i) - getQueryPacket().getStartPoint(i) < 0.05)
					scaleupflag = true;
			}
			int idx[] = new int[getDimension()]; //will be set to all 0's.
			
			if(PicassoConstants.a[0] == -1 || PicassoConstants.a[1] == -1 )
			// this is necessary because after viewing a 1D diagram, one of the PicassConstats.a elements is -1. This is to remove this.
			{
				PicassoConstants.a[0] = 0;
				PicassoConstants.a[1] = 1;
			}
			for (int i=0; i < dataPoints.length; i++)
			{
				double fullval = 1.0;
				double curval = 0.0;
				int we;

				int []ressum = new int[getDimension()];
				for(int p = 1; p < getDimension(); p++)
					ressum[p] += ressum[p-1]+getResolution(p-1);
				//find the area represented by this point by multiplying its length in each dimension
				if(dim == 1)
				{
					if(i!=0 && i!=getResolution(0)-1)
					{
						curval=(selvals[i+1]-selvals[i-1])/2;
					}
					else if(i==0)
					{
						curval=selvals[0]+(selvals[1]-selvals[0])/2;
					}
					else //if(k==getResolution()-1)
					{
						curval=getQueryPacket().getEndPoint(0)*100-selvals[getResolution(0)-1] + (selvals[getResolution(0)-1]-selvals[getResolution(0)-2])/2;
					}
					fullval = curval;
				}
				else if(dim == 2)
				{
					for(we = 0; we < dim; we++)
					{
						if(idx[we]!=0 && idx[we]!=getResolution(PicassoConstants.a[we])-1)
						{
							curval=(selvals[ressum[PicassoConstants.a[we]]+ idx[we]+1]-selvals[ressum[PicassoConstants.a[we]]+ idx[we]-1])/2;
						}
						else if(idx[we]==0)
						{
							curval=/*getQueryPacket().getStartPoint(we)*100+*/(selvals[ressum[PicassoConstants.a[we]]+1]-selvals[ressum[PicassoConstants.a[we]]])/2;
						}
						else //if(idx[we]==getResolution()-1)
						{
							curval=getQueryPacket().getEndPoint(PicassoConstants.a[we])*100-selvals[ressum[PicassoConstants.a[we]]+getResolution(PicassoConstants.a[we])-1] + (selvals[ressum[PicassoConstants.a[we]]+getResolution(PicassoConstants.a[we])-1]-selvals[ressum[PicassoConstants.a[we]]+ getResolution(PicassoConstants.a[we])-2])/2;
						}
						if(!scaleupflag)
							fullval*=curval;
						else
							fullval*=(curval * 10);
					}
					
					for(int p = 0; p < dim; p++)
					{
						idx[p]++;
						if(idx[p] == getResolution(PicassoConstants.a[p]))
						{
							idx[p] = 0;
						}
						else break;
					}
				}
				else
				{
					// start ma
			   		// modifying selvals as required

					int dim1 = PicassoConstants.a[0];
			   		int dim2 = PicassoConstants.a[1];
			   		int res1 = getResolution(dim1);
			   		int res2 = getResolution(dim2);
			   		
					int res[] = new int[getDimension()];
					double startpt[] = new double[getDimension()];
					double endpt[] = new double[getDimension()];
					for(int k = 0; k < res.length; k++)
					{
						res[k] = getResolution(k);
						startpt[k] = getQueryPacket().getStartPoint(k);
						endpt[k] = getQueryPacket().getEndPoint(k);
					}
					
					// swapping resolution and startpoint, endpoint locally for use in the area calculation
					int t = res[dim1];
					res[dim1] = res[dim2];
					res[dim2] = t;
					
					double x = startpt[dim1];
					startpt[dim1] = startpt[dim2];
					startpt[dim2] = x;
					
					x = endpt[dim1];
					endpt[dim1] = endpt[dim2];
					endpt[dim2] = x;
					
					float []tvals = picsel;
					float temp1[] = new float[res1];
					float temp2[] = new float[res2];
					int index = 0;
					
					for(int k = 0; k < res1; k++)
					{
						temp1[k] = selvals[ressum[dim1] + k];
					}
					for(int k = 0; k < res2; k++)
					{
						temp2[k] = selvals[ressum[dim2] + k];
					}
					
					for(int k = 0; k < getDimension(); k++)
					{
						if(inA(k))
						{
							if(dim1 == k)
							{
								for(int j = 0; j < temp1.length; j++)
									tvals[index++] = temp1[j];
							}
							else if(dim2 == k)
							{
								for(int j = 0; j < temp2.length; j++)
									tvals[index++] = temp2[j];
							}
						}
						else
						{
							for(int j = 0; j < getResolution(k); j++)
								tvals[index++] = picsel[ressum[k] + j];
						}
					}
					selvals = tvals;
					
					for(we=0;we<dim;we++)
					{
						if(idx[we]!=0 && idx[we]!=res[we]-1)
						{
							curval=(selvals[idx[we]+1]-selvals[ idx[we]-1])/2;
						}
						else if(idx[we]==0)
						{
							curval=startpt[we]*100+(selvals[1]-selvals[0])/2;
						}
						else //if(idx[we]==getResolution()-1)
						{
							curval=endpt[we]*100-selvals[res[we]-1] + (selvals[res[we]-1]-selvals[res[we]-2])/2;
						}
	
						if(scaleupflag)
							fullval*=(curval*10);
						else 
							fullval*=curval;
					}
					
					for(int p = 0; p < dim; p++)
					{
						idx[p]++;
						if(idx[p] == res[p])
						{
							idx[p] = 0;
						}
						else break;
					}
				}
				planCount[dataPoints[i].getPlanNumber()]+=(fullval*100);
			} //end of for loop (through all points)
		} //end of else (exponential) part
		
		
		for (int i=0; i < dataPoints.length; i++) 
		{
			if(planCount[dataPoints[i].getPlanNumber()]==0) 
				planCount[dataPoints[i].getPlanNumber()] = 1;
		}
		
		/*if(scaleupflag)
		{
			for(int i = 0; i < planCount.length; i++)
				planCount[i] /= 100Math.pow(10, getDimension());
		}*/

		return planCount;
	}
	
	private boolean inA(int we)
	{
		for(int f = 0; f < PicassoConstants.a.length; f++)
			if(we == PicassoConstants.a[f])
				return true;
		return false;
	}
	
	/*
	 * Takes an array of the count of each of the plans..
	 * plan[0] = 300, plan[1] = 230 etc..
	 * returns an array which contains the plan# with max points and so on..
	 * The algo can be fine tuned further... right now it is very crude..
	 */
	public int[][] getSortedPlanArray() {
		int[] planCount = getPlanCountArray();
		int[][] sortedPlan = new int[3][planCount.length];
		
		int count = 0;
		int max = -1;
		int pos = -1;
		while (count < planCount.length) 
		{
			max = -1;
			for (int i=0; i < planCount.length; i++) {
				if ( planCount[i] > max ) {
					max = planCount[i];
					pos = i;
				}
			}
			sortedPlan[0][pos] = count; // plan #, for getting color..
			sortedPlan[1][pos] = max; // count
			sortedPlan[2][count] = pos; // For reverse mapping...
			count++;
			
			planCount[pos] = -1;
		}
		// added by ma
		int resprod = 1;
		for(int i = 0; i < this.dimension; i++)
			resprod *= this.queryPacket.getResolution(i);
		if(this.getData().length == resprod)
			setGlobalColours(sortedPlan, this.queryPacket.getDistribution());
		
		sortedPlan[0] = getGlobalColours();
		sortedPlan[2] = getGlobalColours2();
		return sortedPlan;
	}
	static int[] globalColours;
	static int[] globalColours2;
	public static int[] getGlobalColours()
	{
		return globalColours;
	}
	public static int[] getGlobalColours2()
	{
		return globalColours2;
	}
	public static void setGlobalColours(int [][] sortedplan, String dist)
	{
		// the following piece of code must work for both uniform and expo. 
		globalColours = new int [sortedplan[0].length];
		globalColours2 = new int [sortedplan[2].length];
		for(int i = 0; i < sortedplan[0].length; i++)
		{
			globalColours[i] = sortedplan[0][i];
			globalColours2[i] = sortedplan[2][i];
		}
	}
	// end addition by ma
	
	public float[] getPicassoSelectivity()
	{
		return picsel;
	}
	public void setPicassoSelectivity(float[] vals)
	{
		picsel = vals;
	}

	public float[] getPlanSelectivity()
	{
		return plansel;
	}
	public void setPlanSelectivity(float[] vals) 
	{
		plansel = vals;
	}

	public float[] getPredicateSelectivity()
	{
		return predsel;
	}
	public void setPredicateSelectivity(float[] vals) 
	{
		predsel = vals;
	}

	public float[] getDataSelectivity()
	{
		return datasel;
	}
	
	public void setDataSelectivity(float[] vals) 
	{
		datasel = vals;
	}
	public void setDimension(int dim) //-ma
	{
		dimension = dim;
	}
	public String[] getConstants()
	{
		return constants;
	}
	public void setConstants(String names[])
	{
		constants = names;
	}
	//ADG
	 public boolean isFPC()
	 {
		 return(FPCMode);
	 }
	 public int getSamplingMode()
     {
     	return(samplingMode);
     }
     public double getSampleSize()
     {
     	return(sampleSize);
     }
 	public void setApproxSampleSize(double sample,int sMode,boolean FPC)
    {
    	sampleSize = sample;    	
    	samplingMode = sMode;
    	FPCMode = FPC;
    }
     public double getAreaError()
     {
     	return(areaError);
     }
     public double getIdentityError()
     {
     	return(identityError);
     }
	 public void setApproxError(double AreaError,double IdentityError)
     {        	
     	areaError = AreaError;
     	identityError = IdentityError;     	
     }
}
//Notes: Transpose, getPlanCountArray have been changed. they need to be tested.



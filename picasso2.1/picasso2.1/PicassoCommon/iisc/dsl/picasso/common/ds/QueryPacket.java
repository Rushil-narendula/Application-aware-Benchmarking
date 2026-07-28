
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

package iisc.dsl.picasso.common.ds;

import iisc.dsl.picasso.common.PicassoConstants;

import java.io.Serializable;

public class QueryPacket implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String	queryTemplate;
	private String	queryName;
	private int		resolution[];
	private int		dimension;
	public double 	startPoint[]; // stores the starting point along a dimension 
	public double 	endPoint[]; // stores the end point along a dimension
	private String	execType;
	private String	distribution;
	private String	planDiffLevel;
	private String	optLevel;
	private long	genTime;
	private long	genDuration;
	private String  selecThreshold;
	private String  newQueryName; // used for the rename query template
	private int		estGenTime; // used to retain the estmated generation time for exact diagram
	public boolean  genSuccess;
	public boolean  dummyEntry = false;
        
    public QueryPacket()
    {
    	resolution = new int[PicassoConstants.NUM_DIMS];
    	startPoint = new double[PicassoConstants.NUM_DIMS];
    	endPoint = new double[PicassoConstants.NUM_DIMS];
    	estGenTime = -1;
    }

    public QueryPacket(QueryPacket p)
    {
        if(p.queryName!=null)
            this.queryName = new String(p.queryName);
        if(p.queryTemplate!=null)
            this.queryTemplate = new String(p.queryTemplate);
        
        this.dimension = p.dimension;
        resolution = new int[p.resolution.length];
    	startPoint = new double[p.getStartPoint().length];
    	endPoint = new double[p.getEndPoint().length];
    	
    	for(int i = 0; i < this.resolution.length; i++)
    	{
    		if(p.resolution != null)
    			this.resolution[i] = p.resolution[i];
    		else
    			System.out.println("WTF");
    	}
    	for(int i = 0; i < dimension; i++)
    	{	
    		this.startPoint[i] = p.startPoint[i];
    		this.endPoint[i] = p.endPoint[i];
    	}
        if(p.execType!=null)
            this.execType = new String(p.execType);
        if(p.distribution!=null)
            this.distribution = new String(p.distribution);
        if(p.planDiffLevel!=null)
            this.planDiffLevel = new String(p.planDiffLevel);
        if(p.optLevel!=null)
            this.optLevel = new String(p.optLevel);
        this.genTime = p.genTime;
        this.genDuration = p.genDuration;
        if(p.selecThreshold!=null)
            this.selecThreshold = new String(p.selecThreshold);
    	estGenTime = -1;
    }

    public void setQueryTemplate(String qt)
	{
		queryTemplate = qt;
	}
	public double [] getStartPoint()
    {
    	return startPoint;
    }
    public double [] getEndPoint()
    {
    	return endPoint;
    }
    public double getStartPoint(int dim)
    {
    	return startPoint[dim];
    }
    public double setStartPoint(double startpt, int dim)
    {
    	if(startPoint.length<PicassoConstants.NUM_DIMS)
			startPoint = new double [PicassoConstants.NUM_DIMS];
    	return startPoint[dim] = startpt;
    }
    public double getEndPoint(int dim)
    {
    	return endPoint[dim];
    }
    public double setEndPoint(double endpt, int dim)
    {
    	if(endPoint.length<PicassoConstants.NUM_DIMS)
			endPoint = new double [PicassoConstants.NUM_DIMS];
    	return endPoint[dim] = endpt;
    }
	public String getQueryTemplate()
	{
		return queryTemplate;
	}
	public String getTrimmedQueryTemplate()
	{
		return queryTemplate.replaceAll("\\s+","");
	}
	
	public void setQueryName(String qn)
	{
		queryName = qn;
	}
	public String getQueryName()
	{
		return queryName;
	}
	public String getNewQueryName()
	{
		return newQueryName;
	}
	public void setNewQueryName(String s)
	{
		newQueryName = s;
	}
	public void setResolution(int res, int dim)
	{	
		if(resolution.length<PicassoConstants.NUM_DIMS)
			resolution = new int [PicassoConstants.NUM_DIMS];
		resolution[dim] = res;
	}
	public int getMaxResolution()
	{
		int maxres = resolution[0];
    	for(int i = 1; i < dimension; i++)
    		if(resolution[i] > maxres)
    			maxres = resolution[i];
    	return maxres;
	}
	public int getResolution(int dim)
	{
		return resolution[dim];
	}
	public int[] getResolution()
	{
		return resolution;
	}
	
	public void setDimension(int dimension)
	{
		this.dimension = dimension;
	}
	public int getDimension()
	{
		return dimension;
	}

	public void setExecType(String execType)
	{
		this.execType = execType;
	}
	public String getExecType()
	{
		return execType;
	}

	public void setDistribution(String distribution)
	{
		this.distribution = distribution;
	}
	public String getDistribution()
	{
		return distribution;
	}

	public void setOptLevel(String optLevel)
	{
		this.optLevel = optLevel;
	}
	public String getOptLevel()
	{
		return optLevel;
	}
	
	public void setGenTime(long genTime)
	{
		this.genTime = genTime;
	}
	public long getGenTime()
	{
		return genTime;
	}
	
	public void setGenDuration(long genDuration)
	{
		this.genDuration = genDuration;
	}
	public long getGenDuration()
	{
		return genDuration;
	}

	public void setPlanDiffLevel(String planDiffLevel)
	{
		this.planDiffLevel = planDiffLevel;
	}
	
	public String getPlanDiffLevel()
	{
		return planDiffLevel;
	}
	
	public void setSelecThreshold(String thresh)
	{
		this.selecThreshold = thresh;
	}
	
	public String getSelecThreshold()
	{
		return selecThreshold;
	}
	public void setEstimatedTime(int genTime)
	{
		this.estGenTime = genTime;
	}
	
	public int getEstimatedTime()
	{
		return estGenTime;
	}
	
	public String toString() {
		if(this.execType.equals(PicassoConstants.RUNTIME_DIAGRAM))
			return(queryName + "    (C,E)");
		else if(execType.equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM))
			return(queryName + " (A)");
		else
			return(queryName + "    (C)");
	}
}

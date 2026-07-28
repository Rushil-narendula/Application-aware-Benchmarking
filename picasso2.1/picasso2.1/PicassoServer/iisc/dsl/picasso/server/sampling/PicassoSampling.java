package iisc.dsl.picasso.server.sampling;


import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.*;


import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ApproxParameters;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.mssql.MSSQLDatabase;
import iisc.dsl.picasso.server.db.oracle.OracleDatabase;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;
import iisc.dsl.picasso.server.db.sybase.SybaseDatabase;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.query.Query;
import iisc.dsl.picasso.server.PicassoDiagram;
import iisc.dsl.picasso.server.PicassoException;

import linpack.QR_j;

public abstract class PicassoSampling extends PicassoDiagram{

	private static final long serialVersionUID = 8039356788103077907L;
	
	protected DataValues[] 		actual_data;
	protected int[] 			planList;
	protected Vector<Plan> 		actual_plans;
	protected Vector<String>	plan_area;
	protected double 			areaError=0;
	protected double 			identityError = 0;	
	protected double 			errorPercent_I = 0;
	protected double 			errorPercent_L = 0;
	
	protected ApproxParameters 	ApproxParams;
	
	protected int 				reqSampleSize;
	protected int 				SamplingMode;
	protected int 				actualPlans;
	protected int 				sampleSize;	
	protected String 			CompareQuery = null;
	protected boolean 			devMode = false;
	public boolean 				stopCompile = false;
	protected int				totalSize;
	protected int				activity;
	protected int[] 			offsetFromOrigin;
	protected int reloc_resolution[];
	int LPDist = 1;
	boolean endIteration =false;
	int[] cbIndex;
	int[] currIndex;
	int[] dimVar;
	int[] planInx;
	int[] index;
	int lazyqtid;
	float[] planWts;
	int distance;
	long startTime;
	boolean stopNN=false;	
	boolean dimPresent = false;
	protected boolean FPCMode = false;			//This variable denotes whether AGS is running in FPC mode or not
	LinkedList<String> abstractPlanList = null;	//This list stores the abstract plan list found in sampling
	Query query;
	Histogram[] hist;
	
	boolean seerMode = true;
	double minCard = -1;
	long timeTaken = 0; 
	boolean isEstimationProcess = false;
	
	public PicassoSampling(Socket s, ObjectInputStream in, ObjectOutputStream out, ClientPacket cp, Database db) {
		super(s,  in,  out,  cp,  db);		
		ApproxParams = cp.getApproxParameters();
		try {
			SamplingMode = ApproxParams.SamplingMode;		
			FPCMode = ApproxParams.FPCMode;
		} catch (RuntimeException e) {
			e.printStackTrace();
		}		
		try{
			if(Integer.parseInt(ApproxParams.getValue("UserMode"))==1)
				devMode = true;
		}
		catch(Exception rSizeNotInt){
			devMode = false;
		}	
		try{
			errorPercent_I = Double.parseDouble(ApproxParams.getValue("IError"));			
		}
		catch(Exception eNotDouble){
			errorPercent_I = (double)10;
		}	
		try{

			errorPercent_L = Double.parseDouble(ApproxParams.getValue("LError"));			

		}

		catch(Exception eNotDouble){

			errorPercent_L = (double)10;

		}	
		if(devMode)
		{			
			CompareQuery = ApproxParams.getValue("CompareQuery");
			try{
				reqSampleSize = Integer.parseInt(ApproxParams.getValue("SampleSize"));
			}
			catch(Exception rSizeNotInt){
				reqSampleSize = -1;
			}
		}		
		
	}
	
	public FileWriter RMSwriter =  null;	
	
	public void initializeParams() throws PicassoException
	{
		try{			
			startTime = System.currentTimeMillis();
			database.emptyPlanTable();
			int qtid = database.getQTID(queryPacket.getQueryName());
			boolean flag = false;
			dimension = queryPacket.getDimension();
			
			/*
			 * If we already have Picasso Diagram generated and another request comes in... throw an Exception
			 */
			lazyqtid = -1;
			if(qtid >= 0)
			{
				QueryPacket sqp = database.getQueryPacket(queryPacket.getQueryName());
				for(int i= 0; i< dimension; i++)
					if(sqp.getResolution(i) != queryPacket.getResolution(i) || sqp.getStartPoint(i) != queryPacket.getStartPoint(i) || sqp.getEndPoint(i) != queryPacket.getEndPoint(i)) 
						flag=true;
				if((!sqp.getDistribution().equals(queryPacket.getDistribution())) ||
						(!sqp.getOptLevel().equals(queryPacket.getOptLevel())) ||
						(!sqp.getPlanDiffLevel().equals(queryPacket.getPlanDiffLevel())) ||
						(!sqp.getTrimmedQueryTemplate().equals(queryPacket.getTrimmedQueryTemplate())) || flag)
				{
					// database.deletePicassoDiagram(queryPacket.getQueryName());
					lazyqtid = qtid;
					qtid = -1;
				}
				else if(queryPacket.getExecType().equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM)){
					lazyqtid = qtid;
					// database.deletePicassoDiagram(queryPacket.getQueryName());
					qtid = -1;
				}
			}
			query = Query.getQuery(queryPacket,database);	
			
			if(!isEstimationProcess) {
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Approximate Compilation Diagram");
				ServerMessageUtil.SPrintToConsole("Generating Approximate Compilation Diagram");
			}
			
			dimension = query.getDimension();
			queryPacket.setDimension(dimension);
			
			reloc_resolution = queryPacket.getResolution();
			resolution = new int[dimension];
			for(int j=0;j<dimension;j++)
				resolution[j] = reloc_resolution[j];
			offsetFromOrigin = new int[dimension];
			for(int i=0;i<dimension;i++){		
				offsetFromOrigin[i] = 0;
			}	
			query.genConstants(resolution, queryPacket.getDistribution(), queryPacket.getStartPoint(), queryPacket.getEndPoint());
			

			totalSize=1;
			int ressum = 0;
			
			for(int i=0;i<dimension;i++){
				totalSize *= resolution[i];
				ressum += resolution[i];				
			}
			/* Initialize all data structures */
			plans = new Vector<Plan>();
			data = new DataValues[totalSize];
			picsel = new float[ressum];
			plansel = new float[ressum];
			predsel = new float[ressum];			
			relNames = new String[dimension];

			index = new int[dimension];
			currIndex = new int[dimension];
			hist = new Histogram[dimension];

			for(int i=0;i<dimension;i++){
				relNames[i] = query.getRelationName(i);
				index[i]=0;
				hist[i] = query.getHistogram(i);
			}
			if(skipOptimize())
			{
				
				actual_data = getActualPlan(CompareQuery);
				actualPlans = actual_plans.size();				
				planList = new int[actualPlans];
				for(int i=0;i<actualPlans;i++)
				{
					planList[i]=-1;
				}
			}
			plan_area = new Vector<String>();
			if(reqSampleSize <= 0)
				reqSampleSize = totalSize;
			if(FPCMode)
				abstractPlanList = new LinkedList<String>();		
			XMLplans = new Vector();
			trees = new Vector();
			long startTime = System.currentTimeMillis();
			queryPacket.setGenTime(startTime);		
			
			
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new PicassoException(e.getMessage());
		}	
	}
	
	protected void savePicassoDiagram()throws PicassoException 
	{
		try{
			Plan plan = null;
			int[] indextemp = new int[dimension];
			for(int j=0;j<dimension;j++)
			{
				index[j] = 0;
				indextemp[j]=0;
			}
			for(int j=0;j<dimension;j++)
			{
				for(int k=0;k<dimension;k++)
					index[k]=0;
				for(int i=0;i<resolution[j];i++)
				{
					index[j] = i;
					indextemp[j]=i;
					plan = (Plan)plans.get(data[getIndex(indextemp)].getPlanNumber());
					computeSelectivity(query,index,hist,plan);
				}
			}
			/*for(int k=0;k<totalSize;k++)
			{
				if(data[k].getCard() < 0)
						System.out.println("Card<0:"+k);									
			}*/
			/*
			 * If we have generated a diagram here, storedResolution is equal to the requested resolution
			 * Which we pass to the doReadPicassoDiagram() function.
			 */		
		
			long duration = System.currentTimeMillis() - startTime;
			queryPacket.setGenDuration(duration);
			/* Store the QTIDMap entry now */
			DecimalFormat df = new DecimalFormat("0.00E0");
			df.setMaximumFractionDigits(2);
			double sSize = (sampleSize*100.0)/(double)totalSize;
			//String qname = queryPacket.getQueryName();
			
			if(lazyqtid != -1)
				database.deletePicassoDiagram(queryPacket.getQueryName());
			
			int qtid = database.addApproxQueryPacket(queryPacket);
			storePicassoDiagram(qtid);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Saving Selectivity Logs");
			storeSelectivityLog(query, qtid, hist);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,30,"Saving Plan Trees");
			storePlans(qtid);
			storeApproxMap(qtid);
			if(database instanceof MSSQLDatabase && PicassoConstants.SAVE_XML_INTO_DATABASE == true)
				storeXMLPlans(qtid);
			String durStr = getTimeString((int)(System.currentTimeMillis()-startTime)/1000);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,100,"Finished. Duration "+durStr);
			DiagramPacket dp = doReadPicassoDiagram(query, resolution);
			if(dp == null)
				ServerMessageUtil.SPrintToConsole("Reading Picasso Diagram failed");
			/*Vector v = clientPacket.getDimensions();
			if(v.size()<2)
				throw new PicassoException("Dimensions to vary are not selected");
			int dim1 = ((Integer)v.get(0)).intValue();
			int dim2 = ((Integer)v.get(1)).intValue();
			if(dim2<dim1)
				dp.transposeDiagram();*/
			dp.setDataPoints(data);
			dp.setApproxError(errorPercent_L, errorPercent_I);			
			dp.setApproxSampleSize(sSize,SamplingMode,FPCMode);
			dp.approxDiagram = true;			
			
			ServerMessageUtil.sendPlanDiagram(sock, reader, writer, queryPacket, dp,trees);
		}catch(Exception e) {
			if(database.isConnected() && lazyqtid == -1)
				database.deletePicassoDiagram(queryPacket.getQueryName());
			e.printStackTrace();
			throw new PicassoException(e.getMessage());
		}
	}	
	
	protected void storeApproxMap(int qtid) throws PicassoException
	{
		//int sid = 0;
		try{
			Statement stmt = database.createStatement();
			
			String attribList = "QTID, SAMPLESIZE, SAMPLINGMODE, AREAERROR, IDENTITYERROR ,FPCMODE" ;
			String FPCval = "";
			
			if(FPCMode)
				FPCval += ",1";
			else
				FPCval += ",0";
			
			/*if(!devMode)
			{
				areaError = 101;
				identityError = 101;				
			}*/
			double sSize = (sampleSize*100.0)/(double)totalSize; 
			String temp = "insert into "+database.getSettings().getSchema()+".PicassoApproxMap ("+attribList+
			") values ("+qtid+", "/*+sid+", "*/+sSize+", "+SamplingMode+", "+errorPercent_L+", "+errorPercent_I+FPCval+")";
			//System.out.println(temp);
			stmt.executeUpdate("insert into "+database.getSettings().getSchema()+".PicassoApproxMap ("+attribList+
					") values ("+qtid+", "/*+sid+", "*/+sSize+", "+SamplingMode+", "+errorPercent_L+", "+errorPercent_I+FPCval+")");
			stmt.close();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error adding Approx QTIDMap Entry:"+e);
			throw new PicassoException("Error adding Approx QTIDMap Entry:"+e);
		}
		
	}
	public boolean skipOptimize()
	{
		if((devMode && !CompareQuery.equals("-1")) )
			return(true);
		return(false);
			
	}
	protected void setPlanResult() throws PicassoException
	{
		try{			
			
			Plan plan;		 
			for(int i=0;i<dimension;i++)
			{
				currIndex[i] = index[i] + offsetFromOrigin[i];				
			}
			//System.out.println(currIndex[0]+","+currIndex[1]);
			int inx = getIndex(index);
			int act_inx = getIndexRelocated(currIndex);
			/*if(data[inx]!=null && data[inx].isRepresentative)
				return;*/
			String planDiffLevel = queryPacket.getPlanDiffLevel();		
			MSSQLDatabase mdb=null;
			SybaseDatabase sdb=null;
			int planNumber;				
			String newQuery=null;
			String absPlan = null;			
			if(!skipOptimize())
			{				
				newQuery = query.generateQuery(currIndex);							 
				plan = database.getPlan(newQuery,query);
				
				if ( database instanceof MSSQLDatabase )
					plan.computeMSSQLHash(planDiffLevel);
				else
					plan.computeHash(planDiffLevel);
				planNumber = plan.getIndexInVector(plans);
				if(plan == null){
					ServerMessageUtil.SPrintToConsole("Error getting proper plan from database");
					throw new PicassoException("Error getting proper plan from database");
				}
			}
			else
			{
				try {
					plan = (Plan)actual_plans.get(actual_data[act_inx].getPlanNumber());
				} catch (RuntimeException e) {					
					plan = null;
				}
				planNumber = planList[actual_data[act_inx].getPlanNumber()] ;
			}			
			if(planNumber == -1){
				plans.add(plan);			
				planNumber=plans.size() - 1;			
				try {
					plan.setPlanNo(planNumber);
					
				} catch (RuntimeException e) {
					;
				}	
				if(skipOptimize())
					planList[actual_data[act_inx].getPlanNumber()] = planNumber;
				plan_area.add(""+1);
				if(FPCMode){
					if (database instanceof MSSQLDatabase){
						mdb = (MSSQLDatabase)database;
						absPlan = mdb.getAbsPlan(newQuery);
					}
					else if (database instanceof SybaseDatabase){
						sdb = (SybaseDatabase)database;
						absPlan = sdb.getAbsPlan(newQuery);
					}
					abstractPlanList.addLast(absPlan);
					//System.out.println(" Abstract plan "+planNumber+" saved");//\nPlan: "+absPlan);
				}				
				trees.add(new Integer(planNumber));
				trees.add(plan.createPlanTree());
			}
			else
				plan_area.set(planNumber,""+(Integer.parseInt(plan_area.get(planNumber).toString())+1));
			
			data[inx] = new DataValues();
			data[inx].setPlanNumber(planNumber);
			
			if(!skipOptimize())
			{						
				data[inx].setCard(plan.getCard());
				data[inx].setCost(plan.getCost());				
			}
			else
			{
				data[inx].setCard(actual_data[act_inx].getCard());
				data[inx].setCost(actual_data[act_inx].getCost());
				
			}
			data[inx].setRepresentative(true);			 
			sampleSize++;			
		}
		catch(Exception e1)
		{
			e1.printStackTrace();
		}
		
	}
	protected int errCost = 0;
	boolean isLP = false;
	protected double maxErrCost = 0.0;int costCount = 0;
	public DataValues setData(DataValues toData,DataValues fromData)
	{
		toData = new DataValues();
		toData.setPlanNumber(fromData.getPlanNumber());	
		toData.setCard(fromData.getCard());
		toData.setCost(fromData.getCost());		
		toData.setRepresentative(false);		
		return(toData);
	}
	protected double maxCost = 0.0,maxCardErr = 0.0,maxCard=0.0;
	public void compareRunTime()
	{		
		for(int i=0;i<dimension;i++){			
			index[i]=0;			
		}
		int inx = 0;
		areaError = 0;
		try {
			identityError  = ((double)actualPlans - (double)plans.size())/(double)actualPlans;
			identityError = identityError * 100;
		} catch (RuntimeException e) {			
			identityError = -1;
		}			
		try {
			while(index[dimension-1] <= resolution[dimension-1]-1){
				inx = getIndex(index);	
				
				if(planList[actual_data[inx].getPlanNumber()]!=data[inx].getPlanNumber())
				{
					areaError++;					
				}				
				for(int i=0;i<dimension;i++){
					if(index[i] < resolution[i]-1) {
						index[i]++;
						break;
					}else if(i == dimension -1)
						index[i]++;
					else
						index[i] = 0;
				}
			}			
			areaError = (areaError*100)/totalSize;
		} catch (RuntimeException e) {
			areaError = -1;
			e.printStackTrace();
		}	
		
		System.out.println("Sample Size : "+sampleSize+",Area error : "+areaError+",Identity Error : " + identityError);
	}
	protected int sendProgress(int count,int pointsEvaluated,long startTime,int activity)
	{	
		if(isEstimationProcess)
			ServerMessageUtil.sendStatusMessage(sock, reader, writer,0,"Estimating Approximation Time");
		else if(count++ == PicassoConstants.SEGMENT_SIZE)
		{
			// Check if thread has been interrupted, if so throw an interrupt exception
			try {
				ifInterruptedStop();
				ifPausedSleep();
				//ifCompileStopped();
			} catch (PicassoException e) {
				e.printStackTrace();
			}			
			DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(0);
            //This is to ensure that progress doesn't go beyond 100%
            //Not a very good way - find a better solution
            if(pointsEvaluated > totalSize)
            	pointsEvaluated = totalSize;
            //End
			double progress = 100 * pointsEvaluated / (double)totalSize;
			int sampleRemaining = (reqSampleSize-sampleSize);
			if(sampleRemaining<0)
				sampleRemaining = 0;			
			String durStr = getTimeString((int)(System.currentTimeMillis() - startTime)/1000);
            String statstr = "";
            
            if(activity == -1){
            	statstr = "  Preprocessing  ";
            }else if(activity == 0){
            	statstr = "  Plan Diagram Phase 1   ";
            }else if(activity == 1){
            	statstr = "  Plan Diagram Phase 2   ";
            }else if(activity == 2){
            	statstr = "  Cost and Cardinality Diagram Phase   ";
            }
            String message = statstr;
            if(activity >= 0){
            	if(progress%10 == 0)
	                System.out.println(statstr+""+Math.round(progress)+"% Completed "+"Elapsed Time: "+durStr);
	            message += df.format(progress)+"%    "+"Elapsed Time: "+durStr;
	            if(activity == 0 || activity == 1)
	            	message += "       Plans discovered: "+plans.size();
            }
            ServerMessageUtil.sendStatusMessage(sock, reader, writer, (int)progress,message);
			clientPacket.setProgress((int)progress);
           	count = 0;
		}
		return(count);
	}
	public void ifCompileStopped()throws PicassoException
	{
		if(stopCompile == true)
		{
			stopCompile = false;
			reqSampleSize = sampleSize;	
		}		
	}
	public int[] getIndexBack(int inx)
	{
		int[] index = new int[dimension];
		for(int i = 0; i < dimension;i++)
		{
			index[i] = inx % resolution[i];
			inx /= resolution[i];
		}
		return index;
	}
	/*Checks actual and approx diagrams have identical resolutions*/
	public boolean checkActualAndApproxResolution(QueryPacket actual, QueryPacket approx)
	{
		int[] actual_res = actual.getResolution();
		int[] approx_res = approx.getResolution();
		for(int i = 0;i < dimension;i++){
			if(actual_res[i] != approx_res[i])
				return false;
		}
		return true;
	}
	public DataValues[] getActualPlan(String sql) throws PicassoException
	{
		
		double[] step;
		int length=1;
		Hashtable selAttrib = clientPacket.getAttributeSelectivities();		
		DataValues[] p_data;

/*		int skipDim[]=null;
		double skipSel[]=null;
		if(selAttrib != null){
			skipDim = new int[selAttrib.size()];
			skipSel = new double[selAttrib.size()];
		}		
*/		
		actual_plans = new Vector<Plan>();		
		int actual_qtid = database.getQTID(sql);		
		QueryPacket approx_qp = database.getQueryPacket(sql);
		
		//if(sqp.getDimension() != queryPacket.getDimension() ||
		if((!approx_qp.getDistribution().equals(queryPacket.getDistribution())) ||
				(!approx_qp.getOptLevel().equals(queryPacket.getOptLevel())) ||
				(!approx_qp.getPlanDiffLevel().equals(queryPacket.getPlanDiffLevel())) ||				
				(dimension != approx_qp.getDimension()) ||
				!checkActualAndApproxResolution(queryPacket,approx_qp)) {
			ServerMessageUtil.SPrintToConsole("Actual & Approx Data are not matching!!!");
			throw new PicassoException("Actual & Approx Data are not matching!!!");
		}
		
		int[] storedResolution = approx_qp.getResolution();		
/*		if(resolution != approx_qp.getResolution() || dimension != approx_qp.getDimension())
		{
			ServerMessageUtil.SPrintToConsole("Actual & Approx Data are not matching!!!");
			throw new PicassoException("Actual & Approx Data are not matching!!!");
		}
*/		for(int i=0;i<dimension;i++)
			length *= resolution[i];
		
		p_data = new DataValues[length];
		String qText;
		qText= "select QID,PLANNO,CARD,COST from "+database.getSchema()+".PicassoPlanStore "+
			"where PicassoPlanStore.QTID="+actual_qtid+" order by PicassoPlanStore.QID";
		try{

			Statement stmt = database.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT SID,PICSEL,PLANSEL,PREDSEL,DATASEL,CONST from "+database.getSchema()+
					".PicassoSelectivityLog where QTID="+actual_qtid+" order by DIMENSION,SID");	
			
			int i=0;
			//int tmpindex=0;
			step = new double[dimension];
			for(int j = 0;j < dimension;j++)
				step[j] = (double)((storedResolution[j])) / (double)(resolution[j]);
			stmt = database.createStatement();
			rs = stmt.executeQuery(qText);
			int qid;
			DataValues max = new DataValues();
			DataValues min = new DataValues();
			min.setCost(1e300);
			min.setCard(1e300);
			double index[] = new double[dimension];
			
			for(i=0;i<dimension;i++)
				index[i]=step[i] / 2.0;
			i=-1;
			float cost,card;
			int planno;
			while(rs.next()){
				ifInterruptedStop();
				ifPausedSleep();
				boolean skipFlag=false;
				qid = rs.getInt(1);
				planno = rs.getInt(2);
				card = (float)rs.getDouble(3);
				cost = (float)rs.getDouble(4);
				if(cost > max.getCost())
					max.setCost(cost);
				if(planno+1 > max.getPlanNumber())
					max.setPlanNumber(planno+1);
				if(card > max.getCard())
					max.setCard(card);
				if(cost < min.getCost())
					min.setCost(cost);
				if(card < min.getCard())
					min.setCard(card);				
				if(qid<getIndex(index,storedResolution))
					continue;			
				
				for(int j=0;j<dimension;j++){
					 if(index[j] < storedResolution[j] - step[j]) 
					 {
						 index[j] += step[j];
						 break;
					 }
					 else if(j == dimension -1)
						 index[j] += step[j];
					 else
						 index[j] = step[j] / 2.0;
				 }
				
				 if(skipFlag)
					 continue;		 
				 
				 i++;
				 p_data[i] = new DataValues();
				 p_data[i].setPlanNumber(planno);
				 p_data[i].setCard(card);
				 p_data[i].setCost(cost);				
				 try{
					 if(actual_plans.get(planno) == null || actual_plans.get(planno).equals(null))
					 {
						 actual_plans.set(planno,Plan.getPlan(database,actual_qtid, p_data[i].getPlanNumber(),approx_qp.getPlanDiffLevel())); 
					 }
				 }
				 catch(Exception ex){
					 if(planno >= actual_plans.size())
						 while(planno >= actual_plans.size())
							 actual_plans.add(null);
					 actual_plans.set(planno,Plan.getPlan(database,actual_qtid, p_data[i].getPlanNumber(),approx_qp.getPlanDiffLevel()));
				 }			 
			}

			if(i+1<length){
				ServerMessageUtil.SPrintToConsole("Number of generated points="+(i+1)+" and length="+length);
				throw new PicassoException("Picasso Diagram was not generated properly.");
			}
			rs.close();
			stmt.close();			
			actualPlans = actual_plans.size();			
			return(p_data);
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error reading Plan Diagram "+e);
			throw new PicassoException("Error reading Plan Diagram "+e);
		}
	}
	
	protected double abstractPlanCost(Query query, int index[], int sourcePlan) throws PicassoException
	{
		
		int inx = getIndex(index); 
		if(data[inx]!=null &&  data[inx].FPCdone == true)
			return(data[inx].getCost());
		
		Plan plan;						
		String newQuery=null;
		//long startTime = System.currentTimeMillis();
		String absPlan = (String)abstractPlanList.get(sourcePlan);//Fetch the plan from list
		//Abstract plan costing for a particular index
		if (database instanceof MSSQLDatabase){
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\noption (use plan N'" + absPlan + "')\n";
		}else if (database instanceof SybaseDatabase){
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\nplan '" + absPlan + "'\n";
		}else{
			newQuery = query.generateQuery(index) ;
		}
		//end
		plan = database.getPlan(newQuery,query);
		//Check for the cost and return it
		double foreignPlanCost = plan.getCost();
		/*long durTime = System.currentTimeMillis();
		long timeTaken = (durTime-startTime);*/
		//System.out.println(sourcePlan + "--> Time taken by FPC: "+timeTaken);		
		return(foreignPlanCost);
	}
	protected Plan abstractPlan(Query query, int index[], int sourcePlan) throws PicassoException
	{
		int inx = getIndex(index); 
		if(data[inx]!=null && data[inx].FPCdone == true)
			return(plans.get(data[inx].getPlanNumber()));
		Plan plan;						
		String newQuery=null;
		//long startTime = System.currentTimeMillis();
		String absPlan = (String)abstractPlanList.get(sourcePlan);//Fetch the plan from list
		//Abstract plan costing for a particular index
		if (database instanceof MSSQLDatabase){
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\noption (use plan N'" + absPlan + "')\n";
		}else if (database instanceof SybaseDatabase){
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\nplan '" + absPlan + "'\n";
		}else{
			newQuery = query.generateQuery(index) ;
		}
		//end
		plan = database.getPlan(newQuery,query);
		//Check for the cost and return it
		//double foreignPlanCost = plan.getCost();
		//long durTime = System.currentTimeMillis();
		//long timeTaken = (durTime-startTime);
		//system.out.println("Time taken by FPC: "+timeTaken);
		return(plan);
	}

/*
	//Function to cost a local plan using abstract plan methods - cost should be equal
	private boolean checkCostCorrectness(Query query, int index[], int sourcePlan) throws PicassoException
	{
		Plan plan;				
		String newQuery=null;
		int inx = getIndex(index);
		double cost1,cost2;
		cost1 = data[inx].getCost();
		String absPlan = (String)abstractPlanList.get(sourcePlan);//Fetch the plan from list
		//Abstract plan costing for a particular index
		if (database instanceof MSSQLDatabase)
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\noption (use plan N'" + absPlan + "')\n";
		else if (database instanceof SybaseDatabase)
			newQuery = query.generateQuery(index) + "--Picasso_Abstract_Plan\nplan '" + absPlan + "'\n";
		//end
		newQuery = query.generateQuery(index);
		plan = database.getPlan(newQuery,query);
		//Check for the cost and return it
		cost2 = plan.getCost();
		if(Math.abs(cost1 - cost2) < 0.0005)
			return(true);
		return(false);
	}
*/	
	
	protected void lowPass()
	{		
		int []boxDim = new int[dimension];
		for(int i=0;i<dimension;i++){
			index[i]=resolution[i] - 1;
			boxDim[i]=1;
		}
		int inx;		
		endIteration = false;
		while(!endIteration)
		{			
			inx = getIndex(index);							
			try {
				if(!data[inx].isRepresentative)
				{				
					lowPass_ND(index);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			decrIndex(index,boxDim);			
		}
	}	
	double cardLP = 0;
	private void lowPass_ND(int[] index)
	{
		cbIndex = new int[dimension];		
		dimVar = new int[dimension];
		for(int i=0;i<dimension;i++)
		{
			cbIndex[i]=index[i];
		}
				
		distance = 1;		
		cardLP = 0;
		int num_plans = plans.size();
		planWts = new float[num_plans];
		planInx = new int[num_plans];
		for(int i=0;i<num_plans;i++)
		{
			planWts[i]=0;
			planInx[i]=-1;
		}
		nCount = 0.0;
		lpCost = 0.0;
		recursiveLowPass(dimension);
		//data[getIndex(cbIndex)].setCard(cardLP/nCount);
	}	
	double lpCost = 0,nCount=0;
	private void recursiveLowPass(int depth)
	{
		int pnt = depth-1;	
		//int inx = 0;		
		if(depth == dimension)
		{
			while(distance<=LPDist)
			{				
				dimPresent = false;
				if(dimension == 1)
				{
					dimVar[pnt]=distance;	
					doLPJob();					
					
					dimVar[pnt]=(short)-distance;	
					doLPJob();
				}
				else
					for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){					
						if(Math.abs(dimVar[pnt])==distance)
							dimPresent = true;
						else
							dimPresent = false;					
						recursiveLowPass(pnt);						
					}
				distance++;
			}			
		}
		else if(depth == 1)
		{
			if(!dimPresent)
			{
				dimVar[pnt]=distance;	
				doLPJob();					
				
				dimVar[pnt]=(short)-distance;	
				doLPJob();
				
			}
			else
				for(dimVar[pnt]=(short)-distance;dimVar[pnt]<=distance;dimVar[pnt]++){						
					doLPJob();					
				}
		}
		else
		{
			for(dimVar[pnt]=(short)-distance;dimVar[pnt]<=distance && !stopNN;dimVar[pnt]++){
				if(Math.abs(dimVar[pnt])==distance)
					dimPresent = true;
				recursiveLowPass(depth-1);
			}
		}
		
	}
	private void doLPJob()
	{
		int pNum = 0;
		int neighbor[] = new int[dimension];
		for(int i=0;i<dimension;i++)
		{
			neighbor[i] = cbIndex[i] + dimVar[i];
			if(neighbor[i]>resolution[i]-1 || neighbor[i]<0)
				return;
		}
		int inx = getIndex(neighbor);
		if(data[inx]==null)
			return;
		pNum = data[inx].getPlanNumber();							
		planWts[pNum]++;
		if(planInx[pNum] < 0)
			planInx[pNum] = inx;	
		if(data[inx].getCost() > 0)
		{
			lpCost += data[inx].getCost();			
			
		}
		if(data[inx].isRepresentative)
		{
			cardLP += 4*data[inx].getCard();
			nCount +=4;
		}
		cardLP += data[inx].getCard();
		nCount ++;
	}
	//decrements index by boxdim amount
	protected int[] decrIndex(int[] index,int[] boxDim)
	{
		endIteration = false;
		for(int i=0;i<dimension;i++){
			if(index[i] >= boxDim[i]) {
				index[i] -= boxDim[i];
				break;
			}else if(i == dimension -1)
			{				
				if(index[i]>=boxDim[i])
					index[i] -= boxDim[i];
				else
					endIteration = true;
			}
			else
				index[i] = resolution[i] - 1;
		}
	
		return(index);
	}
	protected int getPlanNumber(DataValues[] data,int[] index)
	{
		int inx = getIndex(index);
		return(data[inx].getPlanNumber());
	}
	/*
	 * Cost and Cardinality Estimation Functions
	 */
	
	double plan_coeff[][];
	CostFunction costF;
	CardFunction cardF;
	
	double x_picsel[];
	protected void findCardnCostFunction()
	{
		int res = 0;
		for(int j=0;j<dimension;j++)
		{
			for(int i = 0;i < resolution[j];i++){
				picsel[res + i] = (float)hist[j].getPicSel(i);
			}
			res += resolution[j];
		}
		cardF = new CardFunction(data,dimension,resolution,picsel);
		costF = new CostFunction(data,picsel,plans.size(),dimension,resolution);
	}
	
	protected double getCard(int []a_index)
	{	
		double card = cardF.getCard(getIndex(a_index));
		
		return card;
	}
	protected double getCost(int []a_index,int p_id)
	{	
		double cost = costF.getCost(getIndex(a_index), p_id);
		return cost;
	}	
	double fpcCost = 0;
	protected int findNearestNeighbor()
	{
		dimVar = new int[dimension];					
		distance = 1;		
		stopNN=false;
		int num_plans = plans.size();
		planWts = new float[num_plans];
		planInx = new int[num_plans];
		for(int i=0;i<num_plans;i++)
		{
			planWts[i]=0;
			planInx[i]=-1;
		}		
		recursiveNNSearch(dimension);
		double tmpDistance = 0;int pNum=-1;		
		int prevPNum = -1;
		for(int i=0;i<num_plans;i++)
		{
			if(planWts[i]<=0)
				continue;
			if(tmpDistance < planWts[i])
			{
				tmpDistance = planWts[i];				
				if(pNum >= 0 && FPCMode)
				{
					double fpcPrev = 0;
					double fpcNext = 0;
					if(prevPNum != pNum)
					{
						try {
							fpcPrev = abstractPlanCost(query,index,pNum);						
						} catch (PicassoException e) {						
							e.printStackTrace();
						}
						prevPNum = pNum;
						fpcCost = fpcPrev;
					}
					try {						
						fpcNext = abstractPlanCost(query,index,i);
					} catch (PicassoException e) {						
						e.printStackTrace();
					}					
					if(fpcNext < fpcPrev)
					{
						pNum = i;
						fpcCost = fpcNext;
						prevPNum = pNum;
					}
				}
				else
					pNum = i;
			}				
		}		
		return(planInx[pNum]);
	}		
	private void recursiveNNSearch(int depth)
	{
		int pnt = depth-1;		
		if(depth == dimension)
		{
			while(!stopNN)
			{				
				dimPresent = false;
				if(dimension == 1)
				{
					dimVar[pnt]=distance;	
					doNNJob();
					dimVar[pnt]=-distance;	
					doNNJob();
				}
				else					
					for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){					
						if(Math.abs(dimVar[pnt])==distance)
							dimPresent = true;
						else
							dimPresent = false;					
						recursiveNNSearch(pnt);						
					}
				distance++;
			}			
		}
		else if(depth == 1)
		{
			if(!dimPresent)
			{
				dimVar[pnt]=distance;	
				doNNJob();
				dimVar[pnt]=-distance;	
				doNNJob();
			}
			else
				for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){					
					doNNJob();				
				}
		}
		else
		{
			for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){
				if(Math.abs(dimVar[pnt])==distance)
					dimPresent = true;
				recursiveNNSearch(depth-1);
			}
		}
		
	}
	private void doNNJob()
	{
		int pNum = 0;
		int neighbor[] = new int[dimension];
		for(int i=0;i<dimension;i++)
		{
			neighbor[i] = index[i] + dimVar[i];
			if(neighbor[i]>resolution[i]-1 || neighbor[i]<0)
				return;
		}
		int inx = getIndex(neighbor);
		if(data[inx]==null || !data[inx].isRepresentative)
			return;
		pNum = data[inx].getPlanNumber();		
		planWts[pNum]++;
		if(planInx[pNum] < 0)
			planInx[pNum] = inx;
		stopNN=true;		
	}
	public double euclideanDist(int[] dist1,int[] dist2)
	{
		double dist=0;
		for(int i=0;i<dimension;i++)
		{
			dist += Math.pow(dist1[i]-dist2[i],2);
		}
		dist = Math.sqrt(dist);
		return(dist);
	}
	
	public double stdDev = 0.0,mean = 0.0;
	protected String sampleMode;
	
	public void doGenerateCostnCardDiagram()
	{
		double planMaxCost[] = new double[plans.size()]; 
		double planMinCost[] = new double[plans.size()];
		int progressCount = 0, pointsEvaluated = 0;
		for(int k=0;k<plans.size();k++)
		{
			planMaxCost[k] = 0;
			planMinCost[k] = 99999;
		}
		minCard = Integer.MAX_VALUE;
		maxCard = 0;
		maxCost = 0;
		activity = 2;
		for(int k=0;k<totalSize;k++)
		{
			if(data[k].isRepresentative == true)
			{
				if(maxCard < data[k].getCard())
					maxCard = data[k].getCard();
				if(minCard > data[k].getCard())
					minCard = data[k].getCard();
				if(maxCost < data[k].getCost())
					maxCost = data[k].getCost();
				if(planMaxCost[data[k].getPlanNumber()]<data[k].getCost())
					planMaxCost[data[k].getPlanNumber()]=data[k].getCost();
				if(planMinCost[data[k].getPlanNumber()]>data[k].getCost())
					planMinCost[data[k].getPlanNumber()]=data[k].getCost();
				pointsEvaluated++;
				progressCount++;
				progressCount = sendProgress(progressCount,pointsEvaluated,startTime,activity);
			}
			
		}
		double cost = 0,card=0;
		if(FPCMode)
		{
			Plan xplan;
			for(int k=0;k<totalSize;k++)
			{
				if(data[k].isRepresentative == false )
				{					
					if(devMode)
					{
						
						try {
							cost = actual_data[k].getCost();//use original cost if not area error
							card = actual_data[k].getCard();
							if(planList[actual_data[k].getPlanNumber()]!=data[k].getPlanNumber())
							{
								xplan = abstractPlan(query, index, data[k].getPlanNumber());
								cost = xplan.getCost();
								card = xplan.getCard();
							}
							data[k].setCost(cost);
							data[k].setCard(card);
						} catch (PicassoException e) {				
							;
						}						
					}
					else 
					{
						try {
							xplan = abstractPlan(query, index, data[k].getPlanNumber());
							cost = xplan.getCost();
							card = xplan.getCard();						
							data[k].setCost(cost);
							data[k].setCard(card);
						} catch (PicassoException e) {				
							;
						}
					}
					pointsEvaluated++;
					progressCount++;
					progressCount = sendProgress(progressCount,pointsEvaluated,startTime,activity);
				}
			}
		}
		else	
		{			
			findCardnCostFunction();	
			
			/************************CARD interpolation**********************/			
			for(int k=0;k<totalSize;k++)
			{
				if(!data[k].isRepresentative)
				{
					index = getIndexBack(k);				
					card = getCard(index);					
					card = Math.max(card,minCard);
					if(database instanceof OracleDatabase || database instanceof PostgresDatabase)
						card = Math.round(card);
					data[k].setCard(card);
				}
			}			
			/************************COST interpolation + WITH_CORRECT**********************/
			Vector<Integer> planPts[] = new Vector[plans.size()];
			for(int k=0;k<plans.size();k++)
			{
				planPts[k] = null;
			}
			int[] Ind = new int[dimension];
			for(int k=0;k<totalSize;k++)
			{	
				index = getIndexBack(k);
				int planNum = data[k].getPlanNumber();
				
				if(!data[k].isRepresentative)
				{				
					if( data[k].succProb >= 0.5)
					{
						cost = data[k].getCost();
						try {
							if(costF.plan_coeff[planNum] != null)
							{															
								cost = getCost(index, planNum);
								if(cost > planMinCost[planNum]*0.5 && cost < 2*planMaxCost[planNum])
									data[k].setCost(cost);									
							}					
						}			
						catch (RuntimeException e){;}					
					}
					else if(findDistance(k,findNearestNeighbor()) <= 2)
					{
						index = getIndexBack(k);
						cost = data[k].getCost();
						try {
							if(costF.plan_coeff[planNum] != null)
							{															
								cost = getCost(index, planNum);
								if(cost > planMinCost[planNum]*0.5 && cost < 2*planMaxCost[planNum])
									data[k].setCost(cost);										
							}					
						}			
						catch (RuntimeException e){
							
							;
							
						}
					}
					else
					{					
						int candidate = 0;					
						if(planPts[planNum]==null)
						{
							planPts[planNum] = new Vector<Integer>();
							for(int m=0;m<totalSize;m++)
							{							
								if(data[m].getPlanNumber() == planNum && data[m].isRepresentative)
								{
									Ind = getIndexBack(m);
									for(int d=0;d<dimension;d++)
									{
										Ind[d]+=1;
									}
									planPts[planNum].add(new Integer(getIndex(Ind)));
								}
							}
						}
						candidate = planPts[planNum].size();
						boolean isConvex = true;
						if(candidate >= dimension)
						{
							double [][] A = new double[dimension][candidate];
							double []B = new double[dimension];
							for(int m=0;m<candidate;m++)
							{
								index = getIndexBack(planPts[planNum].get(m));
								for(int d=0;d<dimension;d++)
								{
									A[d][m] = index[d]+1;
								}
							}
							index = getIndexBack(k);
							for(int d=0;d<dimension;d++)
							{
								B[d] = index[d]+1;
							}
							
							
							double auxQR[] = new double[candidate]; 
							int[] pivotData = new int[candidate];
							double[] tmpSpace = new double[candidate];
							
							QR_j.dqrdc_j(A, dimension, candidate, auxQR, pivotData, tmpSpace, 1); //Finding QR decomposition using LINPACK
							
							double[] Output = new double[dimension]; 
							double QB[] = new double[dimension];
							double QTB[] = new double[dimension];
							double residual[] = new double[dimension];
							double XB[] = new double[dimension];
							
							try {
								QR_j.dqrsl_j(A, dimension, dimension, auxQR, B, QB, QTB, Output, residual, XB, 111); //Finding solution of Ax=B using LINPACK
							} catch (RuntimeException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}					
							for(int c=0;c<A.length;c++)
							{
								if(Output[c]>1 || Output[c]<0)
								{
									isConvex = false;
									break;
								}
							}
						}					
						if(isConvex)
						{
							index = getIndexBack(k);
							cost = data[k].getCost();
							try {
								if(costF.plan_coeff[planNum] != null)
								{															
									cost = getCost(index, planNum);
									if(cost > planMinCost[planNum]*0.5 && cost < 2*planMaxCost[planNum])
										data[k].setCost(cost);										
								}					
							}			
							catch (RuntimeException e){
								
								;
								
							}
						}
						else
						{
							index = getIndexBack(k);
							neighbor_count = 3;
							neighbor_plan = planNum;
							findNearestPlanNeighbor();
							if(planInx[0] != -1 && planInx[1] != -1)
							{							
								double alpha = findDistance(k,planInx[0])/findDistance(planInx[1],planInx[0]);
								cost = 0.0;
								if(alpha != 1)
									cost = data[planInx[0]].getCost() + alpha * (data[planInx[1]].getCost() - data[planInx[0]].getCost());
								else
									cost = (data[planInx[0]].getCost() + data[planInx[0]].getCost())/2.0; 
								
								/*int degree = -1;							 
								for(int m=0;m<3;m++)
								{
									if(planInx[m] > 0)
										degree++;								
								}
								float [] pos = new float[degree+1];
								float [] val = new float[degree+1];
								for(int m=0;m<degree+1;m++)
								{
									pos[m] = findDistance(0, planInx[m]);
									val[m] = (float)data[planInx[m]].getCost();
								}
								float cost =  lagrangeInterpolatingPolynomial(pos,val,degree,findDistance(0, k));	*/						
								
								if(cost > planMinCost[planNum]*0.5 && cost < 2*planMaxCost[planNum])
									data[k].setCost(cost);
							}
						}
						}			
					}				
				}			
		}
	}
	/****************Cost Interpolation Procedures: START*****************/
	int neighbor_plan, neighbor_count;
	protected float findDistance(int p,int q)
	{
		int pInx[] = new int[dimension]; 
		int qInx[] = new int[dimension];
		pInx = getIndexBack(p);
		qInx = getIndexBack(q);
		float maxDistance = 0;
		for(int i=0;i<dimension;i++)
		{
			maxDistance += Math.pow(pInx[i]-qInx[i],2);
			/*if(maxDistance < tmp)
				maxDistance = tmp;*/
		}
		return (float)Math.sqrt(maxDistance);
	}
	protected void findNearestPlanNeighbor()
	{
				
		dimVar = new int[dimension];					
		distance = 1;		
		stopNN=false;			
		planInx = new int[neighbor_count];
		planWts = new float[neighbor_count];
		for(int i=0;i<neighbor_count;i++)
		{			
			planInx[i]=-1;
		}		
		recursiveNNSearchForCost(dimension);		
	}		
	private void recursiveNNSearchForCost(int depth)
	{
		int pnt = depth-1;		
		if(depth == dimension)
		{
			while(!stopNN)
			{				
				dimPresent = false;
				if(dimension == 1)
				{
					dimVar[pnt]=distance;	
					doNNJob(dimVar[pnt]);
					dimVar[pnt]=-distance;	
					doNNJob(dimVar[pnt]);
				}
				else					
					for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){					
						if(Math.abs(dimVar[pnt])==distance)
							dimPresent = true;
						else
							dimPresent = false;					
						recursiveNNSearchForCost(pnt);						
					}
				distance++;
			}			
		}
		else if(depth == 1)
		{
			if(!dimPresent)
			{
				dimVar[pnt]=distance;	
				doNNJob(dimVar[pnt]);
				dimVar[pnt]=-distance;	
				doNNJob(dimVar[pnt]);
			}
			else
				for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){					
					doNNJob(dimVar[pnt]);				
				}
		}
		else
		{
			for(dimVar[pnt]=-distance;dimVar[pnt]<=distance;dimVar[pnt]++){
				if(Math.abs(dimVar[pnt])==distance)
					dimPresent = true;
				recursiveNNSearchForCost(depth-1);
			}
		}
		
	}	
	private void doNNJob(int dist)
	{
		for(int i=0;i<dimension;i++)
		{
			if(distance > resolution[i]/2)
			{
				stopNN = true;
				return;
			}
		}
				
		int pNum = 0;
		int neighbor[] = new int[dimension];
		for(int i=0;i<dimension;i++)
		{
			neighbor[i] = index[i] + dimVar[i];
			if(neighbor[i]>resolution[i]-1 || neighbor[i]<0)
				return;
		}
		int inx = getIndex(neighbor);
		if(data[inx]==null || !data[inx].isRepresentative)
			return;
		pNum = data[inx].getPlanNumber();	
		boolean isModified = false;
		if(pNum == neighbor_plan)
		{
			int i=0;
			for(;i<neighbor_count;i++)
			{
				if(planInx[i]<0)
				{
					/*if(i>0 && planWts[i-1]==dist)
						continue;
					else{*/
						planInx[i] = inx;					
						planWts[i] = dist;
						isModified = true;
						break;
					//}
				}
			}
			if(isModified && i==neighbor_count)
				stopNN=true;
		}				
	}
	/****************Cost Interpolation Procedures: END*****************/
	
	
	public void setOffset(int[] offset)
	{
		offsetFromOrigin = new int[dimension];
		for(int i=0;i<dimension;i++){		
			offsetFromOrigin[i] = offset[i];
		}
	}
	/*public void setPoints(int inx)
	{
		for(int i=0;i<dimension;i++)
		{
			currIndex[i] = index[i] + offsetFromOrigin[i];
		}
		compiledPts.add(""+currIndex);		
	}*/
	protected int getIndexRelocated(int[] index)
	{
		int tmp=0;
		for(int i=index.length-1;i>=0;i--)
			tmp=tmp*reloc_resolution[i]+index[i];
		return tmp;
	}
	public int getRelocatedPoints(int inx)
	{
		currIndex = getIndexBack(inx);
		for(int i=0;i<dimension;i++)
		{
			currIndex[i] += offsetFromOrigin[i];
		}
		return getIndex(currIndex);	
	}
	boolean isDone = false;
	public int estimateTime() throws PicassoException
	{		
		/*if(!isDone){
			try {
				initializeParams();
				isDone = true;
			} catch (PicassoException e) {			
				e.printStackTrace();
			}
		}
		try {			
			int orig[] = new int[dimension];
			int len[] = new int[dimension];
			for(int i=0;i<dimension;i++)
			{
				orig[i] = 0;
				len[i] = 16;
			}			
			estimateSampleSize(orig,len);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		return(60);
	}
	
}


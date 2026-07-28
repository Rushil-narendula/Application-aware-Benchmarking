
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

package iisc.dsl.picasso.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.mssql.MSSQLDatabase;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;
import iisc.dsl.picasso.server.db.sybase.SybaseDatabase;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.query.Query;
public class PicassoDiagram {
	protected Socket 			sock;
	protected ObjectInputStream 	reader;
	protected ObjectOutputStream 		writer;
	
	protected Database database;
	protected QueryPacket queryPacket;
	protected ClientPacket clientPacket;
	
	/*
	 * The following variables are used for reading and generating Picasso Diagrams
	 * They will be initialized in the respective functions
	 */
	protected int dimension;
	protected int resolution[];
	protected String[] relNames;
	protected String[] attribNames;
	protected String[] attribTypes;
	protected DataValues[] data;
	protected Vector<Plan> plans;
	protected Vector trees;
	protected float picsel[];
	protected float plansel[];
	protected float predsel[];
	protected float datasel[];
	protected String constants[];
	protected Vector<String> XMLplans;
	protected int clientId;
	
	public PicassoDiagram(Socket s, ObjectInputStream in, ObjectOutputStream out, ClientPacket cp, Database db)
	{
		sock = s;
		reader = in;
		writer = out;
		database = db;
		clientPacket = cp;
		clientId = new Integer(cp.getClientId()).intValue();
		queryPacket = cp.getQueryPacket();
	}
	int HelpEstimatingTime(long duration, int qtid ,int dimension, int []resolution,long numpoints, double pointpos)
	{
	    int time =0;
	    int myqid=0,res=1,i=0;
	    for(i=0;i<dimension;i++)
	    {
	    	myqid += pointpos*res;
	    	res=res*resolution[i];
	    }
		try 
		{
			String qtext = "select sum(COST) from "+database.getSchema()+".PicassoPlanStore where PicassoPlanStore.QTID="+qtid;
			String qtext1 = "select COST from "+database.getSchema()+".PicassoPlanStore where PicassoPlanStore.QTID="+qtid+" and QID="+myqid;
			Statement st = database.createStatement();
			ResultSet rs = st.executeQuery(qtext);
			int flag=0;
			double totcost=0, mycost=0;
			
			if(rs.next())
			    totcost = rs.getDouble(1);
			else
			    flag=1;
			    
			rs.close();
			rs=st.executeQuery(qtext1);
			
			if(rs.next())
			    mycost = rs.getDouble(1);
			else
			    flag=1;
			rs.close();
			st.close();
			if(flag == 0)
			    time = (int)((double)totcost * duration/mycost)/1000;
			else
			    time = (int)((double)numpoints*(duration)/1000);
		} 
		catch (Exception ex) 
		{
			time = (int)((double)numpoints*(duration)/1000);
			ex.printStackTrace();
		}
	    return time;
	}
	int estimateGenerationTime(Query query,boolean existing) throws PicassoException
	{
        if(queryPacket.getQueryName()==null || queryPacket.getQueryName().trim().equals(""))
        	ServerMessageUtil.sendErrorMessage(sock, reader, writer,"QTID empty. Diagram could not be generated.");
        long startTime, endTime;
		int duration=0, duration2=0;
		String execType = queryPacket.getExecType();
		long numPoints=1;
		dimension = query.getDimension();
		if(resolution == null)
			resolution = new int[dimension];
			
		for(int i = 0; i < dimension; i++)
			resolution[i] = queryPacket.getResolution(i); // -ma
			
		double sel[] = new double[dimension];
		/*try{*/
			// Compiling a query to avoid cold cache issue
		for(int i=0;i<dimension;i++)
			sel[i]=PicassoConstants.MID_DIAGONAL;
			
		database.removeFromPlanTable(PicassoConstants.HIGH_QNO);
		Plan plan = database.getPlan(query.generateQuery(sel),PicassoConstants.HIGH_QNO);
		/*
		 * keep the database clean always. Otherwise we may lead to lot of problems including
		 * NullPointerException when we call getPlan for DB2 and Oracle
		 */
		database.removeFromPlanTable(PicassoConstants.HIGH_QNO);
		/*
		 * We time the query for compilation at both 5% selectivity and 95% selectivity.
		 * The higher value of these two timings are send to the client after converting to a time string.
         * For execution diagram, the query at 5% selectivity is timed.
		 */
		 
		for(int i=0;i<dimension;i++)
			numPoints *= resolution[i];
			
		//overflow
		if(numPoints != (int)numPoints)
		{
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"More query points requested than Picasso can handle.",MessageIds.ERROR_ID);
			return -1;
		}
		
		try{
			if(execType.equals(PicassoConstants.RUNTIME_DIAGRAM))
			{
				for(int i=0;i<dimension;i++)
					sel[i]=PicassoConstants.LOWER_DIAGONAL;
					
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating time to generate; please wait...");
				startTime = System.currentTimeMillis();
				Statement stmt = database.createStatement();
				ResultSet rs = stmt.executeQuery(query.generateQuery(sel));
				
				while(rs.next());
				
				rs.close();
				stmt.close();
				endTime = System.currentTimeMillis();
				duration += HelpEstimatingTime((endTime - startTime), database.getQTID(queryPacket.getQueryName()) ,dimension, resolution, numPoints, PicassoConstants.LOWER_DIAGONAL);
				return duration;
			}
		} 
		catch(SQLException e) 
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error estimating Duration "+e);
			throw new PicassoException("Estimating expected duration of Picasso Diagram generation failed.");
		}
		
		long times[]=new long[5];
		for(int j=0;j<5;j++)
		{
			for(int i=0;i<dimension;i++)
				sel[i]=0.1+0.2*j;
				
			startTime = System.currentTimeMillis();
			if(existing==false)
			{
				database.removeFromPlanTable(PicassoConstants.HIGH_QNO);
			plan = database.getPlan(query.generateQuery(sel),PicassoConstants.HIGH_QNO);
			database.removeFromPlanTable(PicassoConstants.HIGH_QNO);
			plan.createPlanTree();
			}
			endTime = System.currentTimeMillis();
			times[j]=endTime - startTime;
		}		
		//Sort to take median
		for(int i=0;i<4;i++)
		{
			for(int j=0;j<5-i-1;j++)
			{
				if(times[j]>times[j+1])
				{
					long temp=times[j];
					times[j]=times[j+1];
					times[j+1]=temp;
				}
			}
		}
		int i=0;
		while(i<5 && times[i]==0) i++;
		// If all values are 0, make all of them 10 msec.
		if(i==5) { for(int j=4;j>=0;j--) { times[j]=10; } }
		// Else, set all the 0's to the lowest non-zero value.
		else	 { for(int j=i;j>0;j--) { times[j-1]=times[j]; } }
		//return totdur/10;
		//use median
		//2*resolution for the predsel queries
		int ressum=0;
		for(int q = 0; q < resolution.length; q++)
			ressum+=resolution[q];
		return (int)((numPoints+ressum)*(times[2])/1000.0) + 30;
	}

	int lazyqtid;
	
	protected void generatePicassoDiagram() throws PicassoException
	{
		database.emptyPlanTable();
		int qtid = database.getQTID(queryPacket.getQueryName());
		boolean flag = false;
		/*
		 * If we already have Picasso Diagram generated and another request comes in... throw an Exception
		 */
		
		lazyqtid = -1;
		if(qtid >= 0)
		{
			QueryPacket sqp = database.getQueryPacket(queryPacket.getQueryName());
			for(int i= 0; i< dimension; i++)
				if(sqp.getResolution(i) != queryPacket.getResolution(i) || sqp.getStartPoint(i) != queryPacket.getStartPoint(i) || sqp.getEndPoint(i) != queryPacket.getEndPoint(i))
				{
					flag=true;
					break;
				}
			if((!sqp.getDistribution().equals(queryPacket.getDistribution())) ||
					(!sqp.getOptLevel().equals(queryPacket.getOptLevel())) ||
					(!sqp.getPlanDiffLevel().equals(queryPacket.getPlanDiffLevel())) ||
					(!sqp.getTrimmedQueryTemplate().equals(queryPacket.getTrimmedQueryTemplate())) || flag)
			{
				// database.deletePicassoDiagram(queryPacket.getQueryName());
				lazyqtid = qtid;
				qtid = -1;
			}
			else if(sqp.getExecType().equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM) && queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM)) {
				lazyqtid = qtid;
				qtid = -1;
			}
			else if(queryPacket.getExecType().equals(PicassoConstants.COMPILETIME_DIAGRAM)){
				// database.deletePicassoDiagram(queryPacket.getQueryName());
				lazyqtid = qtid;
				qtid = -1;
			}
			else if(queryPacket.getQueryTemplate().equals(sqp.getQueryTemplate()))
			{
				lazyqtid = qtid;
				qtid = -1;
			}
		}
		Query query = Query.getQuery(queryPacket,database);
		/*
		 * If Someone wants to create Execdiagram directly without first having Picasso Diagram then...
		 */
		if(queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM) && qtid < 0){
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Compilation Diagram");
			long estDuration = queryPacket.getGenDuration();
			genPicassoDiagram(query);
			queryPacket.setGenDuration(estDuration);
			qtid = database.getQTID(queryPacket.getQueryName());

			if(PicassoConstants.LIMITED_DIAGRAMS==true)
			{
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only Compilation Diagrams are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
				return;
			}
			ServerMessageUtil.SPrintToConsole("Generating Execution Diagram");
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Execution Diagram");
			genExecDiagram(query,qtid);
		}
		else{
			if(queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM)) {
				if(PicassoConstants.LIMITED_DIAGRAMS==true)
				{
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only Compilation Diagrams are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
					return;
				}
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Execution Diagram");
				genExecDiagram(query,qtid);
			} else{
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Compilation Diagram");
				genPicassoDiagram(query);
			}
		}
		/*
		 * If we have generated a diagram here, storedResolution is equal to the requested resolution
		 * Which we pass to the doReadPicassoDiagram() function.
		 */
		DiagramPacket dp = doReadPicassoDiagram(query, resolution);
		if(dp == null)
			ServerMessageUtil.SPrintToConsole("Reading Picasso Diagram failed");
		//apa
//		Vector v = clientPacket.getDimensions();
//		if(v.size()<2)
//			throw new PicassoException("Dimensions to vary are not selected");
//		int dim1 = ((Integer)v.get(0)).intValue();
//		int dim2 = ((Integer)v.get(1)).intValue();
//		if(dim2<dim1)
//			dp.transposeDiagram();
		//apae
		
		//doGenerate would have filled trees
		ServerMessageUtil.sendPlanDiagram(sock, reader, writer, queryPacket, dp, trees);
	}

	protected void genExecDiagram(Query query, int qtid) throws PicassoException
	{
		try
		{
			String qtext = "select sum(COST) from "+database.getSchema()+".PicassoPlanStore where PicassoPlanStore.QTID="+qtid;
                        double totalEstimatedTime=0,total_CurrentEstimatedTime=0,total_Duration=0;
                        long timeLeft, beginTime, estTime;
			Statement stmt;
			ResultSet rs;
			
			dimension = query.getDimension();
			if(resolution == null)
				resolution = new int[dimension];
			for(int i = 0; i < dimension; i++)
				resolution[i] = queryPacket.getResolution(i);


			int index[] = new int[dimension];
			Histogram hist[] = new Histogram[dimension];

			for(int i=0;i<dimension;i++)
			{
				index[i]=0;
				hist[i] = query.getHistogram(i);
			}
			long startTime, duration;
			int totalSize = 1, count;
			
			for(int i=0;i<dimension;i++)
				totalSize *= resolution[i];
			
			beginTime = System.currentTimeMillis();
			estTime = queryPacket.getGenDuration();
			System.out.println("Prev. Estimated Time is "+estTime);
			
			query.genConstants(resolution,queryPacket.getDistribution(), queryPacket.getStartPoint(), queryPacket.getEndPoint());
			
            stmt = database.createStatement();
            rs = stmt.executeQuery(qtext);
            if(rs.next())
            	totalEstimatedTime = rs.getDouble(1);
            
			while(index[dimension-1] < resolution[dimension-1]) 
			{
				/*
				 * Generate a query and run it. Number of tuples returned is the RUNCARD and
				 * RUNCOST is the time taken to run the query completely
				 */
				String newQuery = query.generateQuery(index);
				stmt = database.createStatement();
				startTime=System.currentTimeMillis();
				count=0;
				rs = stmt.executeQuery(newQuery);
				while(rs.next())
					count++;
				duration = System.currentTimeMillis() - startTime;
				rs.close();
				stmt.close();
				stmt = database.createStatement();
				stmt.executeUpdate("UPDATE "+database.getSchema()+".PicassoPlanStore SET RUNCOST="+(duration/1000.0)+", RUNCARD="+count+
						" where QTID="+qtid+" and QID="+getIndex(index));
				stmt.close();
				/*
				 * Increment index set
				 */
				ifInterruptedStop();
				ifPausedSleep();
				for(int i=0;i<dimension;i++)
				{
					if(index[i] < resolution[i]-1) 
					{
						index[i]++;
						break;
					}
					else if(i == dimension -1)
						index[i]++;
					else
						index[i] = 0;
				}
				/*
				 * Get the progress info and send it to Client
				 */
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(0);
				double progress = 100*getIndex(index)/(double)totalSize;
				if(progress!=0)
				{
	                double currentEstimatedTime=0;
	                stmt = database.createStatement();
	                String qtext1 = "select sum(COST) from "+database.getSchema()+".PicassoPlanStore where PicassoPlanStore.QTID="+qtid+" and QID="+getIndex(index);
	                rs = stmt.executeQuery(qtext1);
	                if(rs.next())
	                    currentEstimatedTime = rs.getDouble(1);
	                total_CurrentEstimatedTime+=currentEstimatedTime;
	                total_Duration+=duration;
	                timeLeft = (long)((double)totalEstimatedTime * total_Duration/total_CurrentEstimatedTime)/1000;
	                totalEstimatedTime-=currentEstimatedTime;
                 }
				else
					timeLeft = estTime;

				String timeStr = getTimeString((int)timeLeft);
				String durStr = getTimeString((int)(System.currentTimeMillis() - beginTime)/1000);
                if(progress%10 == 0)
                    System.out.println(""+progress+"% Completed "+"Elapsed Time: "+durStr+"      Estimated Time to Complete: "+timeStr);
                ServerMessageUtil.SPrintToConsole(progress+"% completed. Runtime Cost of query "+getIndex(index)+
						" is "+(duration/1000)+" seconds");
				ServerMessageUtil.sendStatusMessage(sock, reader, writer, (int)progress,
						df.format(progress)+"%    "+"Elapsed Time: "+durStr+"      Estimated Time to Complete: "+timeStr);
				clientPacket.setProgress((int)progress);
			}
			stmt = database.createStatement();
			stmt.executeUpdate("UPDATE "+database.getSchema()+".PicassoQTIDMap SET EXECTYPE='"+PicassoConstants.RUNTIME_DIAGRAM+"' where QTID="+qtid);
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			throw new PicassoException("Cannot generate Execution Diagram");
		}
	}

	protected void genPicassoDiagram(Query query) throws PicassoException
	{
		try
		{
			/* Initialize all data structures */
			ServerMessageUtil.SPrintToConsole("Generating Compilation Diagram");
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Compilation Diagram");
	
			dimension = query.getDimension();
			queryPacket.setDimension(dimension);
			if(resolution == null)
				resolution = new int[dimension];
			for(int i = 0; i < dimension; i++)
				resolution[i] = queryPacket.getResolution(i);
	
			query.genConstants(resolution, queryPacket.getDistribution(), queryPacket.getStartPoint(), queryPacket.getEndPoint());
	
			int totalSize=1, ressum = 0;
			for(int i=0;i<dimension;i++)
			{
				totalSize*=resolution[i];
				ressum += resolution[i];
			}
			/* Initialize all datastructures */
			plans = new Vector();
			XMLplans = new Vector();
			trees = new Vector();
			data = new DataValues[totalSize];
			picsel = new float[ressum];
			plansel = new float[ressum];
			predsel = new float[ressum];
			relNames = new String[dimension];
	
			int index[] = new int[dimension];
			Histogram hist[] = new Histogram[dimension];
	
			for(int i=0;i<dimension;i++)
			{
				relNames[i] = query.getRelationName(i);
				index[i]=0;
				hist[i] = query.getHistogram(i);
			}
			long startTime = System.currentTimeMillis();
			queryPacket.setGenTime(startTime);
			/* Generate and store Picasso Diagram */
			doGeneratePicassoDiagram(query, index, hist, startTime);
			long duration = System.currentTimeMillis() - startTime;
			queryPacket.setGenDuration(duration);
			
			// Delete the diagram now if it existed before. We are doing a lazy delete of the diagram now.
			if(lazyqtid != -1)
			{
				database.deletePicassoDiagram(queryPacket.getQueryName());
				// The below line is reqd. so that the above delete (if it was done) to the database is committed 
				// database.reConnect();
			}
			
			/* Store the QTIDMap entry now */
			int qtid = database.addQueryPacket(queryPacket);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Saving Picasso Diagram");
	        storePicassoDiagram(qtid);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Saving Selectivity Logs");
			storeSelectivityLog(query, qtid, hist);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,30,"Saving Plan Trees");
			storePlans(qtid);
			if(database instanceof MSSQLDatabase && PicassoConstants.SAVE_XML_INTO_DATABASE == true)
				storeXMLPlans(qtid);
			
			// PicassoConstants.SAVING_DONE = true;
			String durStr = getTimeString((int)(System.currentTimeMillis()-startTime)/1000);
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,100,"Finished. Duration "+durStr);
		}
		catch(Exception e) 
		{
			if(database.isConnected() && lazyqtid == -1)
				database.deletePicassoDiagram(queryPacket.getQueryName());
			e.printStackTrace();
			throw new PicassoException(e.getMessage());
		}
	}

	private void doGeneratePicassoDiagram(Query query, int index[], Histogram hist[], long startTime)	throws PicassoException
	{
		final String planDiffLevel = queryPacket.getPlanDiffLevel();
		final int dimension = query.getDimension();
		final long estTime = queryPacket.getGenDuration();
		
		int totalSize = 1;
		int count = 0;
		long timeLeft = estTime;
		int inx=0;

		String newQuery;
		Plan plan;
		int planNumber;
		
		// dimension = query.getDimension();
		queryPacket.setDimension(dimension);
		if(resolution == null)
			resolution = new int[dimension];
		for(int i=0;i<dimension;i++)
			totalSize *= resolution[i]; // -ma

		database.emptyPlanTable();
		
		boolean equal = true;
		
		for(int i = 0; i < dimension-1; i++)
			if(resolution[i] != resolution[i+1])
			{
				equal = false;
				break;
			}
//		File tempfile=new File("D:\\bruhathi\\bruhathi\\hash\\"+queryPacket.getQueryName()+".txt");
//		PrintStream po=null;
//		FileOutputStream fos=null;
//		
//		try {
//			fos=new FileOutputStream(tempfile);
//			po = new PrintStream(fos);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
		while(index[dimension-1] < resolution[dimension-1]) 
		{
			ifInterruptedStop();
			ifPausedSleep();
			newQuery = query.generateQuery(index);
			plan = database.getPlan(newQuery,query);
			if(plan == null){
				ServerMessageUtil.SPrintToConsole("Error getting proper plan from database");
				throw new PicassoException("Error getting proper plan from database");
			}

			if ( database instanceof MSSQLDatabase )
				plan.computeMSSQLHash(planDiffLevel);
			else
				plan.computeHash(planDiffLevel);
			
			
			planNumber = plan.getIndexInVector(plans);
			
			if(planNumber == -1)
			{
				plans.add(plan);
				planNumber=plans.size() - 1;
				plan.setPlanNo(planNumber);
//				po.println("Plan Number and Plan Hash "+planNumber +" "+plan.getHash());
				//System.out.println(newQuery);
				//plan.show();
				//plan.showPlan(0);
				if(PicassoConstants.SAVE_XML_INTO_DATABASE == true && database instanceof MSSQLDatabase )
				{
					MSSQLDatabase mdb = (MSSQLDatabase)database;
					String xplan = mdb.getAbsPlan(newQuery);
					XMLplans.add(xplan);
				}
				trees.add(new Integer(planNumber));
				trees.add(plan.createPlanTree());
			}
			
			/*
			 * Compute various kinds of selectivities if the index is on the diagonal of the hypercube
			 * i.e. if index[0]..index[dimension] are all equal (this is checked inside the function).
			 */
			if(equal) {
				computeSelectivity2(query,index,hist,plan);
			}
			else {
				computeSelectivity(query,index,hist,plan);
			}

			inx = getIndex(index);
			data[inx] = new DataValues();
			data[inx].setPlanNumber(planNumber);
			data[inx].setCard(plan.getCard());
			data[inx].setCost(plan.getCost());
			
			/*
			 * Here we increment the index for the next point in selectivity space
			 */
			for(int i=0;i<dimension;i++)
			{
				if(index[i] < resolution[i]-1) 
				{
					index[i]++;
					break;
				}
				else if(i == dimension -1)
					index[i]++;
				else
					index[i] = 0;
			}			
			if(count == PicassoConstants.SEGMENT_SIZE)
				database.emptyPlanTable();

			if(count == totalSize/100) // Status message is sent to the client every 1% // PicassoConstants.STATUS_REFRESH_SIZE)
			{
				// Check if thread has been interrupted, if so throw an interrupt exception
				ifInterruptedStop();
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(0);
				
				//progress is a percentage (0 to 100)
				double progress = 100 * getIndex(index) / (double)totalSize;
				
				if(progress>10)
					timeLeft = (long)(((100.0-progress)/progress) * (System.currentTimeMillis() - startTime)/1000);
				
				else if(progress>0)
					timeLeft = (long)(((100.0-progress)/progress) * (System.currentTimeMillis() - startTime)/1000);
					//timeLeft = (long)(((100.0-progress)/100.0) * (estTime));
			
				else
					timeLeft = estTime;

				String timeStr = getTimeString((int)timeLeft);
				String durStr = getTimeString((int)(System.currentTimeMillis() - startTime)/1000);

				ServerMessageUtil.sendStatusMessage(sock, reader, writer, (int)progress,
						df.format(progress)+"%    "+"Elapsed Time: "+durStr+"      Estimated Time to Complete: "+timeStr);
				clientPacket.setProgress((int)progress);
				count = 0;
			}
			count++;
		}
//		try {
//			po.close();
//			fos.close();
//		} catch(IOException io) {
//			io.printStackTrace();
//		}
		//just add the number of trees at the start of the trees vector
		Vector puttrees = new Vector();
		puttrees.add(new Integer(plans.size()));
		puttrees.addAll(trees);
		trees = new Vector(puttrees);
	}

	// computes the selectivity log
	protected void computeSelectivity(Query query, int index[], Histogram hist[], Plan plan)
	{
		boolean computeSelectivity = false;
		int zerocount = 0, pos = 0, toadd = 0;
		int k;
		for(int i=0;i<dimension;i++)
			if(index[i] == 0)
				zerocount++;

		// TODO: Dont even call this function when we know computeSelectivity will be false
		if(zerocount >= dimension - 1)
			computeSelectivity = true;
		else return;

		if(computeSelectivity)
		{
			for(int i = 0 ; i < dimension; i++)
				if(index[i] != 0)
				{
					pos = i;
					break;
				}
		}
		if(computeSelectivity)
		{
			double osel = 0.0, dpsel = 0.0;
			int [] ressum = new int [dimension];
			
			for(int i = 1; i < dimension; i++)
				ressum[i] = ressum[i - 1] + resolution[i - 1];
			
			if(zerocount == dimension)
			{
				for(int i=0;i<dimension;i++)
				{
					toadd = ressum[i];
					osel=0.0;
					dpsel = hist[i].getPicSel(index[i]);
					String name;
					if(database instanceof SybaseDatabase) 
					{
						if(query.getAliasName(i) != null && !query.getAliasName(i).equals(""))
							name = query.getAliasName(i);
						else
							name = hist[i].getRelationName();
					}
					else if(database instanceof PostgresDatabase) {
						if(query.getAliasName(i) != null && !query.getAliasName(i).equals("") && !query.getAliasName(i).equals(hist[i].getRelationName()))
							name = hist[i].getRelationName()+" "+query.getAliasName(i);
						else
							name = hist[i].getRelationName();
					}
					else
						name = hist[i].getRelationName();
					// System.out.println("index,osel,dpsel " + index[0] + " " + index[1] + " " + osel + " " + dpsel);
					plansel[toadd] = (float) plan.getSelectivity(name,hist[i].getCardinality(),dpsel,osel);
					predsel[toadd] = (float)hist[i].getPredSel(index[i]);
				}
			}
			else
			{			
				toadd = ressum[pos] + index[pos];
				
				osel = plansel[toadd-1];
				dpsel = hist[pos].getPicSel(index[pos]) - hist[pos].getPicSel(index[pos]-1);
				
				// System.out.println("index,osel,dpsel " + index[0] + " " + index[1] + " " + osel + " " + dpsel);
				String name;
				if(database instanceof SybaseDatabase) {
					if(query.getAliasName(pos) != null && !query.getAliasName(pos).equals(""))
						name = query.getAliasName(pos);
					else
						name = hist[pos].getRelationName();
				}
				else if(database instanceof PostgresDatabase) {
					if(query.getAliasName(pos) != null && !query.getAliasName(pos).equals("") && !query.getAliasName(pos).equals(hist[pos].getRelationName()))
						name = hist[pos].getRelationName()+" "+query.getAliasName(pos);
					else
						name = hist[pos].getRelationName();
				}
				else
					name = hist[pos].getRelationName();
				
				/*if(toadd==5)
					k = 0;*/
				plansel[toadd] = (float) plan.getSelectivity(name,hist[pos].getCardinality(),dpsel,osel);
				// System.out.println(plansel[toadd]+ " " + name + " " + hist[pos].getCardinality() + " "+ dpsel + " "+ osel);
				predsel[toadd] = (float)hist[pos].getPredSel(index[pos]);
			}
		}
		/*
		DecimalFormat df = new DecimalFormat("0.00");
		 String msg="";
		 for(int i=0;i<dimension;i++)
		 	msg+=index[i]+", ";
		 msg += " : ";
		 for(int i=0;i<dimension;i++)
		 	msg+=df.format(hist[i].getPicSel(index[i]))+", ";
		 ServerMessageUtil.SPrintToConsole(msg + " : Query "+ getIndex(index) + " Plan is : ");
		 */
		k = 0;
	}

	// This function calculates the predsel and plansel for points along the diagonal. called only for homogeneous resolutions
	protected void computeSelectivity2(Query query, int index[], Histogram hist[], Plan plan)
	{
		int k;
		boolean computeSelectivity = true;
		for(int i=1;i<dimension;i++){
			if(index[i]!=index[i-1]){
				computeSelectivity = false;
				break;
			}
		}
		int res = resolution[0];
		if(computeSelectivity){
			double osel, dpsel;
			for(int i=0;i<dimension;i++){
				if(index[0]==0){
					osel=0.0;
					dpsel = hist[i].getPicSel(index[0]);
				}
				else{
					osel = plansel[i*res+index[0]-1];
					dpsel = hist[i].getPicSel(index[0]) - hist[i].getPicSel(index[0]-1);
				}
				// System.out.println("index,osel,dpsel " + index[0] + " " + index[1] + " " + osel + " " + dpsel);
				String name;
				if(database instanceof SybaseDatabase) {
					if(query.getAliasName(i) != null && !query.getAliasName(i).equals(""))
						name = query.getAliasName(i);
					else
						name = hist[i].getRelationName();
				}
				else if(database instanceof PostgresDatabase) {
					if(query.getAliasName(i) != null && !query.getAliasName(i).equals("") && !query.getAliasName(i).equals(hist[i].getRelationName()))
						name = hist[i].getRelationName()+" "+query.getAliasName(i);
					else
						name = hist[i].getRelationName();
				}
				else
					name = hist[i].getRelationName();
				/*if(index[0]==5)
					k = 0;*/
				plansel[i*res+index[0]]=	(float) plan.getSelectivity(name,hist[i].getCardinality(),dpsel,osel);
				// System.out.println(plansel[i*resolution+index[0]]+ " " + name + " " + hist[i].getCardinality() + " "+ dpsel + " "+ osel);
				predsel[i*res+index[0]] = (float)hist[i].getPredSel(index[0]);
			}
		}
		/*
		DecimalFormat df = new DecimalFormat("0.00");
		 String msg="";
		 for(int i=0;i<dimension;i++)
		 	msg+=index[i]+", ";
		 msg += " : ";
		 for(int i=0;i<dimension;i++)
		 	msg+=df.format(hist[i].getPicSel(index[i]))+", ";
		 ServerMessageUtil.SPrintToConsole(msg + " : Query "+ getIndex(index) + " Plan is : ");
		 */
	}

	void readPicassoDiagram() throws PicassoException
	{
		int estTime;
		int qtid = database.getQTID(queryPacket.getQueryName());
		Query query = Query.getQuery(queryPacket, database);
		QueryPacket sqp = database.getQueryPacket(queryPacket.getQueryName());
		if(qtid<0)
		{
			if(PicassoConstants.LIMITED_DIAGRAMS == true)
			{
				if(clientPacket.getQueryPacket().getMaxResolution() > 10)
				{
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only diagrams with Plot Resolution 10 are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
					return;
				}
				if (clientPacket.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
				{
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only Compilation Diagrams are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
					return;
				}
			}
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating time to generate");
			estTime = estimateGenerationTime(query,false);
			ServerMessageUtil.SPrintToConsole("Estimated time to generate is "+estTime);
			if(database instanceof MSSQLDatabase || database instanceof SybaseDatabase)
				ServerMessageUtil.sendEstimatedTime(sock, reader, writer, queryPacket, estTime,MessageIds.Class2);
			else
				ServerMessageUtil.sendEstimatedTime(sock, reader, writer, queryPacket, estTime,MessageIds.Class1);
			
			return;
		}
		
		boolean regenflag=false;
		dimension = queryPacket.getDimension();
		for(int i = 0; i < dimension; i++)
		{
			if(sqp.getDimension() != queryPacket.getDimension() ||
				(!sqp.getDistribution().substring(0,5).equals(queryPacket.getDistribution().substring(0,5))) ||
				(!sqp.getOptLevel().equals(queryPacket.getOptLevel())) ||
				(!sqp.getPlanDiffLevel().equals(queryPacket.getPlanDiffLevel())) ||
				(!sqp.getTrimmedQueryTemplate().equals(queryPacket.getTrimmedQueryTemplate())) ||
				(sqp.getResolution(i) != queryPacket.getResolution(i)) ||
				(sqp.getStartPoint(i) != queryPacket.getStartPoint(i)) ||
				(sqp.getEndPoint(i) != queryPacket.getEndPoint(i)))
			{
				regenflag=true;
				break;
			}
		
			if(!regenflag && sqp.getResolution(i) == queryPacket.getResolution(i) && !sqp.getDistribution().equals(queryPacket.getDistribution()))
			{
				regenflag=true;
				break;
			}
		}
		
		if(PicassoConstants.LIMITED_DIAGRAMS==true)
		{
			if(regenflag && clientPacket.getQueryPacket().getMaxResolution() > 10)
			{
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only diagrams with plot resolution 10 are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
				return;
			}
			if (regenflag && clientPacket.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
			{
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only Compilation Diagrams are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
				return;
			}	
		}
		if(regenflag)
		 {
            String changed_settings = new String();
            if(sqp.getDimension() != queryPacket.getDimension())
                changed_settings = changed_settings + "No. of dimensions changed from "+sqp.getDimension()+" to "+queryPacket.getDimension()+"\n";
			if(!sqp.getDistribution().equals(queryPacket.getDistribution()))
                            changed_settings = changed_settings + "Distribution changed from "+sqp.getDistribution()+" to "+queryPacket.getDistribution()+"\n";
			if(!sqp.getOptLevel().equals(queryPacket.getOptLevel()))
                            changed_settings = changed_settings + "Optimization level changed from "+sqp.getOptLevel()+" to "+queryPacket.getOptLevel()+"\n";
			if(!sqp.getPlanDiffLevel().equals(queryPacket.getPlanDiffLevel()))
                            changed_settings = changed_settings + "DiffLevel changed from "+sqp.getPlanDiffLevel()+" to "+queryPacket.getPlanDiffLevel()+"\n";
			if(!sqp.getTrimmedQueryTemplate().equals(queryPacket.getTrimmedQueryTemplate()))
                            changed_settings = changed_settings + "QueryTemplate changed \n";
			
			
			for(int i = 0; i < sqp.getDimension(); i++)
			{
				if(sqp.getResolution(i) != queryPacket.getResolution(i))
					changed_settings = changed_settings + "Resolution on dimension " + i + " changed from "+sqp.getResolution(i)+" to "+queryPacket.getResolution(i)+"\n";
				if(sqp.getStartPoint(i) != queryPacket.getStartPoint(i))
					changed_settings = changed_settings + "StartPoint on dimension " + i+ " changed from " + sqp.getStartPoint(i)*100+" to "+queryPacket.getStartPoint(i)*100+"\n";
				if(sqp.getEndPoint(i) != queryPacket.getEndPoint(i))
					changed_settings = changed_settings + "EndPoint on dimension " + i + " changed from " + sqp.getEndPoint(i)*100+" to "+queryPacket.getEndPoint(i)*100+"\n";
				
			}
           	   		
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating time to generate");
			estTime = estimateGenerationTime(query,false);
			ServerMessageUtil.SPrintToConsole("Estimated time to generate is "+estTime);
			if(database instanceof MSSQLDatabase || database instanceof SybaseDatabase)
				ServerMessageUtil.sendEstimatedTimeRegenerate(sock, reader, writer, queryPacket, estTime,changed_settings,MessageIds.Class2);
			else
				ServerMessageUtil.sendEstimatedTimeRegenerate(sock, reader, writer, queryPacket, estTime,changed_settings,MessageIds.Class1);
			return;
		}
		if((sqp.getExecType().equals(PicassoConstants.COMPILETIME_DIAGRAM) ||
			sqp.getExecType().equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM)) &&
				queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
		{

			if(PicassoConstants.LIMITED_DIAGRAMS == true)
			{
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"To minimize the load on this server, only Compilation Diagrams are supported.",MessageIds.DELETE_PICASSO_DIAGRAM);
				return;
			}
			ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating time to generate");
			estTime = estimateGenerationTime(query,true);
			ServerMessageUtil.SPrintToConsole("Estimated time to Generate is "+estTime);
			if(database instanceof MSSQLDatabase || database instanceof SybaseDatabase)
				ServerMessageUtil.sendEstimatedTime(sock, reader, writer, queryPacket, estTime,MessageIds.Class2);
			else
				ServerMessageUtil.sendEstimatedTime(sock, reader, writer, queryPacket, estTime,MessageIds.Class1);			
			return;
		}
		ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Reading Picasso Diagram");
		int storedResolution[] = sqp.getResolution();
		DiagramPacket gdo = doReadPicassoDiagram(query, storedResolution);
		if(gdo == null)
		{
			ServerMessageUtil.SPrintToConsole("Reading Picasso Diagram failed");
			return;
		}		
		trees = new Vector();
		readAllTrees(gdo.getMaxPlanNumber());
		ServerMessageUtil.sendPlanDiagram(sock, reader, writer, queryPacket, gdo, trees);
	}
	double[][] readAllPicassoDiagrams(String[] plans) throws PicassoException
	{
		String origQueryName = queryPacket.getQueryName();
		String origQueryTemplate = queryPacket.getQueryTemplate();

		Query testquery = Query.getQuery(queryPacket, database);

		dimension = queryPacket.getDimension();
		int m = 1;
		for(int i = 0; i < dimension; i++)
			m = m*queryPacket.getResolution(i);
		
		double [][] costs = new double [plans.length][m];
		for(int i=0;i<plans.length;i++)
		{
			queryPacket.setQueryName(origQueryName + "_P" + i);

			String absPlan = null;
			if(database instanceof MSSQLDatabase) {
				absPlan = "--Picasso_Abstract_Plan\noption (use plan N'" + plans[i] + "')\n";
			} else if(database instanceof SybaseDatabase) {
				absPlan = "--Picasso_Abstract_Plan\nplan '" + plans[i] + "'\n";
			}

			queryPacket.setQueryTemplate(origQueryTemplate + absPlan);

			int qtid = database.getQTID(queryPacket.getQueryName());
			Query query = Query.getQuery(queryPacket, database);

			if(qtid<0){
				System.out.println("Generating Plan " + i);
				genPicassoDiagram(query);
			}
			System.out.println("Reading Plan " + i);
			DiagramPacket gdp = doReadPicassoDiagram(query, queryPacket.getResolution());
			DataValues [] data = gdp.getData();
			for(int j = 0;j < data.length;j ++) {
				costs[i][j] = data[j].getCost();
			}
		}
		return costs;
	}	

	/*
	 * We send back a 2D slice of the ND data we have. selAttrib has the selectivity in percentage along with
	 */
	protected DiagramPacket doReadPicassoDiagram(Query query, int storedResolution[]) throws PicassoException
	{
		int qtid;
		int length;
		qtid = database.getQTID(queryPacket.getQueryName());
		resolution = queryPacket.getResolution();
		dimension = queryPacket.getDimension();
//		plans = new Vector();
		for(int i = 0; i < dimension; i++)
			if(storedResolution[i] < resolution[i])
			{
				ServerMessageUtil.SPrintToConsole("Regenerate Picasso Diagram with enough resolution");
				ServerMessageUtil.sendWarningMessage(sock,reader,writer,"Regenerate Picasso Diagram with enough resolution");
				return null;
			}
		length = 1;
		int ressum=0;
		for(int i = 0; i < dimension; i++)
		{
			length *= resolution[i];
			ressum += resolution[i];
		}
		
		relNames = new String[dimension];
		attribNames = new String[dimension];
		attribTypes = new String[dimension];

		for(int i=0;i<dimension;i++)
		{
			relNames[i] = query.getAliasName(i);
			attribNames[i] = query.getAttribName(i);
			attribTypes[i] = query.getHistogram(i).getAttribType();
		}

		picsel = new float[ressum];
		plansel = new float[ressum];
		predsel = new float[ressum];
		datasel = new float[ressum];
		constants = new String[ressum];

		try
		{
			Statement stmt = database.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT SID,PICSEL,PLANSEL,PREDSEL,DATASEL,CONST from "+database.getSchema()+
					".PicassoSelectivityLog where QTID="+qtid+" order by DIMENSION,SID");


			double errorThreshold = Double.parseDouble(clientPacket.getSelecErrorThreshold());
			int i=0;
			while(rs.next())
			{
				picsel[i]=(float)rs.getDouble(2);
				plansel[i]=(float)rs.getDouble(3);
				predsel[i]=(float)rs.getDouble(4);
				datasel[i]=(float)rs.getDouble(5);
				constants[i]=rs.getString(6);


				if(predsel[i]!=0.0 && (Math.abs(picsel[i]-predsel[i])/max(predsel[i],10))*100 > errorThreshold)
				{
					System.out.println("Diff at "+i+" is "+((Math.abs(picsel[i]-predsel[i])/predsel[i])*100)+
							"\tpicsel is "+picsel[i]+" optsel is "+predsel[i]);
				}
				i++;
			}
			rs.close();
			stmt.close();

			data = new DataValues[length];
			String qText;
			if(queryPacket.getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
				qText= "select QID,PLANNO,RUNCARD,RUNCOST from "+database.getSchema()+".PicassoPlanStore "+
				"where PicassoPlanStore.QTID="+qtid+" order by PicassoPlanStore.QID";
			else
				qText= "select QID,PLANNO,CARD,COST from "+database.getSchema()+".PicassoPlanStore "+
				"where PicassoPlanStore.QTID="+qtid+" order by PicassoPlanStore.QID";

			stmt = database.createStatement();
			rs = stmt.executeQuery(qText);
			DataValues max = new DataValues();
			DataValues min = new DataValues();
			min.setCost(1e300);
			min.setCard(1e300);

			i=0;
			float cost,card;
			int planno;
			while(rs.next())
			{
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


				data[i] = new DataValues();
				data[i].setPlanNumber(planno);
				data[i].setCard(card);
				data[i].setCost(cost);

				i++;
			}

			if(i!=length)
			{
				ServerMessageUtil.SPrintToConsole("Number of generated points="+i+" and length="+length);
				throw new PicassoException("Picasso Diagram was not generated properly.");
			}
			rs.close();
			stmt.close();
			
			DiagramPacket dp = DiagramPacket.createDiagramPacket(dimension,resolution,relNames,attribNames,attribTypes,data,
					max,min,picsel,plansel,predsel,datasel,constants,queryPacket,false);
			dp.approxDiagram = false;
			if (queryPacket.getExecType().equals(PicassoConstants.APPROX_COMPILETIME_DIAGRAM))
			{
				try 
				{
					stmt = database.createStatement();
					rs = stmt.executeQuery("SELECT SAMPLESIZE, SAMPLINGMODE, AREAERROR, IDENTITYERROR, FPCMODE from "+database.getSchema()+
							".PicassoApproxMap where QTID="+qtid);
					
					rs.next();
					boolean FPC = false;
					if(database instanceof MSSQLDatabase || database instanceof SybaseDatabase)
						FPC = rs.getBoolean(5);
					dp.setApproxSampleSize(rs.getInt(1), rs.getInt(2),FPC);				
					dp.setApproxError(rs.getDouble(3), rs.getDouble(4));
					dp.approxDiagram = true;
				} 
				catch (Exception e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return dp;
			/*return DiagramPacket.createDiagramPacket(dimension,resolution,relNames,attribNames,data,
					max,min,picsel,plansel,predsel,datasel,constants,queryPacket);*/
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error reading Plan Diagram "+e);
			throw new PicassoException("Error reading Plan Diagram "+e);
		}
	}

	protected void storePicassoDiagram(int qtid) throws PicassoException
	{
        int SID=0,tmp;
		try{
			PreparedStatement stmt = database.prepareStatement("insert into PicassoPlanStore values("+qtid+",?,?,?,?,0.0,0.0)");
			int i=0;
			while(i<data.length)
			{
				for(int j=0;j<Math.min(data.length,PicassoConstants.SAVE_BATCH_SIZE);j++) 
				{
					stmt.setInt(1,i);
					stmt.setInt(2,data[i].getPlanNumber());
					stmt.setDouble(3,data[i].getCost());
					stmt.setDouble(4,data[i].getCard());
					stmt.addBatch();
					i++;
					if(i>=data.length) break;
				}
				stmt.executeBatch();
				stmt.clearBatch();
			}
			ServerMessageUtil.sendStatusMessage(sock, reader, writer, 50,"Saving Picasso Diagram: "+50+"% completed");
            		clientPacket.setProgress(50);
			
            // Writes to the PicassoSelectivityMap are now turned off. If someone wants to write to this table, they can uncomment the following block
            // of commented code.
            		
			/*PreparedStatement stmt2 = database.prepareStatement("insert into "+database.getSchema()+".PicassoSelectivityMap values("+qtid+",?,?,?)");
			i=0;
			while(i<data.length)
			{
				for(int k=0;k<Math.min(data.length,PicassoConstants.SAVE_BATCH_SIZE);k++) 
				{

					tmp=i;
					for(int j=dimension-1;j>=0;j--)
					{
						SID=tmp%resolution[j];
						tmp = tmp/resolution[j];
						stmt2.setInt(1,i);
						stmt2.setInt(2,j);
						stmt2.setInt(3,SID);
						stmt2.addBatch();
					}
					i++;
					if(i>=data.length) break;
				}
				stmt2.executeBatch();
				stmt2.clearBatch();
			}*/
			ServerMessageUtil.sendStatusMessage(sock, reader, writer, 100,"Saving Picasso Diagram: "+100+"% completed");
			clientPacket.setProgress(100);
		}	
		catch(SQLException e)
		{   
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot store Picasso Diagram data: "+e);
			throw new PicassoException("Cannot store Picasso Diagram data: "+e);
		}
}

	protected void storePlans(int qtid) throws PicassoException
	{
		try
		{
			Statement stmt = database.createStatement();
			ListIterator it = plans.listIterator();
			while(it.hasNext())
				((Plan)it.next()).storePlan(stmt,qtid,database.getSchema());
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Saving plans failed "+e);
			throw new PicassoException("Saving plans failed "+e);
		}
	}

	protected void storeXMLPlans(int qtid) throws PicassoException
	{
		try
		{
			Statement stmt = database.createStatement();
			ListIterator it = XMLplans.listIterator();
			int i=0;
			while(it.hasNext())
			{
				stmt.executeUpdate("insert into "+database.getSchema()+".PicassoXMLPlan (QTID, PLANNO, XMLPLAN) values ("+qtid+", "+ i
						+ ", '" + ((String)it.next()) + "')");
				i++;
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Saving XML plans failed "+e);
			throw new PicassoException("Saving XML plans failed "+e);
		}
	}


	protected void storeSelectivityLog(Query query, int qtid, Histogram hist[]) throws PicassoException
	{
		try
		{
			String str="INSERT INTO "+database.getSchema()+".PicassoSelectivityLog values ("+qtid+",?,?,?,?,?,0.0,?)";
			PreparedStatement stmt= database.prepareStatement(str);
			int coveredres = 0;

			for(int i=0;i<dimension;i++)
			{
				for(int j=0;j<resolution[i];j++)
				{
					String constant = hist[i].getConstant(j);
					if(constant.startsWith("'"))
						constant = constant.substring(1,constant.length()-1);
					stmt.setInt(1,i);
					stmt.setInt(2,j);
					stmt.setDouble(3,(100*hist[i].getPicSel(j)));
					stmt.setDouble(4,(100*plansel[coveredres+j]));	
					stmt.setDouble(5,(100*predsel[coveredres+j]));
					stmt.setString(6,constant);
					stmt.addBatch();
				}
				coveredres += resolution[i];
			}
			stmt.executeBatch();
		}
		catch(SQLException e)
		{   
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Saving selectivity log failed "+e);
			throw new PicassoException("Saving selectivity log failed "+e);
		}
	}
	
	protected int getIndex(int[] index)
	{
		int tmp=0;
		for(int i=index.length-1;i>=0;i--)
			tmp=tmp*resolution[i]+index[i];
		return tmp;
	}

	protected int getIndex(double[] index,int resolution)
	{
		int tmp=0;
		for(int i=index.length-1;i>=0;i--)
			tmp=tmp*resolution+(int)index[i];
		return tmp;
	}
	
	protected int getIndex(double[] index, int[] res)
	{
		int tmp=0;
		for(int i = index.length-1;i >= 0;i--)
			tmp=tmp * res[i] + (int)index[i];
		return tmp;
	}
	
	protected String getTimeString(int time)
	{
		int hour,mins;
		hour = (int)(time/3600);
		time = time % 3600;
		mins = (int)(time/60);
		time = time % 60;
		if(hour!=0)
			return hour+" hr "+mins+" min";
		else
			return mins+" min "+time+" sec";
	}

	private double max(double a, double b){
		if(a>b)
			return a;
		else
			return b;
	}

	public void ifInterruptedStop() throws PicassoException {
		Thread.yield();
		if (Thread.currentThread().isInterrupted()) {
	
//AP - TODO: also see what was to be done in the other place ifInterruptedStop() is called.				
//			try
//			{
//				FileOutputStream fis = new FileOutputStream ("c:\\aptemp");
//				ObjectOutputStream oos = new ObjectOutputStream (fis);
//
////				oos.writeObject(sock);
////				oos.writeObject(reader);
////				oos.writeObject(writer);
//				oos.writeObject(database.getSettings()); //the Connection part is not serializable
//				oos.writeObject(queryPacket);
//				oos.writeObject(clientPacket);
//
//				oos.writeObject(new Integer(dimension));
//				oos.writeObject(new Integer(resolution));
//				oos.writeObject(relNames);
//				oos.writeObject(attribNames);
//				//Vectors are not directly serializable, so.
//				oos.writeObject(new Integer(plans.size()));
//				for(int i=0;i<plans.size();i++)
//							oos.writeObject(plans.get(i));
//				oos.writeObject(data);
//				oos.writeObject(picsel);
//				oos.writeObject(plansel);
//				oos.writeObject(predsel);
//				oos.writeObject(datasel);
//				oos.writeObject(constants);
//
//				
//				oos.writeObject(new Integer(totalSize));
//				oos.writeObject(new Integer(count));
//				oos.writeObject(new Long(timeLeft));
//				oos.writeObject(new Integer(inx));
//
//				oos.flush();
//				oos.close();
//			}
//			catch(IOException e)
//			{
//				System.out.println("Couldn't write");
//				System.err.println("Couldn't write");
//			}
			throw new PicassoException("ThreadInterruption");
		}
	}
	
	//Fill the trees vector by constructing plan trees (TreeNodes) for each plan in the plan diagram. 
	//This will be called only when a diagram is read from the database and not freshly generated, 
	//as during generation, the "plan"s will be directly
	//available and the trees can be created from that.
	public void readAllTrees(int maxPlan) throws PicassoException
	{
		int qtid = database.getQTID(queryPacket.getQueryName());
		//number of trees will be equal to number of plans in the plan diagram w.r.t. next two lines.
		trees.add(new Integer(maxPlan));
		for (int i=0; i < maxPlan; i++) {
			
			//This gets the info from Picasso tables, not re-optimization.
			Plan plan = Plan.getPlan(database,qtid, i,queryPacket.getPlanDiffLevel());
			if(plan!=null)
			{
				trees.add(new Integer(i));
				trees.add(plan.createPlanTree());
			}
			else
				throw new PicassoException("Cannot read plan tree from database");
		}
	}
	protected void ifPausedSleep()
	{
		try {
			while(PicassoConstants.IS_SUSPENDED)
				Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

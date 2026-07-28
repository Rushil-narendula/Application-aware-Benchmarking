
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

import iisc.dsl.picasso.common.ApproxParameters;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.ds.TreeNode;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.mssql.MSSQLDatabase;
import iisc.dsl.picasso.server.db.sybase.SybaseDatabase;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.query.Query;
import iisc.dsl.picasso.server.sampling.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * Processor object is a thread of execution which is spawned to process requests coming from
 * the client that does not concern with managing the server. ( Server messages are handled in
 * Picasso_Server itself). The requests READ_PICASS_DIAGRAM, GENERATE_PICASSO_DIAGRAM and TIME_TO_GENERATE
 * are handled using PicassoDiagram object. Other requests dealing with picasso tables stored in the database
 * are handled using the database object.   
 */
public class Processor extends Thread
{
	protected Socket 			sock;
	protected ObjectInputStream 	reader;
	protected ObjectOutputStream 		writer;
	protected ClientPacket 		clientPacket;
	
	private int msgId;
	private Database database;
	PicassoSampling apd;//ADG
	/*
	 * Sets the required fields and starts the thread
	 */
	public Processor(Socket s, ObjectInputStream in, ObjectOutputStream out, ClientPacket packet) {
		sock = s;
		reader = in;
		writer = out;
		clientPacket = packet;
		start();
	}
		
	public void run() {
		QueryPacket qp = null;
		double sel[];
		ServerMessageUtil.SPrintToConsole("New Server Thread is processing " + msgId + " request currently");
		
		try{
			qp = initialize();
			/*
			 * At this point we expect database to be connected and msgId to be set correctly
			 */
			switch(msgId)
			{
			case MessageIds.DELETE_PICASSO_TABLES:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Dropping Picasso Tables");
				database.checkPicassoTables();
				database.deletePicassoTables();
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Picasso Tables Dropped.",
						MessageIds.DELETE_PICASSO_TABLES);
				break;

			case MessageIds.DELETE_PICASSO_DIAGRAM:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Deleting QTD");
				database.checkPicassoTables();
				database.deletePicassoDiagram(qp.getQueryName());
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"QTD Deleted",
						MessageIds.DELETE_PICASSO_DIAGRAM);
				break;

			case MessageIds.RENAME_PICASSO_DIAGRAM:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Renaming QTD");
				database.checkPicassoTables();
				database.renamePicassoDiagram(qp.getQueryName(), qp.getNewQueryName());
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"QTD Renamed", MessageIds.RENAME_PICASSO_DIAGRAM);
				break;
				
			case MessageIds.CLEANUP_PICASSO_TABLES:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Cleaning up Picasso Tables");
				database.checkPicassoTables();
				database.cleanUpTables();
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Server Cleaned up",
						MessageIds.DELETE_PICASSO_DIAGRAM);
				break;

			case MessageIds.GET_QUERYTEMPLATE_NAMES:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Fetching QueryTemplate List");
				database.checkPicassoTables();
				Vector queryNames = database.getAllQueryPackets();
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"QueryTemplate List fetched");
				ServerMessageUtil.sendQueryNames(sock,reader,writer,queryNames);
				break;

			case MessageIds.GET_PLAN_TREE:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Fetching Representative Plan Tree");
				String planStr = clientPacket.getPlanNumbers();
				sendPlanTree(qp,planStr);
				break;
            case MessageIds.GET_PLAN_TREES:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Changing the Diff Level");
				planStr = clientPacket.getPlanNumbers();
				sendPlanTree(qp,planStr);
				break;

			//added for multiplan
			case MessageIds.GET_MULTI_PLAN_TREES:
				ServerMessageUtil.sendStatusMessage(sock, reader, writer, 0,
						"Generating Foreign Plan Tree");

				sendMultiPlanTrees(qp, clientPacket);
				break;
			//multiplan addition ends here

			case MessageIds.GET_ABSTRACT_PLAN:
			case MessageIds.GET_COMPILED_PLAN_TREE:
				if (msgId == MessageIds.GET_COMPILED_PLAN_TREE)
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Compiled Plan Tree");
				else if (msgId == MessageIds.GET_ABSTRACT_PLAN && PicassoConstants.ENABLE_COST_MODEL)
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generating Abstract Plan");
				planStr = clientPacket.getPlanNumbers();
				/*
				 * getCompileTreeValues returns the selectivity values for the present slice of PlanDiagram
				 * getAttributeSelectivities returns the selectivity of other dimensions which determines the slice
				 * These are seperate for historical reasons and there is no technical reason why these should be
				 * different for our purpose
				 */
				Hashtable selAttrib1 = clientPacket.getCompileTreeValues();
				Hashtable selAttrib2 = clientPacket.getAttributeSelectivities();
				// Remember that getQuery actually sets the Dimension value in qp obtained from QTIDMap
				Query query = Query.getQuery(qp,database);
				int dimension = qp.getDimension();
				sel = new double[dimension];
				String tmp;
				/*
				 * We construct a single double array with selectivity values for each dimension which identifies
				 * the query point in the selectivity space. We fire the query to get the plan and send it across 
				 */
				for(int j=0;j<dimension;j++){
					if(selAttrib2!=null){
						tmp = (String)selAttrib2.get(new Integer(j));
						if(tmp!=null)
							sel[j] = Double.parseDouble(tmp)/100;
					}
					
					tmp = (String)selAttrib1.get(new Integer(j));
					if(tmp!=null)
						sel[j] = Double.parseDouble(tmp)/100;
					
					// This is a hack for readPicassoDiagram() not to break :-(
					// This actually makes readPicassoDiagram() simpler
					if(sel[j]==1.0)
						sel[j]=0.999999;
				}
				for (int i=0; i < sel.length; i++) {
					System.out.println("Selec :: " + sel[i]);
				}
				if (msgId == MessageIds.GET_COMPILED_PLAN_TREE)
					sendCompiledPlanTree(qp,query,sel,Integer.parseInt(planStr));
				else if (msgId == MessageIds.GET_ABSTRACT_PLAN && PicassoConstants.ENABLE_COST_MODEL) 
					sendQueryPlusPlan(qp,query,sel,Integer.parseInt(planStr));
				break;

			case MessageIds.TIME_TO_GENERATE:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating Time to Generate. Please Wait");
				database.checkPlanTable();
				database.checkPicassoTables();
				query = Query.getQuery(qp,database);
				PicassoDiagram pd = new PicassoDiagram(sock,reader,writer,clientPacket,database);
				int estTime = pd.estimateGenerationTime(query,false);
				ServerMessageUtil.SPrintToConsole("Estimated time to Generate is "+estTime);
				if ( database instanceof MSSQLDatabase || database instanceof SybaseDatabase)
					ServerMessageUtil.sendEstimatedTime(sock, reader, writer, qp, estTime,MessageIds.Class2);
				else
					ServerMessageUtil.sendEstimatedTime(sock, reader, writer, qp, estTime,MessageIds.Class1);
				break;
			case MessageIds.TIME_TO_GENERATE_APPROX:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Estimating Time to Generate. Please Wait");
				database.checkPlanTable();
				database.checkPicassoTables();
				query = Query.getQuery(qp,database);
				ApproxParameters ApproxParams = clientPacket.getApproxParameters();
				int SamplingMode = ApproxParams.SamplingMode;
				switch(SamplingMode) 
				{
					case MessageIds.SRSWOR:
						apd = new SRS(sock,reader,writer,clientPacket,database);					
						break;
					case MessageIds.GSPQO:
						apd = new GSPQO(sock,reader,writer,clientPacket,database);					
						break;
				}
				estTime = apd.estimateTime();
				int exactEstTime = clientPacket.getQueryPacket().getEstimatedTime();
				if(exactEstTime > 0 && estTime > exactEstTime)
					estTime = exactEstTime;
				ServerMessageUtil.SPrintToConsole("Estimated time to Generate the approximate diagram is "+estTime);
				ServerMessageUtil.sendEstimatedTimeApprox(sock, reader, writer, qp, estTime);
				break;
			case MessageIds.READ_PICASSO_DIAGRAM:
				database.checkPlanTable();
				database.checkPicassoTables();
				pd = new PicassoDiagram(sock,reader,writer,clientPacket,database);
				pd.readPicassoDiagram();
				break;

			case MessageIds.GENERATE_PICASSO_DIAGRAM:
				database.checkPlanTable();
				database.checkPicassoTables();
				pd = new PicassoDiagram(sock,reader,writer,clientPacket,database);
				pd.generatePicassoDiagram();
				break;
			case MessageIds.GENERATE_APPROX_PICASSO_DIAGRAM:
				database.checkPlanTable();
				database.checkPicassoTables();
				ApproxParams = clientPacket.getApproxParameters();
				SamplingMode = ApproxParams.SamplingMode;
				switch(SamplingMode) 
				{
					case MessageIds.SRSWOR:
						apd = new SRS(sock,reader,writer,clientPacket,database);					
						break;
					case MessageIds.GSPQO:
						apd = new GSPQO(sock,reader,writer,clientPacket,database);					
						break;
				}
				apd.generatePicassoDiagram();

				break;	
			case MessageIds.GET_PLAN_STRINGS:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Getting Plan Strings");
				ArrayList points = (ArrayList) clientPacket.get("PlanPoints");
				sendPlanStrings(qp,points);
				break;
				
			case MessageIds.GET_PLAN_COSTS:
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Getting Plan Costs");
				points = (ArrayList) clientPacket.get("PlanPoints");
				String absplan = (String) clientPacket.get("PlanString");
				sendPlanCosts(qp,absplan,points);
				break;
				
			case MessageIds.GET_ALL_PLAN_COSTS:
				String[]plans = (String[]) clientPacket.get("PlanStrings");
				double[][] costs = new PicassoDiagram(sock,reader,writer,clientPacket,database).readAllPicassoDiagrams(plans);
				ServerMessageUtil.sendAllPlanCosts(sock, reader, writer, costs);
				break;
			case MessageIds.PAUSE_PROCESSING:
				// PicassoDiagram.paused = true;
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Processing Paused");
				break;
			case MessageIds.RESUME_PROCESSING:
				// PicassoDiagram.paused = false;
				ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Processing Resumed");
				break;
			}
			database.close();
			ServerMessageUtil.removeFromQueue(clientPacket);
			ServerMessageUtil.SPrintToConsole("Server is done processing " + msgId + " request");
		}catch(Exception e){
			e.printStackTrace();
			if(e.getMessage()!=null){
//				 Hack for not reporting error if user cancels generation...
				if(e.getMessage().equals("ThreadInterruption")){
					ServerMessageUtil.sendStatusMessage(sock,reader,writer,0,"Generation Cancelled");
					ServerMessageUtil.sendErrorMessage(sock,reader,writer,null);
				}
				else{
					ServerMessageUtil.SPrintToConsole("Sending Error message:"+e.getMessage());
					ServerMessageUtil.sendErrorMessage(sock,reader,writer,e.getMessage());
				}
			}
			else
				ServerMessageUtil.sendErrorMessage(sock,reader,writer,"General Error");
			ServerMessageUtil.removeFromQueue(clientPacket);
			ServerMessageUtil.SPrintToConsole("Server had an error processing request" + msgId);
		}
	}
	public void stopCompiling()//ADG
	{		
		apd.stopCompile = true;
	}
	/*
	 * Sets the clientId, msgId and database instance variables and return the QueryPacket object
	 */
	private QueryPacket initialize() throws PicassoException
	{
		QueryPacket qp = (QueryPacket)clientPacket.getQueryPacket();
		
		String msgIdStr = clientPacket.getMessageId();
		msgId = new Integer(msgIdStr).intValue();
		DBSettings settings = clientPacket.getDBSettings();

		/* Check database connectivity and Query validity */
		database = Database.getDatabase(settings);
		if(database == null)
			throw new PicassoException("Cannot determine the Database type");
		if(database.connect() == false){
			ServerMessageUtil.SPrintToConsole("Failed to connect to database engine");
			throw new PicassoException("Failed to connect to database engine");
		}
		// delete plan diagram if we are regenerating
		return qp;
	}

	private void sendPlanTree(QueryPacket qp, String planStr) throws PicassoException
	{
		StringTokenizer st = new StringTokenizer(planStr, ",");
		int[] planNumbers = new int[st.countTokens()];
		int i=0;
		while ( st.hasMoreTokens() ) {
			String str = st.nextToken();
			planNumbers[i++] = Integer.parseInt(str);
		}
		TreeNode tree[] = new TreeNode[planNumbers.length];
		int qtid = database.getQTID(qp.getQueryName());
		Plan plan;
		for (i=0; i < tree.length; i++) {
			plan = Plan.getPlan(database,qtid, planNumbers[i],qp.getPlanDiffLevel());
			if(plan!=null)
				tree[i] = plan.createPlanTree();
			else
				throw new PicassoException("Cannot read plan tree from database");
		}
		
		
//		try
//		{
//		FileOutputStream fis = new FileOutputStream ("c:\\bptemp");
//		ObjectOutputStream ois = new ObjectOutputStream (fis);
//			ois.writeObject(tree);
//		
//		ois.flush();
//		ois.close();
//		}
//		catch(Exception ex)
//		{
//			System.out.println(ex.getMessage());
//			ex.printStackTrace();
//		}
		ServerMessageUtil.sendPlanTree(sock, reader, writer, planNumbers, tree,
				msgId);
	}
	
	//multiplan start
	private void sendMultiPlanTrees(QueryPacket qp, ClientPacket clipac)
			throws PicassoException {
		try {
			
			Hashtable selAttrib1 = clientPacket.getCompileTreeValues();
			Hashtable selAttrib2 = clientPacket.getAttributeSelectivities();
			// Remember that getQuery actually sets the Dimension value in
			// qp obtained from QTIDMap
			QueryPacket apqueryPacket = clientPacket.getQueryPacket();
			Query apquery = Query.getQuery(apqueryPacket, database);
			//Query query = Query.getQuery(qp, database);
			int dimension = apqueryPacket.getDimension();
			double sel[] = new double[dimension];
			String tmp;
			/*
			 * We construct a single double array with selectivity values
			 * for each dimension which identifies the query point in the
			 * selectivity space. We fire the query to get the plan and send
			 * it across
			 */
			for (int j = 0; j < dimension; j++) 
			{
				if (selAttrib2 != null) 
				{
					tmp = (String) selAttrib2.get(new Integer(j));
					if (tmp != null)
						sel[j] = Double.parseDouble(tmp) / 100;
				}
				if(selAttrib1 != null)
				{
				tmp = (String) selAttrib1.get(new Integer(j));
				if (tmp != null)
					sel[j] = Double.parseDouble(tmp) / 100;
				}

				// This is a hack for readPicassoDiagram() not to break :-(
				// This actually makes readPicassoDiagram() simpler
				if (sel[j] == 1.0)
					sel[j] = 0.999999;
			}
			for (int i = 0; i < sel.length; i++) {
				System.out.println("Selec :: " + sel[i]);
			}
                        DBSettings otherengdbset = (DBSettings)clipac.get("otherdbsettings");
                        Database tempdatabase1;
                        try {
                        
                            tempdatabase1 = Database.getDatabase(otherengdbset);
                        } catch (PicassoException ex) {
                            ex.printStackTrace();
                            throw new PicassoException("Database Engine "+otherengdbset.getInstanceName()+" is not accepting connections");
                        }
		   
			
			System.out.println(" qText for database "+otherengdbset.getDbName()+", optlevel is "+otherengdbset.getOptLevel());
                        apquery = Query.getQuery(apqueryPacket, tempdatabase1);
                        apquery.setQueryTemplate((String)clipac.get("otherdbquerytext"));
			String qText = apquery.generateQuery(sel);
			//System.out.println("Query For Compile : " + qText);
			java.util.ArrayList sel_val_attr_arrlist = new java.util.ArrayList();
			String[] selvalarr0 = apquery.getSelectivityValue(sel);
			String[] selattrarr0 = new String[sel.length];
			String[] cost0 = new String[1];
			String[] card0 = new String[1];
			
			for(int i=0;i<sel.length;i++)
			{
				selattrarr0[i] = apquery.getAttribName(i);
			}
			try{
				for(int i=0;i<sel.length;i++)
				{
					int index = ((String)selvalarr0[i]).indexOf(".");
					if(index!=-1)
					{
							if(index+3<((String)selvalarr0[i]).length())
							{
								selvalarr0[i]=((String)selvalarr0[i]).substring(0,index+3);
							}
					}
				}
								
			}catch(Exception e1)
			{
				e1.printStackTrace();
			}
			
			sel_val_attr_arrlist.add(selvalarr0);
			sel_val_attr_arrlist.add(selattrarr0);
			if (tempdatabase1 == null)
				throw new PicassoException("Cannot determine the Database type");
			if (tempdatabase1.connect() == false) {
				ServerMessageUtil
						.SPrintToConsole("Failed to connect to database engine");
				throw new PicassoException("Failed to connect to database engine");
			}
			tempdatabase1.removeFromPlanTable(30000001);
			
			Plan plan1 = tempdatabase1.getPlan(qText, 30000001);
			tempdatabase1.removeFromPlanTable(30000001);
			tempdatabase1.close();
			
			cost0[0] = new Double(plan1.getCost()).toString(); // Added to send cost along with the trees in a multi plan tree -ma
			card0[0] = new Double(plan1.getCard()).toString(); // Added to send card. along with the trees in a multi plan tree -ma
			sel_val_attr_arrlist.add(cost0);
			sel_val_attr_arrlist.add(card0);
			
			DBSettings dbsetorig = (DBSettings)clipac.get("origdbsettings");
                        Database tempdatabase2;
                        try {
                            tempdatabase2 = Database.getDatabase(dbsetorig);
                        } catch (PicassoException ex) {
                            ex.printStackTrace();
                            throw new PicassoException("Database Engine "+dbsetorig.getInstanceName()+" is not accepting connections");
                        }
			if (tempdatabase2 == null)
				throw new PicassoException("Cannot determine the Database type");
			if (tempdatabase2.connect() == false) {
				ServerMessageUtil
						.SPrintToConsole("Failed to connect to database engine");
				throw new PicassoException("Failed to connect to database engine");
			}
			tempdatabase2.removeFromPlanTable(30000001);
			apquery = Query.getQuery(apqueryPacket, tempdatabase2);
			String qText_orig= apquery.generateQuery(sel);
			String[] selvalarr1 = apquery.getSelectivityValue(sel);
			String[] selattrarr1 = new String[sel.length];
			String[] cost1 = new String[1];
			String[] card1 = new String[1];
			for(int i=0;i<sel.length;i++)
			{
				selattrarr1[i] = apquery.getAttribName(i);
			}
			try{
				for(int i=0;i<sel.length;i++)
				{
					int index = ((String)selvalarr1[i]).indexOf(".");
						if(index!=-1)
						{
							if(index+3<((String)selvalarr1[i]).length())
							{
								selvalarr1[i]=((String)selvalarr1[i]).substring(0,index+3);
							}
						}
				}
			}catch(Exception e1)
			{
				e1.printStackTrace();
			}
			sel_val_attr_arrlist.add(selvalarr1);
			sel_val_attr_arrlist.add(selattrarr1);
			System.out.println(" original database is "+dbsetorig.getDbName()+", optlevel is "+dbsetorig.getOptLevel());
			System.out.println(" original query is "+qText_orig);
			Plan plan2 = tempdatabase2.getPlan(qText_orig, 30000001);
			tempdatabase2.removeFromPlanTable(30000001);
			tempdatabase2.close();
			
			cost1[0] = new Double(plan2.getCost()).toString(); // Added to send cost along with the trees in a multi plan tree -ma
			card1[0] = new Double(plan2.getCard()).toString(); // Added to send card. along with the trees in a multi plan tree -ma
			sel_val_attr_arrlist.add(cost1);
			sel_val_attr_arrlist.add(card1);
			
			TreeNode tree[] = new TreeNode[2];
			int planNumbers[] = new int[2];
			planNumbers[0] = 0;//new plan of the other engine is being assigned number 0
			planNumbers[1] = Integer.parseInt(clipac.getPlanNumbers());
			if (plan1 != null)
			{
				tree[0] = plan1.createPlanTree();//contains plan of the other db engine
				tree[1] = plan2.createPlanTree();//contains original db engine plan
			}
			else
				throw new PicassoException("Cannot read plan tree from database");
			//ServerMessageUtil.sendPlanTree(sock, reader, writer, planNumbers, tree,
			//		MessageIds.GET_COMPILED_PLAN_TREE);
			
			// AP: the method is generic enough to send multiple trees back
			ServerMessageUtil.sendMultiPlanTree(sock, reader, writer, planNumbers,
					tree, MessageIds.GET_MULTI_PLAN_TREES,sel_val_attr_arrlist);
		} catch (Exception e) {
			e.printStackTrace();
			throw new PicassoException(e.getMessage());
		}
	}
	//multiplan end

	private void sendCompiledPlanTree(QueryPacket qp, Query query, double sel[],int planno) throws PicassoException
	{
		String qText = query.generateQuery(sel);
		//System.out.println("Query For Compile : " + qText);
		database.removeFromPlanTable(30000001);
		Plan plan = database.getPlan(qText,30000001);
		database.removeFromPlanTable(30000001);
		TreeNode tree[] = new TreeNode[1];
		int planNumbers[] = new int[1];
		planNumbers[0] = planno;
		if(plan!=null)
			tree[0] = plan.createPlanTree();
		else
			throw new PicassoException("Cannot read plan tree from database");
		
//		try
//		{
//		FileOutputStream fis = new FileOutputStream ("c:\\bptemp");
//		ObjectOutputStream ois = new ObjectOutputStream (fis);
//			ois.writeObject(tree);
//		
//		ois.flush();
//		ois.close();
//		}
//		catch(Exception ex)
//		{
//			System.out.println(ex.getMessage());
//			ex.printStackTrace();
//		}

		ServerMessageUtil.sendPlanTree(sock, reader, writer, planNumbers, tree,
				MessageIds.GET_COMPILED_PLAN_TREE);
	}
	private void sendQueryPlusPlan (QueryPacket qp, Query query,double sel[],int planno ) throws PicassoException 
	{
		String xplan = null;
		String qText = query.generateQuery(sel);
		if (database instanceof MSSQLDatabase){
			MSSQLDatabase mdb = (MSSQLDatabase)database;
			xplan = mdb.getAbsPlan(qText);
		}
		else if (database instanceof SybaseDatabase){
			SybaseDatabase sdb = (SybaseDatabase)database;
			xplan = sdb.getAbsPlan(qText);
		}
		int planNumbers[] = new int[1];
		planNumbers[0] = planno;
		ServerMessageUtil.sendQueryPlusPlan(sock, reader, writer, planNumbers, xplan, MessageIds.GET_ABSTRACT_PLAN);
	}
	
	
	private void sendPlanStrings(QueryPacket qp, ArrayList points) throws PicassoException {
		ArrayList plans = new ArrayList();
		Query query = Query.getQuery(qp,database);
		query.genConstants(qp.getResolution(), qp.getDistribution(), qp.getStartPoint(), qp.getEndPoint());
		int d = qp.getDimension();
		int r[] = qp.getResolution();
		int [] sel = new int[d];
		for(Iterator it = points.iterator();it.hasNext();) {
			Integer ii = (Integer) it.next();
			int pt= ii.intValue();
			for(int i = 0;i < d;i ++) {
				sel[i] = pt % r[i];
				pt /= r[i];
			}
			String xplan = null;
			String qText = query.generateQuery(sel);
			if (database instanceof MSSQLDatabase){
				MSSQLDatabase mdb = (MSSQLDatabase)database;
				xplan = mdb.getAbsPlan(qText);
			}
			else if (database instanceof SybaseDatabase){
				SybaseDatabase sdb = (SybaseDatabase)database;
				xplan = sdb.getAbsPlan(qText);
			}
			plans.add(xplan);
		}
		ServerMessageUtil.sendPlanStrings(sock, reader, writer, plans, MessageIds.GET_PLAN_STRINGS);
	}

	private void sendPlanCosts(QueryPacket qp, String planStr, ArrayList points) throws PicassoException {
		ArrayList costs = new ArrayList();
		Query query = Query.getQuery(qp,database);
		query.genConstants(qp.getResolution(), qp.getDistribution(), qp.getStartPoint(), qp.getEndPoint());
		int d = qp.getDimension();
		int r[] = qp.getResolution();
		int [] sel = new int[d];
		String absPlan = "";
		if(database instanceof MSSQLDatabase) {
			absPlan = "--Picasso_Abstract_Plan\noption (use plan N'" + planStr + "')\n";
		} else if(database instanceof SybaseDatabase) {
			absPlan = "--Picasso_Abstract_Plan\nplan '" + planStr + "'\n";
		}

		for(Iterator it = points.iterator();it.hasNext();) {
			Integer ii = (Integer) it.next();
			int pt= ii.intValue();
			for(int i = 0;i < d;i ++) {
				sel[i] = pt % r[i];
				pt /= r[i];
			}
			String newQuery = query.generateQuery(sel);
			newQuery += absPlan;
			Plan plan = database.getPlan(newQuery,query);
			double cost = plan.getCost();
			costs.add(new Double(cost));
		}
		ServerMessageUtil.sendPlanCosts(sock, reader, writer, costs, MessageIds.GET_PLAN_COSTS);
	}

}

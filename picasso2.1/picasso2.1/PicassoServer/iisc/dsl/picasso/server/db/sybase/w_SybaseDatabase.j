
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

package iisc.dsl.picasso.server.db.sybase;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


// import com.sybase.jdbc3.jdbc.SybConnection;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.db.SybaseInfo;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

public class SybaseDatabase extends Database {

//	private SybaseMessageHandler smh;

	public SybaseDatabase(DBSettings settings) throws PicassoException
	{
		super(settings);
//		smh = new SybaseMessageHandler();
	}
	protected void createPicassoHistogram(Statement stmt) throws SQLException {
    }


	public boolean connect(DBSettings settings) throws PicassoException{
		try
		{
//			Class.forName("com.sybase.jdbc3.jdbc.SybDriver").newInstance();
			con=DriverManager.getConnection("jdbc:sybase:Tds:" + settings.getServerName() + ":" + settings.getServerPort()+"/"+settings.getDbName()+"?user="+settings.getUserName()+"&password="+settings.getPassword());
			if(con!=null){
//				SybConnection scon = (SybConnection) con;
//				scon.setSybMessageHandler(smh);
                        	return true;
			}
			return false;
		}
		catch (SQLException se) {
			System.out.println(se.getSQLState() + " Exception : " + se.getErrorCode());
			System.err.println(se.getSQLState() + " Exception : " + se.getErrorCode());
			SQLException sel = se;
			sel = sel.getNextException();
			if ( sel != null )
				sel.printStackTrace();
			se.printStackTrace();
            throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
			//return false;
		}
		catch(Exception e)
		{
			System.out.println("Exception : "+e);
			e.printStackTrace();
			return false;
		}
	}

	public Histogram getHistogram(String tabName, String schema, String attribName) throws PicassoException
	{
		return new SybaseHistogram(this, tabName, schema, attribName);
	}

	public void emptyPlanTable(){ }
	public void removeFromPlanTable(int qno){ }

	public boolean checkPlanTable()
	{
		return true;
	}

	protected void createPicassoColumns(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create view picasso_columns(COLUMN_NAME,TABLE_NAME,OWNER ) as select " +
				"sc.name ,so.name ,su.name from sysobjects so,syscolumns sc,sysusers su where sc.id=so.id " +
		"and so.uid=su.uid");
	}
	protected void createQTIDMap(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table PicassoQTIDMap ( QTID int NOT NULL, QTEMPLATE text, " +
			"QTNAME varchar(" + PicassoConstants.QTNAME_LENGTH + ") UNIQUE NOT NULL, RESOLUTION int, DIMENSION int,  EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
			"OPTLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIME decimal(19,0), GENDURATION int, PRIMARY KEY (QTID))");
	}
	protected void createPlanTree(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTree ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, PARENTID int NOT NULL, "+
				"NAME varchar(20), COST float, CARD float, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
		"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) )");
	}
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		try {
			stmt.executeQuery("dump tran tpch with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID int NOT NULL, DIMNUM int NOT NULL, RESOLUTION int NOT NULL, STARTPOINT float NOT NULL, ENDPOINT float NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID))");
	}
	// There is no sub operator level stuff so far
	protected void createPlanTreeArgs(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, "+
				"ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME,ARGVALUE), " +
		"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) )");
	}
	
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	
	protected void createPlanStore(Statement stmt) throws SQLException
	{
		System.out.println("Settings :: " + settings.getSchema());
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID int NOT NULL, QID int NOT NULL, PLANNO int, COST float, CARD float, " +
		"RUNCOST float, RUNCARD float, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) )");
	}
	protected void createSelectivityMap(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID int NOT NULL, QID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
		"PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) )");
	}
	protected void createSelectivityLog(Statement stmt) throws SQLException
	{
		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
				"PICSEL float, PLANSEL float, PREDSEL float, DATASEL float, CONST varchar("+PicassoConstants.LARGE_COLUMN+"), " +
		"PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) )");
	}
	protected void createApproxSpecs(Statement stmt) throws SQLException
	{

		try {
			stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
		}
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID int NOT NULL, " +
				"SAMPLESIZE float, SAMPLINGMODE int, AREAERROR float, IDENTITYERROR float, FPCMODE int, " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) )");
	}
	
    // A Sybase specific version of the delete Picasso tables function
	public void deletePicassoTables() throws PicassoException
	{
		try{
			Statement stmt = createStatement();
			stmt.executeUpdate("drop table "+getSchema()+".PicassoSelectivityLog");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoSelectivityMap");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanTreeArgs");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanTree");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoPlanStore");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoRangeResMap"); //-ma
			stmt.executeUpdate("drop table "+getSchema()+".PicassoApproxMap");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoQTIDMap");
			stmt.executeUpdate("drop view "+getSchema()+".picasso_columns");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Dropping Picasso Tables"+e);
			throw new PicassoException("Error Dropping Picasso Tables"+e);
		}
	}
	 public void deletePicassoDiagram(String queryName) throws PicassoException
	{
		try{
			int qtid = getQTID(queryName);
			if(qtid>=0){
				Statement stmt = createStatement();
				stmt.executeUpdate("delete from "+getSchema()+".PicassoSelectivityLog where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoSelectivityMap where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoPlanTreeArgs where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoPlanTree where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoPlanStore where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoRangeResMap where QTID="+qtid); //-ma
				stmt.executeUpdate("delete from "+getSchema()+".PicassoApproxMap where QTID="+qtid); //-ma
				stmt.executeUpdate("delete from "+getSchema()+".PicassoQTIDMap where QTNAME='"+queryName+"'");
				stmt.close();
			}
		}
		catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Deleting Picasso Diagram "+e);
			throw new PicassoException("Error Deleting Picasso Diagram "+e);
		}
	}
	
	
	public String getAbsPlan(String qText)throws PicassoException
	{
		String absPlan = null;
		try {
//			smh.clear();
			Statement stmt = createStatement();
			stmt.executeUpdate("set option show_abstract_plan on");
			stmt.executeUpdate("set noexec on");
			stmt.execute(qText);
			stmt.executeUpdate("set noexec off");
			stmt.executeUpdate("set option show_abstract_plan off");
			stmt.close();
			Vector lines=null;// = smh.getMessageVector();
			absPlan = "";
			for(int i = 1  ; i<lines.size();i++){
				String temp = (String)lines.get(i);
				absPlan = absPlan + temp.substring(0,temp.length()-1);
			}
			absPlan = absPlan.substring(0, absPlan.indexOf(PicassoConstants.SYBASE_ABSTRACT_PLAN_ENDS));
		
		}
		catch (Exception e){
			e.printStackTrace();
			throw new PicassoException("Unable to obtain abstract plan.");
		}
		return absPlan;
	}
	public Plan getPlan(String query) throws PicassoException
	{
		Plan plan = new Plan();
		try{
//			smh.clear();
			Statement stmt = createStatement();
			stmt.execute("dbcc purgesqlcache");
			try {
				stmt.executeQuery("dump tran "+settings.getDbName()+" with no_log");
			} catch (SQLException e) {
				System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
			}
			stmt.execute("dbcc traceon(3604)");

			if( !(settings.getOptLevel().trim().equalsIgnoreCase("Default")))
				stmt.execute("sp_configure \'optimization goal\', 0, \'" + settings.getOptLevel() + "\'");
			else {
				SybaseInfo si = new SybaseInfo();
				stmt.execute("sp_configure \'optimization goal\', 0, \'" + si.defaultOptLevel + "\'");
			}

			stmt.execute("use " + settings.getDbName());
			stmt.execute("set option show_best_plan long");
			stmt.execute("set noexec on");
			stmt.execute(query);
			stmt.execute("set noexec off");
			stmt.execute("dbcc traceoff(3604)");
			stmt.execute("set option show_best_plan off");
			stmt.close();
			Vector lines=null;// = smh.getMessageVector();
			String line;
			do{
				line = (String)lines.remove(0);
			}while(!line.trim().startsWith("FINAL PLAN"));

			String cost = line.substring(line.indexOf("cost = ")+7,line.indexOf(")"));
			lines.remove(0);
			parseNode(plan,0,-1,lines);

			if(plan==null || plan.getNode(0)==null)
				throw new PicassoException("Cannot obtain plan");

			plan.getNode(0).setCost(Float.parseFloat(cost));
		}catch(Exception e){
			e.printStackTrace();
			throw new PicassoException("Cannot obtain plan");
		}
		return plan;
	}

	public Plan getPlan(String query,int startQueryNumber) throws PicassoException
	{
		return getPlan(query);
	}

	private int parseNode(Plan plan, int id, int parent, Vector lines)
	{
		boolean stored=false;
		int nodeId = id;
		Node node = new Node();
		String oper=null,object=null,cost=null,card=null;
//	System.out.println("Starting Node");

		String line = (String)lines.remove(0);
//	System.out.print(line);
		line = line.trim();
		if(!line.startsWith("("))
			return -1;
		int end = line.indexOf(" ",line.indexOf("Pop"));
		if(end==-1)
			oper = line.substring(line.indexOf("Pop")+3,line.length());
		else
			oper = line.substring(line.indexOf("Pop")+3,end);
		//System.out.println("Operation<"+oper+">");
		while(lines.size()>0){
			line = (String)lines.remove(0);
//		System.out.print(line);
			line = line.trim();
			if(line.startsWith("table")){
				object = line.substring(line.indexOf("(")+2,line.indexOf(")")-1);
				//System.out.println("Object<"+object+">");
			}
			else if(line.startsWith("cost")){
				cost = line.substring(line.indexOf(":")+2);
				//System.out.println("Cost<"+cost+">");
			}
			else if(line.startsWith("rowcount")){
				card = line.substring(line.indexOf("=")+1);
				//System.out.println("Card<"+card+">");
			}
			else if(line.startsWith(")")){
				//System.out.println("Finished Node");
				break;
			}
			else if(line.startsWith("Cache Strategy")){
				while(lines.size()>0){
					line = (String)lines.remove(0);
					line = line.trim();
					if(line.startsWith("]"))
						break;
					else{
						node.addArgType(line.substring(0,line.indexOf("=")));
						node.addArgValue(line.substring(line.indexOf("=")+1));
					}
				}
			}
			else if(line.startsWith("order")){
				node.addArgType("order");
				node.addArgValue(line.substring(line.indexOf(":")+2));
			}
			line = (String)lines.get(0);
			line = line.trim();
			if(line.startsWith("(")){
				if(!stored){
					node.setId(nodeId);
					node.setName(oper);
					node.setParentId(parent);
					node.setCost(Double.parseDouble(cost));
					node.setCard(Double.parseDouble(card));
					plan.setNode(node,plan.getSize());
					if(object!=null && !object.equals("")){
						node = new Node();
						node.setId(-1);
						node.setParentId(nodeId);
						node.setName(object);
						node.setCost(0.0);
						node.setCard(0.0);
						plan.setNode(node,plan.getSize());
					}
					stored = true;
				}
				id = parseNode(plan,id+1,nodeId,lines);
			}
		}
		if(!stored){
			node.setId(nodeId);
			node.setName(oper);
			node.setParentId(parent);
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			plan.setNode(node,plan.getSize());
			if(object!=null && !object.equals("")){
				node = new Node();
				node.setId(-1);
				node.setParentId(nodeId);
				node.setName(object);
				node.setCost(0.0);
				node.setCard(0.0);
				plan.setNode(node,plan.getSize());
			}
		}
		return id;
	}
}

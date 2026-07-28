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

package iisc.dsl.picasso.server.db.oracle;

import iisc.dsl.picasso.common.PicassoConstants;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.db.OracleInfo;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

public class OracleDatabase extends Database
{
	private int qno;

	public OracleDatabase(DBSettings settings) throws PicassoException
	{
		super(settings);
		qno = 0;
	}
	
	protected void createPicassoHistogram(Statement stmt) throws SQLException {
    }

	public boolean connect(DBSettings settings) throws PicassoException{
		String connectString;
		if(isConnected())
			return true;
		this.settings = settings;
		try{
			connectString = "jdbc:oracle:thin:@//" + settings.getServerName() + ":" +	settings.getServerPort() +
					"/" + settings.getDbName();
//			DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver ());
			con = DriverManager.getConnection (connectString, settings.getUserName(),
					settings.getPassword());
		}
		catch (Exception e)	{
            System.err.println("Database: " + e);
            System.out.println("Database: " + e);
			throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
				//return false;
		}
		if(con != null) {
			if( !(settings.getOptLevel().trim().equalsIgnoreCase("Default"))) {
				try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
				}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of Oracle: "+se);
				}
			}
			else {
				try	{
					Statement stmt = createStatement();
					OracleInfo oi = new OracleInfo();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = " + oi.defaultOptLevel;
					stmt.execute(optLevelQuery);
				}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of Oracle: "+se);
				}
			}

					
		try
		{	
			Statement stmt = createStatement();
			stmt.executeUpdate("create index "+settings.getSchema()+".PlanTableStmtID on " +settings.getSchema()+".PLAN_TABLE (\"STATEMENT_ID\")");
		}
		//If the index already exists, don't do anything
		catch(SQLException e)
		{
			//System.err.println(e.getMessage());
		}

		return true;
		}
		return false;
	}

	public Histogram getHistogram(String tabName, String schema, String attribName)
		throws PicassoException
	{
		return new OracleHistogram(this, tabName, schema, attribName);
	}

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
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Deleting Picasso Diagram "+e);
			throw new PicassoException("Error Deleting Picasso Diagram "+e);
		}
	}

	public void emptyPlanTable() throws PicassoException
	{
		qno = 0;
		try{
			Statement stmt = createStatement ();
			stmt.executeUpdate ("delete from plan_table");
			//System.err.println("delete from plan_table;");
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error emptying plan table: "+e);
			throw new PicassoException("Database: Error emptying plan table: "+e);
		}
	}

	public void removeFromPlanTable(int qno) throws PicassoException
	{
		try{
			Statement stmt = createStatement ();
			stmt.executeUpdate ("delete from plan_table where statement_id='"+qno+"'");
			//System.err.println("delete from plan_table where statement_id='"+qno+"';");
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error emptying plan table: "+e);
			throw new PicassoException("Database: Error emptying plan table: "+e);
		}
	}

	public boolean checkPlanTable()
	{
		return checkTable("plan_table");
	}

	protected void createPicassoColumns(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create view "+settings.getSchema()+".picasso_columns( column_name, table_name,owner)" +
		" as select column_name, table_name, owner from ALL_TAB_COLUMNS");
	}
	
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID INTEGER NOT NULL, DIMNUM INTEGER NOT NULL, RESOLUTION INTEGER NOT NULL, STARTPOINT FLOAT NOT NULL, ENDPOINT FLOAT NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE)");
	}

        protected void createQTIDMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID INTEGER NOT NULL, QTEMPLATE CLOB, " +
			"QTNAME VARCHAR(" + PicassoConstants.QTNAME_LENGTH + ") UNIQUE NOT NULL, RESOLUTION INTEGER, DIMENSION INTEGER,  EXECTYPE VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), " +
			"OPTLEVEL VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), PLANDIFFLEVEL VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), GENTIME INTEGER, GENDURATION INTEGER, PRIMARY KEY (QTID))");
	}
	protected void createPlanTree(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTree ( QTID INTEGER NOT NULL, PLANNO INTEGER NOT NULL, ID INTEGER NOT NULL, PARENTID INTEGER NOT NULL, "+
		"NAME varchar(" + PicassoConstants.SMALL_COLUMN + "), COST FLOAT, CARD FLOAT, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
		"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createPlanTreeArgs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID INTEGER NOT NULL, PLANNO INTEGER NOT NULL, ID INTEGER NOT NULL, "+
				"ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME,ARGVALUE), " +
				"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	
	protected void createPlanStore(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID INTEGER NOT NULL, QID INTEGER NOT NULL, PLANNO INTEGER, COST FLOAT, CARD FLOAT, " +
		"RUNCOST FLOAT, RUNCARD FLOAT, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createSelectivityMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID INTEGER NOT NULL, QID INTEGER NOT NULL, DIMENSION INTEGER NOT NULL, " +
        	"SID INTEGER NOT NULL, PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createSelectivityLog(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID INTEGER NOT NULL, DIMENSION INTEGER NOT NULL, SID INTEGER NOT NULL, " +
		"PICSEL FLOAT, PLANSEL FLOAT, PREDSEL FLOAT, DATASEL FLOAT, CONST VARCHAR("+PicassoConstants.LARGE_COLUMN+"), " +
		"PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID INTEGER NOT NULL, " +
				"SAMPLESIZE FLOAT, SAMPLINGMODE INTEGER, AREAERROR FLOAT, IDENTITYERROR FLOAT ,FPCMODE INTEGER, " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	public Plan getPlan(String query) throws PicassoException
	{
		Plan plan;
		ResultSet rset;
		Statement stmt;

		// fire Query
		try	{
			qno++;
			stmt = createStatement();
			stmt.executeUpdate("explain plan set statement_id='" + qno + "' for " + query);
			//System.err.println("explain plan set statement_id='" + qno + "' for " + query + ";");
			stmt.close();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error explaining query: "+e);
			throw new PicassoException("Database: Error explaining query: "+e);
		}

		// Initialize
		plan = new Plan();

		String planQuery = "select id,parent_id,operation,object_name, " +
				"cost, cardinality, options from "+settings.getSchema()+".PLAN_TABLE where statement_id='" + qno + "' order by id";
		try	{
			// getting information from plan_table table to get plan
			// tree information
			Node node;
                        java.util.Hashtable HT = new java.util.Hashtable();
			stmt = createStatement ();
			rset = stmt.executeQuery (planQuery);
			//System.err.println(planQuery + ";");
			int curNode = 0;
			String operation, option;
			int indexId = 100;
			while (rset.next ()) {
				node = new Node();
				/*
				 * Warning: Update from planQuery
				 * The following is the ordering of information accessed from Oracle explain tables
				 * 1: Id
				 * 2: Parent Id
				 * 3: Operation
				 * 4: Object Name
				 * 5: Cost
				 * 6: Cardinality
				 * 7: Options
				 */
				node.setId(rset.getInt(1));
				if(curNode==0)
					node.setParentId(-1);
				else
					node.setParentId(rset.getInt(2));
				operation = rset.getString(3);
				node.setCost(rset.getDouble(5));
				node.setCard(rset.getDouble(6));
				option = rset.getString(7);
				if(option != null){
					node.addArgType("options");
					node.addArgValue(rset.getString(7));
				}
				node.setName(operation);
				plan.setNode(node,curNode);
				curNode++;
				if(operation.equals("TABLE ACCESS") ) { //|| operation.equals("INDEX")){
					/*node = new Node();
					node.setId(-1);
					node.setParentId(rset.getInt(1));
					node.setName(rset.getString(4));
					node.setCost(0.0);
					node.setCard(0.0);
					plan.setNode(node,curNode);
					curNode++;*/
                                        HT.put(""+rset.getInt(1),rset.getString(4));
				}
				if (operation.equals("INDEX")) {
					String indexStr = "select TABLE_NAME from user_indexes where index_name='" + rset.getString(4) + "'";
					Statement stmt1 = createStatement ();
					ResultSet rset1 = stmt1.executeQuery (indexStr);
                                        int tblacsid = node.getParentId();
                                        node = new Node();
					node.setId(indexId);
					node.setParentId(rset.getInt(1));
					node.setName(rset.getString(4));
                                        node.setCost(0.0);
					node.setCard(0.0);
					plan.setNode(node,curNode);
					curNode++;

                                        
                                        if(HT.get(""+tblacsid)!=null)
                                        {
                                        node = new Node();
					node.setId(-1);
					node.setParentId(indexId++);
					node.setName((String)HT.remove(""+tblacsid));
					node.setCost(0.0);
					node.setCard(0.0);
					plan.setNode(node,curNode);
					curNode++;
                                        }
                                        else if (rset1.next()) {
						node = new Node();
						node.setId(-1);
						node.setParentId(indexId++);
						node.setName(rset1.getString(1));
						node.setCost(0.0);
						node.setCard(0.0);
						plan.setNode(node,curNode);
						curNode++;
					}
                                        rset1.close();
                                        stmt1.close();
				}
			}
			rset.close();
			stmt.close ();

                        for(java.util.Enumeration enum1 = HT.keys(); enum1.hasMoreElements();)
                        {
                                int pid = Integer.parseInt((String)enum1.nextElement());
                                node = new Node();
        			node.setId(-1);
				node.setParentId(pid);
				node.setName((String)HT.get(""+pid));
				node.setCost(0.0);
				node.setCard(0.0);
				plan.setNode(node,curNode);
				curNode++;
                        }
                            
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error accessing plan: "+e);
			throw new PicassoException("Database: Error accessing plan: "+e);
		}
		return plan;
	}

	public Plan getPlan(String query,int startQueryNumber) throws PicassoException
	{
		int tmp = qno;
		qno = startQueryNumber-1;
		Plan plan = getPlan(query);
		qno = tmp;
		return plan;
	}
}

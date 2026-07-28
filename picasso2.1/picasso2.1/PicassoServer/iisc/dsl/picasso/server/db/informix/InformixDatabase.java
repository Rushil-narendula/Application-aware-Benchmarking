package iisc.dsl.picasso.server.db.informix;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.db.InformixInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.oracle.OracleHistogram;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import com.informix.jdbc.*;  

public class InformixDatabase extends Database {
	private int qno;

	public InformixDatabase(DBSettings settings) throws PicassoException
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
		
		try
		{
			//Uncomment to enable.
			//Class.forName("com.informix.jdbc.IfxDriver");
		}
		catch (Exception e)
		{
			System.out.println("Error: failed to load Informix JDBC driver.");
			e.printStackTrace();
			return false;
		} // End Driver Loading Block

		System.out.println("Informix JDBC driver loaded successfully.");
		
		try{
			//System.out.println(System.getProperty("INFORMIXSERVER"));
			connectString = "jdbc:informix-sqli://"+ settings.getServerName() + ":" +	settings.getServerPort() +"/" + settings.getDbName()+ 
			":INFORMIXSERVER=" +  settings.getUserName() + ";password=" + settings.getPassword();
			//DriverManager.registerDriver (new informix.jdbc.driver.InformixDriver ());
			con = DriverManager.getConnection (connectString);
		}
		catch(SQLException e)
		{
			System.out.println("ERROR: Failed to connect!");
			System.out.println("ERROR:" + e.getMessage());
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: " + e);
            throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
//			return false;
		} // End of Connect Database block

		System.out.println("\nConnection established\n");

		if(con != null) {
			if( !(settings.getOptLevel().trim().equalsIgnoreCase("Default"))) {
				/*try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
				}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of Oracle: "+se);
				}*/
			}
			else {/*
				try	{
					Statement stmt = createStatement();
					InformixInfo oi = new InformixInfo();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = " + oi.defaultOptLevel;
					stmt.execute(optLevelQuery);
				}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of Oracle: "+se);
				}
			*/}
			return true;
		}
		return false;
	}
	
	public Histogram getHistogram(String tabName, String schema, String attribName)
	throws PicassoException
{
	return new InformixHistogram(this, tabName, schema, attribName);
}
	
	
	
	

	public void emptyPlanTable() throws PicassoException
	{
		qno = 0;
		try{
			Statement stmt = createStatement ();
			stmt.executeUpdate ("delete from sqexplain_plan");
			//System.err.println("delete from sqexplain_plan;");
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
			stmt.executeUpdate ("delete from sqexplain_plan where statement_id="+1); //actually qno
			//System.err.println("delete from sqexplain_plan where statement_id='"+qno+"';");
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error emptying plan table: "+e);
			throw new PicassoException("Database: Error emptying plan table: "+e);
		}
	}

	public boolean checkPlanTable()
	{
		return checkTable("sqexplain_plan");
	}

	protected void createPicassoColumns(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create view "+settings.getSchema()+".picasso_columns( column_name, table_name,owner)" +
		" as select syscolumns.colname, systables.tabname, systables.owner from syscolumns, systables where syscolumns.tabid = systables.tabid");
    }

        protected void createQTIDMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID INTEGER NOT NULL, QTEMPLATE LVARCHAR(5000), " +
			"QTNAME VARCHAR(" + PicassoConstants.QTNAME_LENGTH + ") NOT NULL UNIQUE, RESOLUTION INTEGER, DIMENSION INTEGER,  EXECTYPE VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), " +
			"OPTLEVEL VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), PLANDIFFLEVEL VARCHAR(" + PicassoConstants.SMALL_COLUMN + "), GENTIME INT8, GENDURATION INT8, PRIMARY KEY (QTID))");
	}
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID INTEGER NOT NULL, DIMNUM INTEGER NOT NULL, RESOLUTION INTEGER NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID))");
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
		"PICSEL FLOAT, PLANSEL FLOAT, PREDSEL FLOAT, DATASEL FLOAT, CONST LVARCHAR("+PicassoConstants.LARGE_COLUMN+"), " +
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
			stmt.executeUpdate("delete from sqexplain_plan");
			stmt.executeUpdate("set explain on"); //avoid_execute
			stmt.execute(query);
			stmt.executeUpdate("set explain off");
			//stmt.executeUpdate("explain plan set statement_id=" + qno + " for " + query);
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
				"cost, cardinality, options from "+settings.getSchema()+".sqexplain_plan"
				/* + "where statement_id='" + qno + "'" */ 
				+ " order by id";
		try	{
			// getting information from sqexplain_plan table to get plan
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
				 * 
				 * The following is the ordering of information accessed from Informix explain tables
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
				
				
				    if(operation.equals("SEQUENTIAL SCAN") || operation.equals("INDEX PATH")){
					node = new Node();
					node.setId(-1);
					node.setParentId(rset.getInt(1));
					node.setName(rset.getString(4));
					node.setCost(0.0);
					node.setCard(0.0);
					plan.setNode(node,curNode);
					curNode++;
                              /*          HT.put(""+rset.getInt(1),rset.getString(4)); */
				}
				/*
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
			*/}
			rset.close();
			stmt.close ();
/*
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
  */                          
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
}

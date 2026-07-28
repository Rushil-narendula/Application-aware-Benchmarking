/*
 # Copyright (C) 2005, 2006 Indian Institute of Science
 # Bangalore 560012, INDIA
 #
 # This program is part of the PICASSO distribution invented at the
 # Database Systems Lab, Indian Institute of Science. The use of
 # the software is governed by the licensing agreement set up between 
 # the owner, Indian Institute of Science, and the licensee.
 #
 # This program is distributed WITHOUT ANY WARRANTY; without even the 
 # implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 #
 # The public URL of the PICASSO project is
 # http://dsl.serc.iisc.ernet.in/projects/PICASSO/index.html
 #
 # For any issues, contact 
 #       Prof. Jayant R. Haritsa
 #       SERC
 #       Indian Institute of Science
 #       Bangalore 560012, India.
 #       Telephone: (+91) 80 2293-2793
 #       Fax      : (+91) 80 2360-2648
 #       Email: haritsa@dsl.serc.iisc.ernet.in
 #       WWW: http://dsl.serc.iisc.ernet.in/~haritsa
 */
package iisc.dsl.picasso.server.db.mysql;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class MysqlDatabase extends Database 
{
	private int qno;
	public MysqlDatabase(DBSettings settings) throws PicassoException
	{
		super(settings);
		qno = 0;
	}
	
	public boolean connect(DBSettings settings) 
	{
		String connectString;
		if(isConnected())
				return true;
		this.settings = settings;
		try{
			connectString = "jdbc:mysql://" + settings.getServerName() + ":" +	settings.getServerPort() + "/" + settings.getDbName();
			//Register the JDBC driver for MySQL
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			//Get a connection to the database
			con = DriverManager.getConnection (connectString, settings.getUserName(), settings.getPassword());
			}
		catch (Exception e)	{ 
			ServerMessageUtil.SPrintToConsole("Database: " + e);
			return false;
			}
		if(con != null) {
			if( !(settings.getOptLevel().trim().equalsIgnoreCase("Default"))) {
				try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
					}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database : Error setting the Optimization Level of Oracle : "+se);
					}
			}
			else {
				try	{
					Statement stmt = createStatement();
					String optLevelQuery ="alter session set OPTIMIZER_MODE = "+settings.getOptLevel();
					stmt.execute(optLevelQuery);
					}
				catch(SQLException se) {
					ServerMessageUtil.SPrintToConsole("Database : Error setting the Optimization Level of Oracle : "+se);
					}
			}
			return true;
		}
		return false;
	}
	
	public Histogram getHistogram(String tabName, String schema, String attribName) throws PicassoException
	{
		return new MysqlHistogram(this, tabName, schema, attribName);
	}
	
//	MySql server doesn't have plantables 
	public void emptyPlanTable(){ }
	public void removeFromPlanTable(int qno){ }
	
	public boolean checkPlanTable()
	{
		return true;
	}
	protected void createPicassoColumns(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create view "+settings.getSchema()+".picasso_columns as SELECT COLUMN_NAME, TABLE_NAME, TABLE_SCHEMA AS owner" +
		" FROM  INFORMATION_SCHEMA.COLUMNS");
	}
	
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID int NOT NULL, DIMNUM int NOT NULL, RESOLUTION int NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID))");
	}
	
	protected void createQTIDMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID int, QTEMPLATE longtext, " +
				"QTNAME varchar(" + PicassoConstants.MEDIUM_COLUMN + ") UNIQUE NOT NULL, RESOLUTION integer, DIMENSION integer, EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
		"OPTLEVEL varchar(" + PicassoConstants.QTNAME_LENGTH + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIME bigint, GENDURATION bigint, PRIMARY KEY (QTID))");
	}
	
	protected void createPlanTree(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTree ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, PARENTID int NOT NULL, "+
				"NAME varchar(" + PicassoConstants.MEDIUM_COLUMN + "), COST float, CARD float,  " +
		"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createPlanTreeArgs(Statement stmt) throws SQLException 
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, "+
				"ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, " +
		"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	
	protected void createPlanStore(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID int NOT NULL, QID int NOT NULL, PLANNO int, COST float, CARD float, " +
		"RUNCOST float, RUNCARD float, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createSelectivityMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID int NOT NULL, QID int NOT NULL, DIMENSION int NOT NULL, " +
		"SID int NOT NULL, PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createSelectivityLog(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
				"PICSEL float, PLANSEL float, PREDSEL float, DATASEL float, CONST varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
		"PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID int NOT NULL, " +
				"SAMPLESIZE float, SAMPLINGMODE int, AREAERROR float, IDENTITYERROR float,FPCMODE int, " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	 protected void createPicassoHistogram(Statement stmt) throws SQLException
		{
		 try {
				stmt.executeUpdate("create table " +settings.getSchema()+ ".picassohistogram (SCHEMANAME VARCHAR (20) ASCII NOT NULL," +
				"TABLENAME VARCHAR (64) ASCII NOT NULL, COLUMNNAME VARCHAR (20) ASCII NOT NULL,ISHISTOGRAM VARCHAR (4) ASCII NOT NULL, PRIMARY KEY(SCHEMANAME,TABLENAME,COLUMNNAME))");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	 
	 
	 public void enterNode(int id, int pid, String name, double cost, double card, String arg){
		    
	    	try {
				Statement stmt2 = createStatement();
				stmt2.executeUpdate("INSERT INTO "+settings.getSchema()+".EXPLAINTABLE VALUES ("+id+","+pid+",'"+name.trim()+"',"+cost+","+card+",'"+arg.trim()+"')");
				stmt2.close();	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 		
	    }
	
		double filter= 1; 
		int flag =1;
	
	public Plan getPlan(String query) throws PicassoException
	{
		Plan plan = new Plan();
		ResultSet rset,rset1,rset2;
		String sel_type, tb_name, join_arg, join_arg_key_used, tb_arg, join_strat="NL Join", tb_strat, extra;
		boolean sort_flag = false;
		Statement stmt,stmt1,stmt2;
		int count = 0 ,id = 0,pid= 0,row = 0, tabCnt=0, jid=3;
		double card=1, cost =0, card_j=1, card_base=0;
		
		/* EXPLAIN EXTENDED COLUMN FORMAT
		 * 2.)SELECT_TYPE
		 * 3.)TABLE
		 * 4.)jOIN ARGUMENT (for first table single table access strategy) can be used for parameter diff
		 * 6.)SINGLE TABLE ACCESS STRATEGY
		 * 8.) KEY/INDEX USED FOR join
		 * 9.)cardinality
		 * 11.) EXTRA INFO
		 * 
		 */
		
		try {
			stmt = createStatement();
			rset=stmt.executeQuery("EXPLAIN "+query);
			while(rset.next()){
					row++;		
			if(rset.getString(2).matches(".*SUBQUERY.*")){
				ServerMessageUtil.SPrintToConsole("Error: Subquery is not allowed");
				throw new PicassoException("Error: Subquery is not allowed");
				}
			if(rset.getString(2).matches(".*DERIVED.*")){
				ServerMessageUtil.SPrintToConsole("Error: Subquery is not allowed");
				throw new PicassoException("Error: Subquery is not allowed");
				}
			}
			rset.close();
			stmt.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		createExplainTable();
		if(row==1){
			
			try {
				stmt = createStatement();
				rset=stmt.executeQuery("EXPLAIN EXTENDED "+query);
				
				while(rset.next()){
					
					sel_type = rset.getString(2);
					tb_name = rset.getString(3);
					join_arg = " Type: "+rset.getString(4);
					tb_arg = rset.getString(6);
					join_arg_key_used = rset.getString(8);
					card = rset.getDouble(9);
					filter = rset.getDouble(10);
					card_j = card*(filter/100);
					extra = rset.getString(11);			
						if(extra.matches(".*filesort.*"))
							sort_flag=true;
					pid=id;	
					Statement stmt5 = con.createStatement();
					ResultSet rset5 = stmt5.executeQuery("select TABLE_ROWS from INFORMATION_SCHEMA.TABLES where TABLE_NAME='"+tb_name+"' and TABLE_SCHEMA='"+ settings.getSchema()+"'"); 
					while(rset5.next()){
					card_base=rset5.getDouble(1);}
					enterNode(-1,pid,tb_name,cost,card_base,tb_name);			//table node
					tabCnt++;
					stmt5.close();
					rset5.close();
					if(tb_arg == null){
						tb_strat = "TABLE SCAN";
						tb_arg = "TABLE SCAN";
					}
					else if(tb_arg.matches(".*PRIMARY.*"))
						tb_strat = "KEY ACCESS";
						
					else 
					{
						stmt1 = createStatement();
						rset1=stmt1.executeQuery("SELECT COLUMN_NAME FROM information_schema.`COLUMNS` C where TABLE_SCHEMA= '" + settings.getSchema()+"' AND TABLE_NAME='"+tb_name+"' AND COLUMN_NAME='"+tb_arg+"';");
						if(rset1.next())	
								tb_strat = "KEY ACCESS";
						else
								tb_strat = "INDEX ACCESS";
						
						rset1.close();
						stmt1.close();
					}
					pid=id+1;
					if(flag!=1)
					{
						Statement stmt3 = createStatement();
						ResultSet rset3=stmt3.executeQuery(query);
						while(rset3.next())
						{
							card = rset3.getDouble(1);
						}
						rset3.close();
						stmt3.close();
					}
					enterNode(id,pid,tb_strat,cost,card, tb_arg);	//Single table strategy node
					id++;				
				}
				rset.close();
				stmt.close();
				
				if(sort_flag){
					pid=id+1;
					enterNode(id,pid,"SORT",cost,card,"SORT");		//SORT node
					id++;						
				}
				
				
					stmt2 = createStatement();
					stmt2.executeQuery("EXPLAIN EXTENDED "+query);
					rset2=stmt2.executeQuery("SHOW STATUS LIKE 'Last_query_cost'");
						if(rset2.next())
						{
								cost = (rset2.getDouble(2));
							//	System.out.println("Cost1: " + cost);
						}		
					rset2.close();
					stmt2.close();
					
					pid=id+1;
					enterNode(id,pid,"SELECT",cost,card,"SELECT STATEMENT");  //Select node
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		else
		{
		try {
			stmt = createStatement();
			rset=stmt.executeQuery("EXPLAIN EXTENDED "+query);
			
			while(rset.next()){
				
				sel_type = rset.getString(2);
				tb_name = rset.getString(3);
				join_arg = " Type: "+rset.getString(4);
				tb_arg = rset.getString(6);
				join_arg_key_used = rset.getString(8);
				card = rset.getDouble(9);
				filter = rset.getDouble(10);
				card = card*(filter/100);
				card_j = card*card_j;
				extra = rset.getString(11);
				Statement stmt5 = con.createStatement();
				ResultSet rset5 = stmt5.executeQuery("select TABLE_ROWS from INFORMATION_SCHEMA.TABLES where TABLE_NAME='"+tb_name+"' and TABLE_SCHEMA='"+ settings.getSchema()+"'"); 
				while(rset5.next()){
				card_base=rset5.getDouble(1);}
				if(count == 0){
					if(extra.matches(".*filesort.*"))
						sort_flag=true;
					count++;
				}
			pid=id;	
			enterNode(-1,pid,tb_name,cost,card_base,tb_name);			//table node
			stmt5.close();
			rset5.close();
			tabCnt++;
			
			if(tb_arg == null){
				tb_strat = "TABLE SCAN";
				tb_arg = "TABLE SCAN";
			}
			else if(tb_arg.matches(".*PRIMARY.*"))
				tb_strat = "KEY ACCESS";
				
			else 
			{
				stmt1 = createStatement();
				rset1=stmt1.executeQuery("SELECT COLUMN_NAME FROM information_schema.`COLUMNS` C where TABLE_SCHEMA= '" + settings.getSchema()+"' AND TABLE_NAME='"+tb_name+"' AND COLUMN_NAME='"+tb_arg+"';");
				if(rset1.next())	
						tb_strat = "KEY ACCESS";
				else
				{
						tb_strat = "INDEX ACCESS";
						//System.out.println(tb_arg);
				}
				rset1.close();
				stmt1.close();
			}
			
			if(id==1 || (row-1)==0) pid = id+2; else pid = id +3;
			
			enterNode(id,pid,tb_strat,cost,card,tb_arg);	//Single table strategy node
			if(count < 3)
			id++;		
			else id=id+2;
			
		if(count>1)	
		{
			if(extra.matches(".*Using join buffer*."))
			{
				if(join_arg.matches(".*ALL*.") || join_arg.matches(".*index*.") || join_arg.matches(".*range*."))
						join_strat = "Block Nested-Loop";
				if(join_arg.matches(".*ref*.") || join_arg.matches(".*eq_ref*."))
					join_strat = "Batched Key Access";
			}
			if((row-2)>0) {
				pid = jid+2;	
			}
			else pid=jid+1;
			enterNode(jid,pid,join_strat,cost,card_j,join_arg);	//Join node
			if((row-2)>0) {
				jid = jid+2;	
			}
			else jid=jid+1;						
		}
		count++;
		row--;
		}
		id=jid;
			if(sort_flag){
				pid=id+1;
				enterNode(id,pid,"SORT",cost,card_j,"SORT");		//SORT node
				id++;						
			}
			
			rset.close();
			stmt.close();
			
				stmt2 = createStatement();
				stmt2.executeQuery("EXPLAIN EXTENDED "+query);
				rset2=stmt2.executeQuery("SHOW STATUS LIKE 'Last_query_cost'");
					if(rset2.next()){
							cost = (rset2.getDouble(2));
							//System.out.println("Cost2: " + cost);
					}
				rset2.close();
				stmt2.close();
				
				pid=id+1;
				enterNode(id,pid,"SELECT",cost,card_j,"SELECT STATEMENT");  //Select node
				
				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			deleteExplainTable();
			e.printStackTrace();
		}
		}
		plan = generateTree(tabCnt);
		deleteExplainTable();
		return plan;
	}
	
	public void processExplain(int tabCnt) {
		
		Plan plan1 = new Plan();
		int i=0,cnt=0,count;
		
		int id,pid;
		double cost,card;
		String name;
		String arg;		
		
		
		try {
			
			Statement stmt = createStatement ();
			ResultSet rset=stmt.executeQuery("select count(*) from "+settings.getSchema()+".EXPLAINTABLE");
			rset.next();
			count = rset.getInt(1);
			rset.close();
			stmt.close();
			
			count-=tabCnt;
			
			 stmt = createStatement ();
			 rset=stmt.executeQuery("select * from "+settings.getSchema()+".EXPLAINTABLE");
			
			while(rset.next()){
				
				id=rset.getInt(1);
				pid=rset.getInt(2);
				name=rset.getString(3);
				cost=rset.getDouble(4);
				card=rset.getDouble(5);
				arg = rset.getString(6);
				
				if(qno!=PicassoConstants.HIGH_QNO-1){
				if(id != -1)
					id=count - id;
				pid = count - pid;
				}
				else
					card=card*filter/100;
				Statement stmt2 = createStatement();
				stmt2.executeUpdate("INSERT INTO "+settings.getSchema()+".TEMP_EXPLAINTABLE VALUES ("+id+","+pid+",'"+name.trim()+"',"+cost+","+card+",'"+arg.trim()+"')");
				stmt2.close();	
			}
			
			rset.close();
			stmt.close();
			Statement stmt2 = createStatement(); 
    		stmt2.executeUpdate("delete from "+settings.getSchema()+".EXPLAINTABLE");
    		stmt2.close();
    		
    		stmt2 = createStatement(); 
    		ResultSet rset2=stmt2.executeQuery("select * from "+settings.getSchema()+".TEMP_EXPLAINTABLE order by ID");
    		while(rset2.next())
    		{
    			id=rset2.getInt(1);
				pid=rset2.getInt(2);
				name=rset2.getString(3);
				cost=rset2.getDouble(4);
				card=rset2.getDouble(5);
				arg = rset2.getString(6);
				Statement stmt3 = createStatement();
				stmt3.executeUpdate("INSERT INTO "+settings.getSchema()+".EXPLAINTABLE VALUES ("+id+","+pid+",'"+name.trim()+"',"+cost+","+card+",'"+arg.trim()+"')");
				stmt3.close();	
    		}
    		
    		rset2.close();
    		stmt2.close();
			
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		try {
			Statement stmt = createStatement ();
			ResultSet rset=stmt.executeQuery("select * from "+settings.getSchema()+".EXPLAINTABLE");
			
			while(rset.next()){
				
				id=rset.getInt(1);
				pid=rset.getInt(2);
				name=rset.getString(3);
				cost=rset.getDouble(4);
				card=rset.getDouble(5);
				arg = rset.getString(6);
				
				if(rset.getInt(1) == -1){
				Node temp_node = new Node();
					temp_node.setId(id);
					temp_node.setParentId(pid);
					temp_node.setName(name);
					temp_node.setCost(cost);
					temp_node.setCard(card);
					temp_node.addArgValue(arg);
					plan1.setNode(temp_node,i);
					i++;
				}
				else
				{
					Statement stmt2 = createStatement();
					stmt2.executeUpdate("INSERT INTO "+settings.getSchema()+".NEW_EXPLAINTABLE VALUES ("+id+","+pid+",'"+name.trim()+"',"+cost+","+card+",'"+arg.trim()+"')");
					stmt2.close();	
				}
			}
			rset.close();
			stmt.close();
			
		Node new_node = new Node();
		
			while(cnt<i){
				new_node = plan1.getNode(cnt);
				id = new_node.getId();
				pid = new_node.getParentId();
				name = new_node.getName();
				cost = new_node.getCost();
				card = new_node.getCard();
				arg = new_node.getArgValue().toString();
				
				Statement stmt2 = createStatement();
				stmt2.executeUpdate("INSERT INTO "+settings.getSchema()+".NEW_EXPLAINTABLE VALUES ("+id+","+pid+",'"+name.trim()+"',"+cost+","+card+",'"+arg.trim()+"')");
				stmt2.close();	
			cnt++;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Plan generateTree(int tabCnt) {
		
		Plan plan = new Plan();
		int curNode = 0, cnt=0;
		try {
			processExplain(tabCnt);
			
			Statement stmt = createStatement ();
			ResultSet rset=stmt.executeQuery("select * from "+settings.getSchema()+".NEW_EXPLAINTABLE");
						
			while(rset.next()){
				Node node = new Node();
				if(rset.getInt(1)== -1)
					node.setId(-1);
				else
					node.setId(rset.getInt(1));
				
				node.setParentId(rset.getInt(2));
				node.setName(rset.getString(3).trim());
				node.setCost(rset.getDouble(4));
				node.setCard(rset.getDouble(5));
				node.addArgType("Argument: ");
				node.addArgValue(rset.getString(6).trim());
				plan.setNode(node, curNode);
				curNode++;		
			}
			rset.close();
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return plan;
		
	}
	
public void createExplainTable(){
    	
    	try {
    		Statement stmt2 = createStatement(); 
			stmt2.executeUpdate("create table " +settings.getSchema()+ ".EXPLAINTABLE (id int NOT NULL,pid  int NOT NULL, name varchar (50) ASCII NOT NULL, " +
					"cost double precision , cardinality double precision , arg varchar (50))");
			stmt2.close();
			

			stmt2 = createStatement(); 
			stmt2.executeUpdate("create table " +settings.getSchema()+ ".TEMP_EXPLAINTABLE (id int NOT NULL,pid  int NOT NULL, name varchar (50) ASCII NOT NULL, " +
					"cost double precision , cardinality double precision , arg varchar (50))");
			stmt2.close();
			
			stmt2 = createStatement(); 
			stmt2.executeUpdate("create table " +settings.getSchema()+ ".NEW_EXPLAINTABLE (id int NOT NULL,pid  int NOT NULL, name varchar (50) ASCII NOT NULL, " +
					"cost double precision , cardinality double precision , arg varchar (50))");
			stmt2.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
    }
	 public void deleteExplainTable(){
	    	
	    	try {
	    		Statement stmt2 = createStatement(); 
	    		stmt2.executeUpdate("drop table "+settings.getSchema()+".EXPLAINTABLE");
	    		stmt2.close();
	    		
	    		stmt2 = createStatement(); 
	    		stmt2.executeUpdate("drop table "+settings.getSchema()+".TEMP_EXPLAINTABLE");
	    		stmt2.close();
	    		
	    		stmt2 = createStatement(); 
	    		stmt2.executeUpdate("drop table "+settings.getSchema()+".NEW_EXPLAINTABLE");
	    		stmt2.close();
	    		}
	    	catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	    }
	
	public Plan getPlan(String query,int startQueryNumber) throws PicassoException
	{
		int tmp = qno;
		qno = startQueryNumber-1;
		flag=0;
		Plan plan = getPlan(query);
		flag=1;
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


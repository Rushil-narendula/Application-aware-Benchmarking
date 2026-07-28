
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

package iisc.dsl.picasso.server.db.db2;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.db.DB2Info;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.common.PicassoConstants;

public class DB2Database extends Database {
    
    private int qno;
    private final int NODE_OFFSET = 100;
    private final int INDEX_NODE_OFFSET = 10;
    
    public DB2Database(DBSettings settings) throws PicassoException {
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
                        /*if(settings.getServerName().equals("localhost")) {
                                connectString = "jdbc:db2:" + settings.getDbName();
                                Class.forName ("COM.ibm.db2.jdbc.app.DB2Driver").newInstance();
                        }
                        else*/ {
                            connectString = "jdbc:db2://" + settings.getServerName() + ":" +
                                    settings.getServerPort() + "/" + settings.getDbName();
                          Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
                        }
                        con = DriverManager.getConnection(connectString,
                                settings.getUserName(), settings.getPassword());
                        
        } catch (Exception e)	{
            System.err.println("Database: " + e);
            System.out.println("Database: " + e);
            e.printStackTrace();
            throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
            //return false;
        }
        if(con != null) {
            if(!settings.getOptLevel().equalsIgnoreCase("Default")) {
                try	{
                    Statement stmt = createStatement();
                    String optLevelQuery ="set current query optimization = " + settings.getOptLevel();
                    stmt.execute(optLevelQuery);
                    stmt.close();
                } catch(SQLException se) {
                    ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of DB2 : "+se);
                }
            } else {
                try	{
                    Statement stmt = createStatement();
                    DB2Info di = new DB2Info();
                    String optLevelQuery ="set current query optimization = " + di.defaultOptLevel ;
                    stmt.execute(optLevelQuery);
                    stmt.close();
                } catch(SQLException se) {
                    ServerMessageUtil.SPrintToConsole("Database: Error setting the Optimization Level of DB2 : "+se);
                }
            }
            
            return true;
        }
        return false;
    }
    
    public Histogram getHistogram(String tabName, String schema,String attribName) throws PicassoException {
        return new DB2Histogram(this, tabName, schema, attribName);
    }
    
    public boolean checkPlanTable() {
        return checkTable("explain_statement");
    }
    
    protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID int NOT NULL, DIMNUM int NOT NULL, RESOLUTION int NOT NULL, STARTPOINT double NOT NULL, ENDPOINT double NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES " + settings.getSchema()+ ".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
    
    protected void createPicassoColumns(Statement stmt) throws SQLException {
        stmt.executeUpdate("create view "+settings.getSchema()+".picasso_columns( column_name, table_name,owner)" +
                " as select sysibm.syscolumns.name, sysibm.syscolumns.tbname," +
                " sysibm.syscolumns.tbcreator from sysibm.syscolumns");
    }
    protected void createQTIDMap(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID int NOT NULL , QTEMPLATE long varchar, " +
                "QTNAME varchar(" + PicassoConstants.QTNAME_LENGTH + ") UNIQUE NOT NULL, RESOLUTION int, DIMENSION int, EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
                "OPTLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIME bigint, GENDURATION bigint, PRIMARY KEY (QTID))");
    }
        /*
         * I added PARENTID to the primary key since ID for all leaf nodes are -1 for db2. This has to be fixed
         */
    protected void createPlanTree(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTree ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, PARENTID int NOT NULL, "+
                "NAME varchar(" + PicassoConstants.SMALL_COLUMN + "), COST double, CARD double, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
                "FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createPlanTreeArgs(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, "+
                "ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME,ARGVALUE), " +
                "FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	protected void createPlanStore(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID int NOT NULL, QID int NOT NULL, PLANNO int, COST double, CARD double, " +
                "RUNCOST double, RUNCARD double, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) " +
                "ON DELETE CASCADE )");
    }
    protected void createSelectivityMap(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID int NOT NULL, QID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
                "PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createSelectivityLog(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
                "PICSEL double, PLANSEL double, PREDSEL double, DATASEL double, CONST varchar("+PicassoConstants.LARGE_COLUMN+"), " +
                "PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		try {
			stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID int NOT NULL, " +
					"SAMPLESIZE double, SAMPLINGMODE int, AREAERROR double, IDENTITYERROR double ,FPCMODE int, " +
					"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    // A DB2 specific version of the delete Picasso tables function
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
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Deleting Picasso Diagram "+e);
			throw new PicassoException("Error Deleting Picasso Diagram "+e);
		}
	}
    
    
    public void emptyPlanTable() throws PicassoException {
        qno = 0;
        try{
            Statement stmt = createStatement();
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_predicate");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_argument");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_instance");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_operator");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_object");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_statement");
            stmt.executeUpdate("delete from "+settings.getSchema()+".explain_stream");
            stmt.execute("commit");
            stmt.close();
        }catch(SQLException e) {
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Database: Error emptying plan table: "+e);
            throw new PicassoException("Database: Error emptying plan table: "+e);
        }
    }
    
    public void removeFromPlanTable(int qno) throws PicassoException {
        try{
            Statement stmt = createStatement();
            stmt.executeUpdate("delete from " + settings.getSchema() + ".explain_statement "+
                    "where queryno="+qno);
            stmt.execute("commit");
            stmt.close();
        }catch(SQLException e) {
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Database: Error emptying plan table: "+e);
            throw new PicassoException("Database: Error emptying plan table: "+e);
        }
    }
    
    
    
        /*
         *  May be we can speed up a bit more by not having sub-operator level attributes
         *  in the main query and resultSet. ( Profiling needs to be done )
         */
    public Plan getPlan(String query) throws PicassoException {
        Plan plan;
        ResultSet rset;
        Statement stmt;
        
        explainQuery(query,++qno);
        plan = new Plan();
        
                /*
                 * explain_predicate.how_applied in some cases has 'START' and 'STOP' for which DB2 duplicates
                 * entries. But we don't want this to affect our joins to produce spurious tuples. So we explicitly
                 * avoids tuples in explain_predicate with 'STOP' (arbitrarily) value in how_applied attribute
                 */
        
        String planQuery =
                "select source_id, target_id, operator_type, object_name, explain_operator.total_cost, stream_count, argument_type, argument_value " +
                "from "+settings.getSchema()+".explain_statement, "+settings.getSchema()+".explain_stream LEFT OUTER JOIN "+settings.getSchema()+".explain_operator " +
                "ON (source_id = explain_operator.operator_id or (source_id=-1 and source_id = explain_operator.operator_id)) and " +
                "explain_operator.explain_time = explain_stream.explain_time " +
                "LEFT OUTER JOIN "+settings.getSchema()+".explain_argument " +
                "ON  explain_operator.operator_id = explain_argument.operator_id and explain_stream.explain_time = explain_argument.explain_time " +
                "where explain_stream.explain_time = explain_statement.explain_time and explain_statement.explain_level='P' and queryno=" +
                qno + " order by target_id, source_id, argument_type asc, argument_value asc";
        
        // explain_statement info
        try	{
            // getting information from explain_statement table to get plan
            // tree information
            Node node = null;
            stmt = createStatement();
            rset = stmt.executeQuery(planQuery);
            int curNode = 0;
            int id,indexId;
            String argType, argValue;
            
                                /*
                                 * Warning: Update from planQuery
                                 * The following is the ordering of information accessed from DB2 explain tables
                                 * 1: Id
                                 * 2: Parent Id
                                 * 3: Node Type
                                 * 4: Node Name
                                 * 5: Cost
                                 * 6: Cardinality
                                 * 7: Argument type ( this spans across multiple tuples for the same node )
                                 * 8: Argument value ( this spans across multiple tuples for the same node )
                                 */
            while (rset.next()) {
            /*
            * By using the above query, we lose the first node which is the 'RETURN' with same cost
            * and cardinality as that of the second node. So we just insert it in Plan manually
            * as a special case when curNode == 0
            */
                
                if(curNode == 0) {
                    node = new Node();
                    node.setId(NODE_OFFSET+1);
                    node.setParentId(-1);
                    node.setName("RETURN");
                    node.setCost(rset.getDouble(5));
                    node.setCard(rset.getDouble(6));
                    //node.setPredicate(rset.getString(8));
                    //node.setSelectivity(rset.getDouble(7));
                    plan.setNode(node,curNode);
                    curNode++;
                }
                
                /*We have to Remove the node below Fetch with table name*/
                if(plan.getNodeById(rset.getInt(2)+NODE_OFFSET).getName().equals("FETCH") && rset.getString(3)==null)
                    continue;
                /*Adding node to the Tree */
                id = rset.getInt(1);
                if(id!=-1) {
                    id+=NODE_OFFSET;
                    /*if not already present add it to the plan else it is a suboperator*/
                    if (!plan.isIdPresent(id)){
                        node = new Node();
                        node.setId(id);
                        node.setParentId(rset.getInt(2)+NODE_OFFSET);
                        node.setName(rset.getString(3));
                        node.setCost(rset.getDouble(5));
                        node.setCard(rset.getDouble(6));
                        plan.setNode(node,curNode);
                        curNode++;
                    }
                    argType = rset.getString(7);
                    argValue = rset.getString(8);
                    if(argType==null || argValue==null)
                        continue;
                    if(argType.trim().toUpperCase().equals("NUMROWS"))
                        continue;
                    if(argType.trim().toUpperCase().equals("BITFLTR"))
                        continue;
                    if(argType.trim().toUpperCase().equals("FETCHMAX"))
                        continue;
                    if(argType.trim().toUpperCase().equals("ISCANMAX"))
                        continue;
                    if(argType.trim().toUpperCase().equals("MAXPAGES"))
                        continue;
                    if(argType.trim().toUpperCase().equals("MAXRIDS"))
                        continue;
                    if(argType.trim().toUpperCase().equals("SPILLED"))
                        continue;
                    if(argType.trim().toUpperCase().equals("HASHTBSZ"))
                        continue;
                    if(!node.isArgTypePresent(argType)){
                        node.addArgType(argType);
                        node.addArgValue(argValue);
                    }
                    continue;
                }
                /*
                 *Adding leaf nodes having id = -1
                 */
                node = new Node();
                node.setId(id);
                node.setParentId(rset.getInt(2)+NODE_OFFSET);
                if(id==-1)
                    node.setName(rset.getString(4));
                else
                    node.setName(rset.getString(3));
                node.setCost(rset.getDouble(5));
                node.setCard(rset.getDouble(6));
                plan.setNode(node,curNode);
                curNode++;
            }
            rset.close();
            stmt.close();
            
                        /*
                         *Adding relation names below indexnames
                         */
            int plansize = plan.getSize();
            indexId = INDEX_NODE_OFFSET;
            for(int lp = 0;lp<plansize;lp++) {
                Node curnode = plan.getNode(lp);
                if ( curnode.getParentId()!=-1){
		    String nodename = plan.getNodeById(curnode.getParentId()).getName();
                    if ( nodename.equals("IXSCAN")||
			 nodename.equals("FIXSCAN")||
			 nodename.equals("SIXSCAN")||
			 nodename.equals("EISCAN")){
                        String indexStr = "select TABNAME from SYSCAT.INDEXES where indname='" + curnode.getName() + "'";
                        Statement stmt1 = createStatement();
                        ResultSet rset1 = stmt1.executeQuery(indexStr);
                        if ( rset1.next() ) {
                            curnode.setId(indexId);
                            Node IDXNODE = plan.getNodeById(curnode.getParentId());
                            curnode.setCard(plan.getNodeById(IDXNODE.getParentId()).getCard());
                            IDXNODE.setCard(plan.getNodeById(IDXNODE.getParentId()).getCard());
                            node = new Node();
                            node.setParentId(indexId++);
                            node.setId(-1);
                            
                            node.setName(rset1.getString(1));
                            DB2Histogram toGetCard = new DB2Histogram(this,node.getName(),settings.getSchema());
                            double card = toGetCard.getCardinality();
                            //System.out.println("The cardinality estimated for "+node.getName()+" is: " +card);
                            node.setCost(0.0);
                            node.setCard(card);
                            plan.setNode(node,curNode);
                            curNode++;
                        }
                        rset1.close();
                        stmt1.close();
                    }
                    String curNodeName= curnode.getName();
                    //Added so that column names of the indexes are made visible.
                    if(curNodeName.matches("SQL\\d*")) {
                    	String indexStr = "select COLNAMES from SYSCAT.INDEXES where indname='" + curnode.getName() + "'";
                        Statement stmt1 = createStatement();
                        ResultSet rset1 = stmt1.executeQuery(indexStr);
                        if ( rset1.next() ) {
                         
                        	curnode.addArgType("Index Columns: ");
                            curnode.addArgValue((rset1.getString(1)));
                          
                        }
                        rset1.close();
                        stmt1.close();
                    }
		}
            }
        }catch(SQLException e){
            System.out.println(planQuery);
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Database: Error accessing plan: "+e);
            throw new PicassoException("Database: Error explaining query: "+e);
        }
        return plan;
    }
    
    public Plan getPlan(String query,int startQueryNumber) throws PicassoException {
        int tmp = qno;
        qno = startQueryNumber-1;
        Plan plan = getPlan(query);
        qno = tmp;
        return plan;
    }
    
    private void explainQuery(String query, int qno) throws PicassoException {
        try	{
            Statement stmt = createStatement();
            // String x = "explain plan set queryno=30000000 for select o_year, sum(case when nation = 'BRAZIL' then volume else 0 end) / sum(volume) from ( select YEAR(o_orderdate) as o_year, l_extendedprice * (1 - l_discount) as volume, n2.n_name as nation from DSLADMIN.part, DSLADMIN.supplier, DSLADMIN.lineitem, DSLADMIN.orders, DSLADMIN.customer, DSLADMIN.nation n1, DSLADMIN.nation n2, DSLADMIN.region where p_partkey = l_partkey and s_suppkey = l_suppkey and l_orderkey = o_orderkey and o_custkey = c_custkey and c_nationkey = n1.n_nationkey and n1.n_regionkey = r_regionkey and r_name = 'AMERICA' and s_nationkey = n2.n_nationkey and o_orderdate between '1995-01-01' and '1996-12-31' and p_type = 'ECONOMY ANODIZED STEEL' and s_acctbal  <= 4654.645 and l_extendedprice  <= 36615.4793485745 ) as all_nations group by o_year order by o_year";
            stmt.executeUpdate("explain plan set queryno=" + qno + " for " + query);
            //stmt.executeUpdate(x);
            // stmt.executeUpdate("EXPLAIN PLAN SET QUERYNO = 13 FOR SELECT * FROM NATION");
            stmt.close();
        }catch(SQLException e) {
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Database: Error explaining query: "+e);
            throw new PicassoException("Database: Error explaining query: "+e);
        }
    }
}


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

package iisc.dsl.picasso.server.db.postgres;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class PostgresDatabase extends Database {
    private int leafid=0;
    
    public PostgresDatabase(DBSettings settings) throws PicassoException {
        super(settings);
    }

    protected void createPicassoHistogram(Statement stmt) throws SQLException {
    }

    
    public boolean connect(DBSettings settings) throws PicassoException {
        String connectString = null;
        if(isConnected())
            return true;
        this.settings = settings;
        try{
            connectString="jdbc:postgresql://"+settings.getServerName()+":"+settings.getServerPort()+"/"+settings.getDbName();
            Class.forName("org.postgresql.Driver").newInstance();
            con=DriverManager.getConnection(connectString,settings.getUserName(),
                    settings.getPassword());
        } catch (Exception e)	{
            System.err.println("Database: " + e);
            System.out.println("Database: " + e);
			e.printStackTrace();
            throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
            //return false;
        }
        if(con != null)
            return true;
        return false;
    }
    
    public Histogram getHistogram(String tabName, String schema, String attribName) throws PicassoException {
        return new PostgresHistogram(this, tabName, schema, attribName);
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
				stmt.executeUpdate("delete from "+getSchema()+".PicassoRangeResMap where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoApproxMap where QTID="+qtid);
				stmt.executeUpdate("delete from "+getSchema()+".PicassoQTIDMap where QTNAME='"+queryName+"'");
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Deleting Picasso Diagram "+e);
			throw new PicassoException("Error Deleting Picasso Diagram "+e);
		}
	}

    public void emptyPlanTable() { }
    public void removeFromPlanTable(int qno) { }
    
    public boolean checkPlanTable() {
        return true;
    }
    
    protected void createPicassoColumns(Statement stmt) throws SQLException {
        stmt.executeUpdate("create view picasso_columns  (column_name, table_name, owner) as" +
                " select pg_attribute.attname, pg_class.relname, pg_class.relowner " +
                " from pg_class, pg_attribute where pg_attribute.attrelid = pg_class.oid");
    }
    
    // changed QTName's length from MEDIUM_COLUMN to LARGE_COLUMN. -ma
    protected void createQTIDMap(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID integer NOT NULL , QTEMPLATE text, " +
                "QTNAME varchar(" + PicassoConstants.QTNAME_LENGTH + ") UNIQUE NOT NULL, RESOLUTION integer, DIMENSION integer, EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
                "OPTLEVEL varchar(" + PicassoConstants.MEDIUM_COLUMN + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIME bigint, GENDURATION bigint, PRIMARY KEY (QTID))");
    }
    protected void createPlanTree(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+
                ".PicassoPlanTree ( QTID integer NOT NULL, PLANNO integer NOT NULL, ID integer NOT NULL, PARENTID integer NOT NULL, "+
                "NAME varchar(" + PicassoConstants.MEDIUM_COLUMN + "), COST float8, CARD float8, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
                "FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createPlanTreeArgs(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID integer NOT NULL, PLANNO integer NOT NULL, ID integer NOT NULL, "+
                "ARGNAME varchar(" + PicassoConstants.MEDIUM_COLUMN + ") NOT NULL, ARGVALUE varchar(" + PicassoConstants.MEDIUM_COLUMN + ") NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME,ARGVALUE), " +
                "FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
	}
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID integer NOT NULL, DIMNUM integer NOT NULL, RESOLUTION integer NOT NULL, "+
				"STARTPOINT integer NOT NULL, ENDPOINT integer NOT NULL, "+
				"PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID))");
	}
    protected void createPlanStore(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID integer NOT NULL, QID integer NOT NULL, PLANNO integer, COST float8, CARD float8, " +
                "RUNCOST float8, RUNCARD float8, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) " +
                "ON DELETE CASCADE )");
    }
    protected void createSelectivityMap(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID integer NOT NULL, QID integer NOT NULL, DIMENSION integer NOT NULL, SID integer NOT NULL, " +
                "PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createSelectivityLog(Statement stmt) throws SQLException {
        stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID integer NOT NULL, DIMENSION integer NOT NULL, SID integer NOT NULL, " +
                "PICSEL float8, PLANSEL float8, PREDSEL float8, DATASEL float8, CONST varchar("+PicassoConstants.LARGE_COLUMN+"), " +
                "PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
    }
    protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID INTEGER NOT NULL, " +
				"SAMPLESIZE float8, SAMPLINGMODE INTEGER, AREAERROR float8, IDENTITYERROR float8,FPCMODE INTEGER, " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES "+settings.getSchema()+".PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
    public Plan getPlan(String query,int startQueryNumber) throws PicassoException {
        return getPlan(query);
    }
    
    public Plan getPlan(String query) throws PicassoException {
        //System.out.println("Query :"+query);
        Vector textualPlan = new Vector();
        Plan plan = new Plan();
        
        try{
            //System.out.println("Trying to Explain Query");
            Statement stmt = createStatement();
            ResultSet rs = stmt.executeQuery("EXPLAIN "+query);
            while(rs.next())
                textualPlan.add(rs.getString(1));
            rs.close();
            stmt.close();
            if(textualPlan.size()<=0)
                return null;
                        /*ListIterator it = textualPlan.listIterator();
                        System.out.println("Parsing Query Plan");
                        while(it.hasNext())
                                System.out.println((String)it.next());*/
        }catch(Exception e){
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
            throw new PicassoException("Error getting plan: "+e);
        }
        //parseNode(plan,0,-1,textualPlan);
        //plan.show();
        CreateNode(plan, (String)textualPlan.remove(0), 0, -1);
        FindChilds(plan, 0, 1, textualPlan, 2);
        SwapSORTChilds(plan);
        return plan;
    }
    
    int CreateNode(Plan plan, String str, int id, int parentid) {
        
        if(id==1)
            leafid=-1;
        Node node = new Node();
        if(str.indexOf("->")>=0)
            str=str.substring(str.indexOf("->")+2).trim();
        String cost = str.substring(str.indexOf("..") + 2, str.indexOf("rows") - 1);
        String card = str.substring(str.indexOf("rows") + 5, str.indexOf("width")-1);
        if(str.indexOf(" on ") != -1 ||str.startsWith("Subquery Scan")) {
            node.setId(id++);
            node.setParentId(parentid);
            node.setCost(Double.parseDouble(cost));
            node.setCard(Double.parseDouble(card));
            if(str.startsWith("Index Scan"))
                node.setName("Index Scan");
            else if(str.startsWith("Subquery Scan"))
                node.setName("Subquery Scan");
            else
                node.setName(str.substring(0,str.indexOf(" on ")).trim());
            plan.setNode(node,plan.getSize());
            node = new Node();
            node.setId(leafid--);
            node.setParentId(id-1);
            node.setCost(0.0);
            node.setCard(0.0);
            if(str.startsWith("Subquery Scan"))
                node.setName(str.trim().substring("Subquery Scan".length(),str.indexOf("(")).trim());
            else
                node.setName(str.substring(str.indexOf(" on ")+3,str.indexOf("(")).trim());
            plan.setNode(node,plan.getSize());
        } else {
            node.setId(id++);
            node.setParentId(parentid);
            node.setCost(Double.parseDouble(cost));
            node.setCard(Double.parseDouble(card));
            node.setName(str.substring(0,str.indexOf("(")).trim());
            plan.setNode(node,plan.getSize());
        }
        return id;
    }
    int FindChilds(Plan plan, int parentid, int id, Vector text, int childindex) {
        String str ="";
        int oldchildindex=-5;
        while(text.size()>0) {
            int stindex;
            str = (String)text.remove(0);
            if(str.trim().startsWith("InitPlan"))
                stindex=str.indexOf("InitPlan");
            else if(str.trim().startsWith("SubPlan"))
                stindex=str.indexOf("SubPlan");
            else
                stindex=str.indexOf("->");
            
            if(stindex==-1)
                continue;
            if(stindex==oldchildindex) {
                childindex=oldchildindex;
                oldchildindex=-5;
            }
            if(stindex < childindex) {
                text.add(0,str);
                break;
            }
            
            
            if(stindex>childindex) {
                if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
                    str = (String)text.remove(0);
                    stindex=str.indexOf("->");
                    oldchildindex=childindex;
                    childindex=str.indexOf("->");
                }
                text.add(0,str);
                id = FindChilds(plan, id-1, id, text, stindex);
                continue;
            }
            
            if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
                str = (String)text.remove(0);
                stindex=str.indexOf("->");
                oldchildindex=childindex;
                childindex=str.indexOf("->");
            }
            
            
            
            if(stindex==childindex)
                id = CreateNode(plan,str, id, parentid);
        }
        return id;
    }
    
    void SwapSORTChilds(Plan plan) {
        for(int i =0;i<plan.getSize();i++) {
            Node node = plan.getNode(i);
            if(node.getName().equals("Sort")) {
                int k=0;
                Node[] chnodes = new Node[2];
                for(int j=0;j<plan.getSize();j++) {
                    if(plan.getNode(j).getParentId() == node.getId()) {
                        if(k==0)chnodes[0]=plan.getNode(j);
                        else chnodes[1]=plan.getNode(j);
                        k++;
                    }
                }
                if(k>=2) {
                    k=chnodes[0].getId();
                    chnodes[0].setId(chnodes[1].getId());
                    chnodes[1].setId(k);
                    
                    for(int j=0;j<plan.getSize();j++) {
                        if(plan.getNode(j).getParentId() == chnodes[0].getId())
                            plan.getNode(j).setParentId(chnodes[1].getId());
                        else if(plan.getNode(j).getParentId() == chnodes[1].getId())
                            plan.getNode(j).setParentId(chnodes[0].getId());
                    }
                }
            }
        }
    }
}

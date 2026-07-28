
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

package iisc.dsl.picasso.server.db.mssql;

import iisc.dsl.picasso.common.PicassoConstants;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;

//import com.sun.xml.internal.bind.v2.runtime.reflect.ListIterator;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.query.Query;

public class MSSQLDatabase extends Database
{
        Query query;
	public MSSQLDatabase(DBSettings settings) throws PicassoException
	{
		super(settings);
	}
	
	protected void createPicassoHistogram(Statement stmt) throws SQLException {
    }

	public boolean connect(DBSettings settings)throws PicassoException
	{
		if(isConnected())
			return true;
		this.settings = settings;
		try {
//			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
			String url= "jdbc:sqlserver://"+settings.getServerName()+":"+
				settings.getServerPort()+";databasename="+settings.getDbName();
			con = DriverManager.getConnection(url, settings.getUserName(),	settings.getPassword());
		}
		catch(Exception e) {
            System.err.println("Database: " + e);
            System.out.println("Database: " + e);	
            throw new PicassoException("Database Engine "+settings.getInstanceName()+" is not accepting connections");
			//return false;
		}
		if (con != null)
			return true;
		return false;
	}

	public Histogram getHistogram(String tabName, String schema, String attribName) throws PicassoException
	{
		return new MSSQLHistogram(this, tabName, schema, attribName);
	}

	// MsSql server doesn't have plantables
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
	protected void createQTIDMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoQTIDMap ( QTID int NOT NULL , QTEMPLATE text, " +
				"QTNAME varchar(" + PicassoConstants.QTNAME_LENGTH + ") UNIQUE NOT NULL, RESOLUTION int, DIMENSION int,  EXECTYPE varchar(" + PicassoConstants.SMALL_COLUMN + "), DISTRIBUTION varchar(" + PicassoConstants.SMALL_COLUMN + "), " +
				"OPTLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), PLANDIFFLEVEL varchar(" + PicassoConstants.SMALL_COLUMN + "), GENTIME bigint, GENDURATION bigint, PRIMARY KEY (QTID))");
	}
	protected void createPlanTree(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTree ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, PARENTID int NOT NULL, "+
				"NAME varchar(" + PicassoConstants.SMALL_COLUMN + "), COST float, CARD float, PRIMARY KEY(QTID,PLANNO,ID,PARENTID), " +
				"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	protected void createRangeResMap(Statement stmt) throws SQLException //-ma
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoRangeResMap ( QTID int NOT NULL, DIMNUM int NOT NULL, RESOLUTION int NOT NULL, "+
				"STARTPOINT float NOT NULL, ENDPOINT float NOT NULL,PRIMARY KEY(QTID,DIMNUM), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE)");
	}
	
	// There is no sub operator level stuff so far
	protected void createPlanTreeArgs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanTreeArgs ( QTID int NOT NULL, PLANNO int NOT NULL, ID int NOT NULL, "+
			"ARGNAME varchar(" + PicassoConstants.SMALL_COLUMN + ") NOT NULL, ARGVALUE varchar(max) NOT NULL, PRIMARY KEY(QTID,PLANNO,ID,ARGNAME), " +
			"FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createXMLPlan(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoXMLPlan ( QTID int NOT NULL, PLANNO int NOT NULL, XMLPLAN text, PRIMARY KEY(QTID,PLANNO), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) " +
				"ON DELETE CASCADE )");
	}
	protected void createPlanStore(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoPlanStore ( QTID int NOT NULL, QID int NOT NULL, PLANNO int, COST float, CARD float, " +
				"RUNCOST float, RUNCARD float, PRIMARY KEY(QTID,QID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) " +
				"ON DELETE CASCADE )");
	}
	protected void createSelectivityMap(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityMap ( QTID int NOT NULL, QID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
		"PRIMARY KEY(QTID,QID,DIMENSION), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	protected void createSelectivityLog(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoSelectivityLog ( QTID int NOT NULL, DIMENSION int NOT NULL, SID int NOT NULL, " +
				"PICSEL float, PLANSEL float, PREDSEL float, DATASEL float, CONST varchar("+PicassoConstants.LARGE_COLUMN+"), " +
				"PRIMARY KEY(QTID,DIMENSION,SID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}

	protected void createApproxSpecs(Statement stmt) throws SQLException
	{
		stmt.executeUpdate("create table "+settings.getSchema()+".PicassoApproxMap ( QTID int NOT NULL,  " +
				"SAMPLESIZE float, SAMPLINGMODE int, AREAERROR float, IDENTITYERROR float, FPCMODE bit " +
				"PRIMARY KEY(QTID), FOREIGN KEY(QTID) REFERENCES PicassoQTIDMap(QTID) ON DELETE CASCADE )");
	}
	
	private String processObjectName(String objectName)
	{
		StringTokenizer st = null;
		if(objectName!=null) {
			st = new StringTokenizer(objectName,", ");
			if(st.hasMoreTokens())
				objectName=st.nextToken();
			else
				objectName = "";
		}
		else
			objectName = "";
		//Removing unwanted parameters in object_name ie) [master].[dbo]
		if(objectName.startsWith("OBJECT"))	{
			//  Format of a typical object
			//	OBJECT:([master].[dbo].[NATION].[PK__NATION__4BB72C21])
			//	OBJECT:([tpch].[dbo].[NATION].[PK__NATION__4BB72C21])
			int index = objectName.indexOf('[');
			index = objectName.indexOf('[',index+1);
			index = objectName.indexOf('[',index+1);
			objectName = objectName.substring(index+1,objectName.length());
			index = objectName.indexOf(']');
			objectName = objectName.substring(0,index);
		}
		else
			objectName = "";
		return objectName;
	}
        private boolean isPicassoPredicate(String s)
        {
            if (query == null)
                return true;
            for(int i=0;i<query.getDimension();i++)
            {
                String tmp ="["+query.getAttribName(i).toUpperCase()+"]";
                if(tmp.equalsIgnoreCase(s))return true;
            }
            return false;
        }
        private String removeBrackets(String str1)
	{
                int oitmp=0,itmp=0,jtmp;
                String str = str1;
                while(true)
                {
                    oitmp=itmp;
                    itmp = str.indexOf("<=",itmp+2);
                    if(itmp==-1)
                        break;
                    jtmp=itmp;
                    while(jtmp>0 && str.charAt(jtmp) != ' ' && str.charAt(jtmp) != '[')jtmp--;
                    //while(str.charAt(jtmp--) != '[');
                    //jtmp++;
                    String tmp = str.substring(jtmp,itmp);
                    if(isPicassoPredicate(tmp))
                    {
                        boolean str_date_check=false;
                        int k = str.indexOf(" ",itmp+2);
                        int k1 = str.indexOf(")",itmp+2);
                        if ((k1 != -1 && k1<k)||k==-1)k=k1;
                        tmp = str.substring(k,str.length());
                        if(str.substring(itmp+1,k).indexOf("'")!=-1) tmp = tmp.substring(tmp.indexOf("'")+1,tmp.length());;
                        str = str.substring(0,itmp)+":VARIES"+tmp;
                    }
                }
                return str;
	}
        /*private String removeBrackets(String str)
	{
		String ret="";
		while(str.indexOf("<=")>=0){
			ret += str.substring(0,str.indexOf('('));
			str = str.substring(str.indexOf('('),str.length());
			int counter=0,i=0;
			for(i=0;i<str.length();i++){
				if(str.charAt(i)=='(')
					counter++;
				if(str.charAt(i)==')')
					counter--;
				if(counter==0)
					break;
			}
			str=str.substring(i+1);
		}
		return ret+str;
	}*/

	private void addArgument(Node node, String argument)
	{
		String type,value;
		if(argument!=null) {
                    String sp[] = argument.split (",[ ]+");
                    int i = 0;
                    while(i < sp.length ) {
	        	String tmp = sp[i];
                        i++;
	        	if(tmp.indexOf(':')<0)
	        		continue;
	        	type = tmp.substring(0,tmp.indexOf(':'));
				value = tmp.substring(tmp.indexOf(':')+1,tmp.length());
                                while(i<sp.length && sp[i].indexOf(':')<0)
                                {
                                    value = value+sp[i];i++;
                                }
				if(type.equals("WHERE"))
				{
					value = removeBrackets(value);
                    // System.out.println("WHERE value: "+value);
				}
				else if(type.equals("DEFINE"))
				{
					value = ""; //(for now)
				}
				node.addArgType(type);
				node.addArgValue(value);
	        }
		}
	}
	public Plan getPlan(String query) throws PicassoException
	{
		
		Plan plan = new Plan();
		Node node;
		int curNode=0;
		String objectName, argument;
		try{
			Statement stmt = createStatement ();
			stmt.execute("DBCC FREEPROCCACHE");
			stmt.execute("set showplan_all on");
			ResultSet rset=stmt.executeQuery(query);
			while(rset.next()){
				node = new Node();

//				Format:
//				1) StmtText, 2) StmtId, 3) NodeId, 4) Parent, 5) PhysicalOp, 6) LogicalOp, 7) Argument, 8) DefinedValues, + 10 more columns
//				StmtText is split into columns 5,6,7.
//				StmtId is 1 (or whatever set, I think).
//				PhysicalOp -> NULL for top, else Hash Match, etc.
//				LogicalOp -> Same as PhysicalOp or Inner Join etc.
//				Argument -> Key:Value
//				DefinedValues -> Lots of column names or null.
//				Column 16 is Type which is either SELECT or PLAN_ROW
				
				node.setId(rset.getInt(3));
				node.setParentId(rset.getInt(4));
				node.setName(rset.getString(5));
				if(node.getName().startsWith("Hash Match") )
					if(rset.getString(6).equals("Aggregate") || rset.getString(6).equals("Partial Aggregate"))
						node.setName(rset.getString(5)+" - "+rset.getString(6));
				if((rset.getString(16)).equals("SELECT"))
					node.setName("SELECT STATEMENT");
				node.setCost(rset.getDouble(13));
				node.setCard(rset.getDouble("EstimateRows"));
				argument = rset.getString(7);
				if(rset.getString(6)!=null){
				    node.addArgType("Logical-Op");
				    node.addArgValue(rset.getString(6));
				}
                		addArgument(node,argument);
				objectName = processObjectName(rset.getString(7));
				plan.setNode(node,curNode);
				curNode++;
				if(!objectName.equals("")){
					node = new Node();
					node.setId(-1);
					node.setParentId(rset.getInt(3));
					node.setName(objectName);
					node.setCost(rset.getDouble(13));
					node.setCard(rset.getDouble("EstimateRows"));
					plan.setNode(node,curNode);
					curNode++;
					
				}
				
			}
			rset.close();
			stmt.execute("set showplan_all off");
			Node tempNode;
			Vector planNodes = plan.getNodes();
			for(Iterator it = planNodes.iterator(); it.hasNext();) {
				tempNode = (Node)it.next();
				if(tempNode.getId()==-1) {
					MSSQLHistogram toGetCard = new MSSQLHistogram(this, tempNode.getName().trim(), settings.getSchema());
					int card2 = toGetCard.getCardinality();
					tempNode.setCard(card2);
					tempNode.setCost(0.0);
				}
			}
			stmt.close();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Database: Error accessing plan: "+e);
			throw new PicassoException("Database: Error explaining query: "+e);
		}
		return plan;
	}
        
       	public Plan getPlan(String query,Query q) throws PicassoException
	{
                this.query = q;
		return getPlan(query);

	}

	public Plan getPlan(String query,int startQueryNumber) throws PicassoException
	{
		return getPlan(query);
	}
	
	public String getAbsPlan(String qText)throws PicassoException 
	{
		String xPlan = null;
		ResultSet rset;
		try{
			Statement stmt = createStatement();
			stmt.executeUpdate("set showplan_xml on");
			rset = stmt.executeQuery(qText);
			rset.next();
			xPlan = rset.getString(1);
			//System.out.println(xPlan);
			rset.close();
			stmt.executeUpdate("set showplan_xml off");
			stmt.close();
			// The below code does some corrections of the XML plan - Sourjya
			// Every instance of single quote is converted to two single quotes in the XML plan
			StringBuilder sb = new StringBuilder(xPlan);
			int index = 0,nextIndex;
			while((nextIndex = sb.indexOf("&apos;", index)) != -1){
				if((sb.charAt(nextIndex+6) != '&')|(sb.charAt(nextIndex+7) != 'a')|
				(sb.charAt(nextIndex+8) != 'p')|(sb.charAt(nextIndex+9) != 'o')|
				(sb.charAt(nextIndex+10) != 's')|(sb.charAt(nextIndex+11) != ';')){
					sb.insert(nextIndex+6,"&apos;");
				}
				index = nextIndex + 12;
			}
			xPlan = sb.toString();
			// XML plan correction ends here - Sourjya
		}
		catch (Exception e){
			String errMsg = (e.getMessage());
			int index = errMsg.indexOf("showplan_xml");
			if (index < 0) 
				throw new PicassoException(e.getMessage());
			else throw new PicassoException("The Abstract Plan function is supported only for SQL Server 2005.");
		}
		return xPlan;
	}
}

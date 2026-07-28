
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

package iisc.dsl.picasso.server.db;

import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.db2.DB2Database;
import iisc.dsl.picasso.server.db.mssql.MSSQLDatabase;
import iisc.dsl.picasso.server.db.oracle.OracleDatabase;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;
import iisc.dsl.picasso.server.db.sybase.SybaseDatabase;
import iisc.dsl.picasso.server.db.informix.InformixDatabase;
import iisc.dsl.picasso.server.db.mysql.MysqlDatabase;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.query.Query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

/*
 * Instantiating a Database Object is an expensive operation since we check for the presence
 * of picasso related tables by running a select query on those tables ( Can we do better? )
 * Try to instantiate as minimal as possible
 */
public abstract class Database {
	protected Connection con;
	protected DBSettings settings;
	// Size of segment which is generated at a time

	 // return true on success
	public boolean connect() throws PicassoException
	{
		return connect(settings);
	}

	abstract public 	boolean 	connect(DBSettings settings) throws PicassoException;
	// acts as the builder of Histogram of correct type
	abstract public 	Histogram 	getHistogram(String tabname, String schema, String attrib) throws PicassoException;
	abstract public 	Plan 		getPlan(String query) throws PicassoException;
	public 	Plan 		getPlan(String query,Query q) throws PicassoException{return getPlan(query);}
	abstract public 	Plan 		getPlan(String query,int startQueryNumber) throws PicassoException;
	abstract public 	void 		emptyPlanTable() throws PicassoException;
	abstract public	void			removeFromPlanTable(int qno) throws PicassoException;
	abstract public 	boolean 		checkPlanTable();

	abstract protected void createPicassoColumns(Statement stmt) throws SQLException;
	abstract protected void createQTIDMap(Statement stmt) throws SQLException;
	abstract protected void createPlanTree(Statement stmt) throws SQLException;
	abstract protected void createPlanTreeArgs(Statement stmt) throws SQLException;
	abstract protected void createPlanStore(Statement stmt) throws SQLException;
	abstract protected void createSelectivityMap(Statement stmt) throws SQLException;
	abstract protected void createSelectivityLog(Statement stmt) throws SQLException;
	abstract protected void createXMLPlan(Statement stmt) throws SQLException;
	abstract protected void createApproxSpecs(Statement stmt) throws SQLException;
	abstract protected void createRangeResMap(Statement stmt) throws SQLException; //-ma
	abstract protected void createPicassoHistogram(Statement stmt) throws
	SQLException;
	
	public Database(DBSettings settings) throws PicassoException
	{
		this.settings = settings;
		connect(settings);
	}

	public String getSchema()
	{
		return settings.getSchema();
	}

	public boolean isConnected()
	{
		return con != null;
	}
	 //return true on success
	public boolean reConnect() throws PicassoException
	{
		if(isConnected())
			if(!close())
				return false;
		return connect();
	}

	public DBSettings getSettings()
	{
		return settings;
	}

	public boolean commit() {
		try
		{
			con.commit();
		}
		catch(SQLException e)
		{
			ServerMessageUtil.SPrintToConsole("Database: "+e);
			return false;
		}
		return true;
	}

	public boolean close() {
		if (isConnected() == false)
			return true;
		try
		{
			con.close ();
		}
		catch(SQLException e) 
		{
			ServerMessageUtil.SPrintToConsole("Database: "+e);
			con = null;
			return false;
		}
		con = null;
		return true;
	}

	public Statement createStatement() throws SQLException
	{
		return con.createStatement();
	}

	public PreparedStatement prepareStatement(String temp) throws SQLException
	{
		return con.prepareStatement(temp);
	}
	
	/*
	 * The following functions which deals with PicassoCommon objects cannot be put in those
	 * classes since we PicassoCommon is stand alone and doesn't depend on either PicassoServer
	 * or PicassoClient. Therefore this is the best place to put these functions.
	 */
	public int addQueryPacket(QueryPacket qp) throws PicassoException
	{
		int qtid=-1;
		int tempres = -1;
		boolean flag = false;

		try
		{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery("select max(QTID) from PicassoQTIDMap");
			if(rs.next())
				qtid = rs.getInt(1);
			rs.close();
			qtid++;
			if(!(this instanceof InformixDatabase))
			{
				
				// added to provide backward compatibility for the schema
				for(int i = 0; i < qp.getDimension() - 1; i++)
				{
					if(qp.getResolution(i) != qp.getResolution(i+1))
						flag = true; // true => heterogeneous resolutions
					int sp = new Double(qp.getStartPoint(i) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(i) * 1000000).intValue();
					if(((sp) != 0) || ((ep) != 1000000))
						flag = true;
					if(flag)
						break;
				}
				if(qp.getDimension() == 1)
				{
					int sp = new Double(qp.getStartPoint(0) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(0) * 1000000).intValue();
					if(sp != 0 || ep != 1000000)
						flag = true;
				}
				if(!flag)
					tempres = qp.getResolution(0);
				
				stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, " +
					"RESOLUTION, DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '"+
					escapeQuotes(qp.getQueryTemplate())+"','"+qp.getQueryName()+"',"+tempres+","+qp.getDimension()+
					",'"+PicassoConstants.COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
					"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");
				/*
				 * Added the following query to add the resolution values into the RangeResMap table -ma 
				 */
				if(tempres == -1)
					for(int i = 0; i < qp.getDimension(); i++)
						stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoRangeResMap (QTID, DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT) " +
							"values ("+qtid+","+i+","+qp.getResolution(i)+","+qp.getStartPoint(i)+","+qp.getEndPoint(i)+")");
			} // end not instance of Informix
			else // if Informix
			{
				String xyz = qp.getQueryTemplate().replace('\n', ' ');

				// added to provide backward compatibility for the schema
				for(int i = 0; i < qp.getDimension() - 1; i++)
				{
					if(qp.getResolution(i) != qp.getResolution(i+1))
						flag = true; // true => heterogeneous resolutions
					int sp = new Double(qp.getStartPoint(i) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(i) * 1000000).intValue();
					if(((sp) != 0) || ((ep) != 1000000))
						flag = true;
					if(flag)
						break;
				}
				if(!flag)
					tempres = qp.getResolution(0);
				
				stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, " +
						"RESOLUTION, DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '" +
						escapeQuotes(xyz)+"','"+qp.getQueryName()+"',"+tempres+","+qp.getDimension()+
						",'"+PicassoConstants.COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
						"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");
				/*
				 * Added the following query to add the resolution values into the RangeResMap table -ma 
				 */
				if(tempres == -1)
					for(int i = 0; i < qp.getDimension(); i++)
						stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoRangeResMap (QTID, DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT) " +
						"values ("+qtid+","+i+","+qp.getResolution(i)+","+qp.getStartPoint(i)+","+qp.getEndPoint(i)+")");
				// end change
			}
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error adding QTIDMap Entry:"+e);
			throw new PicassoException("Error adding QTIDMap Entry:"+e);
		}
		return qtid;
	}
	
	public int addApproxQueryPacket(QueryPacket qp) throws PicassoException
	{
		int qtid=-1;
		int tempres = -1;
		boolean flag = false;
		try
		{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery("select max(QTID) from PicassoQTIDMap");
			if(rs.next())
				qtid = rs.getInt(1);
			rs.close();
			qtid++;
			if(!(this instanceof InformixDatabase))
			{
				
				// added to provide backward compatibility for the schema
				for(int i = 0; i < qp.getDimension() - 1; i++)
				{
					if(qp.getResolution(i) != qp.getResolution(i+1))
						flag = true; // true => heterogeneous resolutions
					int sp = new Double(qp.getStartPoint(i) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(i) * 1000000).intValue();
					if(((sp) != 0) || ((ep) != 1000000))
						flag = true;
					if(flag)
						break;
				}
				if(qp.getDimension() == 1)
				{
					int sp = new Double(qp.getStartPoint(0) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(0) * 1000000).intValue();
					if(sp != 0 || ep != 1000000)
						flag = true;
				}
				if(!flag)
					tempres = qp.getResolution(0);
				/*System.out.println("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, " +
					"RESOLUTION, DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '"+
					escapeQuotes(qp.getQueryTemplate())+"','"+qp.getQueryName()+"',"+tempres+","+qp.getDimension()+
					",'"+PicassoConstants.APPROX_COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
					"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");*/
				stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, " +
					"RESOLUTION, DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '"+
					escapeQuotes(qp.getQueryTemplate())+"','"+qp.getQueryName()+"',"+tempres+","+qp.getDimension()+
					",'"+PicassoConstants.APPROX_COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
					"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");
				/*
				 * Added the following query to add the resolution values into the RangeResMap table -ma 
				 */
				if(tempres == -1)
					for(int i = 0; i < qp.getDimension(); i++)
						stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoRangeResMap (QTID, DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT) " +
							"values ("+qtid+","+i+","+qp.getResolution(i)+","+qp.getStartPoint(i)+","+qp.getEndPoint(i)+")");
			} // end not instance of Informix
			else // if Informix
			{
				String xyz = qp.getQueryTemplate().replace('\n', ' ');

				// added to provide backward compatibility for the schema
				for(int i = 0; i < qp.getDimension() - 1; i++)
				{
					if(qp.getResolution(i) != qp.getResolution(i+1))
						flag = true; // true => heterogeneous resolutions
					int sp = new Double(qp.getStartPoint(i) * 1000000).intValue();
					int ep = new Double(qp.getEndPoint(i) * 1000000).intValue();
					if(((sp) != 0) || ((ep) != 1000000))
						flag = true;
					if(flag)
						break;
				}
				if(!flag)
					tempres = qp.getResolution(0);
				
				stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, " +
						"RESOLUTION, DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '" +
						escapeQuotes(xyz)+"','"+qp.getQueryName()+"',"+tempres+","+qp.getDimension()+
						",'"+PicassoConstants.APPROX_COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
						"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");
				/*
				 * Added the following query to add the resolution values into the RangeResMap table -ma 
				 */
				if(tempres == -1)
					for(int i = 0; i < qp.getDimension(); i++)
						stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoRangeResMap (QTID, DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT) " +
						"values ("+qtid+","+i+","+qp.getResolution(i)+","+qp.getStartPoint(i)+","+qp.getEndPoint(i)+")");
				// end change
			}
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error adding QTIDMap Entry :"+e);
			throw new PicassoException("Error adding QTIDMap Entry:"+e);
		}
		return qtid;
	}
/*	public int addApproxQueryPacket2(QueryPacket qp) throws PicassoException //ADG
	{
		int qtid=-1;
		try
		{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery("select max(QTID) from PicassoQTIDMap");
			if(rs.next())
				qtid = rs.getInt(1);
			rs.close();
			qtid++;
			stmt.executeUpdate("insert into "+settings.getSchema()+".PicassoQTIDMap (QTID, QTEMPLATE, QTNAME, RESOLUTION, " +
				"DIMENSION, EXECTYPE, DISTRIBUTION, OPTLEVEL, PLANDIFFLEVEL, GENTIME, GENDURATION) values ("+qtid+", '"+
				escapeQuotes(qp.getQueryTemplate())+"','"+qp.getQueryName()+"',"+qp.getResolution()+","+qp.getDimension()+
				",'"+PicassoConstants.APPROX_COMPILETIME_DIAGRAM+"','"+qp.getDistribution()+"','"+qp.getOptLevel()+"','"+qp.getPlanDiffLevel()+
				"',"+qp.getGenTime()+", "+qp.getGenDuration()+")");
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error adding QTIDMap Entry:"+e);
			throw new PicassoException("Error adding QTIDMap Entry:"+e);
		}
		return qtid;
	}
*/	private String escapeQuotes(String str)
	{
		return str.replaceAll("'","''");
	}

	public QueryPacket getQueryPacket(String queryName) throws PicassoException
	{
		QueryPacket qp = null;
		int qtid, dim, res;
		double startpt, endpt;
		
		try
		{
			Statement stmt = createStatement();
			Statement stmt1 = createStatement();
			
			ResultSet rs = stmt.executeQuery(
				"select QTEMPLATE,QTNAME,RESOLUTION,DIMENSION,EXECTYPE,DISTRIBUTION,OPTLEVEL,PLANDIFFLEVEL,GENTIME,GENDURATION,QTID "+
				"from "+settings.getSchema()+".PicassoQTIDMap where QTNAME='"+queryName+"'");
			if(rs.next()){
				qp = new QueryPacket();
				qp.setQueryTemplate(rs.getString(1));
				qp.setQueryName(rs.getString(2));
				// qp.setResolution(rs.getInt(3));
				// if resolution value in QTIDMap is valid, set the same res. on all dimensions
				if(rs.getInt(3) != -1)
				{
					for(int i = 0; i < rs.getInt(4); i++)
					{
						qp.setResolution(rs.getInt(3), i);
						qp.setStartPoint(0.0, i);
						qp.setEndPoint(1.0, i);
					}
						
				}
				/*
				 * Added the following query to get the resolution values for a query packet -ma
				 */
				else
				{
					qtid = rs.getInt(11);
					ResultSet rs1 = stmt1.executeQuery(
							"select DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT from "+ settings.getSchema()+".PicassoRangeResMap where QTID="+qtid);
					while(rs1.next())
					{
						dim = rs1.getInt(1);
						res = rs1.getInt(2);
						startpt = rs1.getDouble(3);
						endpt = rs1.getDouble(4);
						qp.setResolution(res, dim);
						qp.setStartPoint(startpt, dim);
						qp.setEndPoint(endpt, dim);
					}
					// end change -ma
				}				
				qp.setDimension(rs.getInt(4));
				qp.setExecType(rs.getString(5));
				qp.setDistribution(rs.getString(6));
				qp.setOptLevel(rs.getString(7));
				qp.setPlanDiffLevel(rs.getString(8));
				qp.setGenTime(rs.getLong(9));
				qp.setGenDuration(rs.getLong(10));
			}
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot read PicassoQTIDMap entries:"+e);
			throw new PicassoException("Cannot read query names:"+e);
		}
		return qp;
	}

	public Vector getAllQueryPackets() throws PicassoException
	{
		Vector queryInfo = new Vector();
		QueryPacket qp;
		long duration;
		try{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(
				"select QTEMPLATE,QTNAME,RESOLUTION,DIMENSION,EXECTYPE,DISTRIBUTION,OPTLEVEL,PLANDIFFLEVEL,GENTIME,GENDURATION,QTID "+
				"from "+settings.getSchema()+".PicassoQTIDMap ORDER BY QTID DESC");
			Statement stmt1 = createStatement();
			int qtid;
			int dim;
			int res;
			double startpt, endpt;
			
			while(rs.next()){
				qp = new QueryPacket();
				qp.setQueryTemplate(rs.getString(1));
				qp.setQueryName(rs.getString(2));
				// qp.setResolution(rs.getInt(3));
				// if resolution value in QTIDMap is valid, set the same res. on all dimensions
				if(rs.getInt(3) != -1)
				{
					for(int i = 0; i < rs.getInt(4); i++)
					{
						qp.setResolution(rs.getInt(3), i);
						qp.setStartPoint(0.0, i);
						qp.setEndPoint(1.0, i);
					}
				}				
				/*
				 * Added the following query to get the resolution values for a query packet
				 */
				else
				{
					qtid = rs.getInt(11);
					ResultSet rs1 = stmt1.executeQuery(
							"select DIMNUM, RESOLUTION, STARTPOINT, ENDPOINT from "+ settings.getSchema()+".PicassoRangeResMap where QTID="+qtid);
					while(rs1.next())
					{
						dim = rs1.getInt(1);
						res = rs1.getInt(2);
						startpt = rs1.getDouble(3);
						endpt = rs1.getDouble(4);
						qp.setResolution(res, dim);
						qp.setStartPoint(startpt, dim);
						qp.setEndPoint(endpt, dim);
					}
					// end change -ma
				}
				qp.setDimension(rs.getInt(4));
				qp.setExecType(rs.getString(5));
				qp.setDistribution(rs.getString(6));
				qp.setOptLevel(rs.getString(7));
				qp.setPlanDiffLevel(rs.getString(8));
				qp.setGenTime(rs.getLong(9));
				duration = rs.getLong(10);
				/*if(duration==0)
					continue;*/
				qp.setGenDuration(duration);
				queryInfo.add(qp);
			}
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot read PicassoQTIDMap entries:"+e);
			throw new PicassoException("Cannot read PicassoQTIDMap entries:"+e);
		}
		return queryInfo;
	}

	public int getQTID(String queryName) throws PicassoException
	{
		int qtid = -1;
		try{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(
					"select QTID from "+settings.getSchema()+".PicassoQTIDMap where QTNAME='"+queryName+"'");
			if(rs.next()){
				qtid = rs.getInt(1);
			}
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot read PicassoQTIDMap entry:"+e);
			throw new PicassoException("Cannot read PicassoQTIDMap entry:"+e);
		}
		return qtid;
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
				if(!(this instanceof MysqlDatabase))
					stmt.executeUpdate("delete from "+getSchema()+".PicassoXMLPlan where QTID="+qtid); //-ma
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
	
	public void renamePicassoDiagram(String old, String name) throws PicassoException
	{
		try
		{
			int qtid = getQTID(old);
			if(qtid>=0)
			{
				Statement stmt = createStatement();
				stmt.executeUpdate("update " + getSchema() + ".PicassoQTIDMap SET QTNAME = '" + name + "' WHERE QTNAME = '" + old + "'");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Renaming Picasso Diagram "+e);
			throw new PicassoException("Error Renaming Picasso Diagram "+e);
		}
	}
	
	public void  cleanUpTables() throws PicassoException
	{
		long duration;
		try{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(
					"select QTNAME,GENDURATION from "+settings.getSchema()+".PicassoQTIDMap");
			while(rs.next()){
				duration = rs.getLong(2);
				if(duration!=0)
					continue;
				deletePicassoDiagram(rs.getString(1));
			}
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot cleanup database:"+e);
			throw new PicassoException("Cannot cleanup database:"+e);
		}
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
			stmt.executeUpdate("drop table "+getSchema()+".PicassoXMLPlan"); //-ma
			stmt.executeUpdate("drop table "+getSchema()+".PicassoApproxMap");
			stmt.executeUpdate("drop table "+getSchema()+".PicassoQTIDMap");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error Dropping Picasso Tables"+e);
			throw new PicassoException("Error Dropping Picasso Tables"+e);
		}
	}

	public void checkPicassoTables() throws PicassoException
	{
		try{
			Statement stmt = createStatement ();
			
			if(checkTable(getSchema()+".PicassoQTIDMap") == false)
				createQTIDMap(stmt);
			if(checkTable(getSchema()+".picasso_columns") == false)
				createPicassoColumns(stmt);
			if(checkTable(getSchema()+".PicassoPlanStore") == false)
				createPlanStore(stmt);
			if(checkTable(getSchema()+".PicassoPlanTree") == false)
				createPlanTree(stmt);
			if(checkTable(getSchema()+".PicassoPlanTreeArgs") == false)
				createPlanTreeArgs(stmt);
			if(checkTable(getSchema()+".PicassoSelectivityMap")==false)
				createSelectivityMap(stmt);
			if(checkTable(getSchema()+".PicassoSelectivityLog") == false)
				createSelectivityLog(stmt);
			if(checkTable(getSchema()+".PicassoApproxMap") == false)
				createApproxSpecs(stmt);
			if(checkTable(getSchema()+".PicassoRangeResMap") == false)
			{
				createRangeResMap(stmt);
				ServerMessageUtil.SPrintToConsole("Creating Picasso Tables");
			}

			if(this instanceof MysqlDatabase && checkTable(getSchema()+".picassohistogram")==false)
					createPicassoHistogram(stmt);
			if(this instanceof MSSQLDatabase && checkTable("PicassoXMLPlan")==false)
				createXMLPlan(stmt);
			stmt.close();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Creating PicassoTables failed: "+e);
			throw new PicassoException("Creating PicassoTables failed: "+e);
		}
	}

	protected boolean checkTable(String tabName)
	{
		try {
			Statement stmt = createStatement ();
			ResultSet rset = stmt.executeQuery ("select count(*) from "+tabName); 
			// changed * to count(*). guess it will be more efficient
			if(rset.next())
				rset.close();
			stmt.close ();
		}catch(SQLException e){
			System.out.println("CheckTable:"+e);
			return false;
		}
		return true;
	}

	/*
	 * For supporting a new Database system add the new database name in the if.else ladder and
	 * instantiate the corresponding Database class.
	 */
	public static Database getDatabase(DBSettings settings) throws PicassoException
	{
		Database db = null;
		String vendor = settings.getDbVendor();
		if(vendor.equals(DBConstants.DB2))
			db = new DB2Database(settings);
		else if(vendor.equals(DBConstants.ORACLE))
			db = new OracleDatabase(settings);
		else if(vendor.equals(DBConstants.MSSQL))
			db = new MSSQLDatabase(settings);
		else if(vendor.equals(DBConstants.SYBASE))
			db = new SybaseDatabase(settings);
		else if(vendor.equals(DBConstants.POSTGRES))
			db = new PostgresDatabase(settings);
		 else if(vendor.equals(DBConstants.INFORMIX))
			db = new InformixDatabase(settings);
		else if(vendor.equals(DBConstants.MYSQL))
			db = new MysqlDatabase(settings);
		return db;
	}
}

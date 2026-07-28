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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;

public class MysqlHistogram extends Histogram {
	
	protected Datatype lowValue,highValue;
	
	MysqlHistogram(Database db, String tabName, String schema, String attribName)
		throws PicassoException
	{
		this.db = db;
		this.tabName = tabName;
		this.schema = schema;
		this.attribName = attribName;
		
		value = new Vector();
		freq = new Vector();
		cardinality=getCard(attribName);
		/*
		 * We need to get the dType first so that we can create correct
		 * Datatype object for lowValue, highValue and histogram values
		 */
		dType = getDatatype();
		if (!(dType.equals("integer") || dType.equals("real") || dType
				.equals("string") || dType.equals("date"))) {
			throw new PicassoException(
					"One of the datatypes of the attributes among the :varies predicates is not handled in Picasso currently.");
		}

		readHistogram();
	}
	
	public String getConstant(double selectivity)
	{
		long leftBoundary = 0 , rightBoundary = 0;
		double step = (selectivity * cardinality);
		int index = 0;
		ListIterator it = freq.listIterator();
		if(it.hasNext())
			leftBoundary = ((Integer)it.next()).intValue();
		// This is for frequency histogram where leftBoundary is not zero
		if(step < leftBoundary)
			return ((Datatype)value.get(0)).getStringValue();
		while( it.hasNext()){
			rightBoundary = ((Integer)it.next()).intValue();
			index++;
			if (step >= leftBoundary && step < rightBoundary) {
				double scale = ((double)(step - leftBoundary)) / (rightBoundary - leftBoundary);
				Datatype lbValue = (Datatype)value.get(index-1);
				Datatype rbValue = (Datatype)value.get(index);
				return lbValue.interpolate(rbValue,scale);
			}
			leftBoundary = rightBoundary;
		}
		return ((Datatype)value.lastElement()).getStringValue();
	}
	
	private String getDatatype()
	{
		String type=null;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery
			("select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME= '"
					+ tabName.toLowerCase()+ "' and  COLUMN_NAME='" 
					+ attribName.toUpperCase()+ "'");
			if (rset.next ()) 
				type = rset.getString (1).trim();
			rset.close();
			stmt.close ();
		}catch(SQLException e) {
			ServerMessageUtil.SPrintToConsole("getDatatype : "+e);
		}
		if (type.equals ("int")||type.equals ("bigint")||type.equals ("mediumint")||type.equals ("tinyint")||type.equals ("smallint"))    
			type = "integer";
		else if (type.equals ("float")||type.equals ("decimal")||type.equals ("real")||type.equals ("double precision")||type.equals ("numeric"))
			type = "real";
		else if (type.equals ("datetime")||type.equals ("date")||type.equals ("time")||type.equals ("timestamp"))	    
			type = "date";
		else if (type.equals ("char") || type.equals ("varchar")||type.equals("text")||type.equals ("blob")||type.equals ("enum")||type.equals ("set"))
			type = "string";
		else{
			ServerMessageUtil.SPrintToConsole("Unknown datatype="+type+" for table "+tabName.toLowerCase()+" attribute "+attribName.toUpperCase());
			type = "invalid";
		}
		return type;
	}
	/*
	 * We expect lowValue and dType to be set before readHistogram is called
	 */
	
	private void createAttributeHistogram(String colName, String tabName) throws PicassoException
	{
		try {
			String str = "create table " + this.schema + ".picasso_"+ tabName+"_"+colName + "_hist (endpoint_number integer PRIMARY KEY , endpoint_value varchar(300))";
			//System.out.println(str);
			Statement stmt = db.createStatement();
			stmt.executeUpdate(str);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public int getmaxEndPoint () throws PicassoException {
		int maxEndPoint = 0;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery
			("select max(endpoint_number)"
					+ " from " + this.schema + ".picasso_"+ tabName+"_"+attribName + "_hist");
			if (rset.next ())	{
				maxEndPoint = rset.getInt(1);
			}
			rset.close();
			stmt.close();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getHighValue: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
		return maxEndPoint;
	}
	private void readHistogram() throws PicassoException
	{
		Plan plan;
		try{
			
			String col;
			
			String str1 = "select ISHISTOGRAM from " +this.schema +" .picassohistogram where schemaname = " + "'" + this.schema + "'" + " and tablename =  " + 
			"'" + tabName + "'" + " and columnname = " + "'" + attribName + "'";
		//System.out.println(str);
		Statement stmt1 = db.createStatement ();
		ResultSet rset1 = stmt1.executeQuery(str1);
		
		if(!(rset1.next())){col = "false";}
		else {col = rset1.getString(1).toUpperCase(); }
			
		rset1.close();
		stmt1.close();
	
		double range=0;	
		Statement stmt2;
		
		if(!(col.equals("TRUE")))
		{
			createAttributeHistogram(attribName, tabName);
			str1 = "select count(" + attribName + ") from " + this.schema + "." + tabName;
			stmt1 = db.createStatement ();
			rset1 = stmt1.executeQuery(str1);
			
			long card = 0, depth = 0, endpt_no = 0, dist_card=0;
			String val = null; double frequency = 0, temp = 0;
			boolean flag = true;
			
			while(rset1.next())
				card = rset1.getLong(1);
		
			rset1.close();
			stmt1.close();
		
			str1 = "select distinct (count(" + attribName + ")) from " + this.schema + "." + tabName;
			stmt1 = db.createStatement ();
			rset1 = stmt1.executeQuery(str1);
			
			while(rset1.next())
				dist_card = rset1.getLong(1);
			
			rset1.close();
			stmt1.close();
			
			str1 = "select " + attribName + " , count(" + attribName + ") from " + this.schema + "." + tabName + " group by " + attribName;
			stmt1 = db.createStatement ();
			rset1 = stmt1.executeQuery(str1);
			
			if(dist_card <= PicassoConstants.HIST_BUCKETS){
				while(rset1.next()){
					val = rset1.getString(1);
					str1 = "insert into "+ this.schema + ".picasso_" + tabName+"_"+attribName + "_hist (endpoint_number, endpoint_value) values (" 
					+ (endpt_no) +"," + val +")";
					stmt2 = db.createStatement();
					stmt2.executeUpdate(str1);
					stmt2.close();
					endpt_no++;
				}
			}
			else
			{
				depth = card/PicassoConstants.HIST_BUCKETS;
			
			while(rset1.next()){
				
				frequency = rset1.getDouble(2);
				temp = temp + frequency;
				
				if(flag){
					val = rset1.getString(1);
					str1 = "insert into "+ this.schema + ".picasso_" + tabName+"_"+attribName + "_hist (endpoint_number, endpoint_value) values (" 
					+ (endpt_no) +",'" + val +"')";
					stmt2 = db.createStatement();
					stmt2.executeUpdate(str1);
					stmt2.close();
					
					flag = false;
				}
				
				if(temp > depth) {
					endpt_no ++ ;
					str1 = "insert into "+ this.schema + ".picasso_" + tabName+"_"+attribName + "_hist (endpoint_number, endpoint_value) values (" 
					+ (endpt_no) +",'" + val +"')";
					stmt2 = db.createStatement();
					stmt2.executeUpdate(str1);
					stmt2.close();
					temp = frequency;
					
				}
				val = rset1.getString(1);
			}
			str1 = "insert into "+ this.schema + ".picasso_" + tabName+"_"+attribName + "_hist (endpoint_number, endpoint_value) values (" 
			+ (endpt_no+1) +",'" + val +"')";
			stmt2 = db.createStatement();
			stmt2.executeUpdate(str1);
			stmt2.close();
			
			rset1.close();
			stmt1.close();
			}
		
			
			str1 = "insert into "+ this.schema + ".picassohistogram (SCHEMANAME,TABLENAME,COLUMNNAME,ISHISTOGRAM) values ('"
			+ this.schema +"','" + tabName + "','" + attribName + "','TRUE')";
			System.out.println(str1);
			stmt2 = db.createStatement();
			stmt2.executeUpdate(str1);
			stmt2.close();
			}
		
		
	
		int maxEndPoint = getmaxEndPoint();
		int valcount = 0;
		String colvalue = null;
			str1 = "select endpoint_number, endpoint_value from " + this.schema + ".picasso_"  + tabName+"_"+attribName + "_hist";
			stmt1 = db.createStatement ();
			rset1 = stmt1.executeQuery(str1);
			while (rset1.next ()) {
				if(dType.equals("real") || dType.equals("integer") || dType.equals("date"))
				{	colvalue = String.valueOf(rset1.getString(2));
					if(dType.equals("integer"))
					{
						colvalue=String.valueOf(rset1.getString(2));
					}
				
					if(dType.equals("date"))
					{
						
					}
				}
				else if(dType.equals("string"))
					colvalue = rset1.getString(2);
				valcount = (int)(((double)cardinality /maxEndPoint) * rset1.getFloat(1));
				//valcount = rset1.getInt(2);
				System.out.println("value :"+colvalue+"\tfreq :"+valcount);
				if(dType.equals("date"))
				{
					value.addElement(Datatype.makeObject(dType,colvalue));
				}
				else
				{
					value.addElement(Datatype.makeObject(dType,colvalue));
				}
				freq.addElement(new Integer(valcount));
			}
			if(value.size()<=0){
				rset1.close();
				stmt1.close();
				throw new PicassoException("Distribution statistics is not available for "+
						tabName+"."+attribName+"\nPlease build statistics.");
				/*value.addElement(lowValue);
				freq.addElement(new Integer(0));
				value.addElement(highValue);
				freq.addElement(new Integer(cardinality));*/
			}
			rset1.close();
			stmt1.close ();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
	
	}
	
	private int getCard(String attribName) throws PicassoException
	{

		int card = 0;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery("select count(" + attribName + ") from " + db.getSchema()+"." + tabName );
			if (rset.next ()) 
				card = (int)rset.getInt(1);
			rset.close();
			stmt.close ();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getCardinality: "+e);
			throw new PicassoException("Cannot get cardinality of "+
					tabName+": "+e);
		}
		return card;

	}
	public int getCard2(String query) throws PicassoException
	{

		int card = 0;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery(query);
			if (rset.next ()) 
				card = (int)rset.getInt(1);
			rset.close();
			stmt.close ();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getCardinality: "+e);
			throw new PicassoException("Cannot get cardinality of "+
					tabName+": "+e);
		}
		return card;

	}
}

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class OracleHistogram extends Histogram {
	
	protected Datatype lowValue,highValue;
	OracleHistogram(Database db, String tabName, String schema, String attribName)
		throws PicassoException
	{
		this.db = db;
		this.tabName = tabName;
		this.schema = schema;
		this.attribName = attribName;
		
		value = new Vector();
		freq = new Vector();
		
		cardinality = getCard();
		/*
		 * We need to get the dType first so that we can create correct
		 * Datatype object for lowValue, highValue and histogram values
		 */
		dType = getDatatype();
		if(dType.equals("date"))
			throw new PicassoException(
					"Date is not handled for :varies predicates in Oracle");
			
		if (!(dType.equals("integer") || dType.equals("real") || dType
				.equals("string") /*|| dType.equals("date")*/)) {
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
	
	private String getDatatype() throws PicassoException
	{
		String type=null;
		try{
			Statement stmt_type = db.createStatement ();
			ResultSet rset_type = stmt_type.executeQuery("select data_type,low_value,high_value from SYS.DBA_TAB_COLS where TABLE_NAME= '"
						+ tabName.toUpperCase () + "' and OWNER= '"
						+ schema.toUpperCase () + "' and  COLUMN_NAME='" 
						+ attribName.toUpperCase () + "'");
			
				if (rset_type.next ()){
				type = rset_type.getString (1).trim();
				//lowValue = Datatype.makeObject(type, rset_type.getString(2));
				//highValue = Datatype.makeObject(type, rset_type.getString(3));
			}
			rset_type.close();
			stmt_type.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getDatatype: "+e);
			throw new PicassoException("Cannot get datatype: "+e);
		}
		type=getDatatype(type);

		return type;
	}
	
	/*
	 * We expect dType to be set before readHistogram is called
	 */
	private void readHistogram() throws PicassoException
	{
		String colvalue = null;
		int valcount, maxEndPoint;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery("select endpoint_value, endpoint_actual_value, endpoint_number"
					+ " from all_tab_histograms where table_name= '"
					+ tabName.toUpperCase() + "' and owner= '"
					+ schema.toUpperCase() + "' and column_name='"
					+ attribName.toUpperCase() + "' order by endpoint_number");
			 
			maxEndPoint = getMaxEndPoint();
			//System.out.println("table :"+tabName +"\tcolumn :"+attribName+"\tcard :"+card+"\tmaxEndPoint :"+maxEndPoint);
			while (rset.next ()) {
				if(dType.equals("real") || dType.equals("integer") || dType.equals("date"))
				{	colvalue = String.valueOf(rset.getFloat(1));
					if(dType.equals("date"))
					{
						colvalue=String.valueOf((long)(Double.parseDouble(colvalue)*86400.00 + 0.000001));
					}
				}
				else if(dType.equals("string"))
					colvalue = rset.getString(2);
				valcount = (int)(((double)cardinality / maxEndPoint) * rset.getFloat(3));
				//System.out.println("value :"+colvalue+"\tfreq :"+valcount);
				if(dType.equals("date"))
				{
					value.addElement(Datatype.makeObject("integer",colvalue));
				}
				else
				{
					value.addElement(Datatype.makeObject(dType,colvalue));
				}
				freq.addElement(new Integer(valcount));
			}
			if(value.size()<=0){
				rset.close();
				stmt.close();
				throw new PicassoException("Distribution statistics is not available for "+
						tabName+"."+attribName+"\nPlease build statistics.");
				/*value.addElement(lowValue);
				freq.addElement(new Integer(0));
				value.addElement(highValue);
				freq.addElement(new Integer(cardinality));*/
			}
			rset.close();
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
	}

	private int getMaxEndPoint() throws PicassoException
	{
		int maxEndPoint = 0;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery
			("select max(endpoint_number)"
					+ "from all_tab_histograms where table_name= '"
					+ tabName.toUpperCase() + "' and owner= '"
					+ schema.toUpperCase() + "' and column_name='"
					+ attribName.toUpperCase() + "'");
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
	
	private int getCard() throws PicassoException
	{
		int card = 0;
		try{
			Statement stmt = db.createStatement();
			ResultSet rset = stmt.executeQuery
			("select num_rows from SYS.DBA_TABLES where TABLE_NAME= '"
					+ tabName.toUpperCase() + "' and OWNER= '" + schema.toUpperCase() + "'");
			if (rset.next ())
				card = rset.getInt(1);
			rset.close();
			stmt.close ();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getCardinality: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
		return card;
	}
}

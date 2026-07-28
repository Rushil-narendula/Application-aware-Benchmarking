
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

public class SybaseHistogram extends Histogram {
	SybaseHistogram(Database db, String tabName, String schema, String attribName)
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
	
	private String getDatatype() throws PicassoException
	{
		String type=null;
		try{
			Statement stmt_type = db.createStatement ();
			ResultSet rset_type=stmt_type.executeQuery("select st.name from systypes st,sysobjects so,syscolumns" +
					" sc where sc.id=so.id and sc.name='"+ attribName +"' and sc.type=st.type and " +
					"so.name='"+ tabName +"'");
			
			/*    ResultSet rset_type = stmt_type.executeQuery
			 ("select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME= '"
			 + cond[num].leftAttribute.relation+ "' and  COLUMN_NAME='" 
			 + cond[num].leftAttribute.name+ "'");
			 */
			if (rset_type.next ()) 
				type = rset_type.getString (1).trim();
			
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
		int card;
		
		try{
			Statement stmt = db.createStatement ();
			try {
				stmt.executeQuery("dump tran "+db.getSettings().getDbName()+" with no_log");
			} catch (SQLException e) {
				System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
			}
			ResultSet rset=stmt.executeQuery("hist_values '"+tabName+"','"+attribName +"'");
			int count=0;
			card = getCard();
			while (rset.next ()) 
			{
				count=(int)(card *  rset.getFloat(2) + count );
				freq.add(new Integer(count));
				if(dType.equals("date"))
				{
				//remove " quotes
				String myfulldatewoquotes = rset.getString(4).replaceAll("\"","");
				
				//change from MM/dd/yyyy to yyyy-MM-dd
				String myyear = myfulldatewoquotes.substring(6);
				String mymonth = myfulldatewoquotes.substring(0,2);
				String mydateofmonth = myfulldatewoquotes.substring(3,5);
				String mydate = myyear+"-"+mymonth+"-"+mydateofmonth;
				
				value.addElement(Datatype.makeObject(dType,mydate));
				}
				else
					value.addElement(Datatype.makeObject(dType,rset.getString(4)));
			}
			if(freq.size()<=0){
				rset.close();
				stmt.close();
				throw new PicassoException("Distribution statistics is not available for "+
						tabName+"."+attribName+"\nPlease build statistics.");
			}
			rset.close();
			stmt.close ();

		} catch(SQLException e) {
			System.out.println(e.getSQLState() + " Exception : " + e.getErrorCode());
			System.out.println(tabName + "," + attribName);
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram : "+e);
			throw new PicassoException("Cannot read histogram "+e);
		}
	}
	
	private int getCard() throws PicassoException
	{
		int card = 0;
		try{
			Statement stmt = db.createStatement();
			ResultSet rset = stmt.executeQuery
			("sp_spaceused " + tabName);
			if (rset.next ()) 
				card = rset.getInt (2);
			stmt.close ();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getCardinality : "+e);
			throw new PicassoException("Cannot read histogram "+e);
		}
		return card;
	}
}

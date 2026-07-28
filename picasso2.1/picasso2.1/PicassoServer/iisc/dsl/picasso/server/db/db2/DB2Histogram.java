
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

import java.util.ListIterator;
import java.util.Vector;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class DB2Histogram extends Histogram 
{
	protected Datatype lowValue,highValue;
	
	DB2Histogram(Database db, String tabName, String schema) throws PicassoException {
		this.db=db;
		this.tabName=tabName;
		this.schema=schema;
		
		cardinality = getCard();
	}
	
	DB2Histogram(Database db, String tabName, String schema, String attribName) throws PicassoException
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

		getEndValues();
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
			ResultSet rset_type = stmt_type.executeQuery
				("select coltype from SYSIBM.SYSCOLUMNS where TBNAME= '" +
						tabName.toUpperCase () + "'  and TBCREATOR= '" +
						schema.toUpperCase () + "' and NAME='" +
						attribName.toUpperCase () + "'");
			if (rset_type.next ())
				type = rset_type.getString (1).trim();
			rset_type.close();
			stmt_type.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getDatatype: "+e);
			throw new PicassoException("Cannot get datatype of attribute "+
					attribName+": "+e);
		}
		type=getDatatype(type);
		return type;
	}
	
	/*
	 * We expect lowValue and dType to be set before readHistogram is called
	 */
	private void readHistogram() throws PicassoException
	{
		String colvalue;
		int valcount;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery("select colvalue,VALCOUNT " + 
					"from  SYSIBM.SYSCOLDIST where TBNAME= '" 
					+ tabName.toUpperCase () + "' and SCHEMA= '"
					+ schema.toUpperCase () + "'and NAME='"
					+ attribName.toUpperCase () + "' and TYPE='Q' and valcount!=-1");
			
			/*
			 * DB2 doesn't have lowest value stored anywhere. So we make an approximation by choosing
			 * either low2key which gets stored in lowValue and the first bucket boundary and use it as
			 * the left boundary of the first bucket
			 */
			//System.out.println("table :"+tabName +"\tcolumn :"+attribName+"\tcard :"+cardinality);
			if(rset.next()){
				Datatype firstValue;
				colvalue =rset.getString(1).trim();
				if(dType.equals("date"))
				{
					colvalue= colvalue.replaceAll("\'", "");
				}
				firstValue = Datatype.makeObject(dType,colvalue);
				if(lowValue.isLessThan(firstValue))
					value.addElement(lowValue);
				else
					value.addElement(firstValue);
				freq.addElement(new Integer(0));
				value.addElement(firstValue);
				valcount = rset.getInt(2);
				freq.addElement(new Integer(valcount));
				//System.out.println("value :"+firstValue.getStringValue()+"\tfreq :"+valcount);
			} else {
				rset.close();
				stmt.close();
				throw new PicassoException("Distribution statistics is not available for "+
						tabName+"."+attribName+"\nPlease build statistics.");
				/*value.add(lowValue);
				freq.add(new Integer(0));
				value.add(highValue);
				freq.add(new Integer(cardinality));*/
			}
			while (rset.next ()) {
				colvalue = rset.getString(1).trim();
				valcount = rset.getInt(2);
				//remove the quotes
				if(dType.equals("date"))
				{
					colvalue= colvalue.replaceAll("\'", "");
				}
				value.addElement(Datatype.makeObject(dType,colvalue));
				freq.addElement(new Integer(valcount));
				//System.out.println("value :"+colvalue+"\tfreq :"+valcount);
			}
			rset.close();
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram: "+e);
			throw new PicassoException("Cannot read histogram for attribute "+
					attribName+" "+e);
		}
	}
	
	/*
	 * getEndValue expects dType to be set before invocation
	 */
	private void getEndValues() throws PicassoException
	{
		String lowValueStr = null;
		String highValueStr = null;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery
			("select low2key, high2key from SYSIBM.SYSCOLUMNS where TBNAME= '"
				+ tabName.toUpperCase () + "'  and TBCREATOR= '" +
				schema.toUpperCase () + "' and NAME='" +
				attribName.toUpperCase () + "'");
			if (rset.next ())	{
				lowValueStr = rset.getString(1).trim();
				highValueStr = rset.getString(2).trim();
				
				if(dType.equals("date"))
				{
					//remove quotes
					lowValueStr =lowValueStr.replaceAll("\'","");
					highValueStr =highValueStr.replaceAll("\'","");
				}
			}
			rset.close();
			stmt.close();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getLowValue: "+e);
			throw new PicassoException("Cannot read low2key from DB2 "+e);
		}
		lowValue = Datatype.makeObject(dType,lowValueStr);
		highValue = Datatype.makeObject(dType, highValueStr);
	}
	
	private int getCard() throws PicassoException
	{
		int card = 0;
		try{
			Statement stmt = db.createStatement();
			ResultSet rset = stmt.executeQuery
			("select card from SYSIBM.SYSTABLES where NAME= '" +
				tabName.toUpperCase() + "' and CREATOR='" + 
				schema.toUpperCase () + "'");
			if (rset.next ())
				card = rset.getInt (1);
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

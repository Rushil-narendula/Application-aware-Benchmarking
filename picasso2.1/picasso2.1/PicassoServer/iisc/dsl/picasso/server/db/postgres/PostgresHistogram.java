
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class PostgresHistogram extends Histogram
{
	protected Datatype lowValue,highValue;
	
	public PostgresHistogram(PostgresDatabase db, String tabName, String schema, String attribName)
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
		//System.out.println("Table="+tabName+"\tAttrib="+attribName);
		dType = getDatatype();
		if(dType.equals("string"))
			throw new PicassoException(
					"String is not handled for :varies predicates in Postgres");
		
		if (!(dType.equals("integer") || dType.equals("real") || /*dType
				.equals("string") ||*/ dType.equals("date"))) {
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
			ResultSet rset_type = stmt_type.executeQuery("select typname from pg_type, pg_attribute where"
					+ " attname ='" + attribName + "' and pg_attribute.atttypid = pg_type.oid "
					+ "and attrelid = (select oid from pg_class where relname = '" + 
					tabName + "');");
			if (rset_type.next ())
				type = rset_type.getString(1).trim().toLowerCase();
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
		String histStr, valStr, freqStr;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery("select histogram_bounds, most_common_vals, most_common_freqs from pg_stats " +
					"where tablename = '" + tabName + "' and attname = '" +
					attribName + "'");
			if(rset.next ()){
				histStr=rset.getString(1);
				valStr = rset.getString(2);
				freqStr = rset.getString(3);
			}
			else
				throw new PicassoException("Histogram does not exist for "+tabName+"."+attribName+
						"\nPlease build statistics.");
			rset.close();
			stmt.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram : "+e);
			throw new PicassoException("Cannot read histogram for attribute "+
					attribName+" "+e);
		}
		
		double sumMCV=0, step;
		Vector valsVector = null, freqsVector = null;
		if(valStr !=null && freqStr != null)
		{
			StringTokenizer valsToken = new StringTokenizer(valStr,"{,}");
			StringTokenizer freqsToken = new StringTokenizer(freqStr,"{,}");
			valsVector = new Vector();
			freqsVector = new Vector();
			while(valsToken.hasMoreElements())
				valsVector.addElement(Datatype.makeObject(dType,valsToken.nextToken()));
			while(freqsToken.hasMoreElements())
				freqsVector.addElement(new Double(freqsToken.nextToken()));
			if(valsVector.size()==0){
				throw new PicassoException("Distribution statistics is not available for "+
						tabName+"."+attribName+"\nPlease build statistics.");
			}
			ListIterator itf = freqsVector.listIterator();
			while(itf.hasNext())
				sumMCV += ((Double)itf.next()).doubleValue();
		}

                if(histStr==null)histStr=new String(valStr);
		StringTokenizer histToken = new StringTokenizer(histStr,"{,}");
		Vector tokenVector = new Vector();
		while(histToken.hasMoreElements())
			tokenVector.addElement(Datatype.makeObject(dType,histToken.nextToken()));
		step = (1 - sumMCV) / (tokenVector.size()-1);
		// setting the first value of histogram for a freq of 0
		ListIterator it = tokenVector.listIterator();
		Datatype prevValue=null, boundaryValue=null;
		if(it.hasNext()){
			prevValue = (Datatype)it.next();
			boundaryValue = prevValue;
			freq.addElement(new Integer((int)(0)));
			value.addElement(prevValue);
		}
		double cdf=0;
		while(it.hasNext()){
			Datatype tmp = (Datatype)it.next();
			Double mcvf=null;
			ListIterator itv = null, itf = null;
			if(valsVector != null && freqsVector != null){
				itv = valsVector.listIterator();
				itf = freqsVector.listIterator();
			}
			while(itv!=null && itf!=null && itv.hasNext() && itf.hasNext()){
				Datatype mcv = (Datatype)itv.next();
				mcvf = (Double)itf.next();
				if(mcv.isLessThan(tmp) || mcv.isEqual(tmp)){
					cdf+= step * (mcv.minus(prevValue)).divide(tmp.minus(boundaryValue))+mcvf.doubleValue();
					//System.out.println("Freq:value :"+mcv.getStringValue()+"\tfreq :"+(cdf*cardinality));
					prevValue = mcv;
					value.addElement(mcv);
					freq.addElement(new Integer((int)(cdf*cardinality)));
					valsVector.remove(mcv);
					freqsVector.remove(mcvf);
					if(valsVector != null && freqsVector != null){
						itv = valsVector.listIterator();
						itf = freqsVector.listIterator();
					}		
				}
			}
			//System.out.println("Unif:value :"+tmp.getStringValue()+"\tfreq :"+(cdf*cardinality));
			if(prevValue!=tmp)
				cdf+=step * (tmp.minus(prevValue)).divide(tmp.minus(boundaryValue));
			boundaryValue = prevValue = tmp;
			freq.addElement(new Integer((int)(cdf*cardinality)));
			value.addElement(tmp);
		}
	}
	
	private int getCard() throws PicassoException
	{
		int card = 0;
		try{
			Statement stmt = db.createStatement ();
			ResultSet rset = stmt.executeQuery("select reltuples from pg_class where relname = '" +
					tabName + "';");
			if (rset.next ()) 
				card = (int)rset.getFloat(1);
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


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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class MSSQLHistogram extends Histogram {
    private String statisticsName;
    protected Datatype lowValue,highValue;
    
    MSSQLHistogram(Database database, String tabName, String schema) throws PicassoException {
    	this.db = database;
    	this.tabName = tabName;
    	this.schema = schema;
    	value = new Vector();
        freq = new Vector();
    	cardinality = getCard();
    	//System.out.println("Inside MSSQLHistogram2: "+tabName+" "+cardinality);
    }
    
    MSSQLHistogram(Database database, String tabName, String schema, String attribName) throws PicassoException {
        this.db = database;
        this.tabName = tabName;
        this.schema = schema;
        this.attribName = attribName;
        
        value = new Vector();
        freq = new Vector();
        cardinality = getCard();
        statisticsName = getStatisticsName();
        if(statisticsName == null)
            throw new PicassoException("Error reading histogram");
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
    
    public String getConstant(double selectivity) {
        long leftBoundary = 0 , rightBoundary = 0;
        double step = (selectivity * cardinality);
        int index = 0;
        ListIterator it = freq.listIterator();
        if(it.hasNext())
            leftBoundary = ((Integer)it.next()).intValue();
        if (step < leftBoundary) {
            Datatype lbValue = (Datatype)value.get(0);
            return lbValue.getStringValue();
        }
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
    
    private String getDatatype() {
        String type=null;
        try{
            Statement stmt = db.createStatement();
            ResultSet rset = stmt.executeQuery
                    ("select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME= '"
                    + tabName+ "' and  COLUMN_NAME='"
                    + attribName+ "'");
            if (rset.next())
                type = rset.getString(1).trim();
            rset.close();
            stmt.close();
        }catch(SQLException e) {
            ServerMessageUtil.SPrintToConsole("getDatatype: "+e);
        }
        type=getDatatype(type);
        
        return type;
    }
    
    private void readHistogram() throws PicassoException {
        String colvalue;
        double valcount;
        double cdf=0.0;
        Vector tmp = new Vector();
        try{
            Statement stmt = db.createStatement();
            stmt.execute("DBCC SHOW_STATISTICS ("+tabName+", "+statisticsName+")");
            ResultSet rset = stmt.getResultSet();
            if(rset.next()){
                cardinality = rset.getInt("Rows");
            }else
                throw new PicassoException("Cannot read Histogram");
            stmt.getMoreResults();
            stmt.getResultSet();
            stmt.getMoreResults();
            rset = stmt.getResultSet();
            System.out.println("table:"+tabName +"\tcolumn:"+attribName+"\tcard:"+cardinality);
           
            while (rset.next()) {
            	 if (rset.getString(1)==null) {
         			throw new PicassoException("Histogram has null values. Null values are currently not supported.");
         			//break;
         		}
                colvalue = rset.getString(1).trim();
                cdf+=rset.getDouble(2)+rset.getDouble(3);
                valcount = cdf * cardinality;
                Datatype data=Datatype.makeObject(dType, colvalue);
                data.setMsSqlFlag(1);
                value.addElement(data);
                tmp.addElement(new Double(valcount));
                                /*cdf+=rset.getDouble(3);
                                valcount = cdf * cardinality;
                                value.addElement(Datatype.makeObject(dType,colvalue));
                                tmp.addElement(new Double(valcount));*/
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
            //System.out.println("CDF="+cdf);
            rset.close();
            stmt.close();
            ListIterator it = tmp.listIterator();
            ListIterator itv = value.listIterator();
            while(it.hasNext() && itv.hasNext()){
                Double d = (Double)it.next();
                freq.addElement(new Integer((int)(d.doubleValue()/cdf)));
                //System.out.println("value:"+((Datatype)itv.next()).getStringValue()+"\tfreq:"+((int)(d.doubleValue()/cdf)));
            }
        }catch(SQLException e) {
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("readHistogram: "+e);
        }
    }
    
    private String getStatisticsName() {
        String statsName = null;
        try{
            Statement stmt = db.createStatement();
            ResultSet rset=stmt.executeQuery("sp_helpstats "+tabName+" , 'ALL'");
            while(rset.next()){
                if(rset.getString("statistics_keys").equalsIgnoreCase(attribName)){
                    statsName=rset.getString(1);
                }
            }
            rset.close();
            stmt.close();
        }catch(SQLException e){
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("Error getting Statistics: "+e);
        }
        return statsName;
    }
    
    private int getCard() {
        int card = 0;
        try{
            Statement stmt = db.createStatement();
            ResultSet rset = stmt.executeQuery("sp_spaceused " +tabName.toUpperCase());
            if (rset.next())
                card = rset.getInt(2);
            rset.close();
            stmt.close();
        }catch(SQLException e){
            e.printStackTrace();
            ServerMessageUtil.SPrintToConsole("getCardinality: "+e);
        }
        return card;
    }
}

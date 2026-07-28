
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

package iisc.dsl.picasso.common;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.QueryPacket;

public class ClientPacket implements Serializable {
	private static final long serialVersionUID = 2L;
	
	private QueryPacket qp;
	private DBSettings dbSettings;
	private Hashtable fields;
	public ApproxParameters ApproxParams;//ADG
	public boolean approxDiagram;
	public boolean fromCommandLine;//denotes whether the packet has come from PicassoCmd or not
	public String[] attrTypes;
	public String[] constants;
	
	public ClientPacket() {
		fields = new Hashtable();
		qp = new QueryPacket();
		dbSettings = null;
		fromCommandLine = false;
	}
	
	public QueryPacket getQueryPacket() {
		return qp;
	}
	
	public void setQueryPacket(QueryPacket qp) {
		//this.qp = qp;
		if(!PicassoConstants.IS_PKT_LOADED)
			this.qp = qp;
		else
			this.qp = new QueryPacket(qp);
	}
	
	
	//ap - TODO: These two functions should be in querypacket.. move them. 
	public String getSelecErrorThreshold() {
		return(qp.getSelecThreshold());
	}
	
	public void setSelecErrorThreshold(String error) 
	{
		qp.setSelecThreshold(error);
	}
	public DBSettings getDBSettings()
	{
		return dbSettings;
	}
	
	public void setDBSettings(DBSettings dbSettings)
	{
		this.dbSettings = dbSettings;
	}

	public Object get(Object key) 
	{
		return fields.get(key);
	}
	
	public void put(Object key, Object value)
	{
		fields.put(key, value);
	}

	//All below functions are specific wrappers for individual fields and use fields.get and fields.put
	
	public String getMessageId() {
		return((String)fields.get("MessageId"));
	}
	
	public void setMessageId(int msgId) {
		fields.put("MessageId", "" + msgId);
	}
	
	public String getClientId() {
		return ((String)fields.get("ClientId"));
	}
	
	public void setClientId(String clntId) {
		if(clntId != null)
			fields.put("ClientId", clntId);
	}
	
	public String getDbType() {
		return ((String)fields.get("DBType"));
	}
	
	public void setDbType(String dbtype) {
		if(dbtype != null)
			fields.put("DBType",dbtype);
	}
	
	public Vector getDimensions()
	{
		return (Vector)fields.get("dimensions");
	}
	
	public void setDimensions(Vector vect)
	{
		fields.put("dimensions",vect);
	}
	
	/* Predicate number should be used as the key and value should be
	 * selectivity in percentage.
	 */
	public Hashtable getAttributeSelectivities() {
		return (Hashtable)fields.get("attributeSelectivities");
	}
	
	public void setAttributeSelectivities(Hashtable attr) {
		if(attr==null)
			fields.remove("attributeSelectivities");
		else
			fields.put("attributeSelectivities",attr);
	}
	
	public String getPlanNumbers() {
		return((String)fields.get("PlanNumber"));
	}
	
	public void setPlanNumbers(String plannos) {
		fields.put("PlanNumber", plannos);
	}
	
	public Hashtable getCompileTreeValues() {
		return((Hashtable)fields.get("CompileTree"));
	}
	
	public void setCompileTreeValues(Hashtable values) {
		fields.put("CompileTree", values);
	}
	
	public int getProgress() {
		Integer p = (Integer)fields.get("Progress");
		try {
			if ( p == null )
				return(0);
			else
				return(p.intValue());
		} catch (Exception e) {
			return 0;
		}
	}
	
	public void setProgress(int p) {
		fields.put("Progress", new Integer(p));
	}
	/**
	 * ADG
	 */
	public void setApproxParameters(ApproxParameters p)
	{
		ApproxParams=p;
	}
	public ApproxParameters getApproxParameters()
	{
		return(ApproxParams);
	}
}

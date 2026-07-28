
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

package iisc.dsl.picasso.server.plan;

import java.util.Vector;
import java.util.ListIterator;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.TreeNode;

import java.sql.Statement;
import java.sql.SQLException;

public class Node {
	private int 	id, parentId, type;
	private String 	name;
	private double 	cost, card;
	private Vector argType, argValue;
	
	public Node()
	{
		argType = new Vector();
		argValue = new Vector();
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	
	public int getParentId()
	{
		return parentId;
	}
	public void setParentId(int id)
	{
		this.parentId = id;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		if(name != null)
			this.name = name.trim();
		else
			this.name = "";
	}
	
	public double getCost()
	{
		return cost;
	}
	public void setCost(double cost)
	{
		this.cost = cost;
	}
	
	public double getCard()
	{
		return card;
	}
	public void setCard(double card)
	{
		this.card = card;
	}

	public void populateTreeNode(TreeNode node)
	{
		node.setNodeValues(name, type, cost, cost, card, argType, argValue);
	}
	
	public long computeHash(String planDiffLevel)
	{
		long hash = 0,i=1;
		if(name != null) {
				hash +=	name.hashCode();
		}
		if(planDiffLevel.equals(PicassoConstants.SUBOPERATORLEVEL)){
			ListIterator it = argValue.listIterator();
			while(it.hasNext()) {
//			    String nodename = (String)it.next();
//			    nodename=nodename.replaceAll("CUSTOMER_CUSTKEY", "CUSTOMER");
//			    nodename=nodename.replaceAll("SUPPLIER.SUPPKEY", "SUPPLIER");
//			    if(nodename.indexOf("PK__CUSTOMER")==-1 && nodename.indexOf("PK__SUPPLIER")==-1) {
			    	hash += i++ * ((String)it.next()).hashCode();
//			    }
				
			}
		}
		hash = id*parentId*hash;
		return hash;
	}
	
	public long computeHash(String planDiffLevel, int nodeVal)
	{
		long hash = 0,i=1;
		if(name != null) {
				hash +=	name.hashCode();
		}
		if(planDiffLevel.equals(PicassoConstants.SUBOPERATORLEVEL)){
			ListIterator it = argValue.listIterator();
			while(it.hasNext()){
			    String nodename = (String)it.next();
//			    nodename=nodename.replaceAll("CUSTOMER_CUSTKEY", "CUSTOMER");
//			    nodename=nodename.replaceAll("SUPPLIER.SUPPKEY", "SUPPLIER");
//			    if(nodename.indexOf("PK__CUSTOMER")==-1 && nodename.indexOf("PK__SUPPLIER")==-1) {
			    	hash += i++ * nodename.hashCode();
//			    }
			   
			}
		}
		hash = nodeVal*hash;
		return hash;
	}
	
	public void show()
	{
		String tmp = ""+id +"\t"+parentId+"\t'"+name+"'\t"+cost+"\t"+card+"\t'";
		ListIterator itt = argType.listIterator();
		ListIterator itv = argValue.listIterator();
		while(itt.hasNext() && itv.hasNext())
			tmp+= (String)itt.next()+"="+(String)itv.next()+",";
		ServerMessageUtil.SPrintToConsole(tmp);
	}
	
	private String escapeQuotes(String str)
	{
		return str.replaceAll("'","''");
	}
	
	void storeNode(Statement stmt,int qtid,int planno, String schema) throws SQLException
	{
            	stmt.executeUpdate("INSERT INTO "+schema+".PicassoPlanTree values("+qtid+","+planno+","+id+","+parentId+",'"+name+"',"+cost+
				","+card+")");
		ListIterator itt = argType.listIterator();
		ListIterator itv = argValue.listIterator();
		while(itt.hasNext() && itv.hasNext()){
			stmt.executeUpdate("INSERT INTO "+schema+".PicassoPlanTreeArgs values("+qtid+","+planno+","+id+",'"+
			(String)itt.next()+"','"+escapeQuotes((String)itv.next())+"')");
		}
	}
	public boolean isArgTypePresent(String arg)
	{
		return argType.contains(arg);
	}
	public void addArgType(String arg)
	{
		argType.add(arg);
	}
	public void addArgValue(String arg) 
	{
		argValue.add(arg);
	}
	public Vector getArgType()
	{
		return argType;
	}
	public Vector getArgValue()
	{
		return argValue;
	}
}

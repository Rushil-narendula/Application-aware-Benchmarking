
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

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.TreeNode;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.db.Database;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.ListIterator;
import java.io.Serializable;

public class Plan implements Serializable {
	
	//Make sure you change this if you change any of the fields below.
	private static final long serialVersionUID = 1L;
	
	private Vector nodes;
	private int planno;
	private long hash;
	
	public Plan()
	{
		nodes = new Vector();
	}
	
	public double getCost()
	{
		return ((Node)nodes.get(0)).getCost();
	}
	
	public double getCard()
	{
		return ((Node)nodes.get(0)).getCard();
	}
	
	/*
	 * IMPORTANT : We now use the implementation which fails upon ambiguity and use histogram.getOptSel.
	 * We need the picasso selectivity to find the optimizer selectivity
	 * because the given relation can appear more than once in the query which
	 * results in ambiguity when selectivity is obtained from the plan. We resolve
	 * by picking the one that is at a minimum distant from picasso selectivity
	 */
	public double getSelectivity(String tabName,double card, double dpsel, double osel)
	{
		// We assume same relation will not be used more than 10 times in the given query 
		double tmp[] = new double[10];
		double optsel=-1;
		int i=0; int count = 0;
		Node node=null;
		ListIterator it = nodes.listIterator();
		DecimalFormat df = new DecimalFormat("#0.00");
		df.setMaximumFractionDigits(2);
		while(it.hasNext()){
			node = (Node)it.next();
			/*if(osel == 0.01986324042081833)
				System.out.println("here now");*/
			count++;
			if(node.getName().toUpperCase().startsWith(tabName.toUpperCase())){
				node = getNodeById(node.getParentId());
				// card should not be 0, if it is then it is an error in generating histograms
				if (card == 0)
					tmp[i++] = node.getCard();
				else
					tmp[i++] = node.getCard()/card; 
			}
		}
		if(i==0)
			return -1;
		optsel = tmp[i-1];
		for(;--i>=0;){
			if(Math.abs(Math.abs(tmp[i]-osel)-dpsel) < Math.abs(Math.abs(optsel-osel)-dpsel))
				optsel = tmp[i];
		}
		return optsel;
	}

	public double getSelectivity(String tabName,double card, double picsel)
	{
		// We assume same relation will not be used more than 10 times in the given query 
		double tmp[] = new double[10];
		double optsel=-1;
		int i=0;
		Node node=null;
		ListIterator it = nodes.listIterator();
		DecimalFormat df = new DecimalFormat("#0.00");
		df.setMaximumFractionDigits(2);
		while(it.hasNext()){
			node = (Node)it.next();
			if(node.getName().toUpperCase().startsWith(tabName.toUpperCase())){
				node = getNodeById(node.getParentId());
				tmp[i++] = node.getCard()/card; 
			}
		}
		for(;--i>=0;)
			if(Math.abs(tmp[i]-picsel)<Math.abs(optsel-picsel))
				optsel = tmp[i];
		return optsel;
	}

	public double getSelectivity(String tabName,double card)
	{
		double optsel=-1;
		int i=0;
		Node node=null;
		ListIterator it = nodes.listIterator();
		DecimalFormat df = new DecimalFormat("#0.00");
		df.setMaximumFractionDigits(2);
		while(it.hasNext()){
			node = (Node)it.next();
			if(node.getName().equalsIgnoreCase(tabName)){
				i++;
				node = getNodeById(node.getParentId());
				optsel = node.getCard()/card; 
			}
		}
		if(i>1)
			return -1;
		return optsel;
	}

	public Node getNodeById(int id) 
	{
		Node node;
		ListIterator it = nodes.listIterator();
		while(it.hasNext()){
			node = (Node)it.next();
			if(node.getId() == id)
				return node;
		}
		return null;
	}

	public Node getNode(int index)
	{
		if(index >=0 && index<nodes.size())
			return (Node)nodes.get(index);
		System.out.println("For plan "+planno+" index "+index+ " does not exist");
		return null;
	}
	
	public int getPlanNo()
	{
		return planno;
	}
	
	public void setNode(Node node, int index)
	{
		nodes.add(index,node);
	}
	
	public void setPlanNo(int no)
	{
		planno = no;
	}
	
	public void computeHash(String planDiffLevel)
	{
		hash = 0;
		ListIterator it = nodes.listIterator();
		while(it.hasNext())
				hash += ((Node)it.next()).computeHash(planDiffLevel);
	}
	
	public Vector getNodes() {
		return nodes;
	}
	
	public void computeMSSQLHash(String planDiffLevel)
	{
		hash = 0;
		ListIterator it = nodes.listIterator();
		int nodeLevel = 1;
		while(it.hasNext()) {
			hash += ((Node)it.next()).computeHash(planDiffLevel, nodeLevel);
		}
	}
	
	public long getHash()
	{
		return hash;
	}
	
	
	public int getSize()
	{
		return nodes.size();
	}
	
	public int getIndexInVector(Vector plans)
	{
		int index=0;
		ListIterator it = plans.listIterator();
		while(it.hasNext()){
			if(((Plan)it.next()).getHash() == hash)
				return index;
			index++;
		}
		return -1;
	}
	public void storePlan(Statement stmt,int qtid, String schema)throws SQLException
	{
		ListIterator it = nodes.listIterator();
		while(it.hasNext())
				((Node)it.next()).storeNode(stmt, qtid, planno, schema);
	}

	/*
	 * We have to read the plantreeargs to get the sub-operator level information
	 * and add it in the plan...
	 */
	public static Plan getPlan(Database database, int qtid, int planno, String planDiffLevel)
	{
		int id;
		Plan plan = new Plan();
		plan.setPlanNo(planno);
		Node node;
		int curNode = 0;
		try{
			Statement stmt = database.createStatement();
			Statement stmt1 = database.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM "+database.getSchema()+".PicassoPlanTree where QTID="+qtid+
					" and PLANNO="+planno +" order by PARENTID, ID");
			while(rs.next()){
				node = new Node();
				id = rs.getInt("ID");
				node.setId(id);
				node.setParentId(rs.getInt("PARENTID"));
				node.setName(rs.getString("NAME"));
				node.setCost((float)rs.getDouble("COST"));
				node.setCard((float)rs.getDouble("CARD"));
				if(planDiffLevel.equals(PicassoConstants.SUBOPERATORLEVEL)){
					ResultSet rs2 = stmt1.executeQuery("SELECT * FROM "+database.getSchema()+".PicassoPlanTreeArgs where QTID="+
							qtid+" and PLANNO="+planno +" and ID="+id);
					while(rs2.next()){
						node.addArgType(rs2.getString("ARGNAME"));
						node.addArgValue(rs2.getString("ARGVALUE"));
					}
					rs2.close();
				}
				plan.setNode(node,curNode++);
			}
			rs.close();
			stmt.close();
			stmt1.close();
			return plan;
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error getting plan from database "+e);
		}
		return null;
	}

	/*
	 * For testing
	 */
	public void show()
	{
		ListIterator it = nodes.listIterator();
		ServerMessageUtil.SPrintToConsole("Id\tParentId\tName\tCost\tCard");
		while(it.hasNext()) {
			((Node)it.next()).show();
		}
	}

	public void showPlan(int index)
	{
		if(index>= nodes.size())
			return;
		boolean firstCall = false;
		if(index==0)
			firstCall=true;
		Node root = ((Node)nodes.elementAt(index));
		if(!firstCall && root.getId()==-1){
			System.out.print(root.getName());
			return;
		}
		ListIterator it = nodes.listIterator(index);
		System.out.print("(");
		while(it.hasNext()){
			Node left = (Node)it.next();
			if(left.getParentId() == root.getId()){
				showPlan(index);
				break;
			}
			index++;
		}
		System.out.print(" "+root.getName()+" ");
		while(it.hasNext()){
			Node right = (Node)it.next();
			if(right.getParentId() == root.getId())
				showPlan(index);
			index++;
		}
		System.out.print(")");
		if(firstCall==true)
			System.out.println();
	}

	public boolean isIdPresent(int id) 
	{
		ListIterator it = nodes.listIterator();
		while(it.hasNext()){
			if(((Node)it.next()).getId() == id)
				return true;
		}
		return false;
	}
	
	
	public TreeNode createPlanTree()
	{
		return createSubTree(null,0,this,0);
	}
	private TreeNode createSubTree(TreeNode parent, int depth, Plan plan, int index)
	{
		TreeNode root = new TreeNode(depth, parent);
		Node node = plan.getNode(index);
		if(node == null)
			return null;
		node.populateTreeNode(root);
		int id = node.getId();
		if(id==-1){
			root.setChildren(new Vector());
			return root;
		}
		Vector children = new Vector();
		for(int i=0;i<plan.getSize();i++){
			node = plan.getNode(i);
			if(node.getParentId() == id){
				children.add(createSubTree(root, depth+1, plan,i));
			}
		}
		root.setChildren(children);
		return root;
	}
//apexp
//	got from PGraph.java and simplified.
	static  String getAttributeStr2(TreeNode tree) {
		        java.util.Hashtable table = tree.getAttributes();
		        
		        if ( table == null ) {
		            table = new java.util.Hashtable();
		        }
		        
		        Object[] keys = table.keySet().toArray();
		        if ( keys.length == 0 )
		            return "";
		        
		        String str = " ";
		        int length = 150;
		        for (int i=0; i < keys.length; i++) {
		            str += (keys[i] + "=" + table.get(keys[i]) + "; ");
		        }

		        str = str.replace('"','\'');
		        
		        return str;
		    }	
		
		
	public static void showSubTree(TreeNode roo)
	{
		System.out.print(roo.getNodeName());
		
		Vector chd = roo.getChildren();
		if(chd.size()>0)
		{
			System.out.print("(");
			showSubTree((TreeNode)chd.elementAt(0));
			System.out.print(")");
		}
		
		if(chd.size()>1)
		{
			System.out.print(",");
			System.out.print("(");
			showSubTree((TreeNode)chd.elementAt(1));
			System.out.print(")");
		}
		
		if(chd.size()>2)
				System.out.print("More than 2 children!");
	}
//end apexp

}

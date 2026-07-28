
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

package iisc.dsl.picasso.server.network;

import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.ds.TreeNode;
import iisc.dsl.picasso.server.Processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

public class ServerMessageUtil {

	public static final void sendNewClientId(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int clientId) {
		
		if ( sock == null )
			return;
		
		ServerMessageUtil.SPrintToConsole("New Client Id : " + clientId);
		
		ServerPacket packet = new ServerPacket();
		packet.clientId = clientId;
		packet.messageId = MessageIds.GET_CLIENT_ID;
		packet.status = MessageIds.END_MESSAGE;
		packet.diagramPacket = null;
		packet.trees = null;
		
		try {
			out.writeObject(packet);
		} catch (Exception e) { ServerMessageUtil.SPrintToConsole("Error Closing Socket"); }
		return;	
	}
	
	public static final void sendErrorMessage(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, String errorMsg) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.ERROR_ID;
		packet.status = errorMsg;
		packet.diagramPacket = null;
		packet.trees = null;
		
		try {
			out.writeObject(packet);
		} catch (Exception e) { ServerMessageUtil.SPrintToConsole("Error Closing Socket"); }
		return;	
	}
	
	public static final void sendWarningMessage(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, String errorMsg) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.WARNING_ID;
		packet.status = errorMsg;
		packet.diagramPacket = null;
		packet.trees = null;
		
		try {
			out.writeObject(packet);
		} catch (Exception e) { ServerMessageUtil.SPrintToConsole("Error Closing Socket"); }
		return;	
	}
	
	public static final void sendStatusMessage(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int progress, String statusMsg) {
		
		sendStatusMessage(sock, in, out, progress, statusMsg, MessageIds.STATUS_ID);
		return;	
	}

	//this is a generic function to send a string with any MessageID
	public static final void sendStatusMessage(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int progress, String statusMsg, int statusId) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = statusId;
		packet.progress = progress;
		packet.status = statusMsg;
		packet.diagramPacket = null;
		packet.trees = null;
		
		try {
			out.writeObject(packet);
		} catch (Exception e) { 
			ServerMessageUtil.SPrintToConsole("Error Writing Object ");
			e.printStackTrace();
		}
		return;	
	}
	
	
	public static final void sendEstimatedTime(Socket sock, ObjectInputStream in, ObjectOutputStream out, QueryPacket qp, int time,int optClass)
	{
		if ( sock == null )
			return;
		qp.setGenDuration(time);
		qp.setEstimatedTime(time);
		int hour, minute;
		hour = time/3600;
		time = time % 3600;
		minute = time/60;
		time = time % 60;
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.TIME_TO_GENERATE;
		packet.status = "Estimated Time to Generate Exact Picasso Diagram is";
		if(hour!=0)
			packet.status += " "+hour+" hr "+minute+" min.";
		else if(minute != 0)
			packet.status += " "+minute+" min "+time+" sec.";
		else
			packet.status += " "+time+" sec.";
		packet.queryPacket = qp;
		packet.progress = 0;
		packet.diagramPacket = null;
		packet.trees = null;
		packet.queries = null;
		packet.optClass = optClass;
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static final void sendEstimatedTimeApprox(Socket sock, ObjectInputStream in, ObjectOutputStream out, QueryPacket qp, int time)
	{
		if ( sock == null )
			return;
		qp.setGenDuration(time);
		int hour, minute;
		hour = time/3600;
		time = time % 3600;
		minute = time/60;
		time = time % 60;
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.TIME_TO_GENERATE_APPROX;
		packet.status = "Estimated Time to Generate Approximate Picasso Diagram is";
		if(hour == 0 && minute <= 1) {
			packet.status += " less than 1 min.";
		}else {
			if(hour!=0)
				packet.status += " "+hour+" hr "+minute+" min.";
			else if(minute != 0)
				packet.status += " "+minute+" min "+time+" sec.";
			else
				packet.status += " "+time+" sec.";
		}
		packet.status += " Do you want to generate?";
		packet.queryPacket = qp;
		packet.progress = 0;
		packet.diagramPacket = null;
		packet.trees = null;
		packet.queries = null;
		
		//packet.optClass = optClass;
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static final void sendEstimatedTimeRegenerate(Socket sock, ObjectInputStream in, ObjectOutputStream out,
			QueryPacket qp, int time, String changed_settings,int optClass)
	{
		if ( sock == null )
			return;
		qp.setGenDuration(time);
		int hour, minute;
		hour = time/3600;
		time = time % 3600;
		minute = time/60;
		time = time % 60;
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.TIME_TO_GENERATE;
			
		packet.status = "QTID: "+qp.getQueryName()+" exists and settings have changed.\n"+
                        changed_settings+
			"Do you want to generate and overwrite existing diagram?\n\n";
		packet.status += "Estimated Time to Generate Exact Picasso Diagram is";
		if(hour!=0)
			packet.status += " "+hour+" hr "+minute+" min.";
		else if(minute != 0)
			packet.status += " "+minute+" min "+time+" sec.";
		else
			packet.status += " "+time+" sec.";

		packet.queryPacket = qp;
		packet.progress = 0;
		packet.diagramPacket = null;
		packet.trees = null;
		packet.queries = null;
		packet.optClass = optClass;
			
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void sendPlanDiagram(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, QueryPacket qp, DiagramPacket dp, Vector trees) {
		
		if ( sock == null )
			return;
		
		qp.genSuccess = true;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.READ_PICASSO_DIAGRAM;
		packet.status = MessageIds.END_MESSAGE;
		packet.queryPacket = qp;
		
		//we'll send a compressed DiagramPacket instead
		packet.diagramPacket = null;
		
		ByteArrayOutputStream baos;
		
		baos = new ByteArrayOutputStream();
		try
		{
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		ObjectOutputStream oos = new ObjectOutputStream(gos);
		oos.writeObject(dp);
		oos.flush();
		gos.flush();
		gos.finish();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		packet.compressedDiagramPacket = baos.toByteArray();
		
//		we'll send compressed trees instead
		packet.trees = null;
		
		baos = new ByteArrayOutputStream();
		try
		{
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		ObjectOutputStream oos = new ObjectOutputStream(gos);
		oos.writeObject(trees);
		oos.flush();
		gos.flush();
		gos.finish();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		packet.compressedTrees = baos.toByteArray();
		
		packet.queries = null;
			
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void sendQueryNames(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, Vector queryNames) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.GET_QUERYTEMPLATE_NAMES;
		packet.status = MessageIds.END_MESSAGE;
		packet.diagramPacket = null;
		packet.trees = null;
		packet.queries = queryNames;
			
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void sendPlanTree(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int[] planNum, TreeNode[] roots, int type) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = type;
		packet.status = MessageIds.END_MESSAGE;
		
		int numOfPlans = planNum.length;
		
		Vector tree = new Vector();
		tree.addElement(new Integer(numOfPlans));
		
		for (int i=0; i < planNum.length; i++) {
			tree.addElement(new Integer(planNum[i]));
			tree.addElement(roots[i]);
		}
		packet.trees = tree;
			
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//added for multiplan
	//same as sendplantree but a few extra parameters sent
	public static final void sendMultiPlanTree(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int[] planNum, TreeNode[] roots, int type,java.util.ArrayList sel_val_attr_arrlist) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = type;
		packet.status = MessageIds.END_MESSAGE;
		
		int numOfPlans = planNum.length;
		
		Vector tree = new Vector();
		tree.addElement(new Integer(numOfPlans));
		
		for (int i=0; i < planNum.length; i++) {
			tree.addElement(new Integer(planNum[i]));
			tree.addElement(roots[i]);
		}
		packet.trees = tree;
		if(packet.hashmap==null)
		{
		packet.hashmap = new java.util.HashMap();
		}
		packet.hashmap.put("sel_val_attr_arrlist",sel_val_attr_arrlist);
		
		
		
			
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//addition for multiplan ends here
	public static final void sendQueryPlusPlan (Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, int[] planNum, String xplan, int type) {
		
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = type;
		packet.status = MessageIds.END_MESSAGE;
		packet.absPlan = xplan;
		
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void sendServerStatus(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, ClientPacket msg) 
	{
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.clientId = new Integer(msg.getClientId()).intValue();
		packet.messageId = MessageIds.GET_SERVER_STATUS;
		packet.status = MessageIds.END_MESSAGE;
		
		Vector procs = new Vector();
		Hashtable dbIds = (Hashtable)msg.get("DBTABLE");
		if ( dbIds != null ) {
			Object keys[] = dbIds.keySet().toArray();
			for (int i=0; i < keys.length; i++) {
				Vector dbProcesses = (Vector)dbIds.get(keys[i]);
				for (int j=0; j < dbProcesses.size(); j++) {
					ClientPacket cp = new ClientPacket();
					ClientPacket orig = (ClientPacket)dbProcesses.elementAt(j);
					cp.setClientId(orig.getClientId());
					cp.setDBSettings(orig.getDBSettings());
					cp.getQueryPacket().setQueryName(orig.getQueryPacket().getQueryName());
					cp.getQueryPacket().setQueryTemplate(orig.getQueryPacket().getQueryTemplate());
					cp.setProgress(orig.getProgress());
					procs.add(cp);
				}
			}
		}
		packet.queries = procs;
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void deleteProcess(ClientPacket clientPacket) {
		String dbId = getDBId(clientPacket);
		Hashtable dbIds = (Hashtable)clientPacket.get("DBTABLE");
		Vector dbProcesses = (Vector)dbIds.get(dbId);
		
		if ( dbProcesses == null ) {
			return;
		}
		
		for (int i=0; i < dbProcesses.size(); i++) {
			ClientPacket cp = (ClientPacket)dbProcesses.elementAt(i);
			if ( (cp.getQueryPacket().getQueryName()).equals(clientPacket.getQueryPacket().getQueryName()) ) {
				if ( i == 0 ) {
					Processor dbThread = (Processor)cp.get("DBTHREADID");
					dbThread.interrupt();
					return;
				}
				ServerMessageUtil.SPrintToConsole("In remove queue Process:: " + dbIds.size());
				
				// Remove from the database queue
				dbId = getDBId(cp);
				if ( dbProcesses == null ) {
					ServerMessageUtil.SPrintToConsole("DBProcesses is null !!!" + dbId);
					return;
				}
				
				ServerMessageUtil.SPrintToConsole("DBID :: " + dbId + " " + dbProcesses.size());
				
				// Removing element from the vector
				dbProcesses.removeElement(cp);
				//removeFromQueue(cp);
				break;
			}
		}
	}
	
	public static final void SPrintToConsole(String str) {
		if ( PicassoConstants.IS_SERVER_DEBUG == true ) {
			System.out.println("SERVER :: " + str);
			System.err.println("SERVER :: " + str);
		}
	}
	
	public static String getDBId(ClientPacket msg) {
		DBSettings dbSettings = msg.getDBSettings();
		if(dbSettings == null)
			return null;
		String dbServer = dbSettings.getServerName();
		String dbPort = dbSettings.getServerPort();
		String dbSchema = dbSettings.getSchema();
		String dbName = dbSettings.getDbName();
		String dbUser = dbSettings.getUserName();
		String dbPass = dbSettings.getPassword();
		String dbInstance = dbSettings.getInstanceName();
		
		if (dbServer == null || dbPort == null || 
				dbSchema == null || dbName == null )
			return null;
		else
			return(dbServer + ":" + dbPort + ":" + dbName + ":" + dbSchema + ":" + dbUser + ":" + dbPass + ":" + dbInstance);
	}
	
	public synchronized static boolean addToQueue(String dbId, ClientPacket clientPacket) {
		Hashtable dbIds = (Hashtable)clientPacket.get("DBTABLE");
		
		Vector dbProcesses = (Vector)dbIds.get(dbId);
		
		if ( dbProcesses == null ) {
			// Add to the queue and call the appropriate process.
			dbProcesses = new Vector();
			dbProcesses.addElement(clientPacket);
			dbIds.put(dbId, dbProcesses);
			return(false);
		} else if (dbProcesses.size() == 0 ) {
			ServerMessageUtil.SPrintToConsole("Got the db processes : " 
															+ dbProcesses.size());
			dbProcesses.addElement(clientPacket);
			return(false);
		} else {  // Add to queue and send a message to client saying it is queued up
			dbProcesses.addElement(clientPacket);
			return(true);
		}
	}
	
	public synchronized static void removeFromQueue(ClientPacket clientPacket) {
		Hashtable dbIds = (Hashtable)clientPacket.get("DBTABLE");
		ServerMessageUtil.SPrintToConsole("In remove queue :: " + dbIds.size());
		
		// Remove from the database queue
		String dbId = getDBId(clientPacket);
		Vector dbProcesses = (Vector)dbIds.get(dbId);
		if ( dbProcesses == null ) {
			ServerMessageUtil.SPrintToConsole("DBProcesses is null !!!" + dbId);
			return;
		}
		
		ServerMessageUtil.SPrintToConsole("DBID :: " + dbId + " " + dbProcesses.size());
		
		// Removing element from the vector
		dbProcesses.removeElement(clientPacket);
		// If there are dbProcesses in the queue we need to execute them..
		if (dbProcesses.size() > 0) {
			ClientPacket nextMsg = (ClientPacket)dbProcesses.elementAt(0);
			
			/*if(PicassoConstants.SAVING_DONE)
			{
				try {
					Thread.sleep(5000);
					// The 5 second sleep is done to ensure that the database is committed
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				PicassoConstants.SAVING_DONE = false;
				new Processor(null, null, null, nextMsg);
			}*/
			new Processor(null, null, null, nextMsg);
		}
	}

	public static final void sendPlanStrings(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, ArrayList plans, int type) {
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = type;
		packet.status = MessageIds.END_MESSAGE;
		packet.hashmap = new HashMap();
		packet.hashmap.put("PlanStrings", plans);
		
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final void sendPlanCosts(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, ArrayList costs, int type) {
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = type;
		packet.status = MessageIds.END_MESSAGE;
		packet.hashmap = new HashMap();
		packet.hashmap.put("PlanCosts", costs);
		
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final void sendAllPlanCosts(Socket sock, ObjectInputStream in, 
			ObjectOutputStream out, double[][] costs) {
		if ( sock == null )
			return;
		
		ServerPacket packet = new ServerPacket();
		packet.messageId = MessageIds.GET_ALL_PLAN_COSTS;
		packet.status = MessageIds.END_MESSAGE;
		packet.hashmap = new HashMap();
		packet.hashmap.put("AllPlanCosts", costs );
		
		try {
			out.writeObject(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


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

package iisc.dsl.picasso.server;

import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoSettingsManipulator;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.common.PicassoConstants;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;

public class Picasso_Server {

	private int numClients;
	private Hashtable 			DBTable;
	
	public Picasso_Server(int portno) {
		
		// Initialize dbIds
		DBTable = new Hashtable();
		numClients = 0;

		// Create a new service at a port
		ServerSocket serversock;
		try {
			serversock = new ServerSocket(portno);
		} catch (Exception e) {
			System.err.println("Cannot create server socket "+e + "\nMost probably, another Picasso Server or some other software is already running at the provided port.");
			System.out.println("Cannot create server socket "+e + "\nMost probably, another Picasso Server or some other software is already running at the provided port.");
			return;
		}
		
		System.err.println("Picasso Server started. Listening for client requests");
		System.out.println("Picasso Server started. Listening for client requests");
		while (true) {
			//		 wait for the next client connection
			try {
				Socket sock = serversock.accept();
				sock.setSoLinger(true, 60000);
				ObjectInputStream in =  new ObjectInputStream(sock.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
				ClientPacket clientMsg = parseMessage(in);
				//One of the 2 streams is used to write a log file, the other is used to display on screen; that's why the duplication.
				System.err.println("Request from Client "+clientMsg.getClientId()+" with Command "+clientMsg.getMessageId()+":"+MessageIds.MessageString[Integer.parseInt(clientMsg.getMessageId())]);
				System.out.println("Request from Client "+clientMsg.getClientId()+" with Command "+clientMsg.getMessageId()+":"+MessageIds.MessageString[Integer.parseInt(clientMsg.getMessageId())]);
				processMessage(sock, in, out, clientMsg);
			} catch (Exception e) {
				ServerMessageUtil.SPrintToConsole("Error accepting client request ");
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		int portno;
		PicassoSettingsManipulator.ReadPicassoConstantsFromFile();
		try{
			if(args.length > 0)
				portno = Integer.parseInt(args[0]);
			else
				portno = PicassoConstants.SERVER_PORT;
		} catch(NumberFormatException e) {
			portno = PicassoConstants.SERVER_PORT;
		}
		new Picasso_Server(portno);
	}
	
	private ClientPacket parseMessage(ObjectInputStream in) {
		ClientPacket fromClient=null;
		
		try {
			fromClient = (ClientPacket)in.readObject();
		} catch (Exception e) {
			ServerMessageUtil.SPrintToConsole("Error: Cannot parse message from client");
			e.printStackTrace();
		}	
		return fromClient;
	}
	
	private void processMessage(Socket sock, ObjectInputStream in, ObjectOutputStream out, ClientPacket clientPacket) throws InterruptedException 
	{
		
//		Get the clientId and the messageId
		String clientStr = clientPacket.getClientId();
		String msgStr = clientPacket.getMessageId();
		if ( clientStr == null || msgStr == null ) { // Flag error, these fields have to be there
			ServerMessageUtil.SPrintToConsole("Error: Client message is invalid");
			ServerMessageUtil.sendErrorMessage(sock, in, out, "Client message has invalid message id or client id");
			return;
		}
		int clientId = new Integer(clientStr).intValue();
		int messageId = new Integer(msgStr).intValue();
		ServerMessageUtil.SPrintToConsole("Client: " + clientId + "\tcommand: " + messageId);
		
		
		if( messageId == MessageIds.SHUTDOWN_SERVER ) 
		{
			boolean busy = false;
			Set keys = DBTable.keySet();
			Iterator it = keys.iterator();
			while(it.hasNext()){
				Vector process = (Vector)it.next();
				if(process!=null && process.size()>0)
					busy = true;
			}
			if(!busy){
				ServerMessageUtil.sendWarningMessage(sock,in,out,"Picasso Server is stopped");
				ServerMessageUtil.sendErrorMessage(sock,in,out,null);
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
					
				}
				/*try{
					sock.close();
				}catch(IOException e) {
					e.printStackTrace();
				}*/
				ServerMessageUtil.SPrintToConsole("Stopping Server");
				System.exit(0);
			}
			else
				ServerMessageUtil.sendErrorMessage(sock,in,out,"Server is busy");
		}
		else if ( messageId == MessageIds.GET_CLIENT_ID && clientId == 0 ) 
		{
			// Send the client a new id
			ServerMessageUtil.sendNewClientId(sock, in, out, ++numClients);
			return;
		}
		else if ( messageId == MessageIds.GET_SERVER_STATUS ) 
		{
			ServerMessageUtil.sendServerStatus(sock, in, out, clientPacket);
			return;
		}

		else if(messageId == MessageIds.SET_SERVER_SETTING) {
			String settingName = (String) clientPacket.get("SettingName");
			String settingValue = (String) clientPacket.get("SettingValue");
			int value = Integer.parseInt(settingValue);
			boolean bvalue;
			if(value==1) bvalue=false; else bvalue=true;
			if(settingName.equals("IS_SERVER_DEBUG"))
				PicassoConstants.IS_SERVER_DEBUG=bvalue;
			else if(settingName.equals("COLLATION_SCHEME"))
				PicassoConstants.COLLATION_SCHEME=value;
			ServerMessageUtil.sendStatusMessage(sock,in,out,0,"Setting Set");
			System.out.println("The constants now are "+PicassoConstants.COLLATION_SCHEME+" "+PicassoConstants.IS_SERVER_DEBUG);
			return;
			
		}
		// Check if we need to queue the request...
		// We need to queue the requests only if the db is busy,
		// not if we are receiveing multiple requests from client.
		// for this we need to get the db id...
		
		// The db id is just a string of 
		// <DBServerName>:<DBPort>:<DBName>:<DBSchemaName>
		String dbId = ServerMessageUtil.getDBId(clientPacket);
		if ( dbId == null ) {
			ServerMessageUtil.sendErrorMessage(sock, in, out, "No DBConnection present");
			return;
		}

		/*
		 * Add to the hashtable message the DBTable and the Client Info
		 * So that we can remove it from the queue later on.
		 * Not a nice way of doing it..
		 */
		clientPacket.put("DBTABLE",DBTable);
		
		if ( messageId == MessageIds.DELETE_PROCESS ) 
		{
			ServerMessageUtil.deleteProcess(clientPacket);
			return;
		}
		else if ( messageId == MessageIds.STOP_PROCESSING ) 
		{
			Vector dbProcs = (Vector)DBTable.get(dbId);
			if(dbProcs == null)
				return;
			// First entry in the dbProcesses needs to be stopped
			if (dbProcs.size() > 0) {
				ClientPacket procMsg = (ClientPacket)dbProcs.elementAt(0);
				Processor dbThread = (Processor)procMsg.get("DBTHREADID");
				dbThread.interrupt();
			}
		}
		
		// TODO: Eliminate the use of deprecated methods in the following two methods. 
		// Using them for now because the deadlock scenario mentioned in the documentation doesnt arise in this case.
		else if ( messageId == MessageIds.PAUSE_PROCESSING ) 
		{
			Vector dbProcs = (Vector)DBTable.get(dbId);
			if(dbProcs == null)
				return;
			// First entry in the dbProcesses needs to be stopped
			if (dbProcs.size() > 0) {
				ClientPacket procMsg = (ClientPacket)dbProcs.elementAt(0);
				Processor dbThread = (Processor)procMsg.get("DBTHREADID");
				// dbThread.suspend(); // temporary solution
				PicassoConstants.IS_SUSPENDED = true;
				ServerMessageUtil.sendStatusMessage(sock, in, out, 0, "Diagram generation paused", MessageIds.STATUS_ID);
			}
		}
		else if ( messageId == MessageIds.RESUME_PROCESSING ) 
		{
			Vector dbProcs = (Vector)DBTable.get(dbId);
			if(dbProcs == null)
				return;
			// First entry in the dbProcesses needs to be stopped
			if (dbProcs.size() > 0) {
				ClientPacket procMsg = (ClientPacket)dbProcs.elementAt(0);
				Processor dbThread = (Processor)procMsg.get("DBTHREADID");
				// dbThread.resume(); // temporary solution
				PicassoConstants.IS_SUSPENDED = false;
			}
		}
		else if ( messageId == MessageIds.STOP_COMPILING ) {
			Vector dbProcs = (Vector)DBTable.get(dbId);
			if(dbProcs == null)
				return;
			// First entry in the dbProcesses needs to be stoped
			if (dbProcs.size() > 0) {
				ClientPacket procMsg = (ClientPacket)dbProcs.elementAt(0);
				Processor dbThread = (Processor)procMsg.get("DBTHREADID");
				dbThread.stopCompiling();
			}
			
		}
//		Do not queue the following messages....
		else if ( messageId == MessageIds.GET_QUERYTEMPLATE_NAMES 
				|| messageId == MessageIds.READ_PICASSO_DIAGRAM 
				|| messageId == MessageIds.GET_PLAN_TREE 
				|| messageId == MessageIds.GET_MULTI_PLAN_TREES
				|| messageId == MessageIds.GET_PLAN_COSTS
				|| messageId == MessageIds.GET_PLAN_STRINGS
				/* || messageId == MessageIds.PAUSE_PROCESSING
				|| messageId == MessageIds.RESUME_PROCESSING */
				) {
			Processor dbThread = new Processor(sock, in, out, clientPacket);
			if ( dbThread != null )
				clientPacket.put("DBTHREADID", dbThread);
		}
		else if ( ServerMessageUtil.addToQueue(dbId, clientPacket) ) 
		{
			ServerMessageUtil.SPrintToConsole("Message has been queued up....");
			ServerMessageUtil.sendStatusMessage(sock, in, out, 0, "Process has been queued", MessageIds.PROCESS_QUEUED);
			//ServerMessageUtil.sendStatusMessage(sock, in, out, clientId, 0, "Process has been queued up");
		}
		else 
		{
			Processor dbThread = new Processor(sock, in, out, clientPacket);
			if ( dbThread != null )
				clientPacket.put("DBTHREADID", dbThread);
		}
	}
}

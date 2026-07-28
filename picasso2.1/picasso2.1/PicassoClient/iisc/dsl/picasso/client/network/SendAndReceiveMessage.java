
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

package iisc.dsl.picasso.client.network;

import iisc.dsl.picasso.client.frame.PlanTreeFrame;
import iisc.dsl.picasso.client.frame.PlanTreeFrame_DifEngines;
import iisc.dsl.picasso.client.frame.ServerStatusFrame;
import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.client.panel.PlanPanel;
import iisc.dsl.picasso.client.panel.ReducedPlanPanel;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SendAndReceiveMessage extends Thread {
	
	ObjectInputStream 	in;
	ObjectOutputStream  out;
	Socket sock;
	PicassoPanel comp;
	ClientPacket clientPacket;
	String		 serverName;
	int			 serverPort;
	
	public SendAndReceiveMessage(String name, int port, 
			ClientPacket cp, PicassoPanel jcomp) 
	{
		try {	
			serverName = name;
			serverPort = port;
			if (serverName == null || serverName.length() == 0) {
				JOptionPane.showMessageDialog(comp, "Picasso Server machine is not specified.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (serverPort == 0 ) {
				JOptionPane.showMessageDialog(comp, "Picasso Server port is blank.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			sock = new Socket(serverName, serverPort);
			out = new ObjectOutputStream(sock.getOutputStream());
			comp = jcomp;
			clientPacket = cp;
			
			int msgId = 0;
			try {
				msgId = Integer.parseInt(cp.getMessageId());
			} catch (Exception e) {
				msgId = 0;
			}

			if(!MessageIds.MessageString[msgId].equals("Read Diagram"))
				comp.setStatus(MessageIds.MessageString[msgId] + " request sent to Server");
			else
				comp.setStatus("Querying the server");
			out.writeObject(clientPacket);
			MessageUtil.CPrintToConsole(" Message " + msgId + ":"+ MessageIds.MessageString[msgId] + " sent to Server " + serverName);
			out.flush();
			//out.close();
			
			start(); //Call the run() method below to receieve response from server.
			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(comp, "Cannot connect to Picasso Server on "+serverName,"Error",JOptionPane.ERROR_MESSAGE);
			MessageUtil.CPrintErrToConsole("Exception: " + e);
		}
		
	}
	
	public void run() {
		try {
		      ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
		      
			// Send a notification to the PicassoPanel to inform 
			// that we have received the packet from the server..
			//ap TODO: check if the next line is necessary
			while (true) {
				final ServerPacket fromServer = (ServerPacket)in.readObject();
				if(fromServer==null)
					break;
				int messageId = fromServer.messageId;
				
				MessageUtil.CPrintToConsole("Message "+ messageId + ":" + MessageIds.MessageString[messageId] + " received from Server; Status: " + fromServer.status);
				if(messageId == MessageIds.STATUS_ID) {
					if(fromServer.status.indexOf("STATUS:") == -1)
						comp.setStatus("STATUS: "+fromServer.status);
					else
						comp.setStatus(fromServer.status);
				}
				else
					comp.setStatus("STATUS: DONE");
					
				
				switch (messageId) 
				{

				case MessageIds.ERROR_ID :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							if(fromServer.status != null && comp.getParent()!=null)
								JOptionPane.showMessageDialog(comp.getParent().getParent(), "ERROR from Picasso Server " + " \n" + serverName + ":" + serverPort + " \n" + fromServer.status,"Error",JOptionPane.ERROR_MESSAGE);
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							comp.processErrorMessage(fromServer);
						}
					});
					break;

				case MessageIds.WARNING_ID :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							if ( comp instanceof PlanPanel ) {
								JOptionPane.showMessageDialog(comp.getParent().getParent(), fromServer.status,"Status",JOptionPane.INFORMATION_MESSAGE);
								comp.addServerMessage(fromServer);
								comp.firePropertyChange("statusReceived", 2, 3);
							}
						}
					});
					break;

					
				case MessageIds.READ_PICASSO_DIAGRAM :
					// Generate the Picasso Diagram before calling the property change..
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							
							PicassoConstants.IS_PKT_LOADED = false;
							ByteArrayInputStream bais;

							DiagramPacket dp = null;
							bais = new ByteArrayInputStream(fromServer.compressedDiagramPacket);
							try
							{
								GZIPInputStream gis = new GZIPInputStream(bais);
								ObjectInputStream ois = new ObjectInputStream(gis);
								dp = (DiagramPacket) ois.readObject();
							}
							catch(IOException e)
							{
								e.printStackTrace();
							}
							catch(ClassNotFoundException e)
							{
								e.printStackTrace();
							}
							fromServer.diagramPacket = dp;
							Vector trees = null;
							bais = new ByteArrayInputStream(fromServer.compressedTrees);
							try
							{
								GZIPInputStream gis = new GZIPInputStream(bais);
								ObjectInputStream ois = new ObjectInputStream(gis);
								trees = (Vector) ois.readObject();
							}
							catch(IOException e)
							{
								e.printStackTrace();
							}
							catch(ClassNotFoundException e)
							{
								e.printStackTrace();
							}
							fromServer.trees = trees;
//							dp.copyTrees(trees);
							
							comp.dispWarningMessage(fromServer);
							if(!clientPacket.fromCommandLine)
							{
								if(dp.getQueryPacket().getExecType().equals(PicassoConstants.RUNTIME_DIAGRAM))
									comp.getPParent().setFullExecDiagramPacket(new DiagramPacket(dp));
								else
									comp.getPParent().setFullDiagramPacket(new DiagramPacket(dp)); //apa
							}
							if(fromServer.diagramPacket.getQueryPacket().genSuccess == false && !clientPacket.fromCommandLine)
							{
								// Test this line
								comp.getPParent().getDBSettingsPanel().removeQid(clientPacket.getQueryPacket());
							}
							else if(!clientPacket.fromCommandLine)// it is no longer dummy
							{
								fromServer.diagramPacket.getQueryPacket().dummyEntry = false;
								// QueryPacket current = comp.getPParent().getDBSettingsPanel().getQtDesc();
								// current.dummyEntry = false;
								QueryPacket qq = new QueryPacket(fromServer.diagramPacket.getQueryPacket());
								qq.setExecType(comp.getPParent().getDBSettingsPanel().getQtDesc().getExecType());
								comp.getPParent().getDBSettingsPanel().setQtDescItem(qq);
							}
							comp.drawAllDiagrams(fromServer);
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("msgReceived", 2, 3);
						}
					});
					break;

				case MessageIds.GET_PLAN_TREE :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							PlanTreeFrame planTree = new PlanTreeFrame(clientPacket, comp.getSortedPlan(), fromServer);
							planTree.setVisible(true);
						}
					});
					break;

				case MessageIds.GET_MULTI_PLAN_TREES :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							PlanTreeFrame_DifEngines planTree123 = new PlanTreeFrame_DifEngines(clientPacket, comp.getSortedPlan(), fromServer);
							planTree123.setVisible(true);
						}
					});
					break;

				case MessageIds.GET_COMPILED_PLAN_TREE :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							PlanTreeFrame planTree = new PlanTreeFrame(clientPacket, comp.getSortedPlan(), fromServer);
							planTree.setVisible(true);
						}
					});
					break;

				case MessageIds.GET_ABSTRACT_PLAN :
					if (PicassoConstants.ENABLE_COST_MODEL) {
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								comp.addServerMessage(fromServer);
								comp.firePropertyChange("statusReceived", 2, 3);
								String absPlan = fromServer.absPlan;
								MainPanel mp = comp.getPParent();
								mp.setQueryPlusPlan(clientPacket, absPlan,comp.getSortedPlan());
							}
						});
					}
					break;

				case MessageIds.GET_SERVER_STATUS :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){

							if ( fromServer.queries.size() == 0 ) {
								JOptionPane.showMessageDialog(comp,  "Server is operational\nName: "+serverName
										+"\nPort: "+serverPort,"Status",JOptionPane.INFORMATION_MESSAGE);
							} else {
								ServerStatusFrame serverStatus = new ServerStatusFrame(serverName, serverPort, clientPacket, fromServer);
								serverStatus.setVisible(true);
							}
						}
					});
					break;
				case MessageIds.GET_PLAN_STRINGS :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							((ReducedPlanPanel)comp).setPlans(fromServer);
							comp.setStatus(" ");
						}
					});
					break;
					
				case MessageIds.GET_PLAN_COSTS :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);
							((ReducedPlanPanel)comp).setCosts(fromServer);
							comp.setStatus(" ");
						}
					});
					break;
					
				case MessageIds.GET_ALL_PLAN_COSTS:
					SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						comp.addServerMessage(fromServer);
						comp.firePropertyChange("statusReceived", 2, 3);
						((ReducedPlanPanel)comp).setAllPlanCosts(fromServer);
						comp.setStatus(" ");
					}
				});
				break;
				case MessageIds.TIME_TO_GENERATE_APPROX :
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("statusReceived", 2, 3);							
						}
					});
					break;
				case MessageIds.STATUS_ID :
				case MessageIds.PROCESS_QUEUED :
				case MessageIds.GET_CLIENT_ID :
				case MessageIds.GET_QUERYTEMPLATE_NAMES :
				case MessageIds.TIME_TO_GENERATE :				
				case MessageIds.GET_PLAN_TREES :
				default:
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							comp.addServerMessage(fromServer);
							comp.firePropertyChange("msgReceived", 2, 3);
						}
					});
				
				} //end switch
				
				
				
				if ( messageId == MessageIds.ERROR_ID || fromServer.status.equals(MessageIds.END_MESSAGE)) {
					sock.close();
					MessageUtil.threadcount--;
					return;
					// Remove the process from dbId queue and clientId queue	
				}		
				
				
			} //end while(true)
			
		} catch (EOFException e) {
			// MessageUtil.CPrintToConsole("In EOF Error Message Message ID ::: " + fromServer.messageId);
			// JOptionPane.showMessageDialog(comp,"EOF from Server");
		} catch (SocketException e) {
			// Connection is reset from the server
			JOptionPane.showMessageDialog(comp,"The Picasso Server on " + serverName + " timed out.", "Server timed out", JOptionPane.ERROR_MESSAGE);
			comp.setStatus("STATUS: DONE");
		} catch (Exception e) {
			MessageUtil.CPrintErrToConsole("In error message ");
			e.printStackTrace();
			ServerPacket fromClient = new ServerPacket();
			fromClient.messageId = MessageIds.ERROR_ID;
			fromClient.status = "Client threw an exception";
			comp.addServerMessage(fromClient);
			comp.firePropertyChange("statusReceived", 2, 3);
			comp.processErrorMessage(fromClient);
		}	
	}
	
	public Hashtable parseMessage(String msg) {
		Hashtable nameValuePairs = new Hashtable();
		
		StringTokenizer st = new StringTokenizer(msg, "&");
		
		while ( st.hasMoreTokens() ) {
			String field = st.nextToken();
			StringTokenizer st1 = new StringTokenizer(field, "=");
			String name = st1.nextToken();
			String value = st1.nextToken();
			
			nameValuePairs.put(name, value);
			
			//MessageUtil.CPrintToConsole("Name : " + name + " Value : " + value);
		}
		return(nameValuePairs);
	}
}

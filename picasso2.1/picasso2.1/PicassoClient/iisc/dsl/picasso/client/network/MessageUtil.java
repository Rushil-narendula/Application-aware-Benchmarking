
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

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MessageUtil {
	public static int threadcount = 0;
	
	public static final String sendMessageToServer(String serverName,
								int serverPort, ClientPacket clientPacket,
								PicassoPanel comp) {
		new SendAndReceiveMessage(serverName, serverPort, clientPacket, comp);
		threadcount++;
		
		return("Started a new thread");
	}
	
	public static final void CPrintErrToConsole(String str) {
		if ( !MainPanel.IS_APPLET ) {
		try
		{
			FileWriter fis = new FileWriter (MainPanel.FileName, true);
			fis.write(str + "\n");
			fis.flush();
			fis.close();
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("File not found: "+fnfe);
		}
		catch(IOException ioe)
		{
			System.out.println("IOExceptio: "+ioe);
		}
		}
		System.out.println("CLIENT ERROR :: " + str);
	}
	
	public static final void CPrintToConsole(String str) {
		if ( !MainPanel.IS_APPLET ) {
		try
		{
			FileWriter fis = new FileWriter (MainPanel.FileName, true);
			fis.write(str + "\n");
			fis.flush();
			fis.close();
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("File not found: " + MainPanel.FileName);
		}
		catch(IOException ioe)
		{
			System.out.println("IOExceptio: "+ioe);
		}
		}
		if ( PicassoConstants.IS_CLIENT_DEBUG == true )
			System.out.println("CLIENT :: " + str);
	}
	
	public static final void sendCloseMessageToServer(String serverName, int serverPort, String clientId) {
		ObjectOutputStream    out;
		Socket sock;
		
		try {	
			sock = new Socket(serverName, serverPort);
			out = new ObjectOutputStream(sock.getOutputStream());
			
			ClientPacket clientPacket = new ClientPacket();
			clientPacket.setMessageId(MessageIds.CLOSE_CLIENT);
			clientPacket.setClientId(clientId);
			out.writeObject(clientPacket);
			MessageUtil.CPrintToConsole("Close Object Written");
			out.flush();
		} catch (Exception e) {
			CPrintToConsole("Exception in sendCloseMessageToServer " + e);
		}
	}
	
	public static final void sendDeleteProcessToServer(String serverName, int serverPort, ClientPacket msg) {
		ObjectOutputStream    out;
		Socket sock;
		
		try {	
			sock = new Socket(serverName, serverPort);
			out = new ObjectOutputStream(sock.getOutputStream());
			
			msg.setMessageId(MessageIds.DELETE_PROCESS);
			out.writeObject(msg);
			//MessageUtil.CPrintToConsole("Object Written");
			out.flush();
		} catch (Exception e) {
			CPrintToConsole("Exception in sendDeleteProcessToServer " + e);
		}
	}
}

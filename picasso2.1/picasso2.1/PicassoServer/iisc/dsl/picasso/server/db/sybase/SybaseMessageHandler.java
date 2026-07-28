
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

package iisc.dsl.picasso.server.db.sybase;

import java.sql.SQLException;
import java.util.Vector;
import com.sybase.jdbcx.SybMessageHandler;

public class SybaseMessageHandler implements SybMessageHandler{
	private Vector messages = new Vector();
	private int loc;
	
	public  SQLException messageHandler(SQLException sqe)
	{
		//System.out.print(sqe.getMessage());
		messages.add(sqe.getMessage());
		return  sqe;
	}
	
	public String getMessageLine()
	{
		if(messages.size()<=0 || loc>=messages.size())
			return null;
		return (String)messages.get(loc++);
	}
	
	public Vector getMessageVector()
	{
		return messages;
	}
	
	public void clear()
	{
		messages.clear();
		loc = 0;
	}
}

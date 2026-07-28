
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

import iisc.dsl.picasso.common.ds.QueryPacket;

import java.io.Serializable;
import java.util.Hashtable;

public class ApproxParameters implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	public int SamplingMode;	
	private Hashtable fields;
	public int optClass;
	public boolean FPCMode;
	public ApproxParameters(int Mode) {		
		SamplingMode = Mode;
		fields = new Hashtable(3);
	}
	public void setValue(String desc, int val) {
		fields.put(desc,val+"");
	}	
	public void setValue(String desc, double val) {
		fields.put(desc,val+"");
	}	
	public void setValue(String desc, String val) {
		fields.put(desc,val);
	}
	public String getValue(String desc)
	{
		return(fields.get(desc)+"");
	}
}

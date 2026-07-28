
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

package iisc.dsl.picasso.server.db.datatype;

import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class PReal extends Datatype{
	double value;
	
	public PReal(String v)
	{
		try{
			value = Double.parseDouble(v);
		}catch(NumberFormatException e) {
			ServerMessageUtil.SPrintToConsole("Cannot convert '"+
					v + "' into Double: "+e);
		}
	}

	public long getIntValue()
	{
		return (long)value;
	}
	
	public double getDoubleValue()
	{
		return value;
	}
	
	public String getStringValue()
	{
		return Double.toString(value);
	}
	
	public boolean isLessThan(Datatype d)
	{
		return (value < d.getDoubleValue());
	}
	
	public boolean isEqual(Datatype d)
	{
		return (value == d.getDoubleValue());
	}

	public String interpolate(Datatype d, double scale)
	{
		return Double.toString(value + (d.getDoubleValue() - value) * scale);
	}

	public Datatype minus(Datatype d) 
	{
		return Datatype.makeObject("real",String.valueOf(value-d.getDoubleValue()));
	}

	public double divide(Datatype d) 
	{
		return value/d.getDoubleValue();
	}
}

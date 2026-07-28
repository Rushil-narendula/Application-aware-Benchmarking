
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

import java.util.Date;
import java.text.DateFormat;

public class PDate extends Datatype{
	private long value;
	private java.text.SimpleDateFormat df;
	public PDate(String v)
	{
		df = new java.text.SimpleDateFormat ("yyyy-MM-dd");
	
		try{
			value = df.parse(v).getTime();
		}catch(Exception e){
			value=0;
		}
	}

	public long getIntValue()
	{
		return (long)value;
	}
	
	public double getDoubleValue()
	{
		return (double)value;
	}
	
	public String getStringValue()
	{
		return Long.toString(value);
	}

	public boolean isLessThan(Datatype d)
	{
		if(value<d.getIntValue())
			return true;
		return false;
	}
	
	public boolean isEqual(Datatype d)
	{
		if(value==d.getIntValue())
			return true;
		return false;
	}
	
	public String interpolate(Datatype d, double scale)
	{
		Date date = new Date((long)(value+(d.getIntValue()-value)*scale));
		return df.format(date);
	}

	public Datatype minus(Datatype d) {
		return Datatype.makeObject("date",df.format(new Date(value-d.getIntValue())));
	}

	public double divide(Datatype d) {
		return (double)value/d.getIntValue();
	}
}

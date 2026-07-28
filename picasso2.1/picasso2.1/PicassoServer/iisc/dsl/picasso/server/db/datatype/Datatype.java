
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

public abstract class Datatype {
	private int msSqlFlag=0;
	/*
	 * Shouldn't we make use of AbstractFactory to build right objects
	 * This approach is a little inefficient but removes the need for many classes
	 * without losing any of the advantages of AbstractFactory
	 */
	public void setMsSqlFlag(int set) {
		msSqlFlag=set;
	}
	
	public int getMsSqlFlag() {
		return msSqlFlag;
	}
	
	public static Datatype makeObject(String type, String value)
	{
		Datatype dType = null;
		if(type.equals("integer"))
			dType = new PInteger(value);
		else if(type.equals("real"))
			dType = new PReal(value);
		else if(type.equals("string"))
			dType = new PString(value);
		else if(type.equals("date"))
			dType = new PDate(value);
		else {
			ServerMessageUtil.SPrintToConsole("Unknown Datatype: "+
					type + " value: "+value);
		}
		return dType;
	}
	
	public abstract long getIntValue();
	public abstract double getDoubleValue();
	public abstract String getStringValue();
	
	public abstract boolean isLessThan(Datatype d);
	public abstract boolean isEqual(Datatype d);
	public abstract Datatype minus(Datatype d);
	public abstract double divide(Datatype d);
	public abstract String interpolate(Datatype d,double scale);
}

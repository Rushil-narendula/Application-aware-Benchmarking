
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

package iisc.dsl.picasso.common.db;

import iisc.dsl.picasso.common.DBConstants;

public class DB2Info extends DBInfo {

	private static final long serialVersionUID = -2619718651430502798L;

	public DB2Info()
	{
		name = DBConstants.DB2;
		defaultPort = "50000";
		defaultOptLevel = "5";
		optLevels = new String[] { "Default", "0", "1", "3", "5", "7", "9" };
		treeNames = new String[] {
				"RETURN", "SORT", "TBSCAN", "IXAND", "Dummy", 
				"IXSCAN", "EISCAN", "DUMMY" ,"DUMMY", 
				"NLJOIN", "MSJOIN",  "HSJOIN", "UNION",
				"GRPBY",  "FETCH", "FILTER", "UNIQUE",
				"GENROW", "RIDSCN", "PIPE", "TEMP", "DELETE", "INSERT", "UPDATE"
		};
		treecard = new int[] {
				1,1,1,2,-1,1,1,-1,-1,
				2,2,2,100,1,1,1,1,
				0,1,1,1,1,1,1,1
		};
	}

}

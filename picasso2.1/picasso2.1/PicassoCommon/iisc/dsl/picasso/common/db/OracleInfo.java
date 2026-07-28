
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

public class OracleInfo extends DBInfo {
	
	private static final long serialVersionUID = 5750744422320017330L;

	public OracleInfo()
	{
		name = DBConstants.ORACLE;
		defaultPort = "1521";
		defaultOptLevel = "ALL_ROWS";
		optLevels = new String[] { "Default", "ALL_ROWS", "FIRST_ROW" };
		treeNames = new String[] {
				"SELECT STATEMENT", "SORT", "TABLE ACCESS", "Dummy", "Dummy", 
				"INDEX", "DUMMY", "DUMMY",  "DUMMY", 
				"NESTED LOOPS", "MERGE JOIN", "HASH JOIN", "UNION",  
				"DUMMY","DUMMY", "FILTER", "SEQUENCE",
				"VIEW", "DUMMY" , "CONCATENATION", "COUNT", "DOMAIN INDEX", 
				"FIRST ROW", "INLIST ITERATOR", "INTERSECTION", "MINUS",
				"PARTITION", "REMOTE",  "AND-EQUAL", "CONNECT BY",
				"INSERT STATEMENT", "DELETE STATEMENT", "UPDATE STATEMENT"
		};
		treecard = new int[] {
		    1,1,1,-1,-1,
		    1,-1,-1,-1,
		    2,2,2,2,
		    -1,-1,1,1,
		    1,-1,100,1,1,
		    1,1,2,2,
		    1,1,1,1,
		    1,1,1
		};
	}
}

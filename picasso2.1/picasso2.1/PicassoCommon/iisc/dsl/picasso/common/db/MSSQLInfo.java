
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

public class MSSQLInfo extends DBInfo {

	private static final long serialVersionUID = -4654377106552021504L;

	public MSSQLInfo()
	{
		name = DBConstants.MSSQL;
		defaultPort = "1433";
		optLevels = new String[] { "Default" };
		treeNames = new String[] {
				"SELECT STATEMENT", "Sort", "TABLE ACCESS", "Table Scan","Table Spool",//5
				"CLUSTERED INDEX SCAN", "CLUSTERED INDEX SEEK", "NonCLUSTERED INDEX SCAN", "NonCLUSTERED INDEX SEEK","Nested Loops",//10 
				"Merge Join","HASH JOIN","Union","Stream Aggregate","Fetch Query", //15
				"Filter","Compute Scalar","Hash Match", "Hash Match Root", "Hash Match Team",//20 
				"Constant Scan","Distinct","Distinct Sort",	"Flow Distinct","Cross Join", //25
				"Aggregate","Eager Spool", "Arithmetic Expression", "Assert", "Assign",  //30
				"Full Outer Join", "Inner Join","Left Anti Semi Join", "Left Outer Semi Join", "Left Semi Join",//35 
				"Right Anti Semi Join","Right Outer Semi Join", "Right Semi Join","Bookmark Lookup", "Cache",//40
				"Concatenation", "Convert", "If", "Lazy Spool", "Row Count Spool", //45
				"Segment", "Spool", "Switch", "Top", "Top N Sort",//50
				"While", "Parallelism", "Bitmap","Hash Match - Aggregate", "Hash Match Root - Aggregate", //55
				"Hash Match Team - Aggregate","Insert","Delete","Update","Hash Match - Partial Aggregate", //60
				"Hash Match Root- Partial Aggregate","Hash Match Team- Partial Aggregate"
		};
		treecard = new int[] {
		    1,1,1,1,1,1,1,1,1,
		    2,2,2,2,1,1,1,
		    1,2,2,2,1,1,1,1,2,1,1,1,1,1,
		    2,2,2,2,2,2,2,2,
		    1,1,100,1,1,1,1,1,1,1,1,1,1,1,1,
		    1,1,1,1,1,1,
		    1,1,1
		};
	}
	
}

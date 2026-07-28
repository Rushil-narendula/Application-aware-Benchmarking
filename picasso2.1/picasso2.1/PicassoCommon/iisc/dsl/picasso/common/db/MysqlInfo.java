/*
 # Copyright (C) Indian Institute of Science
 # Bangalore 560012, INDIA
 #
 # This program is part of the PICASSO distribution invented at the
 # Database Systems Lab, Indian Institute of Science. The use of
 # the software is governed by the licensing agreement set up between 
 # the owner, Indian Institute of Science, and the licensee.
 #
 # This program is distributed WITHOUT ANY WARRANTY; without even the 
 # implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 #
 # The public URL of the PICASSO project is
 # http://dsl.serc.iisc.ernet.in/projects/PICASSO/index.html
 #
 # For any issues, contact 
 #       Prof. Jayant R. Haritsa
 #       SERC
 #       Indian Institute of Science
 #       Bangalore 560012, India.
 #       Telephone: (+91) 80 2293-2793
 #       Fax      : (+91) 80 2360-2648
 #       Email: haritsa@dsl.serc.iisc.ernet.in
 #       WWW: http://dsl.serc.iisc.ernet.in/~haritsa
 */
package iisc.dsl.picasso.common.db;

import iisc.dsl.picasso.common.DBConstants;

public class MysqlInfo extends DBInfo{
	private static final long serialVersionUID = -3157705397756034506L;
	public MysqlInfo()
	{
		name = DBConstants.MYSQL;
		defaultPort = "3306";
		optLevels = new String[] { "Default"};
		treeNames = new String[] {
				"SELECT", "SORT", "Table scan", "Index scan", "Index range scan", 
				"Index by reference", "Index by unique reference", "Constant",  "Constant join",
				"Index subquery by IN ref", "Unique subquery by IN ref", "NL Join", "KEY ACCESS",
				"INDEX ACCESS","Block Nested-Loop", "Dummy", "Dummy",
				"Dummy", "Dummy","Dummy", "Dummy", 
				"Dummy", "Dummyg", "Dummy", 
				"Dummy", "Dummy", "Dummy", "Dummy", 
				"Dummyt", "Dummy",
				"Dummy", "Dummy", "Dummy", "Dummy", "Dummy", 
				"Dummy", "Dummy", "Dummy"
		};
		treecard = new int[] {
			    1,1,1,1,1,1,1,1,2,
			    1,1,2,1,1,2,2,
			    2,2,100,2,1,1,1,1,2,1,1,1,1,1,
			    2,2,2,2
			};
	}
	
}
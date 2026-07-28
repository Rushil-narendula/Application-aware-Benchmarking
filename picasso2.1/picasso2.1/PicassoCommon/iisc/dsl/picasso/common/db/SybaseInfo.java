
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

public class SybaseInfo extends DBInfo {

	private static final long serialVersionUID = 4036881879662294324L;

	public SybaseInfo()
	{
		name = DBConstants.SYBASE;
		defaultPort = "5000";
		defaultOptLevel = "allrows_mix";
		optLevels = new String[] { "Default" ,"allrows_dss", "allrows_oltp","allrows_mix"};
		treeNames = new String[] {
				"Emit", "Sort", "TBScan", "RIDScan", "LogScan",
								"IndScan", "DUMMY", "DUMMY",  "DUMMY",
								"NlJoin", "MergeJoin", "HashJoin", "Union",
								"GroupedAggregate","Fetch", "Dummy", "Sequence",
								"SubqJoin", "GroupSorted","GroupHashing", "Scalar",
								"DistinctHashing", "DistinctSorting", "DistinctSorted",
								"NlLeftSemiJoin", "ridjoin", "VectorAggregate", "Distinct",
								"HashDistinct", "HashVectorAggregate",
								"HashUnion", "MergeUnion", "UnionAll", "ScalarAgg", "Restrict",
				"Store", "Sequencer", "Sqfilter",
				"Insert","Update","Delete"
		};
		treecard = new int[] {
		    1,1,1,1,1,
		    1,-1,-1,-1,
		    2,2,2,2,
		    1,1,-1,1,
		    2,1,1,1,
		    1,1,1,
		    2,2,1,1,
		    1,1,
		    2,2,100,1,1,
		    1,1,1,
		    1,1,1
		};
	}
}



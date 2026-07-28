package iisc.dsl.picasso.common.db;

import iisc.dsl.picasso.common.DBConstants;

public class InformixInfo extends DBInfo {
	private static final long serialVersionUID = -3157705397756034506L;

	public InformixInfo()
	{
		name = DBConstants.INFORMIX;
		defaultPort = "7001";
		defaultOptLevel = "Default";
		optLevels = new String[] { "Default" };
		treeNames = new String[] {
				"SELECT STMT", "SEQUENTIAL SCAN", "INDEX PATH", "NESTED LOOP JOIN", "DYNAMIC HASH JOIN", 
		};
		treecard = new int[] {
				1,1,1,2,2
		};
	}
}
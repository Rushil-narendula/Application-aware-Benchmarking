
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

import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.db.DB2Info;
import iisc.dsl.picasso.common.db.OracleInfo;
import iisc.dsl.picasso.common.db.MSSQLInfo;
import iisc.dsl.picasso.common.db.MysqlInfo;
import iisc.dsl.picasso.common.db.PostgresInfo;
import iisc.dsl.picasso.common.db.SybaseInfo;
import iisc.dsl.picasso.common.db.InformixInfo;

public class DBConstants {

	public static final String DB2 = "DB2";
	public static final String ORACLE = "ORACLE";
	public static final String POSTGRES = "POSTGRES";
	public static final String SYBASE = "SYBASE";
	public static final String MSSQL = "SQL SERVER";
	public static final String MYSQL = "MYSQL";
	public static final String INFORMIX = "INFORMIX";
	
	public static final DBInfo databases[] = {
			new DB2Info(),
			new OracleInfo(),
			new PostgresInfo(),
			new MSSQLInfo(),
			new SybaseInfo()
			//, new InformixInfo()
	// Please uncomment the following line to enable Mysql
			, new MysqlInfo()
	};
}

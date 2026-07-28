
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

package iisc.dsl.picasso.common.ds;

import java.io.Serializable;

public class DBSettings implements Serializable{

	private static final long serialVersionUID = 5630120190143651339L;
	
	private String dbInstanceName;	// Name given to this instance
	private String dbVendor;		// Type of database ORACLE, DB2 etc.
	private String serverName;		// Machine Name of database server
	private String serverPort;		// port of database server
	private String databaseName;	// name of database 
	private String schemaName;		// name of schema having catalog tables
	private String userName;			
	private String userPassword;		
	private String optLevel;		// Optimization level
//	private boolean isDefault;		// Do we have default values
	
	public String getInstanceName()
	{
		return dbInstanceName;
	}
	public void setInstanceName(String name)
	{
		this.dbInstanceName = name;
	}
	
	public void setDbVendor(String dbVendor)
	{
		this.dbVendor = dbVendor;
	}
	public String getDbVendor()
	{
		return dbVendor;
	}
	
	public void setServerName(String serverName)
	{
		this.serverName = serverName;
	}
	public String getServerName()
	{
		return serverName;
	}
	
	public void setServerPort(String serverPort)
	{
		this.serverPort = serverPort;
	}
	public String getServerPort()
	{
		return serverPort;
	}
	
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	public String getUserName()
	{
		return userName;
	}
	public void setPassword(String password)
	{
		this.userPassword = password;
	}
	public String getPassword()
	{
		return userPassword;
	}
	
	public void setSchema(String schema)
	{
		this.schemaName = schema;
	}
	public String getSchema()
	{
		return schemaName;
	}
	
	public void setDbName(String dbName)
	{
		this.databaseName = dbName;
	}
	public String getDbName()
	{
		return databaseName;
	}
	
	public void setOptLevel(String optLevel)
	{
		this.optLevel = optLevel;
	}
	public String getOptLevel()
	{
		return optLevel;
	}
}


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

package iisc.dsl.picasso.client.util;

import iisc.dsl.picasso.common.ds.DBSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;

public class PicassoSettings implements Serializable {

	private static final long serialVersionUID = 7906869566539047562L;

	private class DBObject implements Serializable {
		
		private static final long serialVersionUID = -7843280198794250206L;
		Hashtable 	dbSettingsTable;
		String		currentInstance;
	}
	
	DBObject 	dbObj;
	String 		filename;
	
	public PicassoSettings(String name) {
		filename = name;
		readDBSettings();
	}
	
	public void connect (DBSettings settings) {
		dbObj.currentInstance = settings.getInstanceName();
	}
	public void add(DBSettings settings) {
		dbObj.dbSettingsTable.put(settings.getInstanceName(), settings);
		dbObj.currentInstance = settings.getInstanceName();
		writeSettings();
	}
	
	public void delete(String dbInstance) {
		dbObj.dbSettingsTable.remove(dbInstance);
		Object[] keys = dbObj.dbSettingsTable.keySet().toArray();
		if ( keys.length > 0 )
			dbObj.currentInstance = (String)keys[0];
		writeSettings();
	}
	public void edit(String prevInstance, DBSettings dbSettings) {
		dbObj.dbSettingsTable.remove(prevInstance);
		String dbInstance = dbSettings.getInstanceName();
		dbObj.dbSettingsTable.put(dbInstance, dbSettings);
		dbObj.currentInstance = dbInstance;
		writeSettings();
	}
	
	public void setCurrentInstance(String inst) {
		System.out.println(dbObj.currentInstance + " Setting current Instance " + inst);
		dbObj.currentInstance = inst;
		writeSettings();
	}
	
	public String getCurrentInstance() {
		return dbObj.currentInstance;
	}
	
	public Object[] getAllInstances() {
		return ((Object[])dbObj.dbSettingsTable.keySet().toArray());
	}
	
	public DBSettings get(String dbInstance) {
		if ( dbInstance == null )
			return null;
		return((DBSettings)dbObj.dbSettingsTable.get(dbInstance));
	}
	
	
	// function used to write settings file
	public synchronized void writeSettings()  
	{
		try
		{
			FileOutputStream fis = new FileOutputStream (filename);
			ObjectOutputStream ois = new ObjectOutputStream (fis);
			ois.writeObject(dbObj);
			ois.flush();
			ois.close();
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("File not found: "+fnfe);
		}
		catch(IOException ioe)
		{
			System.out.println("IOExceptio: "+ioe);
		}

	}
	
	/* Utility method to read the settings file */
	public void readDBSettings() {
		try 
		{
			FileInputStream fis = new FileInputStream (filename);
			//InputStream is = ClassLoader.getSystemResourceAsStream(filename);
			ObjectInputStream ois = new ObjectInputStream (fis);
			dbObj = (DBObject) ois.readObject ();
			ois.close ();
			
		} catch (Exception e) {
			System.out.println("Warning: Settings file " + filename + " could not be found in the directory " + System.getProperty("user.dir"));
			//e.printStackTrace();
			dbObj = new DBObject();
			dbObj.dbSettingsTable = new Hashtable();
			
		}
	}
	
	
}

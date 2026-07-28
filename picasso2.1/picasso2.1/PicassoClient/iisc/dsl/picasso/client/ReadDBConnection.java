package iisc.dsl.picasso.client;

import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.client.util.PicassoSettings;

import java.io.Serializable;
import java.util.Hashtable;

public class ReadDBConnection {
	private class DBObject implements Serializable {
		
		private static final long serialVersionUID = -7843280198794250206L;
		Hashtable 	dbSettingsTable;
		String		currentInstance;
	}
	
	DBObject 	dbObj;
	String 		filename;
	
	public ReadDBConnection(String fileName) {
		try 
		{
			PicassoSettings ps = new PicassoSettings(fileName);
			
			Object[] objs = ps.getAllInstances();
			for (int i=0; i < objs.length; i++) {
				String instName = (String)objs[i];
				DBSettings db = ps.get(instName);
				System.out.println("========================================");
				System.out.println("Instance Name: " + db.getInstanceName());
				System.out.println("DB Vendor: " + db.getDbVendor());
				System.out.println("DB Name: " + db.getDbName());
				System.out.println("DB Schema: " + db.getSchema());
				System.out.println("Server Name: " + db.getServerName());
				System.out.println("Server Port: " + db.getServerPort());
				System.out.println("User Name: " + db.getUserName());
				System.out.println("User Password: " + db.getPassword());
				System.out.println("========================================");
			}
		} catch (Exception e) {
			System.out.println("Error: Settings file " + filename);
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName="";
		
		if(args.length > 0)
			fileName = args[0];
		else {
			System.out.println("Usage: ReadDBConnection <FileName>");
			return;
		}
		new ReadDBConnection(fileName);
	}

}

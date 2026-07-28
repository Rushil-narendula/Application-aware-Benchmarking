
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

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.common.PicassoConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFileChooser;

public class QueryLoader {

//	 This function is used to get query from file
    public static String getText(MainPanel parent) {
			JFileChooser chooser = new JFileChooser(new File(PicassoConstants.INPUT_QUERY_FOLDER));
			int returnVal = chooser.showOpenDialog(parent);

			if (returnVal != JFileChooser.APPROVE_OPTION)
				return "";
			String path = "";
				path = chooser.getCurrentDirectory() + "";
			return read(path + System.getProperty("file.separator")
					+ chooser.getSelectedFile().getName());
	}
    
//  this function is used to read file used by previous function
	public static String read(String filename) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String str1 = "";
			String str;

			while ((str = in.readLine()) != null)
				str1 = str1 + str + "\n";
			in.close();
			return str1;
		} catch (IOException e) {
		}
		return "";
	}
	
	// Remove the white spaces from the text given and generate a hash code
	// for the query text
	public static String getQueryName(String query) {
		String str = query.replaceAll("\\s", "");
		int hashCode = str.hashCode();
		return("" + hashCode);
	}
}

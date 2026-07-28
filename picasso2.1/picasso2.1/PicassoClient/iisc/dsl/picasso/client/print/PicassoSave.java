
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

package iisc.dsl.picasso.client.print;

import iisc.dsl.picasso.client.frame.PlanTreeFrame;
import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.SelectivityPanel;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JFileChooser;
import visad.DisplayImpl;

import javax.imageio.*;

//Do not use the following packages
//import sun.awt.image.codec.JPEGImageEncoderImpl;
//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGEncodeParam;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class PicassoSave {
	
	public static void save(PlanTreeFrame ptf) {
		String imageDir = PicassoConstants.INPUT_IMAGE_FOLDER;
		JFileChooser chooser = new JFileChooser(new File(imageDir));
		chooser.setFileFilter(new ImageFilter());
		chooser.setAcceptAllFileFilterUsed(false);
		
		//Add custom icons for file types.
		chooser.setFileView(new ImageView());
		
		int returnVal = chooser.showSaveDialog(ptf);
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return;
		
		String path = chooser.getCurrentDirectory() + "";
		String fName = path + System.getProperty("file.separator")
		+ chooser.getSelectedFile().getName();
		//String fileName = chooser.getSelectedFile().getName();
		String ext = PicassoUtil.getExtension(fName);
		
		try {
			if ( !ext.equalsIgnoreCase("jpeg") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("png"))
				fName += ".jpeg";
			
			ext = PicassoUtil.getExtension(fName);

			if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg"))
				saveGraphImageAsJPEG(ptf, fName,true);
			else
				saveGraphImageAsJPEG(ptf, fName,false);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void saveAD(PlanTreeFrame ptf,int planNum,String fName) {				/*String path = "E:\\PlanTrees\\";		String fName = path + "Plan"+(planNum+1)+".jpeg"; 					String ext = "jpeg";*/		/*String imageDir = PicassoConstants.INPUT_IMAGE_FOLDER;
		JFileChooser chooser = new JFileChooser(new File(imageDir));
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);		
		chooser.setAcceptAllFileFilterUsed(false);
		    
	    if (chooser.showOpenDialog(ptf) != JFileChooser.APPROVE_OPTION) {	    
	    	return;
	    }		
		
		String path = chooser.getCurrentDirectory() + "";
		String fName = path + System.getProperty("file.separator") + chooser.getSelectedFile();*/
		String ext = "jpeg";		fName = fName + "plan"+(planNum+1)+".jpeg";		try {			if ( !ext.equalsIgnoreCase("jpeg") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("png"))				fName += ".jpeg";			ext = PicassoUtil.getExtension(fName);			if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg"))				saveGraphImageAsJPEG(ptf, fName,true);			else				saveGraphImageAsJPEG(ptf, fName,false);		}		catch (Exception e) {			e.printStackTrace();		}		ptf.dispose();	}
	public static void saveGraphImageAsJPEG(PlanTreeFrame ptf, String fName, boolean jpegornot) {
		BufferedImage image = new BufferedImage(ptf.getWidth(), 
				ptf.getHeight(),BufferedImage.TYPE_INT_RGB);
		
		Graphics g2d = image.createGraphics();
		g2d.translate (0, 0);
		PicassoUtil.disableDoubleBuffering (ptf);
		ptf.paint(g2d);
		PicassoUtil.enableDoubleBuffering (ptf);
		
		try {
			File fil = new File(fName);
			if(jpegornot)
			{
			FileOutputStream fos = new FileOutputStream(fil);
			
			//JPEGImageEncoderImpl jpeg = new JPEGImageEncoderImpl(fos);
			//jpeg.encode(image);
			ImageIO.write(image, "JPEG", fos);
			fos.close();
			}
			else
			{
				javax.imageio.ImageIO.write(image, "png", fil);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void save(MainPanel mp) {
		JFileChooser chooser = new JFileChooser(new File(PicassoConstants.INPUT_IMAGE_FOLDER));
		chooser.setFileFilter(new ImageFilter());
		chooser.setAcceptAllFileFilterUsed(false);
		
		//Add custom icons for file types.
		chooser.setFileView(new ImageView());
		
		int returnVal = chooser.showSaveDialog(mp);
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return;
		
		String path = chooser.getCurrentDirectory() + "";
		String fName = path + System.getProperty("file.separator")
		+ chooser.getSelectedFile().getName();
		String ext = PicassoUtil.getExtension(fName);
		
		try {
			if ( !ext.equalsIgnoreCase("jpeg") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("png"))
				fName += ".jpeg";
			
			ext = PicassoUtil.getExtension(fName);

			if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg"))
				saveImageAsJPEG(mp, fName, chooser.getSelectedFile().getName(),true);
			else
				saveImageAsJPEG(mp, fName, chooser.getSelectedFile().getName(),false);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void saveImageAsJPEG(MainPanel mp, String fName, String fileName, boolean jpegornot) throws IOException {	
		DisplayImpl display = mp.getCurrentTab().getDisplayImage();
		
		if ( display != null ) {
			if(jpegornot)
				saveVisadImageJPG(mp, display, fName);
			else
				saveVisadImagePNG(mp, display, fName);
		} else {
			if ( mp.getCurrentTab() instanceof SelectivityPanel ) {
				saveSelectivityLogAsTxt(mp, mp.getCurrentDir() + "\\PicassoRun\\Logs\\" + fileName+".log");
			}
			saveOtherImage(mp, fName, jpegornot);
		}
	}
	
	static void saveSelectivityLogAsTxt(MainPanel mp, String fName) {
		ServerPacket sp = mp.getServerPacket();
		
		DiagramPacket gdp = sp.diagramPacket;
		try
		{
			FileWriter fis = new FileWriter(fName, false);
			
			String[] relNames = gdp.getRelationNames();
			String[] attrNames = gdp.getAttributeNames();
			
			float[] picassoSelec = gdp.getPicassoSelectivity();
			float[] planSelec = gdp.getPlanSelectivity();
			float[] predSelec = gdp.getPredicateSelectivity();
			String[] constants = gdp.getConstants();
			
			DecimalFormat df = new DecimalFormat("0.00");
			df.setMaximumFractionDigits(2);
			for(int k=0;k<gdp.getDimension();k++){//rss
			int res = gdp.getResolution(k);//rss
			fis.write("QTD: " + sp.queryPacket.getQueryName() +"\n");
			for ( int i=0; i < relNames.length; i++ ) {
				fis.write("\n-------------------------------------------------------\n");
				fis.write("Relation: " + relNames[i] + "   Attribute: " + attrNames[i] + "\n");
				fis.write("-------------------------------------------------------\n");
				fis.write("PicSel\tConstant\t\t\tPredSel\tPlanSel");
				fis.write("\n");
				for (int j=0; j < res; j++) {
					String constStr = "    " + constants[i*res+j];
					if ( constStr.length() < 25 ) {
						for (int l=constStr.length(); l < 25; l++)
							constStr += " ";
					}
					fis.write(df.format(picassoSelec[i*res+j]) 
							+ "\t" + constStr
							+ "\t" + df.format(predSelec[i*res+j])
							+ "\t\t" + df.format(planSelec[i*res+j])
							+ "\n");
				}
			}
			}
			fis.flush();
			fis.close();
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("File not found: "+fnfe);
		}
		catch(IOException ioe)
		{
			System.out.println("I/O Exception: "+ioe);
		}
	}
	
	public static void saveOtherImage(MainPanel mp, String fName, boolean jpegornot) {
		BufferedImage image = new BufferedImage(mp.getWidth(), 
				mp.getHeight(),BufferedImage.TYPE_INT_RGB);
		
		Graphics g2d = image.createGraphics();
		g2d.translate (0, 0);
		PicassoUtil.disableDoubleBuffering (mp);
		mp.paint(g2d);
		PicassoUtil.enableDoubleBuffering (mp);
		
		try {
			File fil = new File(fName);
			if(jpegornot)
			{
			FileOutputStream fos = new FileOutputStream(fil);
			//JPEGImageEncoderImpl jpeg = new JPEGImageEncoderImpl(fos);
			//jpeg.encode(image);
			ImageIO.write(image, "JPEG", fos);
			fos.close();
			}
			else
			{
				javax.imageio.ImageIO.write(image, "png", fil);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void saveVisadImageJPG(MainPanel mp, DisplayImpl display, String fName) {
		final DisplayImpl disp = display;
		final File fn = new File(fName);
		final MainPanel comp = mp;
		final int compX = mp.getCurrentTab().getX();
		final int compY = mp.getCurrentTab().getVisadY();
		
		final BufferedImage image = new BufferedImage(mp.getWidth(), 
				mp.getHeight(),BufferedImage.TYPE_INT_RGB);
		
		final Graphics g2d = image.createGraphics();
		g2d.translate (0, 0);
		PicassoUtil.disableDoubleBuffering (mp);
		mp.paint(g2d);
		PicassoUtil.enableDoubleBuffering (mp);
		
		//MessageUtil.CPrintToConsole("In Save Visad Image ");
		
		Runnable savedisp = new Runnable() {
			public void run() {
				BufferedImage visadImage = disp.getImage(false);
				g2d.drawImage(visadImage, compX, compY, comp);
				//MessageUtil.CPrintToConsole("In Runnable Visad Image ");
				try {
					//JPEGEncodeParam param = JPEGCodec.getDefaultJPEGEncodeParam(image);
					//param.setQuality(1.0f, true);
					FileOutputStream fout = new FileOutputStream(fn);
					//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(fout);
					//encoder.encode(image, param);
					ImageIO.write(image, "JPEG", fout);
					fout.close();
				}
				catch (Exception err) {
					System.err.println("Error whilst saving JPEG: "+err);
					System.out.println("Error whilst saving JPEG: "+err);
				}
			}
		};
		Thread t = new Thread(savedisp);
		t.start();
	}
	
	public static void saveVisadImagePNG(MainPanel mp, DisplayImpl display, String fName) {
		final DisplayImpl disp = display;
		final File fn = new File(fName);
		final MainPanel comp = mp;
		final int compX = mp.getCurrentTab().getX();
		final int compY = mp.getCurrentTab().getVisadY();
		
		final BufferedImage image = new BufferedImage(mp.getWidth(), 
				mp.getHeight(),BufferedImage.TYPE_INT_RGB);
		
		final Graphics g2d = image.createGraphics();
		g2d.translate (0, 0);
		PicassoUtil.disableDoubleBuffering (mp);
		mp.paint(g2d);
		PicassoUtil.enableDoubleBuffering (mp);
		
		//MessageUtil.CPrintToConsole("In Save Visad Image ");
		
		Runnable savedisp = new Runnable() {
			public void run() {
				BufferedImage visadImage = disp.getImage(false);
				g2d.drawImage(visadImage, compX, compY, comp);
				//MessageUtil.CPrintToConsole("In Runnable Visad Image ");
				try {
					javax.imageio.ImageIO.write(image, "png", fn);
				}
				catch (Exception err) {
					System.err.println("Error whilst saving JPEG: "+err);
					System.out.println("Error whilst saving JPEG: "+err);
				}
			}
		};
		Thread t = new Thread(savedisp);
		t.start();
	}
}

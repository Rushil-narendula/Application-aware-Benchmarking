
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
import iisc.dsl.picasso.client.panel.PrintDiagramPanel;
import iisc.dsl.picasso.client.panel.PrintTreePanel;
import iisc.dsl.picasso.client.util.PicassoUtil;
//import iisc.dsl.picasso.common.PicassoConstants;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
//import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
//import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import visad.DisplayImpl;

public class PicassoPrint implements Printable, ActionListener {

	MainPanel 		mainPanel;
	BufferedImage	visadImage;
	BufferedImage   planImage;
	PrinterJob 		printJob;
	PlanTreeFrame	planTree;
	public static int 			pageCount;
	int				printType; // Is it print or preview
	PageFormat 		pageFormat;
	PageFormat 		modPageFormat;
	JButton 		printButton, zoomin, zoomout;
	static JButton prevButton;
	static JButton nextButton;
	static JLabel			pageLabel;
	int				previewPageIndex;
	
	public static final int PRINT_TYPE = 0;
	public static final int PREVIEW_TYPE = 1;
	
	public PicassoPrint(MainPanel mp, int type) {
		mainPanel = mp;
		printType = type;
		
		//MessageUtil.CPrintToConsole("In Print Function");
		
		final DisplayImpl display = mainPanel.getCurrentTab().getDisplayImage();
		
		
		if ( display != null ) {
			Runnable savedisp = new Runnable() {
			      public void run() {
			        visadImage = display.getImage(false);
			        //MessageUtil.CPrintToConsole("In Runnable Visad Image ");
			    }
			};
			Thread t = new Thread(savedisp);
			t.start();
		} else visadImage = null;
		
		// Create a printerJob object
		printJob = PrinterJob.getPrinterJob ();
		 
		// Set the printable class to this one since we
		// are implementing the Printable interface
		printJob.setPrintable (this);
		printJob.setJobName("PICASSO PRINT");
		Book book = new Book();
		pageFormat = printJob.defaultPage();
		pageFormat.setOrientation(PageFormat.LANDSCAPE);
		modPageFormat = printJob.pageDialog(pageFormat);
		    pageCount = 1;
	    book.append (this, modPageFormat, pageCount);
		printJob.setPageable(book);
	}
	

	public int checkPages(PlanTreeFrame planf, int pgCount){
		
		if (pgCount > 1){
		BufferedImage image = planf.getTreeImage(pgCount - 2);
		BufferedImage image1 = planf.getTreeImage(pgCount - 1);
			
			if(image.getWidth() + image1.getWidth() < 600) {
				pgCount=1;
			}
		}
return pgCount;
	}
	
	
	

	public PicassoPrint(PlanTreeFrame planf) {
		planTree = planf;
		mainPanel = null;
		
		//MessageUtil.CPrintToConsole("In Print Function");
		
		// Create a printerJob object
		printJob = PrinterJob.getPrinterJob ();
		 
		// Set the printable class to this one since we
		// are implementing the Printable interface
		printJob.setPrintable (this);
		printJob.setJobName("PICASSO PRINT");
		Book book = new Book();
		
		pageFormat = printJob.defaultPage();
		pageFormat.setOrientation(PageFormat.LANDSCAPE);
		// pageFormat = printJob.pageDialog(pageFormat);
		modPageFormat = printJob.pageDialog(pageFormat);
		
	    pageCount = planTree.getTreeCount();
	    pgChk=checkPages(planf,pageCount);
	   book.append (this, modPageFormat, pgChk);
		printJob.setPageable(book);
	}
	
	public void printPicasso() {
		
		if(pageFormat.equals(modPageFormat))
			return;
		// Show a print dialog to the user. If the user
		// clicks the print button, then print, otherwise
		// cancel the print job
		if (printJob.printDialog()) {
			 try {
				 printJob.print();
			 } catch (Exception PrintException) {
				 PrintException.printStackTrace();
			 }
		 }
	}
	
	double scale = 1;
	JFrame previewFrame;
	JPanel printPanel;
	static JPanel topPanel;
	static int pgChk;
	
	public void previewPicasso() {
		
		if(pageFormat.equals(modPageFormat))
			return;
		
		previewFrame = new JFrame("Print Preview");
		previewFrame.getContentPane().setLayout(new BorderLayout());
		
		scale = 1.2;
		previewPageIndex = 1;
		if ( mainPanel == null && planTree != null ) {
			printPanel = new PrintTreePanel(planTree, 0, scale, pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
		} else {
			printPanel = new PrintDiagramPanel(mainPanel, visadImage, scale, pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
			((PrintDiagramPanel)printPanel).setInsets(new Insets(10,10,0,0));
		}
		
		topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout());
		printButton = new JButton("Print");
		printButton.addActionListener(this);
		prevButton = new JButton("Prev");
		nextButton = new JButton("Next");
		prevButton.addActionListener(this);
		nextButton.addActionListener(this);
		pageLabel = new JLabel("Page " + previewPageIndex + " of " + pageCount);
		
	
		topPanel.add(printButton);
		
		
		//	zoomout.setEnabled(false);
		
		previewFrame.setResizable(false);
		previewFrame.setSize((int)pageFormat.getImageableWidth()+20, (int)pageFormat.getImageableHeight()+topPanel.getHeight()+70);
		previewFrame.getContentPane().add(topPanel, BorderLayout.NORTH);
		previewFrame.getContentPane().add(printPanel, BorderLayout.CENTER);
		previewFrame.setVisible(true);
	}
	
	public static void addNavButtons() 
	{
		if ( pageCount > 1 )
			topPanel.add(prevButton);
		
		topPanel.add(pageLabel);
		
		if ( pageCount > 1)
			topPanel.add(nextButton);
	}
	

	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

		//MessageUtil.CPrintToConsole(pageIndex + " Page Count :: " + pageCount);
		if (pageIndex > pageCount) {
			return (NO_SUCH_PAGE);
		}
		else {
			
			scale = 1;
			//MessageUtil.CPrintToConsole("Scale :: " + scale);
			Graphics2D g2d = (Graphics2D)graphics;
			g2d.translate (pageFormat.getImageableX (), pageFormat.getImageableY ());
			if ( mainPanel == null && planTree != null ) {
				PrintTreePanel thisPrintTree = new PrintTreePanel(planTree, pageIndex, scale, pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
				PicassoUtil.disableDoubleBuffering (thisPrintTree);
				thisPrintTree.paint (g2d);
				PicassoUtil.enableDoubleBuffering (thisPrintTree);
			} else {
				PrintDiagramPanel thisPrintPanel = new PrintDiagramPanel(mainPanel, visadImage, scale, pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
				PicassoUtil.disableDoubleBuffering (thisPrintPanel);
				thisPrintPanel.paint (g2d);
				PicassoUtil.enableDoubleBuffering (thisPrintPanel);
			}
			
			//pageFormat.setOrientation(PageFormat.LANDSCAPE);
			//Paper paper = pageFormat.getPaper();
			//MessageUtil.CPrintToConsole(paper.getImageableWidth() + " Paper Attrs :: " + paper.getImageableHeight());
			//paper.setImageableArea(0, 0, mainPanel.getWidth(), mainPanel.getHeight());
			return (PAGE_EXISTS);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == printButton ) {
			if(PrintTreePanel.pgFlag){pageCount = 1;}
			this.printPicasso();
		} else if ( e.getSource() == prevButton ) {
			previewPageIndex--;;
			redrawPreview();	
		} else if ( e.getSource() == nextButton ) {
			previewPageIndex++;
			redrawPreview();
		}
	}
	
	void redrawPreview() {
		//previewFrame.remove(printPanel);
		//MessageUtil.CPrintToConsole("In Redraw preview");
		int width = previewFrame.getWidth()-20+1;
		int height = previewFrame.getHeight()-topPanel.getHeight()-70+1;
		
		if ( mainPanel == null && planTree != null ) {
			((PrintTreePanel)printPanel).setPaperSettings(previewPageIndex-1, scale, width, height);
		} else {
			((PrintDiagramPanel)printPanel).setPaperSettings(scale, (pageFormat.getImageableWidth()*scale), (pageFormat.getImageableHeight()*scale));
			((PrintDiagramPanel)printPanel).setInsets(new Insets(10,10,10,10));
		}
		
		if ( previewPageIndex == pageCount ) {
			prevButton.setEnabled(true);
			nextButton.setEnabled(false);
		} else if ( previewPageIndex == 1 ) {
			prevButton.setEnabled(false);
			nextButton.setEnabled(true);
		} else {
			prevButton.setEnabled(true);
			nextButton.setEnabled(true);
		}

		//MessageUtil.CPrintToConsole("In Redraw after enable and disable");
		
		//int width = (int)(pageFormat.getImageableWidth()*scale);
		//int height = (int)(pageFormat.getImageableHeight()*scale);
		
		//previewFrame.setResizable(true);
		previewFrame.getContentPane().setSize(width+20, height+topPanel.getHeight()+70);
		previewFrame.setSize(width+20, height+topPanel.getHeight()+70);
		printPanel.setSize(width, height);
		previewFrame.getContentPane().repaint(0, 0, 0, width, height);
		
		
		pageLabel.setText("Page " + previewPageIndex + " of " + pageCount);
	}
}

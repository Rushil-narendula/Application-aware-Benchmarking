
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
package iisc.dsl.picasso.client;import iisc.dsl.picasso.client.frame.AboutFrame;import iisc.dsl.picasso.client.network.MessageUtil;import iisc.dsl.picasso.client.panel.MainPanel;import iisc.dsl.picasso.client.panel.QueryBuilderPanel;import iisc.dsl.picasso.client.panel.SelectivityPanel;import iisc.dsl.picasso.client.print.PicassoPrint;import iisc.dsl.picasso.client.print.PicassoSave;import iisc.dsl.picasso.common.PicassoConstants;import java.awt.event.ActionEvent;import java.awt.event.ActionListener;import javax.swing.JApplet;import javax.swing.JMenu;import javax.swing.JMenuBar;import javax.swing.JMenuItem;import javax.swing.JOptionPane;import javax.swing.SwingUtilities;

public class Picasso_Applet extends JApplet {	//	 Auto Generated Serial ID	private static final long serialVersionUID = 2881159848139303053L;
	private MainPanel mainPanel;
	JMenuBar				pMenuBar;	JMenuItem 				about, exit, print, preview, save, contentHelp,							newDB, editDB, deleteDB,							refreshQ, deleteQ, renameQ, aboutS, cleanupS, deltabS;//, selecQ;	String					serverName, serverPort, currentDir;		public void init() 	{
        //Execute a job on the event-dispatching thread:        //creating this applet's GUI.        try         {            SwingUtilities.invokeAndWait(new Runnable()             {                public void run()                 {                	pMenuBar = new JMenuBar();    
                	JMenu file = new JMenu("File");            	    JMenu help = new JMenu("Help"); 	
            	    exit = new JMenuItem("Exit");            	    print = new JMenuItem("Print");            	    preview = new JMenuItem("Print Preview");            	    save = new JMenuItem("Save");
       	    
            	    JMenu database = new JMenu("Database");
            	    JMenu query = new JMenu("QueryTemplate");
            	    JMenu server = new JMenu("Server");
            	    
            	    newDB = new JMenuItem("New");
            	    editDB = new JMenuItem("Edit");
            	    deleteDB = new JMenuItem("Delete");
            	    
            	    refreshQ = new JMenuItem("Refresh QueryTemplate List");
            	    deleteQ = new JMenuItem("Delete QueryTemplate");
            	    //selecQ = new JMenuItem("Selectivity Error Log");
            	    renameQ = new JMenuItem("Rename QueryTemplate"); //ma
            	    
            	    aboutS = new JMenuItem("Status");
            	    //processS = new JMenuItem("Check Processes");
            	    cleanupS = new JMenuItem("Cleanup Picasso Tables");
            	    deltabS = new JMenuItem("Destroy Picasso Database");
            	    
            	    /*edit.add(database);
            	    edit.add(query);
            	    edit.add(server);*/
            	    database.add(newDB);
            	    database.add(editDB);
            	    database.add(deleteDB);
            	    query.add(refreshQ);
            	    query.add(deleteQ);
            	    query.add(renameQ);
            	    //query.add(selecQ);
            	    //server.add(processS);
            	    server.add(aboutS);
            	    server.add(cleanupS);
            	    server.add(deltabS);
            	    
            	    about = new JMenuItem("About Picasso");
            	    contentHelp = new JMenuItem("Usage Guide");
            	    
            	    help.add(about);
            	    help.add(contentHelp);
            	    
            	    file.addSeparator();
            	    file.add(save);
            	    file.add(print);
            	    file.add(preview);
            	    file.addSeparator();
            	    file.add(exit);
            	    pMenuBar.add(file);
            	    pMenuBar.add(database);
            	    pMenuBar.add(query);
            	    pMenuBar.add(server);
            	    pMenuBar.add(help);
            	    setJMenuBar(pMenuBar);
            	    
            	    about.addActionListener(actionListener);
            	    save.addActionListener(actionListener);
            	    print.addActionListener(actionListener);
            	    preview.addActionListener(actionListener);
            	    exit.addActionListener(actionListener);
            	    contentHelp.addActionListener(actionListener);
            	    newDB.addActionListener(actionListener);
            	    editDB.addActionListener(actionListener);
            	    deleteDB.addActionListener(actionListener);
            	    refreshQ.addActionListener(actionListener);
            	    deleteQ.addActionListener(actionListener);
            	    renameQ.addActionListener(actionListener);
            	    //processS.addActionListener(actionListener);
            	    aboutS.addActionListener(actionListener);
            	    cleanupS.addActionListener(actionListener);
            	    deltabS.addActionListener(actionListener);

            	    serverName = getParameter("ServerName");
            	    serverPort = getParameter("ServerPort");
            	    currentDir = getParameter("CurrentDir");
                	mainPanel = new MainPanel(serverName, serverPort);
                	getContentPane().add(mainPanel);
                }
            });
        } catch (Exception e) {
            System.err.println("createAppletGUI didn't successfully complete ");
            System.out.println("createAppletGUI didn't successfully complete ");
            e.printStackTrace();
        }
        
    }
	
	public void start() {
        MessageUtil.CPrintToConsole("In Start");
    }

    public void stop() {
    	MessageUtil.CPrintToConsole("In Stop");
    	//mainPanel.sendCloseMessage();
    }
    
    ActionListener actionListener = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		Object source = e.getSource();
    		if ( source == about )
    			new AboutFrame(serverName, serverPort, currentDir, false).setVisible(true);
    		else if ( source == exit )
    			System.exit(0);
    		else if ( source == print ) {
    			if ( mainPanel.getCurrentTab() instanceof QueryBuilderPanel || 
    					mainPanel.getCurrentTab() instanceof SelectivityPanel ) {
    				JOptionPane.showMessageDialog(null, "Print supported only for diagrams.","Information",JOptionPane.INFORMATION_MESSAGE);
    			} else
    				new PicassoPrint(mainPanel, PicassoPrint.PRINT_TYPE).printPicasso();
    		}
    		else if ( source == preview ) {
    			if ( mainPanel.getCurrentTab() instanceof QueryBuilderPanel || 
    					mainPanel.getCurrentTab() instanceof SelectivityPanel ) {
    				JOptionPane.showMessageDialog(null, "Print supported only for diagrams.","Information",JOptionPane.INFORMATION_MESSAGE);
    			} else
    				new PicassoPrint(mainPanel, PicassoPrint.PRINT_TYPE).previewPicasso();
    		} else if ( source == save )
    			PicassoSave.save(mainPanel);
    		else if ( source == contentHelp )
    			mainPanel.help();
    		else if ( source == newDB )
    			mainPanel.doMenuAction(PicassoConstants.NEW_DB_INSTANCE);
    		else if ( source == editDB )
    			mainPanel.doMenuAction(PicassoConstants.EDIT_DB_INSTANCE);
    		else if ( source == deleteDB )
    			mainPanel.doMenuAction(PicassoConstants.DELETE_DB_INSTANCE);
    		else if ( source == refreshQ )
    			mainPanel.doMenuAction(PicassoConstants.GET_DIAGRAM_LIST);
    		else if ( source == deleteQ )
    			mainPanel.doMenuAction(PicassoConstants.DELETE_DIAGRAM);
    		else if ( source == renameQ )
    			mainPanel.doMenuAction(PicassoConstants.RENAME_DIAGRAM);
    		/*else if ( source == processS )
    			mainPanel.doMenuAction(PicassoConstants.CHECK_SERVER);*/
    		else if ( source == aboutS )
    			mainPanel.doMenuAction(PicassoConstants.CHECK_SERVER);
    		else if ( source == cleanupS )
    			mainPanel.doMenuAction(PicassoConstants.CLEAN_PICDB);
    		else if ( source == deltabS )
    			mainPanel.doMenuAction(PicassoConstants.DELETE_PICDB);
    	}
    };
}


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

package iisc.dsl.picasso.client;

import iisc.dsl.picasso.client.frame.AboutFrame;
import iisc.dsl.picasso.client.frame.AboutPicassoFrame;

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.panel.QueryBuilderPanel;
import iisc.dsl.picasso.client.panel.SelectivityPanel;
import iisc.dsl.picasso.client.panel.ReducedPlanPanel;
import iisc.dsl.picasso.client.panel.PicassoPanel;
/*import iisc.dsl.picasso.client.print.ImageFilter;
import iisc.dsl.picasso.client.print.ImageView;*/
import iisc.dsl.picasso.client.print.PicassoPrint;
import iisc.dsl.picasso.client.print.PicassoSave;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.PicassoSettingsManipulator;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

public class Picasso_Frame extends JFrame {
	private static final long serialVersionUID = -3096444189571968747L;

	private MainPanel 		mainPanel;
	static private MainPanel m;

	JMenuBar				pMenuBar;
	JMenuItem 				about, exit, print, preview, save, savepacket, loadPacket, settings, contentHelp,
	connDB, newDB, editDB, deleteDB, keymap, docHome,
	refreshQ, deleteQ, renameQ, connectP, statusS, cleanupS, deltabS, shutdownS;//, selecQ;
	String					serverName, serverPort, currentDir;
	
	JFrame mouseMap;
	Font 				font = new Font("Tahoma", Font.PLAIN, 14);
	String mouseMapStr=  "\n\n    Plan Diagram | Reduced Plan Diagram controls:\n"
		+ "              Left-click drag: Pan \n"
		+ "              Shift + Left-click: Zoom - In (drag up) | Out (drag down)\n"
		+ "              Right-click: Schematic Plan Tree\n"
		+ "              Right-click drag drop: Plan Difference\n"
		+ "              Shift + Right-click: Selectivities, Constants, Plan#, Cost, Card \n\n"
		+ "      Additional Plan Diagram controls: \n"
        + "              Ctrl + Right-click: Compiled Plan Tree\n"
		+ "              Alt + Right-click: Abstract Plan-based Plan Diagram \n"
		+ "              Ctrl + Alt + Right-click: Foreign Plan Tree\n\n"
		+ "      Compilation Diagram | Execution Diagram controls:\n"
		+ "              Left-click drag: (2D) Pan | (3D) Rotate \n"
		+ "              Ctrl + Left-click drag: (3D) Pan\n"
		+ "              Shift + Left-click: Zoom - In (drag up) | Out (drag down)\n\n"
		+ "      Plan Legend controls:\n"
		+ "              Click: Schematic Plan Tree\n"
		+ "              Click drag drop: Plan Difference\n" 
		+ "              Alt + Right-click: Abstract Plan-based Plan Diagram";
	JTextArea mouseMapLabel = new JTextArea(mouseMapStr);
	
	public Picasso_Frame(String sn, String sp, String cwd) {
		this.setName("Picasso Database Query Optimizer Visualizer 2.1");
		this.setTitle("Picasso Database Query Optimizer Visualizer 2.1");

		serverName = sn;
		serverPort = sp;
		currentDir = cwd;

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}

		this.setSize(1024, 738);
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		this.setMaximizedBounds(env.getMaximumWindowBounds());
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		pMenuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		//JMenu edit = new JMenu("Edit");
		JMenu help = new JMenu("Help");

		exit = new JMenuItem("Exit");
		print = new JMenuItem("Print Diagram");
		preview = new JMenuItem("Print Preview");
		save = new JMenuItem("Save Screen");
		loadPacket = new JMenuItem("Load Packet");
		savepacket = new JMenuItem("Save Packet");
		settings = new JMenuItem("Local Settings");
		
		JMenu database = new JMenu("DBConnection");
		JMenu server = new JMenu("PicassoServer");
		JMenu client = new JMenu("PicassoClient");
		JMenu query = new JMenu("QueryTemplate");
		
		connDB = new JMenuItem("Connect");
		newDB = new JMenuItem("New");
		editDB = new JMenuItem("Edit");
		deleteDB = new JMenuItem("Delete");

		refreshQ = new JMenuItem("Refresh QueryTemplate List");
		deleteQ = new JMenuItem("Delete QueryTemplate");
		//selecQ = new JMenuItem("Selectivity Error Log");
		renameQ = new JMenuItem("Rename QueryTemplate");

		//aboutS = new JMenuItem("About Server");
		connectP = new JMenuItem("Connect to PicassoServer");
		statusS = new JMenuItem("Server Status");
		cleanupS = new JMenuItem("Cleanup Picasso Tables");
		deltabS = new JMenuItem("Destroy Picasso Database");
		shutdownS = new JMenuItem("Shutdown Picasso Server");

		/*edit.add(database);
		 edit.add(query);
		 edit.add(server);*/
		database.add(connDB);
		database.add(newDB);
		database.add(editDB);
		database.add(deleteDB);
		database.add(deltabS);
		query.add(refreshQ);
		query.add(deleteQ);
		query.add(renameQ);
		//query.add(selecQ);
		server.add(connectP);
		server.add(statusS);
		//server.add(aboutS);
		//server.add(cleanupS);
		//server.add(deltabS);
		//server.add(shutdownS);
		client.add(settings);

		about = new JMenuItem("About Picasso");
		contentHelp = new JMenuItem("Usage Guide");
		keymap = new JMenuItem("Mouse-Key Mappings");
		docHome = new JMenuItem("Documentation Home");
		
		help.add(docHome);
		help.add(contentHelp);
		help.add(keymap);
		help.add(about);

		file.addSeparator();
		file.add(save);
		file.add(savepacket);
		file.add(loadPacket);
		file.add(print);
		file.add(preview);
		file.addSeparator();
		file.add(exit);
		pMenuBar.add(file);
		pMenuBar.add(server);
		pMenuBar.add(client);
		pMenuBar.add(database);
		pMenuBar.add(query);
		pMenuBar.add(help);
		setJMenuBar(pMenuBar);

		settings.addActionListener(actionListener);
		about.addActionListener(actionListener);
		loadPacket.addActionListener(actionListener);
		save.addActionListener(actionListener);
		savepacket.addActionListener(actionListener);
		print.addActionListener(actionListener);
		preview.addActionListener(actionListener);
		exit.addActionListener(actionListener);
		contentHelp.addActionListener(actionListener);
		docHome.addActionListener(actionListener);
		keymap.addActionListener(actionListener);
		connDB.addActionListener(actionListener);
		newDB.addActionListener(actionListener);
		editDB.addActionListener(actionListener);
		deleteDB.addActionListener(actionListener);
		refreshQ.addActionListener(actionListener);
		deleteQ.addActionListener(actionListener);
		renameQ.addActionListener(actionListener);
		statusS.addActionListener(actionListener);
		connectP.addActionListener(actionListener);
		//aboutS.addActionListener(actionListener);
		cleanupS.addActionListener(actionListener);
		deltabS.addActionListener(actionListener);
		shutdownS.addActionListener(actionListener);

		mainPanel = new MainPanel(this, serverName, serverPort);
		getContentPane().add(mainPanel);
		mainPanel.reconnectToPServer();
		
		m=mainPanel;
		//System.out.println("Client packet is in picasso frame: "+mainPanel.getClientPacket().getClientId());
		PicassoUtil.setTheSettingInServer2(m, null, "COLLATION_SCHEME",(new Integer(PicassoConstants.COLLATION_SCHEME)).toString() );
		if(PicassoConstants.IS_SERVER_DEBUG==true)
			PicassoUtil.setTheSettingInServer2(m, null, "IS_SERVER_DEBUG",(new Integer(0)).toString() );
		else
        PicassoUtil.setTheSettingInServer2(m, null, "IS_SERVER_DEBUG",(new Integer(1)).toString() );
	
	}

	public String getCurrentDirectory() {
		return currentDir;
	}
	
	public void enableMenus(boolean value) {
		connDB.setEnabled(value);
		newDB.setEnabled(value);
		editDB.setEnabled(value);
		deleteDB.setEnabled(value);
		refreshQ.setEnabled(value);
		deleteQ.setEnabled(value);
		renameQ.setEnabled(value);
		connectP.setEnabled(value);
		statusS.setEnabled(value);
		cleanupS.setEnabled(value);
		deltabS.setEnabled(value);
		shutdownS.setEnabled(value);
		//aboutS.setEnabled(value);
		print.setEnabled(value);
		preview.setEnabled(value);
		loadPacket.setEnabled(true);
		save.setEnabled(value);
		savepacket.setEnabled(true);
	}

	public void enableDBMenus(boolean value) {
		connDB.setEnabled(value);
		editDB.setEnabled(value);
		deleteDB.setEnabled(value);
		deltabS.setEnabled(value);
		refreshQ.setEnabled(value);
		deleteQ.setEnabled(value);
		renameQ.setEnabled(value);
	}
	
	ActionListener actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if ( source == about ){
				new AboutPicassoFrame().setVisible(true);
			}
			else if ( source == exit )
				System.exit(0);
			else if (source == savepacket)
			{
				if ( mainPanel.getCurrentTab() instanceof QueryBuilderPanel ||
						mainPanel.getCurrentTab() instanceof SelectivityPanel ) 
				{
					JOptionPane.showMessageDialog(null, "Saving packets supported only for diagrams.","Information",JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					JFileChooser chooser = new JFileChooser(new File(PicassoConstants.INPUT_QUERY_FOLDER + "/../"));
					String fileExtension = ".pkt";
					if(PicassoConstants.SAVE_COMPRESSED_PACKET){
						fileExtension = ".pkt.gz";
						chooser.setFileFilter(new GZipFileFilter());
					}
					else{
						chooser.setFileFilter(new PktFileFilter());
					}
					if(mainPanel.getCurrentTab() instanceof ReducedPlanPanel)
						chooser.setSelectedFile(new File(mainPanel.getReducedDiagramPacket().getQueryPacket().getQueryName() + "_R" + fileExtension));
					else if(mainPanel.getCurrentTab().getPanelType() == PicassoPanel.EXEC_PLAN_COST_DIAGRAM || mainPanel.getCurrentTab().getPanelType() == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM)
						chooser.setSelectedFile(new File(mainPanel.getExecDiagramPacket().getQueryPacket().getQueryName() + "_E" + fileExtension));
					else
						chooser.setSelectedFile(new File(mainPanel.getDiagramPacket().getQueryPacket().getQueryName() + "_P" + fileExtension));
					//chooser.setFileFilter(new ImageFilter());
					//chooser.setAcceptAllFileFilterUsed(false);
					
					//Add custom icons for file types.
					//chooser.setFileView(new ImageView());
					
					int returnVal = chooser.showSaveDialog(mainPanel);
					if (returnVal != JFileChooser.APPROVE_OPTION)
						return;
					
					String path = chooser.getCurrentDirectory() + "";
					String fName = path + System.getProperty("file.separator")
					+ chooser.getSelectedFile().getName();
					
					try
					{
					FileOutputStream fis = new FileOutputStream (fName);
					ObjectOutputStream ois;
					GZIPOutputStream gos;
					if(PicassoConstants.SAVE_COMPRESSED_PACKET){
						gos = new GZIPOutputStream(fis);
						ois = new ObjectOutputStream (gos);
					}
					else{
						ois = new ObjectOutputStream (fis);
					}
					if(mainPanel.getCurrentTab() instanceof ReducedPlanPanel)
						ois.writeObject( mainPanel.getFullReducedDiagramPacket());
					else if(mainPanel.getCurrentTab().getPanelType() == PicassoPanel.EXEC_PLAN_COST_DIAGRAM || mainPanel.getCurrentTab().getPanelType() == PicassoPanel.EXEC_PLAN_CARD_DIAGRAM)
						ois.writeObject( mainPanel.getFullExecDiagramPacket());
					else
						ois.writeObject(mainPanel.getFullDiagramPacket());
						ois.writeObject(mainPanel.getServerPacket().trees);
					
					ois.flush();
					ois.close();
					}
					catch(Exception ex)
					{
						System.out.println(ex.getMessage());
						ex.printStackTrace();
					}
				}
			}
			else if (source == loadPacket)
			{
				JFileChooser chooser = new JFileChooser(new File(PicassoConstants.INPUT_QUERY_FOLDER + "/../"));
				
				int returnVal = chooser.showOpenDialog(mainPanel);
				if (returnVal != JFileChooser.APPROVE_OPTION)
					return;
				
				String path = chooser.getCurrentDirectory() + "";
				String fName = path + System.getProperty("file.separator") + chooser.getSelectedFile().getName();;
				
				try
				{
					FileInputStream fis = new FileInputStream (fName);
					ObjectInputStream ois;
					GZIPInputStream gos;
					
					boolean compressedPkt = false;
					
					if(fName.indexOf(".gz") != -1)
						compressedPkt = true;
					
					if(compressedPkt)
					{
						gos = new GZIPInputStream(fis);
						ois = new ObjectInputStream (gos);
					}
					else
						ois = new ObjectInputStream (fis);
					
					DiagramPacket dp = (DiagramPacket)ois.readObject();
					Vector trees = (Vector)ois.readObject();
					
					mainPanel.createNewServerPacket();
					mainPanel.getServerPacket().queryPacket = dp.getQueryPacket();
					mainPanel.getServerPacket().diagramPacket = dp;
					mainPanel.getServerPacket().trees = trees;
					
					int pktType = 0; 
					// 0 - Normal Diagram
					// 1 - Reduced Diagram
					// 2 - Execution Diagram
					if(compressedPkt)
					{
						if(fName.endsWith("_P.pkt.gz"))
							pktType = 0;
						else if(fName.endsWith("_R.pkt.gz"))
							pktType = 1;
						else if(fName.endsWith("_E.pkt.gz"))
							pktType = 2;
					}
					else
					{
						if(fName.endsWith("_P.pkt"))
							pktType = 0;
						else if(fName.endsWith("_R.pkt"))
							pktType = 1;
						else if(fName.endsWith("_E.pkt"))
							pktType = 2;
					}
					switch(pktType)
					{
						case 0:
								mainPanel.setFullDiagramPacket(dp);
								mainPanel.tabbedPane.setSelectedIndex(0);
								break;
						case 1:
//								mainPanel.setFullReducedDiagramPacket(dp);
								mainPanel.setFullDiagramPacket(dp); // Putting the reduced as the full diagram packet. 
								mainPanel.tabbedPane.setSelectedIndex(0);
								mainPanel.setStatusLabel("STATUS: Packet Loaded. The Reduced diagram and Execution diagrams tabs are invalid.");
								break;
						case 2:
								mainPanel.setFullDiagramPacket(dp);
								mainPanel.tabbedPane.setSelectedIndex(0);
								mainPanel.setStatusLabel("STATUS: Packet Loaded. The Execution diagram tabs are invalid.");
								break;
						default:
								System.out.println("Error Loading Packet");
					}
					mainPanel.enableAllTabs();
					// mainPanel.haveSettingsChanged = true;
					PicassoConstants.IS_PKT_LOADED = true;
					mainPanel.loadPacket(dp);
					// mainPanel.setClientPacket(new ClientPacket());
					mainPanel.getClientPacket().setQueryPacket(dp.getQueryPacket());
					PicassoConstants.a[0] = 0;
					PicassoConstants.a[1] = 1;
					mainPanel.msgRcvd = true;
					
					fis.close();
					ois.close();
				}
				catch(Exception ex)
				{
					System.out.println(ex.getMessage());
					ex.printStackTrace();
				}
				
			}
			else if ( source == print ) {
				//MessageUtil.CPrintToConsole("Current Tab :: " + mainPanel.getCurrentTab());
				if ( mainPanel.getCurrentTab() instanceof QueryBuilderPanel ||
						mainPanel.getCurrentTab() instanceof SelectivityPanel ) {
					JOptionPane.showMessageDialog(null, "Print supported only for diagrams.","Information",JOptionPane.INFORMATION_MESSAGE);
			} else
					new PicassoPrint(mainPanel, PicassoPrint.PRINT_TYPE).printPicasso();
			} else if ( source == preview ) {
				if ( mainPanel.getCurrentTab() instanceof QueryBuilderPanel ||
						mainPanel.getCurrentTab() instanceof SelectivityPanel ) {
					JOptionPane.showMessageDialog(null, "Print supported only for diagrams.","Information",JOptionPane.INFORMATION_MESSAGE);
				} else
					new PicassoPrint(mainPanel, PicassoPrint.PRINT_TYPE).previewPicasso();
			} else if ( source == save )
				PicassoSave.save(mainPanel);
			else if ( source == contentHelp )
				mainPanel.help();
			else if ( source == docHome )
				mainPanel.docHome();
			else if (source == keymap)
			{
				mouseMapLabel.setSize(50,50);
				mouseMapLabel.setEditable(false);
				mouseMapLabel.setFont(font);
				mouseMapLabel.setBackground(new Color(239,235,231));
				mouseMap = new JFrame();
				mouseMap.setTitle("Mouse-Key Mappings");
				mouseMap.setSize(500,450);
				mouseMap.setAlwaysOnTop(true);
				mouseMap.setResizable(false);
				mouseMap.add(mouseMapLabel);
				mouseMap.setVisible(true);
			}
			else if ( source == newDB )
				mainPanel.doMenuAction(PicassoConstants.NEW_DB_INSTANCE);
			else if ( source == editDB )
				mainPanel.doMenuAction(PicassoConstants.EDIT_DB_INSTANCE);
			else if ( source == deleteDB )
				mainPanel.doMenuAction(PicassoConstants.DELETE_DB_INSTANCE);
			else if ( source == refreshQ || source == connDB)
				mainPanel.doMenuAction(PicassoConstants.GET_DIAGRAM_LIST);
			else if ( source == deleteQ )
				mainPanel.doMenuAction(PicassoConstants.DELETE_DIAGRAM);
			else if ( source == renameQ )
				mainPanel.doMenuAction(PicassoConstants.RENAME_DIAGRAM);
			else if ( source == connectP ){
				mainPanel.setPanel();
				mainPanel.doMenuAction(PicassoConstants.CONNECT_PICASSO);
			}
			else if ( source == statusS )
			//else if ( source == aboutS )
				mainPanel.doMenuAction(PicassoConstants.CHECK_SERVER);
			else if ( source == cleanupS )
				mainPanel.doMenuAction(PicassoConstants.CLEAN_PICDB);
			else if ( source == deltabS )
				mainPanel.doMenuAction(PicassoConstants.DELETE_PICDB);
			else if ( source == shutdownS )
				mainPanel.doMenuAction(PicassoConstants.SHUTDOWN_SERVER);
			else if ( source == settings )
				mainPanel.doMenuAction(PicassoConstants.SHOW_PICASSO_SETTINGS);
		}
	};

	
	
	public static void main(String[] args) {
		PicassoSettingsManipulator.ReadPicassoConstantsFromFile();
		
		String serverName = "localhost";
		String serverPort = ""+PicassoConstants.SERVER_PORT;
		String currentDir = System.getProperty("user.dir");
		if(args.length > 0)
			serverName = args[0];
		if(args.length > 1)
			serverPort = args[1];
		
		/*if(args.length > 2)
			currentDir = args[2];*/
		new AboutFrame(serverName, serverPort, currentDir, true).setVisible(true);
//		PicassoUtil.setTheSettingInServer(m, null, "COLLATION_SCHEME",(new Integer(PicassoConstants.COLLATION_SCHEME)).toString() );


	}


}

class PktFileFilter extends FileFilter {
	
	//	Accept all directories and all pkt files.
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = PicassoUtil.getExtension(f.getName());
        if (extension != null) {
        	if ( extension.equals("pkt") ) {	
                    return true;
            } else {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription() {
        return "Packet files | *.pkt";
    }
}

class GZipFileFilter extends FileFilter {
	
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = PicassoUtil.getExtension(f.getName());
        if (extension != null) {
        	if (extension.equals("gz") ) {	
                    return true;
            } else {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription() {
        return "GZip files | *.gz";
    }
}

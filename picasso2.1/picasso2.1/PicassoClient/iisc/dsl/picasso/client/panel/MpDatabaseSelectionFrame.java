package iisc.dsl.picasso.client.panel;

import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.UIManager;



public class MpDatabaseSelectionFrame extends JFrame implements ActionListener {

	private JComboBox apcombo;
	private JComboBox apcombo1;
        private JTextArea QTTextBox;
	private JButton		okButton, autoConvertButton;
	private MainPanel	mainPanel;
	private PlanPanel ppframe;
	int planNumber;
	float xDSelec,yDSelec;
	
	public MpDatabaseSelectionFrame(MainPanel mp,PlanPanel apppframe,float x,float y,int pn)
	{
		ppframe=apppframe; xDSelec=x; yDSelec=y; planNumber=pn;
		mainPanel=mp;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		
		getContentPane().setLayout(new BorderLayout());
		
		setBackground(Color.LIGHT_GRAY);
		setTitle("Foreign Plan Settings");
		setSize(820, 600);
		setLocation(100,300);
		
		if(mp!=null)
                    apcombo = new JComboBox(mp.getDBSettings().getAllInstances());
                else 
                    apcombo = new JComboBox();
      		 String[] optarraystr = {"Default"};
		 apcombo1 = new JComboBox(optarraystr);
		okButton = new JButton("OK");


/*		apcombo.setMinimumSize(new Dimension(50,20));
		apcombo.setMaximumSize(new Dimension(100,20));
		
		
		JPanel jpv = new JPanel();
		
		JLabel jlab = new JLabel("Choose which  ");
		Color col = new Color(7);
		jlab.setForeground(col);
		JLabel jlab2 = new JLabel("Database engine's plan");
		JLabel jlab3 = new JLabel("to compare with.");
		Box bv = Box.createVerticalBox();
		 
		
		bv.add(jlab);
		bv.add(jlab2);
		bv.add(jlab3);
		jpv.add(bv);
		JPanel jph = new JPanel();
		 
		 jph.add(apcombo);
		// jph.add(apcombo1);
		okButton = new JButton("OK");
		JPanel jpb = new JPanel();
		 jpb.setLayout(new BorderLayout());
		 Box bh1 = Box.createHorizontalBox();
		// bv1.setAlignmentX(50);
		 //bv1.setAlignmentY(50);
		 String[] optarraystr = {"Default"};
		 apcombo1 = new JComboBox(optarraystr);
		 
			apcombo1.setMinimumSize(new Dimension(50,20));
			apcombo1.setMaximumSize(new Dimension(100,20));
			String optionValue = (String)apcombo.getSelectedItem();
			DBSettings tempdbsets = mainPanel.getDBSettings().get(optionValue);
			setOptBox(tempdbsets);
		JLabel jlab4 = new JLabel(" Choose Optimization Level::    ");
		
		bh1.add(jlab4);
		bh1.add(apcombo1);
		//bv1.add(okButton);
		jpb.setLayout(new BorderLayout());
		 jpb.add(bh1,BorderLayout.NORTH);
		 JPanel jpb1 = new JPanel(new java.awt.FlowLayout());
		 jpb1.add(okButton);
		 jpb.add(jpb1,BorderLayout.SOUTH);
		cancelButton = new JButton("Cancel");
		
		
*/		
		String optionValue = (String)apcombo.getSelectedItem();
		DBSettings tempdbsets = mainPanel.getDBSettings().get(optionValue);
		setOptBox(tempdbsets);
		okButton.addActionListener(this);
		apcombo.addActionListener(this);
//		cancelButton.addActionListener(this);
//		JPanel eastPanel = new JPanel();
//                eastPanel.setLayout(new BorderLayout());
//                eastPanel.add(BorderLayout.NORTH,jpv);
//		eastPanel.add(BorderLayout.CENTER,jph);
//		eastPanel.add(BorderLayout.SOUTH,jpb);
//                eastPanel.setBounds(10,10,310,210);
                QTTextBox=new JTextArea();
                autoConvertButton=new JButton("Auto Convert");
                autoConvertButton.addActionListener(this);
//		QueryEditPanel westPanel = new QueryEditPanel(autoConvertButton,QTTextBox);
                QTTextBox.setText(mp.getQueryText());
                                
//                getContentPane().add(BorderLayout.WEST,eastPanel);
//                getContentPane().add(BorderLayout.EAST,westPanel);
        
        getContentPane().add(new QueryEditPanel(okButton,autoConvertButton,QTTextBox,apcombo,apcombo1));
	}
	
	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == okButton ) {
			
			String optionValue = (String)apcombo.getSelectedItem();
			String optlevelValue = (String)apcombo1.getSelectedItem();
			//JOptionPane.showMessageDialog(mainPanel, optionValue,"INFO",JOptionPane.INFORMATION_MESSAGE);
			this.dispose();	

			String[] messageStrArr = new String[3];
			messageStrArr[0]="";//planno
			
					
		//	messageStrArr[1]=xSelec; //x selec (always out of 100, irrespective of resolution) 
			//messageStrArr[2]=ySelec; //y
		Object allinst[] = (mainPanel.getDBSettings()).getAllInstances();
		
			//if ( planNumber != -1 && mousePressedPlanNum != -1 ) {
				//if ( mousePressedPlanNum == planNumber )
					messageStrArr[0] += planNumber;
				//else messageStrArr[0] = mousePressedPlanNum + "," + planNumber;
				//MessageUtil.CPrintToConsole("PlanStr :: " + planStr);
				PicassoUtil.displayMultiPlanTree(mainPanel, ppframe, messageStrArr,xDSelec, yDSelec, planNumber, optionValue,optlevelValue,QTTextBox.getText());
		} else if(e.getSource()== apcombo) {
                    String optionValue = (String)apcombo.getSelectedItem();
                    DBSettings tempdbsets = mainPanel.getDBSettings().get(optionValue);
                    setOptBox(tempdbsets);
                    //JOptionPane.showMessageDialog(mainPanel, "u clicked","INFO",JOptionPane.INFORMATION_MESSAGE);
		} else if(e.getSource()==autoConvertButton){
                    String origQueryText = QTTextBox.getText();
                    String changedQueryText = origQueryText.replaceAll(":varies|:VARIES","=00000");
                    try {
                        Class swisAPI = Class.forName("com.adventnet.swissqlapi.SwisSQLAPI");
                        Object swisAPIObject = swisAPI.newInstance();
                        Method setSQL = swisAPI.getMethod("setSQLString",new Class[] {String.class});
                        Method convertSQL = swisAPI.getMethod("convert",new Class[] {int.class,boolean.class});
                        Field fieldDB2 = swisAPI.getDeclaredField("DB2");
                        Field fieldORACLE = swisAPI.getDeclaredField("ORACLE");
                        Field fieldMSSQLSERVER = swisAPI.getDeclaredField("MSSQLSERVER");
                        Field fieldSYBASE = swisAPI.getDeclaredField("SYBASE");
                        Field fieldPOSTGRESQL = swisAPI.getDeclaredField("POSTGRESQL");
                        
                    //com.adventnet.swissqlapi.SwisSQLAPI swissqlapi = new com.adventnet.swissqlapi.SwisSQLAPI(changedQueryText);
                        setSQL.invoke(swisAPIObject,new Object[]{changedQueryText});
                    String optionValue = (String)apcombo.getSelectedItem();
                    DBSettings tempdbsets = mainPanel.getDBSettings().get(optionValue);
                    
                        if(tempdbsets.getDbVendor().equals(DBConstants.DB2)){
                            //changedQueryText =swissqlapi.convert(swissqlapi.DB2,true);
                            changedQueryText = (String)convertSQL.invoke(swisAPIObject,new Object[]{fieldDB2.get(null),Boolean.TRUE});
                        }
                        else if(tempdbsets.getDbVendor().equals(DBConstants.MSSQL)){
                            //changedQueryText =swissqlapi.convert(swissqlapi.MSSQLSERVER,true);
                            changedQueryText = (String)convertSQL.invoke(swisAPIObject,new Object[]{fieldMSSQLSERVER.get(null),Boolean.TRUE});
                        }
                        else if(tempdbsets.getDbVendor().equals(DBConstants.ORACLE)){
                            //changedQueryText =swissqlapi.convert(swissqlapi.ORACLE,true);
                            changedQueryText = (String)convertSQL.invoke(swisAPIObject,new Object[]{fieldORACLE.get(null),Boolean.TRUE});
                        }
                        else if(tempdbsets.getDbVendor().equals(DBConstants.POSTGRES)){
                            //changedQueryText =swissqlapi.convert(swissqlapi.POSTGRESQL,true);
                            changedQueryText = (String)convertSQL.invoke(swisAPIObject,new Object[]{fieldPOSTGRESQL.get(null),Boolean.TRUE});
                        }
                        else if(tempdbsets.getDbVendor().equals(DBConstants.SYBASE)){
                            //changedQueryText =swissqlapi.convert(swissqlapi.SYBASE,true);
                            changedQueryText = (String)convertSQL.invoke(swisAPIObject,new Object[]{fieldSYBASE.get(null),Boolean.TRUE});
                        }
                    changedQueryText = changedQueryText.replaceAll("= 00000|=00000",":varies");
                    QTTextBox.setText(changedQueryText);
                    } catch (Exception ex) {
                        QTTextBox.setText(origQueryText);
                        JOptionPane.showMessageDialog(this,"Unable to convert the query\n Check if the query template is correct.","Auto Convert Failed",JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                    
                }
	}
	void setOptBox(DBSettings db) {
		String dbType = db.getDbVendor();
		if(dbType == null)
			return;
		apcombo1.removeAllItems();
		for(int i=0; i<DBConstants.databases.length; i++) {
			DBInfo dbinfo = DBConstants.databases[i];
			if ( dbType.equals(dbinfo.name)){
				for(int j=0; j<dbinfo.optLevels.length; j++){
					apcombo1.addItem(dbinfo.optLevels[j]);
					apcombo.setVisible(true);
					//optLbl.setVisible(true);
				}
			}
		}
		apcombo1.setSelectedItem(db.getOptLevel());
	}
		//addition for multi plan display ends here
	
//        public static void main(String[] args)
//        {
//            
//            //new MpDatabaseSelectionFrame(null,null,0,0,0).show();
//        }
}

/*
 * Created on April 30, 2007, 3:40 PM
 */

/**
 * @author  TR
 */
class QueryEditPanel extends javax.swing.JPanel {
    
    public QueryEditPanel(JButton b1, JButton b2, JTextArea jt1, JComboBox jcb1, JComboBox jcb2) {
        okButton = b1;
        autoConvertButton = b2;
        QTTextBox = jt1;
        apcombo = jcb1;
        apcombo1 = jcb2;
        initComponents();
    }
    
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        
        QTTextBox.setColumns(20);
        QTTextBox.setRows(5);
        jScrollPane1.setViewportView(QTTextBox);

        autoConvertButton.setText("Auto Convert SQL Dialect");
        try {
            Class.forName("com.adventnet.swissqlapi.SwisSQLAPI");
        } catch (ClassNotFoundException ex) {
             autoConvertButton.setEnabled(false);
        }
        jLabel3.setText("Database Engine");

        jLabel4.setText("Optimization Level");

        okButton.setText("Submit");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(layout.createSequentialGroup()
                            .addContainerGap()
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(jLabel4)
                                .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE))
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(layout.createSequentialGroup()
                                    .add(apcombo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 164, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                                .add(apcombo, 0, 164, Short.MAX_VALUE)))
                        .add(layout.createSequentialGroup()
                            .addContainerGap()
                            .add(autoConvertButton))))
                .add(19, 19, 19)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 800, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(19, 19, 19))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(28, 28, 28)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(apcombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .add(16, 16, 16)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(apcombo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(24, 24, 24)
                .add(autoConvertButton)
                .add(60, 60, 60)
                .add(okButton)
                .add(368, 368, 368))
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                .addContainerGap())
        );
    }
    
    private javax.swing.JTextArea QTTextBox;
    private javax.swing.JComboBox apcombo;
    private javax.swing.JComboBox apcombo1;
    private javax.swing.JButton autoConvertButton;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    
}

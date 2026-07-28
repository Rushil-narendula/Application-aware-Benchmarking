package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.PicassoSettingsManipulator;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;
import javax.swing.*;

public class PicassoSettingsFrame extends JFrame
    implements ActionListener
{

    private static final long serialVersionUID = 0x8fc0195eff140372L;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField SERVER_PORT;
    /*
    private JTextField SMALL_COLUMN;
    private JTextField MEDIUM_COLUMN;
    private JTextField LARGE_COLUMN;
    */
    private MainPanel m;
    private JTextField SELECTIVITY_LOG_REL_THRESHOLD;
    private JTextField SELECTIVITY_LOG_ABS_THRESHOLD;
    private JTextField PLAN_REDUCTION_THRESHOLD;
    private JTextField COST_DOMINATION_THRESHOLD;
    private JTextField DESIRED_NUM_PLANS;
    /*
    private JTextField QDIST_SKEW_10;
    private JTextField QDIST_SKEW_30;
    private JTextField QDIST_SKEW_100;
    private JTextField QDIST_SKEW_300;
    private JTextField QDIST_SKEW_1000;
    */
    private JComboBox REDUCTION_ALGORITHM;
    private JComboBox LOW_VIDEO;
    private JComboBox COLLATION;
    private JComboBox SAVE_FORMAT;
    /*
    private JComboBox ENABLE_COST_MODEL;
    private JComboBox IS_INTERNAL;
    */
    private JComboBox IS_CLIENT_DEBUG;
    private JComboBox IS_SERVER_DEBUG;
    /*
    private JComboBox LIMITED_DIAGRAMS;
    */
    FocusListener focusListener;

    public PicassoSettingsFrame(MainPanel mp)
    {
    	m=mp;
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        createGUI();
        SERVER_PORT.addFocusListener(focusListener);
        /*
        SMALL_COLUMN.addFocusListener(focusListener);
        MEDIUM_COLUMN.addFocusListener(focusListener);
        LARGE_COLUMN.addFocusListener(focusListener);
        */
        SELECTIVITY_LOG_REL_THRESHOLD.addFocusListener(focusListener);
        SELECTIVITY_LOG_ABS_THRESHOLD.addFocusListener(focusListener);
        PLAN_REDUCTION_THRESHOLD.addFocusListener(focusListener);
        COST_DOMINATION_THRESHOLD.addFocusListener(focusListener);
        DESIRED_NUM_PLANS.addFocusListener(focusListener);
        /*
        QDIST_SKEW_10.addFocusListener(focusListener);
        QDIST_SKEW_30.addFocusListener(focusListener);
        QDIST_SKEW_100.addFocusListener(focusListener);
        QDIST_SKEW_300.addFocusListener(focusListener);
        QDIST_SKEW_1000.addFocusListener(focusListener);
        */
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    private void createGUI()
    {
        setBackground(Color.LIGHT_GRAY);
        setTitle("Picasso Local Settings");
        setSize(850,510);
        setLocation(10, 30);
        Font f = new Font("Courier", 1, 12);
        String trueandfalse[] = {
            "true", "false"
        };
        String redalgos[] = {
            "1) Cost Greedy", "2) CC-SEER", "3) LiteSEER"
        };
        //For collations. Default is Case In-sensitive for MSSQL and Case senstive for
        // all other engines
        String collations[] = {
        		"1) Default", "2) Case Sensitive", "3) Case In-sensitive"
        };
        String saveFormat[] = {
        		"Normal", "Compressed"
        };
        SERVER_PORT = new JTextField((new Integer(PicassoConstants.SERVER_PORT)).toString());
        JLabel SERVER_PORTLbl = new JLabel("SERVER_PORT: ", JLabel.RIGHT);
        SERVER_PORTLbl.setFont(f);
        LOW_VIDEO = new JComboBox(trueandfalse);
        LOW_VIDEO.setSelectedItem((new Boolean(PicassoConstants.LOW_VIDEO)).toString());
        JLabel LOW_VIDEOLbl = new JLabel("LOW_VIDEO: ", JLabel.RIGHT);
        LOW_VIDEOLbl.setFont(f);
        /*
        SMALL_COLUMN = new JTextField((new Integer(PicassoConstants.SMALL_COLUMN)).toString());
        JLabel SMALL_COLUMNLbl = new JLabel("SMALL_COLUMN: ", JLabel.RIGHT);
        SMALL_COLUMNLbl.setFont(f);
        MEDIUM_COLUMN = new JTextField((new Integer(PicassoConstants.MEDIUM_COLUMN)).toString());
        JLabel MEDIUM_COLUMNLbl = new JLabel("MEDIUM_COLUMN: ", JLabel.RIGHT);
        MEDIUM_COLUMNLbl.setFont(f);
        LARGE_COLUMN = new JTextField((new Integer(PicassoConstants.LARGE_COLUMN)).toString());
        JLabel LARGE_COLUMNLbl = new JLabel("LARGE_COLUMN: ", JLabel.RIGHT);
        LARGE_COLUMNLbl.setFont(f);
        */
        SELECTIVITY_LOG_REL_THRESHOLD = new JTextField((new Double(PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD)).toString());
        JLabel SELECTIVITY_LOG_REL_THRESHOLDLbl = new JLabel("SEL_LOG_REL_THRESHOLD: ", JLabel.RIGHT);
        SELECTIVITY_LOG_REL_THRESHOLDLbl.setFont(f);
        SELECTIVITY_LOG_ABS_THRESHOLD = new JTextField((new Double(PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD)).toString());
        JLabel SELECTIVITY_LOG_ABS_THRESHOLDLbl = new JLabel("SEL_LOG_ABS_THRESHOLD: ", JLabel.RIGHT);
        SELECTIVITY_LOG_ABS_THRESHOLDLbl.setFont(f);
        PLAN_REDUCTION_THRESHOLD = new JTextField((new Double(PicassoConstants.PLAN_REDUCTION_THRESHOLD)).toString());
        JLabel PLAN_REDUCTION_THRESHOLDLbl = new JLabel("PLAN_REDUCTION_THRESHOLD: ", JLabel.RIGHT);
        PLAN_REDUCTION_THRESHOLDLbl.setFont(f);
        COST_DOMINATION_THRESHOLD = new JTextField((new Double(PicassoConstants.COST_DOMINATION_THRESHOLD)).toString());
        JLabel COST_DOMINATION_THRESHOLDLbl = new JLabel("COST_DOMINATION_THRESHOLD: ", JLabel.RIGHT);
        COST_DOMINATION_THRESHOLDLbl.setFont(f);
        REDUCTION_ALGORITHM = new JComboBox(redalgos);
        REDUCTION_ALGORITHM.setSelectedIndex(PicassoConstants.REDUCTION_ALGORITHM - 1);
        JLabel REDUCTION_ALGORITHMLbl = new JLabel("REDUCTION_ALGORITHM: ", JLabel.RIGHT);
        REDUCTION_ALGORITHMLbl.setFont(f);
        
        COLLATION = new JComboBox(collations);
        COLLATION.setSelectedIndex(PicassoConstants.COLLATION_SCHEME - 1);
        JLabel COLLATIONLb1 = new JLabel("COLLATION: ", JLabel.RIGHT);
        COLLATIONLb1.setFont(f);
        
        int saveFormatIndex = 0;
        if(PicassoConstants.SAVE_COMPRESSED_PACKET)
        	saveFormatIndex = 1;
        SAVE_FORMAT = new JComboBox(saveFormat);
        SAVE_FORMAT.setSelectedIndex(saveFormatIndex);
        JLabel SAVE_FORMATLbl = new JLabel("Saved Packet Format: ",JLabel.RIGHT);
        SAVE_FORMATLbl.setFont(f);
        
        DESIRED_NUM_PLANS = new JTextField((new Integer(PicassoConstants.DESIRED_NUM_PLANS)).toString());
        JLabel DESIRED_NUM_PLANSLbl = new JLabel("DESIRED_NUM_PLANS: ", JLabel.RIGHT);
        DESIRED_NUM_PLANSLbl.setFont(f);
        /*
        ENABLE_COST_MODEL = new JComboBox(trueandfalse);
        ENABLE_COST_MODEL.setSelectedItem((new Boolean(PicassoConstants.ENABLE_COST_MODEL)).toString());
        JLabel ENABLE_COST_MODELLbl = new JLabel("ENABLE_COST_MODEL: ", JLabel.RIGHT);
        ENABLE_COST_MODELLbl.setFont(f);
        QDIST_SKEW_10 = new JTextField((new Double(PicassoConstants.QDIST_SKEW_10)).toString());
        JLabel QDIST_SKEW_10Lbl = new JLabel("QDIST_SKEW_10: ", JLabel.RIGHT);
        QDIST_SKEW_10Lbl.setFont(f);
        QDIST_SKEW_30 = new JTextField((new Double(PicassoConstants.QDIST_SKEW_30)).toString());
        JLabel QDIST_SKEW_30Lbl = new JLabel("QDIST_SKEW_30: ", JLabel.RIGHT);
        QDIST_SKEW_30Lbl.setFont(f);
        QDIST_SKEW_100 = new JTextField((new Double(PicassoConstants.QDIST_SKEW_100)).toString());
        JLabel QDIST_SKEW_100Lbl = new JLabel("QDIST_SKEW_100: ", JLabel.RIGHT);
        QDIST_SKEW_100Lbl.setFont(f);
        QDIST_SKEW_300 = new JTextField((new Double(PicassoConstants.QDIST_SKEW_300)).toString());
        JLabel QDIST_SKEW_300Lbl = new JLabel("QDIST_SKEW_300: ", JLabel.RIGHT);
        QDIST_SKEW_300Lbl.setFont(f);
        QDIST_SKEW_1000 = new JTextField((new Double(PicassoConstants.QDIST_SKEW_1000)).toString());
        JLabel QDIST_SKEW_1000Lbl = new JLabel("QDIST_SKEW_1000: ", JLabel.RIGHT);
        QDIST_SKEW_1000Lbl.setFont(f);
        IS_INTERNAL = new JComboBox(trueandfalse);
        IS_INTERNAL.setSelectedItem((new Boolean(PicassoConstants.IS_INTERNAL)).toString());
        JLabel IS_INTERNALLbl = new JLabel("IS_INTERNAL: ", JLabel.RIGHT);
        IS_INTERNALLbl.setFont(f);
        */
        IS_CLIENT_DEBUG = new JComboBox(trueandfalse);
        IS_CLIENT_DEBUG.setSelectedItem((new Boolean(PicassoConstants.IS_CLIENT_DEBUG)).toString());
        JLabel IS_CLIENT_DEBUGLbl = new JLabel("IS_CLIENT_DEBUG: ", JLabel.RIGHT);
        IS_CLIENT_DEBUGLbl.setFont(f);
        IS_SERVER_DEBUG = new JComboBox(trueandfalse);
        IS_SERVER_DEBUG.setSelectedItem((new Boolean(PicassoConstants.IS_SERVER_DEBUG)).toString());
        JLabel IS_SERVER_DEBUGLbl = new JLabel("IS_SERVER_DEBUG: ", JLabel.RIGHT);
        IS_SERVER_DEBUGLbl.setFont(f);
        /*
        LIMITED_DIAGRAMS = new JComboBox(trueandfalse);
        LIMITED_DIAGRAMS.setSelectedItem((new Boolean(PicassoConstants.LIMITED_DIAGRAMS)).toString());
        JLabel LIMITED_DIAGRAMSLbl = new JLabel("LIMITED_DIAGRAMS: ", JLabel.RIGHT);
        LIMITED_DIAGRAMSLbl.setFont(f);
        */
        okButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        getContentPane().setLayout(new GridLayout(14, 4, 15, 10));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(SERVER_PORTLbl);
        getContentPane().add(SERVER_PORT);
        getContentPane().add(LOW_VIDEOLbl);
        getContentPane().add(LOW_VIDEO);
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(SELECTIVITY_LOG_REL_THRESHOLDLbl);
        getContentPane().add(SELECTIVITY_LOG_REL_THRESHOLD);
        getContentPane().add(SELECTIVITY_LOG_ABS_THRESHOLDLbl);
        getContentPane().add(SELECTIVITY_LOG_ABS_THRESHOLD);
        getContentPane().add(PLAN_REDUCTION_THRESHOLDLbl);
        getContentPane().add(PLAN_REDUCTION_THRESHOLD);
        getContentPane().add(COST_DOMINATION_THRESHOLDLbl);
        getContentPane().add(COST_DOMINATION_THRESHOLD);
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(REDUCTION_ALGORITHMLbl);
        getContentPane().add(REDUCTION_ALGORITHM);
        getContentPane().add(DESIRED_NUM_PLANSLbl);
        getContentPane().add(DESIRED_NUM_PLANS);
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(COLLATIONLb1);
        getContentPane().add(COLLATION);
        getContentPane().add(SAVE_FORMATLbl);
        getContentPane().add(SAVE_FORMAT);
       // getContentPane().add(new JLabel(""));
       // getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(IS_SERVER_DEBUGLbl);
        getContentPane().add(IS_SERVER_DEBUG);
        getContentPane().add(IS_CLIENT_DEBUGLbl);
        getContentPane().add(IS_CLIENT_DEBUG);
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));
        getContentPane().add(new JLabel(""));        
      /*
        getContentPane().add(QDIST_SKEW_10Lbl);
        getContentPane().add(QDIST_SKEW_10);
        getContentPane().add(QDIST_SKEW_30Lbl);
        getContentPane().add(QDIST_SKEW_30);
        getContentPane().add(QDIST_SKEW_100Lbl);
        getContentPane().add(QDIST_SKEW_100);
        getContentPane().add(QDIST_SKEW_300Lbl);
        getContentPane().add(QDIST_SKEW_300);
        getContentPane().add(QDIST_SKEW_1000Lbl);
        getContentPane().add(QDIST_SKEW_1000);
        */
        getContentPane().add(new JLabel(""));
        getContentPane().add(okButton);
        getContentPane().add(cancelButton);
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == okButton)
        {
            PicassoSettingsManipulator.setthesetting("SERVER_PORT", SERVER_PORT.getText());
            PicassoSettingsManipulator.setthesetting("LOW_VIDEO", LOW_VIDEO.getSelectedItem().toString());
            /*
            PicassoSettingsManipulator.setthesetting("SMALL_COLUMN", SMALL_COLUMN.getText());
            PicassoSettingsManipulator.setthesetting("MEDIUM_COLUMN", MEDIUM_COLUMN.getText());
            PicassoSettingsManipulator.setthesetting("LARGE_COLUMN", LARGE_COLUMN.getText());
            */
            PicassoSettingsManipulator.setthesetting("SELECTIVITY_LOG_REL_THRESHOLD", SELECTIVITY_LOG_REL_THRESHOLD.getText());
            PicassoSettingsManipulator.setthesetting("SELECTIVITY_LOG_ABS_THRESHOLD", SELECTIVITY_LOG_ABS_THRESHOLD.getText());
            PicassoSettingsManipulator.setthesetting("PLAN_REDUCTION_THRESHOLD", PLAN_REDUCTION_THRESHOLD.getText());
            PicassoSettingsManipulator.setthesetting("COST_DOMINATION_THRESHOLD", COST_DOMINATION_THRESHOLD.getText());
            PicassoSettingsManipulator.setthesetting("REDUCTION_ALGORITHM", (new Integer(REDUCTION_ALGORITHM.getSelectedIndex() + 1)).toString());
            PicassoSettingsManipulator.setthesetting("DESIRED_NUM_PLANS", DESIRED_NUM_PLANS.getText());
            PicassoSettingsManipulator.setthesetting("COLLATION", (new Integer(COLLATION.getSelectedIndex() + 1)).toString());
            
            String saveCompressedPacket = "false";
            if(SAVE_FORMAT.getSelectedIndex() == 1)
            	saveCompressedPacket = "true";
            PicassoUtil.setTheSettingInServer(m, null, "COLLATION_SCHEME",(new Integer(COLLATION.getSelectedIndex() + 1)).toString() );
            PicassoUtil.setTheSettingInServer(m, null, "IS_SERVER_DEBUG",(new Integer(IS_SERVER_DEBUG.getSelectedIndex())).toString() );
            PicassoSettingsManipulator.setthesetting("SAVE_COMPRESSED_PACKET", saveCompressedPacket);
            
            /*
            PicassoSettingsManipulator.setthesetting("ENABLE_COST_MODEL", ENABLE_COST_MODEL.getSelectedItem().toString());
            PicassoSettingsManipulator.setthesetting("QDIST_SKEW_10", QDIST_SKEW_10.getText());
            PicassoSettingsManipulator.setthesetting("QDIST_SKEW_30", QDIST_SKEW_30.getText());
            PicassoSettingsManipulator.setthesetting("QDIST_SKEW_100", QDIST_SKEW_100.getText());
            PicassoSettingsManipulator.setthesetting("QDIST_SKEW_300", QDIST_SKEW_300.getText());
            PicassoSettingsManipulator.setthesetting("QDIST_SKEW_1000", QDIST_SKEW_1000.getText());
            PicassoSettingsManipulator.setthesetting("IS_INTERNAL", IS_INTERNAL.getSelectedItem().toString());
            */
            
            PicassoSettingsManipulator.setthesetting("IS_CLIENT_DEBUG", IS_CLIENT_DEBUG.getSelectedItem().toString());
            PicassoSettingsManipulator.setthesetting("IS_SERVER_DEBUG", IS_SERVER_DEBUG.getSelectedItem().toString());
            /*
            PicassoSettingsManipulator.setthesetting("LIMITED_DIAGRAMS", LIMITED_DIAGRAMS.getSelectedItem().toString());
            */
            PicassoSettingsManipulator.putIntoPicassoConstants();
            PicassoSettingsManipulator.WritePicassoConstantsToFile();
            dispose();
        } else
        if(e.getSource() == cancelButton)
        {
            dispose();
        }
    }

}

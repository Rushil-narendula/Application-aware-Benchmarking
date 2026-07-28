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

package iisc.dsl.picasso.client.frame;import iisc.dsl.picasso.client.panel.DataBaseSettingsPanel;import iisc.dsl.picasso.client.panel.QueryBuilderPanel;import iisc.dsl.picasso.client.panel.MainPanel;import iisc.dsl.picasso.common.ClientPacket;import iisc.dsl.picasso.common.PicassoConstants;import iisc.dsl.picasso.common.ds.QueryPacket;import java.awt.*;import java.awt.event.ActionEvent;import java.awt.event.ActionListener;import java.util.Hashtable;import javax.swing.*;import javax.swing.text.*;
class FixedLengthPlainDocument extends PlainDocument
{ 
	                
	                private int maxLength; 
	                 
	                public FixedLengthPlainDocument(int maxLength) { 
	                          this.maxLength = maxLength; 
	                } 
	                public void insertString(int offset, String str,
AttributeSet a) 
	                        throws BadLocationException { 
	                        if(getLength() + str.length() > maxLength) {

	                                Toolkit.getDefaultToolkit().beep(); 
	                        } 
	                        else { 
	                                 super.insertString(offset, str, a);

	                        } 
	                }                
	                
	    } 
	        
	        class FixedLengthTextField extends JTextField { 
	                
	                public FixedLengthTextField(int length) 
	                { 
	                        this(null, length); 
	                } 
	                
	                public FixedLengthTextField(String text, int length)
{ 
	                        super(new FixedLengthPlainDocument(length),
text, length); 
	                } 

	        }
public class ResolutionFrame extends JDialog implements ActionListener{	private static final long serialVersionUID = 1L;
		JButton			okButton, cancelButton;//need	
	int NUM_DIMS=PicassoConstants.NUM_DIMS;	MainPanel mainPanel;
	
	JLabel[] attributes = new JLabel[NUM_DIMS]; 	private JComboBox[] resBox1 = new JComboBox[NUM_DIMS];	public JTextField [][] range = new FixedLengthTextField[2][NUM_DIMS];			JLabel lblGranu = new JLabel("Granularity", JLabel.LEFT);	JLabel lblStart = new JLabel("Start Point (%)", JLabel.LEFT);	JLabel lblEnd = new JLabel("End Point in (%)", JLabel.LEFT);	
	ClientPacket locmssg;	private QueryBuilderPanel 		locQBP;	boolean valid =true;	
	String locdist;	JPanel right;	boolean flag = false;

	public ResolutionFrame()
{	for(int i=0; i <NUM_DIMS;i++)		
{	
	resBox1[i]=new JComboBox();//
	range[1][i]= new FixedLengthTextField("100",6);
	range[0][i]= new FixedLengthTextField("0",6);
	attributes[i]=new JLabel();
	//range[1][i].
}

	}
	public ResolutionFrame(ClientPacket message, String distribution, QueryBuilderPanel queryBuilderPanel, MainPanel mp) 
	{

		boolean flag=false; 
		if ( PicassoConstants.NUM_DIMS <= 0 ) 

		{

			JOptionPane.showMessageDialog(this, "No Picasso Selectivity Predicates selected","Error",JOptionPane.ERROR_MESSAGE);
		flag=true;
		}
		if (flag)
		dispose();
			locmssg = message;
		locdist = distribution;
		locQBP =queryBuilderPanel;
		mainPanel = mp;

		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
				setTitle("Choose Range and Resolution Values");

		for(int i=0; i <NUM_DIMS;i++)					
		{	
			resBox1[i]=new JComboBox();//
			range[1][i]= new FixedLengthTextField(6);
			range[0][i]= new FixedLengthTextField(6);
			attributes[i]=new JLabel(PicassoConstants.params[i],JLabel.LEFT);
			range[1][i].setText(new Double(DataBaseSettingsPanel.locrange[1][i]*100).toString());
			range[0][i].setText(new Double(DataBaseSettingsPanel.locrange[0][i]*100).toString());
		}
		
		for(int i=0; i < NUM_DIMS;i++)						//
		{										
			resBox1[i].setEnabled(true);	
			range[1][i].setEnabled(true);
			range[0][i].setEnabled(true);
		}
	
		for(int i=0; i<NUM_DIMS;i++) 	
		{
		resBox1[i] = new JComboBox();
		resBox1[i].setMinimumSize(new Dimension(100,20));
		resBox1[i].addItem("10");
		resBox1[i].addItem("30");
		resBox1[i].addItem("100");
		resBox1[i].addItem("300");
		resBox1[i].addItem("1000");
		resBox1[i].setSelectedItem(new Integer(DataBaseSettingsPanel.locres[i]).toString());
		resBox1[i].setToolTipText("Resolution for dimension " + i);	
		range[0][i]= new FixedLengthTextField(new Double(mainPanel.getDBSettingsPanel().locrange[0][i]*100).toString(),6);
		range[1][i]=new FixedLengthTextField(new Double(mainPanel.getDBSettingsPanel().locrange[1][i]*100).toString(),6);
		range[0][i].setToolTipText("lower threshold for dimension " +i);
		range[1][i].setToolTipText("upper threshold for dimension" +i);
		}
		
		right = new JPanel();

		GridBagConstraints c = new GridBagConstraints();
	
		right.setLayout(new GridBagLayout());
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.ipady = 2;
		c.ipadx = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 10, 10, 10);
		c.gridy=0;
		for(int i=0;i<NUM_DIMS;i++)
		{
			c.gridx=1+i;
			c.gridwidth=1;
			right.add(attributes[i],c);
		}

		c.gridx = 0;
		c.gridy = 1;
		right.add(lblGranu,c);

		c.gridx=1;
		for (int i=0;i<NUM_DIMS;i++)	
		{
			c.gridx = 1+i;
			c.gridwidth = 1;
			right.add(resBox1[i], c);
		}
		
		c.gridy=2;
		c.gridx=0;
		right.add(lblStart,c);
		
		for (int i=0;i<NUM_DIMS;i++)	
		{
			c.gridx = 1+i;
			c.gridwidth = 1;
			right.add(range[0][i], c);
		}	
		
		c.gridy=3;
		c.gridx=0;
		right.add(lblEnd,c);
		for (int i=0;i<NUM_DIMS;i++)	
		{
			c.gridx = 1+i;
			c.gridwidth = 1;
			right.add(range[1][i], c);
		}	
		
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		
		enablerange(false);
		enableres(false);
		enableright();
	
		right.setOpaque(false);
		
		getContentPane().setLayout(new GridBagLayout());

		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.ipady = 2;
		c.ipadx = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 10, 10, 10);
		getContentPane().add(right,c);

		c.gridx=0;
		c.gridy=2;
		getContentPane().add(okButton, c);
		
		c.gridx = 1;
		getContentPane().add(cancelButton, c);
		
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		setSize(200, 200);
		setLocation(100, 100);
	}
	
	public void actionPerformed(ActionEvent event) 
	{	
	
		if (event.getSource() == okButton)
		{	
			valid=true;
			for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
			{				if((Double.parseDouble(getEndPoint(i))-Double.parseDouble(getStartPoint(i))) < (PicassoConstants.MINIMUMRANGE/100) && valid)
				{
					valid=false;					JOptionPane.showMessageDialog (this, "Entered End Point must be greater than Start Point by atleast a value of "+ PicassoConstants.MINIMUMRANGE,
						"Warning", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			if (valid)
			{
				locmssg=setClientMsgFields(locmssg);
				mainPanel.setClientPacket(locmssg);
				mainPanel.setqp(true);
				flag = true;
				dispose();
			} 
		}
		else if (event.getSource() == cancelButton)
		{
			if(mainPanel.getDBSettingsPanel().regen)
			{
				mainPanel.getDBSettingsPanel().regen=false;
				mainPanel.getDBSettingsPanel().setRangeFlag=mainPanel.getDBSettingsPanel().oldRangeFlag;
				mainPanel.getDBSettingsPanel().setResFlag=mainPanel.getDBSettingsPanel().oldResFlag;
				mainPanel.getDBSettingsPanel().setrange(mainPanel.getDBSettingsPanel().rangeIndex);
				mainPanel.getDBSettingsPanel().setresbox(mainPanel.getDBSettingsPanel().resIndex);
				mainPanel.getDBSettingsPanel().readonly=true;
			}
			mainPanel.processErrorMessage(mainPanel.getServerPacket());
			dispose();
		}
	}
	
	public void enableright ()
	{
		if(mainPanel.getDBSettingsPanel().getRangeStr()=="Custom Per Dimension")
			enablerange ( true);
		if(mainPanel.getDBSettingsPanel().getResolution()=="Custom Per Dimension")
			enableres ( true);	
		else
			for(int i=0;i<PicassoConstants.NUM_DIMS;i++)
			{
				resBox1[i].setSelectedItem(mainPanel.getDBSettingsPanel().getResolution());			
			}
	}

	public void enableres (boolean value)
	{
		for(int i=0;i<NUM_DIMS;i++)
		{
			resBox1[i].setEnabled(value);		 
		}
	}

	public void enablerange (boolean value)
	{
		for(int i=0;i<NUM_DIMS;i++)	
		{
			range[0][i].setEnabled(value);
			range[1][i].setEnabled(value);
		}
	}

	public String getResolution(int i) 
	{
		if(i < resBox1.length)
			return((String)resBox1[i].getSelectedItem());
		return "10";
	}

	public String getEndPoint(int i)
	{   
		double d=1.0;
		if (i<range[1].length)
		{
			try	
			{	
				d= Double.parseDouble(range[1][i].getText())/100.0;
			}catch(NumberFormatException nfe)
			{
				valid=false;
				JOptionPane.showMessageDialog (this, "Enter End Point as a Valid Floating Point Number",
				"Warning", JOptionPane.ERROR_MESSAGE);	
			}
			
			if ((d<0 ||d>1) && valid)
			{
				valid=false;
				JOptionPane.showMessageDialog (this, "Enter End Point between 0 and 100",
						"Warning", JOptionPane.ERROR_MESSAGE);	
			}
		return Double.toString(d);
		}
		return"1.0";
	}

	public String getStartPoint(int i)
	{
		double d=1.0;
		if(i<range[0].length)
		{
			try	
			{	
				d= Double.parseDouble(range[0][i].getText())/100.0;
			}catch(NumberFormatException nfe)
			{
				valid=false;
				JOptionPane.showMessageDialog (this, "Enter Start Point as a Valid Floating Point Number",
					"Warning", JOptionPane.ERROR_MESSAGE);	
			}	
			if ((d<0 ||d>1) && valid)
			{	
				valid=false;
				JOptionPane.showMessageDialog (this, "Enter Start Point between 0 and 100",
					"Warning", JOptionPane.ERROR_MESSAGE);	
			}
			return Double.toString(d);
		}
		return"0.0";
	}

	public ClientPacket setClientMsgFields(ClientPacket message) 
	{
		String val;
		QueryPacket qp = message.getQueryPacket();
	
		for (int i=0;i<PicassoConstants.NUM_DIMS;i++)					
		{															
			val =getResolution(i);									
			if ( val != null )										
				qp.setResolution(Integer.parseInt(val),i);			
			else													
				qp.setResolution(10,i);								
			val =getStartPoint(i);									
			if ( val != null )										
				qp.setStartPoint(Double.parseDouble(val),i);		
			else													
				qp.setStartPoint(0,i);								
			val =getEndPoint(i);									
			if ( val != null )										
				qp.setEndPoint(Double.parseDouble(val),i);			
			else													
				qp.setEndPoint(1.0,i);								
		}	
		String dist_const =locdist;
		if(dist_const.equals(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
		{
			for(int i=0;i<PicassoConstants.NUM_DIMS;i++)				
			{
				switch(qp.getResolution(i))
				{
				case 10:
					dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_10;
					break;
				case 30:
					dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_30;
					break;
				case 100:
					dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_100;
					break;
				case 300:
					dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_300;
					break;
				case 1000:
					dist_const = dist_const+"_"+PicassoConstants.QDIST_SKEW_1000;
					break;
				}
			}	
		}
		qp.setDimension(PicassoConstants.NUM_DIMS);
		qp.setDistribution(dist_const);
		Hashtable tmp = locQBP.getPredicateValues();
		if(tmp!=null) 
		{
			Object[] keys = tmp.keySet().toArray();
			Hashtable tmp1 = new Hashtable();
			for (int i=0; i < keys.length; i++) 
			{
				String predValue = (String)tmp.get(keys[i]);
				double pv = Double.parseDouble(predValue);
				if(dist_const.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
				{
					for (int j=0;j<PicassoConstants.NUM_DIMS;j++)								
						if ( pv < 100/(qp.getResolution(j)*2))				
							pv = 100/(qp.getResolution(j)*2);				
				}
                tmp1.put(keys[i], ""+pv);
			}
			message.setAttributeSelectivities(tmp1);
		}
		else
		{
			message.setAttributeSelectivities(null);
		}
		message.setSelecErrorThreshold(""+PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD);
		message.setQueryPacket(qp);
		return message;
	}
}
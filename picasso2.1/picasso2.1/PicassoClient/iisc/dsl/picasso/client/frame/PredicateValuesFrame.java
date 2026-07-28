
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

package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.panel.PicassoPanel;
import iisc.dsl.picasso.common.PicassoConstants;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.UIManager;

public class PredicateValuesFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	JButton			saveButton, cancelButton;
	JLabel[]		labels;
	public JComboBox[]		textFields;
	int[]			indexes;
	PicassoPanel	qbp;
	int 			resolution[] = new int [PicassoConstants.NUM_DIMS];
	double[]		startpt = new double[PicassoConstants.NUM_DIMS];
	double[]		endpt = new double[PicassoConstants.NUM_DIMS];
	String 			distribution;
	double[][] 		selectivity;
	String[][] 		sselectivity;


	
	public PredicateValuesFrame(PicassoPanel qp, Vector queryParams, Hashtable vals, int[] resolution, double []startpoint, double [] endpoint, String distribution) 
	{
		int num = queryParams.size() - 2;
		
		if ( num <= 0 ) 
		{
			JOptionPane.showMessageDialog(this, "QueryTemplate predicates less than three","Error",JOptionPane.ERROR_MESSAGE);
			this.dispose();
			return;
		}
		this.resolution=resolution;
		this.distribution=distribution;
		this.startpt = startpoint;
		this.endpt = endpoint;
		
		try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) 
		{
			System.out.println(e);
		}
		setTitle("Choose Predicate Values");
		labels = new JLabel[num];
		textFields = new JComboBox[num];
		indexes = new int[num];
		qbp = qp;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.ipady = 2;
		c.ipadx = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 10, 10, 10);
		
		int count = 0;
		selectivity = new double[PicassoConstants.NUM_DIMS][];
		sselectivity = new String[PicassoConstants.NUM_DIMS][];
		for(int i=0 ;i<PicassoConstants.NUM_DIMS;i++)						
		{												
			selectivity[i] = new double[resolution[i]];	
			sselectivity[i] = new String[resolution[i]];	
		}												
		genSelectivities(selectivity, sselectivity, resolution, startpoint, endpoint, distribution);
		for(int j=0;j<PicassoConstants.NUM_DIMS;j++)			
			for(int i=0;i<resolution[j];i++)
			{
				sselectivity[j][i]=sselectivity[j][i]+"%";
			}
		
		for (int i=0; i < queryParams.size(); i++) // queryParams.size() = #dimensions 
		{
			String str = (String)queryParams.elementAt(i);
			Integer dimKey = new Integer(i);
			String dimVal = (String)vals.get(dimKey);
			if ( dimVal == null )
				continue;
			labels[count] = new JLabel(str);
			
			textFields[count] = new JComboBox(sselectivity[i]);
			indexes[count] = i;
			textFields[count].setSelectedIndex(PicassoConstants.prevselected[count]);
			PicassoConstants.first=false;
			c.gridx = 0;
			c.gridy = count;
			getContentPane().add(labels[count], c);
			c.gridx = 1;
			getContentPane().add(textFields[count], c);
			count++;
		}
		
		saveButton = new JButton("Save");
		cancelButton = new JButton("Cancel");
		
		c.gridx = 0;
		c.gridy = count;
		getContentPane().add(saveButton, c);
		c.gridx = 1;
		getContentPane().add(cancelButton, c);
		saveButton.addActionListener(this);
		cancelButton.addActionListener(this);
		setSize(200, 200);
		setLocation(100, 100);
	}
	public void resetmarkers(){
		for (int i=0;i<5;i++)
			{
			PicassoConstants.prevselected[i]=0;
			PicassoConstants.slice[i]=0;
			PicassoConstants.first=false;
			}
		}
	//The first two parameters passed are set here. They must be allocated and passed here.
	public static void genSelectivities(double[][] selectivity,String[][] sselectivity,int []resolution, double[] startpoint, double[] endpoint,String distribution)
	{
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(5);

		double[] sel = new double [resolution.length];	

		for(int i=0;i<resolution.length;i++)				
			sel[i]=startpoint[i] + ((endpoint[i] - startpoint[i])/(2*resolution[i]));			
		
		if(distribution.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
		{	
			for(int j=0;j<PicassoConstants.NUM_DIMS;j++)							
			{																
				for(int i=0;i<resolution[j];i++)
				{															
					selectivity[j][i] = sel[j];								
					sselectivity[j][i]=df.format(selectivity[j][i]*100);	
					sel[j] += ((endpoint[j] - startpoint[j])/resolution[j]);							
				}
			}
		}
		else //if(distribution.startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
		{
			//initialization will be overridden.
			double r=1.0;
			for (int j=0;j<PicassoConstants.NUM_DIMS;j++)
			{
				switch(resolution[j])
				{
					case 10:
						r=PicassoConstants.QDIST_SKEW_10;
						break;
					case 30:
						r=PicassoConstants.QDIST_SKEW_30;
						break;
					case 100:
						r=PicassoConstants.QDIST_SKEW_100;
						break;
					case 300:
						r=PicassoConstants.QDIST_SKEW_300;
						break;
					case 1000:
						r=PicassoConstants.QDIST_SKEW_1000;
						break;
				}
			
				int i;
				int popu=resolution[j];
				double a=1;
				double curval=a,sum=a/2;
				
				for(i=1;i<=popu;i++)
				{
					curval*=r;
					if(i!=popu)
						sum+=curval;
					else
						sum+=curval/2;
				}
				
				a=1/sum;
				curval=a;
				sum=a/2;
				
				for(i=1;i<=popu;i++)
				{
	
					selectivity[j][i-1] = startpoint[j] + sum;
					sselectivity[j][i-1] = df.format(selectivity[j][i-1]*100);
					curval*=r;
					if(i!=popu)
						sum+=(curval * (endpoint[j] - startpoint[j]));
					else
						sum+=(curval * (endpoint[j] - startpoint[j]))/2;
				}
			}
		}
	}
	
	
	
	public void actionPerformed(ActionEvent event) 
	{
		if ( event.getSource() == saveButton ) 
		{
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(5);
			try 
			{
				Hashtable table = new Hashtable();
			
				for (int i=0; i < indexes.length; i++) 
				{
					int str = textFields[i].getSelectedIndex();
					PicassoConstants.prevselected[i]=str;
					
					double tmp = selectivity[indexes[i]][str];
					PicassoConstants.slice[i]=tmp*100;
					if(tmp<0 || tmp>100)
						throw new Exception("Improper value entered");
					table.put(new Integer(indexes[i]), df.format(selectivity[indexes[i]][str]*100));
				}
				qbp.setPredicateValues(table);
				//this.dispose();
				this.setVisible(false);
			}
			catch (Exception e) {
					JOptionPane.showMessageDialog(this, "Value entered should be a number between startpoint and endpoint.\n Enter Another Value","Error",JOptionPane.ERROR_MESSAGE);
				}
		} 
		else if( event.getSource() == cancelButton ) 
		{
			this.dispose();
		}
	}
}
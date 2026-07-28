
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

package iisc.dsl.picasso.client.panel;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class SelectivityPanel extends PicassoPanel {
	
	private static final long serialVersionUID = 2597020192118294266L;
	
	public SelectivityPanel(MainPanel app) {
		super(app);
		
		panelType = SELECTIVITY_LOG;
		setPanelString("SelectivityPanel");
		setLayout(new BorderLayout());
	}
	
	public void process(int msgType) {
		sendProcessToServer(msgType); 
	}
	
	public void emptyPanel() {
		this.removeAll();
	//	 this.dimensionBox1.setEnabled(false);
	  //      this.dimensionBox2.setEnabled(false);
	}
	
	public int setSelectivityLog(ServerPacket packet) {
		this.removeAll();
		
		DiagramPacket gdp = packet.diagramPacket;
		
		double value = -1;
		String selecThreshold = "-1";
		String defaultThreshold = "" + PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD;
		
		while ( value < 0 ) {
			selecThreshold = defaultThreshold;//JOptionPane.showInputDialog(this.getPParent().getParent(), "Enter Selectivity Difference Threshold (in %)", defaultThreshold);
			if ( selecThreshold == null ) {
				return -1;
			}
			try {
				double s = Double.parseDouble(selecThreshold);
				if ( s < 1 ) {
					JOptionPane.showMessageDialog(this.getPParent().getParent(), "Value entered should be a number greater than 1\n Enter Another Value","Error",JOptionPane.ERROR_MESSAGE);
					selecThreshold = defaultThreshold;
					value = -1;
				} else value = s;
			} catch (Exception e) {
				if (selecThreshold == null)
					value = 0;
				JOptionPane.showMessageDialog(this.getPParent().getParent(), "Value entered should be a number greater than 1\n Enter Another Value","Error",JOptionPane.ERROR_MESSAGE);
				selecThreshold = defaultThreshold;
				value = -1;
			}
		}
		
		String[] relNames = gdp.getRelationNames();
		String[] attrNames = gdp.getAttributeNames();
		
		setLayout(new BorderLayout());
		
		Font f = new Font("Arial", Font.PLAIN, 14);
		JPanel logPanel = new JPanel(new GridLayout(1, relNames.length));
		
		JLabel infoLabel = new JLabel("QTD: " + packet.queryPacket.getQueryName() + ", Threshold: "	+ selecThreshold+"%");
		infoLabel.setFont(f);
		infoLabel.setForeground(Color.RED);
		
		JPanel[] selecPanels = new JPanel[relNames.length];
		JTable[] selecTables = new JTable[relNames.length];
		
		float[] picassoSelec = gdp.getPicassoSelectivity();
		float[] planSelec = gdp.getPlanSelectivity();
		float[] predSelec = gdp.getPredicateSelectivity();
		String[] constants = gdp.getConstants();
		
		//apexp
		DecimalFormat df;
		if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
			 df = new DecimalFormat("0.00");
			df.setMaximumFractionDigits(2);
		}
		else
		{
			df = new DecimalFormat("0.00000");
			df.setMaximumFractionDigits(5);
		}
		//end apexp
		
		// int res = gdp.getResolution(0);//rss
		int ressum = 0;
		for ( int i=0; i < relNames.length; i++ ) //dimension loop
		{
			int res = gdp.getResolution(i);//rss
			String[] columnNames = new String[6];
			columnNames[0] = "Picasso Sel.";
			columnNames[1] = "Constant";
			columnNames[2] = "Predicate Sel.";
			columnNames[5] = "Plan Sel.";
			columnNames[3] = "Relative Diff";
			columnNames[4] = "Absolute Diff";
			
			String[][] data = new String[res][6];
			boolean[] mark = new boolean[res];
			ressum = 0;
			for(int k = 0; k < i; k++)
				ressum += gdp.getResolution(k);
			for (int j=0; j < res; j++) 
			{
				data[j][0] = df.format(picassoSelec[ressum+j]);
				data[j][1] = constants[ressum+j];
				data[j][2] = df.format(predSelec[ressum+j]);
				data[j][5] = df.format(planSelec[ressum+j]);
				double err = -1;
				float abs = Math.abs(picassoSelec[ressum+j] - predSelec[ressum+j]);
				if(predSelec[ressum+j]>0)
				{
					err = (Math.abs(picassoSelec[ressum+j] - predSelec[ressum+j])*100) / predSelec[ressum+j];
					data[j][3] = df.format(err) + "%";
				} 
				else 
				{
					err = -1;
					data[j][3] = "Division by zero";
				}
				//System.out.println(selecThreshold + " Selec Threshold : " + gdp.getQueryPacket().getSelecThreshold());
				double sth = Double.parseDouble(selecThreshold);
				if ( (sth > 1 && abs < PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD) || (err <= sth && err >=0) ) // gdp.getQueryPacket().getSelecThreshold()
					mark[j] = false;
				else
					mark[j] = true;
				data[j][4] = df.format(Math.abs(picassoSelec[ressum+j] - predSelec[ressum+j]));
			}
			selecPanels[i] = new JPanel(new BorderLayout());
			JLabel lbl = new JLabel("Relation: " + relNames[i]
			                                                + ", Attribute: " + attrNames[i]);
			lbl.setFont(f);
			lbl.setForeground(Color.magenta);
			
			Font f1 = new Font("Arial", Font.PLAIN, 12);
			selecPanels[i].add(lbl, BorderLayout.NORTH);
			selecTables[i] = new JTable(data, columnNames);
			selecTables[i].setToolTipText(relNames[i]);
			ColoredTableCellRenderer myCellRenderer = new ColoredTableCellRenderer(
					mark);
			selecTables[i].setDefaultRenderer(Object.class, myCellRenderer);
			selecTables[i].setFont(f1);
			selecTables[i].setForeground(Color.BLUE);
			selecPanels[i].add(new JScrollPane(selecTables[i]),BorderLayout.CENTER);
			logPanel.add(selecPanels[i]);
		}
		
		add(infoLabel, BorderLayout.NORTH);
		add(new JScrollPane(logPanel), BorderLayout.CENTER);
		return 0;
	}
	
	class ColoredTableCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 5545730712516929367L;
		boolean[] toColor = null;
		
		ColoredTableCellRenderer(boolean[] mark) {
			toColor = mark;
			//MessageUtil.CPrintToConsole("In toColor");
		}
		
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {
			
			//MessageUtil.CPrintToConsole(column + " In Table cell renderer :: " + row + "," + toColor[row]);
			Component c = 
				super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus,
						row, column);
			
			// Only for specific cell
			if ((toColor != null && toColor.length >= row && toColor[row] == true )) {
				//c.setFont(/* special font*/);
				// you may want to address isSelected here too
				c.setForeground(Color.RED);
				//c.setBackground(/*special background color*/);
			} else
				c.setForeground(Color.BLUE);
			return c;
		}
	}
	
	String getSelectivityString(int cond, int res, double[] selec, String str) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		for (int j=0; j < res; j++) {
			str += "(";
			for (int i=0; i < cond; i++) {
				if ( i != (cond - 1) )
					str += df.format(selec[i*res+j]) + ", ";
				else str += df.format(selec[i*res+j]);
			}
			str += ")\n";
		}
		return str;
	}
}


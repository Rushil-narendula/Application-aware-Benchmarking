
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

import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.frame.PredicateValuesFrame;
import iisc.dsl.picasso.client.util.DiagramUtil;
import iisc.dsl.picasso.client.util.PicassoUtil;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.ds.TreeNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JOptionPane;

import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.MouseBehavior;
import visad.VisADException;

public class PlanPanel extends PicassoPanel implements DisplayListener {
    
    private static final long serialVersionUID = -2759328818436642956L;
    //made public so that it can be checked from PrintDiagramPanel
    public javax.swing.JToggleButton planDiff;
   
    DisplayImpl		display;
    ServerPacket            opLevelPacket;
    ServerPacket            paramLevelPacket;
    //made public so that it can be checked from PrintDiagramPanel
    
    public static String diffLevel1 = "Parameter \u2192 Operator Diff";
    public static String diffLevel2 = "Operator \u2192 Parameter Diff";
    
    
   public PlanPanel(MainPanel app) {
        super(app);
        panelType = PLAN_DIAGRAM;
        setPanelString("Plan Diagram");
    
        BorderLayout bl = new BorderLayout();
		bl.setVgap(40);
		this.setBackground(Color.WHITE);
		setLayout(bl);
        
		addInfoPanel(PicassoConstants.PLAN_COLOR);
        
        planDiff = new javax.swing.JToggleButton();
        planDiff.setText(diffLevel1);
        PicassoConstants.OP_LVL=false;
        planDiff.setMaximumSize(new Dimension(100,40));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = infoLabels.length;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(40, 2, 2, 2);
        infoPanel.add(planDiff,c);
        planDiff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChangeDiagram(evt);
            }
        });
        
        opLevelPacket = null;
        
        //apa
        predicateValues = new Hashtable();
        addBottomPanel();
        //apae
    }
    public void emptyPanel() {
        super.emptyPanel();
        opLevelPacket = null;
        paramLevelPacket = null;
        //planDiff.setText(diffLevel1);
        planDiff.setVisible(false);
        planDiff.setSelected(false);
        setPanelLabel(" ");
        System.gc();
    }
    private void ChangeDiagram(java.awt.event.ActionEvent e) {
    	    planDiff.setVisible(false);
    	//param -> op
        if(planDiff.getText().equals(diffLevel1))
        {
            planDiff.setText(diffLevel2);
            PicassoConstants.OP_LVL=true;
            parent.getServerPacket().diagramPacket = parent.getFullDiagramPacket();
            
           	paramLevelPacket = new ServerPacket(parent.getServerPacket());
            opLevelPacket = new ServerPacket(paramLevelPacket);
        
            transformToOperatorLevel();
            parent.setParamSP(paramLevelPacket); // to recover the paramlevel packet when the button is toggled.
            parent.setFullDiagramPacket(opLevelPacket.diagramPacket);
            
            setSliceDiagramPacket(false);
            parent.retainSlice = true;
            opLevelPacket.diagramPacket = parent.getDiagramPacket();            
            
            parent.getServerPacket().diagramPacket = opLevelPacket.diagramPacket;
            parent.drawAllDiagrams(opLevelPacket,(PicassoPanel)this);
         
            parent.repaint();
        }
        
        else
        {
            planDiff.setText(diffLevel1);
            PicassoConstants.OP_LVL=false;
            if(paramLevelPacket == null)
            {
            	paramLevelPacket = parent.getParamSP();
            	// paramLevelPacket.diagramPacket = parent.getODP();
            	/*parent.getClientPacket().setMessageId(MessageIds.READ_PICASSO_DIAGRAM);
            	MessageUtil.sendMessageToServer(parent.getServerName(), parent.getServerPort(), parent.getClientPacket(), this);
            	return;*/
            }
            parent.setFullDiagramPacket(paramLevelPacket.diagramPacket);
            
            setSliceDiagramPacket(false);   
            parent.retainSlice = true;
            paramLevelPacket.diagramPacket = parent.getDiagramPacket();
                     
            parent.getServerPacket().diagramPacket = paramLevelPacket.diagramPacket;
            parent.drawAllDiagrams(paramLevelPacket,(PicassoPanel)this);
            parent.repaint();
        }  
        planDiff.setVisible(true);
    }
    

    public void transformToOperatorLevel() {
        Vector trees = paramLevelPacket.trees;
        boolean flag;
        int numPlans = ((Integer)trees.elementAt(0)).intValue();
        int newPlanNum[] = new int[numPlans];
        int newMaxPlans = 0;
        int i,j;
        newPlanNum[0]=newMaxPlans++;
        for(i=2;i<=numPlans;i++){
            flag=false;
            for(j = 1;j<i;j++){
                if(TreeSame((TreeNode)trees.elementAt(i*2),(TreeNode)trees.elementAt(j*2))){
                    flag = true;
                    break;
                }
            }
            if(flag)
                newPlanNum[i-1]=j-1;
            else{
                newPlanNum[i-1]=i-1;
                newMaxPlans++;
            }
        }
        DataValues data[] = opLevelPacket.diagramPacket.getData();
        for(i=0;i<data.length;i++)
            data[i].setPlanNumber(newPlanNum[data[i].getPlanNumber()]);
    }
    
    private boolean TreeSame(TreeNode root1, TreeNode root2) {
        //return true;
        Vector tree1 = new Vector();
        Vector tree2 = new Vector();
        tree1.add(0,root1);
        tree2.add(0,root2);
        TreeNode n1,n2;
        while(tree1.size()>0 && tree2.size()>0){
            n1=(TreeNode)tree1.remove(0);
            n2=(TreeNode)tree2.remove(0);
            
            // Avoids some null ptr errors that occur in some cases. ex: Q9@300 on DB2
            if((n1 == null || n2 == null))
            	return false;
            
            if(!n1.getNodeName().equals(n2.getNodeName()) || n1.getChildren().size() != n2.getChildren().size())
                return false;
            Vector temp;
            temp = n1.getChildren();
            for(int i =0 ; i< temp.size();i++)
                tree1.add(0,temp.get(i));
            temp = n2.getChildren();
            for(int i =0 ; i< temp.size();i++)
                tree2.add(0,temp.get(i));
        }
        if(tree1.size()==0 && tree2.size()==0)
            return true;
        else
            return false;
    }
    
    public void process(int msgType) {
        //MessageUtil.CPrintToConsole("In Plan Panel " + getPanelString());
        sendProcessToServer(msgType);
    }
    
    boolean noMouseBehavior = false;
    
    public DisplayImpl drawDiagram(ServerPacket packet, int type) {
        if ( display != null )
            display.removeDisplayListener(this);
        display = super.drawDiagram(packet, type);
        display.addDisplayListener(this);
        setInfoLabels(super.getDiagramPacket(), type, infoLabels);
        int selectivity = this.getPParent().getDBSettingsPanel().getSelecType();
        if ( selectivity != PicassoConstants.PICASSO_SELECTIVITY &&
                DiagramUtil.checkEngineSelectivity(selectivity, packet.diagramPacket) == false)
            noMouseBehavior = true;
        else
            noMouseBehavior = false;
        
        String warning = packet.warning;
        if ( warning != null && warning.length() > 3 ) {
            MessageUtil.CPrintToConsole("Warning :: " + warning);
            JOptionPane.showMessageDialog(this.getPParent().getParent(), "WARNING: " + warning, "Warning",JOptionPane.WARNING_MESSAGE); //+ "\nShow Selectivity Log ?");
        }
        
//        predicateValues = new Hashtable();
//        addToQueryParams();
//        fillBottomBar();
        //actionListener.actionPerformed(null);

        return display;
    }
    
    int mousePressedPlanNum = -1;
    
    public void displayChanged(DisplayEvent e) throws VisADException, RemoteException {

        
        if (noMouseBehavior == true)
            return;
        
        MouseBehavior mb = display.getDisplayRenderer().getMouseBehavior();
        
        double[] position1 = new double[] {1.0, 1.0, 1.0};
        double[] position2 = new double[] {-1.0, -1.0, -1.0};
        
        int[] screen1 = mb.getScreenCoords(position1);
        int[] screen2 = mb.getScreenCoords(position2);
        
        // Compute i and j here..
        // screen2[0] gives the bottom x coord
        // screen2[1] gives the bottom y coord
        // screen1[0] gives the top x coord
        // screen1[1] gives the top y coord
      
       DiagramPacket gdp = getPParent().getDiagramPacket();
       QueryPacket qp=null;
       DataValues[] data = null;
       if(gdp!=null)
    	  {
    	   qp = gdp.getQueryPacket();
           data = gdp.getData();
           
           /*System.out.println("IN PLAN PANEL");
           for(int i = gdp.getResolution(PicassoConstants.a[1])-1; i >=0; i--)
			{
				for(int j = 0; j < gdp.getResolution(PicassoConstants.a[0]); j++)
					System.out.print(data[i*gdp.getResolution(PicassoConstants.a[0])+j].getPlanNumber() + "  ");
				System.out.println("");
			}*/
    	  }
       else return;
        DiagramPacket fullgdp = getPParent().getFullDiagramPacket();
               
        int [] res = new int[fullgdp.getDimension()];//
        for(int i=0;i<fullgdp.getDimension();i++)//
        	res[i]=fullgdp.getResolution(i);//
        
        int ressum[] = new int [fullgdp.getDimension()];
        for(int i=1;i<fullgdp.getDimension();i++)//
        	ressum[i]=res[i-1]+ ressum[i-1];
        
      //  if ( fullgdp.getDimension() == 1 )
        //    res[0] = 0;
        
        // 0.8 is the aspect ratio...
        double diffX = (screen1[0] - screen2[0]) * ((1.0-PicassoConstants.ASPECT_2D_X)/2);
        double diffY = (screen2[1] - screen1[1]) * ((1.0-PicassoConstants.ASPECT_2D_Y)/2);
        
        double[] newScreen1 = new double[2];
        newScreen1[0] = screen1[0] - diffX;
        newScreen1[1] = screen1[1] + diffY;
        
        double[] newScreen2 = new double[2];
        newScreen2[0] = screen2[0] + diffX;
        newScreen2[1] = screen2[1] - diffY;
        
        double xRes = (newScreen1[0] - newScreen2[0])/gdp.getResolution(PicassoConstants.a[0]);//rss
        xRes=xRes*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
        
        double yRes = 1;
        if(gdp.getDimension() != 1)
        {	
        	yRes = (newScreen2[1] - newScreen1[1])/gdp.getResolution(PicassoConstants.a[1]);//rss
        	yRes = yRes*(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
        }
        
        else
       	{
        	yRes = (newScreen2[1] - newScreen1[1])/gdp.getResolution(PicassoConstants.a[0]);//rss
           	yRes = yRes*0.3;
       	}
    
        double xValue = (e.getX() - newScreen2[0])/xRes; // xvalue measured from left. intuitive. leftmost 0. rightmost is resolution.
        double yValue = (e.getY() - newScreen1[1])/yRes; //yvalue measured from the top. topmost is 0. bottom most is resolution
        
        int i;
        double iTrans; 
        if(gdp.getDimension() != 1)
         	iTrans = (1-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]))*gdp.getResolution(PicassoConstants.a[1])/(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[1])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[1]));
        else
        	iTrans=gdp.getResolution(PicassoConstants.a[0])/0.3;
        i = (int)(iTrans-yValue);//rss
        
      	double jTrans=gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0])*gdp.getResolution(PicassoConstants.a[0])/(gdp.getQueryPacket().getEndPoint(PicassoConstants.a[0])-gdp.getQueryPacket().getStartPoint(PicassoConstants.a[0]));
        int j = (int)(xValue-jTrans);
      
        int planNumber = -1;
        DataValues curData = data[0];
      
		//apexp
		if(gdp.getQueryPacket().getDistribution().equals(PicassoConstants.UNIFORM_DISTRIBUTION))
		{
		        if ( gdp.getDimension() != 1 ) 
		        {
		        	if ( i >= 0 && i < gdp.getResolution(PicassoConstants.a[1]) && j >=0 && j < gdp.getResolution(PicassoConstants.a[0]) )
		        		curData = data[i*res[PicassoConstants.a[0]]+j];
		        	else 
		        		return;
		        }
		        else 
		        {
		        	if ( i >= 0 && i < gdp.getResolution(PicassoConstants.a[0]) && j >=0 && j < gdp.getResolution(PicassoConstants.a[0]) )//rss
		        		curData = data[j];
		        	else 
		        		return;
		        }
		//apexp
		}
		
		else //exponential
		{
					
			double ires;
			if(gdp.getDimension() != 1)
			{
				ires=gdp.getResolution(PicassoConstants.a[1])/(qp.getEndPoint(PicassoConstants.a[1])-qp.getStartPoint(PicassoConstants.a[1]));
			}
			else
			{
				ires=gdp.getResolution(PicassoConstants.a[0])/0.3;	
			}
			
			double jres=gdp.getResolution(PicassoConstants.a[0])/(qp.getEndPoint(PicassoConstants.a[0])-qp.getStartPoint(PicassoConstants.a[0]));
			
			double fi=(ires-yValue)*100.0/ires;
			double fj=xValue*100.0/ jres;//rss
			
			//fi and fj now represent the selectivity of the point on scale of 0-100
		
			
			if(gdp.getDimension()==1)
			{
				if ( !(fi >= 0 && fi <=30 && fj>=qp.getStartPoint(0)*100 && fj<=qp.getEndPoint(0)*100))
					return;
			}
			double filow,fihigh;
			if(qp.getDimension()==1)
			{
				filow=0;
				fihigh=30;
			}
			else
			{
				filow=qp.getStartPoint(PicassoConstants.a[1])*100;
				fihigh=qp.getEndPoint(PicassoConstants.a[1])*100;
			}
			//do for all dims
		
				if ( fi >= filow && fi <=fihigh && fj >=qp.getStartPoint(PicassoConstants.a[0])*100 && fj <=100*qp.getEndPoint(PicassoConstants.a[0]) )
				{		
					float[] selvals;
					selvals=gdp.getPicassoSelectivity();
		        			        	
					int iflag=0,jflag=0;
					
					if(gdp.getDimension()!=1)										//L B
					{	if(fi<selvals[ressum[PicassoConstants.a[1]]]) 
						{ 
						i=0; iflag=1; 
						}															//O O
					}																//W U
					else															//E N
						if(fi<selvals[0]) { i=0; iflag=1; }							//R D
					if(fj<selvals[ressum[PicassoConstants.a[0]]]) { j=0; jflag=1; }								//	
					
					//UPPER BOUND
					if(gdp.getDimension()!=1)
					{
						int offset=ressum[PicassoConstants.a[1]]+res[PicassoConstants.a[1]];
					if(fi>=(selvals[offset-1]+selvals[offset-2])/2) 
						{ 
							i=gdp.getResolution(PicassoConstants.a[1])-1;
							iflag=1; 
						}
					}
					int offset=ressum[PicassoConstants.a[0]]+res[PicassoConstants.a[0]];
					if(fj>=(selvals[offset-1]+selvals[offset-2])/2) 
					{ j=gdp.getResolution(PicassoConstants.a[0])-1; jflag=1; }
					//SELECTIVY OVER FIRST DIMENSION
					float prev=selvals[ressum[PicassoConstants.a[0]]];
					for(int k=ressum[PicassoConstants.a[0]];k<(ressum[PicassoConstants.a[0]]+res[PicassoConstants.a[0]]);k++)//rss
					{
						if(jflag==1) break;
						
						if(jflag==0)
						if(fj>prev && fj<=(selvals[k+1]+selvals[k])/2)
						{
							j=k;
							jflag=1;
							j=j-ressum[PicassoConstants.a[0]];
						}
						prev=(selvals[k+1]+selvals[k])/2;
					}
					
					if(gdp.getDimension()!=1)
					{
					prev=selvals[ressum[PicassoConstants.a[1]]];
					for(int k=ressum[PicassoConstants.a[1]];k<ressum[PicassoConstants.a[1]]+res[PicassoConstants.a[1]];k++)
					{
						if(iflag==1)break;
						if(iflag==0)
						
						if(fi>prev && fi<=(selvals[k+1]+selvals[k])/2)
						{
							i=k;
							iflag=1;
							i=i-ressum[PicassoConstants.a[1]];
						//	i=i-gdp.getResolution(PicassoConstants.a[0]);
						}
						prev=(selvals[k+1]+selvals[k])/2;
					}
				
					}
					else i=0;
					curData = data[i*res[PicassoConstants.a[0]]+j];
				}
			
				//clicking outside the graph range 
				else {
					//display.getDisplayRenderer().setForegroundColor(PicassoConstants.IMAGE_BACKGROUND);
					return;
				}
		}
		//end apexp
        // display.getDisplayRenderer().setForegroundColor(PicassoConstants.PLAN_COLOR);
        planNumber = curData.getPlanNumber();
        InputEvent ie = e.getInputEvent();
        int mods = ie.getModifiers();
        String[] rel = gdp.getRelationNames();
        String[] attr = gdp.getAttributeNames();
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
        
        DecimalFormat df1 = new DecimalFormat("0.00E0");
        df1.setMaximumFractionDigits(2);
        
        float[] pselec = gdp.getPicassoSelectivity();
        float xDSelec = j>=0?pselec[ressum[PicassoConstants.a[0]]+j]:0;
        float yDSelec = 0;
        if(gdp.getDimension() != 1)
        	yDSelec = (ressum[PicassoConstants.a[1]]+i>0 && ressum[PicassoConstants.a[1]]+i<pselec.length)?pselec[ressum[PicassoConstants.a[1]]+i]:0;//rss
        
        String xSelec = "" + df.format(xDSelec);
        String ySelec = "" + df.format(yDSelec);
        
        String[] infoStr = new String[4];
        infoStr[0] = "Cost: " + df1.format(curData.getCost()) + ", Card: " + df1.format(curData.getCard()) + "      ";
        
        infoStr[1] = rel[0] + "." + attr[0] + " [" + "Selectivity: " + xSelec + ", Constant: " + constants[/*ressum[PicassoConstants.a[0]]+*/j] + "]";

        if ( fullgdp.getDimension() == 1 )
            infoStr[2] = "";
        else
            infoStr[2] = rel[1] + "." + attr[1] + " [" + "Selectivity: " + ySelec + ", Constant: " + constants[res[PicassoConstants.a[0]]+i] + "]";
        
//        if ( gdp.getDimension() > 2 ) {
//            Hashtable attrSelec = getPParent().getClientPacket().getAttributeSelectivities();
//            Object[] keys = attrSelec.keySet().toArray();
//            int index = ((Integer)keys[0]).intValue();
//            
//            String attrName = getPParent().getQueryAttrName(index);
//            String dimSelec =  "Sel[" + attrName + "]=" + attrSelec.get(keys[0]) + "%";
//            infoStr[3] = dimSelec;
//        }
        /*********************All Plan Save**********/
		
        
        if ( e.getId() == DisplayEvent.MOUSE_PRESSED_RIGHT ) {
            mousePressedPlanNum = planNumber;
            //MessageUtil.CPrintToConsole("(" + i + "," + j + ")" + e.getId() + " X :: " + e.getX() + " Y :: " + e.getY() + " Plan # " + planNumber);
        } else if (e.getId() == DisplayEvent.MOUSE_RELEASED_RIGHT ) {
			//apexp
			
			//DEBUG:
		//	System.out.println("" + e.getX() + " " + e.getY());
			//screen1 = mb.getScreenCoords(position1);
			//screen2 = mb.getScreenCoords(position2);
			//System.out.println("i,j: " + (xValue*100.0)/gdp.getResolution() + " " + ((gdp.getResolution() - yValue)*100.0)/gdp.getResolution());
			/*
			for(i=0;i<screen1.length;i++)
			{System.out.print(" " + screen1[i]);
			}
			System.out.println();
			for(i=0;i<screen2.length;i++)
			{System.out.print(" " + screen2[i]);
			}
			*/
			//end apexp
			//added for multiplan display
        	int key = e.getKeyCode();
			if((mods & InputEvent.CTRL_MASK)!=0 &&(mods & InputEvent.ALT_MASK)!=0) 
			{
				//JOptionPane.showMessageDialog(this.getPParent().getParent(), "u pressed ctrl shift and right click",
                  //                                  "INFO",JOptionPane.INFORMATION_MESSAGE);
				//JOptionPane.showOptionDialog(this.getPParent().getParent(),"Which engine ...","Engine selection",1,1,NULL,);
				
				//add(apcombo);
				new MpDatabaseSelectionFrame(parent,this,xDSelec,yDSelec,planNumber).setVisible(true);
			}
			
			else //addition for multiplan ends here
				if ((mods & InputEvent.CTRL_MASK) != 0) {
                PicassoUtil.displayCompiledTree(parent, this, infoStr, gdp.getMaxResolution(), xDSelec, yDSelec, planNumber);//rss
            } else if ((mods & InputEvent.ALT_MASK) != 0 && PicassoConstants.ENABLE_COST_MODEL) {
                String dbVendor = parent.getDBSettingsPanel().getCurrentDBSettings().getDbVendor();
                if (!(dbVendor.equalsIgnoreCase(DBConstants.MSSQL)) && !(dbVendor.equalsIgnoreCase(DBConstants.SYBASE))) {
                    JOptionPane.showMessageDialog(parent,"The Abstract Plan function is supported only for MS SQLServer 2005 and Sybase 15","Function Not Supported",JOptionPane.INFORMATION_MESSAGE);
                } else
                    PicassoUtil.getAbsPlan(parent, this, infoStr, gdp.getMaxResolution(), xDSelec, yDSelec, planNumber);//rss
            } else if ((mods & InputEvent.SHIFT_MASK) != 0) {
                display.getDisplayRenderer().setCursorOn(false);
                double cost = 0.0;
                if ( gdp.getDimension() == 1 )
                    cost = data[j].getCost();
                else
                    cost = data[i*gdp.getResolution(PicassoConstants.a[0])+j].getCost();//rss
                df = new DecimalFormat("0.00E0");
                df.setMaximumFractionDigits(2);
                if ( gdp.getDimension() == 1 ){
                    JOptionPane.showMessageDialog(this.getPParent().getParent(), "Selectivity = (" + xSelec
                            + ")\nConstant = (" + constants[j]
                            + ")\nPlan = P"
                            + (getPParent().getSortedPlan()[0][planNumber]+1)
                            + "\nCost = " + df.format(cost)
                            + "\nCardinality = " + df.format(curData.getCard()),
                            "QueryInfo",JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this.getPParent().getParent(), "Selectivities = (" + xSelec + "," + ySelec
                            + ")\nConstants = (" + constants[j] + "," + constants[res[PicassoConstants.a[0]]+i]
                            + ")\nPlan = P"
                            + (getPParent().getSortedPlan()[0][planNumber]+1)
                            + "\nCost = " + df.format(cost)
                            + "\nCardinality = " + df.format(curData.getCard()),
                            "QueryInfo",JOptionPane.INFORMATION_MESSAGE);
                }
            }     
                   
            else {
                String planStr = "";
                if ( planNumber != -1 && mousePressedPlanNum != -1 ) {
                    if ( mousePressedPlanNum == planNumber )
                        planStr += planNumber;
                    else planStr = mousePressedPlanNum + "," + planNumber;
                    //MessageUtil.CPrintToConsole("PlanStr :: " + planStr);
                    PicassoUtil.displayTree(parent, this, planStr);
                    
                }
            }
			
            // MessageUtil.CPrintToConsole("In Mouse Released Right :: " + e.getX() + " Y :: " + e.getY() + " Plan # " + planNumber);
        } /*else if ( e.getId() == DisplayEvent.MOUSE_PRESSED_CENTER ) {*/
        else if(e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER && (mods & InputEvent.SHIFT_MASK) != 0)
	    {
	        String planStr = "";
	        PicassoUtil.displayTreeAD(parent, this, planStr);	        
	    }  
        
    }
        
}

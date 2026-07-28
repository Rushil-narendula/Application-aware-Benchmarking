
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

package iisc.dsl.picasso.client.panel;import iisc.dsl.picasso.client.util.Draw1DDiagram;import iisc.dsl.picasso.client.util.Draw3DDiagram;import iisc.dsl.picasso.common.PicassoConstants;import iisc.dsl.picasso.common.ServerPacket;import iisc.dsl.picasso.common.ds.DiagramPacket;import java.awt.BorderLayout;import java.awt.Color;import java.awt.Graphics;import java.util.Hashtable;import visad.DisplayImpl;import visad.ScalarMap;
public class PlanCardPanel extends PicassoPanel {
	private static final long serialVersionUID = -553070827640285282L;
	PlanCardPanel				myPanel;	DisplayImpl					twoDdisplay=null;	ScalarMap[]					twoDmaps;
	public PlanCardPanel(MainPanel app, int pType) {		super(app);		myPanel = this;		twoDmaps = new ScalarMap[4];		for (int i=0; i < 4; i++)			twoDmaps[i] = null;
		panelType = pType;
		setPanelString("Compilation Cardinality Diagram");				BorderLayout bl = new BorderLayout();		bl.setVgap(40);		this.setBackground(Color.WHITE);		setLayout(bl);				if ( panelType == EXEC_PLAN_CARD_DIAGRAM) {
			panelString = "Execution Cardinality Diagram";
			addInfoPanel(PicassoConstants.EXEC_COLOR);
		} else
			addInfoPanel(PicassoConstants.PLAN_COLOR);
		
		//apa
        predicateValues = new Hashtable();
        addBottomPanel();
        //apae
	}
	
	public void process(int msgType) {
		//MessageUtil.CPrintToConsole("In Plan Cardinality Panel " + getPanelString());
		sendProcessToServer(msgType);
	}
	
	public void update(Graphics g) {
		super.paint(g);
	}
	
	public DisplayImpl drawDiagram(ServerPacket msg, int type) {
		DisplayImpl display1=null;

		//MessageUtil.CPrintToConsole(panelType + "," + type + " In Draw Graph of ::: " + panelString);
		//DiagramPacket gdp = msg.diagramPacket;
                DiagramPacket gdp = null; 
        if ( panelType == EXEC_PLAN_CARD_DIAGRAM) {
			gdp = parent.getExecDiagramPacket();
		} else
			gdp = parent.getDiagramPacket();
		if ( gdp == null )
			return null;

		enableRegen(true);
		if ( gdp.getDimension() == 1 ) {
			display1 = Draw1DDiagram.draw(twoDdisplay, getPParent(), gdp, type, twoDmaps);
			twoDdisplay = display1;
			display = display1;
		} else {
			display1 = Draw3DDiagram.draw(display, getPParent(), gdp, type, maps);
			display = display1;
		}

		//this.removeAll();
		diagramComponent = display1.getComponent();
		add(diagramComponent, BorderLayout.CENTER);
		validate();
		setInfoLabels(gdp, type, infoLabels);
		return display1;
	}
	
	public DisplayImpl getDisplayImage() {
		DiagramPacket gdp = null;
		
		if ( panelType == EXEC_PLAN_CARD_DIAGRAM) {
			gdp = parent.getExecDiagramPacket();
		} else
			gdp = parent.getDiagramPacket();
		if ( gdp == null )
			return null;

		if ( gdp.getDimension() == 1 )
			return(twoDdisplay);
		else
			return display;
	}
	
	/*public DisplayImpl drawDiagram(ServerPacket msg, int type) {
		display = super.drawDiagram(msg, type);
		setInfoLabels(msg, type, infoLabels);
		return display;
	}*/
}

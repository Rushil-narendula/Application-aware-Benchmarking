
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

package iisc.dsl.picasso.common;

import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

public class ServerPacket implements Serializable {

	private static final long serialVersionUID = 2L;
	public int 			clientId;
	public int 			messageId;
	public int			progress;
	
	public String 			status;
	public String			warning;
	
	public QueryPacket		queryPacket;
	public DiagramPacket	diagramPacket;
	public byte[] compressedDiagramPacket;
	
	public Vector			trees; // # of Plans, planNumber1, treeNode1, planNumber2, treeNode2 and so on..
	public byte[] compressedTrees;
	
	public Vector			queries;

	//added for multiplan
	public HashMap hashmap;
	//addition for multiplan ends here
	
	public String 			absPlan; //This is the query appended with plan
	public int 				optClass; //ADG    
	
        public ServerPacket()
        {
            ;
        }
        public ServerPacket(ServerPacket p)
        {
            clientId = p.clientId;
            messageId = p.messageId;
            progress = p.progress;
            
            if(p.status!=null)
                status = new String(p.status);
            if(p.warning!=null)
                warning = new String(p.warning);
            
            queryPacket = new QueryPacket(p.queryPacket);
            diagramPacket = new DiagramPacket(p.diagramPacket);
            
            if(p.trees!=null)
            {
            	trees = new Vector();
                for(int i=0;i<p.trees.size();i++)
                    trees.add(i,p.trees.get(i));
            }
            
            if(p.queries!=null)
            {
            	queries = new Vector();
                for(int i=0;i<p.queries.size();i++)
                	queries.add(i,p.queries.get(i));
            }
            
            if(p.hashmap != null)
            {
            	hashmap = new HashMap();
            	hashmap = new HashMap(p.hashmap);
            }
            
            if(p.absPlan!=null)
                absPlan=new String(p.absPlan);
            
            optClass = p.optClass;
       }
}

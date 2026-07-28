
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

package iisc.dsl.picasso.server.query;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.mssql.MSSQLDatabase;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class Query {
	private String qName;
	private String qTemplate;
	private String parts[];
	private Histogram hist[];
	private String schemas[];
	private String relations[];
	private String aliases[];
	private String attributes[];
	private int dimension;

	public Query(String queryName, String queryText, Database db) throws PicassoException
	{
		qName = queryName;
                setQueryTemplate(queryText);
		hist = new Histogram[dimension];
		schemas = new String[dimension];
		relations = new String[dimension];
		aliases = new String[dimension];
		attributes = new String[dimension];
		PicassoParser parser = new PicassoParser();
		parser.parse(db,qTemplate,dimension,schemas,relations,aliases,attributes);
		if(dimension>PicassoConstants.MAX_DIMS)
			throw new PicassoException("Maximum number of allowed dimensions is 4");
		for(int i=0;i<dimension;i++){
			System.out.println("PICASSO PREDICATE varies on Attribute<"+attributes[i]+"> relation<"+
					relations[i]+"> schema<"+schemas[i]+">");
			hist[i] = db.getHistogram(relations[i],schemas[i],attributes[i]);
			for(int j=i+1;j<dimension;j++)
				if(aliases[i].equalsIgnoreCase(aliases[j]))
					throw new PicassoException("Choose Picasso predicates on different relations");
		}
	}
	
    public void setQueryTemplate(String qt)
    {
    	qTemplate = qt;
        parts = qTemplate.split("(:varies)|(:VARIES)");
        dimension = parts.length - 1;
        if((qTemplate.endsWith(":varies") || qTemplate.endsWith(":VARIES")))
        {
            dimension+=1;
            String tmp[] = new String[parts.length+1];
            for(int i=0 ; i<parts.length ; i++)
                tmp[i]=parts[i];
            tmp[parts.length] = "";
            parts=tmp;
        }
        //thus, dimension is always set to parts.length - 1
    }

    public static Query getQuery(QueryPacket qp, Database database) throws PicassoException
    {
		Query query = new Query(qp.getQueryName(), qp.getQueryTemplate(), database);
		if(query.getDimension()<=0)
		{
			ServerMessageUtil.SPrintToConsole("No range predicates are selected using :varies");
			throw new PicassoException("No range predicates are selected using :varies");
		}
	 // Setting dimension value is not the right way. But is done for practicality
		qp.setDimension(query.getDimension());
		return query;
    }

	public void genConstants(int resolution[], String distribution, double [] startpoint, double [] endpoint)
	{
		for(int i=0;i<dimension;i++)
			hist[i].genConstants(resolution[i], distribution, startpoint[i], endpoint[i]);
	}
	
	public String getQueryname()
	{
		return qName;
	}
	
	public String getQueryTemplate()
	{
		return qTemplate;
	}
	
	public int getDimension()
	{
		return dimension;
	}
	
	public String getRelationName(int i)
	{
		return relations[i];
	}

	public String getAliasName(int i)
	{
		return aliases[i];
	}

	public String getAttribName(int i)
	{
		return attributes[i];
	}
	
	public double getRelationCard(int i) {
		return hist[i].getCardinality();
	}
	
	public String generateQuery(double sel[])
	{
		int i;
		
		String newQuery = parts[0];
                if(parts.length==1)
                {
                	if(!(/* hist[0].getAttribType().equals("string") || */ hist[0].getAttribType().equals("date")))
                		return newQuery+" <= " + hist[0].getConstant(sel[0]);
                	else
                		return newQuery+" <= '" +  hist[0].getConstant(sel[0]) + "'";
                }
                	
                else if(sel.length != parts.length-1)
			return null;
		for(i=0;i<sel.length; i++) {
	    	if(!(/* hist[i].getAttribType().equals("string") || */ hist[i].getAttribType().equals("date")))
	    		newQuery += " <= " + hist[i].getConstant(sel[i])+parts[i+1];
	    	else
	    		newQuery += " <= '" + hist[i].getConstant(sel[i])+ "' "+parts[i+1];
		}
		return newQuery;
	}
	//added for multiplan
	//this function just returns whatever is there is hist[i] and 
	//assumes everything was created before by other calls
	public String[] getSelectivityValue(double sel1[])
	{
		try{
			String[] retstrarr = new String[sel1.length];
			for(int i=0;i<sel1.length; i++) {
				retstrarr[i]=hist[i].getConstant(sel1[i]);
			}
			return retstrarr;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	//addition for multiplan ends here
	/*
	 * This function assumes a call to genConstants before being invoked!
	 */
	public String generateQuery(int index[])
	{
		int i;
		String newQuery = parts[0];
                if(parts.length==1)
                {
                	if(!(hist[0].getAttribType().equals("date") /*|| hist[0].getAttribType().equals("string")*/))
                		return newQuery+" <= " + hist[0].getConstant(index[0]);
                	else
                		return newQuery+" <= '" + hist[0].getConstant(index[0]) + "'";
                }	
		if(index.length != parts.length-1)
			return null;
		for(i=0;i<index.length; i++) {
			if(!(hist[i].getAttribType().equals("date") /*|| hist[i].getAttribType().equals("string")*/))
				newQuery += " <= " + hist[i].getConstant(index[i])+parts[i+1];
			else
				newQuery += " <= '" + hist[i].getConstant(index[i])+"' "+ parts[i+1];
		}
		return newQuery;
	}
	public Histogram getHistogram(int i)
	{
		return hist[i];
	}

}

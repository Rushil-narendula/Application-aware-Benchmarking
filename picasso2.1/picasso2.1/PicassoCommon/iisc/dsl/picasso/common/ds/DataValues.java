
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

package iisc.dsl.picasso.common.ds;

import java.io.Serializable;

public class DataValues implements Serializable  {
	private	int 		planNumber;
	private	double		cost;			// The cost associated with it
	private	double 		cardinality;	// The cardinality associated with it.
	public boolean isRepresentative; 	// true for optimized points
	public float succProb; 				// probability of area error
	public boolean FPCdone;				// if FPC is already done
    public int getPlanNumber() {
		return(planNumber);
	}
    
	public void setPlanNumber(int plan) {
		planNumber = plan;
	}
	
	public double getCost() {
		return(cost);
	}
	
	public void setCost(double c) {
		cost = c;
	}
	
	public double getCard() {
		return(cardinality);
	}
	
	public void setCard(double c) {
		cardinality = c;
	}
	
	public DataValues copy() {
		DataValues dv = new DataValues();
		dv.setCard(cardinality);
		dv.setCost(cost);
		dv.setPlanNumber(planNumber);
		return dv;
	}
	
	public void printToConsole() {
		System.out.println("DATA VALUES : " + planNumber + " " + cost 
				+ " " + cardinality);
	}
	public void setRepresentative(boolean flag) //ADG
	{
		isRepresentative = flag;
	}
}

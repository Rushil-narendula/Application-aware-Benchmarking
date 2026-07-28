/**
 * 
 */
package iisc.dsl.picasso.server.sampling;

import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
//import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.common.PicassoConstants;

import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;


public class SRS extends PicassoSampling {

	public final int estimationBudget = PicassoConstants.RSNN_ESTIMATION_BUDGET;
	public FileWriter SRSwriter =  null;
	public SRS(Socket s, ObjectInputStream in, ObjectOutputStream out,
			ClientPacket cp, Database db) {
		super(s, in, out, cp, db);
		
	}
	
/*	void recursiveInitGrid(int depth)
	{
	    	if(depth == 1)
	    	{
	    		index[depth-1]=0;
	    		{    			
	    			try {	    				
						setPlanResult();						
					} catch (PicassoException e) {
						e.printStackTrace();
					}	
	    		}
	    		index[depth-1] = resolution[depth-1]-1;
	    		try {	    			
					setPlanResult();					
				} catch (PicassoException e) {					
					e.printStackTrace();
				}	
	    	}
	    	else
	    	{
	    		index[depth-1]=0;
	    		recursiveInitGrid(depth-1);    			
	    		    		
	    		index[depth-1] = resolution[depth-1]-1;
	    		recursiveInitGrid(depth-1);
	    	}
	}*/
	public void doOptimize()
	{				
		double tmpIndex = 0;
    	activity = -1;
    	int count = PicassoConstants.SEGMENT_SIZE;
    	count = sendProgress(count,0,startTime,activity);
		SRSPool SRS =  new SRSPool(totalSize);	
		int interval = (int)(totalSize * 0.01);
		if(interval == 0)
			interval = 2;
		count = 0;
		activity = 0;
		//Optimize all corner points
		recursiveInitCorner(dimension,resolution);
		while(sampleSize < reqSampleSize) {
			try {
				ifInterruptedStop();
				ifPausedSleep();
			} catch (PicassoException e1) {
				;
			}
			tmpIndex = -1;
			while(tmpIndex==-1)
				tmpIndex = SRS.sampleWithoutReplacement();
			
			index = getIndexBack((int)tmpIndex);			
			try {
				setPlanResult();
			} catch (PicassoException e) {
				;
			}
			if(count == PicassoConstants.SEGMENT_SIZE){
				try {
					database.emptyPlanTable();
				} catch (PicassoException e) {
					;
				}
			}
			count = sendProgress(count,sampleSize,startTime,activity);
			if(sampleSize % interval == 0)
			{
				if(srsSTOP(SRS_HYBRID)) // Identity Error Part
				{					
					doInterpolate();
					if(skipOptimize())
						compareRunTime();						
					if(estAreaError <= errorPercent_L) //Area Error Part
					{
						reqSampleSize = sampleSize;						
					}					
				}
			}
			
		}		
		
	}
	int singletonPlans;
	final int SRS_HYBRID = 0;
	final int SRS_D_ML = 1;
	final int SRS_D_MAX = 2;
	private boolean srsSTOP(int stopMode)
	{
		singletonPlans = 0;
		if(sampleSize < totalSize*0.05)
			return false;
		double estPlans = 0;
		double discoveredPlans = plans.size();
		for(int i=0;i<plans.size();i++)
		{
			if(plan_area.get(i).toString().equals("1"))
				singletonPlans++;
		}
		switch(stopMode)
		{
			case SRS_HYBRID: estPlans = d_HYBRID();break;
			case SRS_D_ML: estPlans = d_ML();break;
			case SRS_D_MAX: estPlans = d_MAX();break;
		}
		double relErr = ((estPlans - discoveredPlans)*100)/estPlans;			
		estIdError = relErr;
		if(relErr > errorPercent_I) {
			return false;
		} else {
			return true;
		}		
	}
	private double d_HYBRID()
	{		
		double d = d_MAX();
		if(d <= (1.3 * plans.size()))
		{			
			d = d_ML();
		}
		return(d);		
	}
	private double d_ML()
	{		
		return((Math.sqrt((double)totalSize/(double)sampleSize) - 1.0) * singletonPlans + plans.size());		
	}
	private double d_MAX()
	{
		return(((double)totalSize/(double)sampleSize - 1.0) * singletonPlans + plans.size());		
	}
	private double d_ML_Est(double size)
	{		
		return((Math.sqrt(size/(double)sampleSize) - 1.0) * singletonPlans + plans.size());		
	}
	private double d_MAX_Est(double size)
	{
		return((size/(double)sampleSize - 1.0) * singletonPlans + plans.size());		
	}
	float succProb = (float)0.0;
	double estAreaError=0.0,estIdError=0.0,estAreaError2=0.0;
	public void doInterpolate()
	{		
		int inx=0,tmpInx=0,count=0;
		int wrgPts = 0,restrictedWrgPts=0;
		int pointsEvaluated = sampleSize;				
		int boxDim[] = new int[dimension];
		
		for(int i=0;i<dimension;i++){			
			index[i]=resolution[i] - 1;
			boxDim[i] = 1;
		}		
		endIteration = false;
		tied = 0;		
		activity = 1;
		try{
			while(!endIteration) {				
				ifInterruptedStop();
				ifPausedSleep();
				inx = getIndex(index);			
				if(data[inx]==null)
				{					
					tmpInx = findNearestNeighbor();						
					data[inx] = setData(data[inx],data[tmpInx]);
					if(FPCMode)
					{
						data[inx].setCost(fpcCost);
						data[inx].FPCdone = true;
					}
					data[inx].succProb = succProb;
					if(succProb<=0.5)
						wrgPts++;
					if(succProb<0.9)
						restrictedWrgPts++;
											
				}
				
				pointsEvaluated++;
				if(count == PicassoConstants.SEGMENT_SIZE){
					database.emptyPlanTable();
				}				
				count = sendProgress(count,pointsEvaluated,startTime,activity);
				index = decrIndex(index,boxDim);	
			}
		}
		catch(Exception e)
		{
			;
		}		
		//System.out.println(" Wrong pts : "+wrgPts);
		//System.out.println(" Restricted Wrong pts : "+restrictedWrgPts);
		estAreaError = (double)(wrgPts+(double)tied/2.0)*100.0 / (double)totalSize;
		
	}
	
	int tied = 0;
	void recursiveInitCorner(int depth,int[] interval)
    {
    	if(depth == 1)
    	{
    		for(index[depth-1]=0;index[depth-1]<=interval[depth-1]-1;index[depth-1]+=interval[depth-1]-1)
    		{   				    				
    			try {
					setPlanResult();
				} catch (PicassoException e) {
					;
				}	
    		}
    	}
    	else
    	{
    		for(index[depth-1]=0;index[depth-1]<=interval[depth-1]-1;index[depth-1]+=interval[depth-1]-1)
    		{
    			recursiveInitCorner(depth-1,interval);    			
    		}    		
    		
    	}
    }
	public int estimateTime() throws PicassoException
	{		
		isEstimationProcess = true;
		initializeParams();
		int diagSize = 1;
		double areaFraction = 1.0, indexFraction = 1.5;
		for(int i = 0;i < dimension;i++) {
			diagSize *= resolution[i];
			areaFraction *= indexFraction;
		}
		if(diagSize < PicassoConstants.RSNN_ESTIMATION_START_THRESHOLD)
			return 60;
		long startTime = System.currentTimeMillis();
		double errorBound = (errorPercent_I < errorPercent_L)?errorPercent_I:errorPercent_L;
		int fullSize = totalSize;
		double fieldSize = (double)totalSize / areaFraction;
		int samplesTaken = 0;		
		Random estGen = new Random(123456);
		boolean flag = true;
		double est1=100.0,est2,estPlans,discoveredPlans,sampleEstimate;		
		while(samplesTaken < estimationBudget) {
			int tmp = estGen.nextInt(totalSize);
			if(tmp == -1)
				continue;
			index = getIndexBack(tmp);
			for(int i = 0;i < dimension;i++)
				index[i] = (int)((double)index[i]/indexFraction);
			try {
				setPlanResult();
			} catch (PicassoException e) {
				;
			}
			samplesTaken++;
			if(samplesTaken >= estimationBudget/2 && flag) {
				flag = false;
				singletonPlans = 0;
				discoveredPlans = plans.size();
				for(int i=0;i<plans.size();i++)
				{
					if(plan_area.get(i).toString().equals("1"))
						singletonPlans++;
				}
				//estPlans = (d_ML_Est(fieldSize)+2.0*d_MAX_Est(fieldSize))/3.0;
				estPlans = d_MAX_Est(fieldSize);
				est1 = ((estPlans - discoveredPlans)*100)/estPlans;			
			}
		}
		singletonPlans = 0;
		discoveredPlans = plans.size();
		for(int i=0;i<plans.size();i++)
		{
			if(plan_area.get(i).toString().equals("1"))
				singletonPlans++;
		}
		//estPlans = (d_ML_Est(fieldSize)+2.0*d_MAX_Est(fieldSize))/3.0;
		estPlans = d_MAX_Est(fieldSize);
		est2 = ((estPlans - discoveredPlans)*100)/estPlans;
		if(est2 > est1) {
			double temp = est2;
			est2 = est1;
			est1 = temp;
		}
		timeTaken = System.currentTimeMillis()-startTime;
		long actualEstimate = (long)((double)timeTaken / estimationBudget);
		sampleEstimate = (estimationBudget*errorBound/2.0+(est1*estimationBudget-est2*estimationBudget/2.0))/(est1-est2);
		if(est1 - est2 < 0.001 || sampleEstimate < 0.05 * (double)totalSize)
			sampleEstimate = 0.05 * (double)totalSize;
		if(dimension > 2)
			sampleEstimate *= Math.sqrt((double)dimension-1.0);
		double percentage = ((double)sampleEstimate/(double)fullSize) * 100;
		System.out.println("RS_NN Sample Estimate: "+percentage);
		sampleEstimate = sampleEstimate * (double)actualEstimate;
		return((int)(sampleEstimate/1000.0));
	}
	class SRSPool{
		public LinkedList<Integer> InxList;
		private Random generator;
		public SRSPool(int totalSize){
			InxList = new LinkedList<Integer>();
			for(int i = 0;i < totalSize;i++){
				Integer integer = new Integer(i);
				InxList.addLast(integer);
			}
			generator = new Random(123456);
		}
		public int sampleWithoutReplacement(){
			int size = InxList.size();
			if(size == 0){
				System.out.println("Error in random sampling - No points in sample space");
				return -1;
			}
			int index = generator.nextInt(size);
			int inxval = -1;
			try{
				inxval = ((Integer)(InxList.get(index))).intValue();
				InxList.remove(index);
			}catch(IndexOutOfBoundsException e){
				System.out.println("Error in random sampling - index "+index+" out of bounds");
				return -1;
			}
			return inxval;
		}	
		public int getSize(){
			return InxList.size();
		}
	}
	public void generatePicassoDiagram() throws PicassoException
	{		
		initializeParams();		
		doOptimize();	
		lowPass();		
		doGenerateCostnCardDiagram();
		Vector puttrees = new Vector();
		puttrees.add(new Integer(plans.size()));
		puttrees.addAll(trees);
		trees = new Vector(puttrees);
		savePicassoDiagram();		
	}
}

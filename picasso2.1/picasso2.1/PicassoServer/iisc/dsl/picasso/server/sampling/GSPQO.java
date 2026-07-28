package iisc.dsl.picasso.server.sampling;

import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.DBConstants;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.TreeUtil;
import iisc.dsl.picasso.common.db.DBInfo;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.common.ds.DataValues;

import iisc.dsl.picasso.common.ds.TreeNode;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.server.PicassoException;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

public class GSPQO extends PicassoSampling{
	
	public ArrayList<PQORectangle> maxHeap;
	public int maxHeapSize;
	public ArrayList<Integer> compiledPts;	
	PQORectangle currRectangle;
	double stopThreshold = 0;
	boolean isInterpolate = false,isCorrection = false;
	int plansMissed = 0;
	int progressCount = 0;
	int agsPointsEvaluated=0;
	ArrayList<PQORectangle> backupHeap;
	Random generator ;

	public GSPQO(Socket s, ObjectInputStream in, ObjectOutputStream out, ClientPacket cp, Database db)
	{
		super(s, in, out, cp, db);	
		generator = new Random(999999);
		
	}
	
	public String[] treeNames=null;
	public int[] treecard=null;
	public String[] getTreeName(){
		return(treeNames);
	}
	public int[] getTreeCard(){
		return(treecard);
	}
    public void addToHeap(PQORectangle x)
    {
    	maxHeap.add(maxHeapSize,x);
    	maxHeapSize++;
    	for (int v=maxHeapSize; v>1; v/=2)
    	{
    		int w = v/2;    		   		
        	PQORectangle ap = (PQORectangle)maxHeap.get(w-1);
        	PQORectangle ax = (PQORectangle)maxHeap.get(v-1);
        	if(ax.mean>ap.mean)
        	{
        		exchange(v-1, w-1);
        	}
    	}
    	
    }
    public void addToHeapByArea(PQORectangle x)
    {
    	maxHeapSize = maxHeap.size();
    	maxHeap.add(maxHeapSize,x);
    	maxHeapSize++;
    	for (int v=maxHeapSize; v>1; v/=2)
    	{
    		int w = v/2;    		   		
        	PQORectangle ap = (PQORectangle)maxHeap.get(w-1);
        	PQORectangle ax = (PQORectangle)maxHeap.get(v-1);
        	if(ax.area>ap.area)
        	{
        		exchange(v-1, w-1);
        	}
    	}
    	
    }
    public void maxHeapify(int i)
    {
    	int largest = i;  
    	int base_i = i+1;
    	PQORectangle o = (PQORectangle)maxHeap.get(base_i-1);//root
    	PQORectangle l=null;
		try {
			l = (PQORectangle)maxHeap.get(2*base_i-1);
		} catch (RuntimeException e) {
			;
			
		}
    	PQORectangle r=null;
		try {
			r = (PQORectangle)maxHeap.get(2*base_i);
		} catch (RuntimeException e) {
			;
		}
		if(l != null && o!= null)
		{
	    	try {
				if(l.mean > o.mean)
					largest = 2*base_i-1;
				l = (PQORectangle)maxHeap.get(largest);//root
			} catch (RuntimeException e) {
				;
			}
		}
		if(r != null && o!= null)
		{
	    	try {
				if(r.mean > o.mean)
					largest = 2*base_i;
			} catch (RuntimeException e) {
				;
			}
		}
    	if(largest != i)
    	{
    		exchange(i,largest);
    		maxHeapify(largest);
    	}   	
    }
    public void maxHeapifyByArea(int i)
    {
    	int largest = i;  
    	int base_i = i+1;
    	PQORectangle o = (PQORectangle)maxHeap.get(base_i-1);//root
    	PQORectangle l=null;
		try {
			l = (PQORectangle)maxHeap.get(2*base_i-1);
		} catch (RuntimeException e) {
			;
			
		}
    	PQORectangle r=null;
    	try {
			r = (PQORectangle)maxHeap.get(2*base_i);
		} catch (RuntimeException e) {
			;
		}
    	int lWidth=0;
		int rWidth=0;
		int oWidth=0;
		try {
			lWidth = l.area;
		} catch (RuntimeException e1) {
			;
		}	
		try {
			rWidth = r.area;
		} catch (RuntimeException e1) {
			;
		}	
		try {
			oWidth = o.area;
		} catch (RuntimeException e1) {
			;
		}		
    	try {    		
			if(lWidth > oWidth)
				largest = 2*base_i-1;
			o = (PQORectangle)maxHeap.get(largest);//root
		} catch (RuntimeException e) {
			;
		}
    	try {
			if(rWidth > oWidth)
				largest = 2*base_i;
		} catch (RuntimeException e) {
			;
		}
    	if(largest != i)
    	{
    		exchange(i,largest);
    		maxHeapifyByArea(largest);
    	}   	
    }
    public PQORectangle extractMax()
    {
    	PQORectangle ap = (PQORectangle)maxHeap.get(0);
    	maxHeap.set(0, maxHeap.get(maxHeap.size()-1));
    	maxHeap.remove(maxHeap.size()-1);
    	maxHeapSize--;
    	if(maxHeapSize>0)
    		maxHeapify(0);
    	return(ap);
    }
    public PQORectangle extractMaxByArea()
    {
    	PQORectangle ap = (PQORectangle)maxHeap.get(0);
    	maxHeap.set(0, maxHeap.get(maxHeap.size()-1));
    	maxHeap.remove(maxHeap.size()-1);
    	maxHeapSize = maxHeap.size();
    	maxHeapSize--;
    	if(maxHeapSize>0)
    		maxHeapifyByArea(0);
    	return(ap);
    }
    public void buildheap()
    {
    	int start = (int)Math.floor((double)maxHeapSize/2.0);
        for (int v=start; v>0; v--)
            maxHeapify(v-1);
    }

    public void buildheapByArea()
    {
    	int start = (int)Math.floor((double)maxHeapSize/2.0);
        for (int v=start; v>0; v--)
            maxHeapifyByArea(v-1);
    }

    private void exchange(int i, int j)
    {
    	PQORectangle t= (PQORectangle)maxHeap.get(i);
    	maxHeap.set(i, maxHeap.get(j));//a[i]=a[j];
    	maxHeap.set(j, t);//a[j]=t;
    }
    
   
    int agsInterval[],agsResolution[],agsLeap[],origin;
    boolean dimFlags[];
    void initialGrid()
    {
    	dimVar = new int[dimension];
		index = new int[dimension];
		agsInterval = new int[dimension];
		agsLeap = new int[dimension];
    	activity = -1;
    	int count = PicassoConstants.SEGMENT_SIZE;
    	if(!isEstimationProcess)
    	{
	    	count = sendProgress(count,0,startTime,activity);    	
			for(int i = 0;i < dimension;i++){
				if(resolution[i] <= 30)				
					agsInterval[i] = 4;			
				else if(resolution[i] <= 100)
					agsInterval[i] = 8;
				else if(resolution[i] <= 300)
					agsInterval[i] = 16;
				else
					agsInterval[i] = 32;
				agsLeap[i] = agsInterval[i];
				agsResolution[i] = resolution[i];
			}
    	}
    	else
    	{
    		for(int i = 0;i < dimension;i++){				
				agsInterval[i] = resolution[i];				
				agsLeap[i] = agsInterval[i];
				agsResolution[i] = resolution[i];
			}	
    	}
		try {
			recursiveInitGrid(dimension);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
    }
    void recursiveInitGrid(int depth)
    {
    	try {
			ifInterruptedStop();
			ifPausedSleep();
		} catch (PicassoException e1) {
			;
		}
    	if(depth == 1)
    	{
    		for(index[depth-1]=0;index[depth-1]<resolution[depth-1];index[depth-1]+=agsInterval[depth-1])
    		{    			
    			try {
    				
					setPlanResult();
					boolean skip = false;
					for(int k=0;k<dimension;k++)
					{
						if(index[k]==resolution[k]-1)
						{
							skip = true;
							break;
						}
					}
					if(!skip)
						compiledPts.add(getIndex(index));
				} catch (PicassoException e) {
					
					e.printStackTrace();
				}	
    		}
    		index[depth-1] = resolution[depth-1] - 1;
    		try {
    			
				setPlanResult();		
				
			} catch (PicassoException e) {
				
				e.printStackTrace();
			}	
    	}
    	else
    	{
    		for(index[depth-1]=0;index[depth-1]<resolution[depth-1];index[depth-1]+=agsInterval[depth-1])
    		{
    			recursiveInitGrid(depth-1);    			
    		}    		
    		index[depth-1] = resolution[depth-1] - 1;
    		recursiveInitGrid(depth-1);
    	}
    }
    private void recursiveAGSLoop(int depth){
		int start = 0;
		int dim = dimension-depth;	
		if(dimFlags[dim])
			start = agsInterval[dim];		
		if(depth == 1){
			if(!isInterpolate){
				if(agsLeap[dim] == 0)
				{
					dimVar[dim] = 0;
					doAGSSample();
				}
				else
					for(dimVar[dim] = start;dimVar[dim] <= agsLeap[dim];dimVar[dim] += agsLeap[dim])
					{						
						doAGSSample();
					}
			}
			else
			{
				for(dimVar[dim] = start;dimVar[dim] < resolution[dim];dimVar[dim] += agsLeap[dim]){
					doAGSSample();					
				}
			}
		}else{
			if(!isInterpolate){
				if(agsLeap[dim] == 0)
				{
					dimVar[dim] = 0;
					recursiveAGSLoop(depth-1);
				}
				else
					for(dimVar[dim] = start;dimVar[dim] <= agsLeap[dim];dimVar[dim] += agsLeap[dim])
						recursiveAGSLoop(depth-1);	
			}
			else
			{
				for(dimVar[dim] = start;dimVar[dim] < resolution[dim];dimVar[dim] += agsLeap[dim])
					recursiveAGSLoop(depth-1);
			}
		}
	}
    int fpcErrorCount =0,fpcCount=0;int errPts = 0;
    private int doAGSSample(){
		int i, planPrev=0, planNext=0;
		int tmpInx=-1,inx=0,prevInx=-1;;
		boolean isRep=false, isDiff=false;
		double fpcPrev,fpcNext;
		int[] tmpIndex = new int[dimension];
		int[] strataId = getIndexBack(origin);
		activity = 0;
		for(i = 0;i < dimension;i++){
			if(!isInterpolate)
			{
				index[i] = strataId[i];
				if(agsResolution[i]>1)
					index[i] += dimVar[i];			
				if(index[i] >= resolution[i])
					index[i] = resolution[i] - 1;
			}
			else
			{
				index[i] = dimVar[i];
			}
		}
		
		inx = getIndex(index);
		
		boolean eqErr = false;
		//boolean fpcErr= false; int probErr = 0;
		if(data[inx] == null || (isCorrection && !data[inx].isRepresentative) || (isInterpolate && !data[inx].isRepresentative))//If the point is already colored by twoCubeFill - return			
		{
			agsPointsEvaluated++;
				
			try{
				ifInterruptedStop();
				ifPausedSleep();
				ifCompileStopped();		
				try {
					for(i = 0;i < dimension;i++){
						
						for(int k = 0;k < dimension;k++)
							tmpIndex[k] = index[k];
						if(dimFlags[i] == false)
							continue;
						tmpIndex[i] = index[i] - agsInterval[i];
						if(tmpIndex[i] < 0)
							tmpIndex[i] = 0;
						prevInx = getIndex(tmpIndex);
						try {
							planPrev = getPlanNumber(data,tmpIndex);
						} catch (RuntimeException e) {					
							
							planPrev = -1;
						}							
						tmpIndex[i] = index[i] + agsInterval[i];
						if(tmpIndex[i] >= resolution[i])
							tmpIndex[i] = resolution[i] - 1;							
						tmpInx = getIndex(tmpIndex);
						try {
							planNext = getPlanNumber(data,tmpIndex);
						} catch (RuntimeException e) {								
							planNext = -1;
						}
						if(planPrev != planNext){
							eqErr = true;
							isDiff = true;							
							if(!isInterpolate && currRectangle!=null){// && currRectangle.mean >= stopThreshold){
								setPlanResult();
								isRep = true;
							}
							else if(planNext != -1 && planPrev != -1)
							{								
								if(FPCMode){									
									if(devMode){
										if(actual_data[inx].getPlanNumber() == actual_data[prevInx].getPlanNumber())
										{
											data[inx]= setData(data[inx],data[prevInx]);											
										}
										else
										{
											data[inx]= setData(data[inx],data[tmpInx]);											
										}
									}
									else
									{
										fpcPrev = abstractPlanCost(query,index,planPrev);
										fpcNext = abstractPlanCost(query,index,planNext);
										
										if(fpcNext < fpcPrev){
											data[inx]= setData(data[inx],data[tmpInx]);
											data[inx].setCost(fpcNext);
											data[inx].FPCdone = true;
											
										}else if(fpcNext > fpcPrev){
											data[inx]= setData(data[inx],data[prevInx]);
											data[inx].setCost(fpcPrev);
											data[inx].FPCdone = true;
											
										}else{
											data[inx]= setData(data[inx],data[tmpInx]);
											data[inx].setCost(fpcNext);
											data[inx].FPCdone = true;
											
										}	
									}										
								}
								else
								{								
									/*Random g = new Random(generator.nextInt());
									int x = g.nextInt(10000);
									if(x < 5000)
*/										
									data[inx]= setData(data[inx],data[tmpInx]);
									/*else
										data[inx]= setData(data[inx],data[prevInx]);*/
								}
								
							}
							else if(planNext == -1)
							{
								data[inx]= setData(data[inx],data[prevInx]);
							}
							else 
							{								
								data[inx]= setData(data[inx],data[tmpInx]);								
							}
						}
						else
						{
							data[inx]= setData(data[inx],data[prevInx]);
							if(data[prevInx].succProb == 0.5 || data[tmpInx].succProb == 0.5)
								eqErr = true;
						}						
						if(isDiff)
							break;
						if(data[inx]==null)
							System.out.println(inx);
					}
				} catch (RuntimeException e) {					
					e.printStackTrace();
				}	
				progressCount++;				
				progressCount = sendProgress(progressCount,agsPointsEvaluated,startTime,activity);
			}catch(PicassoException e){
				System.out.println("Error in doAGS3DSample...");
			}
		}		
		
		if(!isInterpolate)
		{			
			index = getIndexBack(origin);			
			for(int j=0;j<dimension;j++)
			{
				index[j] += agsInterval[j];
			}
			int maxResolution = getIndex(index);
			isDiff = true;
			if(inx <= maxResolution)
			{
				for(i=0;i<compiledPts.size();i++)
					if(compiledPts.get(i) == inx)
						isDiff = false;
				if(isDiff)
					compiledPts.add(inx);
			}
		}
		else if(data[inx].isRepresentative == false )
		{
			if(eqErr)
			{
				errPts++;
				data[inx].succProb = (float)0.5;
			}
		}
		if(isRep)
			return 1;
		return 0;
	}
    public int agsChoose(int length,int ones){
		int i,diff = length - ones;
		int fact1 = 1,fact2 = 1;
		if(diff < 0 || ones < 0)
			return 1;
		for(i = length;i > diff;i--)
			fact1 *= i;
		for(i = ones;i > 0;i--)
			fact2 *= i;
		return(fact1/fact2);
	}
	public int[] agsPermute(int length,int ones){
		int options = agsChoose(length,ones);
		int[] res = new int[options];
		if(length == 1||ones == 0){
			res[0] = ones;
			return res;
		}
		if(length == ones){
			int val = 1;
			for(int i = 0;i < ones;i++)
				val *= 2;
			res[0] = val - 1;
			return res;
		}
		int i,l0,l1;
		int[] part0 = agsPermute(length-1,ones);
		l0 = part0.length;
		for(i = 0;i < l0;i++)
			res[i] = 2 * part0[i];
		int[] part1 = agsPermute(length-1,ones-1);
		l1 = part1.length;
		for(;i < l0+l1;i++)
			res[i] = 2 * part1[i-l0] + 1;
		return res;
	}
	public void setAGSFlags(int choice){
		for(int i = 1;i <= dimension;i++){
			if(choice % 2 == 1)
				dimFlags[dimension - i] = true;
			else
				dimFlags[dimension - i] = false;
			if(agsResolution[dimension-i] == 1)
				dimFlags[dimension - i] = false;
			choice /= 2;
		}		
	}
	void recursivefillCube(int depth)
	{
		if(depth==1)
		{
						
			for(dimVar[depth-1]=0;dimVar[depth-1]<agsResolution[depth-1];dimVar[depth-1]++)
			{
				index = getIndexBack(origin);
				for(int i=0;i<dimension;i++)
				{
					index[i]+=dimVar[i];					
				}
				int inx = getIndex(index);
				if(origin != inx && data[inx]==null)
				{					
					data[inx] = setData(data[inx],data[origin]);
					
				}
			}
		}
		else
		{
			for(dimVar[depth-1]=0;dimVar[depth-1]<agsResolution[depth-1];dimVar[depth-1]++)
			{
				recursivefillCube(depth-1);
			}
		}
	}	
	
	private void doOptimize()
	{    	
    	int phase,choice,permutations[];    	
		int skipDim = 0;
		int maxMeanRect=1;
		int count = 0,backupCounter = 0;;
		double maxMean = 0.0,cFactor = 0.0;
		
		activity = 0;
		dimVar = new int[dimension];
		index = new int[dimension];
		dimFlags = new boolean[dimension];
		agsResolution = new int[dimension];
		agsInterval = new int[dimension];
		agsLeap = new int[dimension];
		
		
		maxHeap = new ArrayList<PQORectangle>();
		backupHeap = new ArrayList<PQORectangle>();		
		compiledPts = new ArrayList<Integer>();
		initialGrid();
		
		for(int i=0;i<compiledPts.size();i++)
		{
			origin = compiledPts.get(i);
			currRectangle = new PQORectangle(origin, agsInterval);
			
			if(currRectangle.mean > 0)
			{	
				if(maxMean < currRectangle.mean)
				{
					maxMean = currRectangle.mean;
					maxMeanRect = currRectangle.planMap.size();
					cFactor = currRectangle.contribution;
				}
				maxHeap.add(count++,currRectangle);
				
			}	
			else
				backupHeap.add(backupCounter++,currRectangle);
			
		}
		
		
		int[] index = new int[dimension];
		int inx;
		//if(!isEstimationProcess)
			stopThreshold *= maxMean/cFactor;
		/*else
			stopThreshold = (errorPercent_I * 10) / 100;*/
		if(maxMean > 0)
			{
			try {
			
				maxHeapSize = maxHeap.size();
				buildheap();
				
				while(maxHeap.size() > 0)
				{			
					currRectangle = extractMax();
					if(currRectangle.mean == 0.0)
					{
						backupHeap.add(backupCounter++,currRectangle);
						continue;
					}
					if(currRectangle.mean < stopThreshold)// && currRectangle.planMap.size()<= maxMeanRect)
					{
						backupHeap.add(backupCounter++,currRectangle);
						continue;
					}
					else if(currRectangle == null)
						continue;	
					compiledPts = new ArrayList<Integer>();
					index = getIndexBack(currRectangle.StrataIdentifier);
									
					origin = currRectangle.StrataIdentifier;				
					compiledPts.add(origin);
					dimVar = new int[dimension];
					index = new int[dimension];
					dimFlags = new boolean[dimension];
					agsResolution = new int[dimension];
					agsInterval = new int[dimension];
					
					skipDim = 0;
					
					for(int i=0;i<dimension;i++)
					{
						
						agsResolution[i] = currRectangle.strataWidth[i];					
						if(agsResolution[i] <= 1)
						{
							skipDim++;					
						}
						if(agsResolution[i] == 1)
							agsInterval[i] = 0;
						else
						{
							agsInterval[i] = (int)Math.ceil((double)agsResolution[i] / 2.0);
							if(agsInterval[i]*2 != agsResolution[i])
							{
								int interval = 2;
								for(int k=1;interval<agsInterval[i];k++)
								{
									interval *= 2;
								}
								agsInterval[i] = interval;
							}
						}
						agsLeap[i] = agsInterval[i]*2;
						
					}
					if(skipDim == dimension)
					{					
						continue;
					}
					if(/*currRectangle.planMap.size()<=maxMeanRect && */(currRectangle.mean == 0.0 || currRectangle.mean < stopThreshold))
					{						
						
						if(currRectangle.mean > 0.0)
							backupHeap.add(backupCounter++,currRectangle);					
					}
					else
					{					
						for(phase = 1;phase <= dimension;phase++){
							permutations = agsPermute(dimension,phase);
							for(choice = 0;choice < permutations.length;choice++){
								setAGSFlags(permutations[choice]);						
								recursiveAGSLoop(dimension);
							}
						}			
						index = getIndexBack(origin);			
						for(int j=0;j<dimension;j++)
						{
							index[j] += agsInterval[j];
						}
						int maxResolution[] = index;
						for(int i=0;i<compiledPts.size();i++)
						{
							inx = compiledPts.get(i);							
							index = getIndexBack(inx);
							skipDim = 0;
							for(int k=0;k<dimension;k++)
							{
								if(index[k]>maxResolution[k])
									skipDim++;
							}
							if(skipDim > 0)
								continue;
							currRectangle = new PQORectangle(inx, agsInterval);		
							if(/*currRectangle.planMap.size()<=maxMeanRect && */(currRectangle.mean == 0.0 || currRectangle.mean < stopThreshold))
							{							
								if(currRectangle.mean > 0.0)
									backupHeap.add(backupCounter++,currRectangle);		
							}
							else
								addToHeap(currRectangle);				
						}
					}			
				}			
			} catch (Exception e) {			
				e.printStackTrace();
			}
		}
		
    }
	protected void doCorrection()
	{
		int phase,choice,permutations[];    	
		int skipDim = 0;	
		isCorrection = true;
		dimVar = new int[dimension];
		index = new int[dimension];
		dimFlags = new boolean[dimension];
		agsResolution = new int[dimension];
		agsInterval = new int[dimension];
		agsLeap = new int[dimension];		
		compiledPts = new ArrayList<Integer>();
		maxHeap = new ArrayList();
		for(int i=0;i<backupHeap.size();i++)
		{
			maxHeap.add(i,backupHeap.get(i));
		}	
		backupHeap = new ArrayList();
		int backupCounter = 0;
		buildheapByArea();
		
		while(maxHeap.size()>0 && sampleSize < reqSampleSize)
		{
			try {
				ifInterruptedStop();
				ifPausedSleep();
			} catch (PicassoException e) {
				;
			}
			currRectangle = extractMaxByArea();
			/* Testing if this is redundant */
			//maxHeap.remove(maxHeap.size()-1);
			currRectangle.calcAreaError();
			int totSize = (int)Math.floor((double)currRectangle.getTotalSize()*errorPercent_L/100.0);			
			if(totSize < currRectangle.locErrorCount)
			{				
				compiledPts = new ArrayList();
				index = getIndexBack(currRectangle.StrataIdentifier);								
				origin = currRectangle.StrataIdentifier;				
				compiledPts.add(origin);
				dimVar = new int[dimension];
				index = new int[dimension];
				dimFlags = new boolean[dimension];
				agsResolution = new int[dimension];
				agsInterval = new int[dimension];				
				skipDim = 0;				
				for(int i=0;i<dimension;i++)
				{					
					agsResolution[i] = currRectangle.strataWidth[i];					
					if(agsResolution[i] <= 1)
					{
						skipDim++;					
						agsInterval[i] = 0;
					}
					else
					{
						agsInterval[i] = (int)Math.ceil((double)agsResolution[i] / 2.0);
						if(agsInterval[i]*2 != agsResolution[i])
						{
							int interval = 2;
							for(int k=1;interval<agsInterval[i];k++)
							{
								interval *= 2;
							}
							agsInterval[i] = interval;
						}
					}
					agsLeap[i] = agsInterval[i]*2;
					
				}
				if(skipDim == dimension)
				{					
					continue;
				}								
				for(phase = 1;phase <= dimension;phase++){
					permutations = agsPermute(dimension,phase);
					for(choice = 0;choice < permutations.length;choice++){
						setAGSFlags(permutations[choice]);						
						recursiveAGSLoop(dimension);
					}
				}			
				index = getIndexBack(origin);			
				for(int j=0;j<dimension;j++)
				{
					index[j] += agsInterval[j];
				}
				int maxResolution[] = index,inx;
				for(int i=0;i<compiledPts.size();i++)
				{
					inx = compiledPts.get(i);							
					index = getIndexBack(inx);
					skipDim = 0;
					for(int k=0;k<dimension;k++)
					{
						if(index[k]>maxResolution[k])
							skipDim++;
					}
					if(skipDim > 0)
						continue;
					currRectangle = new PQORectangle(inx, agsInterval);	
					if(currRectangle.mean > 0)
					{						
						addToHeapByArea(currRectangle);						
					}									
				}							
			}			
		}
		while(maxHeap.size()>0)
		{
			currRectangle = extractMaxByArea();
			backupHeap.add(backupCounter++,currRectangle);
		}		
	}
    protected void doInterpolate()
    {
    	int phase,choice,permutations[];    	
		activity = 1;
    	isInterpolate = true;
    	isCorrection = false;
		origin = 0;
		currRectangle = null;
		errPts = 0;
		for(int i=0;i<totalSize;i++)
		{
			if(data[i]!=null && data[i].isRepresentative)
				data[i].succProb = 1;
		}	
		if(!isEstimationProcess)
		{
			for(int i = 0;i < dimension;i++){			
				if(resolution[i] <= 30)				
					agsInterval[i] = 4;			
				else if(resolution[i] <= 100)
					agsInterval[i] = 8;
				else if(resolution[i] <= 300)
					agsInterval[i] = 16;
				else
					agsInterval[i] = 32;
				agsLeap[i] = agsInterval[i];
				agsResolution[i] = resolution[i];
				dimFlags[i] = false;
			}
		}
		else
    	{
    		for(int i = 0;i < dimension;i++){				
				agsInterval[i] = resolution[i];				
				agsLeap[i] = agsInterval[i];
				agsResolution[i] = resolution[i];
			}	
    	}
		int skipDim;
		while(isInterpolate)
		{
			
			try {
				ifInterruptedStop();
				ifPausedSleep();
			} catch (PicassoException e) {
				;
			}
			for(phase = 1;phase <= dimension;phase++){
				permutations = agsPermute(dimension,phase);
				for(choice = 0;choice < permutations.length;choice++){
					setAGSFlags(permutations[choice]);
					recursiveAGSLoop(dimension);
				}
			}
			skipDim = 0;
			for(int i=0;i<dimension;i++){
				agsInterval[i] /= 2;
				if(agsInterval[i] <= 0)
				{
					skipDim++;
					agsInterval[i] = 1;
				}
				agsLeap[i] = agsInterval[i] * 2;
				
			}			
			
			if(skipDim == dimension)
			{	
				isInterpolate = false;
				break;
			}
		}
    }
    
    
	class PQORectangle {	
		public int StrataIdentifier;
		public double mean = -1;		
		int[] strataWidth;
		public int area;
		LinkedList<Plan> planList;
		Hashtable<Integer,Integer> planMap;		
		LinkedList<Integer> planPts;
		Vector trees;
		public int locErrorCount;
		public int depNodes[];
		public double cFactor[];
		public double contribution;
		public PQORectangle(int sID,int[] interval) {
			StrataIdentifier = sID;
			strataWidth = new int[dimension];			
			int[] index=new int[dimension];
			
			contribution = 0;
			index = getIndexBack(StrataIdentifier);
			area = 1;
			for(int i=0;i<dimension;i++)
			{				
				strataWidth[i] = interval[i];
				if(resolution[i]-index[i]-1<strataWidth[i])
				{
					strataWidth[i] = resolution[i]-index[i]-1;
				}
				area *= strataWidth[i];
			}
			planList = new LinkedList<Plan>();
			planMap = new Hashtable<Integer,Integer>();
			treeNames = getTreeName();
			treecard = getTreeCard();
			addPlans();
			cntPlans();
			depNodes = new int[planList.size()];
			cFactor = new double[planList.size()];
			for(int i=0;i<planList.size();i++)
			{
				cFactor[i] = 9999;
			}			
			
			getMeanTreeDiff();			
			for(int i=planList.size()-1;i>=0;i--)			
				planList.remove(i);			
			treeNames = null;
			treecard = null;
			cFactor = null;
			depNodes = null;
			//trees.removeAllElements();
		}		
		private void cntPlans()
		{	
			int pCnt = 0,pNo=0;
			for(int i = 0;i<planList.size();i++)
			{			
				try{
				   Plan p = (Plan)planList.get(i);
				   pNo = p.getPlanNo();
				   pCnt = Integer.parseInt(planMap.get(pNo).toString()) + 1;
				   planMap.put(pNo, pCnt);
				}
				catch(Exception e)
				{
				   planMap.put(pNo, 1);
				}				
			}
		}
		public void calcAreaError()
		{
			locErrorCount = 0;
			int totSize = getTotalSize(),inx;			
			for(int i=0;i<totSize;i++)
			{
				inx = i+StrataIdentifier;
				if(data[inx].succProb == 0.5)
					locErrorCount++;
			}
			
		}
		public int getTotalSize()
		{
			int totSize = 1;
			for(int i=0;i<dimension;i++)
			{
				totSize *= (strataWidth[i]+1);
			}
			return(totSize);
		}
		private void RemDuplicates()
		{
			for(int i = 0;i<planMap.size();i++)
			{
				for(int j = i+1;j<planMap.size();j++)
				{
					if(planMap.get(i) == planMap.get(j))
					{
						planMap.remove(j);
						planList.remove(j);
						j--;
					}
				}
			}
		}
		private void addPlans()
		{						
			int inx = 0; int[] index = new int[dimension];
			planPts = new LinkedList<Integer>();
			dimVar = new int[dimension];
			recursiveAddPlans(dimension);			
			for(int i=0;i<planPts.size();i++)
			{
				try {
					inx = Integer.parseInt(planPts.get(i).toString());
					dimVar = getIndexBack(inx);
					index = getIndexBack(StrataIdentifier);
					boolean skip = false;
					for(int j=0;j<dimension;j++)
					{
						index[j] += dimVar[j];
						if(index[j] >= resolution[j])
							skip = true;
					}
					if(skip)
						continue;
					inx = getIndex(index);			
					int planNum=-1;
					try {
						planNum = data[inx].getPlanNumber();
						planList.addLast(plans.get(planNum));						
					} catch (RuntimeException e) {					
						System.out.println("error -> : "+inx);
					}
					
				} catch (Exception e) {
					//system.out.println("Error with :"+inx);
				}
			}
			//RemDuplicates();
		}		
		private void recursiveAddPlans(int depth)
		{			
			if(depth>1)
			{
				dimVar[depth-1]=0;
				recursiveAddPlans(depth-1);
				if(strataWidth[depth-1]>0)
				{
					dimVar[depth-1]=strataWidth[depth-1];
					recursiveAddPlans(depth-1);
				}
			}
			else
			{
				dimVar[depth-1]=0;				
				planPts.addLast(getIndex(dimVar));
				if(strataWidth[depth-1]>0)
				{
					dimVar[depth-1]=strataWidth[depth-1];
					planPts.addLast(getIndex(dimVar));
				}
			}
		}
		Vector planTrees;
		public void calDependentNodes()
		{
			TreeNode root;			
			Plan plan;	
			int planCnt = planList.size();
			if (planCnt == 1) {				
				return;
			}
			planTrees = new Vector();
			for (int i = 0; i < planCnt; i++) {
				plan = (Plan) planList.get(i);
				root = plan.createPlanTree();
				depNodes[i] = recursiveSetDependency(root,0);
				planTrees.add(i,root);
			}
		}
		public int recursiveSetDependency(TreeNode root,int count)
		{
			Vector ch = root.getChildren();
			if(ch.size() > 0)
			{
				for (int j = 0; j < ch.size(); j++) {
					count = recursiveSetDependency((TreeNode)ch.get(j),count);	
				}
				if(root.getDependency() == false)
				{
					for (int j = 0; j < ch.size(); j++) {
						TreeNode tmpNode = (TreeNode)ch.get(j);
						if(tmpNode.getDependency() == true)
						{						
							count++;
							root.setDependency(true);
							break;
						}
					}
				}
			}
			else
			{
				for(int i=0;i<dimension;i++){
					String tmpStr = root.getNodeName();
					if(tmpStr.indexOf(relNames[i]) >= 0)
					{
						if(root.getDependency() == false)
						{
							count++;
							root.setDependency(true);
							break;
						}
					}
				}
			}
			return count;			
		}
		int depDiff;
		public double getMeanTreeDiff() {		
			int planCnt = planList.size();
			if (planCnt == 1) {
				mean = 0;
				return (0);
			}
			double mean_i = 0.0, tmp = 0.0;
			TreeNode tree_i, tree_j;
			int size_i, size_j;
			Plan plan_i,plan_j;
			int[][] diffNodes = new int[planCnt][planCnt];
			int[] sizeNodes = new int[planCnt];
			Hashtable planDone = new Hashtable();
			for (int i = 0; i < planList.size(); i++) {
				Plan p = (Plan)planList.get(i);
				int pNo = p.getPlanNo();
				planDone.put(pNo, false);
			}			
			int count = 0;double max = 0;
			contribution = 9999.0;
			try {
				for (int i = 0; i < planCnt; i++) {
					try {	
						plan_i = (Plan) planList.get(i);					
						size_i = plan_i.getSize();
						trees = new Vector();
						trees.addElement(new Integer(2));					
						tree_i = plan_i.createPlanTree();
						trees.addElement(plan_i.getPlanNo());
						trees.addElement(tree_i);
						sizeNodes[i] = size_i;
						
						for (int j = i + 1; j < planCnt; j++) {			
							
							plan_j = (Plan) planList.get(j);					
							size_j = plan_j.getSize();						
							tree_j = plan_j.createPlanTree();
							trees.addElement(plan_j.getPlanNo());
							trees.addElement(tree_j);
							diff = 0.0;
							depDiff = 0;
							try {
								treeDiff(2, trees, TreeUtil.SUB_OPERATOR_LEVEL);
							} catch (RuntimeException e) {
								e.printStackTrace();
							}
							tmp = size_i + size_j;							
							diffNodes[i][j] = (int)Math.round(diff);
							diffNodes[j][i] = (int)Math.round(diff);
							sizeNodes[j] = size_j;							
							diff *= 2;
							diff /= tmp;
							diff = 1 - diff;						
							if(max < diff)
								max = diff;
							mean_i += diff;
							count++;							
							trees.remove(trees.size()-1);
							trees.remove(trees.size()-1);
							
						}
						
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
				
				mean_i = mean_i / count;
			} catch (Exception e) {
				e.printStackTrace();
			}
			mean = mean_i;
			double tempVal = 0;
			cFactor = new double[planCnt];
			for(int i=0;i<planCnt;i++)							
			{
				cFactor[i] = -1;
			}
			for(int i=0;i<planCnt;i++)							
			{					
				cFactor[i] = 1;
				plan_i = (Plan) planList.get(i);
				for (int k = 0; k < planList.size(); k++) {
					Plan p = (Plan)planList.get(k);
					int pNo = p.getPlanNo();
					planDone.put(pNo, false);
				}
				planDone.put(plan_i.getPlanNo(), true);				
				for(int j=0;j<planCnt;j++)							
				{	
					plan_j = (Plan) planList.get(j);
					if(planDone.get(plan_j.getPlanNo()).equals(true))
					{
						continue;
					}					
					tempVal = (double)(sizeNodes[j] - diffNodes[i][j])/(double)sizeNodes[j];
					
					tempVal = 2*tempVal + 0.5;					
					if(tempVal > 1)
						  tempVal = 1;					
					cFactor[i] += tempVal;					
					planDone.put(plan_j.getPlanNo(), true);
				} 
			}			
			try {
				for(int i=0;i<planMap.size();i++)							
				{				
					contribution = Math.min(contribution,cFactor[i]);			
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			return (mean);
		}
	
		/* The below functions are for Tree-Difference operations */
		
		private double diff = 0;
	
		public String[] treeNames;
	
		public int[] treecard;
	
		private final int SUBOP_OFFSET = PicassoConstants.SO_BASE;
	
		private void treeDiff(int numOfPlans, Vector trees, int diffType) {
	
			if (numOfPlans <= 1)
				return;
			matchNum = 1;
			// The first element is the plan number...
			TreeNode root1 = (TreeNode) trees.elementAt(2);
			TreeNode root2 = (TreeNode) trees.elementAt(4);
	
			Hashtable matching = getBestMatching(root1, root2, diffType);
			Object[] keys = matching.keySet().toArray();
			setSimilarity(root1, root2, matching);
			setFetchNodes(root1);
			setFetchNodes(root2);
			if (checkIfTreesSame(root1) == true && checkIfTreesSame(root2) == true) {
			}
			diff = 0;
			getMatchingNodes(root1);
		}
	
		void getMatchingNodes(TreeNode root) {
			int sim = root.getSimilarity();
			switch (sim) {
			
				//Identical(white)
				case PicassoConstants.T_IS_SIMILAR :	
				//case PicassoConstants.T_SUB_OP_DIF :
					diff+=1;
					if(root.getDependency() == true)
						depDiff++;
				break;
				//case PicassoConstants.T_LEFT_EQ_RIGHT :
		
				/*//Sub-operator swappped
				case PicassoConstants.T_SO_LEFT_EQ_RIGHT : diff+=1; break;
		
				//sub-opertor changes(green border + red diff edge)
				case PicassoConstants.T_SO_LEFT_SIMILAR :
				case PicassoConstants.T_SO_RIGHT_SIMILAR :
				case PicassoConstants.T_SO_NO_CHILD_SIMILAR : diff+=1; break;
		
				//child realtions are not similar
				//atleast one child is same (orange border - red diff edge)
				case PicassoConstants.T_LEFT_SIMILAR :
				case PicassoConstants.T_RIGHT_SIMILAR : diff+=1; break;
		
				//child swapped
				case PicassoConstants.T_LEFT_EQ :
				case PicassoConstants.T_RIGHT_EQ : diff+=1; break;
		
				//none same
				case PicassoConstants.T_NO_CHILD_SIMILAR : diff+=1;break;*/
		
				default:
					break;
			}
			Vector children = root.getChildren();
			for (int i = 0; i < children.size(); i++) {
				getMatchingNodes((TreeNode) (children.elementAt(i)));
			}
		}
	
		int getSingletonTreeLength(TreeNode n1, int len) {
			TreeNode p1 = n1.getParent();
	
			if (p1 != null
					&& (p1.getNodeName().equals("FETCH")
							|| p1.getNodeName().equals("Seq Scan") || p1
							.getNodeName().equals("TABLE ACCESS"))) // If it is a
																	// fetch treat
																	// it as a
																	// singleton..
				/* if(p1 !=null && p1.getNodeCard(treeNames, treecard)==1) */
				return (getSingletonTreeLength(p1, len + 1));
	
			if (p1 == null || TreeUtil.getRightTree(p1) != null) {
				return len;
			} else
				return (getSingletonTreeLength(p1, len + 1));
		}
	
		int getEditDistance(TreeNode n1, TreeNode n2) {
			int len1 = getSingletonTreeLength(n1, 0);
			int len2 = getSingletonTreeLength(n2, 0);
	
			// //system.out.println(n1.getNodeName() + " Len1 :: " + len1 + " " +
			// n2.getNodeName() + " LEN2 :: " + len2);
			if (len1 == 0)
				return len2;
			else if (len2 == 0)
				return len1;
	
			int[][] editDistances = new int[len1 + 1][len2 + 1];
			TreeNode t1 = n1;
			TreeNode t2 = n2;
			editDistances[0][0] = 0;
			TreeNode[] nodes1 = new TreeNode[len1 + 1];
			TreeNode[] nodes2 = new TreeNode[len2 + 1];
			for (int i = 0; i <= len1; i++) {
				editDistances[i][0] = i * 2;
				// //system.out.println("T1 :: " + t1.getNodeName());
				nodes1[i] = t1;
				t1 = t1.getParent();
			}
			for (int j = 0; j <= len2; j++) {
				editDistances[0][j] = j * 2;
				// //system.out.println("T2 :: " + t2.getNodeName());
				nodes2[j] = t2;
				t2 = t2.getParent();
			}
			t1 = n1;
			t2 = n2;
			for (int i = 1; i <= len1; i++) {
				t1 = t1.getParent();
				t2 = n2;
				for (int j = 1; j <= len2; j++) {
					t2 = t2.getParent();
					int val;
					if (TreeUtil.isEquals(t1, t2, TreeUtil.SUB_OPERATOR_LEVEL))
						val = 0;
					else
						val = 1;
					editDistances[i][j] = getMin(editDistances[i - 1][j] + 2,
							editDistances[i][j - 1] + 2,
							editDistances[i - 1][j - 1] + val);
					// //system.out.println("i " + i + " j " + j + " Edit :: " +
					// editDistances[i][j]);
				}
			}
			return editDistances[len1][len2];
		}
	
		// Get the edit distance for non matching nodes with only one child
		// First we need to get the two singleton matching nodes to match
		void setEditNodes(TreeNode n1, TreeNode n2, Hashtable matching) {
			int len1 = getSingletonTreeLength(n1, 0);
			int len2 = getSingletonTreeLength(n2, 0);
	
			// //system.out.println(n1.getNodeName() + " Len1 :: " + len1 + " " +
			// n2.getNodeName() + " LEN2 :: " + len2);
			if (len1 == 0 || len2 == 0) {
				// //system.out.println(n1.getNodeName() + " Len1 :: " + len1 + " " +
				// n2.getNodeName() + " LEN2 :: " + len2);
				TreeNode t1 = n1;
				for (int i = 0; i < len1; i++) {
					t1 = t1.getParent();
					t1.setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					// //system.out.println(t1.getSimilarity() + " T1 :: " +
					// t1.getNodeName());
				}
	
				TreeNode t2 = n2;
				for (int i = 0; i < len2; i++) {
					t2 = t2.getParent();
					t2.setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					// //system.out.println(t2.getSimilarity() + " T2 :: " +
					// t2.getNodeName());
				}
	
				return;
			}
	
			int[][] editDistances = new int[len1 + 1][len2 + 1];
			TreeNode t1 = n1;
			TreeNode t2 = n2;
			editDistances[0][0] = 0;
			TreeNode[] nodes1 = new TreeNode[len1 + 1];
			TreeNode[] nodes2 = new TreeNode[len2 + 1];
			for (int i = 0; i <= len1; i++) {
				editDistances[i][0] = i * 2;
				// //system.out.println("T1 :: " + t1.getNodeName());
				nodes1[i] = t1;
				t1 = t1.getParent();
			}
			for (int j = 0; j <= len2; j++) {
				editDistances[0][j] = j * 2;
				// //system.out.println("T2 :: " + t2.getNodeName());
				nodes2[j] = t2;
				t2 = t2.getParent();
			}
			t1 = n1;
			t2 = n2;
			for (int i = 1; i <= len1; i++) {
				t1 = t1.getParent();
				t2 = n2;
				for (int j = 1; j <= len2; j++) {
					t2 = t2.getParent();
					int val;
					if (TreeUtil.isEquals(t1, t2, TreeUtil.SUB_OPERATOR_LEVEL))
						val = 0;
					else
						val = 1;
					editDistances[i][j] = getMin(editDistances[i - 1][j] + 2,
							editDistances[i][j - 1] + 2,
							editDistances[i - 1][j - 1] + val);
					// //system.out.println("i " + i + " j " + j + " Edit :: " +
					// editDistances[i][j]);
				}
			}
	
			int m = len1;
			int n = len2;
			int curEdit;
			/*
			 * if ( editDistances[m][n] != 0 ) printEditArray(editDistances);
			 */
			while (m != -1 || n != -1) {
				curEdit = editDistances[m][n];
				if (curEdit == 0) { // Set everything above to white because they
									// are the same...
					// //system.out.println(m + " Set to Similar from here " + n);
					for (int i = 1; i <= m; i++) {
						nodes1[i].setSimilarity(PicassoConstants.T_IS_SIMILAR);
						nodes1[i].setMatchNumber(matchNum++);
						// //system.out.println("Node1 :: " + nodes1[i]);
					}
					matchNum -= (m);
					for (int j = 1; j <= n; j++) {
						nodes2[j].setSimilarity(PicassoConstants.T_IS_SIMILAR);
						nodes2[j].setMatchNumber(matchNum++);
						// //system.out.println("Node2 :: " + nodes2[j]);
					}
					break;
				}
				// Check where we got the edit distance from..
				// If it is from m-1, n-1 and the change is 0, set color to white,
				// else it is substitution so set it to red
				// If it is from m-1, n or m, n-1 set it to brown because it is
				// Insert/Delete..
				int val1 = -1, val2 = -1, val3 = -1;
				if (m != 0 && n != 0)
					val1 = editDistances[m - 1][n - 1];
				if (n != 0)
					val2 = editDistances[m][n - 1];
				if (m != 0)
					val3 = editDistances[m - 1][n];
	
				int num = getMinNum(val1, val2, val3, editDistances[m][n]);
				// //system.out.println(m + "," + n + " Edit Number :: " + num);
				switch (num) {
				case 0:
					// Could be a sub operator change..
					if (!TreeUtil.isEquals(nodes1[m], nodes2[n],
							TreeUtil.SUB_OPERATOR_LEVEL)) {
						nodes1[m].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
						nodes2[n].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
						nodes1[m].setMatchNumber(matchNum);
						nodes2[n].setMatchNumber(matchNum);
						matchNum++;
					} else {
						// No Change
						nodes1[m].setSimilarity(PicassoConstants.T_IS_SIMILAR);
						nodes2[n].setSimilarity(PicassoConstants.T_IS_SIMILAR);
						nodes1[m].setMatchNumber(matchNum);
						nodes2[n].setMatchNumber(matchNum);
						matchNum++;
					}
					// setFetchNodes(nodes1[m]);
					// setFetchNodes(nodes2[n]);
					m = m - 1;
					n = n - 1;
					break;
	
				case 1:
					// Labels are changed...
					// Not required, we only need to check the node itself and since
					// it is
					// different it has to be a sub-op difference.
					// int simType = getSimilarityType(nodes1[m], nodes2[n]);
	
					if (nodes1[m].getNodeName().equals(nodes2[n].getNodeName())) { // Sub
																					// op
																					// has
																					// to
																					// be
																					// diff
						nodes1[m].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
						nodes2[n].setSimilarity(PicassoConstants.T_SUB_OP_DIF);
						nodes1[m].setMatchNumber(matchNum);
						nodes2[n].setMatchNumber(matchNum);
						matchNum++;
					} else {
						nodes1[m].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
						nodes2[n].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					}
					// //system.out.println(nodes1[m].getNodeName() + " Node Change ::
					// " + nodes2[n].getNodeName());
					// setFetchNodes(nodes1[m]);
					// setFetchNodes(nodes2[n]);
					m = m - 1;
					n = n - 1;
					break;
	
				case 2:
					nodes2[n].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					// //system.out.println(n + " Node Insert :: " +
					// nodes2[n].getNodeName());
					n = n - 1;
					break;
	
				case 3:
					nodes1[m].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
					// //system.out.println(m + " Node Delete :: " +
					// nodes1[m].getNodeName());
					m = m - 1;
					break;
				}
	
			}
			
			return;
		}	
		
	
		void setFetchNodes(TreeNode node) {
			// Go through the entire and check for fetch nodes and set them..
			if (node == null)
				return;
	
			
			Vector children = node.getChildren();	
			if (children != null && children.size() != 0) {
				
				for (int i = 0; i < children.size(); i++) {
					
					TreeNode childNode = (TreeNode) children.elementAt(i);
					if (childNode == null) {
						
						continue;
					}
					setFetchNodes(childNode);
				}
			}
		}
	
		boolean checkIfTreesSame(TreeNode root1) {
			if (root1.getSimilarity() == PicassoConstants.T_IS_SIMILAR) {
				Vector ch = root1.getChildren();
				for (int i = 0; i < ch.size(); i++) {
					if (checkIfTreesSame((TreeNode) ch.elementAt(i)) == false)
						return false;
				}
				return true;
			} else
				return false;
		}
	
		int getMin(int val1, int val2, int val3) {
			int min = val1;
			if (min > val2) {
				min = val2;
			}
			if (min > val3) {
				min = val3;
			}
			return min;
		}
	
		int getMinNum(int val1, int val2, int val3, int orig) {
			int retVal = -1;
	
			if (orig == val1)
				retVal = 0;
			if (orig == val1 + 1)
				retVal = 1;
			if (orig == (val2 + 2)) {
				if (retVal != -1) { // There is a clash, get the minimum value index
					if (val2 < val1)
						retVal = 2;
				} else
					retVal = 2;
			}
			if (orig == (val3 + 2)) {
				if (retVal != -1) { // There is a clash, get the minimum value index
					if (val3 < val1)
						retVal = 3;
				} else
					retVal = 3;
			}
			return retVal;
	
			
		}
	
		int getSimilarityType(TreeNode n1, TreeNode n2, boolean relations) {
			TreeNode l1 = TreeUtil.getLeftTree(n1);
			TreeNode l2 = TreeUtil.getLeftTree(n2);
			TreeNode r1 = TreeUtil.getRightTree(n1);
			TreeNode r2 = TreeUtil.getRightTree(n2);
	
			boolean ll, rr, lr, rl;
			if (relations == true) {
				ll = getRelationMatch(l1, l2);
				rr = getRelationMatch(r1, r2);
				lr = getRelationMatch(l1, r2);
				rl = getRelationMatch(r1, l2);
			} else {
				ll = TreeUtil.areTreesEqual(l1, l2);
				rr = TreeUtil.areTreesEqual(r1, r2);
				lr = TreeUtil.areTreesEqual(l1, r2);
				rl = TreeUtil.areTreesEqual(r1, l2);
			}
			
	
			// Need to set up truth table here to make the code less confusing..
	
			int subop = 0;
			if (TreeUtil.isEquals(n1, n2, TreeUtil.OPERATOR_LEVEL)) {
				if (!TreeUtil.isEquals(n1, n2, TreeUtil.SUB_OPERATOR_LEVEL))
					subop = SUBOP_OFFSET; // Sub operators are not the same...
				if (ll == false) { // l1, l2 == false
					if (lr == true) { // ll is false, but lr is true
						if (rl == true) // lr = true and rl = true
							return (PicassoConstants.T_LEFT_EQ_RIGHT + subop);
						else
							// lr = true && rl = false
							return (PicassoConstants.T_LR_SIMILAR + subop);
					} else if (rl == true) // lr is false, rl is true
						return (PicassoConstants.T_RL_SIMILAR + subop);
					if (rr == false) // l1, l2 == false, r1, r2 == false
						return (PicassoConstants.T_NO_CHILD_SIMILAR + subop);
					else
						// ll is false, rr is true
						return (PicassoConstants.T_RIGHT_SIMILAR + subop);
				} else { // l1, l2 == true
					if (rr == false)
						return (PicassoConstants.T_LEFT_SIMILAR + subop); // ll is
																			// true,
																			// rr is
																			// false
					else if (TreeUtil.isEquals(n1, n2, TreeUtil.SUB_OPERATOR_LEVEL) == false) // Only
																								// sub-operator
																								// is
																								// diffrent
						return (PicassoConstants.T_SUB_OP_DIF);
					else
						return (PicassoConstants.T_IS_SIMILAR); // Should not come
																// here...
				}
			} else {
				if (ll == false) {
					if (lr == true) { // l1, l2 == false, l1, r2 == true
						if (rl == true) // Left and right are swapped
							return (PicassoConstants.T_NP_LEFT_EQ_RIGHT);
						else
							return (PicassoConstants.T_NP_LR_SIMILAR); // l1, r2 ==
																		// true but
																		// not r1,
																		// l2
					} else if (rl == true) // l1, r2 == false
						return (PicassoConstants.T_NP_RL_SIMILAR);
					if (rr == false)
						return (PicassoConstants.T_NP_NOT_SIMILAR);
					else
						return (PicassoConstants.T_NP_RIGHT_SIMILAR);
				} else { // l1, l2 == true
					if (rr == false)
						return (PicassoConstants.T_NP_LEFT_SIMILAR);
					else
						return (PicassoConstants.T_NP_SIMILAR);
				}
			}
		}
	
		void getNonMatchingNodes(Vector entries, TreeNode root) {
			if (root.getSimilarity() != PicassoConstants.T_IS_SIMILAR) {
				entries.add(""+root);
			}
			Vector children = root.getChildren();
			for (int i = 0; i < children.size(); i++) {
				getNonMatchingNodes(entries, (TreeNode) (children.elementAt(i)));
			}
		}
	
		void setSimilarity(TreeNode root1, TreeNode root2, Hashtable matching) {
			// show/mark the matching nodes with the help of similarity-box
	
			Object[] keys = matching.keySet().toArray();
			
			for (int i = 0; i < keys.length; i++) {
	
				TreeNode p1 = (TreeNode) keys[i];
				TreeNode p2 = (TreeNode) matching.get(keys[i]);
				p1.setSimilarity(PicassoConstants.T_IS_SIMILAR);
				p2.setSimilarity(PicassoConstants.T_IS_SIMILAR);
				
			}
		}
	
		void setMatchNumber(TreeNode node1, TreeNode node2) {
			node1.setMatchNumber(matchNum);
			node2.setMatchNumber(matchNum);
			matchNum++;
		}
	
		/** calculate the best matching between two trees * */
		Hashtable getBestMatching(TreeNode tree1, TreeNode tree2, int diffType) {
			// initialise the matching table
			Hashtable matching = new Hashtable();
			// find the matching between leaves, here leaves are tables and indexes
			TreeNode[] leaves1 = TreeUtil.getLeaves(tree1);
			TreeNode[] leaves2 = TreeUtil.getLeaves(tree2);
	
			boolean[] done = new boolean[leaves2.length];
			for (int j = 0; j < leaves2.length; j++)
				done[j] = false;
	
			boolean[] idone = new boolean[leaves1.length];
			for (int i = 0; i < leaves1.length; i++)
				idone[i] = false;
	
			// Check for duplicates in this and pick the one with the mimimum edit
			// distance
			doDuplicateLeafMatch(matching, leaves1, leaves2, idone, done);
	
			for (int i = 0; i < leaves1.length; i++) {
				if (idone[i] == true)
					continue;
				for (int j = 0; j < leaves2.length; j++) {
					if (done[j] == true)
						continue;
					if (TreeUtil.isEquals(leaves1[i], leaves2[j], diffType)) {
						
						matching.put(leaves1[i], leaves2[j]);
						setMatchNumber(leaves1[i], leaves2[j]);
						done[j] = true;
						break;
					}
				}
			}
	
			// find the matching between join-nodes. Join nodes are matched if join
			// method and tables getting joined are same.
			TreeNode[] joins1 = TreeUtil.getJoinNodes(tree1, treeNames, treecard);
			TreeNode[] joins2 = TreeUtil.getJoinNodes(tree2, treeNames, treecard);
	
			done = new boolean[joins2.length];
			for (int j = 0; j < joins2.length; j++) {
				done[j] = false;
				
			}
	
			idone = new boolean[joins1.length];
			for (int i = 0; i < joins1.length; i++) {
				idone[i] = false;
				
			}
			for (int i = 0; i < joins1.length; i++)
				for (int j = 0; j < joins2.length; j++) {
					if (done[j] == true)
						continue;
	
					if (TreeUtil.isEquals(joins1[i], joins2[j], diffType) == false)
						continue;
	
					TreeNode t1 = TreeUtil.getSubTree(tree1, joins1[i]);
					TreeNode t2 = TreeUtil.getSubTree(tree2, joins2[j]);
	
					boolean isMatching = true;
	
					// check for matching in left relation
					TreeNode left1 = TreeUtil.getLeftTree(t1);
					TreeNode left2 = TreeUtil.getLeftTree(t2);
					String[] rel1 = TreeUtil.getRelations(left1);
					String[] rel2 = TreeUtil.getRelations(left2);
	
					if (rel1.length != rel2.length)
						isMatching = false;
	
					
					for (int ii = 0; isMatching && ii < rel1.length; ii++) {
						int jj;
						for (jj = 0; jj < rel2.length; jj++)
							if (rel1[ii].equals(rel2[jj]))
								break;
	
						if (jj == rel2.length)
							isMatching = false;
					}
	
					if (isMatching == false)
						// left relations are not matching
						continue;
	
					// check for matching in right relations
					TreeNode right1 = TreeUtil.getRightTree(t1);
					TreeNode right2 = TreeUtil.getRightTree(t2);
					rel1 = TreeUtil.getRelations(right1);
					rel2 = TreeUtil.getRelations(right2);
	
					if (rel1.length != rel2.length)
						isMatching = false;
	
					for (int ii = 0; isMatching && ii < rel1.length; ii++) {
						int jj;
						for (jj = 0; jj < rel2.length; jj++)
							if (rel1[ii].equals(rel2[jj]))
								break;
	
						if (jj == rel2.length)
							isMatching = false;
					}
	
					if (isMatching) {
						// both left/right relations and join method matched
						matching.put(joins1[i], joins2[j]);
						setMatchNumber(joins1[i], joins2[j]);
						done[j] = true;
						idone[i] = true;
						break;
					}
				}
	
			// Get the non match join nodes for both trees
			setJoinSimilarity(joins1, joins2, idone, done, matching);
	
			if (tree1.getChildren().size() <= 1
					&& matching.containsKey(tree1) == false) {
				matching.put(tree1, tree2);
				setMatchNumber(tree1, tree2);
			}
	
			Object[] keys = matching.keySet().toArray();
			done = new boolean[keys.length];
			for (int i = 0; i < keys.length; i++) {
				done[i] = false;
			}
	
			/* This is done to get the edit distance for each of the singleton trees */
			for (int i = 0; i < keys.length; i++) {
				if (done[i] == true)
					continue;
	
				TreeNode t1 = (TreeNode) keys[i];
				TreeNode t2 = (TreeNode) matching.get(keys[i]);
	
				setEditNodes(t1, t2, matching);
			}
			Vector entries = new Vector();
			getNonMatchingNodes(entries, tree1);
			if (entries.size() == 0) {
				getNonMatchingNodes(entries, tree1);				
			}
			return matching;
		}
	
		boolean getRelationMatch(TreeNode node1, TreeNode node2) {
			boolean isMatching = true;
			String[] rel1 = TreeUtil.getRelations(node1);
			String[] rel2 = TreeUtil.getRelations(node2);
			boolean[] done2 = new boolean[rel2.length];
	
			if (rel1.length != rel2.length)
				isMatching = false;
	
			for (int ii = 0; isMatching && ii < rel1.length; ii++) {				
				done2[ii] = false;
			}
	
			for (int ii = 0; isMatching && ii < rel1.length; ii++) {
				int jj;
				for (jj = 0; jj < rel2.length; jj++) {
					if (rel1[ii].equals(rel2[jj]) && done2[jj] != true) {
						done2[jj] = true;
						break;
					}
				}
	
				if (jj == rel2.length)
					isMatching = false;
			}
			return isMatching;
		}
	
		void doDuplicateLeafMatch(Hashtable matching, TreeNode[] leaves1,
				TreeNode[] leaves2, boolean[] idone, boolean[] jdone) {
			for (int i = 0; i < leaves1.length; i++) {
				if (idone[i] == true)
					continue;
				int[] jdup = new int[leaves2.length];
				int jcount = 0;
				for (int j = 0; j < leaves2.length; j++) {
					if (leaves1[i].getNodeName().equals(leaves2[j].getNodeName())
							&& jdone[j] == false)
						jdup[jcount++] = j;
				}
				if (jcount > 1) {
					int[] idup = new int[leaves1.length];
					int icount = 0;
					// Get all the leaves1 duplicates as weel to get the best match
					for (int j = i; j < leaves1.length; j++) {
						if (leaves1[i].getNodeName().equals(
								leaves1[j].getNodeName())) {
							idup[icount++] = j;
						}
					}
					// Compute edit distance for all of them
					int minEditDistance = 100;
					int mj = -1;
					int mi = -1;
					int[][] editDist = new int[icount][jcount];
					for (int l = 0; l < icount; l++) {
						for (int j = 0; j < jcount; j++) {
							editDist[l][j] = getEditDistance(leaves1[idup[l]],
									leaves2[jdup[j]]);
							if (minEditDistance > editDist[l][j]) {
								minEditDistance = editDist[l][j];
								mi = l;
								mj = j;
							}
						}
					}
					// Put the first one in and then do the rest again in a similar
					// fashion..
					// Match for this is mj and mi
					while (icount != 0) {
						matching.put(leaves1[idup[mi]], leaves2[jdup[mj]]);
						setMatchNumber(leaves1[idup[mi]], leaves2[jdup[mj]]);
						jdone[jdup[mj]] = true;
						idone[idup[mi]] = true;
						editDist[mi][mj] = icount + jcount; // set to a high number
						minEditDistance = icount + jcount;
						mi = -1;
						mj = -1;
						for (int l = 0; l < icount; l++) {
							for (int j = 0; j < jcount; j++) {
								if (minEditDistance > editDist[l][j]
										&& idone[idup[l]] == false
										&& jdone[jdup[j]] == false) {
									minEditDistance = editDist[l][j];
									mi = l;
									mj = j;
								}
							}
						}
						if (mi == -1 || mj == -1)
							break;
						// get next i and j
						icount--;
					}
				} // end of k > 1
			}
		}
	
		Vector getAllChildren(TreeNode node, Vector children) {
			if (node == null)
				return children;
	
			Vector ch = node.getChildren();
			for (int i = 0; i < ch.size(); i++) {
				children.add(""+ch.elementAt(i));
			}
			for (int i = 0; i < ch.size(); i++) {
				getAllChildren((TreeNode) ch.elementAt(i), children);
			}
			return children;
		}
	
		void getExactRelationMatch(TreeNode[] joins1, TreeNode[] joins2,
				boolean[] idone, boolean[] jdone, Hashtable matching) {
			for (int i = 0; i < joins1.length; i++) {
				if (idone[i] == true)
					continue;
	
				TreeNode node1 = joins1[i];
				int csimType = PicassoConstants.T_NO_DIFF_DONE;
				// Get an array of all relational # match
				for (int j = 0; j < joins2.length; j++) {
					if (jdone[j] == true)
						continue;
					TreeNode node2 = joins2[j];
					if (getRelationMatch(node1, node2) == false)
						continue;
					csimType = getSimilarityType(node1, node2, true);
					setJoinSimType(node1, node2, csimType);
					jdone[j] = true;
					idone[i] = true;
					setEditNodes(node1, node2, matching);
					break;
				}
			}
		}
	
		void getCountRelationMatch(TreeNode[] joins1, TreeNode[] joins2,
				boolean[] idone, boolean[] jdone, Hashtable matching) {
			for (int i = 0; i < joins1.length; i++) {
				if (idone[i] == true)
					continue;
	
				TreeNode node1 = joins1[i];
				int csimType = PicassoConstants.T_NO_DIFF_DONE;
				String[] rel1 = TreeUtil.getRelations(node1);
				int simType;
				TreeNode cnode2 = null;
				int jindex = -1;
	
				// Get an array of all relational # match
				for (int j = 0; j < joins2.length; j++) {
					if (jdone[j] == true)
						continue;
					TreeNode node2 = joins2[j];
					String[] rel2 = TreeUtil.getRelations(joins2[j]);
					if (rel1.length != rel2.length)
						continue;
					simType = getSimilarityType(node1, node2, true);
					int subop = 0;
					if (!TreeUtil.isEquals(node1, node2,
							TreeUtil.SUB_OPERATOR_LEVEL))
						subop = SUBOP_OFFSET;
					if (simType < csimType + subop) {
						csimType = simType;
						cnode2 = node2;
						jindex = j;
					}
				}
				if (cnode2 != null) {					
					setJoinSimType(node1, cnode2, csimType);
					jdone[jindex] = true;
					idone[i] = true;
					setEditNodes(node1, cnode2, matching);
				}
			}
		}
	
		void getNonExactJoinMatch(TreeNode[] joins1, TreeNode[] joins2,
				boolean[] idone, boolean[] jdone, Hashtable matching) {
			for (int i = 0; i < joins1.length; i++) {
				if (idone[i] == true)
					continue;
	
				joins1[i].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
			}
	
			for (int j = 0; j < joins2.length; j++) {
				if (jdone[j] == true)
					continue;
				joins2[j].setSimilarity(PicassoConstants.T_NO_DIFF_DONE);
			}
		}
	
		int matchNum = 1;
	
		void setJoinSimilarity(TreeNode[] joins1, TreeNode[] joins2,
				boolean[] idone, boolean[] jdone, Hashtable matching) {
			 /*Do two passes. In the first pass get exact relation matches, then in
			 the next pass
			 take the unmatched nodes and do the count and group them all
			 together.*/
			getExactRelationMatch(joins1, joins2, idone, jdone, matching);
			getCountRelationMatch(joins1, joins2, idone, jdone, matching);
			getNonExactJoinMatch(joins1, joins2, idone, jdone, matching);
		}
	
		void setJoinSimType(TreeNode node1, TreeNode node2, int simType) {
			if (simType == PicassoConstants.T_NP_LR_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_NP_LEFT_EQ);
				node2.setSimilarity(PicassoConstants.T_NP_RIGHT_EQ);
			} else if (simType == PicassoConstants.T_NP_RL_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_NP_RIGHT_EQ);
				node2.setSimilarity(PicassoConstants.T_NP_LEFT_EQ);
			} else if (simType == PicassoConstants.T_RL_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_RIGHT_EQ);
				node2.setSimilarity(PicassoConstants.T_LEFT_EQ);
			} else if (simType == PicassoConstants.T_LR_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_LEFT_EQ);
				node2.setSimilarity(PicassoConstants.T_RIGHT_EQ);
			} else if (simType == PicassoConstants.T_SO_RL_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_SO_RIGHT_SIMILAR);
				node2.setSimilarity(PicassoConstants.T_SO_LEFT_SIMILAR);
			} else if (simType == PicassoConstants.T_SO_LR_SIMILAR) {
				node1.setSimilarity(PicassoConstants.T_SO_LEFT_SIMILAR);
				node2.setSimilarity(PicassoConstants.T_SO_RIGHT_SIMILAR);
			} else {
				node1.setSimilarity(simType);
				node2.setSimilarity(simType);
			}
			node1.setMatchNumber(matchNum);
			node2.setMatchNumber(matchNum);
			matchNum++;
		}
	
		int getSameCardinality(TreeNode node1, TreeNode[] joins) {
			for (int i = 0; i < joins.length; i++) {
				if (joins[i].getCardinality() == node1.getCardinality())
					return i;
			}
			return -1;
		}	
	}	
	/*PQOrectangle class ends here*/
	
	public void generatePicassoDiagram() throws PicassoException
	{	
		generator = new Random(999999);
		initializeParams();				
		stopThreshold = (errorPercent_I * 10) / 100;
		if(FPCMode == true)
			stopThreshold *= Math.sqrt(dimension);
		sampleSize = 0;
		errPts = 0;
		DBSettings dbSettings = clientPacket.getDBSettings();
		for(int i=0; i<DBConstants.databases.length; i++){
    		DBInfo db = DBConstants.databases[i];
    		if ( dbSettings.getDbVendor().equals(db.name)){
    			treeNames = db.treeNames;
    			treecard = db.treecard;
    		}
    	}		
		doOptimize();		
		doInterpolate();
		if(skipOptimize())
			compareRunTime();
		if(!FPCMode)
		{
			errPts = (int)Math.ceil(errPts/2.0);
			errPts = (int)Math.ceil(((double)errPts*100.0/(double)totalSize));
			int prevSampleSize = totalSize;
			reqSampleSize = sampleSize;
			while(errPts > errorPercent_L  && prevSampleSize != sampleSize)
			{	
				reqSampleSize += 0.005 * totalSize;
				prevSampleSize = sampleSize;
				isInterpolate = false;
				doCorrection();
				errPts = 0;
				doInterpolate();
				if(skipOptimize())
					compareRunTime();
				errPts = (int)Math.ceil(errPts/2.0);
				errPts = (int)Math.ceil(((double)errPts*100.0/(double)totalSize));					
			} 	
		}
		doGenerateCostnCardDiagram();
		//throw new PicassoException("SampleSize:" + (sampleSize/totalSize) * 100);
		Vector puttrees = new Vector();
		puttrees.add(new Integer(plans.size()));
		puttrees.addAll(trees);
		trees = new Vector(puttrees);
		savePicassoDiagram();
	}
	public void estimateSample(int[] offset) throws PicassoException
	{	
		//initializeParams();
		
		data = new DataValues[totalSize];				
		stopThreshold = (errorPercent_I * 10) / 100;
		setOffset(offset);
		sampleSize = 0;
		errPts = 0;		
		doOptimize();		
		doInterpolate();
		/*if(skipOptimize())
			compareRunTime();*/
		errPts = (int)Math.ceil(errPts/2.5);
		errPts = (int)Math.min(Math.ceil(((double)errPts*100.0/(double)totalSize)),errPts);
		int prevSampleSize = totalSize;
		reqSampleSize = sampleSize;
		while(errPts > (errorPercent_L)*1.5  && prevSampleSize != sampleSize)
		{				
			reqSampleSize += Math.max(0.0005 * totalSize,5);
			prevSampleSize = sampleSize;
			isInterpolate = false;
			doCorrection();
			errPts = 0;
			doInterpolate();
			/*if(skipOptimize())
				compareRunTime();*/
			errPts = (int)Math.ceil(errPts/2.5);
			errPts = (int)Math.min(Math.ceil(((double)errPts*100.0/(double)totalSize)),errPts);					
		} 	
		/*doGenerateCostnCardDiagram();
		savePicassoDiagram();*/
		
	}
	
 void recursiveInitGridSmall(int depth,int[] interval,int[] orig)
    {
    	if(depth == 1)
    	{
    		for(index[depth-1]=orig[depth-1];index[depth-1]<=orig[depth-1]+interval[depth-1];index[depth-1]+=interval[depth-1])
    		{    			
    			try {	    				
					setPlanResult();										
				} catch (PicassoException e) {
					
					e.printStackTrace();
				}				
    		}
    		
    	}
    	else
    	{
    		for(index[depth-1]=orig[depth-1];index[depth-1]<=orig[depth-1]+interval[depth-1];index[depth-1]+=interval[depth-1])
    		{
    			recursiveInitGridSmall(depth-1,interval,orig);    			
    		}    		
    		
    	}
    }
 
	public int estimateTime() throws PicassoException
	{		
		//DataValues[] backup = null;
		long startTime = System.currentTimeMillis();
		double estTimeTaken = 0;
		
		//***********************Leave as it is *************************************//
		isEstimationProcess = true;
		initializeParams();
		
		int diagSize = 1;
		for(int i = 0;i < dimension;i++)
			diagSize *= resolution[i];
		if(diagSize < PicassoConstants.RSNN_ESTIMATION_START_THRESHOLD)
			return 60;
		int fullSize = totalSize;
		stopThreshold = (errorPercent_I * 10) / 100;
		if(FPCMode == true)
			stopThreshold *= Math.sqrt(dimension);
		plans = new Vector();
		if(skipOptimize())
		{
			for(int i=0;i<actualPlans;i++)
			{
				planList[i]=-1;
			}
		}
		DBSettings dbSettings = clientPacket.getDBSettings();
		for(int i=0; i<DBConstants.databases.length; i++){
    		DBInfo db = DBConstants.databases[i];
    		if ( dbSettings.getDbVendor().equals(db.name)){
    			treeNames = db.treeNames;
    			treecard = db.treecard;
    		}
    	}
		
		
		
		//***********************Leave as it is :end*************************************//
		
		//********************** specify the minimum box volume*************************//
		int samplesTaken = 0;		
		int[] interval=new int[dimension];
		for(int i = 0;i < dimension;i++){
			if(resolution[i] <= 30)				
				interval[i] = (int)(4.0/((double)dimension-1.0));			
			else if(resolution[i] <= 300)
				interval[i] =(int)(8.0/((double)dimension-1.0));
			else 
				interval[i] = (int)(16.0/((double)dimension-1.0));
				
		}	
		//****************************change origin values******************************//
		int[] orig = new int[dimension];
		offsetFromOrigin = new int[dimension];
		PQORectangle[] Boxes = new PQORectangle[dimension];
		int maxBOX = 0;
		
		for(int i=0;i<dimension;i++)
		{
			offsetFromOrigin[i] = 0;
		}
		
		for(int k=0;k<dimension;k++)
		{				
			for(int i=0;i<dimension;i++)
			{					
				if(i==k)
					orig[i] = resolution[i]-interval[i]-1;
				else
					orig[i] = 0;									
			}			
			recursiveInitGridSmall(dimension,interval,orig);
			Boxes[k] = new PQORectangle(getIndex(orig),interval);
			if(Boxes[maxBOX].mean > Boxes[k].mean)
			{
				maxBOX = k;
			}
		}		
		
		try {
						
			estimateSampleSize(getIndexBack(Boxes[maxBOX].StrataIdentifier),interval);
			int totBoxes = (int)Math.floor((double)fullSize/(double)totalSize);
			int denseBoxes = 0;
			for(int i=0;i<dimension;i++)
			{
				denseBoxes += (int)Math.floor((double)resolution[i]/(double)interval[i]);
			}
			denseBoxes -= (dimension-1);
			estTimeTaken = denseBoxes * sampleSize;
			samplesTaken += sampleSize;

			for(int i=0;i<dimension;i++)
			{					
				orig[i] = resolution[i]-interval[i];							
			}		
			
			estimateSampleSize(orig,interval);	
			estTimeTaken += (totBoxes - denseBoxes) * sampleSize;		
			samplesTaken += sampleSize;			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		//********************************end		
		timeTaken = System.currentTimeMillis()-startTime;
		//estTimeTaken /= Math.pow(dimension-1, 0.5);
		long actualEstimate = (long)((double)timeTaken / (double)samplesTaken);		
		double percentage = ((double)estTimeTaken/(double)fullSize) * 100;
		System.out.println("% Samples ="+percentage);
		estTimeTaken *= actualEstimate; 
		return((int)(estTimeTaken/1000.0));
	}
	
	public void estimateSampleSize(int[] origin,int[] rectangleLength) throws PicassoException
	{
		totalSize = 1;
		reloc_resolution = new int[dimension];				
		for(int i=0;i<dimension;i++)
		{
			reloc_resolution[i] = resolution[i];
			resolution[i] = rectangleLength[i];
			totalSize *= rectangleLength[i];
		}		
		try {
			estimateSample(origin);
		} catch (PicassoException e) {			
			e.printStackTrace();
		}		
		System.out.println("Samples Wasted:"+sampleSize);
		for(int j=0;j<dimension;j++)
			resolution[j] = reloc_resolution[j];
	}
}

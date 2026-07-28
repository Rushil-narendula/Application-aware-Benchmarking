package iisc.dsl.picasso.server.sampling;

import java.util.ArrayList;
import java.util.HashMap;
import iisc.dsl.picasso.common.ds.DataValues;
import linpack.NotFullRankException;
import linpack.LU_j;
import linpack.QR_j;

public class CardFunction {	

	float[] index;
	int dimension, resolution[];
	float card_coeff[],theta;
	float[] picsel;
	int paramSize = 9;
	int totalSize;
	double params[][];
	double iparams[];
	double RA2;
	boolean isSplit = false;
	int minVal[],maxVal[]; 
	float minCard = Float.MAX_VALUE;
	float maxCard = Float.MAX_VALUE;
	int sampleSize = 0;	
	CardFunction(DataValues[] xData,int dim,int[] res,float[] psel)
	{	
		dimension = dim;
		resolution = res;	
		picsel = copyfarray(psel);
		getSampleSize(xData);		
		paramSize = 1 + 2*dimension;
		for(int i=2;i<=dimension;i++)
			paramSize += 2*(int)combination(dimension, i);
		params = new double[paramSize][paramSize+1];
		iparams = new double[paramSize+1];	
		totalSize = xData.length;
		card_coeff = new float[paramSize];
		findCardFunction(xData);
	}

	void findCardFunction(DataValues[] xData)
	{			
		try {	
			constructParams(xData);
			curveFit();
			RA2 = findRA2(xData);
			double prevRA2 = RA2;
			if(RA2 < 0.7)
			{			
				isSplit = true;
				minVal = new int[dimension];
				maxVal = new int[dimension];
				int minData = (int)(Math.sqrt(totalSize)*0.5);
				DBScan(minData , (float)0.25, xData);	
				constructParams(xData);
				curveFit();
				RA2 = findRA2(xData);
			}
			if(RA2 < prevRA2)
			{
				isSplit = false;
				constructParams(xData);
				curveFit();
				RA2 = findRA2(xData);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	private void curveFit()
	{						
		double A[][] = new double[paramSize][paramSize];
		double B[] = new double[paramSize];
		for(int i=0;i<paramSize;i++)
		{			
			for(int j=0;j<paramSize;j++)
			{				
				A[i][j] = params[i][j];				
			}
			B[i] = params[i][paramSize];			
		}				 
		int[] pivotData = new int[paramSize];	

		try {			
			LU_j.dgefa_j(A, paramSize, pivotData); // Find LU decomposition of A using LINPACK
			LU_j.dgesl_j(A, paramSize, pivotData, B, 0); //Find solution of Ax=B
			for(int i=0;i<paramSize;i++)
			{
				card_coeff[i] = (float)B[i];
			}
		} catch (NotFullRankException e1) {
			for(int i=0;i<paramSize;i++)
			{			
				for(int j=0;j<paramSize;j++)
				{				
					A[i][j] = params[i][j];				
				}
				B[i] = params[i][paramSize];			
			}
			pivotData = new int[paramSize];			
			double auxQR[] = new double[paramSize];		
			double[] tmpSpace = new double[paramSize];
			double[] Output = new double[paramSize]; 
			double QB[] = new double[paramSize];
			double QTB[] = new double[paramSize];
			double residual[] = new double[paramSize];
			double XB[] = new double[paramSize];
			QR_j.dqrdc_j(A, paramSize, paramSize, auxQR, pivotData, tmpSpace, 1); //Finding QR decomposition using LINPACK		
			QR_j.dqrsl_j(A, paramSize, paramSize, auxQR, B, QB, QTB, Output, residual, XB, 111); //Finding solution of Ax=B using LINPACK	
			for(int i=0;i<paramSize;i++)
			{
				card_coeff[pivotData[i]-1] = (float)Output[i];
			}	
		}		
	}
	int paramCount = 0;
	private void constructiParams(int inx,float card)
	{									
		iparams = new double[paramSize+1];				
		index = getIndexBack(inx);					
		for(int i=0;i<dimension;i++)
			index[i] = getPicSel((int)index[i], i);		
		paramCount = 0;
		iparams[paramCount++] = 1;
		for(int i=1;i<dimension;i++)
			for(int m=0;m<dimension;m++)
				recursiveFindParam((float)index[m],m,i);
		double tmp = 1;
		for(int i=0;i<dimension;i++)
		{
			tmp *=index[i];
		}
		iparams[paramCount++] = tmp;
		tmp = (tmp * Math.log(tmp));
		iparams[paramCount++] = tmp;
		iparams[paramCount] = card;		
	}
	private void constructiParams(int inx)
	{									
		iparams = new double[paramSize];				
		index = getIndexBack(inx);					
		for(int i=0;i<dimension;i++)
			index[i] = getPicSel((int)index[i], i);		
		paramCount = 0;
		iparams[paramCount++] = 1;
		for(int i=1;i<dimension;i++)
			for(int m=0;m<dimension;m++)
				recursiveFindParam((float)index[m],m,i);
		double tmp = 1;
		for(int i=0;i<dimension;i++)
		{
			tmp *=index[i];
		}
		iparams[paramCount++] = tmp;
		tmp = (tmp * Math.log(tmp));
		iparams[paramCount++] = tmp;						
	}
	private void recursiveFindParam(float val,int m,int depth)
	{
		if(depth == 1)
		{			
			iparams[paramCount++] = val;				
			iparams[paramCount] = 0;			 
			if(val != 0)				
				iparams[paramCount] = (val * Math.log(val));
			paramCount++;
		}		
		else
		{
			for(int i = m+1;i<dimension;i++)
				recursiveFindParam((val*index[i]),m+1,depth-1);
		}		
	}
	private void constructParams(DataValues[] xData)
	{				
		params = new double[paramSize][paramSize+1];
		card_coeff = new float[paramSize];
		
		for(int i=0;i<paramSize;i++)		
			for(int m=0;m<=paramSize;m++)
				params[i][m]=0;
		
		for(int k=0;k<totalSize;k++)
		{	
			if(xData[k]!= null && xData[k].isRepresentative && isFitted(k))
			{
				constructiParams(k,(float)xData[k].getCard());
				for(int i=0;i<paramSize;i++)							
					for(int j=0;j<=paramSize;j++)
						params[i][j] += iparams[j] * iparams[i];	
			}
		}		
	}
	private double findRA2(DataValues[] xData)
	{	
		double YBar=0,fi=0,Yi=0,SSE=0,SST=0;		
		for(int i = 0;i<totalSize;i++)
		{	
			if(xData[i] != null && xData[i].isRepresentative)
			{
				YBar += xData[i].getCard();			
			}
		}
		YBar /= sampleSize;
		for(int i = 0;i<totalSize;i++)
		{	
			if(xData[i] != null && xData[i].isRepresentative)
			{
				Yi = xData[i].getCard();
				fi = getCard(i);
				SST +=  Math.pow(Yi - YBar,2);
				SSE +=  Math.pow(Yi - fi,2);				
			}			
		}		
		double numerator = SSE / (sampleSize - (paramSize + 1));
		double denominator = SST / (sampleSize - 1);
		double Ra_Sqr = 1 - numerator/denominator;
		return Ra_Sqr;
	}
	public boolean isFitted(int a_index)
	{
		index = getIndexBack(a_index);
		if(isSplit)
		{
			int allLow = 0,allHigh = 0;
			for(int j=0;j<dimension;j++)
			{
				if(index[j] <= minVal[j])
					allLow++;
				if(index[j] >= maxVal[j])
					allHigh++;	
			}

			if(allLow == dimension)
			{
				return false;
			}	
			else if(allHigh == dimension)
			{
				return false;
			}
		}
		return(true);
}
	public float getCard(int a_index)
	{	
		index = getIndexBack(a_index);
		if(isSplit)
		{
			int allLow = 0,allHigh = 0;
			for(int j=0;j<dimension;j++)
			{
				if(index[j] <= minVal[j])
					allLow++;
				if(index[j] >= maxVal[j])
					allHigh++;	
			}
			if(allLow == dimension)
			{
				return minCard;
			}	
			else if(allHigh == dimension)
			{
				return maxCard;
			}
		}
		constructiParams(a_index);		
		float card = 0;		
		try {
			for(int i=0;i<paramSize;i++)
			{			
				card += card_coeff[i] * iparams[i];
			}
		} catch (Exception e) {
			card = -1;
		}
		return card;
	}

	private float getPicSel(int pos,int dim)
	{
		int res = 0;
		for(int i = 0;i < dim;i++)
			res += resolution[i];
		return(picsel[res + pos]);
	}
	public void getSampleSize(DataValues[] source)
	{
		sampleSize = 0;
		for(int i = 0 ;i<source.length;i++){
        	if(source[i]!=null && source[i].isRepresentative)
        	{
        		sampleSize++;
        	}
		}
	}
    public long factorial(int n)
    {
        return (n <= 1) ? 1 : n*factorial(n-1);
    }

    public long combination(int n, int p)
    {
        return factorial(n) / (factorial(n - p) * factorial(p));
    }
	
	
	
	public float[] copyfarray(float[] source)
    {
        float[] target = new float[source.length];
        for(int i = 0 ;i<source.length;i++)
            target[i]=source[i];
        return target;
    }
	public DataValues[] copydata(DataValues[] source)
    {
        DataValues[] target = new DataValues[source.length];
        for(int i = 0 ;i<source.length;i++){
        	if(source[i]!=null && source[i].isRepresentative)
        	{
	            target[i] = new DataValues();
	            target[i].setPlanNumber(source[i].getPlanNumber());
	            target[i].setCard(source[i].getCard());
	            target[i].setCost(source[i].getCost());
        	}
        }
        return target;            
    }	
	
	public float[] getIndexBack(int inx)
	{
		float[] index = new float[dimension];
		for(int i = 0; i < dimension;i++)
		{
			index[i] = (float)(inx % resolution[i]);
			inx /= resolution[i];
		}
		return index;
	}

	 /*****************************DBSCAN*******************************/
	public float[] getDBDist(float currVal,int currInd,DataValues[] data)
	{
		float [] dVal = new float [totalSize];
		for(int i=1;i<totalSize;i++)
		{
			dVal[i] = -1;
			if(data[i] != null && data[i].isRepresentative)
			{
				dVal[i] = Math.min(Math.abs((float)data[i].getCard() - currVal),Math.abs((float)data[i].getCard() - currVal)/currVal);
			}			
		}
		return dVal;
	}
	protected int getIndex(float[] index)
	{
		int tmp=0;
		for(int i=index.length-1;i>=0;i--)
			tmp=(int)(tmp*resolution[i]+index[i]);
		return tmp;
	}
	HashMap<Integer,Integer> retVal;
	public void findDBNeighbors(float radius,float[] D,int origin,int increment)
	{
		
		float index[]= new float[dimension];
		float tmpIndex[]= new float[dimension];
		tmpIndex = getIndexBack(origin);
		boolean doBreak = false;
		for(int dim1=0;dim1<dimension;dim1++)
		{
			doBreak = false;
			for(int dim2=0;dim2<dimension;dim2++)
			{
				index[dim2] = tmpIndex[dim2];
				if(dim1 == dim2)
					index[dim2]+=increment; 
				if(index[dim2]>resolution[dim2]-1 || index[dim2] < 0)
				{
					doBreak = true;
				}
			}			
			if(doBreak)
				break;
			int inx = getIndex(index);
			boolean interiorPt = true;
			if(increment > 0)
			{
				for(int k=0;k<dimension;k++)
					if(minVal[k] < (int)index[k] && minVal[k]!=-1)
						interiorPt = false;
			}
			else
			{
				for(int k=0;k<dimension;k++)
					if(maxVal[k] > (int)index[k] && maxVal[k]!=resolution[k])
						interiorPt = false;
			}
			if((D[inx]>=0 && D[inx] <= radius) || (D[inx]==-1 && interiorPt))
			{				
				try {
					if(retVal.get(inx) == null)
					{
						retVal.put(inx,inx);
						findDBNeighbors(radius,D,inx,increment);
						if(increment > 0)
						{
							for(int k=0;k<dimension;k++)
			  					if(minVal[k] < (int)index[k])
			  						minVal[k] = (int)index[k];
						}
						else
						{
							for(int k=0;k<dimension;k++)
								if(maxVal[k]>index[k])
									maxVal[k] = (int)index[k];
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void DBScan(int minData,float radius,DataValues[] data)
	{		
		int inx;int i = 0;
		minCard = 0; maxCard = 0;
		for(int j=0;j<dimension;j++)
		{
			minVal[j] = -1;
			maxVal[j] = resolution[j];
		}		
		/*****************************************Card_min***********************************************************/
		
		float observedVal = (float)data[i].getCard();
		float[] D = getDBDist(observedVal,i,data);
		retVal = new HashMap<Integer,Integer>();
		retVal.put(i,i);
		findDBNeighbors(radius,D,i,1);
		ArrayList<Integer> Qualified = new ArrayList<Integer>();
		for(int j=0;j<totalSize;j++)
		{
			if(retVal.get(j)!=null)
			{
				Qualified.add(j);
			}
		}
		
        if(Qualified.size() > 1 && Qualified.size()< minData)
        {
          for(int j=0;j<dimension;j++)	
	  			minVal[j] = -1;
        }

        if(Qualified.size()==1)
        {	         
          for(int j=0;j<dimension;j++)
  			minVal[j] = -1;
        }	

        if(Qualified.size()>=minData) 
        {
          for(int j=0;j<Qualified.size();j++)
          {       	  
        	  inx = Qualified.get(j);
        	  minCard += (float)data[inx].getCard();
        	  index = getIndexBack(inx);
          }
          minCard /= Qualified.size();
        }  		
			
		/*****************************************Card_max***********************************************************/
		i = totalSize-1;
		
		observedVal = (float)data[i].getCard();
		D = getDBDist(observedVal,i,data);
		retVal = new HashMap<Integer,Integer>();
		retVal.put(i,i);
		findDBNeighbors(radius,D,i,-1);
		Qualified = new ArrayList<Integer>();
		for(int j=0;j<totalSize;j++)
		{
			if(retVal.get(j)!=null)
			{
				Qualified.add(j);
			}
		}    
        if(Qualified.size() > 1 && Qualified.size()< minData)
        {
          for(int j=0;j<dimension;j++)
	  			maxVal[j] = resolution[j];	          
        }
        if(Qualified.size()==1)
        {
          for(int j=0;j<dimension;j++)
	  			maxVal[j] = resolution[j];
        }	
        if(Qualified.size()>=minData) 
        { 
          for(int j=0;j<Qualified.size();j++)
          {       	 
        	  inx = Qualified.get(j);	        	  
        	  maxCard += (float)data[inx].getCard();
        	  index = getIndexBack(inx);
          }		          
          maxCard /= Qualified.size();
        }			
		retVal = null;
	}   	
   
}




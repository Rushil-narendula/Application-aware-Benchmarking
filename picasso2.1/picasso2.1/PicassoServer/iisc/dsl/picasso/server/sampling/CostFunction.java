package iisc.dsl.picasso.server.sampling;

import java.util.LinkedList;

import linpack.NotFullRankException;
import linpack.LU_j;
import linpack.QR_j;

import iisc.dsl.picasso.common.ds.DataValues;

public class CostFunction {
	DataValues[] data;
	float[] index;
	int num_plans;
	float plan_coeff[][];
	//float boundary[][];
	double params[][];
	double iparams[];
	int dimension, resolution[];
	int paramSize;
	int matrixSize;
	protected float picsel[];	
	boolean ignoredModelParams[][];
	LinkedList<Integer> coord_plan[];
	int modelParamCount = 0;
	int allowParams = 0;
	int paramCount = 0;
	int currPlanNo=0;
	
	CostFunction(DataValues[] xdata,float[] psel,int nPlans,int dim,int[] res)
	{		
		
		picsel = copyfarray(psel);
		num_plans = nPlans;
		dimension = dim;
		resolution = res;
		paramSize = 1 + 2 * dimension;
		for(int i=2;i<=dimension;i++)
			paramSize += 2 * (int)combination(dimension, i);		
		ignoredModelParams = new boolean[num_plans][paramSize];
		plan_coeff = new float[num_plans][];
		findCostFunction(xdata);
	}
	
	public void findCostFunction(DataValues[] data)
	{
		coord_plan = new LinkedList[num_plans];
		for(int j=0;j<num_plans;j++)
		{
			coord_plan[j] = new LinkedList<Integer>();
		}
		int totalSize = 1;		
		for(int i=0;i<dimension;i++)
			totalSize *= resolution[i];		
		for(int j=0;j<totalSize;j++)
		{
			if(data[j]!=null && data[j].isRepresentative)
				coord_plan[data[j].getPlanNumber()].addLast(j);			
		}		
		for(currPlanNo=0;currPlanNo<num_plans;currPlanNo++)
		{		
			if(coord_plan[currPlanNo].size() <= dimension)
			{
				try {
					for(int i=0;i<paramSize;i++)
						ignoredModelParams[currPlanNo][i] = true;
					ignoredModelParams[currPlanNo][0] = false;
					matrixSize = 1;
					plan_coeff [currPlanNo]= new float[1];				
					plan_coeff[currPlanNo][0] = (float)data[coord_plan[currPlanNo].get(0)].getCost();
				} catch (RuntimeException e) {
					plan_coeff[currPlanNo] = null;
				}
			}
			else if(coord_plan[currPlanNo].size() <= 2*paramSize)
			{	
				try {
					for(int i=0;i<paramSize;i++)
						ignoredModelParams[currPlanNo][i] = true;
					float RA2 = 0,prevRA2 = -999;
					matrixSize = 0;
					for(allowParams = 1;allowParams<=paramSize;allowParams++)
					{
						matrixSize++;
						ignoredModelParams[currPlanNo][allowParams-1] = false;
						constructParams(data);
						curveFit();
						RA2 = findRA2(data);					
						if(RA2 <= prevRA2)
						{
							ignoredModelParams[currPlanNo][allowParams-1] = true;
							matrixSize --;
						}
						else
							prevRA2 = RA2;
					}			
					constructParams(data);
					curveFit();
				} catch (RuntimeException e) {
					plan_coeff[currPlanNo] = null;
				}
			}
			else
			{	
				try {
					matrixSize = paramSize;		
					for(int i=0;i<paramSize;i++)
						ignoredModelParams[currPlanNo][i] = false;
					constructParams(data);
					curveFit();
				} catch (RuntimeException e) {
					plan_coeff[currPlanNo] = null;
				}
			}
						
		}		
				
	}
	private void constructParams(DataValues[] data)
	{
		params = new double[matrixSize][matrixSize+1];		
		plan_coeff[currPlanNo] = new float[matrixSize];
		for(int i=0;i<matrixSize;i++)		
			for(int m=0;m<=matrixSize;m++)
				params[i][m]=0;		
		int inx = 0;
		for(int k=0;k<coord_plan[currPlanNo].size();k++)
		{		
			inx = coord_plan[currPlanNo].get(k);				
			constructiParams(currPlanNo,inx,(float)data[inx].getCost());
			for(int i=0;i<matrixSize;i++)							
				for(int j=0;j<=matrixSize;j++)
					params[i][j] += iparams[j] * iparams[i];								
		}		
	}
	private void curveFit()
	{						
		double A[][] = new double[matrixSize][matrixSize];
		double B[] = new double[matrixSize];
		
		for(int i=0;i<matrixSize;i++)
		{			
			for(int j=0;j<matrixSize;j++)
			{				
				A[i][j] = params[i][j];				
			}
			B[i] = params[i][matrixSize];			
		}				 
		int[] pivotData = new int[matrixSize];	
		 
		try {			
			LU_j.dgefa_j(A, matrixSize, pivotData); // Find LU decomposition of A using LINPACK
			LU_j.dgesl_j(A, matrixSize, pivotData, B, 0); //Find solution of Ax=B
			for(int i=0;i<matrixSize;i++)
			{
				plan_coeff[currPlanNo][i] = (float)B[i];
			}
		} catch (NotFullRankException e1) {
			for(int i=0;i<matrixSize;i++)
			{			
				for(int j=0;j<matrixSize;j++)
				{				
					A[i][j] = params[i][j];				
				}
				B[i] = params[i][matrixSize];			
			}
			pivotData = new int[matrixSize];			
			double auxQR[] = new double[matrixSize];		
			double[] tmpSpace = new double[matrixSize];
			double[] Output = new double[matrixSize]; 
			double QB[] = new double[matrixSize];
			double QTB[] = new double[matrixSize];
			double residual[] = new double[matrixSize];
			double XB[] = new double[matrixSize];
			QR_j.dqrdc_j(A, matrixSize, matrixSize, auxQR, pivotData, tmpSpace, 1); //Finding QR decomposition using LINPACK		
			QR_j.dqrsl_j(A, matrixSize, matrixSize, auxQR, B, QB, QTB, Output, residual, XB, 111); //Finding solution of Ax=B using LINPACK	
			for(int i=0;i<matrixSize;i++)
			{
				plan_coeff[currPlanNo][pivotData[i]-1] = (float)Output[i];
			}	
		}		
		/*Matrix m = new Matrix(A);
		Matrix s = new Matrix(B,matrixSize);
		Matrix R = null;
		try {					
			R = m.solve(s);	
						
		} catch (RuntimeException e) {	
			;
		}		*/
	}
	private float findRA2(DataValues[] data )
	{	
		float YBar=0,fi=0,Yi=0,SSE=0,SST=0;
		int inx = 0;
		int planSize = coord_plan[currPlanNo].size();
		for(int i = 0;i<planSize;i++)
		{
			inx = coord_plan[currPlanNo].get(i);
			YBar += (float)data[inx].getCost();		
		}
		YBar /= (float)planSize;
		for(int i = 0;i<planSize;i++)
		{
			inx = coord_plan[currPlanNo].get(i);
			Yi = (float)data[inx].getCost();
			fi = (float)getCost(inx,currPlanNo);
			SST +=  Math.pow(Yi - YBar,2);
			SSE +=  Math.pow(Yi - fi,2);
			
		}		
		float numerator = SSE / (planSize - (matrixSize + 1));
		float denominator = SST / (planSize - 1);
		float Ra_Sqr = 1 - numerator/denominator;
		return Ra_Sqr;
	}
	
	
	int count = 0;
	private float getPicSel(int pos,int dim)
	{
		int res = 0;
		for(int i = 0;i < dim;i++)
			res += resolution[i];
		return(picsel[res + pos]);
	}
	private void recursiveFindParam(int planNo,float val,int m,int depth)
	{
		if(depth == 1)
		{			
			setiParams(planNo,val);			
			if(val == 0)
				setiParams(planNo,0);
			else
				setiParams(planNo,(float)(val * Math.log(val)));						
		}		
		else
		{
			for(int i = m+1;i<dimension;i++)
				recursiveFindParam(planNo,(float)(val*index[i]),m+1,depth-1);
		}		
	}
	
	private void constructiParams(int planNo,int inx)
	{									
		iparams = new double[matrixSize];
		paramCount = 0;	
		modelParamCount = 0;
		index = getIndexBack(inx);					
		for(int i=0;i<dimension;i++)
			index[i] = getPicSel((int)index[i], i);		
		setiParams(planNo,1);	
		for(int i=1;i<dimension;i++)
			for(int m=0;m<dimension;m++)
				recursiveFindParam(planNo,(float)index[m],m,i);
		double tmp = 1;
		for(int i=0;i<dimension;i++)
		{
			tmp *=index[i];
		}
		setiParams(planNo,(float)tmp);	//iparams[paramCount++] = tmp;
		tmp = tmp * Math.log(tmp);
		setiParams(planNo,(float)tmp);  //iparams[paramCount++] = tmp * Math.log(tmp);
			
	}
	private void constructiParams(int planNo,int inx,float cost)
	{									
		iparams = new double[matrixSize+1];
		
		paramCount = 0;	
		modelParamCount = 0;
		index = getIndexBack(inx);					
		for(int i=0;i<dimension;i++)
			index[i] = getPicSel((int)index[i], i);		
		setiParams(planNo,1);	
		for(int i=1;i<dimension;i++)
			for(int m=0;m<dimension;m++)
				recursiveFindParam(planNo,(float)index[m],m,i);
		double tmp = 1;
		for(int i=0;i<dimension;i++)
		{
			tmp *=index[i];
		}
		setiParams(planNo,(float)tmp);	//iparams[paramCount++] = tmp;
		tmp = tmp * Math.log(tmp);
		setiParams(planNo,(float)tmp);  //iparams[paramCount++] = tmp * Math.log(tmp);
		iparams[paramCount] = cost;		
	}
	private void setiParams(int planNo,float val)
	{
		if(!ignoredModelParams[planNo][modelParamCount])
			iparams[paramCount++] = val;
		modelParamCount++;
	}
	
	public double getCost(int a_index,int p_id)
	{
		matrixSize = plan_coeff[p_id].length;		
		constructiParams(p_id,a_index);		
		double cost = 0.0;		
		try {
			for(int i=0;i<matrixSize;i++)
			{			
				cost += plan_coeff[p_id][i] * iparams[i];
			}
		} catch (Exception e) {
			;
		}
		return cost;
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
        int countTrue = 0;
        for(int i = 0 ;i<source.length;i++){
        	if(source[i]!=null && source[i].isRepresentative)
        	{
	            target[i] = new DataValues();
	            target[i].setPlanNumber(source[i].getPlanNumber());
	            target[i].setCard(source[i].getCard());
	            target[i].setCost(source[i].getCost());
	            countTrue++;
        	}
        }
        // System.out.println(countTrue);
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
	
	public long factorial(int n)
    {
        return (n <= 1) ? 1 : n*factorial(n-1);
    }

    public long combination(int n, int p)
    {
        return factorial(n) / (factorial(n - p) * factorial(p));
    }	
}

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Vector;
import java.util.*;

import iisc.dsl.picasso.common.ds.DiagramPacket;
import iisc.dsl.picasso.common.ds.QueryPacket;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.TreeNode;

public class DisplayDiagramPacket {

	public static Vector trees;
	public static Stack stack = new Stack();
	public static BufferedWriter bout;
	
	public static void main(String[] s)
	{
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(5);
		df.setMinimumFractionDigits(5);
		String newLine = System.getProperty("line.separator");
		
		FileInputStream fis=null;
		FileOutputStream fos=null;
		ObjectInputStream ois=null;
		DiagramPacket p=null;
		
		String fname=null;
		String ipfname = null;
		InputStreamReader isr = new InputStreamReader ( System.in );
	    BufferedReader br = new BufferedReader ( isr );
	    
		System.err.print("Enter the packet file name: ");
		try
		{
			fname = br.readLine();
			ipfname = new String(fname);
		}
		catch(IOException e)
		{
			System.err.println(e.getMessage());
		}
		
	
		try
		{
			fis = new FileInputStream (fname);
		}
		catch(FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			return;
		}
			
		System.err.print("Enter the output file name: ");
		try
		{
		fname = br.readLine();
		}
		catch(IOException e)
		{
			System.err.println(e.getMessage());
		}
		
		try
		{
		fos = new FileOutputStream (fname);
		}
		catch(FileNotFoundException e)
		{	
		}
		
		DataOutputStream dout = new DataOutputStream(fos);
		bout = new BufferedWriter(new OutputStreamWriter(dout));
		
		ByteArrayInputStream bais;
		try
		{
			// System.out.println(ipfname);
			if(ipfname.indexOf(".gz")== -1)
			{
				ois = new ObjectInputStream (fis);
			}
			else
			{
				System.out.println("Decompressing the file.");
				try
				{
					GZIPInputStream gis = new GZIPInputStream(fis);
					ois = new ObjectInputStream(gis);
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		
			p = (DiagramPacket)ois.readObject();
			trees = (Vector)ois.readObject();
			
			bout.newLine();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}

		QueryPacket q = p.getQueryPacket();

		try
		{
		bout.write("QueryTemplate Descriptor: " + q.getQueryName()); bout.newLine();
		bout.write(newLine + "QueryTemplate:" + newLine + q.getQueryTemplate()); bout.newLine();
		bout.write(newLine + "Diagram Execution Type: " + q.getExecType()); bout.newLine();
		if(q.getExecType().equals("APPROX_COMPILETIME"))
		{
			String approxAlgo = new String();
			if(p.getSamplingMode() == 0)
				approxAlgo = "Sampling based algorithm";
			else
				approxAlgo = "Grid based algorithm";
			bout.write(newLine + "Sampling Mode: " + approxAlgo); 
			bout.write(newLine + "Location Error Bound: " + p.getAreaError()); 
			bout.write(newLine + "Identity Error Bound: " + p.getIdentityError()); 
			bout.newLine();
		}
		bout.write("Query Point Distribution: " + q.getDistribution()); bout.newLine();
		bout.write("Number of Dimensions: " + p.getDimension()); bout.newLine();
		
		for(int i = 0; i < p.getDimension(); i++)
		{
			bout.write("{Resolution, StartPoint, EndPoint} on dimension " + i + ": {" + p.getQueryPacket().getResolution(i) + ", " + p.getQueryPacket().getStartPoint(i)*100+ ", " + p.getQueryPacket().getEndPoint(i)*100 + "}");	
			bout.newLine();
		}

		bout.write(newLine + "Total Number of Plans: " + p.getMaxPlanNumber()); bout.newLine();
		bout.write("Minimum Cost: " + p.getMinCost()); bout.newLine();
		bout.write("Maximum Cost: " + p.getMaxCost()); bout.newLine();
		bout.write("Minimum Cardinality: " + p.getMinCard()); bout.newLine();
		bout.write("Maximum Cardinality: " + p.getMaxCard()); bout.newLine();

		String[] rels=p.getRelationNames(), atts=p.getAttributeNames();
		
		bout.write("Relation.Attribute on which :varies is applied:"); bout.newLine();
		for(int i=0;i<rels.length;i++)
		{
			bout.write(rels[i] + "." + atts[i]); bout.newLine();
		}
		

		bout.write(newLine + "Selectivities and Constants for these attributes:"); bout.newLine();
		bout.write("Picasso Selectivity	Predicate Selectivity  	Plan Selectivity	Selectivity Constant"); bout.newLine();
		float[] picsel=p.getPicassoSelectivity();
	        float[]	predsel=p.getPredicateSelectivity();
		float[] plansel=p.getPlanSelectivity();
		String[] constants=p.getConstants();
		
		for(int i=0;i<picsel.length;i++)
		{
			bout.write(rightAlign(df.format(picsel[i]),15) + "\t\t" + rightAlign(df.format(predsel[i]),15) + "\t\t" + rightAlign(df.format(plansel[i]),15) + "\t\t" + constants[i]); bout.newLine(); 
		}
	
		/*	
		bout.write("Press any key to display info about all points in the diagram.");
		try
		{
		System.in.read();
		}
		catch(IOException e) {}
		*/
		
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		
		DataValues[] d = p.getData();
		bout.write(newLine + newLine + "Points in row major order from lowest selectivity vector to highest:"); bout.newLine();
		bout.write("Point number  PlanNumber  	Cost		Cardinality"); bout.newLine();
		
		for(int i=0;i<d.length;i++)
		{
			bout.write(rightAlign(String.valueOf(i),12) + " " + rightAlign(String.valueOf(d[i].getPlanNumber()),10) + " " + rightAlign(df.format(d[i].getCost()),15) + " " + rightAlign(df.format(d[i].getCard()),15)); bout.newLine();
		}
			
		bout.write("----- TREES PRINTED IN DEPTH FIRST MANNER -----");
		for(int i = 2; i < trees.size(); i += 2)
		{
			emptystack();
			bout.write("Plan " + i/2);
			bout.newLine();
			stack.push((TreeNode)(trees.elementAt(i)));
			showTrees(0);
			bout.newLine();
			bout.newLine();
		}
		
		bout.close();
		fos.close();
	}
	catch(IOException e)
	{
		System.err.println(e.getMessage());
	}

	System.err.println("Done.");
	}

	static String rightAlign(String s,int l)
	{
		String ret="";
		if(l<=s.length())
			return s;
		else 
		{
			for(int i=0;i<l-s.length();i++)
			{
				ret+=" ";
			}
			ret+=s;
			return ret;
		}		
	}
	
	
	public static void showTrees(int depth)
	{
		try
		{
			/*bout.write(trees.toString() + " ");
			for(int i = 0; i < trees.getChildren().size(); i++)
				showTrees((TreeNode)(trees.getChildren()).elementAt(i),bout);*/

			TreeNode node = null;
			if(!stack.isEmpty())
			{
				node = (TreeNode)stack.pop();
			}
			
			if(node == null)
				// System.out.println("Shout");
				return;
			
			for(int i = 0; i < depth; i++)
			{
				bout.write("  ");
			}
			bout.write(node.toString());
			bout.newLine();
			
			Vector children = node.getChildren();
			for(int i = 0; i < children.size(); i++)
			{
				if(children.elementAt(i) instanceof TreeNode)
				{
					stack.push((TreeNode)(children.elementAt(i)));
					showTrees(depth + 1);
				}
			}
		} 
		catch (Exception e) 
		{
				;
		}
	}
	
	public static void emptystack()
	{
		while(!stack.isEmpty())
			stack.pop();
	}
}

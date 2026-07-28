package iisc.dsl.picasso.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public class PicassoSettingsManipulator
{

	static int SERVER_PORT_NEW=PicassoConstants.SERVER_PORT,
/*	SMALL_COLUMN_NEW=PicassoConstants.SMALL_COLUMN,
	MEDIUM_COLUMN_NEW=PicassoConstants.MEDIUM_COLUMN,
	LARGE_COLUMN_NEW=PicassoConstants.LARGE_COLUMN, */
	PLAN_REDUCTION_THRESHOLD_NEW=(int)PicassoConstants.PLAN_REDUCTION_THRESHOLD,
	REDUCTION_ALGORITHM_NEW=PicassoConstants.REDUCTION_ALGORITHM,
	COLLATION_NEW=PicassoConstants.COLLATION_SCHEME,
	DESIRED_NUM_PLANS_NEW=PicassoConstants.DESIRED_NUM_PLANS;
	static boolean SAVE_COMPRESSED_PACKET_NEW = PicassoConstants.SAVE_COMPRESSED_PACKET;

	static boolean LOW_VIDEO_NEW=PicassoConstants.LOW_VIDEO,
/*	ENABLE_COST_MODEL_NEW=PicassoConstants.ENABLE_COST_MODEL,
	IS_INTERNAL_NEW=PicassoConstants.IS_INTERNAL, */
	IS_CLIENT_DEBUG_NEW=PicassoConstants.IS_CLIENT_DEBUG,
	IS_SERVER_DEBUG_NEW=PicassoConstants.IS_SERVER_DEBUG,
	LIMITED_DIAGRAMS_NEW=PicassoConstants.LIMITED_DIAGRAMS;

	static double SELECTIVITY_LOG_REL_THRESHOLD_NEW=PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD,
	SELECTIVITY_LOG_ABS_THRESHOLD_NEW=PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD,
	COST_DOMINATION_THRESHOLD_NEW=PicassoConstants.COST_DOMINATION_THRESHOLD
/*	,QDIST_SKEW_10_NEW=PicassoConstants.QDIST_SKEW_10,
	QDIST_SKEW_30_NEW=PicassoConstants.QDIST_SKEW_30,
	QDIST_SKEW_100_NEW=PicassoConstants.QDIST_SKEW_100,
	QDIST_SKEW_300_NEW=PicassoConstants.QDIST_SKEW_300,
	QDIST_SKEW_1000_NEW=PicassoConstants.QDIST_SKEW_1000*/;


	public static void ReadPicassoConstantsFromFile()
	{	
		FileInputStream fis; 
		try
		{
			fis = new FileInputStream(PicassoConstants.PICASSO_SETTINGS_FILE);	
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Picasso settings file (" + PicassoConstants.PICASSO_SETTINGS_FILE + ") not found.");
			return;
		}
		DataInputStream din = new DataInputStream(fis);
		BufferedReader in = new BufferedReader(new InputStreamReader(din));
		
	
		String lhs=null,rhs=null;
		try
		{
			while(true)
			{
				String fullline=in.readLine();
				if(fullline==null) break;
				StringTokenizer st = new StringTokenizer(fullline,"=");
				lhs=null;
				rhs=null;
				if(st.hasMoreTokens())
				{
					lhs=st.nextToken().trim();
				}
				if(st.hasMoreTokens())
				{
					rhs=st.nextToken().trim();
				}
				if(lhs==null || rhs==null) continue;

				setthesetting(lhs,rhs);
			}
		}		
		catch(IOException e)
		{
			System.out.println("Error while reading Picasso settings file.");
		}
		try
		{
			fis.close();
		}
		catch(IOException e)
		{
			System.out.println("Error while closing Picasso settings file.");
		}
		putIntoPicassoConstants();
	}
	
	
	
	//Public so as to access from PicassoSettingsFrame
	public static void setthesetting(String lhs,String rhs) 
	{
		
		
		if(lhs.equals("SERVER_PORT"))
		{
			try
			{
				SERVER_PORT_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				SERVER_PORT_NEW=PicassoConstants.SERVER_PORT;
			}
		}
		else if(lhs.equals("LOW_VIDEO"))
		{
			try
			{
				LOW_VIDEO_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				LOW_VIDEO_NEW=PicassoConstants.LOW_VIDEO;
			}
		}
		/*
		else if(lhs.equals("SMALL_COLUMN"))
		{
			try
			{
				SMALL_COLUMN_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				SMALL_COLUMN_NEW=PicassoConstants.SMALL_COLUMN;
			}
		}
		else if(lhs.equals("MEDIUM_COLUMN"))
		{
			try
			{
				MEDIUM_COLUMN_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				MEDIUM_COLUMN_NEW=PicassoConstants.MEDIUM_COLUMN;
			}
		}
		else if(lhs.equals("LARGE_COLUMN"))
		{
			try
			{
				LARGE_COLUMN_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				LARGE_COLUMN_NEW=PicassoConstants.LARGE_COLUMN;
			}
		}
		*/		
		else if(lhs.equals("SELECTIVITY_LOG_REL_THRESHOLD"))
		{
			try
			{
				SELECTIVITY_LOG_REL_THRESHOLD_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				SELECTIVITY_LOG_REL_THRESHOLD_NEW=PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD;
			}
		}		
		else if(lhs.equals("SELECTIVITY_LOG_ABS_THRESHOLD"))
		{
			try
			{
				SELECTIVITY_LOG_ABS_THRESHOLD_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				SELECTIVITY_LOG_ABS_THRESHOLD_NEW=PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD;
			}
		}						
		else if(lhs.equals("PLAN_REDUCTION_THRESHOLD"))
		{
			try
			{
				PLAN_REDUCTION_THRESHOLD_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				PLAN_REDUCTION_THRESHOLD_NEW=(int)PicassoConstants.PLAN_REDUCTION_THRESHOLD;
			}
		}		
		else if(lhs.equals("COST_DOMINATION_THRESHOLD"))
		{
			try
			{
				COST_DOMINATION_THRESHOLD_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				COST_DOMINATION_THRESHOLD_NEW=PicassoConstants.COST_DOMINATION_THRESHOLD;
			}
		}			
		else if(lhs.equals("REDUCTION_ALGORITHM"))
		{
			try
			{
				REDUCTION_ALGORITHM_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				REDUCTION_ALGORITHM_NEW=PicassoConstants.REDUCTION_ALGORITHM;
			}
		}
		else if(lhs.equals("COLLATION")) {
			try {
				COLLATION_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e) {
				COLLATION_NEW=PicassoConstants.COLLATION_SCHEME;
			}
		}
		else if(lhs.equals("DESIRED_NUM_PLANS"))
		{
			try
			{
				DESIRED_NUM_PLANS_NEW=Integer.parseInt(rhs);
			}
			catch(Exception e)
			{
				DESIRED_NUM_PLANS_NEW=PicassoConstants.DESIRED_NUM_PLANS;
			}
		}
		else if(lhs.equals("SAVE_COMPRESSED_PACKET"))
		{
			try
			{
				SAVE_COMPRESSED_PACKET_NEW = Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				SAVE_COMPRESSED_PACKET_NEW = PicassoConstants.SAVE_COMPRESSED_PACKET;
			}
		}
		/*
		else if(lhs.equals("ENABLE_COST_MODEL"))
		{
			try
			{
				ENABLE_COST_MODEL_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				ENABLE_COST_MODEL_NEW=PicassoConstants.ENABLE_COST_MODEL;
			}
		}
		else if(lhs.equals("IS_INTERNAL"))
		{
			try
			{
				IS_INTERNAL_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				IS_INTERNAL_NEW=PicassoConstants.IS_INTERNAL;
			}
		}
		*/
		else if(lhs.equals("IS_CLIENT_DEBUG"))
		{
			try
			{
				IS_CLIENT_DEBUG_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				IS_CLIENT_DEBUG_NEW=PicassoConstants.IS_CLIENT_DEBUG;
			}
		}	
		else if(lhs.equals("IS_SERVER_DEBUG"))
		{
			try
			{
				IS_SERVER_DEBUG_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				IS_SERVER_DEBUG_NEW=PicassoConstants.IS_SERVER_DEBUG;
			}
		}	
		/*
		else if(lhs.equals("QDIST_SKEW_10"))
		{
			try
			{
				QDIST_SKEW_10_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				QDIST_SKEW_10_NEW=PicassoConstants.QDIST_SKEW_10;
			}
		}	
		else if(lhs.equals("QDIST_SKEW_30"))
		{
			try
			{
				QDIST_SKEW_30_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				QDIST_SKEW_30_NEW=PicassoConstants.QDIST_SKEW_30;
			}
		}		
		else if(lhs.equals("QDIST_SKEW_100"))
		{
			try
			{
				QDIST_SKEW_100_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				QDIST_SKEW_100_NEW=PicassoConstants.QDIST_SKEW_100;
			}
		}		
		else if(lhs.equals("QDIST_SKEW_300"))
		{
			try
			{
				QDIST_SKEW_300_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				QDIST_SKEW_300_NEW=PicassoConstants.QDIST_SKEW_300;
			}
		}		
		else if(lhs.equals("QDIST_SKEW_1000"))
		{
			try
			{
				QDIST_SKEW_1000_NEW=Double.parseDouble(rhs);
			}
			catch(Exception e)
			{
				QDIST_SKEW_1000_NEW=PicassoConstants.QDIST_SKEW_1000;
			}
		}
		*/	
		else if(lhs.equals("LIMITED_DIAGRAMS"))
		{
			try
			{
				LIMITED_DIAGRAMS_NEW=Boolean.valueOf(rhs).booleanValue();
			}
			catch(Exception e)
			{
				LIMITED_DIAGRAMS_NEW=PicassoConstants.LIMITED_DIAGRAMS;
			}
		}		
	}
	
//	Public so as to access from PicassoSettingsFrame
	public static void putIntoPicassoConstants()
	{
		PicassoConstants.SERVER_PORT=SERVER_PORT_NEW;
		PicassoConstants.LOW_VIDEO=LOW_VIDEO_NEW;
		/*
		PicassoConstants.SMALL_COLUMN=SMALL_COLUMN_NEW;
		PicassoConstants.MEDIUM_COLUMN=MEDIUM_COLUMN_NEW;
		PicassoConstants.LARGE_COLUMN=LARGE_COLUMN_NEW;
		*/
		PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD=SELECTIVITY_LOG_REL_THRESHOLD_NEW;
		PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD=SELECTIVITY_LOG_ABS_THRESHOLD_NEW;
		PicassoConstants.PLAN_REDUCTION_THRESHOLD=PLAN_REDUCTION_THRESHOLD_NEW;
		PicassoConstants.COST_DOMINATION_THRESHOLD=COST_DOMINATION_THRESHOLD_NEW;
		PicassoConstants.REDUCTION_ALGORITHM=REDUCTION_ALGORITHM_NEW;
		PicassoConstants.COLLATION_SCHEME=COLLATION_NEW;
		PicassoConstants.DESIRED_NUM_PLANS=DESIRED_NUM_PLANS_NEW;
		PicassoConstants.SAVE_COMPRESSED_PACKET=SAVE_COMPRESSED_PACKET_NEW;
		/*
		PicassoConstants.ENABLE_COST_MODEL=ENABLE_COST_MODEL_NEW;
		PicassoConstants.IS_INTERNAL=IS_INTERNAL_NEW;
		*/
		PicassoConstants.IS_CLIENT_DEBUG=IS_CLIENT_DEBUG_NEW;
		PicassoConstants.IS_SERVER_DEBUG=IS_SERVER_DEBUG_NEW;
		/*
		PicassoConstants.QDIST_SKEW_10=QDIST_SKEW_10_NEW;
		PicassoConstants.QDIST_SKEW_30=QDIST_SKEW_30_NEW;
		PicassoConstants.QDIST_SKEW_100=QDIST_SKEW_100_NEW;
		PicassoConstants.QDIST_SKEW_300=QDIST_SKEW_300_NEW;
		PicassoConstants.QDIST_SKEW_1000=QDIST_SKEW_1000_NEW;
		*/
		PicassoConstants.LIMITED_DIAGRAMS=LIMITED_DIAGRAMS_NEW;
		//System.out.println("THIS IS COLLATION_NEW: "+COLLATION_NEW+" "+PicassoConstants.COLLATION_SCHEME);
	}

	//Public so as to access from PicassoSettingsFrame
	public static void WritePicassoConstantsToFile()
	{
		FileOutputStream fos; 
		try
		{
			fos = new FileOutputStream(PicassoConstants.PICASSO_SETTINGS_FILE);	
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Picasso settings file (" + PicassoConstants.PICASSO_SETTINGS_FILE + ") not found.");
			return;
		}
		
		DataOutputStream dout = new DataOutputStream(fos);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(dout));
		
		String towrite="";
		try
		{
	
		towrite="SERVER_PORT"; towrite+=" = "; towrite+=new Integer(PicassoConstants.SERVER_PORT).toString();
		out.write(towrite);
		out.newLine();
		towrite="LOW_VIDEO"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.LOW_VIDEO).toString();
		out.write(towrite);
		out.newLine();
		/*
		towrite="SMALL_COLUMN"; towrite+=" = "; towrite+=new Integer(PicassoConstants.SMALL_COLUMN).toString();
		out.write(towrite);
		out.newLine();
		towrite="MEDIUM_COLUMN"; towrite+=" = "; towrite+=new Integer(PicassoConstants.MEDIUM_COLUMN).toString();
		out.write(towrite);
		out.newLine();
		towrite="LARGE_COLUMN"; towrite+=" = "; towrite+=new Integer(PicassoConstants.LARGE_COLUMN).toString();
		out.write(towrite);
		out.newLine();
		*/
		towrite="SELECTIVITY_LOG_REL_THRESHOLD"; towrite+=" = "; towrite+=new Double(PicassoConstants.SELECTIVITY_LOG_REL_THRESHOLD).toString();
		out.write(towrite);
		out.newLine();
		towrite="SELECTIVITY_LOG_ABS_THRESHOLD"; towrite+=" = "; towrite+=new Double(PicassoConstants.SELECTIVITY_LOG_ABS_THRESHOLD).toString();
		out.write(towrite);
		out.newLine();
		towrite="PLAN_REDUCTION_THRESHOLD"; towrite+=" = "; towrite+=new Double(PicassoConstants.PLAN_REDUCTION_THRESHOLD).toString();
		out.write(towrite);
		out.newLine();
		towrite="COST_DOMINATION_THRESHOLD"; towrite+=" = "; towrite+=new Double(PicassoConstants.COST_DOMINATION_THRESHOLD).toString();
		out.write(towrite);
		out.newLine();
		towrite="REDUCTION_ALGORITHM"; towrite+=" = "; towrite+=new Integer(PicassoConstants.REDUCTION_ALGORITHM).toString();
		out.write(towrite);
		out.newLine();
		towrite="COLLATION"; towrite+=" = "; towrite+=new Integer(PicassoConstants.COLLATION_SCHEME).toString();
		out.write(towrite);
		out.newLine();
		towrite="DESIRED_NUM_PLANS"; towrite+=" = "; towrite+=new Integer(PicassoConstants.DESIRED_NUM_PLANS).toString();
		out.write(towrite);
		out.newLine();
		towrite="SAVE_COMPRESSED_PACKET"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.SAVE_COMPRESSED_PACKET).toString();
		out.write(towrite);
		out.newLine();
		/*
		towrite="ENABLE_COST_MODEL"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.ENABLE_COST_MODEL).toString();
		out.write(towrite);
		out.newLine();
		towrite="QDIST_SKEW_10"; towrite+=" = "; towrite+=new Double(PicassoConstants.QDIST_SKEW_10).toString();
		out.write(towrite);
		out.newLine();
		towrite="QDIST_SKEW_30"; towrite+=" = "; towrite+=new Double(PicassoConstants.QDIST_SKEW_30).toString();
		out.write(towrite);
		out.newLine();
		towrite="QDIST_SKEW_100"; towrite+=" = "; towrite+=new Double(PicassoConstants.QDIST_SKEW_100).toString();
		out.write(towrite);
		out.newLine();
		towrite="QDIST_SKEW_300"; towrite+=" = "; towrite+=new Double(PicassoConstants.QDIST_SKEW_300).toString();
		out.write(towrite);
		out.newLine();
		towrite="QDIST_SKEW_1000"; towrite+=" = "; towrite+=new Double(PicassoConstants.QDIST_SKEW_1000).toString();
		out.write(towrite);
		out.newLine();
		towrite="IS_INTERNAL"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.IS_INTERNAL).toString();
		out.write(towrite);
		out.newLine();
		*/
		towrite="IS_CLIENT_DEBUG"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.IS_CLIENT_DEBUG).toString();
		out.write(towrite);
		out.newLine();
		towrite="IS_SERVER_DEBUG"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.IS_SERVER_DEBUG).toString();
		out.write(towrite);
		out.newLine();
		towrite="LIMITED_DIAGRAMS"; towrite+=" = "; towrite+=new Boolean(PicassoConstants.LIMITED_DIAGRAMS).toString();
		out.write(towrite);
		out.newLine();
		
		out.flush();
		}
		catch(IOException e)
		{
			System.out.println("Error while writing Picasso settings file.");
			return;
		}
		try
		{
			fos.close();
		}
		catch(IOException e)
		{
			System.out.println("Error while closing Picasso settings file.");
			return;
		}
	}
}
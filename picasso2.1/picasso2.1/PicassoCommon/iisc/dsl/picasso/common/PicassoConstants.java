
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

import java.awt.Color;
import java.util.Vector;

public class PicassoConstants {

//	USER SETTABLE CONSTANTS 

	public static int SERVER_PORT = 4444;
	public static boolean LOW_VIDEO = false;
	// Thresholds
	public static double SELECTIVITY_LOG_REL_THRESHOLD = 10;
	public static double SELECTIVITY_LOG_ABS_THRESHOLD = 1;

	public static double PLAN_REDUCTION_THRESHOLD = 10;
	public static double COST_DOMINATION_THRESHOLD = 95;
	
	//Dimensions restricted to 4. If needed can increase it. Will need to give suitable support 
	//in Cost Greedy reduction.
	public static int MAX_DIMS=4;
	
	// Used by range-res. Do not touch these variables.
	public static int NUM_DIMS=5;
	public static int a[]={0,1};
	public static double MINIMUMRANGE=1;
	public static int []slicePlans;
	public static double []sgpercs;
	public static boolean IS_SUSPENDED; // Assuming no one would use more than MAX_CLIENTS at once.
	// public static boolean SAVING_DONE = false;

	// Reduction Algorithm
	public static final int REDUCE_AG = -1;//Removed from GUI
	public static final int REDUCE_CG = 1;
	public static final int REDUCE_CGFPC = -1;//Removed from GUI
	public static final int REDUCE_SEER = -1;//Removed from GUI
	public static final int REDUCE_CCSEER = 2;
	public static final int REDUCE_LITESEER = 3;
	
	public static int REDUCTION_ALGORITHM = REDUCE_CG;
	public static int DESIRED_NUM_PLANS = 10;

	//For Collations
	public static final int COLLATE_DEFAULT = 1;
	public static final int COLLATE_SENSITIVE=2;
	public static final int COLLATE_INSENSITIVE=3;
	
	public static int COLLATION_SCHEME=COLLATE_DEFAULT;
	
	public static boolean SAVE_COMPRESSED_PACKET = true;

	//For MySQL Histogram
	public static final int HIST_BUCKETS = 75;
	
	// Produce debugging ouptut
	public static boolean IS_CLIENT_DEBUG = false;
	public static boolean IS_SERVER_DEBUG = false;
	
	// On engines that support XML plans, whether to generate and save XML plan strings during diagram generation
	public static boolean SAVE_XML_INTO_DATABASE = true;
	public static int NUM_GEN_THREADS = 4;
	
	//Required for RS_NN estimation
	public static final int RSNN_ESTIMATION_BUDGET = 100;
	public static final int RSNN_ESTIMATION_START_THRESHOLD = 1000;

//	END USER SETTABLE CONSTANTS 







	// For imposing restrictions on diagram generation.
	public static boolean LIMITED_DIAGRAMS = false;

	// Data Sizes
	public static int SMALL_COLUMN = 32;
	public static int MEDIUM_COLUMN = 64;
	public static int QTNAME_LENGTH = 128;
	public static int LARGE_COLUMN = 512;
	
	//operator or parameter level
	public static boolean OP_LVL=false;

	// Skew for the exponential distribution
	public static double QDIST_SKEW_10 = 2.0;
	public static double QDIST_SKEW_30 = 1.33;
	public static double QDIST_SKEW_100 = 1.083;
	public static double QDIST_SKEW_300 = 1.027;
	public static double QDIST_SKEW_1000 = 1.00808;

	// Enable AbstractPlan feature on compatible engines
	public static boolean  ENABLE_COST_MODEL = true;

	public static boolean IS_DEVELOPER = false;
	
	public static boolean IS_CLASS2_OPT_ENABLED = false;

	public static final int SAVE_BATCH_SIZE = 10000;
	
	// Menu Action
	public static final int NEW_DB_INSTANCE = 1;
	public static final int EDIT_DB_INSTANCE = 2;
	public static final int DELETE_DB_INSTANCE = 3;
	public static final int GET_DIAGRAM_LIST = 4;
	public static final int DELETE_DIAGRAM = 5;
	public static final int RENAME_DIAGRAM = 14;
	public static final int CONNECT_PICASSO = 12;
	public static final int CHECK_SERVER = 6;
	public static final int ABOUT_SERVER = 7;
	public static final int CLEAN_PROCESSES = 8;
	public static final int CLEAN_PICDB = 9;
	public static final int DELETE_PICDB = 10;
	public static final int SHUTDOWN_SERVER = 11;
	public static final int SHOW_PICASSO_SETTINGS = 13;



	// Query Points
	public static final double LOWER_DIAGONAL = 0.05;
	public static final double MID_DIAGONAL = 0.5;
	public static final double HIGHER_DIAGONAL = 0.95;

	public static final int HIGH_QNO = 30000000;

	public static final int	SEGMENT_SIZE = 9;
	public static final int STATUS_REFRESH_SIZE = 9; // changed for 9 to 100 so that the status message is sent every 100 queries instead of 10

	// Diagram Settings
	public static char DIAGRAM_REQUEST_TYPE = 'C';
	public static boolean IS_APPROXIMATE_DIAGRAM = false;
	public static final String COMPILETIME_DIAGRAM = "COMPILETIME";
	public static final String APPROX_COMPILETIME_DIAGRAM = "APPROX_COMPILETIME";	
	public static final String RUNTIME_DIAGRAM = "RUNTIME";
	public static final String UNIFORM_DISTRIBUTION = "UNIFORM";
	public static final String EXPONENTIAL_DISTRIBUTION = "EXPONENTIAL";
	public static final String OPERATORLEVEL = "OPERATOR";
	public static final String SUBOPERATORLEVEL = "SUB-OPERATOR";
	// The term "Sub-Operator" is replaced with "Parameter" in the documentation and in the interface

	public static final String INPUT_QUERY_FOLDER = "QueryTemplates";
	public static final String INPUT_IMAGE_FOLDER = "Images";
	//public static final String SETTINGS_FILE = "DBSettings";
	public static final String DB_SETTINGS_FILE = "PicassoRun/DBConnections";
	public static final String PICASSO_SETTINGS_FILE = "PicassoRun/local_conf";
	public static final String IMAGE_URL = "images/picassologo.jpg";
	public static final String MINI_LOGO = "images/mini_logo.gif";
	public static final String IISC_LOGO = "images/iisc_logo.jpg";
	public static final String ZOOM_IN_IMAGE = "images/zoomin.jpg";
	public static final String ZOOM_OUT_IMAGE = "images/zoomout.jpg";

	// Abstract Plan feature
	public static final String ABSTRACT_PLAN_COMMENT = "--Picasso_Abstract_Plan";
	public static final String SYBASE_ABSTRACT_PLAN_ENDS = "To experiment";

	// Plan Display
	public static final Color BUTTON_COLOR = Color.LIGHT_GRAY;
	public static final Color HIGHLIGHT_COLOR = Color.YELLOW;
	public static final Color IMAGE_BACKGROUND = Color.WHITE;
	public static final Color IMAGE_TEXT_COLOR = Color.BLUE;
	public static final Color PLAN_COLOR = new Color(0xff00de56);
	public static final Color EXEC_COLOR = Color.RED;

	public static final int matchColor[] = {
		0xff000000, 0xffff0000, 0xff0000ff, 0xff00ffff, 0xffff00ff,
		0xffff5000, 0xff6000ff, 0xff70ffff, 0xffff80ff,
		0xffff0000, 0xff0000ff, 0xff00ffff, 0xffff00ff,
		0xffff5000, 0xff6000ff, 0xff70ffff, 0xffff80ff
	};

	public static final int treeColor[] = {
		0xFFFFFF00,	0xFF33AA33, 0xFFFA83FA, 0xFFFFAFAF,
		0xFFFFC800, 0xFFEC8807, 0xFF0088FF, 0xFF00FF00,
		0xFF009F3E, 0xFFAC99FF, 0xfffad300, 0xffa3dc5a,
		0xEE117777, 0xFFDC8699, 0xFFF4C281, 0xffe0fa00,
		0xFF4567EE, 0xFFE395E3,	0xFF95E3E3, 0xFF688799,
		0xfffa697f, 0xFF987654, 0xFF9872AA, 0xFFC3C3A1,
		0xFF4975BF, 0xFF67DE52, 0xFFDAF08B, 0xFF363A5B,
		0xFFEFDFE0, 0xFF777733,	0xFF662222, 0xFF10FF01,
		0xFF7E6753, 0xFFCAF711, 0xFFA33803, 0xFFA59103,
		0xFF6A6034, 0xFFF8DAFF, 0xFF222266, 0xFF337777,
		0xFF916BEC, 0xFFA3004A, 0xFFF5CEA0,	0xFF3cF1A4,
		0xFFD3550E, 0xFF2300FF, 0xFFCF8677, 0xFFBBAA00,
		0xFF00AABB,	0xFFAA00BB, 0xFF66EE33, 0xFF555555,
		0xFF669900, 0xFF6633EE, 0xFF33EE66, 0xFF654321,
		0xFF578577, 0xFF229900, 0xFFbfa3af, 0xFFad9ad1,
		0xFF501020, 0xFFC010C0, 0xFF514122,	0xFFCCCCCC,
		0xFF5CFFC0, 0xFF2CF10E, 0xFF2FF2DF, 0xFFDFFF22,
		0xFF843065, 0xFF475577, 0xFF677387,	0xFF998933,
		0xFF114441, 0xFF333444, 0xFF00478F, 0xFF0011FF,
		0xFF56E12E, 0xFF2EE13E, 0xFF226622, 0xFF3E5612,
		0xFF119911,0xFF00FF00,0xFF977A5B
		/*0xFFFFFF00, 0xffa2fafa, 0xfffa83fa,
		 0xffffafaf, 0xffffc800, 0xfffa703e, 0xff7ffafa,
		 0xff00ff00, 0xff009f3e*/
	};

	public static final int color[]  = {
		0xFFFF0000,	0xFF0000FF, 0xFF991111, 0xffffff00,
		0xffa654da, 0xFFEC8807, 0xFF0088FF, 0xFFF78789,
		0xFF33d8ed, 0xFFAC99FF, 0xFFFF1199, 0xffdb7f6c,
		0xff74fad5, 0xFFAA33AA, 0xFFAA3333, 0xFFEE2345,
		0xff537bc3, 0xffe35be3,	0xff72dbd5, 0xFF123456,
		0xFFAB2358, 0xFF987654, 0xFF9872AA, 0xFFC3C3A1,
		0xFF27529F, 0xFF67DE52, 0xFFDAF08B, 0xFF363A5B,
		0xFFEFDFE0, 0xFF777733,	0xFF662222, 0xFF10FF01,
		0xFF7E6753, 0xFFCAF711, 0xFFA33803, 0xFFA59103,
		0xFF6A6034, 0xFFF8DAFF, 0xFF222266, 0xFF337777,
		0xFF916BEC, 0xFFA3004A, 0xFFF5CEA0,	0xFF3cF1A4,
		0xFFD3550E, 0xFF2300FF, 0xFFCF8677, 0xFFBBAA00,
		0xFF00AABB,	0xFFAA00BB, 0xFF66EE33, 0xFF555555,
		0xFF669900, 0xFF6633EE, 0xFF33EE66, 0xFF654321,
		0xFF578577, 0xFF229900, 0xFF3A1223, 0xFF3A1287,
		0xFF501020, 0xFFC010C0, 0xFF514122,	0xFFCCCCCC,
		0xFF5CFFC0, 0xFF2CF10E, 0xFF2FF2DF, 0xFFDFFF22,
		0xFF843065, 0xFF475577, 0xFF677387,	0xFF998933,
		0xFF114441, 0xFF333444, 0xFF00478F, 0xFF0011FF,
		0xFF56E12E, 0xFF2EE13E, 0xFF226622, 0xFF3E5612,
		0xFF119911, 0xFF00FF00, 0xFF977A5B, 0xFF000000   // The last color black has to be there..
	};


	public static final int DEFAULT_TREE_NODE_COLOR = 0xfff3faaa;


	public static final String SCALE_FONT = /*"Courier New";  "Microsoft Sans Serif";*/"Arial";
	public static final int FONT_SIZE = 22;
	public static final int ONE_D = 1;
	public static final int TWO_D = 2;
	public static final int THREE_D = 3;

	//It was 1200*4 and 150 plans were fitting in, now I have increased it.
	public static final int LEGEND_HEIGHT=1200*4*4;
	public static final int LEGEND_SIZE = 25;
	public static final int LEGEND_MARGIN_Y = 20;
	public static final int LEGEND_MARGIN_X = 2;
	public static final String LEGEND_FONT = "Arial";
	public static final int LEGEND_FONT_SIZE = 14;

	public static final double ASPECT_2D_X = 0.8;
	public static final double ASPECT_2D_Y = 0.8;
	public static final double ASPECT_X = 0.8;
	public static final double ASPECT_Y = 0.8;
	public static final double ASPECT_Z = 0.8;

	public static final int STATUS_LENGTH = 500;

	// Selectivities
	public static final int PICASSO_SELECTIVITY = 0;
	public static final int PREDICATE_SELECTIVITY = 1;
	public static final int PLAN_SELECTIVITY = 2;
	public static final int DATA_SELECTIVITY = 3;

	// Compiled Plan Tree
	public static final int SHOW_BOTH = 0;
	public static final int SHOW_COST = 1;
	public static final int SHOW_CARD = 2;
	public static final int SHOW_NONE = 3;

	// PlanDiff 
	public static final String IS_NODE_SIMILAR = "ISNDSM";
	public static final Color SAME_NODE_COLOR = Color.WHITE; // Temp

	// PlanDiff at OPERATOR Level
	public static final int T_IS_SIMILAR = 0;
	public static final int T_SUB_OP_DIF = 1;
	public static final int T_LEFT_EQ_RIGHT = 3;
	public static final int T_LEFT_SIMILAR = 4;
	public static final int T_RIGHT_SIMILAR = 5;
	public static final int T_NO_CHILD_SIMILAR = 8;
	public static final int T_NP_SIMILAR = 9;
	public static final int T_NP_LEFT_EQ_RIGHT = 10;
	public static final int T_NP_LEFT_SIMILAR = 11;
	public static final int T_NP_RIGHT_SIMILAR = 12;
	public static final int T_NP_NOT_SIMILAR = 15;
	public static final int T_NO_DIFF_DONE = 16;
	public static final int T_EDIT_NODE = 17;

	// The following constants are used only to set the two trees properly in PlanDiff
	public static final int T_NP_LR_SIMILAR = 13;
	public static final int T_NP_RL_SIMILAR = 14;
	public static final int T_LR_SIMILAR = 6;
	public static final int T_RL_SIMILAR = 7;

	public static final int T_NP_LEFT_EQ = 13;
	public static final int T_NP_RIGHT_EQ = 14;
	public static final int T_LEFT_EQ = 6;
	public static final int T_RIGHT_EQ = 7;

	// PlanDiff at SUBOPERATOR (aka PARAMETER) Level 
	public static final int SO_BASE = 20;
	public static final int T_SO_LEFT_EQ_RIGHT = SO_BASE + T_LEFT_EQ_RIGHT;
	public static final int T_SO_LEFT_SIMILAR = SO_BASE + T_LEFT_SIMILAR;
	public static final int T_SO_RIGHT_SIMILAR = SO_BASE + T_RIGHT_SIMILAR;
	public static final int T_SO_NO_CHILD_SIMILAR = SO_BASE + T_NO_CHILD_SIMILAR;
	public static final int T_SO_LR_SIMILAR = SO_BASE + T_LR_SIMILAR;
	public static final int T_SO_RL_SIMILAR = SO_BASE + T_RL_SIMILAR;
//Data for initialization of range frame
	public static int[] prevselected = {0,0,0,0,0};
	public static double[] slice={0,0,0,0,0};
	public static boolean first=true;
	public 	static String[]	params;
	public static boolean IS_PKT_LOADED = false;

	// Data Types
	public static final Vector<String> INT_ALIASES;
	public static final Vector<String> REAL_ALIASES;
	public static final Vector<String> STRING_ALIASES;
	public static final Vector<String> DATE_ALIASES;

	static{
		INT_ALIASES=new Vector<String>();
		REAL_ALIASES=new Vector<String>();
		STRING_ALIASES=new Vector<String>();
		DATE_ALIASES=new Vector<String>();

		INT_ALIASES.add("INTEGER");
		INT_ALIASES.add("BIGINT");
		INT_ALIASES.add("SMALLINT");
		INT_ALIASES.add("INT");
		INT_ALIASES.add("TINYINT");
		INT_ALIASES.add("LONG");
		INT_ALIASES.add("INT2");
		INT_ALIASES.add("INT4");
		INT_ALIASES.add("INT8");

		REAL_ALIASES.add("REAL");
		REAL_ALIASES.add("DECIMAL");
		REAL_ALIASES.add("DOUBLE");
		REAL_ALIASES.add("FLOAT");
		REAL_ALIASES.add("NUMBER");
		REAL_ALIASES.add("NUMERIC");
		REAL_ALIASES.add("FLOAT4");
		REAL_ALIASES.add("FLOAT8");
		REAL_ALIASES.add("SMALLFLOAT");

		STRING_ALIASES.add("CHAR");
		STRING_ALIASES.add("VARCHAR");
		STRING_ALIASES.add("NVARCHAR");
		STRING_ALIASES.add("VARCHAR2");
		STRING_ALIASES.add("NCHAR");
		STRING_ALIASES.add("SYSNAME");
		STRING_ALIASES.add("NVARCHAR");
		STRING_ALIASES.add("LVARCHAR");
		
		DATE_ALIASES.add("DATE");
		DATE_ALIASES.add("DATETIME");
		DATE_ALIASES.add("SMALLDATETIME");
	}
}

/*
 * PicassoCmd.java
 *
 * Created on March 11, 2007, 5:51 PM
 *
 */
// TEST STRINGS on DURGA

// FOR RANGERES: PicassoCmd -R localhost 4444 abhi Default CMDTEST5 Uniform Compilation "C:\Documents and Settings\Abhirama\My Documents\Picasso1.9\QueryTemplates\sqlserver\4.sql" 10 0 40 10 10 50 ---TESTED. WORKS.
// FOR NON RANGERES: PicassoCmd localhost 4444 abhi Default CMDTEST Uniform Compilation "C:\Documents and Settings\Abhirama\My Documents\Picasso1.9\QueryTemplates\sqlserver\4.sql" 10 ---TESTED. WORKS.

// WITH APPROX
// FOR RANGERES: PicassoCmd -R localhost 4444 abhi Default CMDTEST6 Uniform Approximate "C:\Documents and Settings\Abhirama\My Documents\Picasso1.9\QueryTemplates\sqlserver\4.sql" 1 10 1 10 0 40 10 10 50 ---TESTED. WORKS.
// FOR NON RANGERES: PicassoCmd localhost 4444 abhi Default CMDTEST4 Uniform Approximate "C:\Documents and Settings\Abhirama\My Documents\Picasso1.9\QueryTemplates\sqlserver\4.sql" 1 10 1 10 --TESTED. WORKS.

package iisc.dsl.picasso.client;
import iisc.dsl.picasso.client.network.MessageUtil;
import iisc.dsl.picasso.client.util.PicassoSettings;
import iisc.dsl.picasso.client.util.QueryLoader;
import iisc.dsl.picasso.common.ApproxParameters;
import iisc.dsl.picasso.common.ClientPacket;
import iisc.dsl.picasso.common.MessageIds;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.PicassoSettingsManipulator;
import iisc.dsl.picasso.common.ServerPacket;
import iisc.dsl.picasso.common.ds.DBSettings;
import iisc.dsl.picasso.client.panel.PicassoPanel;
import java.awt.Container;
import java.io.File;
import java.util.Vector;

import javax.swing.JOptionPane;


/**
 *
 * @author TR
 */
public class PicassoCmd {
    
    /** Creates a new instance of PicassoCmd */
    public PicassoCmd() {
    }
    
    public static void main(String[] args) {
    	System.out.println("Type \"PicassoCmd\" or \"PicassoCmd -R\" to get help");
    	if(args.length == 0)
    		return;
    	if(args != null && args[0].startsWith("-R"))
    	{
    		if(args.length == 1 || args.length < 12){
    			printUsage(1);
    			System.exit(-1);
    		}
    		new SupportRangeRes(args).Run(args);
    	}
    	else{
    		if(args.length == 9 || (args.length == 12 && args[6].equalsIgnoreCase("Approximate"))){
    			new Support(args).Run();
    		}
    		else{
	    		printUsage(0);
	    		System.exit(-1);
	        }
       	}
    }
    private static void printUsage(int type)
    {
		if(type == 0){
			System.out.println("Basic Format:");
			System.out.println("USAGE : PicassoCmd <ServerName> <Port> <Connection Desc> <OptLevel> <QTID> <QDist> <DiagType> <Filename> <Approx-algo> <Id Error> <Loc Error> <Resolution>");			
		}
		else{
			System.out.println("Dimension specific range and resolution Format:");
			System.out.println("USAGE : PicassoCmd [-R] <ServerName> <Port> <Connection Desc> <OptLevel> <QTID> <QDist> <DiagType> <Filename> <Approx-algo> <Error>  {<Resolution> <startPoint> <endPoint>}");
	        System.out.println("        {<Resolution> <startPoint> <endPoint>} means the three-tuple have to repeated as many times as :varies occurs in the QT");
		}
        System.out.println("\tServer Name    : Name/IP of machine running Picasso server");
        System.out.println("\tPort           : Port no. on which Picasso server is listening");
        System.out.println("\tConnection Desc: Connection Descriptor for database");
        System.out.println("\tOptLevel       : Optimization level of the database");
        System.out.println("\tQTID           : QueryTemplate Name");
        System.out.println("\tQDist          : Distribution of Points (Uniform or Exponential)");
        System.out.println("\tDiagType       : Diagram type (Compilation, Execution or Approximate)");
        System.out.println("\tFilename       : Absolute path of the file containing the QueryTemplate\n");
        System.out.println("\tIf the diagram type is \"Approximate\" the following 3 parameters are needed");
        System.out.println("\t  Approx-Algo	 : Sampling - RS_NN, Grid - GS_PQO (default)");
        System.out.println("\t  Id Error	 	 : % numeric value [10 - default]");
        System.out.println("\t  Loc Error	 	 : % numeric value [10 - default]");
      //  System.out.println("\t  FPC          : 0 - disable(default), 1 - enable [Only for Class II optimizers]");
        if(type == 0){
        	System.out.println("\tResolution     : Resolution of the picture to be generated");
        	System.out.println("\t                 Allowed values are 10, 30, 100, 300 and 1000");
        }else{
        	System.out.println("\tIf you have used -R option");
        	System.out.println("\t\tResolution     : Resolution for the current dimension of the diagram to be generated");
        	System.out.println("\t\t                 Allowed values are 10, 30, 100, 300 and 1000");
        	System.out.println("\t\tStartPoint     : Start point for the current dimension of the diagram to be generated (0-100)");
        	System.out.println("\t\tEndPoint       : End point for the current dimension the diagram to be generated (0-100)");
        }
    }
}

class Support {
    String serverName="localhost";
    int serverPort=4444;
    public String QTID;
    String Desc;
    String OptLevel;
    String Distribution;
    String DType;
    int Resolution;
    boolean isApproximate;
    int samplingMode,sampleSize,fpc;
    double iError,lError;
    File file;
    boolean busy = false;
    ClientPacket cp;
    PicassoCmdPanel pp = new PicassoCmdPanel(this);
    PicassoSettings picSet = new PicassoSettings(PicassoConstants.DB_SETTINGS_FILE);
    //apchg
    //This flag will be set to true when the Client is assigned a non-zero id by the server. 
    // This is to prevent the client from running (i.e. creating plan diagrams) with a clientid of 0.
    static boolean clientHasGotId=false;
    //end apchg
    public Support(String[] args)  {
    	PicassoSettingsManipulator.ReadPicassoConstantsFromFile();
        try {
            serverName = args[0];
            serverPort = Integer.parseInt(args[1]);
            Desc = args[2];
            OptLevel = args[3];
            QTID = args[4];
            Distribution = args[5];
            DType = args[6];
            file = new File(args[7]);
            if (!file.canRead()){
                System.out.println("File "+file.toString()+" does not exist or you do not have enough permissions to access it.");
                System.exit(-1);
            }
            isApproximate = false;
            if(DType.equalsIgnoreCase("Approximate"))
            {
            	isApproximate = true;
            	try {
            		if(args[8].equalsIgnoreCase("Grid"))
            			samplingMode = 1;
            		else if(args[8].equalsIgnoreCase("Sampling"))
            			samplingMode = 0;
            		else {
            			System.out.println("Approximation algorithm "+args[8]+" not supported -- legal values are \"Sampling\" and \"Grid\"");
                        System.exit(-1);
            		}
				} catch (RuntimeException e) {
					samplingMode = 1;
				}
				try {
					iError = Double.parseDouble(args[9]);
					if(iError < 1.0 || iError > 99.0)
					{
						System.out.println("Identity Error must be in the range [1, 99]");
						System.exit(-1);
					}
				} catch (RuntimeException e1) {
					iError = 10;
				}
				try {
					lError = Double.parseDouble(args[10]);
					if(lError < 1.0 || lError > 99.0)
					{
						System.out.println("Location Error must be in the range [1, 99]");
						System.exit(-1);
					}
				} catch (RuntimeException e1) {
					lError = 10;
				}
            /*	try {
					fpc = Integer.parseInt(args[11]);
				} catch (RuntimeException e) {*/
					fpc = 0;
			//	}
				Resolution = Integer.parseInt(args[11]);
            }
            else{
            	Resolution = Integer.parseInt(args[8]);
            }
            switch(Resolution) {
                case 10:
                case 30:
                case 100:
                case 300:
                case 1000:
                    break;
                default:
                    System.out.println("Resolution value "+Resolution+" not supported -- legal values are 10,30,100,300,1000");
                    System.exit(-1);
            }
            cp = new ClientPacket();
            cp.setMessageId(MessageIds.GET_CLIENT_ID);
            cp.setClientId("0");
            cp.fromCommandLine = true;
            MessageUtil.sendMessageToServer(serverName, serverPort, cp, pp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void Run()  {
        DBSettings dbs=null;
        try {
            if((dbs=picSet.get(Desc))==null){
                System.out.println("Descriptor "+Desc+" does not exist.");
                System.exit(-1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        dbs.setOptLevel(OptLevel);
        do
        {
        	cp.setClientId(pp.getClientId());
        } while(clientHasGotId==false);
        cp.setClientId(pp.getClientId());
        
        cp.setMessageId(MessageIds.TIME_TO_GENERATE);
        cp.setDBSettings(dbs);
        cp.setDbType(dbs.getDbVendor());
        cp.setSelecErrorThreshold("10");
        if(Distribution.equalsIgnoreCase(PicassoConstants.UNIFORM_DISTRIBUTION)) 
            cp.getQueryPacket().setDistribution(PicassoConstants.UNIFORM_DISTRIBUTION);
        else if(Distribution.equalsIgnoreCase(PicassoConstants.EXPONENTIAL_DISTRIBUTION)){
            switch(Resolution){
                    case 10:
                    	Distribution =PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_10;
                    	break;
                    case 30:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_30;
                    	break;
                    case 100:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_100;
                    	break;
                    case 300:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION+"_"+PicassoConstants.QDIST_SKEW_300;
                    	break;
                    case 1000:
                   	Distribution =PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_1000;
                    	break;
                    }
            cp.getQueryPacket().setDistribution(Distribution);
        }
        else{
            System.out.println("Unknown Distribution (Use Uniform or Exponential)");
            System.exit(-1);
        }
        if(DType.equalsIgnoreCase("Compilation"))
            cp.getQueryPacket().setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
        else if(DType.equalsIgnoreCase("Execution"))
            cp.getQueryPacket().setExecType(PicassoConstants.RUNTIME_DIAGRAM);
        else if(DType.equalsIgnoreCase("Approximate")){
        	cp.getQueryPacket().setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);
        	ApproxParameters ap = new ApproxParameters(samplingMode);        	
        	ap.setValue("IError", iError );
        	ap.setValue("LError", lError );
        	ap.setValue("UserMode", 0);
        	if ( dbs.getDbVendor().equals("SQL SERVER") || dbs.getDbVendor().equals("SYBASE"))
        	{
        		ap.optClass = MessageIds.Class2;
        		if(fpc == 1)
            		ap.FPCMode = true;
        	}
        	else
        	{
        		ap.optClass = MessageIds.Class1;
        		if(fpc == 1)
        		{
        			System.out.println("For Class I optimizer FPC will be disabled");
        		}
        	}
        	cp.setApproxParameters(ap);
        }
        else{
            System.out.println("Unknown diagram type (Use Compilation or Execution)");
            System.exit(-1);
        }
        cp.getQueryPacket().setOptLevel(OptLevel);
        cp.getQueryPacket().setPlanDiffLevel(PicassoConstants.SUBOPERATORLEVEL);
        cp.getQueryPacket().setQueryName(QTID);
        String queryText = new String();
        queryText = QueryLoader.read(file.getAbsolutePath());
        
        cp.getQueryPacket().setQueryTemplate(queryText);
        String[] parts = queryText.split("(:varies)|(:VARIES)");
        int dimension = parts.length - 1;
        if(dimension==0 && (queryText.indexOf(":varies")!=-1 || queryText.indexOf(":VARIES")!=-1))
            dimension=1;
        cp.getQueryPacket().setDimension(1);
        Vector<Integer> tmp = new Vector<Integer>();
        tmp.add(new Integer(0));
        tmp.add(new Integer(1));
        cp.setDimensions(tmp);
        for(int i = 0; i < dimension; i++)
        	cp.getQueryPacket().setResolution(Resolution,i);
        for(int i = 0; i < dimension; i++)
        	cp.getQueryPacket().setStartPoint(0, i);
        for(int i = 0; i < dimension; i++)
        	cp.getQueryPacket().setEndPoint(1,i);
        cp.getQueryPacket().setSelecThreshold("10");
        MessageUtil.sendMessageToServer(serverName, serverPort, cp, pp);
    }
    public void setBusy(boolean bool){
        busy = bool;
    }
    
    public boolean isBusy(){
        return busy;
    }
    
    public void confirm(long duration) {
        if(isApproximate)
        	cp.setMessageId(MessageIds.GENERATE_APPROX_PICASSO_DIAGRAM);
        else
        	cp.setMessageId(MessageIds.GENERATE_PICASSO_DIAGRAM);
        cp.getQueryPacket().setGenDuration(duration);
        System.err.println("Estimated time to generate for the client is: "+duration);
        MessageUtil.sendMessageToServer(serverName, serverPort, cp,pp);
    }
    
}

class PicassoCmdPanel extends PicassoPanel  {
   	int clientId = 0;
    Support support;
    public PicassoCmdPanel(Support s) {
        this.support = s;
    }
    
    public void processServerMessage() {
        ServerPacket serverPacket = getServerMessage();
        int msgId = serverPacket.messageId;
        switch (msgId ) {
            case MessageIds.GET_CLIENT_ID :
            	Support.clientHasGotId=true;
                clientId = serverPacket.clientId;
                break;
            case MessageIds.ERROR_ID :
                System.out.print("Error in generating diagram "+support.QTID);
                if(serverPacket.status!=null)
                    System.out.println(" "+serverPacket.status);
                System.exit(-2);
                break;
            case MessageIds.PROCESS_QUEUED :
                System.out.println("Server busy; diagram "+support.QTID+" queued for generation");
                System.exit(0);
                break;
            case MessageIds.READ_PICASSO_DIAGRAM :
                System.out.println("Diagram generated");
                System.exit(0);
                break;
            case MessageIds.TIME_TO_GENERATE :
                System.out.println("YES");
                support.confirm(serverPacket.queryPacket.getGenDuration());
                break;
            default:
                break;
        }
    }
    
    public void setStatus(String status) {
        ;
    }
    
    public String getClientId() {
        return ""+clientId;
    }
    
    public Container getParent() {
        return null;
    }
    
/*    public void processErrorMessage(ServerPacket packet) {
        ;
    }*/
    
    public void dispWarningMessage(ServerPacket msg) {
        ;
    }
    public void drawAllDiagrams(ServerPacket msg) {
        ;
    }
}

class SupportRangeRes {
    String serverName="localhost";
    int serverPort=4440;
    public String QTID;
    String Desc;
    String OptLevel;
    String Distribution;
    String DType;
    int Resolution[];
    double startPoint[];
    double endPoint[];
    boolean isApproximate;
    int samplingMode,sampleSize,fpc;
    double iError,lError;
    File file;
    boolean busy = false;
    ClientPacket cp;
    PicassoCmdPanelRR pp = new PicassoCmdPanelRR(this);
    PicassoSettings picSet = new PicassoSettings(PicassoConstants.DB_SETTINGS_FILE);
    //apchg
    //This flag will be set to true when the Client is assigned a non-zero id by the server. 
    // This is to prevent the client from running (i.e. creating plan diagrams) with a clientid of 0.
    static boolean clientHasGotId=false;
    //end apchg
    // -R localhost 4444 sql@pahadi Default batch1_q8_rangeres Uniform Compilation /home/atreyee/Works/Picasso2.0/QueryTemplates/sqlserver/8.sql 10 20.0 60.0 10 30.0 70.0
    public SupportRangeRes(String[] args)  {
    	PicassoSettingsManipulator.ReadPicassoConstantsFromFile();
        try {
            serverName = args[1];
            serverPort = Integer.parseInt(args[2]);
            Desc = args[3];
            OptLevel = args[4];
            QTID = args[5];
            Distribution = args[6];
            DType = args[7];
            isApproximate = false;
            if(DType.equalsIgnoreCase("Approximate"))
            {
            	isApproximate = true;
            	try {
            		if(args[9].equalsIgnoreCase("Grid"))
            			samplingMode = 1;
            		else if(args[9].equalsIgnoreCase("Sampling"))
            			samplingMode = 0;
            		else {
            			System.out.println("Approximation algorithm "+args[9]+" not supported -- legal values are \"Sampling\" and \"Grid\"");
                        System.exit(-1);
            		}
				} catch (RuntimeException e) {
					samplingMode = 1;
				}
				try {
					iError = Double.parseDouble(args[10]);
					if(iError < 1.0 || iError > 99.0)
					{
						System.out.println("Identity Error must be in the range [1, 99]");
						System.exit(-1);
					}
				} catch (RuntimeException e1) {
					iError = 10;
				}
				try {
					lError = Double.parseDouble(args[11]);
					if(lError < 1.0 || lError > 99.0)
					{
						System.out.println("Location Error must be in the range [1, 99]");
						System.exit(-1);
					}
				} catch (RuntimeException e1) {
					lError = 10;
				}
            	/*try {
					fpc = Integer.parseInt(args[12]);
				} catch (RuntimeException e) {
				*/	fpc = 0;
				//} 
            }
            file = new File(args[8]);
            if (!file.canRead()){
                System.out.println("File "+file.toString()+" does not exist or you do not have enough permissions to access it.");
                System.exit(-1);
            }
            cp = new ClientPacket();
            cp.setMessageId(MessageIds.GET_CLIENT_ID);
            cp.fromCommandLine = true;
            cp.setClientId("0");
            MessageUtil.sendMessageToServer(serverName, serverPort, cp, pp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void Run(String []args)  {
        DBSettings dbs=null;
        try {
            if((dbs=picSet.get(Desc))==null){
                System.out.println("Descriptor "+Desc+" does not exist.");
                System.exit(-1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        dbs.setOptLevel(OptLevel);
        do
        {
        	cp.setClientId(pp.getClientId());
        } while(clientHasGotId==false);
        cp.setClientId(pp.getClientId());
        
        cp.setMessageId(MessageIds.TIME_TO_GENERATE);
        cp.setDBSettings(dbs);
        cp.setDbType(dbs.getDbVendor());
        cp.setSelecErrorThreshold("10");
        
        if(DType.equalsIgnoreCase("Compilation"))
            cp.getQueryPacket().setExecType(PicassoConstants.COMPILETIME_DIAGRAM);
        else if(DType.equalsIgnoreCase("Execution"))
            cp.getQueryPacket().setExecType(PicassoConstants.RUNTIME_DIAGRAM);
        else if(DType.equalsIgnoreCase("Approximate")){
        	cp.getQueryPacket().setExecType(PicassoConstants.APPROX_COMPILETIME_DIAGRAM);        	
        	ApproxParameters ap = new ApproxParameters(samplingMode);        	
        	ap.setValue("IError", iError);
        	ap.setValue("LError", lError);
        	ap.setValue("UserMode", 0);
        	if ( dbs.getDbVendor().equals("SQL SERVER") || dbs.getDbVendor().equals("SYBASE"))
        	{
        		ap.optClass = MessageIds.Class2;
        		if(fpc == 1)
            		ap.FPCMode = true;
        	}
        	else
        	{
        		ap.optClass = MessageIds.Class1;
        		if(fpc == 1)
        		{
        			System.out.println("For Class I optimizer FPC will be disabled");
        		}
        	}
        	cp.setApproxParameters(ap);
        }
        else{
            System.out.println("Unknown diagram type (Use Compilation or Execution)");
            System.exit(-1);
        }
        cp.getQueryPacket().setOptLevel(OptLevel);
        cp.getQueryPacket().setPlanDiffLevel(PicassoConstants.SUBOPERATORLEVEL);
        cp.getQueryPacket().setQueryName(QTID);
        // TODO Read QueryTemplate From File and find no of Dimensions
        String queryText = new String();
        queryText = QueryLoader.read(file.getAbsolutePath());
        
        cp.getQueryPacket().setQueryTemplate(queryText);
        String[] parts = queryText.split("(:varies)|(:VARIES)");
        int dimension = parts.length - 1;
        if(dimension==0 && (queryText.indexOf(":varies")!=-1 || queryText.indexOf(":VARIES")!=-1))
            dimension=1;
        int clindex = DType.equalsIgnoreCase("Approximate")?12:9;
        
        if(args.length < clindex + 3 * dimension)
        {
        	System.out.println("You have entered lesser number of {Resolution, StartPoint, EndPoint} triplets than required. The file you specified contains " + dimension  + " number of :varies predicates. Please enter exactly " + dimension + " triplets.");
   			System.exit(-1);
        }
        else if(args.length > clindex + 3 * dimension)
        {
        	System.out.println("You have entered greater number of {Resolution, StartPoint, EndPoint} triplets than required. The file you specified contains " + dimension  + " number of :varies predicates. Please enter exactly " + dimension + " triplets.");
   			System.exit(-1);
        }
        Resolution = new int [dimension];
        startPoint = new double [dimension];
        endPoint = new double [dimension];
        
        cp.getQueryPacket().setDimension(1);
        Vector<Integer> tmp = new Vector<Integer>();
        tmp.add(new Integer(0));
        tmp.add(new Integer(1));
        cp.setDimensions(tmp);
        for(int i = 0; i < dimension; i++)
        {
        	Resolution[i] = Integer.parseInt(args[clindex]);
        	clindex++;
        	cp.getQueryPacket().setResolution(Resolution[i],i);
        	startPoint[i] = Double.parseDouble(args[clindex])/100.0;
        	clindex++;
        	cp.getQueryPacket().setStartPoint(startPoint[i], i);
        	endPoint[i] = Double.parseDouble(args[clindex])/100.0;
        	clindex++;
        	cp.getQueryPacket().setEndPoint(endPoint[i],i);
       		if((endPoint[i]-startPoint[i]) < (PicassoConstants.MINIMUMRANGE/100))
       		{
       			System.out.println("Entered End Point must be greater than Start Point by atleast a value of "+ PicassoConstants.MINIMUMRANGE);
       			System.exit(-1);
       		}
        }
        for(int i = 0; i < dimension; i++)
	        switch(Resolution[i]) {
		        case 10:
		        case 30:
		        case 100:
		        case 300:
		        case 1000:
		            break;
		        default:
		            System.out.println("Resolution value "+Resolution+" not supported -- legal values are 10,30,100,300,1000");
		            System.exit(-1);
	        }
       
        if(Distribution.equalsIgnoreCase(PicassoConstants.UNIFORM_DISTRIBUTION)) 
            cp.getQueryPacket().setDistribution(PicassoConstants.UNIFORM_DISTRIBUTION);
        else if(Distribution.equalsIgnoreCase(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
        {
            for(int i = 0; i < dimension; i++)
            {
            	switch(Resolution[i])
            	{
                    case 10:
                    	Distribution =PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_10;
                    	break;
                    case 30:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_30;
                    	break;
                    case 100:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_100;
                    	break;
                    case 300:
                    	Distribution = PicassoConstants.EXPONENTIAL_DISTRIBUTION+"_"+PicassoConstants.QDIST_SKEW_300;
                    	break;
                    case 1000:
                   	Distribution =PicassoConstants.EXPONENTIAL_DISTRIBUTION +"_"+PicassoConstants.QDIST_SKEW_1000;
                    	break;
                }
            }
            cp.getQueryPacket().setDistribution(Distribution);
        }
        else{
            System.out.println("Unknown Distribution (Use Uniform or Exponential)");
            System.exit(-1);
        }
        cp.getQueryPacket().setSelecThreshold("10");
        MessageUtil.sendMessageToServer(serverName, serverPort, cp, pp);
    }
    public void setBusy(boolean bool){
        busy = bool;
    }
    
    public boolean isBusy(){
        return busy;
    }
    
    public void confirm(long duration) {
        if(isApproximate)
        	cp.setMessageId(MessageIds.GENERATE_APPROX_PICASSO_DIAGRAM);
        else
        	cp.setMessageId(MessageIds.GENERATE_PICASSO_DIAGRAM);
        cp.getQueryPacket().setGenDuration(duration);
        System.err.println("Estimated time to generate for the client is: "+duration);
        MessageUtil.sendMessageToServer(serverName, serverPort, cp,pp);
    }
}
class PicassoCmdPanelRR extends PicassoPanel  {
    int clientId = 0;
    SupportRangeRes support;
    public PicassoCmdPanelRR(SupportRangeRes s) {
        this.support = s;
    }
    
    public void processServerMessage() {
        ServerPacket serverPacket = getServerMessage();
        int msgId = serverPacket.messageId;
        switch (msgId ) {
            case MessageIds.GET_CLIENT_ID :
            	SupportRangeRes.clientHasGotId=true;
                clientId = serverPacket.clientId;
                break;
            case MessageIds.ERROR_ID :
                System.out.print("Error in generating diagram "+support.QTID);
                if(serverPacket.status!=null)
                    System.out.println(" "+serverPacket.status);
                System.exit(-2);
                break;
            case MessageIds.PROCESS_QUEUED :
                System.out.println("Server busy; diagram "+support.QTID+" queued for generation");
                System.exit(0);
                break;
            case MessageIds.READ_PICASSO_DIAGRAM :
                System.out.println("Diagram generated");
                System.exit(0);
                break;
            case MessageIds.TIME_TO_GENERATE :
                System.out.println("YES");
                support.confirm(serverPacket.queryPacket.getGenDuration());
                break;
            default:
                break;
        }
    }
    
    public void setStatus(String status) {
        ;
    }
    
    public String getClientId() {
        return ""+clientId;
    }
    
    public Container getParent() {
        return null;
    }
    
/*    public void processErrorMessage(ServerPacket packet) {
        ;
    }*/
    
    public void dispWarningMessage(ServerPacket msg) {
        ;
    }
    public void drawAllDiagrams(ServerPacket msg) {
        ;
    }
}

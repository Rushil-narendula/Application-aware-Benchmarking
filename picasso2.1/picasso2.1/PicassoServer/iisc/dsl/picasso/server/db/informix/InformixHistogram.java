package iisc.dsl.picasso.server.db.informix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.Histogram;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

public class InformixHistogram extends Histogram {
	
	protected Datatype lowValue,highValue;
	InformixHistogram(Database db, String tabName, String schema, String attribName)
		throws PicassoException
	{
		this.db = db;
		this.tabName = tabName;
		this.schema = schema;
		this.attribName = attribName;
		
		value = new Vector();
		freq = new Vector();
		
		cardinality = getCard();
		/*
		 * We need to get the dType first so that we can create correct
		 * Datatype object for lowValue, highValue and histogram values
		 */
		dType = getDatatype();
			
		if (!(dType.equals("integer") || dType.equals("real") || dType
				.equals("string") || dType.equals("date"))) {
			throw new PicassoException(
			"One of the datatypes of the attributes among the :varies predicates is not handled in Picasso currently.");
		
		}

		readHistogram();
	}
	
	private String getDatatype() throws PicassoException
	{
		String type=null;
		int inttype;
				
		try{
			Statement stmt_type = db.createStatement ();
			
			ResultSet rset_type = stmt_type.executeQuery("select syscolumns.coltype from syscolumns,systables where systables.tabname = '"
						+ tabName + "' and systables.owner= '"
						+ schema.toLowerCase() + "' and  syscolumns.colname='" 
						+ attribName + "' and syscolumns.tabid = systables.tabid");
			
				if (rset_type.next ()){
				//Take only the lowest byte to remove other params such as NOT NULL
				inttype = rset_type.getInt(1) & 0xff;
				
				switch(inttype)
				{
				case 0: type="CHAR"; break;
				case 1: type="SMALLINT"; break;
				case 2: type="INTEGER"; break;
				case 3: type="FLOAT"; break;
				case 4: type="SMALLFLOAT"; break;
				case 5: type="DECIMAL"; break;
				case 6: type="SERIAL"; break;
				case 7: type="DATE"; break;
				case 8: type="MONEY"; break;
				case 9: type="NULL"; break;
				case 10: type="DATETIME"; break;
				case 11: type="BYTE"; break;
				case 12: type="TEXT 	"; break;
				case 13: type="VARCHAR"; break;
				case 14: type="INTERVAL"; break;
				case 15: type="NCHAR"; break;
				case 16: type="NVARCHAR"; break;
				case 17: type="INT8"; break;
				case 18: type="SERIAL8"; break;
				case 19: type="SET"; break;
				case 20: type="MULTISET"; break;
				case 21: type="LIST"; break;
				case 22: type="Unnamed ROW"; break;
				case 40: type="Variable-length opaque type"; break;
				}

				//lowValue = Datatype.makeObject(type, rset_type.getString(2));
				//highValue = Datatype.makeObject(type, rset_type.getString(3));
			}
			rset_type.close();
			stmt_type.close ();
		}catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getDatatype: "+e);
			throw new PicassoException("Cannot get datatype: "+e);
		}
		type=getDatatype(type);

		return type;
	}
	
	private void readHistogram() throws PicassoException
	{
		String colvalue = null;
		//int valcount, maxEndPoint;
		try{
			Statement stmt = db.createStatement ();

/*			ResultSet rset = stmt.executeQuery(
	"select endcat from systables,syscolumns,sysdistrib" +
	" where " +
	" systables.tabid = sysdistrib.tabid " +
	" and systables.tabname =" + schema + "." + tabName + 
	" and syscolumns.tabid = sysdistrib.tabid " +
	" and syscolumns.colno = sysdistrib.colno " +
	" and syscolumns.colname = " + attribName
	);
			while (rset.next ()) {
				System.out.println("Informix Histogram Output:" + rset.getString(1));
			}
	*/		
			/*
			ResultSet rset = stmt.executeQuery(
			"select freq, lastval from t7,systables,syscolumns where" +
			" systables.tabid = t7.tabid " +
			" and systables.tabname ='" +  tabName + 
			"' and syscolumns.colno = t7.colid" +
			" and syscolumns.tabid = t7.tabid" + 
			" and syscolumns.colname = '" + attribName +
			"' and binno <= 10000 order by binno"
			);
			*/
			
			ResultSet rset = stmt.executeQuery(
			"select binsize, boundval, frequency, bintype from SQEXPLAIN_HIST, systables, syscolumns where" +
			" systables.tabid = SQEXPLAIN_HIST.tabid " +
			" and systables.tabname ='" +  tabName + 
			"' and syscolumns.colno = SQEXPLAIN_HIST.colno" +
			" and syscolumns.tabid = SQEXPLAIN_HIST.tabid" + 
			" and syscolumns.colname = '" + attribName +
			"'" + 
			//" and bintype = 'D' " +
			//" order by binno"
			" order by boundval"
			);
			
			/*
			ResultSet rset = stmt.executeQuery
			("select *"
					+ "from temptable where table_name= '" 	+ tabName 
					+ "' and owner= '" 	+ schema 
					+ "' and column_name='" + attribName + "'");
*/
			/*
			if (rset.next ())	{
				maxEndPoint = rset.getInt(1);
			}*/
			while(rset.next())
			{
				
				colvalue = rset.getString(2).trim();
				System.out.print("V:" + rset.getString(2));
				value.addElement(Datatype.makeObject(dType,colvalue));
				
				if(rset.getString(4).equals("D")) //normal data buckets
				{
					System.out.println(" FD:" + rset.getInt(1));
					int t= rset.getInt(1);
					if(t==-1) t=0;
					freq.addElement(new Integer(t));
				}
				else //overflow
				{
		//			System.out.println(rset.getInt(3));
					System.out.println(" FO:" + rset.getInt(3));
					int t= rset.getInt(3);
					freq.addElement(new Integer(t));
				}
			}	
			rset.close();
			stmt.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("readHistogram: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
	}
	
	public String getConstant(double selectivity)
	{
		long leftBoundary = 0 , rightBoundary = 0;
		double step = (selectivity * cardinality);
		int index = 0;
		int tot=0;
		ListIterator it = freq.listIterator();
		if(it.hasNext())
			leftBoundary = ((Integer)it.next()).intValue();
		// This is for frequency histogram where leftBoundary is not zero
		//if(step < leftBoundary)
		//	return ((Datatype)value.get(0)).getStringValue();
		while( it.hasNext()){
			rightBoundary = ((Integer)it.next()).intValue();
			index++;
			tot+=leftBoundary;
			if (step >= tot && step < tot+rightBoundary) {
				double scale = ((double)(step - tot)) / rightBoundary;
				Datatype lbValue = (Datatype)value.get(index-1);
				Datatype rbValue = (Datatype)value.get(index);
				
				return lbValue.interpolate(rbValue,scale);
			}
			leftBoundary = rightBoundary;
		}
		return ((Datatype)value.lastElement()).getStringValue();
	}
	
	private int getCard() throws PicassoException
	{
		int card = 0;
		try{
			Statement stmt = db.createStatement();
			ResultSet rset = stmt.executeQuery
			("select nrows from systables where tabname= '"
					+ tabName + "' and owner= '" + schema.trim().toLowerCase() + "'");
			if (rset.next ())
				card = rset.getInt(1);
			rset.close();
			stmt.close ();
		}catch(SQLException e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("getCardinality: "+e);
			throw new PicassoException("Cannot read histogram: "+e);
		}
		return card;
	}
}

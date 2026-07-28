/*
# Copyright (C) Indian Institute of Science
# Bangalore 560012, INDIA
#
# This program is part of the PICASSO distribution invented at the
# Database Systems Lab, Indian Institute of Science. The use of
# the software is governed by the licensing agreement set up between
# the owner, Indian Institute of Science, and the licensee.
#
# This program is distributed WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# The public URL of the PICASSO project is
# http://dsl.serc.iisc.ernet.in/projects/PICASSO/index.html
#
# For any issues, contact
#       Prof. Jayant R. Haritsa
#       SERC
#       Indian Institute of Science
#       Bangalore 560012, India.
#       Telephone: (+91) 80 2293-2793
#       Fax      : (+91) 80 2360-2648
#       Email: haritsa@dsl.serc.iisc.ernet.in
#       WWW: http://dsl.serc.iisc.ernet.in/~haritsa
*/
package iisc.dsl.picasso.server.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.mysql.MysqlDatabase;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;
import iisc.dsl.picasso.server.db.sybase.SybaseDatabase;
import iisc.dsl.picasso.server.db.db2.DB2Database;
import iisc.dsl.picasso.server.db.informix.InformixDatabase;

public class PicassoParser {
	private final int MAX_NESTING_LEVEL = 10;
	public void parse(Database db,String qTemplate,int dimension,String schemas[],String relations[],String aliases[], String attributes[])
		throws PicassoException
	{
		Vector tables = new Vector();
		Vector alias = new Vector();
		Vector schemaVector = new Vector();
		Vector scope = new Vector();
		Hashtable parentTable = new Hashtable();
		Vector attrScope = new Vector();
		
		//Truncate the query template to contain only the query part, not the plan
		int index = qTemplate.indexOf((PicassoConstants.ABSTRACT_PLAN_COMMENT));
		if (index > 0) 
			qTemplate = qTemplate.substring(0, index);
		
		String tokens[] = tokenize(qTemplate);
		getRelationAndAliasList(tokens,schemaVector,tables,alias,scope,parentTable);
		if(getVaryingPredicates(tokens,attributes, attrScope, parentTable) != dimension)
			throw new PicassoException("Predicates are not found correctly");
		getSchemaAndRelations(db,attributes,schemas,relations,aliases,schemaVector,tables,alias,attrScope,scope,parentTable);
	}

	public void check(Database db,String qTemplate,int dimension,int map[],String schemas[],String relations[],String aliases[],String attributes[])
		throws PicassoException
	{

	}

	private void getRelationAndAliasList(String[] tokens,Vector schemas,Vector tables,Vector aliases, Vector scopeVector, Hashtable parentTable)
		throws PicassoException
	{
		int level=1,depth=1,scope[],endmarker[];
		// nobody in the right mind will fire a query that has more than 10 levels of nesting... :-D
		scope = new int[MAX_NESTING_LEVEL];
		scope[0]=-1;
		endmarker = new int[MAX_NESTING_LEVEL];
		boolean flag[] = new boolean[MAX_NESTING_LEVEL];
		for(int i=0;i<flag.length;i++)
			flag[i]=false;
		
		for(int i=0;i<tokens.length;){
			if(tokens[i].equalsIgnoreCase("(")){
				if(tokens[i+1].equalsIgnoreCase("SELECT")){
					depth++;
					if(scope[depth]<scope[depth-1])
						scope[depth]=scope[depth-1];
					scope[depth]++;
					endmarker[depth] = level;
				}
				level++;
				i++;
			}
			if(i>=tokens.length)
				break;
			if(tokens[i].equalsIgnoreCase(")")){
				level--;
				if(endmarker[depth]==level){
					if(scope[depth]<scope[depth+1])
						scope[depth]=scope[depth+1];
					depth--;
				}
				i++;
			}
			if(i>=tokens.length)
				break;
			if(tokens[i].equalsIgnoreCase("FROM") || flag[depth]){
				flag[depth]=true;
				boolean tmpFlag = true;
                                if(tokens[i].equalsIgnoreCase("FROM"))
                                    i++;
				if(i>=tokens.length)
					break;
				do{
					if(tokens[i].equalsIgnoreCase("WHERE"))
						flag[depth]=false;
					if(tokens[i].equalsIgnoreCase("(")){
						if(tokens[i+1].equalsIgnoreCase("SELECT")){
							depth++;
							if(scope[depth]<scope[depth-1])
								scope[depth]=scope[depth-1];
							scope[depth]++;
							endmarker[depth] = level;
						}
						level++;
						i++;
						tmpFlag=false;
					}
					if(i>=tokens.length)
						break;
					if(tokens[i].equalsIgnoreCase(")")){
						level--;
						if(endmarker[depth]==level){
							if(scope[depth]<scope[depth+1])
								scope[depth]=scope[depth+1];
							flag[depth]=false;
							depth--;
						}
						i++;
					}
					if(i>=tokens.length)
						break;
					/*
					 * setting flag[depth]=false here is dangerous as we may miss relations that comes after the ON conditions
					 */
					if(tokens[i].equalsIgnoreCase("ON"))
						flag[depth]=false;
					if(tokens[i].equalsIgnoreCase("GROUP"))
						flag[depth]=false;
					if(tokens[i].equalsIgnoreCase("ORDER"))
						flag[depth]=false;
					if(flag[depth] && tmpFlag)
						i = addRelationAndAlias(tokens,i,schemas,tables,aliases,scopeVector,parentTable,scope[depth],scope[depth-1]);
					if(i>=tokens.length)
						break;
				}while(flag[depth] && tmpFlag);
				if(i>=tokens.length)
					break;
			}
			else
				i++;
		}
	}
	private int addRelationAndAlias(String tokens[],int i,Vector schemas,Vector tables,Vector aliases,
			Vector scopeVector, Hashtable parentTable, int scope, int parent)
		throws PicassoException
	{
		String ignoreToken = tokens[i];
		if("RIGHT".equals(ignoreToken) || "LEFT".equals(ignoreToken) || "INNER".equals(ignoreToken)|| "OUTER".equals(ignoreToken)
				|| "JOIN".equals(ignoreToken)) {
			i++;
			return i;
		}
		int len = tokens.length;
		String schema, table, alias;
		if(tokens[i].indexOf(".")>=0){
			schema = tokens[i].substring(0,tokens[i].indexOf(".")-1);
			table = tokens[i].substring(tokens[i].indexOf(".")+1,tokens[i].length());
		}
		else{
			schema = "";
			table = tokens[i];
		}
		if(table.endsWith(",")){
			table=table.substring(0,table.length()-1);
			schemas.add(schema);
			tables.add(table);
			aliases.add(table);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return ++i;
		}
		i++;
		if(i==len)
			return i;
		if(tokens[i].equals(",")){
			schemas.add(schema);
			tables.add(table);
			aliases.add(table);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return ++i;
		}
		if(tokens[i].equals(")")){
			schemas.add(schema);
			tables.add(table);
			aliases.add(table);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return i;
		}
		if(tokens[i].equalsIgnoreCase("LEFT") || tokens[i].equalsIgnoreCase("RIGHT")){
			i++;
			if(tokens[i].equalsIgnoreCase("OUTER") && tokens[i+1].equalsIgnoreCase("JOIN"))
				i+=2;
			else
				throw new PicassoException("Parse Error Invalid string "+tokens[i-1]);
			schemas.add(schema);
			tables.add(table);
			aliases.add(table);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return i;
		}
		alias = tokens[i];
		if(tokens[i].equalsIgnoreCase("WHERE") || tokens[i].equalsIgnoreCase("(") ||
				tokens[i].equalsIgnoreCase("ON") || tokens[i].equalsIgnoreCase("GROUP") ||
				tokens[i].equalsIgnoreCase("ORDER"))
		{
			schemas.add(schema);
			tables.add(table);
			aliases.add(table);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return i;
		}
		else
			alias = tokens[i];
		if(alias.endsWith(",")){
			alias=alias.substring(0,alias.length()-1);
			schemas.add(schema);
			tables.add(table);
			aliases.add(alias);
			Integer tmp = new Integer(scope);
			scopeVector.add(tmp);
			parentTable.put(tmp, new Integer(parent));
			return ++i;
		}
		i++;
		if(len==i)
			return i;
		if(tokens[i].equals(","))
			i++;
                alias.length();
		schemas.add(schema);
		tables.add(table);
		aliases.add(alias);
		Integer tmp = new Integer(scope);
		scopeVector.add(tmp);
		parentTable.put(tmp, new Integer(parent));
		return i;
	}
	private int getVaryingPredicates(String[] tokens,String attributes[], Vector attrScope, Hashtable parentTable)
	{
		int level=1,depth=1,scope=0,maxscope=0,endmarker[];
		// nobody in the right mind will fire a query that has more than 10 levels of nesting... :-D
		endmarker = new int[MAX_NESTING_LEVEL];
		int count=0;
		for(int i=0;i<tokens.length-2;i++){
			if(tokens[i].equalsIgnoreCase("(")){
				if(tokens[i+1].equalsIgnoreCase("SELECT")){
					depth++;
					maxscope++;
					scope = maxscope;
					endmarker[depth] = level;
				}
				level++;
				i++;
			}
			if(tokens[i].equalsIgnoreCase(")")){
				level--;
				if(endmarker[depth]==level){
					scope = ((Integer)parentTable.get(new Integer(scope))).intValue();
					depth--;
				}
				i++;
			}
			if(tokens[i].equalsIgnoreCase("WHERE"))
				if(tokens[i+2].toUpperCase().matches(":VARIES")){
					attributes[count++]=tokens[i+1];
					attrScope.add(new Integer(scope));
					i+=2;
				}
			if(tokens[i].equalsIgnoreCase("ON"))
				if(tokens[i+2].toUpperCase().matches(":VARIES")){
					attributes[count++]=tokens[i+1];
					attrScope.add(new Integer(scope));
					i+=2;
				}
			if(tokens[i].equalsIgnoreCase("AND"))
				if(tokens[i+2].toUpperCase().matches(":VARIES")){
					attributes[count++]=tokens[i+1];
					attrScope.add(new Integer(scope));
					i+=2;
				}
			if(tokens[i].equalsIgnoreCase("OR"))
				if(tokens[i+2].toUpperCase().matches(":VARIES")){
					attributes[count++]=tokens[i+1];
					attrScope.add(new Integer(scope));
					i+=2;
				}
		}
		return count;
	}
	private void getSchemaAndRelations(Database db,String attributes[],String schemas[],String relations[],String aliases[],
			Vector schemaVector, Vector tables, Vector alias, Vector attrScope, Vector scope, Hashtable parentTable) throws PicassoException
	{
		String prefix;
		ListIterator itas = attrScope.listIterator();
		for(int i=0;i<attributes.length;i++){
			boolean success = false;
			Integer attributeScope = (Integer)itas.next();
			int dl = attributes[i].indexOf(".");
			if(dl >=0){
				prefix = attributes[i].substring(0,dl);
				attributes[i] = attributes[i].substring(dl+1,attributes[i].length());
				if(alias.contains(prefix)){
					relations[i] = ((String)tables.get(alias.indexOf(prefix)));
					aliases[i] = prefix;
				}
				else if(tables.contains(prefix)){
					relations[i] = ((String)tables.get(tables.indexOf(prefix)));
				}
				else
					throw new PicassoException("prefix "+prefix+" cannot be found in fromlist");
				schemas[i] = getSchemaName(db,relations[i],attributes[i]);
				if(!(db instanceof PostgresDatabase || db instanceof SybaseDatabase || db instanceof MysqlDatabase || db instanceof InformixDatabase )){
					attributes[i] = attributes[i].toUpperCase();
					relations[i] = relations[i].toUpperCase();
					aliases[i] = aliases[i].toUpperCase();
				}
				success = true;
			}
			else{
				Vector dbTables = new Vector();
				Vector dbSchemas = new Vector();					
				getRelationNameFromDatabase(db,attributes[i],dbTables,dbSchemas);
				if(dbTables.size()==0)
					throw new PicassoException("The attribute "+attributes[i]+" is not present in database");
				ListIterator it = tables.listIterator();
				ListIterator its = schemaVector.listIterator();
				ListIterator itsc = scope.listIterator();
				while(it.hasNext() && its.hasNext() && itsc.hasNext()){
					boolean examine = false;
					Integer tableScope = (Integer)itsc.next();
					Integer key = attributeScope;
					String table=((String)it.next());
					String schema=((String)its.next());
					do{
						if(key.equals(tableScope)){
							examine=true;
							break;
						}
						key = (Integer)parentTable.get(key);
					}while(key != null);
					if(!examine)
						continue;
					if(dbTables.contains(table.toUpperCase())){
						int index = dbTables.indexOf(table.toUpperCase());
						String dbSchema = (String)dbSchemas.get(index);
 							if(!(db instanceof PostgresDatabase || db instanceof SybaseDatabase || db instanceof InformixDatabase )){
								relations[i]=table.toUpperCase();
                                aliases[i]=new String(table.toUpperCase());
								attributes[i] = attributes[i].toUpperCase();
							}
							else{
								relations[i]=table;
                                aliases[i]=new String(table);
							}
							schemas[i] = dbSchema;
							success = true;
							break;
					}
				}
			}
			if(!success)
				throw new PicassoException("Cannot find the table corresponding to attribute "+attributes[i]);
		}
	}
	private void getRelationNameFromDatabase(Database db,String attrib,Vector tables,Vector schemas) throws PicassoException
	{
		if(!(db instanceof PostgresDatabase || db instanceof SybaseDatabase || db instanceof InformixDatabase  ))
				attrib = attrib.toUpperCase();
		try{
			Statement stmt = db.createStatement();
			
			ResultSet rset = null;
			if(db instanceof DB2Database)
				rset = stmt.executeQuery ("select TABLE_NAME,OWNER from " + db.getSchema()+
						".picasso_columns where COLUMN_NAME='" +	attrib +"'");
			else
				rset = stmt.executeQuery ("select TABLE_NAME,OWNER from " +
						"picasso_columns where COLUMN_NAME='" +	attrib +"'");
			while(rset.next()){
				tables.add(rset.getString(1).trim().toUpperCase());
				schemas.add(rset.getString(2).trim().toUpperCase());
				System.out.println("For attrib <"+attrib+"> table <"+rset.getString(1)+"> schema <"+rset.getString(2)+"> is found");
			}
			rset.close();
			stmt.close();
		}catch(SQLException e){
			e.printStackTrace();
			throw new PicassoException("Cannot read from picasso_columns view");
		}
	}
	private String getSchemaName(Database db,String table,String attrib) throws PicassoException
	{
		String schema=null;
		if(!(db instanceof PostgresDatabase || db instanceof SybaseDatabase || db instanceof MysqlDatabase || db instanceof InformixDatabase)){
			table = table.toUpperCase();
			attrib = attrib.toUpperCase();
		}
		if(db instanceof MysqlDatabase){
			table = table.toLowerCase();
			attrib = attrib.toUpperCase();
		}

		System.out.println("table<"+table+"> attrib<"+attrib+">");
		try{
			Statement stmt = db.createStatement();
			ResultSet rset = stmt.executeQuery ("select OWNER from " +
					"picasso_columns where COLUMN_NAME='" +	attrib +"' and TABLE_NAME='"+table+"'");
			if(rset.next()){
				schema = rset.getString(1);
			}
			else
				throw new PicassoException("Attribute "+attrib+" on relation "+table+" does not exist");
			rset.close();
			stmt.close();
		}catch(SQLException e){
			e.printStackTrace();
			throw new PicassoException("Cannot read from picasso_columns view");
		}
		return schema;
	}

	private String[] tokenize(String qt)
	{
		String tokens[] = qt.trim().split("\\s+");
		Vector tmp = new Vector();
		for(int i=0;i<tokens.length;i++){
			String token = tokens[i];
			if(token.equals(""))
				continue;
			while(token != null){
				int ci = token.indexOf(",");
				int lbi = token.indexOf("(");
				int rbi = token.indexOf(")");
				if(ci<0 && lbi<0 && rbi<0)
					break;
				if(ci>=0 && (ci<lbi || lbi<0) && (ci<rbi || rbi<0)){
					if(ci>0){
						tmp.add(token.substring(0,ci));
					}
					tmp.add(",");
					if(ci<token.length()-1)
						token = token.substring(ci+1,token.length());
					else
						token = null;
				}
				if(lbi>=0 && (lbi<ci || ci<0) && (lbi<rbi || rbi<0)){
					if(lbi>0){
						tmp.add(token.substring(0,lbi));
					}
					tmp.add("(");
					if(lbi<token.length()-1)
						token = token.substring(lbi+1,token.length());
					else
						token = null;
				}
				if(rbi>=0 && (rbi<ci || ci<0) && (rbi<lbi || lbi<0)){
					if(rbi>0){
						tmp.add(token.substring(0,rbi));
					}
					tmp.add(")");
					if(rbi<token.length()-1)
						token = token.substring(rbi+1,token.length());
					else
						token = null;
				}
			}
			if(token!=null){
				tmp.add(token);
			}
		}
		String tok[] = new String[tmp.size()];
		for(int i=0;i<tmp.size();i++)
			tok[i] = (String)tmp.get(i);
		return tok;
	}
}


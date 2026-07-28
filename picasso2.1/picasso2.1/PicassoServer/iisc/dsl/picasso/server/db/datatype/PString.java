
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

package iisc.dsl.picasso.server.db.datatype;

import iisc.dsl.picasso.common.PicassoConstants;

public class PString extends Datatype {
	String value;
	
	public PString(String v) {
		if(v.startsWith("'"))
			value = v.substring(1,v.length()-1);
		else
			value = v;
	}

	public long getIntValue() {
		return 0;
	}
	
	public double getDoubleValue() {
		return 0.0;
	}
	
	public String getStringValue() {
		return "'"+value+"'";
	}

	public boolean isLessThan(Datatype d) {
		if(value.compareTo(d.getStringValue()) < 0)
			return true;
		return false;
	}
	
	public boolean isEqual(Datatype d)
	{
		if(value.compareTo(d.getStringValue()) == 0)
			return true;
		return false;
	}

	public String interpolate(Datatype d, double scale)
	{
		char rlow, rhigh;
		String dStringValue;
		dStringValue=d.getStringValue();
		if((getMsSqlFlag()==1 && PicassoConstants.COLLATION_SCHEME==PicassoConstants.COLLATE_DEFAULT )
				|| PicassoConstants.COLLATION_SCHEME==PicassoConstants.COLLATE_INSENSITIVE) {
			System.out.println("making it case insensitive");
			this.value = this.value.toUpperCase();
			dStringValue = d.getStringValue().toUpperCase();
		}
		if(this.equals(dStringValue))
			return dStringValue;
		rlow = getSmallestChar(value+dStringValue);
		rhigh = getBiggestChar(value+dStringValue);
		if (rlow <= 'Z' && rhigh >= 'A'){
			  if (rlow > 'A') rlow = 'A';
			  if (rhigh < 'Z') rhigh = 'Z';
		}
		if (rlow <= 'z' && rhigh >= 'a'){
			  if (rlow > 'a') rlow = 'a';
			  if (rhigh < 'z') rhigh = 'z';
		}
		// For testing...
		//rlow = 'A';
		//rhigh = 'z';
		if (rlow <= '9' && rhigh >= '0'){
			  if (rlow > '0') rlow = '0';
			  if (rhigh < '9') rhigh = '9';
		}
		//System.out.print("Rlow<"+rlow+"> Rhigh<"+rhigh+">");
		//	Remove any common prefix of low and high
		int clen=0;
		String tmp = dStringValue;
		tmp = tmp.substring(1,tmp.length()-1);
		for(int i=0;i<value.length();i++){
			if(value.charAt(i)==tmp.charAt(i))
				clen++;
			else
				break;
		}
		String prefix = value.substring(0,clen);
		String lowStr = value.substring(clen,value.length());
		String highStr = tmp.substring(clen,tmp.length());
		//System.out.print("prefix<"+prefix+"> low<"+lowStr+"> high<"+highStr+"> scale<"+scale+">");
		double lVal = toRealValue ( lowStr, rlow, rhigh );
		double hVal = toRealValue ( highStr, rlow, rhigh );
		double newVal = lVal + (hVal - lVal) * scale;
		//System.out.print("lval<"+lVal+"> hVal<"+hVal+"> newVal<"+newVal+">");
		String newStr = toStringValue (newVal, rlow, rhigh);
		//System.out.println("Result<"+prefix+newStr+">");
		return "'"+escapeQuotes(prefix+newStr)+"'";
	}

	public Datatype minus(Datatype d) {
		// TODO Auto-generated method stub
		return new PString("");
	}

	public double divide(Datatype d) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private char getSmallestChar(String str) {
		//System.out.print("String<"+str+">");
		char strArr[] = str.toCharArray();
		char ch='z';
		for(int i=0;i<str.length();i++){
			if(strArr[i]<'0')
				continue;
			if(strArr[i]>'9' && strArr[i]<'A')
				continue;
			if(strArr[i]>'Z' && strArr[i]<'a')
				continue;
			if(strArr[i]>'z')
				continue;
			if(ch>strArr[i])
				ch = strArr[i];
		}
		//System.out.println("Low<"+ch+">");
		return ch;
	}
	
	private char getBiggestChar(String str) {
		//System.out.print("String<"+str+">");
		char strArr[] = str.toCharArray();
		char ch='0';
		for(int i=0;i<str.length();i++){
			if(strArr[i]<'0')
				continue;
			if(strArr[i]>'9' && strArr[i]<'A')
				continue;
			if(strArr[i]>'Z' && strArr[i]<'a')
				continue;
			if(strArr[i]>'z')
				continue;
			if(ch<strArr[i])
				ch = strArr[i];
		}
		//System.out.println("High<"+ch+">");
		return ch;
	}

	private double toRealValue(String str, char low, char high) {
		double num = 0;
		int base = high - low+1;
		int denom = base;
		for(int i=0;i<str.length();i++){
			if(i==10)
				break;
			char ch=str.charAt(i);
			if (ch < low)
				ch = low ; // should we substract 1?
			else if (ch > high)
				ch = high; // should we add 1?
			num = num + (ch - low) / (double)denom;
			denom = denom * base;
		}
		return num;
	}

	private String escapeQuotes(String str)
	{
		return str.replaceAll("'","''");
	}

	private String toStringValue(double val, char low, char high){
		double eps = 0.00000001;
		int base = high - low+1;
		double factor = base;;
		char ch;
		String newStr="";
		int i=0;
		while ( val > eps ){
			ch = (char)(low + (int)( val * factor ));
			if(ch=='\'')
				ch++;
			if((getMsSqlFlag()==1 && PicassoConstants.COLLATION_SCHEME==PicassoConstants.COLLATE_DEFAULT )
					|| PicassoConstants.COLLATION_SCHEME==PicassoConstants.COLLATE_INSENSITIVE) {
				System.out.println("making it case insensitive");
				if(ch >'9' && ch <='<')
					ch='9';
				else if(ch > '<' && ch <'A')
					ch='A';
			}
			newStr += ch;
			val = val - ((int) ( val * factor )) / factor;
			factor = factor * base;
			if(i++==10)
				break;
		}
		return newStr;
	}
}

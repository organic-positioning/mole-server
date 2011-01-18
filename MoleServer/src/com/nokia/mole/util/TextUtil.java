/*
 * 
 * Mole - Mobile Organic Localisation Engine
 * Copyright (C) 2010 Nokia Corporation.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.nokia.mole.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class TextUtil {
	
	static Logger log = Logger.getLogger(TextUtil.class);
	public static final Random random = new Random ();
	
	
    public static String hex[] = {"0", "1", "2",
		"3", "4", "5", "6", "7", "8",
		"9", "a", "b", "c", "d", "e",
		"f"};
    
    public static String createUid (int length) {

	StringBuffer id = new StringBuffer();
	for (int i = 0; i < length; i++) {
	    int j = random.nextInt(16);
	    id.append(hex[j]);
	}
	return new String (id);
    }
			
	public static void tokenizeToList (String input, List<String> list) {
		if (input == null) return;
		if (list == null) return;
		list.clear();
		StringTokenizer tokenizer = new StringTokenizer (input);
		while (tokenizer.hasMoreTokens()) {
			list.add(tokenizer.nextToken());
		}
	}
	
	

   //  return a formatted strings, eliminate extra spaces etc. -- this allows better comparison of two strings
    // that are formatted slightly differently.
    public static String stripLine(String a){
        if (a==null) return null;
        String str = new String();
        StringTokenizer st = new StringTokenizer(a);
        while (st.hasMoreTokens()) str = str + " "+ st.nextToken();
        return str.toLowerCase();
    }

    /**
     * Count the number of white space in the beginning of a line
     */
    public static int spacePrefixSize(String line){
        int spacePrefixSize = 0;
        boolean endSpaces = false;
        int i=0;
        while (i<line.length() && !endSpaces){
            String strchar = line.substring(i,i+1);
           if  (strchar.equals(" ") || strchar.equals("\t")) spacePrefixSize++;
            else endSpaces=true;
           i++;
        }
        return spacePrefixSize;
    }


    public static String[] toToks(String str){
        StringTokenizer st = new StringTokenizer(str);
        String[] toks = new String[st.countTokens()];
        int i=0;
        while (st.hasMoreTokens()) {
            toks[i]=st.nextToken();  i++;
        }
        return toks;
    }


    /**
     *  return (the first) common substring that is common to two strings
     */
    public static String shared(String s1, String s2, int len){
        if (s1.trim().length()==0) return null;
        String[] toks = toToks(s1);
        for (int i=0; i<=(toks.length-len); i++){
            String str = new String();
            for (int j=i; j<i+len;j++){
                str = str + " " + toks[j]; }
                if (s2.toLowerCase().trim().equals(str.toLowerCase().trim()))
                    return str;
                else if (s2.toLowerCase().trim().indexOf(str.toLowerCase().trim())>-1)
                    return str;
        }
        return null;
    }


    /**
     * Find the distance between two lines in text
     */
    public static int lineDist(String doc, String line1, String line2){
        String[] lines = doc.split("\n");
        int i=0, index1=-1;
        while (i<lines.length){
            if (lines[i].trim().equals(line1.trim()))   index1=i;
            else if (lines[i].trim().equals(line2.trim()) && index1>-1) 
                return (i-index1);
            i++;
        }
        return -1;
    }

    
    /**
     * File as string 
     * [See also MixupUtil.fileToText(File file), which return the file's content
     * and subject line as a concat. string.]
     * @param filePath
     * @return
     */
    public static String readFileAsString(String filePath){
    	try{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();}
    	catch(Exception e) { return null; }
    }
    

	/*
	 * How much overlap is there between these strings, starting from the beginning of the string,
	 * as a fraction of the longest string.
	 */
	public static double matchFraction (String a, String b) {
		int c = 0;
		if (a.length() == 0 && b.length() == 0) return 1.;
		if (a.length() == 0 || b.length() == 0) return 0.;
		char aV [] = a.toCharArray();
		char bV [] = b.toCharArray();
		while (c < aV.length && c < bV.length && aV[c] == bV[c]) {
			//System.out.println ("c = "+c+" a="+aV[c]+ " b="+bV[c]);
			c++;
		}
		int d = a.length()>b.length()?a.length():b.length();
		return (double)c/(double)d;
	}
	
	public static String dumpMap (String type, Map<String,String> map) {
		StringBuffer sb = new StringBuffer ();
		if (map != null) {
			for (Map.Entry<String,String> m : map.entrySet()) {
				sb.append(type + " key="+m.getKey()+" value="+m.getValue());
			}
		}
		return new String (sb);
	}

	
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public static String asHex(byte[] buf) {
		return asHex (buf, buf.length);
	}
	
	public static String asHex(byte[] buf, int mostToShow){
		StringBuffer sb = new StringBuffer ();
		for (int i = 0; i < buf.length && i < mostToShow; ++i)
		{
			sb.append (HEX_CHARS[(buf[i] & 0xF0) >>> 4]);
			sb.append (HEX_CHARS[buf[i] & 0x0F]);
		}
		return new String(sb);
	}

		
	public static String asHex2(byte[] buf, int mostToShow)	{
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length && i < mostToShow; ++i)
		{
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}
	
	public static String i2s(int i){
		return (new Integer(i)).toString();
	}

	public static void main (String args[]) {
		String in = "AF02AAFF";
		byte [] buf = hexFromString(in);
		String out = asHex (buf);
		System.out.println(""+buf+"\n"+out);
		
		//System.out.println ("m= "+matchFraction ("32","32-155"));
		
	}
	
	public static byte [] hexFromString (String input) throws IllegalArgumentException {
		char [] chars = input.toCharArray();
		if (chars.length % 2 != 0) throw new IllegalArgumentException("Invalid Hex str "+input); 

		byte [] key = new byte[chars.length / 2];
		//System.out.println("length = "+key.length);
		
		for (int i = 0; i < key.length; i++) {
			//System.out.println("i="+i+" unhex="+unhex(chars[i*2]));
			//System.out.println("i+1="+i+" unhex="+unhex(chars[i*2+1]));
			key[i] = (byte) (unhex(chars[i*2]) << 4 | unhex(chars[i*2+1]));
		}
		return key;
	}
    public static byte unhex(char c) throws IllegalArgumentException {
    	switch (c) {
		case '0': return 0x0;
		case '1': return 0x1;
		case '2': return 0x2;
		case '3': return 0x3;
		
		case '4': return 0x4;
		case '5': return 0x5;
		case '6': return 0x6;
		case '7': return 0x7;

		case '8': return 0x8;
		case '9': return 0x9;
		case 'a': case 'A':	return 0xA;
		case 'b': case 'B':	return 0xB;

		case 'c': case 'C':	return 0xC;
		case 'd': case 'D':	return 0xD;
		case 'e': case 'E':	return 0xE;
		case 'f': case 'F':	return 0xF;
		default:
			throw new IllegalArgumentException("Invalid Hex char "+c); 
    	}
    }	

	public static String listToString(List<String> strList) {
		StringBuffer sb = new StringBuffer ();
		for (int i = 0; i < strList.size(); i++) {
			sb.append(strList.get(i));
			if (i < strList.size() - 1) {
				//sb.append(",");
				sb.append(" ");
			}
		}
		return new String (sb);
	}


	public static String passwordPrompt(String msg) {
		BufferedReader in
		   = new BufferedReader(new InputStreamReader(System.in));
		System.out.println (msg);

		try {
			return in.readLine();
		} catch (IOException e) {
			log.warn("passwordPrompt: "+e.toString());
			//e.printStackTrace();
			return null;
		}
	}

	// TODO get this to work so that password isn't echoed
	// Currently when the process is forked by ant, we don't get the console
	public static String passwordPromptConsole(String msg) {
		java.io.Console cons = System.console();

		System.out.println (msg);
		
		if (cons == null) {
			System.out.println ("Null console");
			return null;
		}
		
		char[] passwd;
		 if (cons != null &&
		     (passwd = cons.readPassword("[%s]", "Password:")) != null) {
			 return new String (passwd);
		 }
		return null;
	}

	public static Map<String, String> tokenizeToKV(String line) {
		// TODO not very robust
		//System.out.println ("line "+line);		
		Map<String,String> map = new HashMap<String,String> ();
		String tokens[] = line.split(" ");
		for (int i = 0; i < tokens.length; i++) {
			String kv[] = tokens[i].split("=");
			map.put(kv[0], kv[1]);
			//System.out.println ("k "+kv[0]+ " v "+kv[1]);
		}
		return map;
	}
	
}

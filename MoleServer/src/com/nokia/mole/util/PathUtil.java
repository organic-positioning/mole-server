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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.nokia.mole.MoleServer;
import com.nokia.mole.RequestState;

public class PathUtil {
	static Logger log = Logger.getLogger(PathUtil.class);

    public static SimpleDateFormat dateFormat = null;

	public static final String ROOT_PATH =
		MoleServer.getProperty("root", ".");
	public static final String WEB_ROOT_PATH =
		MoleServer.getProperty("web_root", "/var/www");

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static String getPathname (String filename) {
		// note that this does not check if the file exists
		
		// TODO this is probably broken in the windows case.  Ug.
		
		if (filename.charAt(0) == FILE_SEPARATOR.charAt(0)) {
			// absolute path
			return filename;
		}
		return new String (ROOT_PATH+FILE_SEPARATOR+filename);
	}
	
	public static void loadKV (File file, Map<String,String> map) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException ex) {
			log.warn("Could not open file: "+file+" "+ex);
			return;
		}

		try {

			boolean ready = true;
			while (ready) {
				String line = reader.readLine();

				if (line != null) {
					if (line.length() > 1 && !line.startsWith("#")) {
						String tokens[] = line.split(" ");
						if (tokens.length >= 2) {

							String key = new String (tokens[0]);
							key = key.toLowerCase();
							if (key != null) {
								
								// make the value be rest of the line
								StringBuffer vb = new StringBuffer ();
								for (int i = 1; i < tokens.length; i++) {
									vb.append (tokens[i]);
									if (i != tokens.length -1) {
										vb.append(" ");
									}
								}
								String value = new String(vb);
								if (map.containsKey(key)) {
									log.error("Duplicate keys="+key+" in file="+file);
								}
								map.put(key, value);
							}       

						} else {
							log.warn("Could not tokenize "+file+" line: "+line);
						}
					}

				} else {
					ready = false;
				}
			}
			reader.close();
		} catch (IOException ex) {
			log.warn("Problem reading file "+file+" "+ex);
			ex.printStackTrace();
		}

	}

	public static void loadDir(File dir, List<File> fileList) {
		if (!dir.isDirectory()) {
			log.warn("Cannot load non-directory: "+dir);
			return;
		}
		File[] listOfFiles = dir.listFiles();

		for (File file : listOfFiles) {
			if (!file.isDirectory() &&
				!file.getName().startsWith(".")) {	
			fileList.add(file);
			}
		}

	}

	public static void loadFile(File file, List<String> lines) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException ex) {
			log.warn("Could not open file: "+file+" "+ex);
			return;
		}

		try {

			boolean ready = true;
			while (ready) {
				String line = reader.readLine();
				
				if (line != null) {
					lines.add(line);

				} else {
					ready = false;
				}
			}
			reader.close();
		} catch (IOException ex) {
			log.warn("Problem reading file "+file+" "+ex);
			ex.printStackTrace();
		}
	}


    // This is pretty quick and dirty.  Sorry.
	public static void recordFeedback(HttpServletRequest request, RequestState state) { 

		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		}

		try {
			BufferedReader reader = request.getReader();
			String filename = WEB_ROOT_PATH+FILE_SEPARATOR+"feedback"+
				FILE_SEPARATOR+dateFormat.format(new Date()) +"-"+
				TextUtil.createUid (10)+".txt";

			BufferedWriter writer = new BufferedWriter (new FileWriter (filename));

			writer.write ("Client: "+state+"\n");

			final int buffer_size = 1024;
			char buffer [] = new char [buffer_size];

			int amount = reader.read(buffer,0,buffer_size);
			log.debug ("read A " + amount);
			while (amount > 0) {
				writer.write (buffer, 0, amount);
				amount = reader.read(buffer,0,buffer_size);
				log.debug ("read B " + amount);
			}

			writer.close ();
			log.debug ("done " + amount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/*
     void save () {
             log.debug("Saving feeds config file");
             System.err.println("Saving feeds config file");
             BufferedWriter writer;
             try {
                     writer = new BufferedWriter(new FileWriter(filename));
                     writer.write("This file is automatically regenerated");
                     for (RSSFeeder rssFeeder : feeders) {
                             writer.write(rssFeeder.toFeedFile());
                             writer.newLine();
                     }
                     writer.close();
             } catch (FileNotFoundException ex) {
                     System.err.println("Could not save feeds config file: "+FEEDS_CONFIG+" "+ex);
             } catch (IOException ex) {
                     System.err.println("Problem saving feeds config file: "+ex);
                     ex.printStackTrace();
             }

     }
	 */

	
}

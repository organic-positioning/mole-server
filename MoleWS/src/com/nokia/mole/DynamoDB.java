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

package com.nokia.mole;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


public class DynamoDB implements DB {
	
	public static String DB_URL = MoleWS.getProperty("mole.dsn", "jdbc:mysql://localhost:3306/mole");
	public static String DB_USER = MoleWS.getProperty("mole.db_user","moleuser");
	public static String DB_PASS = MoleWS.getProperty("mole.db_pw","molepw");	
	
	static Logger log = Logger.getLogger(DynamoDB.class);

	Connection connection;
	
	//Map<String,AreaDesc> bssid2area_list = new HashMap<String,AreaDesc> ();
	//Map<String,Integer> location2id = new HashMap<String,Integer> ();
	
	//PreparedStatement loc_query_stmt;
	//PreparedStatement bssid_to_loc_query_stmt;
	//PreparedStatement loc_insert_stmt;
	//PreparedStatement bind_insert_stmt;
	//PreparedStatement ap_reading_insert_stmt;

	public boolean bind(Bind bind) {
		// TODO Auto-generated method stub
		return false;
	}

	public List<LocationProbability> query(Query query) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean remove(Remove remove) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public DynamoDB () {
	    log.info ("Starting DB "+DB_URL+ " user="+DB_USER);
		connect ();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	log.info("DB shutdown");
            	try {
            		// note that this does not actually flush what is in memory
					connection.close();
				} catch (SQLException e) {
					log.warn("Problem closing db connection: "+e);
					e.printStackTrace();
				}
            }
        });;
	}

	private boolean connect () {
		// TODO use jetty session pooling
		// http://docs.codehaus.org/display/JETTY/Session+Clustering+with+a+Database
		// http://dev.mysql.com/tech-resources/articles/connection_pooling_with_connectorj.html
	    //String url = DB_URL + "?wait_timeout=2764800&interactive_timeout=2764800";
	    String url = DB_URL + "?autoReconnect=true";
	    log.info("db url "+url);
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			log.warn ("exception "+e1);
			return false;
		}
		try {
		    connection = DriverManager.getConnection (url, DB_USER, DB_PASS);
			if (connection.isClosed()) {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			log.warn ("exception "+e);
			return false;
		}
		return true;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<Location, List<Scan>> getLocationScans() {
		// TODO Auto-generated method stub
		return null;
	}



}

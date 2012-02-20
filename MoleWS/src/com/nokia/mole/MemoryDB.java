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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;


public class MemoryDB extends AbstractDB implements DB, Serializable {
	
	private static final long serialVersionUID = -6877252405133374978L;
	static Logger log = Logger.getLogger(MemoryDB.class);
	public static String dbFilename = MoleWS.getProperty("moleWS.db_filename", "/var/cache/mole/ws.db");
    
	Map<Location,Fingerprint> loc2fp = new ConcurrentHashMap<Location,Fingerprint>();
	Map<Mac,Set<Location>> mac2loc = new ConcurrentHashMap<Mac,Set<Location>>();
	
	// for testing
	public static boolean recordScans = true;
	Map<Location,List<Scan>> location2scans = new ConcurrentHashMap<Location, List<Scan>>();
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MemoryDB [locations [");
		for (Location loc : loc2fp.keySet()) {
			sb.append(loc+" ");
		}
		sb.append("], macs [");
		for (Mac mac : mac2loc.keySet()) {
			sb.append(mac+" ");
		}
		sb.append("]]");
		return sb.toString();
	}
	
	
	public MemoryDB () {
		log.warn ("MemoryDB dbFilename="+dbFilename+ " recordingScans="+recordScans);
	}
	
    public static DB loadDB () {
    	DB db = null;
    	FileInputStream fis = null;
    	ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(dbFilename);
			ois = new ObjectInputStream(fis);
	    	try {
				db = (MemoryDB)ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			ois.close();
			log.debug("loaded db filename="+dbFilename);
			log.debug("db="+db);
		} catch (IOException e) {
			//e.printStackTrace();
			log.warn("Could not load db filename="+dbFilename);
		}
		if (db == null) {
			db = new MemoryDB();
		}
		return db;
    }
    
    public static void saveDB (DB db) {
    	try {
    		FileOutputStream fos = new FileOutputStream(dbFilename);
    		ObjectOutputStream oos = new ObjectOutputStream(fos);
    		oos.writeObject(db);
    		oos.flush();
    		oos.close();
    		log.debug("saved db filename="+dbFilename);
    		log.debug("db="+db);
    	} catch (IOException e) {
			e.printStackTrace();
		} 
    }

	
    @Override
    void put (Location location, Fingerprint fp) {
    	loc2fp.put(location, fp);
    }
    @Override
    boolean containsKey(Mac mac) {
    	return mac2loc.containsKey(mac);
    }
    @Override
    void put (Mac mac, Set<Location> locations) {
    	mac2loc.put(mac, locations);
    }
    @Override
    Set<Location> get(Mac mac) {
    	return mac2loc.get(mac);
    }
    @Override
    void put(Location location, List<Scan> scans) {
    	location2scans.put(location, scans);
    }
    @Override
    Fingerprint get(Location location) {
    	return loc2fp.get(location);
    }
    @Override
    void remove(Mac mac) {
    	mac2loc.remove(mac);
    }
    @Override
    boolean containsKey(Location location) {
    	return loc2fp.containsKey(location);
    }
    @Override
    void remove(Location location) {
    	loc2fp.remove(location);
    }
    @Override
    void clearLocations() {
    	loc2fp.clear();
    }
    @Override
    void clearMacs() {
    	mac2loc.clear();
    }

	public Map<Location, List<Scan>> getLocationScans() {
		return location2scans;
	}
	
}

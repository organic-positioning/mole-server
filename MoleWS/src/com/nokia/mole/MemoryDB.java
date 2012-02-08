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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;


public class MemoryDB implements DB, Serializable {
	
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
	
	public boolean bind(Bind bind) {
		if (bind == null) {
			log.warn("bind received null bind");
			return false;
		}
		// TODO use *list* of binds to create fingerprint, not just most recent one

		Fingerprint fp = new Fingerprint (bind.scans);
		loc2fp.put(bind.location, fp);
		for (Mac mac : fp.getMacs()) {
			if (! mac2loc.containsKey(mac)) {
				Set<Location> locations = new HashSet<Location>();
				mac2loc.put(mac, locations);
			}
			Set<Location> locations = mac2loc.get(mac);
			if (locations != null) {
				locations.add(bind.location);
			}
		}
		if (recordScans) {
			location2scans.put(bind.location, bind.scans);
		}
		
		return true;
	}

	public List<LocationProbability> query(Query query) {
		List<LocationProbability> locProbabilities = new ArrayList<LocationProbability>();
		try {
			Fingerprint userFp = new Fingerprint (query.scans);
			Set<Location> potentialLocations = new HashSet<Location>();
			for (Mac mac : userFp.getMacs()) {
				Set<Location> macsLocations = mac2loc.get(mac);
				if (macsLocations != null) {
					potentialLocations.addAll(macsLocations);
				}
			}


			for (Location location : potentialLocations) {
				Fingerprint poiFP = loc2fp.get(location);
				if (poiFP != null) {
					double similarity = Fingerprint.similarity(userFp, poiFP);
					log.debug(location+" score="+similarity);
					locProbabilities.add(new LocationProbability(location, similarity));
				}
			}
		} catch (NullPointerException ex) {
			log.warn("NPE parsing query");
		}

		final int MaxLocationsReturnedByQuery = 20;
		Collections.sort(locProbabilities);
		return locProbabilities.subList(0, locProbabilities.size() > MaxLocationsReturnedByQuery ? MaxLocationsReturnedByQuery : locProbabilities.size());
	}

	public boolean remove(Remove remove) {
		if (remove== null) {
			log.warn("remove received null remove");
			return false;
		}
		log.debug("remove "+remove.location+ " start");
		
		int macCount = 0;
		Fingerprint fp = loc2fp.get(remove.location);
		if (fp != null) {
			for (Mac mac : fp.getMacs()) {
				Set<Location> macsLocations = mac2loc.get(mac);
				if (macsLocations != null) {
					macsLocations.remove(remove.location);
					macCount++;
				}
				if (macsLocations.isEmpty()) {
					mac2loc.remove(mac);
				}
			}
		}
		boolean ok = loc2fp.containsKey(remove.location);
		if (ok) {
			loc2fp.remove(remove.location);
		}
		log.debug("remove "+remove.location+ " found="+ok+" macCount="+macCount);
		return ok;
	}
	
	public MemoryDB () {
		log.debug ("MemoryDB dbFilename="+dbFilename+ " recordingScans="+recordScans);
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

	public void clear() {
		loc2fp.clear();
		mac2loc.clear();
		// don't clear location2scans
	}

	public Map<Location, List<Scan>> getLocationScans() {
		return location2scans;
	}
	
}

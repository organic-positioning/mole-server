/*
 * 
 * Mole - Mobile Organic Localisation Engine
 * Copyright (C) 2010-2012 Nokia Corporation.  All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


public abstract class AbstractDB implements DB {
	
	static Logger log = Logger.getLogger(AbstractDB.class);
	
	// for testing
	public static boolean recordScans = true;
	
	public boolean bind(Bind bind) {
		if (bind == null) {
			log.warn("bind received null bind");
			return false;
		}
		// TODO use *list* of binds to create fingerprint, not just most recent one

		Fingerprint fp = new Fingerprint (bind.scans);
		put(bind.location, fp);
		for (Mac mac : fp.getMacs()) {
			
        	// add this location to the entry for these macs
        	// unless it already exists
        	Set<Location> locations = get(mac);
        	if (locations == null) {
        		locations = new HashSet<Location>();
        	}
        	if (!locations.contains(bind.location)) {
        		locations.add(bind.location);
        		put(mac,locations);
        	}
        	
			/*
			if (! containsKey(mac)) {
				Set<Location> locations = new HashSet<Location>();
				put(mac, locations);
			}
			Set<Location> locations = get(mac);
			if (locations != null) {
				locations.add(bind.location);
			}
			*/
		}
		if (recordScans) {
			put(bind.location, bind.scans);
		}
		
		return true;
	}

	
	public List<LocationProbability> query(Query query) {
		List<LocationProbability> locProbabilities = new ArrayList<LocationProbability>();
		try {
			Fingerprint userFp = new Fingerprint (query.scans);
			Set<Location> potentialLocations = new HashSet<Location>();
			for (Mac mac : userFp.getMacs()) {
				Set<Location> macsLocations = get(mac);
				if (macsLocations != null) {
					potentialLocations.addAll(macsLocations);
				}
			}


			for (Location location : potentialLocations) {
				Fingerprint poiFP = get(location);
				if (poiFP != null) {
					double similarity = Fingerprint.similarity(userFp, poiFP);
					log.debug(location+" score="+similarity);
					if (similarity > 0) {
						locProbabilities.add(new LocationProbability(location, similarity));
					}
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
		Fingerprint fp = get(remove.location);
		if (fp != null) {
			for (Mac mac : fp.getMacs()) {
				Set<Location> macsLocations = get(mac);
				if (macsLocations != null) {
					macsLocations.remove(remove.location);
					macCount++;
				}
				if (macsLocations.isEmpty()) {
					remove(mac);
				}
			}
		}
		boolean ok = containsKey(remove.location);
		if (ok) {
			remove(remove.location);
		}
		log.debug("remove "+remove.location+ " found="+ok+" macCount="+macCount);
		return ok;
	}
	
	public void clear() {
		clearLocations();
		clearMacs();
		// don't clear location2scans
	}
	
	abstract void clearMacs();

	abstract void clearLocations();

	abstract void remove(Location location);

	abstract boolean containsKey(Location location);

	abstract void remove(Mac mac);

	abstract void put(Location location, List<Scan> scans);

	abstract Set<Location> get(Mac mac);

	abstract void put(Mac mac, Set<Location> locations);

	abstract boolean containsKey(Mac mac);

	abstract void put(Location location, Fingerprint fp);

	abstract Fingerprint get(Location location);

	
}

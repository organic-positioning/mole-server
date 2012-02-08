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

package com.nokia.mole.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import com.nokia.mole.Bind;
import com.nokia.mole.DB;
import com.nokia.mole.Location;
import com.nokia.mole.LocationProbability;
import com.nokia.mole.MemoryDB;
import com.nokia.mole.Query;
import com.nokia.mole.Scan;
import com.nokia.mole.Source;


public class AlgEval {
	
	/*
	static {
    	String log4j_conf = System.getProperty ("moleWS.log4cfg", "config/log-console.cfg");
    	String main_conf = System.getProperty ("moleWS.config", "config/moleWS.cfg");
    	System.out.println ("alg-eval main_conf="+main_conf + " logj_conf="+log4j_conf);
    	
		PropertyConfigurator.configure(log4j_conf);
    	log = Logger.getLogger(AlgEval.class);
	}
	*/
	static Logger log = Logger.getLogger(AlgEval.class);
	
	Map<Location,List<Scan>> location2scans;
	List<Location> locations;
	Source source;
	DB db;
	Random random;
	
	AlgEval() {
		random = new Random();
    	db = MemoryDB.loadDB();
    	log.debug(db);
		location2scans = db.getLocationScans();
    	source = new Source ("key1", "secret1", "device1", "version1");
    	setLocationList();

	}
	

	public static void main(String[] args) {
    	//if (args.length == 0) {
    		//System.err.println("Database name missing from arguments");
    		//System.exit(-1);
    	//}
    	
		System.out.println ("Starting algorithm evaluation");
		//System.out.println ("Starting algorithm evaluation, db="+args[0]);
    	MemoryDB.dbFilename = "/home/ledlie/projects/mole/server/MoleWS/office.db";
    	MemoryDB.recordScans = false; // otherwise we change the scans as we are looking at them
    	
    	AlgEval eval = new AlgEval();
    	eval.varyUserScans();
    }

    
    void varyUserScans() {
    	
    	//final int fpScanCountTestSizes[] = {1, 2, 4, 8, 16, 32, 64};
    	final int fpScanCountTestSizes[] = {1, 2, 4, 8};
    	//final int maxUserScanCount = 10;
    	final int maxUserScanCount = 5;
    	
    	for (int userScanCount = 1; userScanCount <= maxUserScanCount; userScanCount++) {
			//System.out.println("start user "+userScanCount);
    		for (int fpScanCount : fpScanCountTestSizes) {
    			int accuracySum = 0;
    			int accuracyCount = 0;
    	    	db.clear();
    			for (int testIndex = 0; testIndex < 100; testIndex++) {
    				for (Location location : location2scans.keySet()) {
    		    		//System.err.println("\nbinding location "+location);
    					List<Scan> fpScans = getRandomScans(location, fpScanCount);
    					Bind bind = new Bind (location, fpScans, source);
    					db.bind(bind);
    				}
		    		
    				Location testLocation = getRandomLocation();
    				//System.err.println("\ntesting location "+testLocation);
    				List<Scan> userScans = getRandomScans(testLocation, userScanCount);
    				Query query = new Query(userScans, source);
    				List<LocationProbability> result = db.query(query);
    				if (result.size() == 0 || !result.get(0).location.equals(testLocation)) {
    					// miss
    				} else {
    					accuracySum++;
    				}
    				accuracyCount++;
    				//System.out.println("score="+result.get(0).probability);
    			}
    			double accuracyAvg = (double)accuracySum / (double)accuracyCount;
    			System.out.println("user "+userScanCount+ " fp "+fpScanCount+ " acc " + accuracyAvg);
    		}
    	}
    }
    
    List<Scan> getRandomScans(Location location, int scanCount) {
    	List<Scan> allScans = location2scans.get(location);

    	//for (int i = 0; i < allScans.size(); i++) {
    		//System.out.println(location+" "+i+" scan="+allScans.get(i));
    	//}
    	
    	List<Scan> scanSubset = new ArrayList<Scan>();
    	int max = allScans.size() - scanCount + 1;
    	if (max <= 0) {
    		System.err.println("bad scan count for "+location+" size="+allScans.size()+" sC="+scanCount);
    		System.exit(-1);
    	}
    	int index = random.nextInt(max);

    	for (int i = index; i < index+scanCount; i++) {
    		scanSubset.add(allScans.get(i));
    	}
    	
    	//List<Scan> scanSubset = allScans.subList(index, index+scanCount);
    	//System.out.println("scanSubset size="+scanSubset.size()+ " from="+index+" to="+(index+scanCount));
    	//for (int i = 0; i < scanSubset.size(); i++) {
    	//	System.out.println(location+" scan="+scanSubset.get(i));
    	//}

    	
    	return scanSubset;
	}

	Location getRandomLocation() {
    	int index = random.nextInt(locations.size());
    	return locations.get(index);
    }

    void setLocationList() {
    	locations = new ArrayList<Location>();
    	locations.addAll(location2scans.keySet());
	}

    
}

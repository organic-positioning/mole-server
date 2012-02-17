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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nokia.mole.Bind;
import com.nokia.mole.DB;
import com.nokia.mole.DynamoDB;
import com.nokia.mole.Location;
import com.nokia.mole.LocationProbability;
import com.nokia.mole.Mac;
import com.nokia.mole.MemoryDB;
import com.nokia.mole.Query;
import com.nokia.mole.Reading;
import com.nokia.mole.Remove;
import com.nokia.mole.Scan;
import com.nokia.mole.Source;


public class DBTest {
	static Logger log;
	
	static {
		PropertyConfigurator.configure("config/log-console.cfg");
    	log = Logger.getLogger(DBTest.class);
	}
	
    public static void main(String[] args) {
    	System.out.println ("Starting internal API test");
    	MemoryDB.dbFilename = "/tmp/moleWS.db";
    	
    	
    	//DB db = MemoryDB.loadDB();
    	//log.debug(db);
    	//DB db = new MemoryDB();
    	
    	DB db = new DynamoDB();
    	
    	Source source = new Source ("key1", "secret1", "device1", "version1");
    	
    	Location loc1 =    new Location("container1", "poi1");
    	Location loc1dup = new Location("container1", "poi1");
    	if (! loc1.equals(loc1dup)) {
    		log.fatal("loc1 != loc1dup");
    	}
    	Location loc2a = new Location("container2", "poiA");
    	Location loc2b = new Location("container2", "poiB");
    	
    	Remove remove1 = new Remove(loc1dup, source);
    	if (db.remove(remove1)) {
    		log.fatal("cannot remove from empty db");
    	}
    	
    	if (!loc1.equals(loc1)) {
    		log.fatal("loc equals problem");
    	}
    	if (!loc1.equals(loc1dup)) {
    		log.fatal("loc1dup equals problem");
    	}
    	if (loc1.equals(loc2a)) {
    		log.fatal("loc2a equals problem");
    	}
    	if (loc2a.equals(loc2b)) {
    		log.fatal("loc2a-b equals problem");
    	}
    	

    	
    	/////////////////////////////////////////////////////////////
    	
    	Mac mac1 = new Mac ("00:00:00:00:00:01");
    	Mac mac2 = new Mac ("00:00:00:00:00:02");
    	Mac mac3 = new Mac ("00:00:00:00:00:03");
    	
    	Reading reading1a = new Reading (mac1, "ssid1", 100, 70);
    	Reading reading1b = new Reading (mac2, "ssid2", 100, 75);
    	Reading reading1c = new Reading (mac3, "ssid3", 100, 80);
    	List<Reading> readings1 = new ArrayList<Reading>();
    	readings1.add(reading1a);
    	readings1.add(reading1b);
    	readings1.add(reading1c);
    	
    	Reading reading2a = new Reading (mac2, "ssid2", 100, 65);
    	Reading reading2b = new Reading (mac1, "ssid1", 100, 64);
    	Reading reading2c = new Reading (mac3, "ssid3", 100, 66);
    	List<Reading> readings2 = new ArrayList<Reading>();
    	readings2.add(reading2a);
    	readings2.add(reading2b);
    	readings2.add(reading2c);

    	Reading reading3a = new Reading (mac1, "ssid1", 100, 72);
    	Reading reading3b = new Reading (mac2, "ssid2", 100, 74);
    	Reading reading3c = new Reading (mac3, "ssid3", 100, 81);
    	List<Reading> readings3 = new ArrayList<Reading>();
    	readings3.add(reading3a);
    	readings3.add(reading3b);
    	readings3.add(reading3c);

    	Reading reading4a = new Reading (mac1, "ssid1", 100, 40);
    	Reading reading4b = new Reading (mac2, "ssid2", 100, 40);
    	//Reading reading4c = new Reading (mac3, "ssid3", 100, 40);
    	List<Reading> readings4 = new ArrayList<Reading>();
    	readings4.add(reading4a);
    	readings4.add(reading4b);
    	//readings4.add(reading4c);

    	Scan scan1 = new Scan(readings1);
    	Scan scan2 = new Scan(readings2);
    	Scan scan3 = new Scan(readings3);
    	Scan scan4 = new Scan(readings4);
    	
    	List<Scan> scanList1 = new ArrayList<Scan>();
    	scanList1.add(scan1);
    	scanList1.add(scan2);
    	
    	List<Scan> scanList1subset = new ArrayList<Scan>();
    	scanList1subset.add(scan1);
    	
    	List<Scan> scanList2partialmatch = new ArrayList<Scan>();
    	scanList2partialmatch.add(scan2);
    	scanList2partialmatch.add(scan3);

    	List<Scan> scanList3nomatch = new ArrayList<Scan>();
    	scanList3nomatch.add(scan4);

    	/*
    	List<Scan> scans1 = new ArrayList();
    	scans1.add(scanList1);
    	List<Scan> scans1subset = new Scans(scanList1subset);
    	List<Scan> scans2partialMatch = new Scans(scanList2partialmatch);
    	List<Scan> scans3noMatch = new Scans(scanList3nomatch);
    	*/
    	
    	/////////////////////////////////////////////////////////////
    	
    	Query query1 = new Query(scanList1, source);
    	List<LocationProbability> q1result = db.query(query1);
    	if (q1result.size() != 0) {
    		log.fatal("empty db returned result");	
    	}
    	
    	/////////////////////////////////////////////////////////////
    	// add single space and remove it
    	
    	Bind bind1 = new Bind(loc1, scanList1, source);
    	db.bind(null);
    	db.bind(bind1);
    	q1result = db.query(query1);
    	if (q1result.size() != 1) {
    		log.fatal("should return single result");	
    	}
    	if (! q1result.get(0).location.equals(loc1)) {
    		log.fatal("should return single loc1");
    	}
    	if (! db.remove(remove1)) {
    		log.fatal("should be able to remove location1");
    	}
    	
    	/////////////////////////////////////////////////////////////
    	// add two spaces, do two queries that should match, query that should not match, and remove both

    	db.bind(bind1);
    	Bind bind2 = new Bind(loc2a, scanList2partialmatch, source);
    	db.bind(bind2);
    	Query query2 = new Query(scanList1subset, source);
    	List<LocationProbability> q2result = db.query(query2);
    	if (q2result.size() != 2) {
    		log.fatal("should return two results");
    	}
    	if (! q2result.get(0).location.equals(loc1)) {
    		log.fatal("should return loc1 "+q2result.get(0));
    	}
    	if (! q2result.get(1).location.equals(loc2a)) {
    		log.fatal("should return loc1 "+q2result.get(1));
    	}
    	log.warn("\n\nstarting query3");
    	Query query3 = new Query(scanList3nomatch, source);
    	List<LocationProbability> q3result = db.query(query3);
    	log.debug("q3result" + q3result);
    	if (q3result.size() != 0) {
    		log.fatal("should return no results");
    	}

    	log.debug("\n\ndb " + db);
    	MemoryDB.saveDB(db);
    	//DB db3 = MemoryDB.loadDB();
    	//log.debug("\n\ndb3" + db3);
    	
    	Remove remove2 = new Remove(loc2a, source);
    	if (! db.remove(remove2)) {
    		log.fatal("should be able to remove location2a");
    	}
    	if (! db.remove(remove1)) {
    		log.fatal("should be able to remove location1");
    	}
    	if (db.remove(remove1)) {
    		log.fatal("should not be able to remove location1 again");
    	}
    	if (db.remove(remove2)) {
    		log.fatal("should not be able to remove location2a again");
    	}
    	
    	/////////////////////////////////////////////////////////////
    	// try same tests again but with different db instance
    	
    	DB db2 = MemoryDB.loadDB();
    	log.debug("\n\ndb2" + db2);
    	if (! db2.remove(remove2)) {
    		log.fatal("should be able to remove location2a");
    	}
    	if (! db2.remove(remove1)) {
    		log.fatal("should be able to remove location1");
    	}
    	if (db2.remove(remove1)) {
    		log.fatal("should not be able to remove location1 again");
    	}
    	if (db2.remove(remove2)) {
    		log.fatal("should not be able to remove location2a again");
    	}
    	log.debug("\n\ndb2" + db2);
    	MemoryDB.saveDB(db2);

    	/////////////////////////////////////////////////////////////
    	// dump out what client-sent json should look like for parsing to work
    	
    	GsonBuilder gBuilder = new GsonBuilder();
		gBuilder.registerTypeAdapter(Date.class, new Mac().new MacDeserializer()).create();
		Gson gson = gBuilder.create();

    	String bindJson = gson.toJson(bind1);
    	log.debug("bind as json "+bindJson);
		
    	String removeJson = gson.toJson(remove1);
    	log.debug("remove as json "+removeJson);

    	String queryJson = gson.toJson(query1);
    	log.debug("query as json "+queryJson);

    	
    }
}

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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


public class DynamoDB implements DB {
	
	static Logger log = Logger.getLogger(DynamoDB.class);
	AmazonDynamoDBClient client;
	Gson gson;
	static String loc2fpTable = "loc2fp";
	static String locAttr = "Location";
	static String fpAttr = "Fingerprint";
	
	static String mac2locsTable = "mac2locs";
	static String macAttr = "Mac";
	static String locsAttr = "Locations";
	Type locationListType = new TypeToken<ArrayList<Location>>() {}.getType();

	
	public DynamoDB() {
		log.info ("Starting Dynamo DB");
		GsonBuilder gBuilder = new GsonBuilder();
		gson = gBuilder.create();
		String key = MoleWS.getProperty("moleWS.aws_key");
		String secret = MoleWS.getProperty("moleWS.aws_secret");
		if (key == null || secret == null) {
			log.fatal("aws_key or aws_secret not found");
		}
		AWSCredentials credentials = new BasicAWSCredentials(key, secret);
		client = new AmazonDynamoDBClient(credentials);
	}

	public boolean bind(Bind bind) {
		if (bind == null) {
			log.warn("bind received null bind");
			return false;
		}
		
		// record the new fingerprint for this location
		Fingerprint fp = new Fingerprint (bind.scans);
		put(bind.location, fp);
        
        // update the locations for these macs
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
        }
		return true;
	}

	private void put(Location location, Fingerprint fp) {
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        String locationJson = gson.toJson(location);
        String fpJson = gson.toJson(fp);
		item.put(locAttr, new AttributeValue().withS(locationJson));
		item.put(fpAttr, new AttributeValue().withS(fpJson));
		PutItemRequest itemRequest = new PutItemRequest().withTableName(loc2fpTable).withItem(item);
        client.putItem(itemRequest);
	}

	private void put(Mac mac, Set<Location> locations) {
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		String macJson = gson.toJson(mac);
		String locationsJson = gson.toJson(locations, locationListType);
		item.put(macAttr, new AttributeValue().withS(macJson));
		item.put(locsAttr, new AttributeValue().withS(locationsJson));
		PutItemRequest itemRequest = new PutItemRequest().withTableName(mac2locsTable).withItem(item);
        client.putItem(itemRequest);
	}

	private Set<Location> get(Mac mac) {
		//Set<Location> locations = new HashSet<Location>();
		String macJson = gson.toJson(mac);
		GetItemRequest getItemRequest = new GetItemRequest()
		.withTableName(mac2locsTable)
		.withKey(new Key()
		.withHashKeyElement(new AttributeValue().withS(macJson)))
		.withAttributesToGet(locsAttr);

		GetItemResult result = client.getItem(getItemRequest);
		Map<String,AttributeValue> results = result.getItem(); 
		// we receive null if it's not found
		if (results != null && results.containsKey(locsAttr)) {
			AttributeValue value = results.get(locsAttr);
			String locationsJson = value.getS();
			assert (locationsJson.length() > 2);
			List<Location> locationList = gson.fromJson(locationsJson, locationListType);
			Set<Location> locations = new HashSet<Location> (locationList);
			return locations;
		}
		return null;
	}

	public List<LocationProbability> query(Query query) {
		List<LocationProbability> locProb = new ArrayList<LocationProbability>();
		return locProb;
	}
	
	public boolean remove(Remove remove) {
		// TODO Auto-generated method stub
		return false;
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

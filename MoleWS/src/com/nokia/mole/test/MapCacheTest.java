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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nokia.mole.Mac;
import com.nokia.nrcc.MapCache;
import com.nokia.nrcc.TransientMapCache;


public class MapCacheTest {
	
	static Logger log;
	static {
		PropertyConfigurator.configure("config/log-console.cfg");
    	log = Logger.getLogger(MapCacheTest.class);
	}
	
	MapCache<String,Mac> cache = new TransientMapCache<String,Mac>(10); 
	
	MapCacheTest() {
	}
	
	public static void main(String[] args) {
		System.out.println ("Starting map cache tester");

		MapCacheTest test = new MapCacheTest();
		test.run();
		System.out.println ("Finished");
    }

	public void run() {
		String k1 = new String("key1");
		String k1s = new String("key1");
		Mac v1 = new Mac("mac1");
		if (cache.containsKey(k1)) {
			log.fatal("should not have contained");
		}

		cache.put(k1, v1);
		if(!cache.containsKey(k1)) {
			log.fatal("should have contained k1");
		}
		if(!cache.containsKey(k1s)) {
			log.fatal("should have contained k1s");
		}
		
		try {
			Thread.sleep(11000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(cache.containsKey(k1s)) {
			log.fatal("should not have contained k1s");
		}
		if(cache.containsKey(k1)) {
			log.fatal("should not have contained k1");
		}

		// test remove
		String k2 = new String("key2");
		String k2s = new String("key2");
		Mac v2 = new Mac("mac2");
		cache.put(k2, v2);
		if (!cache.containsKey(k2s)) {
			log.fatal("should have contained k2s");
		}
		cache.remove(k2s);
		if (cache.containsKey(k2s)) {
			log.fatal("should not have contained k2s");
		}
	}
}

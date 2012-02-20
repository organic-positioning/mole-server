/*
* 
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

package com.nokia.nrcc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class TransientMapCache<K extends Comparable<K>,V> implements MapCache<K,V> {

	static Logger log = Logger.getLogger(TransientMapCache.class);

	final int EXPIRE_SECONDS;
	//Map<K,TransientValue> map = new ConcurrentHashMap<K, TransientValue>();
	Map<K,TransientValue> map = new HashMap<K, TransientValue>();

	public TransientMapCache(int expirationSeconds) {
		EXPIRE_SECONDS = expirationSeconds;
	}
	
	class TransientValue {
		
		Date expiration;
		V value;
		
		public TransientValue(V _value) {
			this.value = _value;
			Date current = new Date(); 
			expiration = new Date(current.getTime()+ (EXPIRE_SECONDS*1000));
		}
	}

	public V get(K key) {
		if (map.containsKey(key)) {
			Date current = new Date();
			TransientValue tV = map.get(key); 
			if (tV.expiration.after(current)) {
				return tV.value;
			} else {
				// expire
				map.remove(key);
			}
		}
		return null;
	}
	
	public void put(K key, V value) {
		TransientValue newValue = new TransientValue(value);
		if (map.containsKey(key)) {
			map.remove(key);
		}
		map.put(key, newValue);
	}
	
	public boolean containsKey(K key) {
		if (get(key) != null) {
			return true;
		}
		return false;
	}
	
	public void remove(K key) {
		if (map.containsKey(key)) {
			map.remove(key);
		}
	}
	
}

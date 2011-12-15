/*
* 
* Mole - Mobile Organic Localisation Engine
* Copyright (C) 2010-2011 Nokia Corporation.  All rights reserved.
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

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class Mac implements Comparable<Mac>, Serializable {

	private static final long serialVersionUID = -4116886323107241859L;
	static Logger log = Logger.getLogger(Mac.class);
	public static final Pattern MacPattern = Pattern.compile("^[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]$");
		
	final public String bssid;
		
	public Mac() {
		bssid = null;
	}
	
    public Mac(String rawMac) {
		bssid = rawMac;
	}

	@Override
	public String toString() {
		return bssid;
	}

	public class MacDeserializer implements JsonDeserializer<Mac> {
    	public Mac deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context)
    	throws JsonParseException {
    		
    		// canonicalize MAC addresses
    		String rawMac = json.getAsString();
    		//log.debug("parsing mac "+rawMac);
    		
    		rawMac = rawMac.toLowerCase();
    		if (rawMac.length() > 20) {
    			rawMac = rawMac.substring(0, 20);
    		}
    		rawMac.replace('-', ':');
    		
    		Matcher matcher = MacPattern.matcher (rawMac);
    		if (!matcher.find()) {
    			log.warn ("invalid mac "+rawMac);
    			return null;
    		}
    		return new Mac (rawMac);
    	}
    }
    
	/*
	public boolean equals(Object o){
		Mac m2 = (Mac)o;
		return this.bssid.equals(m2.bssid);
    }
	*/

	public int compareTo(Mac m2) {
		return this.bssid.compareTo(m2.bssid);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bssid == null) ? 0 : bssid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mac other = (Mac) obj;
		if (bssid == null) {
			if (other.bssid != null)
				return false;
		} else if (!bssid.equals(other.bssid))
			return false;
		return true;
	}

	
	
}

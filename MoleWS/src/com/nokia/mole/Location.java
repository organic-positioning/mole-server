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

import java.io.Serializable;

import org.apache.log4j.Logger;

public class Location implements Comparable<Location>, Serializable {

	private static final long serialVersionUID = 10712440295113782L;
	static Logger log = Logger.getLogger(Location.class);

	public final String container;
	public final String poi;
	
    public Location() {
    	container = null;
    	poi = null;
    }

	public String toString() {
		return "Location [container=" + container + ", poi=" + poi + "]";
	}

	public Location(String container, String poi) {
		this.container = container;
		this.poi = poi;
	}

/*
	public boolean equals(Object o){
    	Location l2 = (Location)o;
    	if (this.container.equals(l2.container) && this.poi.equals(l2.poi)) return true;
    	else return false;	
    }
*/
	/*
	public boolean equals(Object o){
    	Location l2 = (Location)o;
    	if (this.container.equals(l2.container) && this.poi.equals(l2.poi)) {
    		log.debug("loc equals true "+l2);
    		return true;
    	}
		log.debug("loc equals false container "+container+" "+l2.container+" "+this.container.equals(l2.container)+ " poi "+poi+" "+l2.poi+" "+this.poi.equals(l2.poi));
		return false;
	}
*/
	
	
	public int compareTo(Location l2) {
    	int containerCompare = this.container.compareTo(l2.container);
    	if (containerCompare != 0) {
    		return containerCompare;
    	}
    	return this.poi.compareTo(l2.poi);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((container == null) ? 0 : container.hashCode());
		result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
		Location other = (Location) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (poi == null) {
			if (other.poi != null)
				return false;
		} else if (!poi.equals(other.poi))
			return false;
		return true;
	}

}

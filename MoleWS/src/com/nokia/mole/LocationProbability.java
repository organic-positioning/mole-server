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

import org.apache.log4j.Logger;

public class LocationProbability implements Comparable<LocationProbability> {

	static Logger log = Logger.getLogger(LocationProbability.class);
	static final long serialVersionUID = 0L;

	final public Location location;
	final public double probability;
	
	// used by gson
	protected LocationProbability() {
		location = null;
		probability = 0.0;
	}

	public LocationProbability(Location location, double probability) {
		this.location = location;
		this.probability = probability;
	}

	@Override
	// Sorts by ascending probability
	public int compareTo(LocationProbability lp) {
		if (probability > lp.probability) {
			return -1;
		} else if (probability < lp.probability) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "LocationProbability [location=" + location + ", probability="
				+ probability + "]";
	}


}

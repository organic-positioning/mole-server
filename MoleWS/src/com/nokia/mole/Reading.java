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

public class Reading implements Serializable {

	private static final long serialVersionUID = -9216965767005900221L;
	static Logger log = Logger.getLogger(Reading.class);
	
	final Mac bssid;
	final String ssid;
	final int frequency;
	// TODO write a deserializer for this to make sure level is valid
	final int level;

	// for gson
	protected Reading () {
		bssid = null;
		ssid = null;
		frequency = 0;
		level = 0;
	}
	
	public Reading(Mac bssid, String ssid, int frequency, int level) {
		this.bssid = bssid;
		this.ssid = ssid;
		this.frequency = frequency;
		this.level = level;
	}


	public String toString () {
		return "["+bssid+","+ssid+","+frequency+","+level+"]";
	}
}

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

package com.nokia.mole.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;


public class AP_Scan implements Serializable {

	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(AP_Scan.class);
	
	final public Date stamp;
	final public List<AP_Reading> readings;
	
	// for gson
	private AP_Scan () {
		readings = null;
		stamp = null;
	}
	
	public AP_Scan (final List<AP_Reading> _readings, final Date _stamp) {
		readings = _readings;
		stamp = _stamp;
		//log.debug ("new scan reading count "+readings.size());
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer ();
		for (AP_Reading reading : readings) {
			buffer.append(reading);
		}
		return new String (buffer);
	}
	
	public static AP_Scan copy(AP_Scan scan) {
		List<AP_Reading> _readings = new ArrayList<AP_Reading>();
		for (AP_Reading _reading : scan.readings) {
			_readings.add(AP_Reading.copy(_reading));
		}
		return new AP_Scan (_readings, new Date (scan.stamp.getTime()));
	}

	public static AP_Scan newRandomInstance(Date start_stamp) {
		//Date stamp = new Date (start_stamp.getTime() + 6000000 + (long)(Bind.random.nextInt(900000)));
		Date stamp = new Date (start_stamp.getTime() + (long)(BindwExtras.random.nextInt(900000)));
		
		int reading_count = BindwExtras.random.nextInt(10);
		//log.debug("reading_count="+reading_count);
		
		
		List<AP_Reading> readings = new ArrayList<AP_Reading>();
		for (int i = 0; i < reading_count; i++) {
			readings.add(AP_Reading.newRandomInstance ());
		}
		return new AP_Scan (readings, stamp);
	}

	public int get_reading_count() {
		return readings.size();
	}
	
}

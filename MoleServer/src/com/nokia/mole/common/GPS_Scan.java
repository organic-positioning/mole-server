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


public class GPS_Scan implements Serializable {

	private static final long serialVersionUID = 1L;
	
	final public Date stamp;
	final public List<GPS_Reading> readings;
	
	public GPS_Scan (List<GPS_Reading> _readings, Date _stamp) {
		readings = _readings;
		stamp = _stamp;
	}

	public static GPS_Scan copy(GPS_Scan scan) {
		List<GPS_Reading> _readings = new ArrayList<GPS_Reading>();
		for (GPS_Reading _reading : _readings) {
			_readings.add(GPS_Reading.copy(_reading));
		}
		return new GPS_Scan (_readings, new Date (scan.stamp.getTime()));
	}
	
}

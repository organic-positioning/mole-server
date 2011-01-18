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

public class GPS_Reading implements Serializable {

	private static final long serialVersionUID = 1L;

	final public double lat;
	final public double lon;
	final public double alt;

	public GPS_Reading(final double lat, final double lon, final double alt) {
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
	}

	public static GPS_Reading copy(GPS_Reading gps) {
		return new GPS_Reading (gps.lat, gps.lon, gps.alt);
	}

}

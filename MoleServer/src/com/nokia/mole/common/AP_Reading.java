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

import org.eclipse.jetty.util.log.Log;


public class AP_Reading implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// TODO check input data so that it matches with the table
	private final String bssid;
	private final String ssid;
	private final int frequency;
	// listed positively in table;
	private final int level;
	
	public String getBssid() {
		if (bssid.length() <= 20) {
			return bssid;
		}
		return bssid.substring(0, 20);
	}

	public String getSsid() {
		if (ssid.length() <= 20) {
			return ssid;
		}
		return ssid.substring(0, 20);
	}

	public int getFrequency() {
		return frequency;
	}

	public int getLevel() {
		return level;
	}

	// for gson
	private AP_Reading () {
		bssid = null;
		ssid = null;
		frequency = 0;
		level = 0;
	}
	
	public String toString () {
		return "["+bssid+","+ssid+","+frequency+","+level+"]";
	}
	
	public AP_Reading(final String _bssid, final String _ssid, final int _frequency, final int _level) {
		//if (_bssid.matches("^\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}$") {

		if (true) {

			this.bssid = _bssid.toUpperCase();
		} else {
			Log.debug("rejecting bssid "+_bssid);
			this.bssid = null;
		}

		this.ssid = _ssid.substring(0, 20);
		this.frequency = _frequency;
		
		// store level positively
		if (_level < 0 && _level > -100) {
			this.level = -1 * _level;
		} else if (_level < 100) {
			this.level = _level;
		} else {
			// bad value
			this.level = 0;
		}
	}

	public static AP_Reading copy(final AP_Reading r) {
		return new AP_Reading (new String (r.bssid), new String (r.ssid), r.frequency, r.level);
	}

	public static AP_Reading newRandomInstance() {
		return new AP_Reading (BindwExtras.newRandomString(1), BindwExtras.newRandomString(1), BindwExtras.random.nextInt(14), BindwExtras.random.nextInt(30));
	}


}

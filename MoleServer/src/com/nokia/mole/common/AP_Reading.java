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
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import org.eclipse.jetty.util.log.Log;

import org.apache.log4j.Logger;

public class AP_Reading implements Serializable {

	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(AP_Reading.class);
	
	// TODO check input data so that it matches with the table
	private final String bssid;
	private String pBssid;
	private final String ssid;
	private String pSsid;
	private final int frequency;
	// listed positively in table;
	private final int level;
	private int pLevel;

    public static final Pattern MacPattern = Pattern.compile("^[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]:[0-9a-f][0-9a-f]$");
	
	public String getBssid() {
	    return pBssid;
	}

	public String getSsid() {
		return pSsid;
	}

	public int getFrequency() {
		return frequency;
	}

	public int getLevel() {
		return pLevel;
	}

	// for gson
	private AP_Reading () {
		bssid = null;
		ssid = null;
		frequency = 0;
		level = 0;
	}
	
	public String toString () {
		return "["+pBssid+","+pSsid+","+frequency+","+pLevel+"]";
	}

    public boolean validate () {
	pBssid = bssid.toLowerCase();
	pSsid = ssid;
	if (pSsid.length() > 20) {
	    pSsid = ssid.substring(0, 20);
	}

	// TODO canonicalize MAC addresses

	Matcher matcher = MacPattern.matcher (pBssid);
	if (!matcher.find()) {
	    log.warn ("invalid bssid "+bssid);
	    return false;
	}

	pLevel = level;
	if (pLevel < 0) {
	    pLevel = -1 * pLevel;
	}

	if (pLevel < 20 || pLevel > 100) {
	    log.warn ("rssi out of range" + pBssid + " " + pLevel);
	    // bad value
	    return false;
	}

	return true;
    }
	
	public AP_Reading(final String _bssid, final String _ssid, final int _frequency, final int _level) {
		//if (_bssid.matches("^\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}:\p{XDigit}\p{XDigit}$") {

	    log.debug ("gson calling AP_Reading ctor");

		if (true) {

			this.bssid = _bssid.toLowerCase();
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

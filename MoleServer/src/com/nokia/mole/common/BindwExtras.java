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
import java.util.Random;

import org.apache.log4j.Logger;

//import edu.mit.csail.oil.util.TextUtil;

public class BindwExtras implements Serializable {

	static Logger log = Logger.getLogger(BindwExtras.class);
	static final long serialVersionUID = 0L;

	final public Location location;
	final public Location est_location;
	final public Date bind_stamp;
	final public Date start_stamp;
	final public Date end_stamp;
	final public List<AP_Scan> ap_scans;
	final public List<GSM_Scan> gsm_scans;
	final public List<GPS_Scan> gps_scans;
	final public Cookie cookie;
	final public String device_model;
	final public String wifi_model;
	
	public static Random random = new Random ();

	public String toString () {
		int ap_reading_count = 0;
		for (AP_Scan ap_scan : ap_scans) {
			ap_reading_count += ap_scan.get_reading_count ();
		}
		return new String ("[loc="+location+",scans="+ap_scans.size()+",readings="+ap_reading_count+
				","+start_stamp+","+bind_stamp+","+end_stamp+"]");
	}
	
	public static BindwExtras newBind (final Location location, final Location est_location, final Date bind_stamp, final Date start_stamp, final Date end_stamp, List<AP_Scan> ap_scans, List<GSM_Scan> gsm_scans, List<GPS_Scan> gps_scans, final Cookie cookie, final String device_model, final String wifi_model) {
		
		// TODO add other checks/improvements on location, dates, etc. 
		
		// sanity check the bind
		if (bind_stamp.after(end_stamp)) {
			return null;
		}
		if (bind_stamp.before(start_stamp)) {
			return null;
		}

		// toss readings that are outside of the start and end stamps
		boolean valid_ap_scans = true;
		for (AP_Scan ap_scan : ap_scans) {
			if (ap_scan.stamp.after(end_stamp)) {
				valid_ap_scans = false;
			} else if (ap_scan.stamp.before(start_stamp)) {
				valid_ap_scans = false;
			} else if (ap_scan.readings.size() == 0) {
				valid_ap_scans = false;
			}
		}

		log.debug("valid_ap_scans = "+valid_ap_scans);
		log.debug("ap_scans size = "+ap_scans.size());
		if (valid_ap_scans == false) {
			List<AP_Scan> _ap_scans = new ArrayList<AP_Scan>();
			for (AP_Scan ap_scan : ap_scans) {
				if ((ap_scan.stamp.before(end_stamp) || ap_scan.stamp.equals(end_stamp)) &&
					(ap_scan.stamp.after(start_stamp) || ap_scan.stamp.equals(start_stamp)) &&
					(ap_scan.readings.size() > 0)) {
					_ap_scans.add(ap_scan);
				}
			}
			ap_scans = _ap_scans;
			if (ap_scans.size() == 0) {
				log.warn("no valid scans in bind for location "+location);
				return null;
			}
			log.debug("ap_scans size = "+ap_scans.size());
		}
		
		return new BindwExtras (location, est_location, bind_stamp, start_stamp, end_stamp, ap_scans, gsm_scans, gps_scans, cookie, device_model, wifi_model);
	}
	
	private BindwExtras(final Location location, final Location est_location, final Date bind_stamp, final Date start_stamp, final Date end_stamp, final List<AP_Scan> ap_scans, final List<GSM_Scan> gsm_scans, final List<GPS_Scan> gps_scans, final Cookie cookie, final String device_model, final String wifi_model) {
		super();
		this.location = location;
		this.est_location = est_location;
		this.bind_stamp = bind_stamp;
		this.start_stamp = start_stamp;
		// for testing integrity constraint
		//this.start_stamp = new Date (end_stamp.getTime() + 5000);
		this.end_stamp = end_stamp;
		this.ap_scans = ap_scans;
		this.gsm_scans = gsm_scans;
		this.gps_scans = gps_scans;
		this.cookie = cookie;
		this.device_model = device_model;
		this.wifi_model = wifi_model;
	}

	public static String newRandomString (int length) {
		byte buffer [] = new byte[length];
		random.nextBytes(buffer);
		//return new String (TextUtil.asHex(buffer));
		return new String ("foobar");
	}
		
	public static String newRandomString () {
		return newRandomString (4);
	}
	
	public static BindwExtras newRandomInstance () {

		for (int b = 0; b < 10; b++) {
			Date now = new Date ();
			Date start_time = new Date (now.getTime() - (long)(random.nextInt(900000)));
			Date end_time = new Date (now.getTime() + (long)(random.nextInt(900000))); 

			int ap_scan_count = random.nextInt (100);
			List<AP_Scan> _ap_scans = new ArrayList<AP_Scan>();
			for (int i = 0; i < ap_scan_count; i++) {
				//log.debug ("adding scan "+i);
				_ap_scans.add(AP_Scan.newRandomInstance(start_time));
			}

			int gsm_scan_count = random.nextInt (100);
			List<GSM_Scan> _gsm_scans = new ArrayList<GSM_Scan>();
			//for (int i = 0; i < gsm_scan_count; i++) {
			//_gsm_scans.add(GSM_Scan.newRandomInstance(start_time));
			//}

			int gps_scan_count = random.nextInt (100);
			List<GPS_Scan> _gps_scans = new ArrayList<GPS_Scan>();
			//for (int i = 0; i < gps_scan_count; i++) {
			//_gps_scans.add(GPS_Scan.newRandomInstance(start_time));
			//}

			// +/- 15 minutes from now

			BindwExtras bind = newBind (Location.newRandomInstance (),
					Location.newRandomInstance (),
					now,
					start_time,
					end_time,
					_ap_scans,
					_gsm_scans,
					_gps_scans,
					Cookie.newRandomInstance (),
					newRandomString (),
					newRandomString ());

			if (bind == null) {
				log.warn ("invalid random bind");
			} else {
				return bind;
			}

		}
		log.error("failed to create a valid random bind");
		return null;
	}

	// Deep copy of bind
	// Don't need to validate here as well.
	public static BindwExtras copy (BindwExtras _bind) {
		
		List<AP_Scan> _ap_scans = new ArrayList<AP_Scan>();
		for (AP_Scan _scan : _bind.ap_scans) {
			_ap_scans.add(AP_Scan.copy(_scan));
		}

		List<GSM_Scan> _gsm_scans = new ArrayList<GSM_Scan>();
		for (GSM_Scan _scan : _bind.gsm_scans) {
			_gsm_scans.add(GSM_Scan.copy(_scan));
		}

		List<GPS_Scan> _gps_scans = new ArrayList<GPS_Scan>();
		for (GPS_Scan _scan : _bind.gps_scans) {
			_gps_scans.add(GPS_Scan.copy(_scan));
		}
		
		return new BindwExtras (Location.copy (_bind.location),
				Location.copy(_bind.est_location),
				new Date (_bind.bind_stamp.getTime()),
				new Date (_bind.start_stamp.getTime()),
				new Date (_bind.end_stamp.getTime()),
				_ap_scans,
				_gsm_scans,
				_gps_scans,
				Cookie.copy(_bind.cookie),
				new String (_bind.device_model),
				new String (_bind.wifi_model));
	}
}

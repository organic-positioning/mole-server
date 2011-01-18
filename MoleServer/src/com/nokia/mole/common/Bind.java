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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.nokia.mole.RequestState;

public class Bind implements Serializable {

	static Logger log = Logger.getLogger(Bind.class);
	static final long serialVersionUID = 0L;


	public int version;
	public Cookie cookie;
	public Cookie session;
	final public Location location;
	final public Location est_location;
	final public Date bind_stamp;
	public Date start_stamp;
	public Date end_stamp;
	public String device_model;
	public String wifi_model;
	public String client_ip;
	public int client_port;
        public String tags;
        public String description;
	
	final public List<AP_Scan> ap_scans;
	transient boolean valid = false;

	/*
	public String toString () {
		return new String 
		("[version="+version+",cookie="+cookie+",loc="+location+",est_loc="+est_location+
		",bind_stamp="+bind_stamp+",start_stamp="+start_stamp+",end_stamp="+end_stamp+
		",device="+device_model+",wifi="+wifi_model+",ap_scans="+ap_scans+"]");
		
	}
	 */

	public boolean isValid () {
		return valid;
	}
	
	public boolean validate () {
		valid = false;
		long now = new Date ().getTime();
		final long DAY_IN_MSEC = 1000 * 60 * 60 * 24;
		final long TWO_HOURS_IN_MSEC = 1000 * 60 * 60 * 2;
		final long THIRTY_MIN_IN_MSEC = 1000 * 60 * 30;

		if (!location.isValid()) {
			log.warn("invalid location="+location.full_space_name);
		} else if (start_stamp.after(bind_stamp)) {
			log.warn("start_stamp="+start_stamp+ " before bind_stamp="+bind_stamp);

		} else if ( Math.abs (end_stamp.getTime() - bind_stamp.getTime()) > THIRTY_MIN_IN_MSEC) {
		    log.warn("end_stamp="+end_stamp+" before bind_stamp="+bind_stamp
			     + "=abs "+Math.abs (end_stamp.getTime() - bind_stamp.getTime())
			     + " 30min="+THIRTY_MIN_IN_MSEC);

		} else if (start_stamp.getTime() + DAY_IN_MSEC < now) {
			log.warn("start_stamp too old "+start_stamp);
		} else if (end_stamp.getTime() - DAY_IN_MSEC > now) {
			log.warn("end_stamp too far in future "+end_stamp);
		} else if (end_stamp.getTime() - start_stamp.getTime() > TWO_HOURS_IN_MSEC) {
			log.warn("start and end too far apart start="+start_stamp+" end_stamp="+end_stamp);
		} else if (client_ip == null || client_ip.length() == 0 || client_ip.length() > 15) {
			log.warn("invalid bind client_ip="+client_ip);
		} else if (client_port <= 0 || client_port > 65536) {
			log.warn("invalid bind client_port="+client_port);
		} else {
			valid = true;
		}
		
		final int tags_max_length = 80;
		final int desc_max_length = 80;
		final int dev_max_length = 40;
		final int wifi_max_length = 20;
		if (tags.length() > tags_max_length) {
		    tags = tags.substring (0, tags_max_length);
		}
		if (description.length() > desc_max_length) {
		     description = description.substring (0, desc_max_length);
		}
		if (device_model.length() > dev_max_length) {
		     device_model = device_model.substring (0, dev_max_length);
		}
		if (wifi_model.length() > wifi_max_length) {
		     wifi_model = wifi_model.substring (0, wifi_max_length);
		}

		for (Iterator<AP_Scan> i = ap_scans.iterator(); i.hasNext();) {
			AP_Scan scan = i.next();
			if (scan.stamp.before(start_stamp)) {
				i.remove();
				log.debug("tossing too old scan");
			} else if (scan.stamp.after(end_stamp)) {
				i.remove();
				log.debug("tossing too recent scan");
			}
		}

		if (ap_scans.size() == 0) {
			log.debug("no valid scans");
			valid = false;
		}
		
		return valid;
	}
	
	public String toString () {
		StringBuffer scans = new StringBuffer ();
		//for (AP_Scan ap_scan : ap_scans) {
		//scans.append("[scan="+ap_scan.toString()+"] ");
	//}
		//return new String ("[version="+version+",ap_scans="+scans+"]");

		return new String 
		("[version="+version+",cookie="+cookie+",loc="+location+",est_loc="+est_location+
		",bind_stamp="+bind_stamp+",start_stamp="+start_stamp+",end_stamp="+end_stamp+
		",device="+device_model+",wifi="+wifi_model+
		",ip="+client_ip+":"+client_port+",ap_scans="+scans+
		 ",tags="+tags+",desc="+description+"]");
		
	}

	
	// used by gson
	private Bind() {
		version = 0;
		cookie = null;
		location = null;
		est_location = null;
		bind_stamp = null;
		start_stamp = null;
		end_stamp = null;
		device_model = null;
		wifi_model = null;
		ap_scans = null;
		client_ip = null;
		client_port = 0;
		tags = null;
		description = null;
	}
	

	/*
	// used by gson
	private BasicBind() {
		version = null;
		ap_scans = null;
	}
	*/
	/*
	public BasicBind(String version, List<AP_Scan> ap_scans) {
		this.version = version;
		this.ap_scans = ap_scans;
	}
	*/

	/*
	public Bind(int version, Cookie cookie, Location location, Location est_location, Date bind_stamp, 
			Date start_stamp, Date end_stamp,
		    String device_model, String wifi_model, List<AP_Scan> ap_scans, String tags, String description) {
		//log.debug("basic bind ctor");
		this.version = version;
		this.cookie = cookie;
		this.location = location;
		this.est_location = est_location;
		this.bind_stamp = bind_stamp;
		this.start_stamp = start_stamp;
		this.end_stamp = end_stamp;
		this.device_model = device_model;
		this.wifi_model = wifi_model;
		//this.client_ip = client_ip;
		//this.client_port = client_port;
		this.ap_scans = ap_scans;
		this.tags = tags;
		this.description = description;
	}
	*/
	
	public Bind(Location location, Location est_location, Date bind_stamp, 
		    String device_model, String wifi_model, List<AP_Scan> ap_scans, String tags, String description) {
		//log.debug("basic bind ctor");
		
		this.location = location;
		this.est_location = est_location;
		this.bind_stamp = bind_stamp;
		this.start_stamp = start_stamp;
		this.end_stamp = end_stamp;
		this.device_model = device_model;
		this.wifi_model = wifi_model;
		
		this.ap_scans = ap_scans;
		this.tags = tags;
		this.description = description;
	}

	
	// Not setting these directly from JSON, so must be set after
	public void setState(RequestState state) {
		client_ip = state.client_address;
		client_port = state.client_port;
		version = state.version;
		cookie = new Cookie (state.cookie);
		session = new Cookie (state.session);

		// previously this was sent from the client, but now we just set it here
		start_stamp = null;
		end_stamp = null;

		for (Iterator<AP_Scan> i = ap_scans.iterator(); i.hasNext();) {
			AP_Scan scan = i.next();
			if (start_stamp == null || scan.stamp.before(start_stamp)) {
				start_stamp = scan.stamp;
			}
			if (end_stamp == null || scan.stamp.after(end_stamp)) {
				end_stamp = scan.stamp;
			}
		}
		log.debug ("set start_stamp "+start_stamp+ " end_stamp "+end_stamp);

	}


}

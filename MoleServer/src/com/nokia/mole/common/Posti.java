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
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.nokia.mole.util.TextUtil;

public class Posti implements Serializable {

	// TODO OK for now
	public static final Random random = new Random ();
	
	static Logger log = Logger.getLogger(Posti.class);
	static final long serialVersionUID = 0L;

	public String uid;
	final public String text;
	final public String username;
	final public int radius;
	final public List<String> fingerprint;

	public String client_ip;
	public int client_port;

	transient boolean valid = false;

	public String toString () {
		return new String 
		("[uid=" +uid.substring(0,10)+
		",radius="+radius+
		 ",text="+text.substring(0,(10>text.length()?text.length()-1:10))+
		 ",username="+username+",fp="+fingerprint.get(0));
		
	}

	public boolean isValid () {
		return valid;
	}
	

	private Posti() {
		uid = null;
		text = null;
		username = null;
		radius = 0;
		fingerprint = null;
	}

	
	public Posti(String text, String username, int radius, List<String> fingerprint) {
		this.text = text;
		this.username = username;
		this.radius = radius;
		this.fingerprint = fingerprint;
	}

	// Not setting these directly from JSON, so must be set after
	public void setClient(String remoteAddr, int remotePort) {
		client_ip = remoteAddr;
		client_port = remotePort;
		uid = TextUtil.createUid (160);
	}

	
}

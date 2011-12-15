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


import java.io.Serializable;

import org.apache.log4j.Logger;

public class Source implements Serializable {

	private static final long serialVersionUID = 285725805698964462L;
	static Logger log = Logger.getLogger(Source.class);

	final public String key;
	final public String secret;

	final public String device;
	final public String version;
	
	public Source() {
		this.key = null;
		this.secret = null;
		this.device = null;
		this.version = null;
	}

	public Source(String key, String secret, String device, String version) {
		this.key = key;
		this.secret = secret;
		this.device = device;
		this.version = version;
	}

	@Override
	public String toString() {
		return "Source [device=" + device + ", key=" + key + ", secret="
				+ secret + ", version=" + version + "]";
	}
	

}

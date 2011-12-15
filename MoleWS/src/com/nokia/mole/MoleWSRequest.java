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
import java.util.Date;

import org.apache.log4j.Logger;

public class MoleWSRequest implements Serializable {

	private static final long serialVersionUID = -3564463510763433890L;
	static Logger log = Logger.getLogger(MoleWSRequest.class);
	

	public String sourceIP;
	public Date timestamp;
	final public Source source;

	
	// used by gson
	protected MoleWSRequest() {
		source = null;
	}

	protected MoleWSRequest(Source source) {
		this.source = source;
	}
	
	public void setSource(String localAddr) {
		timestamp = new Date();
		sourceIP = localAddr;
	}

	@Override
	public String toString() {
		return "MoleWSRequest [source=" + source + ", sourceIP=" + sourceIP
				+ ", timestamp=" + timestamp + "]";
	}

	
}

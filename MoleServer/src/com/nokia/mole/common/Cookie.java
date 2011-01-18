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
import java.math.BigInteger;
import java.security.SecureRandom;

import org.apache.log4j.Logger;

public class Cookie implements Comparable<Cookie>, Serializable {

	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(Cookie.class);
	static private SecureRandom random = new SecureRandom();
	
	public final String cookie;
	
	public Cookie () {
		cookie = new BigInteger(130, random).toString(32);
	}

	public Cookie (String _cookie) {
		cookie = _cookie;
	}
	
	public int compareTo(Cookie _cookie) {
		return cookie.compareTo(_cookie.cookie);
	}

	public static Cookie copy(Cookie cookie) {
		return new Cookie (new String (cookie.cookie));
	}

	public static Cookie newRandomInstance() {
		return new Cookie ();
	}
	
	public String toString () {
		return cookie;
	}
	
	
}

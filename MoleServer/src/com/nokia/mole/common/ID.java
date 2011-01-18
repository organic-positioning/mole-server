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
import java.util.Random;

import org.apache.log4j.Logger;


public class ID implements Comparable<ID>, Serializable {

	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(ID.class);
	public static final Random random = new Random ();
	
	public final long id;
	
	public ID (long _id) {
		id = _id;
	}

	public ID (ID _id) {
		id = _id.id;
	}
	
	public ID () {
		// TODO think about nextLong 48 bit limitation
		// see code in Posti
		id = random.nextLong();
	}
	
	public int compareTo(ID _id) {
		if (_id.id == id) return 0;
		if (_id.id < id) return 1;
		return -1;
	}
	
	
}

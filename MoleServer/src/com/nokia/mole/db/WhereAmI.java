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

package com.nokia.mole.db;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;

import org.apache.log4j.Logger;

/**
 * quick and dirty in-memory bounded cache of location estimates 
 *
 */
public class WhereAmI {

	final int max_entries = 1000;
	Deque<EstimateDesc> entries = new ArrayDeque<EstimateDesc> ();

	static Logger log = Logger.getLogger(WhereAmI.class);
	
	public void add(String username, String estimate) {

	    log.debug ("whereami add " + username + " "+ estimate);

		EstimateDesc entry = new EstimateDesc (username, estimate);
		entries.addLast(entry);
		
		synchronized (entries) {
			while (entries.size() > max_entries) {
				entries.removeFirst ();
			}
			
		}
		
	}

	public String listHtml() {
		StringBuilder builder = new StringBuilder ();
		for (EstimateDesc entry : entries) {
		    builder.append(entry.toHtmlTableEntry()+"\n");
			log.debug ("list entry " + entry);
		}
		return new String(builder);
	}
		
	class EstimateDesc {
		final Date stamp;
		final String username;
		final String estimate;
		
		public EstimateDesc(String username, String estimate) {
			this.stamp = new Date ();
			this.username = username;
			this.estimate = estimate;
		}
		
		public String toString () {
			return stamp + " " + username + " " + estimate;
		}

		public String toHtmlTableEntry () {
			return "<tr><td>"+ stamp + "</td><td>" + username + "</td><td>" + estimate + "</td></tr>";
		}

		
	}
	
}	
	

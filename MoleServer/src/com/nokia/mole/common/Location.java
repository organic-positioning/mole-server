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

import org.apache.log4j.Logger;

// TODO There must be some better way to make sure assign_components is called after deserializing


public class Location implements Comparable<Location>, Serializable {

	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ID.class);

	private static final int INVALID_FLOOR = -987654;
	private static final int max_location_component_length = 100;
	public final String full_space_name; 
	
	transient private String country = null;
	transient private String region = null;
	transient private String city = null;
	transient private String area = null;
	transient private String space = null;
	transient private int floor = INVALID_FLOOR;

	transient private boolean valid = false;
	
	// part of deserializing from json
	private void assign_components () {
		String _parts[] = full_space_name.split("/");
		if (_parts.length != 6) {
			setValid(false);
			return;
		}
		country = _parts[0];
		region = _parts[1];
		city = _parts[2];
		area = _parts[3];
		try {
			floor = Integer.parseInt(_parts[4]);
		} catch (NumberFormatException ex) {
			floor = 0;
			setValid(false);
		}
		space = _parts[5];
		
		log.debug("assign components floor "+floor);
		
		// TODO more sanity checking
		if (country.length() > max_location_component_length) {
			setValid(false);
		} else {
			setValid(true);
		}
	}
	
	private void setValid (boolean _valid) {
		valid = _valid;
	}
	
    public Location() {
    	full_space_name = "";
    }

    public Location (Location l) {
    	full_space_name = l.full_space_name;
    }
    
    public Location(String full_space_name) {
    	log.debug("str ctor");
    	this.full_space_name = full_space_name;
	}

	public boolean equals(Object o){
    	Location l2 = (Location)o;
    	if (this.full_space_name.equals(l2.full_space_name)) return true;
    	else return false;	
    }

    public int hashCode () {
    	return (full_space_name).hashCode();
    }
    
    public String toString(){
        return full_space_name;
    }

	public int compareTo(Location o) {
    	Location l2 = (Location)o;
    	return this.full_space_name.compareTo(l2.full_space_name);
	}

	public static Location copy(Location location) {
		return new Location (location.full_space_name);
	}

	public static Location newRandomInstance() {
		String campus = new String (""+BindwExtras.random.nextInt(2));
		String area = new String (""+BindwExtras.random.nextInt(3));
		String space = new String (""+BindwExtras.random.nextInt(3));
		// Bind.newRandomString()
		return new Location (campus+"/"+area+"/"+space);
	}

	public String getCountry() {
		if (country == null) assign_components ();
		return country;
	}

	public String getRegion() {
		if (region == null) assign_components ();
		return region;
	}

	public String getCity() {
		if (city == null) assign_components ();
		return city;
	}

	public String getArea() {
		if (area == null) assign_components ();
		return area;
	}

	public String getSpace() {
		if (space == null) assign_components ();
		return space;
	}

	public int getFloor() {
		if (floor == INVALID_FLOOR) assign_components ();
		return floor;
	}

	
	public boolean isValid() {
		if (space == null) assign_components ();
		return valid;
	}


}

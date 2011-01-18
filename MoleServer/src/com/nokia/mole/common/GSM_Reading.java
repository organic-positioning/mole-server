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

public class GSM_Reading implements Serializable {

	// see http://developer.android.com/reference/android/telephony/gsm/GsmCellLocation.html
	
	private static final long serialVersionUID = 1L;
	
	public final int cid;
	public final int lac;
	public final int asu;
	
	public GSM_Reading(final int cid, final int lac, final int asu) {
		this.cid = cid;
		this.lac = lac;
		this.asu = asu;
	}

	public static GSM_Reading copy(GSM_Reading r) {
		return new GSM_Reading (r.cid, r.lac, r.asu);
	}
	

}

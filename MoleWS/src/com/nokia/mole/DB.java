/*
* 
* Mole - Mobile Organic Localisation Engine
* Copyright (C) 2010-2012 Nokia Corporation.  All rights reserved.
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

import java.util.List;
import java.util.Map;

public interface DB {

	public abstract boolean bind(Bind bind);

	public abstract List<LocationProbability> query(Query query);

	public abstract boolean remove(Remove remove);

	public abstract void clear();

	public abstract Map<Location, List<Scan>> getLocationScans();

}
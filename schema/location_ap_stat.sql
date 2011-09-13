# 
# Mole - Mobile Organic Localisation Engine
# Copyright (C) 2010, 2011 Nokia Corporation.  All rights reserved.
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 

CREATE TABLE `location_ap_stat` (
`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
`location_id` INT UNSIGNED NOT NULL,
`bssid` CHAR(20) NOT NULL,
`stamp` TIMESTAMP NOT NULL,
`avg` FLOAT(5,3) NOT NULL,
`stddev` FLOAT(5,3) NOT NULL,
`min` TINYINT UNSIGNED NOT NULL,
`max` TINYINT UNSIGNED NOT NULL,
`weight` FLOAT(3,3) NOT NULL,
`is_active` TINYINT(1) DEFAULT '1',
`histogram` VARCHAR(512) NOT NULL,
PRIMARY KEY (id),
INDEX (bssid),
INDEX (location_id),
UNIQUE (location_id,bssid),
FOREIGN KEY (location_id) REFERENCES locations(id)
) ENGINE = InnoDB;

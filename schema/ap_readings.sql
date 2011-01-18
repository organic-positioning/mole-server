# 
# Mole - Mobile Organic Localisation Engine
# Copyright (C) 2010 Nokia Corporation.  All rights reserved.
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


# TODO make bssid and ssid shorter?
# replicated location_id in binds and here to make fp lookup more efficient

# bssid -> macs seen in a particular location

CREATE TABLE `ap_readings` (
`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
`bind_id` INT UNSIGNED NOT NULL,
`location_id` INT UNSIGNED NOT NULL,
`stamp` DATETIME NOT NULL,
`bssid` CHAR(20) NOT NULL,
`ssid` CHAR(20) NOT NULL,
`frequency` INT UNSIGNED NOT NULL,
`level` TINYINT UNSIGNED NOT NULL,
PRIMARY KEY (id),
INDEX (bssid),
INDEX (location_id),
INDEX (stamp),
FOREIGN KEY (location_id) REFERENCES locations(id),
FOREIGN KEY (bind_id) REFERENCES binds(id)
) ENGINE = InnoDB;

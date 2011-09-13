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

CREATE TABLE `location_stats` (
`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
`location_id` INT UNSIGNED NOT NULL,
`first_bind` DATETIME NOT NULL,
`last_bind` DATETIME NOT NULL,
`hit_count` FLOAT(5,3) NOT NULL,
`bind_count` FLOAT(5,3) NOT NULL,
`is_changed` TINYINT(1) DEFAULT '1',
PRIMARY KEY (id),
UNIQUE (location_id),
FOREIGN KEY (location_id) REFERENCES locations(id)
) ENGINE = InnoDB;

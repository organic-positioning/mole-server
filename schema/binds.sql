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

CREATE TABLE `binds` (
`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
`start_stamp` DATETIME NOT NULL,
`bind_stamp` DATETIME NOT NULL,
`end_stamp` DATETIME NOT NULL,
`location_id` INT UNSIGNED NOT NULL,
`est_location_id` INT UNSIGNED,
`cookie` CHAR(20),
`tags` CHAR(80),
`description` CHAR(80),
`device_model` CHAR(40),
`wifi_model` CHAR(20),
# yes, client_ip could be compacted
`client_ip` CHAR(15) NOT NULL,
`client_port` SMALLINT UNSIGNED NOT NULL,
`client_version` SMALLINT UNSIGNED NOT NULL,
`is_new` TINYINT(1) DEFAULT '1',
PRIMARY KEY (id),
INDEX (is_new),
FOREIGN KEY (location_id) REFERENCES locations(id),
FOREIGN KEY (est_location_id) REFERENCES locations(id)
) ENGINE = InnoDB;


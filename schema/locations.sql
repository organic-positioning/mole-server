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


# Obviously could be segmented further...

CREATE TABLE `locations` (
`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`country` CHAR(80),
`region` CHAR(80),
`city` CHAR(80),
`area` CHAR(80),
`name` CHAR(80),
`is_new` TINYINT(1) DEFAULT '1',
PRIMARY KEY (id),
INDEX (country),
INDEX (country,region),
INDEX (country,region,city),
INDEX (country,region,city,area),
INDEX (country,region,city,area,name),
INDEX (is_new),
UNIQUE (country,region,city,area,name)
) ENGINE = InnoDB;

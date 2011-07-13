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

DROP TRIGGER IF EXISTS ap_readings_insert_trigger;

delimiter //


CREATE TRIGGER ap_readings_insert_trigger BEFORE INSERT ON ap_readings
FOR EACH ROW
BEGIN
  DECLARE _start_stamp TIMESTAMP;
  DECLARE _end_stamp TIMESTAMP;
  SELECT start_stamp,end_stamp into _start_stamp, _end_stamp
  FROM binds
  WHERE new.bind_id = binds.id;

  IF NEW.stamp > _end_stamp THEN
    -- insert into log values ('ap trigger A');
    SET NEW.location_id = NULL;
  END IF;

  IF NEW.stamp < _start_stamp THEN
    -- insert into log values ('ap trigger B');
    SET NEW.location_id = NULL;
  END IF;

END;
//

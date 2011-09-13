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


# Enforce timestamp ordering constraint.
# (Tuple is rejected if location_id is null)

DROP TRIGGER IF EXISTS binds_insert_trigger;

DROP TRIGGER IF EXISTS binds_update_trigger;

#delimiter //


CREATE TRIGGER binds_insert_trigger BEFORE INSERT ON binds
FOR EACH ROW
BEGIN
  -- These long time lags can happen because legitimate (different) scans 
  -- can happen slower than we are scanning because they are duplicates.
  -- If scanQueue's max is 60, timestampdiff can be more than 60.
  -- Hopefully 120 is a safe value.
  -- Otherwise old scans could be tossed by the client.
  IF TIMESTAMPDIFF(minute,NEW.start_stamp, NEW.bind_stamp) > 120 THEN
    insert into log values ('start binds_insert_trigger A');
    SET NEW.location_id = NULL;
  END IF;
  IF TIMESTAMPDIFF(minute,NEW.end_stamp, NEW.bind_stamp) > 120 THEN
    insert into log values ('start binds_insert_trigger B');
    SET NEW.location_id = NULL;
  END IF;
  IF NEW.end_stamp < NEW.start_stamp THEN
    insert into log values ('start binds_insert_trigger C');
    SET NEW.location_id = NULL;
  END IF;
  IF NEW.start_stamp < date_sub(now(), interval 1 day) THEN
    insert into log values ('start binds_insert_trigger D');
    SET NEW.location_id = NULL;
  END IF;
  IF NEW.end_stamp > date_add(now(), interval 1 day) THEN
    insert into log values ('start binds_insert_trigger E');
    SET NEW.location_id = NULL;
  END IF;
  IF TIMESTAMPDIFF(minute,NEW.end_stamp,NEW.start_stamp) > 120 THEN
    insert into log values ('start binds_insert_trigger F');
    SET NEW.location_id = NULL;
  END IF;
END;
//


-- CREATE TRIGGER binds_update_trigger BEFORE UPDATE ON binds
-- FOR EACH ROW
-- BEGIN
--   SET NEW.location_id = NULL;
-- END;
-- //

CREATE TRIGGER binds_update_trigger BEFORE UPDATE ON binds
FOR EACH ROW
BEGIN
  IF TIMESTAMPDIFF(minute,NEW.start_stamp, NEW.bind_stamp) > 30 THEN
    insert into log values ('binds_update_trigger A');
    SET NEW.location_id = NULL;
  END IF;
  IF TIMESTAMPDIFF(minute,NEW.end_stamp, NEW.bind_stamp) > 30 THEN
    insert into log values ('binds_update_trigger B');
    SET NEW.location_id = NULL;
  END IF;
  IF NEW.end_stamp < NEW.start_stamp THEN
    insert into log values ('binds_update_trigger C');
    SET NEW.location_id = NULL;
  END IF;
-- Cannot have these because builder might work on old binds
--  IF NEW.start_stamp < date_sub(now(), interval 1 day) THEN
--    SET NEW.location_id = NULL;
--  END IF;
--  IF NEW.end_stamp > date_add(now(), interval 1 day) THEN
--    SET NEW.location_id = NULL;
--  END IF;
  IF TIMESTAMPDIFF(minute,NEW.end_stamp,NEW.start_stamp) > 120 THEN
    insert into log values ('binds_update_trigger D');
    SET NEW.location_id = NULL;
  END IF;
END;
//

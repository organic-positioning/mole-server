#!/bin/bash

TABLES_SCHEMA=/tmp/schema_tables.sql
TRIGGERS_SCHEMA=/tmp/schema_triggers.sql

echo $TMP_SCHEMA

# For clarity, just keep these files separate during development

cat log.sql locations.sql binds.sql ap_readings.sql location_ap_stat.sql > $TABLES_SCHEMA

cat ap_readings_insert_trigger.sql binds_insert_trigger.sql > $TRIGGERS_SCHEMA

#mysql -u oiluser -p oil < $TMP_SCHEMA
mysql -u moleuser -p mole < $TABLES_SCHEMA
mysql -u master -p mole < $TRIGGERS_SCHEMA

#rm $TMP_SCHEMA
#!/bin/bash

TMP_SCHEMA=/tmp/schema.sql

echo $TMP_SCHEMA

# For clarity, just keep these files separate during development

cat locations.sql binds.sql ap_readings.sql location_ap_stat.sql ap_readings_insert_trigger.sql binds_insert_trigger.sql > $TMP_SCHEMA

mysql -u moleuser -p mole < $TMP_SCHEMA

rm $TMP_SCHEMA
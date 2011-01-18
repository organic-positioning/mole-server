#!/usr/bin/perl

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


use strict;
use Sys::Hostname;
use Getopt::Std;

my $hostname = hostname;

my %para = ();
my $main_cfg_file = "config/moleserver.cfg";
my $log_cfg_file = "config/log.cfg";
getopts('c:l:', \%para);
if (defined ($para{'c'})) {
    $main_cfg_file = $para{'c'};
}
if (defined ($para{'l'})) {
    $log_cfg_file = $para{'l'};
}

print "Starting moleserver on node $hostname main_cfg=$main_cfg_file log_cfg=$log_cfg_file ...\n";

if (-e "moleserver-$hostname.log") {
    rename ("moleserver-$hostname.log", "moleserver-$hostname.log.prev");
}

my $ANT="ant -emacs --noconfig";

my $ANT_LOG="-l moleserver-$hostname.log";

my $moleserver_config="-Dmoleserver.cfg=$main_cfg_file -Dmoleserver.log4cfg=$log_cfg_file";

#my $moleserver_config="-Dmoleserver.config=config/moleserver.cfg -Djava.util.logging.config.file=config/log.cfg";


my $CMD="$ANT $moleserver_config run";

print "Launching... $CMD";
exec ($CMD);

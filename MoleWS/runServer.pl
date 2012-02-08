#!/usr/bin/perl

# Mole - Mobile Organic Localisation Engine
# Copyright (C) 2010-2012 Nokia Corporation.  All rights reserved.
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
my $main_cfg_file = "config/moleWS.cfg";
my $log_cfg_file = "config/log.cfg";
getopts('c:l:e', \%para);
if (defined ($para{'c'})) {
    $main_cfg_file = $para{'c'};
}
if (defined ($para{'l'})) {
    $log_cfg_file = $para{'l'};
}

print "Starting moleWS on node $hostname main_cfg=$main_cfg_file log_cfg=$log_cfg_file ...\n";

if (-e "moleWS-$hostname.log") {
    rename ("moleWS-$hostname.log", "moleWS-$hostname.log.prev");
}

my $ANT="ant -emacs --noconfig";

my $ANT_LOG="-l moleWS-$hostname.log";

my $moleWS_config="-DmoleWS.cfg=$main_cfg_file -DmoleWS.log4cfg=$log_cfg_file";

#my $moleserver_config="-Dmoleserver.config=config/moleserver.cfg -Djava.util.logging.config.file=config/log.cfg";


my $CMD="$ANT $moleWS_config run";
if (defined ($para{'e'})) {
    $CMD = "$ANT $moleWS_config run-eval";
}

print "Launching... $CMD";
exec ($CMD);

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


# TODO add shutdown hook / catch ctrl-c
# TODO make logger die on log->fatal 

use strict;
use Data::Dumper;
use Log::Log4perl;
use XML::Simple qw(XMLout);
use DBI;

use IO::Socket;
use threads;
use threads::shared;

use S3::S3Object;
use S3::AWSAuthConnection;
use S3::QueryStringAuthGenerator;

use Data::Dumper;
use Socket;
use Sys::Hostname;

use Getopt::Std;
use Config::Tiny;
use File::Path qw(make_path remove_tree);

######################################################################
# Constants, global parameters

my $VERSION = 'Version: 0.3.1';

# How often should fingerprints be rebuilt in seconds?
my $BUILD_PERIOD = 10;

# Attempt to use this many bind minutes at least
my $TARGET_BIND_MINUTES = 10;

my $MAX_APS_PER_FP = 50;

my $AVG_STDDEV = 1.0;

# Output location
my $OUT_DIR = '/var/www/map';

# set to 1 to generate histograms
my $useHistograms = 1;

# Log
my $log;

# Amazon S3/Cloudfront
my $AWS = 0;

# Database
my $dsn = '';
my $db_user = '';
my $db_pw = '';

my $cfg = Config::Tiny->new;
my $last_error = "OK";
my $reset = 0;

######################################################################

my $AWS_ACCESS_KEY_ID     = '';
my $AWS_SECRET_ACCESS_KEY = '';
my $aws_connection;
my $bucket = '';

######################################################################

sub init {
    my ($self) = @_;

    my $cfg_file = 'builder.cfg';

    my $host = hostname();
    my $addr = inet_ntoa(scalar(gethostbyname($host)) || 'localhost');
    print ("Builder IP address $addr\n");

    my %para = ();
    getopts('c:R', \%para);
    if (defined($para{'c'})) {
	$cfg_file = $para{'c'};
    } else {
	warn ("Using config file $cfg_file\n");
    }
    if (defined($para{'R'})) {
	print ("Are you sure you want to reset? Y/N: ");
	my $y_or_n = <STDIN>;
	chop ($y_or_n);
	if ($y_or_n eq 'Y') {
	    $reset = 1;
	}
    }

    $cfg = Config::Tiny->read ($cfg_file);
    if (!defined ($cfg)) {
	die ("Cannot open config file $cfg_file.  Try builder.pl -c config_file");
    }

    $dsn = &getProperty ('dsn', 'DBI:mysql:database=mole;host=localhost');
    $db_user = &getProperty ('db_user', 'moleuser');
    $db_pw = &getProperty ('db_pw', 'molepw');

    $bucket = &getProperty ('bucket', 'mybucket');
    
    $BUILD_PERIOD = &getProperty ('build_period', 10);
    $TARGET_BIND_MINUTES = &getProperty ('target_bind_minutes', 2);
    $MAX_APS_PER_FP = &getProperty ('max_aps_per_fp', 40);
    $AVG_STDDEV = &getProperty ('avg_stddev', 1.0);
    $OUT_DIR = &getProperty ('output_dir', '/var/www/map');
    $AWS = &getProperty ('aws', 0);

    $AWS_ACCESS_KEY_ID = &getProperty ('aws_key', '');
    $AWS_SECRET_ACCESS_KEY = &getProperty ('aws_secret', '');

    my $log_cfg_file = &getProperty ('log_cfg', 'log.cfg');

    Log::Log4perl::init ($log_cfg_file);
    $log = Log::Log4perl->get_logger;
    #$log->logwarn();
    #$log->logdie();

    $log->info ("Starting Fingerprint Builder r$VERSION");

    if ($AWS) {
	$aws_connection = S3::AWSAuthConnection->new($AWS_ACCESS_KEY_ID,
						     $AWS_SECRET_ACCESS_KEY);
	if (!defined ($aws_connection)) {
	    $log->fatal ("Connection failed $@");
	}
    } else {

	# create out dir
	if (! -d $OUT_DIR) {
	    mkdir "$OUT_DIR" or $log->fatal ("Cannot mkdir $OUT_DIR");
	}
    }
    

    # connect to db
    $log->info ("Connecting to database ".$dsn);
    &db_connect ($self);

}

######################################################################

sub getProperty {
    my ($key, $default) = @_;

    if (defined ($cfg->{_}->{$key})) {
	return $cfg->{_}->{$key};
    }
    return $default;

}

######################################################################

sub fill_space_desc {
    my ($self, $location_id, $desc) = @_;

    $self->{get_location_desc_stmt} = $self->{dbi}->prepare_cached
	("SELECT country,region,city,area,floor,name from locations where id=?");
    $self->{get_location_desc_stmt}->execute ($location_id);

    $desc->{fq_space_name} = 'unknown';
    $desc->{fq_area} = 'unknown';
    if ($self->{get_location_desc_stmt}->rows == 0) {
	$log->warn ("No location for id=$location_id");
    } else {
	my @location = $self->{get_location_desc_stmt}->fetchrow_array();

	my $fq_area = $location[0].'/'.$location[1].'/'.$location[2].'/'.$location[3].'/'.$location[4];
	my $fq_space_name = '['.$fq_area.'/'.$location[5].']';

	$desc->{country} = $location[0];
	$desc->{region} = $location[1];
	$desc->{city} = $location[2];
	$desc->{area} = $location[3];
	$desc->{floor} = $location[4];
	$desc->{name} = $location[5];
	#$desc->{version} = $location[5];

	$desc->{fq_space_name} = $fq_space_name;
	$desc->{fq_area} = $fq_area;

    }

    $self->{get_location_desc_stmt}->finish ();
}

######################################################################

sub remove_space {
    my ($self, $location_id) = @_;

    $log->debug ("remove_space $location_id");
    
    $self->{locations_remove_space_stmt} = $self->{dbi}->prepare_cached
	("update locations set is_active=0, is_changed=1 where id=?");
    $self->{locations_remove_space_stmt}->execute ($location_id);
    $self->{locations_remove_space_stmt}->finish ();

    $self->{location_ap_stat_remove_space_stmt} = $self->{dbi}->prepare_cached
	("update location_ap_stat set is_active=0 where location_id=?");
    $self->{location_ap_stat_remove_space_stmt}->execute ($location_id);
    $self->{location_ap_stat_remove_space_stmt}->finish ();

}

######################################################################

sub process_bind {
    my ($self, $bind_id, $location_id, $space_desc) = @_;

    $log->debug ("START processing bind space ".$space_desc->{fq_space_name}." id $location_id");
    
    # activate this space if it isn't already
    $self->{locations_activate_space_stmt} = $self->{dbi}->prepare_cached
	("update locations set is_active=1, is_changed=1 where id=?");
    $self->{locations_activate_space_stmt}->execute ($location_id);
    $self->{locations_activate_space_stmt}->finish ();

    # Isolate recent binds covering last T minutes.
    # Ignore all really old binds regardless (unless we are doing a reset)
    my $get_recent_binds_sql = "SELECT id, cookie, timestampdiff(minute,start_stamp,end_stamp) as duration, end_stamp from binds ".
	"where location_id=? and source != 'remove'";
    if (!$reset) {
	$get_recent_binds_sql .= " and bind_stamp >= date_sub(now(),interval 1 month) ";
    }
    $get_recent_binds_sql .= " order by bind_stamp desc";
    $self->{get_recent_binds_stmt} = $self->{dbi}->prepare_cached
	($get_recent_binds_sql);

    $self->{get_recent_binds_stmt}->execute ($location_id);

    if ($self->{get_recent_binds_stmt}->rows == 0) {
	$log->error ("no binds for location ".$location_id." when there should be.  bind_id=".$bind_id);
	$self->{get_recent_binds_stmt}->finish();
	return;
    }

    my $binds = $self->{get_recent_binds_stmt}->fetchall_arrayref(
	{ id => 1, cookie => 1, duration => 1, end_stamp => 1 } );

    my $bind_minutes = 0;
    my $earliest_end_stamp;
    for (my $b = 0; $b <= $#$binds && $bind_minutes < $TARGET_BIND_MINUTES; $b++) {

	my $bind = $binds->[$b];

	$earliest_end_stamp = $bind->{end_stamp};


	# TODO add something that attempts to get diversity using cookies
	#if (!defined($seen_cookies{$bind->{cookie}})) {
	$bind_minutes += $bind->{duration};

	$log->debug ("location $location_id recent bind id ".$bind->{id}.
		     " earliest ".$earliest_end_stamp .
		     " duration " . $bind->{duration} . 
		     " sum " . $bind_minutes);


	#    $seen_cookies{$bind->{cookie}} = 1;
	#}

    }
    $self->{get_recent_binds_stmt}->finish();

    # was: "and stamp >= ? ".

    # gaussian overlap generation
    $self->{get_readings_stmt} = $self->{dbi}->prepare_cached
	("SELECT bssid, count(level) as count, avg(level) as avg, ".
	 "stddev(level) as stddev, min(level) as min, max(level) as max ".
	 "from ap_readings where location_id=? ".
	 "and level>=20 and level<100 ".
	 "and stamp >= date_sub(?, interval 600 second) ".
	 "group by bssid order by max asc, count desc limit $MAX_APS_PER_FP");

    $self->{get_readings_stmt}->execute ($location_id, $earliest_end_stamp);
    
    my $ap_stats = $self->{get_readings_stmt}->fetchall_arrayref(
	{ bssid => 1, count => 1, avg => 1, stddev => 1, min => 1, max => 1 });


    # for some unknown reason, "select lower(bssid)" does not give
    # this same result...

    foreach my $ap_stat (@$ap_stats) {
	$ap_stat->{bssid} = lc($ap_stat->{bssid});
	$log->debug ("ap lc ".$ap_stat->{bssid});
	# placeholder for histogram, whether or not it is filled
	$ap_stat->{histogram} = '';
    }

    my %ap2weight = ();
    my %ap2index = ();

    $log->debug ("ap stats count ".$#$ap_stats);

    my $total_count = 0;
    for (my $a = 0; $a <= $#$ap_stats; $a++) {
	my $ap_stat = $ap_stats->[$a];
	$total_count += $ap_stat->{count};
	$log->debug ("a $ap_stat->{bssid} $a count ".$ap_stat->{count}." total $total_count");
	$ap2weight{$ap_stat->{bssid}} = 1;
	$ap2index{$ap_stat->{bssid}} = $a;
    }

    # sanity check
    if ($total_count <= 0) {
	die ("count $total_count loc $location_id"); 
	#$log->fatal ("count $total_count loc $location_id"); 
    }


    for (my $a = 0; $a <= $#$ap_stats; $a++) {

	my $ap_stat = $ap_stats->[$a];
	my $weight = $ap_stat->{count} / $total_count;
	$log->debug ("weight ap ".$ap_stat->{bssid}." $weight");

	if ($weight > 0.001) {
	    $ap2weight{$ap_stat->{bssid}} = $weight;

	    # pull stddev toward a common value, based on number of readings
	    my $stddev = $ap_stat->{stddev};
	    my $pow = $ap_stat->{count}**1;
	    my $b_stddev = (($pow-1) * $stddev + $AVG_STDDEV) / $pow;
	    $ap_stats->[$a]->{stddev} = $b_stddev;
	} else {
	    # prevent aps with what gets rounded down to 0 weight from being put into the fingerprint
	    undef ($ap2weight{$ap_stat->{bssid}});
	}
    }

    # histogram generation
    if ($useHistograms) {

	$self->{get_histogram_stmt} = $self->{dbi}->prepare_cached
	    ("SELECT bssid, count(level) as count, level ".
	     "from ap_readings where location_id=? ".
	     "and level>=20 and level<100 ".
	     "and stamp >= date_sub(?, interval 600 second) ".
	     "group by bssid,level order by level asc");

	$self->{get_histogram_stmt}->execute ($location_id, $earliest_end_stamp);

	my $mac_readings = $self->{get_histogram_stmt}->fetchall_arrayref(
	    { bssid => 1, count => 1, level => 1 });
	my %bssid2hist = ();
	foreach my $mac_reading (@$mac_readings) {
	    my $bssid = lc ($mac_reading->{bssid});
	    if (exists ($bssid2hist{$bssid})) {
		$bssid2hist{$bssid} .= ' ';
	    }
	    $bssid2hist{$bssid} .= $mac_reading->{level} .'='.$mac_reading->{count};
	}
	foreach my $ap_stat (@$ap_stats) {
	    if (exists ($bssid2hist{$ap_stat->{bssid}})) {
		$ap_stat->{histogram} = $bssid2hist{$ap_stat->{bssid}};
	    } else {
		$log->warn ("histogram mismatch bssid ".$ap_stat->{bssid}." location ".$location_id);
	    }
	}

	$self->{get_histogram_stmt}->finish ();

    }



    # Update location_ap_stat table
    # - update existing rows
    # - mark unused rows as inactive
    # - insert missing rows
    # Idea is to keep the table as short as possible.

    $self->{get_location_ap_stat_stmt} = $self->{dbi}->prepare_cached
	("SELECT id, bssid from location_ap_stat where location_id = ?");
    $self->{get_location_ap_stat_stmt}->execute ($location_id);
    my $location_ap_stats = $self->{get_location_ap_stat_stmt}->fetchall_arrayref(
	{ id => 1, bssid => 1 });

    foreach my $ap_stat (@$location_ap_stats) {
	$ap_stat->{bssid} = lc($ap_stat->{bssid});
    }


    # handle existing APs
    my %preexisting_aps = ();
    for (my $s = 0; $s <= $#$location_ap_stats; $s++) {

	my $location_ap_stat = $location_ap_stats->[$s];
	$preexisting_aps{$location_ap_stat->{bssid}} = 1;

	if (defined($ap2weight{$location_ap_stat->{bssid}})) {

	    my $index = $ap2index{$location_ap_stat->{bssid}};
	    if (!defined($index)) { die ("oops A") }
	    my $ap_stat = $ap_stats->[$index];

	    $self->{set_location_ap_stat_active_stmt} = $self->{dbi}->prepare_cached
		("UPDATE location_ap_stat SET stamp=?, avg=?, stddev=?, ".
		 "min=?, max=?, weight=?, is_active=1, histogram=?".
		 "where id=?");
	    $self->{set_location_ap_stat_active_stmt}->execute
		($earliest_end_stamp,
		 $ap_stat->{avg}, $ap_stat->{stddev}, $ap_stat->{min}, $ap_stat->{max},
		 $ap2weight{$location_ap_stat->{bssid}}, $ap_stat->{histogram}, $location_ap_stat->{id});

	    $log->debug ("AP ".$location_ap_stat->{bssid}." updated in space ".$space_desc->{fq_space_name});


	} else {

	    # This AP is no longer seen in this space
	    $self->{set_location_ap_stat_inactive_stmt} = $self->{dbi}->prepare_cached
		("UPDATE location_ap_stat SET is_active=0 where id=?");
	    $self->{set_location_ap_stat_inactive_stmt}->execute ($location_ap_stat->{id});
	    $self->{set_location_ap_stat_inactive_stmt}->finish ();
	    $log->debug ("AP ".$location_ap_stat->{bssid}." no longer seen in space ".$space_desc->{fq_space_name});
	}

    }

    # handle any new APs
    for (my $a = 0; $a <= $#$ap_stats; $a++) {

	my $ap_stat = $ap_stats->[$a];

	if (!defined($preexisting_aps{$ap_stat->{bssid}})) {

	    $self->{insert_location_ap_stat_stmt} = $self->{dbi}->prepare_cached
		("INSERT into location_ap_stat (location_id, bssid, stamp, avg, stddev, min, max, weight, histogram) ".
		 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
	    $self->{insert_location_ap_stat_stmt}->execute
		($location_id, $ap_stat->{bssid}, $earliest_end_stamp,
		 $ap_stat->{avg}, $ap_stat->{stddev}, $ap_stat->{min}, $ap_stat->{max},
		 $ap2weight{$ap_stat->{bssid}}, $ap_stat->{histogram});
	    $self->{insert_location_ap_stat_stmt}->finish ();

	    $log->debug ("AP ".$ap_stat->{bssid}." inserted in space ".$space_desc->{fq_space_name});
	}
    }


    $self->{get_location_ap_stat_stmt}->finish ();

    $self->{get_readings_stmt}->finish ();

    $log->debug ("END processing bind space ".$space_desc->{fq_space_name}." id $location_id");

}


######################################################################
# roll out new fingerprint map for this area

sub process_area {
    my ($self, $area_desc) = @_;

    $log->debug ("START process_area ".$area_desc->{fq_area});

    $self->{get_space_ap_stat_join_stmt} = $self->{dbi}->prepare_cached
	("SELECT name, bssid, avg, stddev, weight, histogram from location_ap_stat, locations ".
	 "where country=? and region=? and city=? and area=? and floor=? and locations.is_active=1 and ".
	 "locations.id = location_ap_stat.location_id ".
	 "and location_ap_stat.is_active=1 ".
	 "order by name, weight");
    $self->{get_space_ap_stat_join_stmt}->execute 
	($area_desc->{country}, $area_desc->{region},
	 $area_desc->{city}, $area_desc->{area}, $area_desc->{floor});


    my $space_count = $self->{get_space_ap_stat_join_stmt}->rows;
    $log->debug ("Space count ".$space_count);
    my %spaces = ();
    if ($space_count == 0) {
	$log->debug ("Removing area with no spaces ".$area_desc->{fq_area});
    } else {

	my $sub_fps = $self->{get_space_ap_stat_join_stmt}->fetchall_arrayref(
	    { name => 1, bssid => 1, avg => 1, stddev => 1, weight => 1, histogram => 1 } );

	foreach my $sub_fp (@$sub_fps) {
	    $sub_fp->{bssid} = lc ($sub_fp->{bssid});
	}

	my %macs = ();
	my $current_space_name = '';

	# for each space, we make a hash with key 'bssid' and values
	# hash {avg, stddev, weight}

	# we then assign this hash to a key called 'mac'

	# this results in a list in the XML output like:
	# <mac name="bssid1" avg="50" .../>
	# <mac name="bssid2" avg="48" .../>

	# in the future, if we want to add more description on to the
	# space, we should probably use the same method

	for (my $s = 0; $s <= $#$sub_fps; $s++) {

	    my $sub_fp = $sub_fps->[$s];

	    # space-name for this mac
	    my $name = $sub_fp->{name};

	    if ($current_space_name eq '') {
		$current_space_name = $name;
	    }

	    if ($current_space_name ne $name) {

		my %i_macs = ();
		my %j_macs = ();
		foreach my $m (keys %macs) {
		    $j_macs{$m} = $macs{$m};
		    $log->debug ("mac $m");
		}

		$i_macs{mac} = \%j_macs;

		$spaces{$current_space_name} = \%i_macs;
		$current_space_name = $name;
		%macs = ();

		$log->debug ("space name $current_space_name");

	    }

	    my %ap = ();
	    $ap{avg} = $sub_fp->{avg};
	    $ap{stddev} = $sub_fp->{stddev};
	    $ap{weight} = $sub_fp->{weight};
	    $ap{histogram} = $sub_fp->{histogram};
	    
	    my $key = $sub_fp->{bssid};
	    $macs{$key} = \%ap;

	}

	# catch the last space

	my %i_macs = ();
	my %j_macs = ();
	foreach my $m (keys %macs) {
	    $j_macs{$m} = $macs{$m};
	}
	$i_macs{mac} = \%j_macs;
	$spaces{$current_space_name} = \%i_macs;

	$log->debug ("space (last) name $current_space_name");


    }

    $self->{get_space_ap_stat_join_stmt}->finish ();

    my %space_fps = ();
    $space_fps{builder_version} = $VERSION;
    # TODO put map version here
    $space_fps{map_version} = 0;
    $space_fps{country} = $area_desc->{country};
    $space_fps{region} = $area_desc->{region};
    $space_fps{city} = $area_desc->{city};
    $space_fps{area} = $area_desc->{area};
    $space_fps{floor} = $area_desc->{floor};
    $space_fps{spaces} = \%spaces;

    my $outfile = $OUT_DIR.'/'.$area_desc->{country}.'/'.
	$area_desc->{region}.
	'/'.$area_desc->{city}.'/'.$area_desc->{area}.'/'.$area_desc->{floor}.'/sig.xml';

    # when space_count > 0
    # we added, changed, or removed a space, but the floor still exists,
    # else, no spaces exist on this floor any more.

    if (!$AWS) {
	if ($space_count > 0) {
	    &make_output_dir ($area_desc->{country}.'/'.$area_desc->{region}.'/'.
			      $area_desc->{city}.'/'.$area_desc->{area}.'/'.$area_desc->{floor});

	    open XML_OUT, ">$outfile" or die ("Cannot open $outfile for writing");
	    print XML_OUT XMLout(\%space_fps, RootName => "area");
	    close XML_OUT;
	} else {
	    if (-e $outfile) {
		unlink $outfile;
	    }
	}
    } else {

	my $response;
	if ($space_count > 0) {
	    my $xml = XMLout(\%space_fps, RootName => "area");
	    my %headers = (
		'Content-Type' => 'text/xml',
		'x-amz-acl' => 'public-read'
		);
	    # outfile == key
	    $response = $aws_connection->put
		( $bucket, $outfile, S3::S3Object->new($xml),
		  \%headers);
	} else {
	    $response = $aws_connection->delete ($bucket, $outfile);
	}


	if ($response->http_response->code == 200) {
	    $log->debug ("Stored/removed xml in S3: $outfile");
	} else {
	    $log->fatal ("Failed to store/remove xml in S3: $outfile $@ ".
			 $response->http_response->code. " ".
			 $response->http_response->message);
	}

    }

    $log->debug ("END process_area ".$area_desc->{fq_area});

}

######################################################################

sub handle_new_binds {
    my ($self) = @_;
    
    my $get_new_binds_sql = "SELECT id, location_id, source from binds";

    if (!$reset) {
	$get_new_binds_sql .= " where is_new=1";
    }

    $self->{get_new_binds_stmt} = $self->{dbi}->prepare_cached
	($get_new_binds_sql);


    $self->{get_new_binds_stmt}->execute();

    # are there any new binds since last time
    if ($self->{get_new_binds_stmt}->rows > 0) {

	my %dirty_areas = ();

	my @new_bind_ids = ();
	my $new_binds = $self->{get_new_binds_stmt}->fetchall_arrayref(
	    { id => 1, location_id => 1, source => 1 } );
	
	for (my $b = 0; $b <= $#$new_binds; $b++) {
	    my $new_bind = $new_binds->[$b];
	    $log->debug ("new bind ".$new_bind->{id}." loc ".$new_bind->{location_id}
		." source ".$new_bind->{source});
	    push (@new_bind_ids, $new_bind->{id});

	    my %space_desc = ();
	    &fill_space_desc ($self, $new_bind->{location_id}, \%space_desc);

	    # process this bind
	    if ($new_bind->{source} eq "remove") {
		&remove_space ($self, $new_bind->{location_id});
	    } else {
		&process_bind ($self, $new_bind->{id}, $new_bind->{location_id}, \%space_desc);
	    }

	    $dirty_areas{$space_desc{fq_area}} = \%space_desc;

	}


	# clear the dirty bit in the binds we have just handled
 	foreach my $new_bind_id (@new_bind_ids) {
	    $log->debug ("new_bind_id $new_bind_id");
	    $self->{set_new_bind_id_false_stmt} = $self->{dbi}->prepare_cached
		("UPDATE binds set is_new=0 WHERE id=?");
	    $self->{set_new_bind_id_false_stmt}->execute ($new_bind_id);
	    $self->{set_new_bind_id_false_stmt}->finish ();

 	}

	foreach my $area (keys %dirty_areas) {

	    my $area_desc = $dirty_areas{$area};
	    #print "area $area\n";
	    #print Dumper ($area_desc);

	    &process_area ($self, $area_desc);

	}

    }

    $self->{get_new_binds_stmt}->finish();    

    $self->{dbi}->commit;


}

######################################################################
sub make_output_dir {
    my ($relpath) = @_;

    $log->debug ("make_output_dir $relpath");

    if ($AWS) { return; }

    my $abspath = $OUT_DIR.'/'.$relpath;
    make_path ($abspath);
}

######################################################################
sub rm_output_files {
    my ($relpath) = @_;

    $log->debug ("rm_output_files $relpath");
    my $abspath = $OUT_DIR.'/'.$relpath;

    foreach my $file ('places.txt', 'sig.xml') {
	my $absfile = $abspath.'/'.$file;

	if ($AWS) {
	    my $response = $aws_connection->delete($bucket, $absfile);
	    if ($response->http_response->code == 200) {
		$log->debug ("rm_output_dir deleted $absfile");
	    } else {
		$log->debug ("rm_output_dir could not delete $absfile");
	    }
	} else {
	    if (-e $absfile) {
		unlink ($absfile);
		$log->debug ("rm_output_file deleted $absfile");
	    } else {
		$log->debug ("rm_output_file could not delete $absfile");
	    }
	}
    }
}

sub rm_output_dir {
    my ($relpath) = @_;

    $log->debug ("rm_output_dir $relpath");

    my $abspath = $OUT_DIR.'/'.$relpath;
    if ($AWS) {
	&rm_output_files ($relpath);
    } else {
	remove_tree ($abspath);
    }
}

######################################################################
sub compare_arrays {
	my ($first, $second) = @_;
	# no warnings;  # silence spurious -w undef complaints
	return 0 unless @$first == @$second;
	for (my $i = 0; $i < @$first; $i++) {
	    return 0 if $first->[$i] ne $second->[$i];
	}
	return 1;
    }  
######################################################################
# Looks at places.txt (list of places).
# If it is different, update it, make it live.
# If we are making a new places.txt, return 1.

sub append_existing_places {
    my ($self, $fq_place, $places) = @_;

    $log->debug ("append_existing_places fq_place $fq_place");

    my $outfile = $OUT_DIR.'/'.$fq_place.'/places.txt';
    if ($fq_place eq '') {
	$outfile = $OUT_DIR.'/places.txt';
    }

    my $place_str = '';
    my @place_list = ();
    foreach my $place (sort keys %$places) {
	$log->debug ("existing places file $outfile place $place");
	$place_str .= "$place\n";
	push (@place_list, $place);
    }

    my $create = 0;
    my $replace = 0;
    if ($AWS) {

	my $response = $aws_connection->get($bucket, $outfile);
	if ($response->http_response->code == 200) {
	    # exists
	    my $place_data = $response->object->data;
	    my (@existing_place_list) = split (/\n/,$place_data);
	    if (! &compare_arrays (\@existing_place_list, \@place_list)) {
		$replace = 1;
	    }

	    $log->debug ("compared old and existing keys fq_place $outfile");
	} else {
	    $create = 1;
	    $log->debug ("old key did not exist fq_place $outfile");
	}

	if ($create || $replace) {
	    my %headers = (
		'Content-Type' => 'text/plain',
		'x-amz-acl' => 'public-read'
		);
	    # outfile == key
	    $response = $aws_connection->put
		( $bucket, $outfile, S3::S3Object->new($place_str),
		  \%headers);
	    if ($response->http_response->code == 200) {
		$log->debug ("Stored place in S3: $outfile");
	    } else {
		$log->fatal ("Failed to store place in S3: $outfile $@ ".
			     $response->http_response->code. " ".
			     $response->http_response->message);
	    }
	} else {
	    $log->debug ("append_existing_places file unchanged $outfile");
	}

    } else {

	if (-e $outfile) {
	    $log->debug ("append_existing_places exists $outfile");
	    open PLACES, "$outfile" or die ("Cannot open $outfile when it exists");

	    my @existing_place_list = ();
	    while (my $line = <PLACES>) {
		chop $line;
		push (@existing_place_list, $line);
	    }
	    close PLACES;
	    if (! &compare_arrays (\@existing_place_list, \@place_list)) {
		$replace = 1;
	    }

	} else {
	    $log->debug ("append_existing_places no file exists $outfile");
	    $create = 1;
	}

	if ($create || $replace) {
	    open OUT, ">$outfile" or die ("Cannot open $outfile");
	    print OUT "$place_str";
	    close OUT;
	    $log->debug ("append_existing_places wrote file $outfile");
	} else {
	    $log->debug ("append_existing_places file unchanged $outfile");
	}

    }

    if ($create) {
	$log->debug ("append_existing_places created $outfile place_str $place_str");
	return 1;
    } else { 
	$log->debug ("append_existing_places did not create $outfile place_str $place_str");
	return 0;
    }


}

######################################################################
# Grab all space names from this area,
# send list to append_existing_places, which returns 1
# if a modification was made.
#
# handle_changed_places_X are all similar.

sub handle_changed_places_space {
    my ($self, $fq_place, $country, $region, $city, $area, $floor) = @_;

    $log->debug ("handle_changed_places_space fq_place $fq_place cnt $country reg $region city $city area $area floor $floor");

    # must be unique (via db constraint)
    $self->{get_existing_space_names_stmt} = $self->{dbi}->prepare_cached
	("SELECT name from locations where country=? and region=? and city=? and area=? and floor=? and is_active=1");

    $self->{get_existing_space_names_stmt}->execute($country,$region,$city,$area,$floor);

    my $existing_places = $self->{get_existing_space_names_stmt}->fetchall_arrayref( { name => 1} );

    my %places = ();

    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{name}} = 1;
    }
    $self->{get_existing_space_names_stmt}->finish();

    #&make_output_dir ($country, $region, $city, $area, $floor);
    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {
	&make_output_dir ($fq_place);
	# seems like we should only propagate change up if creation -- not if exists
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_dir ($fq_place);
	return 1;
    }
}

sub handle_changed_places_floor {
    my ($self, $fq_place, $country, $region, $city, $area) = @_;

    $log->debug ("handle_changed_places_floor fq_place $fq_place cnt $country reg $region city $city area $area");

    $self->{get_existing_floors_stmt} = $self->{dbi}->prepare_cached
	("SELECT floor from locations where country=? and region=? and city=? and area=? and is_active=1 group by floor");

    $self->{get_existing_floors_stmt}->execute($country,$region,$city,$area);

    my $existing_places = $self->{get_existing_floors_stmt}->fetchall_arrayref( { floor => 1} );

    my %places = ();
    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{floor}} = 1;
    }
    $self->{get_existing_floors_stmt}->finish();

    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {
	&make_output_dir ($country.'/'.$region.'/'.$city.'/'.$area);
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_dir ($fq_place);
	return 1;
    }
}

sub handle_changed_places_area {
    my ($self, $fq_place, $country, $region, $city) = @_;

    $log->debug ("handle_changed_places_area fq_place $fq_place cnt $country reg $region city $city");

    $self->{get_existing_areas_stmt} = $self->{dbi}->prepare_cached
	("SELECT area from locations where country=? and region=? and city=? and is_active=1 group by area");

    $self->{get_existing_areas_stmt}->execute($country,$region,$city);

    my $existing_places = $self->{get_existing_areas_stmt}->fetchall_arrayref( { area => 1} );

    my %places = ();
    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{area}} = 1;
    }
    $self->{get_existing_areas_stmt}->finish();

    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {
	&make_output_dir ($country.'/'.$region.'/'.$city);
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_dir ($fq_place);
	return 1;
    }
}

sub handle_changed_places_city {
    my ($self, $fq_place, $country, $region) = @_;

    $log->debug ("handle_changed_places_city fq_place $fq_place cnt $country reg $region");

    $self->{get_existing_cities_stmt} = $self->{dbi}->prepare_cached
	("SELECT city from locations where country=? and region=? and is_active=1 group by city");

    $self->{get_existing_cities_stmt}->execute($country,$region);

    my $existing_places = $self->{get_existing_cities_stmt}->fetchall_arrayref( { city => 1} );

    my %places = ();
    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{city}} = 1;
    }
    $self->{get_existing_cities_stmt}->finish();

    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {
	&make_output_dir ($country.'/'.$region);
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_dir ($fq_place);
	return 1;
    }

}

sub handle_changed_places_region {
    my ($self, $fq_place, $country) = @_;

    $log->debug ("handle_changed_places_region fq_place $fq_place cnt $country");

    $self->{get_existing_regions_stmt} = $self->{dbi}->prepare_cached
	("SELECT region from locations where country=? and is_active=1 group by region");

    $self->{get_existing_regions_stmt}->execute($country);

    my $existing_places = $self->{get_existing_regions_stmt}->fetchall_arrayref( { region => 1} );

    my %places = ();
    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{region}} = 1;
    }
    $self->{get_existing_regions_stmt}->finish();

    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {
	&make_output_dir ($country);
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_dir ($fq_place);
	return 1;
    }


}

sub handle_changed_places_country {
    my ($self, $fq_place) = @_;

    $log->debug ("handle_changed_places_country fq_place $fq_place");

    $self->{get_existing_country_stmt} = $self->{dbi}->prepare_cached
	("SELECT country from locations where is_active=1 group by country");

    $self->{get_existing_country_stmt}->execute();

    my $existing_places = $self->{get_existing_country_stmt}->fetchall_arrayref( { country => 1} );

    my %places = ();
    for (my $p = 0; $p <= $#$existing_places; $p++) {
	$places{$existing_places->[$p]->{country}} = 1;
    }
    $self->{get_existing_country_stmt}->finish();

    $log->debug ("existing place count ".$#$existing_places);
    if ($#$existing_places >= 0) {    
	return &append_existing_places ($self, $fq_place, \%places);
    } else {
	&rm_output_files ($fq_place);
	return 1;
    }
}



######################################################################

sub handle_changed_places {
    my ($self) = @_;

    my $get_changed_places_sql = "SELECT id, country, region, city, area, floor, name from locations";
    if (!$reset) {
	$get_changed_places_sql .= " where is_changed=1";
    }

    $self->{get_changed_places_stmt} = $self->{dbi}->prepare_cached
	($get_changed_places_sql);

    $self->{get_changed_places_stmt}->execute();

    # are there any changed places since last time
    my @changed_place_ids = ();
    if ($self->{get_changed_places_stmt}->rows > 0) {

	my %places = ();

	my $changed_places = $self->{get_changed_places_stmt}->fetchall_arrayref(
	    { id => 1, country => 1, region => 1, city => 1, 
	      area => 1, floor => 1, name => 1 } );
	
	for (my $p = 0; $p <= $#$changed_places; $p++) {
	    my $changed_place = $changed_places->[$p];
	    my $fq_name = $changed_place->{country}.'/'.
		$changed_place->{region}.'/'.
		$changed_place->{city}.'/'.
		$changed_place->{area}.'/'.
		$changed_place->{floor}.'/'.
		$changed_place->{name};
	    
	    $log->debug 
		("changed place id=".$changed_place->{id}." ".$fq_name);

	    push (@changed_place_ids, $changed_place->{id});

	    # Handle each branch of the place tree no more than once.
	    # Start from the bottom up, so we can break out when no more differences exist.

	    my $fq_floor = $changed_place->{country}.'/'.$changed_place->{region}.'/'.
		$changed_place->{city}.'/'.$changed_place->{area}.'/'.$changed_place->{floor};
	    my $fq_area = $changed_place->{country}.'/'.$changed_place->{region}.'/'.
		$changed_place->{city}.'/'.$changed_place->{area};
	    my $fq_city = $changed_place->{country}.'/'.$changed_place->{region}.'/'.
		$changed_place->{city};
	    my $fq_region = $changed_place->{country}.'/'.$changed_place->{region};
	    my $fq_country = $changed_place->{country};
	    my $fq_root = '';

	    # Remember each fq_space, so we only process it once (this time around)

	    # Each handle_X returns 1 if we need to continue up the tree.
	    if (!defined ($places{$fq_floor}) &&
		&handle_changed_places_space 
		($self, $fq_floor, $changed_place->{country}, $changed_place->{region},
		 $changed_place->{city}, $changed_place->{area}, $changed_place->{floor})) {

		if (!defined ($places{$fq_area}) &&
		    &handle_changed_places_floor 
		    ($self, $fq_area, $changed_place->{country}, $changed_place->{region},
		     $changed_place->{city}, $changed_place->{area})) {

		    if (!defined ($places{$fq_city}) &&
			&handle_changed_places_area 
			($self, $fq_city, $changed_place->{country}, $changed_place->{region},
			 $changed_place->{city})) {

			if (!defined ($places{$fq_region}) &&
			    &handle_changed_places_city 
			    ($self, $fq_region, $changed_place->{country}, $changed_place->{region})) {

			    if (!defined ($places{$fq_country}) &&
				&handle_changed_places_region 
				($self, $fq_country, $changed_place->{country})) {

				if (!defined ($places{$fq_root}) &&
				    &handle_changed_places_country 
				    ($self, $fq_root)) {
				    
				}
				$places{$fq_root} = 1;

			    }
			    $places{$fq_country} = 1;

			}
			$places{$fq_region} = 1;

		    }
		    $places{$fq_city} = 1;

		}
		$places{$fq_area} = 1;
	    }
	    $places{$fq_floor} = 1;
	}

    }

    $self->{get_changed_places_stmt}->finish();    

    # clear the dirty bit in the binds we have just handled
    foreach my $changed_place_id (@changed_place_ids) {
	$log->debug ("changed_place_id $changed_place_id");
	$self->{set_changed_place_id_false_stmt} = $self->{dbi}->prepare_cached
	    ("UPDATE locations set is_changed=0 WHERE id=?");
	$self->{set_changed_place_id_false_stmt}->execute ($changed_place_id);
	$self->{set_changed_place_id_false_stmt}->finish ();

    }

    $self->{dbi}->commit;


}



######################################################################

sub run {
    my ($self) = @_;

    # connect to db
    &db_connect ($self);

    $self->{dbi}->{AutoCommit} = 0;
    $self->{dbi}->{RaiseError} = 1;

    eval {

	&handle_new_binds ($self);

	&handle_changed_places ($self);

    };
    if ($@) {

	$log->warn ("Transaction aborted because $@");
	$last_error = "$@";
	eval { $self->{dbi}->rollback };

    }

}

######################################################################

sub db_connect {
    my ($self) = @_;

    #$log->debug ("Connecting to database ".$dsn);
    
    $self->{dbi} = DBI->connect_cached
	($dsn, $db_user, $db_pw, 
	 { RaiseError => 1});
    
    if (!defined ($self->{dbi})) {
	$log->fatal ("Could not connect to database ".$dsn);
    }

}

######################################################################

sub db_disconnect {
    my $self = shift;

    $log->info ("Disconnecting from database ".$dsn);

    if (defined ($self) && defined ($self->{dbi})) {
	$self->{dbi}->disconnect;
    }

}

######################################################################

sub shutdown_hook {
    my $self = shift;

    &db_disconnect ($self);
}

######################################################################

sub status_listener {
    my $port = &getProperty ("port", 4560);

    my $sock = new IO::Socket::INET 
	( LocalPort => $port, Proto => 'tcp',
	  Listen => 1, Reuse => 1, ); 

    die "Could not create socket: $!\n" unless $sock;

    $log->info ("Status listener port $port");

    while (1) {
	my $new_sock = $sock->accept(); 
	my $line = <$new_sock>;
	print "received $line";
	print $new_sock "$last_error\n";
	$last_error = "OK";
	close ($new_sock);
    } 

    #close($sock);

    $log->warn ("Status listener exited.");

}

######################################################################

sub main {
    my %self = ();
    &init (\%self);
    my $thr = threads->create(\&status_listener);
    my $iterations = 0;
    while (1) {
	$iterations++;
	&run (\%self);
	#$log->info ("I Ran $iterations");
	if ($reset) { exit (0); }
	sleep ($BUILD_PERIOD);
    }
}

&main ();

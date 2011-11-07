#!/usr/bin/perl

# Mole - Mobile Organic Localisation Engine
# Copyright (C) 2010 Nokia Corporation.  All rights reserved.

use strict;
use Data::Dumper;
use DBI;
use Log::Log4perl;

use Getopt::Std;
use Config::Tiny;


# Database
my $dsn = '';
my $db_user = '';
my $db_pw = '';

my $cfg = Config::Tiny->new;

my $log;
Log::Log4perl::init ('log-correlation.cfg');
$log = Log::Log4perl->get_logger;

my $AVG_STDDEV = 1.0;

my %bind2name = ();

######################################################################

sub init {
    my ($self) = @_;

    my $cfg_file = 'builder.cfg';

    my %para = ();
    getopts('c:', \%para);
    if (defined($para{'c'})) {
	$cfg_file = $para{'c'};
    } else {
	warn ("Using config file $cfg_file\n");
    }

    $cfg = Config::Tiny->read ($cfg_file);
    if (!defined ($cfg)) {
	die ("Cannot open config file $cfg_file.  Try builder.pl -c config_file");
    }

    $dsn = &getProperty ('dsn', 'DBI:mysql:database=mole;host=localhost');
    $db_user = &getProperty ('db_user', 'moleuser');
    $db_pw = &getProperty ('db_pw', 'molepw');

    $log->info ("Starting Correlation");

    # connect to db
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

sub get_bind_map {
    my ($self) = @_;

    $self->{get_bind_map_stmt} = $self->{dbi}->prepare_cached
	("select area, name, binds.id as bind_id, device_model, bind_stamp from locations, binds where binds.location_id=locations.id");

    $self->{get_bind_map_stmt}->execute ();
    
    my $bind_maps = $self->{get_bind_map_stmt}->fetchall_arrayref(
	{ area => 1, name => 1, bind_id => 1, device_model => 1}, bind_stamp);

    for (my $b = 0; $b <= $#$bind_maps; $b++) {
	my $bind_map = $bind_maps->[$b];
	my $model = $bind_map->{device_model};
	# deal with "Computer/armv7l /Nokia" vs "
	if ($model =~ /Nokia/ || $model =~ /pre-qt/) {
	    $model = 'N900';
	} else {
	    $model = 'laptop'
	}

	my $name = $bind_map->{area}.'-'.$bind_map->{name}.'-'.$model;
	$name =~ s/\s+/-/g;
	$bind2name{$bind_map->{bind_id}} = $name;
    }


    $self->{get_bind_map_stmt}->finish ();
}

######################################################################

sub process_bind {
    my ($self, $bind_id) = @_;

    ####
    # figure out how many scans there where in this bind
    # to generate the response rate

    my $scans = 0;
    $self->{get_scancount_stmt} = $self->{dbi}->prepare_cached
	("SELECT count(*) as count ".
	 "from ap_readings where bind_id=? ".
	 "group by stamp");

    $self->{get_scancount_stmt}->execute ($bind_id);

    my $scancounts = $self->{get_scancount_stmt}->fetchall_arrayref(
	{ count => 1 });
    foreach my $scancount (@$scancounts) {
	$scans++;
    }

    $self->{get_scancount_stmt}->finish ();

    if ($scans == 0) {
	$log->debug ("skipping bind $bind_id because no scans");	
	return;
    }

    ####

    # from gaussian overlap generation
    $self->{get_readings_stmt} = $self->{dbi}->prepare_cached
	("SELECT bssid, count(level) as count, avg(level) as avg, ".
	 "stddev(level) as stddev, min(level) as min, max(level) as max ".
	 "from ap_readings where bind_id=? ".
	 "group by bssid");

    $self->{get_readings_stmt}->execute ($bind_id);
    
    my $ap_stats = $self->{get_readings_stmt}->fetchall_arrayref(
	{ bssid => 1, count => 1, avg => 1, stddev => 1, min => 1, max => 1 });


    # for some unknown reason, "select lower(bssid)" does not give
    # this same result...

    foreach my $ap_stat (@$ap_stats) {
	$ap_stat->{bssid} = lc($ap_stat->{bssid});
    }

    $log->debug ("ap stats count ".$#$ap_stats);

    my $total_count = 0;
    for (my $a = 0; $a <= $#$ap_stats; $a++) {
	my $ap_stat = $ap_stats->[$a];
	$total_count += $ap_stat->{count};
	$log->debug ("a $ap_stat->{bssid} $a count ".$ap_stat->{count}." total $total_count");

    }

    # sanity check
    if ($total_count <= 0) {
	die ("count $total_count bind $bind_id"); 
    }

    my $rrBelowCheck = 0;
    my $rrAboveCheck = 1;
    for (my $a = 0; $a <= $#$ap_stats; $a++) {

	my $ap_stat = $ap_stats->[$a];
	my $weight = $ap_stat->{count} / $total_count;
	$log->debug ("weight ap ".$ap_stat->{bssid}." $weight");
	$ap_stat->{weight} = $weight;

	# pull stddev toward a common value, based on number of readings
	my $stddev = $ap_stat->{stddev};
	my $pow = $ap_stat->{count}**1;
	my $b_stddev = (($pow-1) * $stddev + $AVG_STDDEV) / $pow;
	$ap_stat->{stddev} = $b_stddev;

	$ap_stat->{response_rate} = $ap_stat->{count} / $scans;
	if ($ap_stat->{response_rate} < 1.0) {
	    $rrBelowCheck = 1;
	}
	if ($ap_stat->{response_rate} > 1.0) {
	    $rrAboveCheck = 0;
	}
    }

    # some scans were weird and all of the APs were heard every time
    if ($rrAboveCheck && $rrBelowCheck) {
	for (my $a = 0; $a <= $#$ap_stats; $a++) {

	    my $name = $bind2name{$bind_id};
	    if (!defined($name)) { die ("No name for bind $bind_id"); }



	    my $ap_stat = $ap_stats->[$a];
	    if ($ap_stat->{bssid} =~ /^\s*$/) {
		warn ("skipping empty bssid in bind $bind_id");
		next;
	    }

	    my $out = sprintf ("$bind_id $ap_stat->{bssid} w %8.5f rr %8.5f a %8.5f sd %8.5f mi %8.5f ma %8.5f $name",
			       $ap_stat->{weight}, $ap_stat->{response_rate},
			       $ap_stat->{avg},$ap_stat->{stddev},$ap_stat->{min},$ap_stat->{max});
	    print "$out\n";
	}
    }

    $self->{get_readings_stmt}->finish ();

    $log->debug ("END processing bind $bind_id");

}


######################################################################

sub run {
    my ($self) = @_;

    $self->{dbi}->{AutoCommit} = 0;
    $self->{dbi}->{RaiseError} = 1;



    eval {

	&handle_new_binds ($self);

	&handle_new_places ($self);

    };
    if ($@) {

	$log->warn ("Transaction aborted because $@");
	eval { $self->{dbi}->rollback };

    }

}

######################################################################

sub db_connect {
    my ($self) = @_;

    $log->info ("Connecting to database ".$dsn);
    
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

sub main {
    my %self = ();
    &init (\%self);
    &get_bind_map (\%self);
    #&process_bind (\%self, 37);
    #exit (0);

    my $bindCount = 140;
    #my $bindCount = 1;

    for (my $i = 1; $i <= $bindCount; $i++) {
	&process_bind (\%self, $i);
    }
}

&main ();

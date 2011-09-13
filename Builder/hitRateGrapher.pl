#!/usr/bin/perl

# Mole - Mobile Organic Localisation Engine
# Copyright (C) 2010-2011 Nokia Corporation.  All rights reserved.
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
use Data::Dumper;
use Log::Log4perl;
use DBI;
use Getopt::Std;
use Config::Tiny;
use Date::Format qw(time2str);
use Date::Parse qw(str2time);
use POSIX qw(ceil floor);
use S3::S3Object;
use S3::AWSAuthConnection;
use S3::QueryStringAuthGenerator;
#use FileHandle;
#use IPC::Open2;

# ubuntu packages: libtimedate-perl, libdate-calc-perl, libgd2-xpm-dev

# Output location
my $default_outfile = '/var/www/hitrate/index.html';

# Log
my $log;

# stores binds as key {area,relative fractional minute} value {hit(1)/miss(0)}
my %gBinds = ();

# stores hitrate as key {area, relative minute} value {hitrate}
# may have holes if there aren't any binds during the averaging interval.
# hitrate is averaged over the previous five minutes
my %gHitrate = ();

# read-in configuration data
my $cfg = Config::Tiny->new;
my $last_error = "OK";

my $gpHeader = '';
my $gpPlot = '';

# Requires gnuplot >= 4.5
# Make sure javascript files are in right dir (see jsDir
my $gnuplot_exe = '/usr/local/bin/gnuplot';

######################################################################

# Amazon S3/Cloudfront
my $AWS = 0;
#my $AWS_ACCESS_KEY_ID     = '';
#my $AWS_SECRET_ACCESS_KEY = '';
my $aws_connection;
my $bucket = '';

######################################################################

sub usage {
    my ($msg) = @_;
    my $usageMsg = "hitRateGrapher.pl\n".
	"-c config file\n".
	"-a root of spaces to graph\n".
	"-s start time (optional)\n".
	"-e end time (optional)\n".
	"-f html output file\n".
	"-j gnuplot javascript dir\n";
    die ("$msg\n$usageMsg\n");
}

######################################################################

sub init {
    my ($self) = @_;
    my $cfg_file = 'builder.cfg';
    my %para = ();
    getopts('c:a:s:e:f:j:', \%para);
    if (defined($para{'c'})) {
	$cfg_file = $para{'c'};
    } else {
	warn ("Using config file $cfg_file\n");
    }

    if (defined($para{'a'})) {
	$self->{fqArea} = $para{'a'};

	# Parse the fully-qualified area
	if ($self->{fqArea} =~ /^(.+?)\/(.+?)\/(.+?)\/(.+?)$/) {
	    $self->{country} = $1;
	    $self->{region} = $2;
	    $self->{city} = $3;
	    $self->{area} = $4;
	} else {
	    &usage ("Could not parse area $self->{fqArea}.  Please check it.");
	}
    }

    if (defined ($para{'j'})) {
	$self->{jsDir} = $para{'j'};
    } else {
	$self->{jsDir} = 'http://mole.research.nokia.com/js';
    }

    my $currentStampStr = time2str ("%Y-%m-%d %X", time);
    if (defined($para{'s'})) {
	$self->{startstamp} = $para{'s'};
    } else {
	$self->{startstamp} = $currentStampStr;
    }

    $self->{continuous} = 0;
    if (defined($para{'e'})) {
	$self->{endstamp} = $para{'e'};
    } else {
	$self->{continuous} = 1;
	$self->{endstamp} = $currentStampStr;
    }



    # Parse the config file (for db connect info)

    $cfg = Config::Tiny->read ($cfg_file);
    if (!defined ($cfg)) {
	&usage ("Cannot open config file $cfg_file.");
    }

    $self->{dsn} = &getProperty ('dsn', 'DBI:mysql:database=mole;host=localhost');
    $self->{db_user} = &getProperty ('db_user', 'moleuser');
    $self->{db_pw} = &getProperty ('db_pw', 'molepw');

    $self->{outfile} = &getProperty ('hitrate_dir', $default_outfile);
    if (defined($para{'f'})) {
	$self->{outfile} = $para{'f'};
    }

    my $log_cfg_file = &getProperty ('hitrate_log_cfg', 'hitrate_log.cfg');

    Log::Log4perl::init ($log_cfg_file);
    $log = Log::Log4perl->get_logger;
    #$log->logwarn();
    #$log->logdie();

    $log->info ("Starting Hit Rate Grapher");

    # connect to db
    $log->info ("Connecting to database ".$self->{dsn});
    &db_connect ($self);

    $gpHeader = <<EOHEADER;
RED = "#FF0000"
GREEN = "#008000"
BLACK = "#000000"
set style arrow 1 nohead ls 3 lc rgbcolor RED
set style arrow 2 nohead ls 3 lc rgbcolor GREEN
set style line 1 lw 2 lc rgbcolor BLACK
set grid

set ylabel 'Hit Rate'
set xlabel 'Time (Minutes)'
set format y "%1.1f"
EOHEADER

    $gpPlot = "plot '-' notitle with lines ls 1\n";

    $AWS = &getProperty ('aws', 0);
    if ($AWS) {
	&initAWS ($self);
    }

}

######################################################################

sub initAWS {
    my ($self) = @_;

    my $AWS_ACCESS_KEY_ID = &getProperty ('aws_key', '');
    my $AWS_SECRET_ACCESS_KEY = &getProperty ('aws_secret', '');
    if ($AWS) {
	$aws_connection = S3::AWSAuthConnection->new($AWS_ACCESS_KEY_ID,
						     $AWS_SECRET_ACCESS_KEY);
	if (!defined ($aws_connection)) {
	    $log->fatal ("Connection failed $@");
	}
    }
    $bucket = &getProperty ('bucket', 'mybucket');
    $self->{outfile} = 'hitrate.html';
}

######################################################################

sub initMostRecentArea {
    my ($self) = @_;
    $self->{get_most_recent_location_stmt} = $self->{dbi}->prepare_cached
	("SELECT country,region,city,area from binds, locations ".
	 "where binds.id = (select MAX(id) from binds) ".
	 "and binds.location_id = locations.id");
    $self->{get_most_recent_location_stmt}->execute ();
    if ($self->{get_most_recent_location_stmt}->rows () == 0) {
	&usage ("No locations given and none found in the database");
    }
    my @location = $self->{get_most_recent_location_stmt}->fetchrow_array();
    $self->{fqArea} = $location[0].'/'.$location[1].'/'.$location[2].'/'.$location[3];
    $self->{country} = $location[0];
    $self->{region} = $location[1];
    $self->{city} = $location[2];
    $self->{area} = $location[3];
    $self->{get_most_recent_location_stmt}->finish ();
    $log->debug ("Using location ".$self->{fqArea});
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

sub rebuild_graphs {
    my ($self) = @_;

    # tried doing this with bi-directional pipes
    # and was getting some weirdness on AWS

    my $gpFile = '/tmp/hitrate.gp';
    #my $gpResA = '/tmp/gp1.html';
    #my $gpResB = '/tmp/gp2.html';

    my $gp = '';

    $gp .= "set terminal canvas title 'Hit Rates for ".$self->{fqArea}.'\' '.
	'jsdir \''.$self->{jsDir}."'\n";
    #$gp .= "set output \"$gpResA\"\n";
    # TODO put date/time more clearly on graph
    $gp .= $gpHeader;

    my $lowY = -0.05;
    my $highY = 1.05;
    $gp .= "set yrange [$lowY:$highY]\n";

    my $plotCount = scalar keys %gHitrate;
    my $columnCount = 2;
    my $rowCount = ceil ($plotCount / $columnCount);

    $gp .= "set multiplot layout $rowCount, $columnCount title 'Hit Rates for $self->{fqArea}'\n";
    $gp .= 'set xrange [0:'.($self->{elapsed}+1)."]\n";

    my @aggCount = ();
    my @aggSum = ();
    foreach my $area (sort keys %gHitrate) {
	my $subGP = '';
	my $arrowCount = 1;
	$subGP .= "\nset title '$area'\n";

	my $binds = $gBinds{$area};
	foreach my $elapsed (sort {$a <=> $b} keys %{$binds}) {
	    my $hit = $binds->{$elapsed};
	    my $arrow = "set arrow $arrowCount from $elapsed,$lowY to $elapsed,$highY";
	    if ($hit) {
		$subGP .= "$arrow as 2\n";
	    } else {
		$subGP .= "$arrow as 1\n";
	    }
	    $arrowCount++;
	}
	$subGP .= "\n";

	$subGP .= $gpPlot;
	my $currentHitRate = -1.;
	my $stamp2hitrate = $gHitrate{$area};
	for (my $s = 0; $s <= $self->{elapsed}; $s++) {
	    if (exists ($stamp2hitrate->{$s})) {
		my $hitrate = $stamp2hitrate->{$s};
		$currentHitRate = $hitrate;
	    }
	    if ($currentHitRate != -1.) {
		# append to gnuplot
		$subGP .= "$s $currentHitRate\n";
		$aggSum[$s] += $currentHitRate;
		$aggCount[$s]++;
	    }
	}
	$subGP .= "e\n";

	# delete our own arrows
	for (my $i = 1; $i < $arrowCount; $i++) {
	    $subGP .= "unset arrow $i\n";
	}

	$gp .= $subGP;

    }

     for (my $s = 0; $s <= $self->{elapsed}; $s++) {
	if (exists ($aggSum[$s])) {
	    my $avgHitrate = $aggSum[$s] / $aggCount[$s];
	    $log->debug ("avg at time $s is $avgHitrate");
	    #print "$s $avgHitrate\n";
	}
    }

    open GP, ">$gpFile" or die "Cannot open gp file $gpFile";
    print GP $gp;
    flush GP;
    close GP;


    local *PIPE;
    open PIPE, "gnuplot $gpFile|" or die ("Cannot open gnuplot");
    my $html = '';
    my $insertedRefresh = 0;
    while (my $line = <PIPE>) {
	# a bit dangerous b/c it could in theory be split across multiple lines, but gnuplot doesn't seem to do this
	if (!$insertedRefresh && $line =~ /<meta/) {
	    $html .= '<meta http-equiv="refresh" content="60" />'."\n";
	    $insertedRefresh = 1;
	}
	$html .= $line;
    }
    close PIPE;

    if ($insertedRefresh == 0) {
	die ("Could not insert auto refresh into gnuplot's html");
    }

    if ($AWS) {
	my %headers = (
	    'Content-Type' => 'text/html',
	    'x-amz-acl' => 'public-read'
	    );
	my $response = $aws_connection->put
	    ($bucket, $self->{outfile}, S3::S3Object->new($html), \%headers);
	if ($response->http_response->code == 200) {
	    $log->debug ("Stored/removed html in S3: $self->{outfile}");
	} else {
	    $log->fatal ("Failed to store html in S3: $self->{outfile} $@ ".
			 $response->http_response->code. " ".
			 $response->http_response->message);
	}

    } else {
	open OUT, ">$self->{outfile}" or die "Cannot open $self->{outfile}";
	print OUT $html;
	close OUT;
    }

    $log->info ("Renewed graph");

}

######################################################################

sub extract_recent_binds {
    my ($self) = @_;

    my $current_time_abs = &add_minutes_to_formatted_date 
	($self->{startstamp}, $self->{elapsed});
    $log->debug ("current time $current_time_abs elapsed $self->{elapsed}");

    $self->{get_binds_stmt} = $self->{dbi}->prepare_cached
	("SELECT bind_stamp, location_id, est_location_id, ".
	 "floor, name from binds, locations ".
	 "where binds.location_id=locations.id and ".
	 "country=? and region=? and city=? and area=? ".
	 "and locations.is_active=1 ".
	 "and source != 'remove' ".
	 "and bind_stamp > date_sub(?,interval 5 minute) ".
	 "and bind_stamp <= ?".
	 "order by floor, name");
    $self->{get_binds_stmt}->execute 
	($self->{country}, $self->{region},
	 $self->{city}, $self->{area}, $current_time_abs, $current_time_abs);

    $log->debug ("found rows ".$self->{get_binds_stmt}->rows);

    my $binds = $self->{get_binds_stmt}->fetchall_arrayref(
	{ bind_stamp => 1, location_id => 1, est_location_id => 1, 
	  floor => 1, name => 1 } );
    my %accessCount = ();
    my %hitCount = ();

    for (my $b = 0; $b <= $#$binds; $b++) {
	my $bind = $binds->[$b];

	my $ds = Dumper ($bind);
	$log->debug ("ds $ds");

	#my $area = $self->{area}.'/'.$bind->{floor}.'/'.$bind->{name};
	my $area = $bind->{floor}.'/'.$bind->{name};

	$accessCount{$area}++;
	my $hit = 0;
	if (defined($bind->{est_location_id}) &&
	    ($bind->{location_id} == $bind->{est_location_id})) {
	    $hitCount{$area}++;
	    $hit = 1;
	    $log->debug ("added hitCount $hit $area");
	}

	my $elapsed = &compute_elapsed_time_fractional_min 
	    ($bind->{bind_stamp}, $self->{startstamp});
	# if it's within last minute
	my $bindElapsed = $self->{elapsed} - $elapsed;
	if ($bindElapsed < 1 && $bindElapsed >= 0) {
	    if (!exists ($gBinds{$area})) {
		my %stamp2binds = ();
		$gBinds{$area} = \%stamp2binds;
	    }
	    my $stamp2binds = $gBinds{$area};
	    $stamp2binds->{$elapsed} = $hit;
	    $log->debug ("added bind hit $hit area $area elapsed $elapsed");
	}

    }
    $self->{get_binds_stmt}->finish ();

    foreach my $area (keys %accessCount) {
	my $access = $accessCount{$area};
	my $hits = 0;
	if (defined ($hitCount{$area})) {
	    $hits = $hitCount{$area};
	}
	my $currentHitrate = $hits / $access;
	if (!exists ($gHitrate{$area})) {
	    my %stamp2hitrate = ();
	    $gHitrate{$area} = \%stamp2hitrate
	}
	my $stamp2hitrate = $gHitrate{$area};
	$stamp2hitrate->{$self->{elapsed}} = $currentHitrate;
	$log->debug ("hitrate $currentHitrate elapsed $self->{elapsed} area $area");
    }
}


######################################################################

sub extract_recent_binds_xact {
    my ($self) = @_;

    # connect to db
    &db_connect ($self);

    $self->{dbi}->{AutoCommit} = 0;
    $self->{dbi}->{RaiseError} = 1;

    eval {

	&extract_recent_binds ($self);

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
	($self->{dsn}, $self->{db_user}, $self->{db_pw}, 
	 { RaiseError => 1});
    
    if (!defined ($self->{dbi})) {
	$log->fatal ("Could not connect to database ".$self->{dsn});
    }

}

######################################################################

sub db_disconnect {
    my $self = shift;

    $log->info ("Disconnecting from database ".$self->{dsn});

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

sub compute_time_difference_in_min {
    my ($laterStr, $earlierStr) = @_;
    my $laterTime = str2time ($laterStr);
    my $earlierTime = str2time ($earlierStr);
    die ("Bad start and end times given") if ($earlierTime > $laterTime);
    my $diff = $laterTime - $earlierTime;
    #$log->debug ("dA $diff e $earlierStr l $laterStr");
    $diff = floor ($diff / 60);
    #$log->debug ("dB $diff");
    return $diff;
}

######################################################################

sub compute_elapsed_time_fractional_min {
    my ($stampStr, $startTimeStr) = @_;

    my $startTime = str2time ($startTimeStr);
    my $stamp = str2time ($stampStr);
    my $elapsedSec = $stamp - $startTime;
    my $elapsedFrac = $elapsedSec/60;
    $log->debug ("compute_elapsed_time_fractional_min $stampStr $startTimeStr $elapsedSec $elapsedFrac");
    return $elapsedFrac;
}

######################################################################

sub add_minutes_to_formatted_date {
    my ($dateStr, $minutes) = @_;
    my $dateSec = str2time ($dateStr);
    $dateSec += ($minutes*60);
    my $newDateStr = time2str ("%Y-%m-%d %X", $dateSec);
    return $newDateStr;
}

######################################################################

sub main {
    my %self = ();
    &init (\%self);

    if (!exists ($self{fqArea})) {
	&initMostRecentArea (\%self);
    }

    $self{elapsed} = 0;

    $self{dbi}->{RaiseError} = 1;
    my $preDuration = 0;

    # if requested, we rewind to the past and roll forward
    $preDuration = &compute_time_difference_in_min 
	($self{endstamp},$self{startstamp});
    $log->debug ("preD $preDuration start $self{startstamp} end $self{endstamp}");
    for (; $self{elapsed} <= $preDuration; $self{elapsed}++ ) {
	&extract_recent_binds_xact (\%self);
    }
    if ($preDuration > 0) {
	&rebuild_graphs (\%self);
    }

    # if no endstamp was given, we keep producing graphs every minute
    if ($self{continuous} == 1) {
	while (1) {
	    &extract_recent_binds_xact (\%self);
	    &rebuild_graphs (\%self);
	    sleep (60);
	    $self{elapsed}++;
	}
    }
}

&main ();

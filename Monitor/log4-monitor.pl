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



# Basic monitor.
# Works with log4j / log4perl socket logging the server applications.
# They are intended to be configured to send out error messages
# and periodic heartbeats.

# This listens for errors and no activity.
# If either of these conditions occurs, it mails out the error.

# This kind of setup is useful for monitoring a server app
# that is running on e.g. AWS where smtp is blocked.


use strict;

use IO::Socket;
use threads;
use threads::shared;

use warnings;
use Email::Sender::Simple qw(sendmail);
use Email::Simple;
use Email::Simple::Creator;

use Getopt::Std;
use Config::Tiny;


######################################################################
# default parameters

# what is max heartbeat time from server app
my $max_elapsed = 0;

# how many lines to send with error report
my $max_line_count = 20;

######################################################################

my $g_stamp :shared;
$g_stamp = time;

my @lines : shared;
@lines = ();

my $cfg = Config::Tiny->new;

######################################################################

sub monitor_socket {

    print "monitoring thread started max_elapsed=$max_elapsed\n";

    while (1) {

	my $elapsed = 0;
	
	{ 
	    lock($g_stamp);
	    $elapsed = time - $g_stamp;
	}

	sleep (10);

	print "monitor awake after $elapsed\n";

	if ($elapsed >= $max_elapsed) {
	    my $stamp_str = &time_to_str (time);
	    &send_email ("$stamp_str Exiting monitor due to timeout elapsed $elapsed");
	    exit (0);
	}

    }

}

######################################################################
# main listener

sub app_listener {

    my $host = &getProperty ("host", "localhost");
    my $port = &getProperty ("port", 4560);


    my $sock = new IO::Socket::INET ( LocalHost => $host, LocalPort => $port, Proto => 'tcp', Listen => 1, Reuse => 1, ); 

    die "Could not create socket: $!\n" unless $sock;

    my $new_sock = $sock->accept(); 
    while(my $line = <$new_sock>) { 
	
	my $stamp = time;
	my $stamp_str = &time_to_str ($stamp);

	my $out = "$stamp_str $line"; 
	print "received $out";

	push (@lines, $out);

	if ($#lines > $max_line_count) {
	    shift (@lines);
	}

	{ 
	    lock($g_stamp);
	    $g_stamp = $stamp;
	}


    } 
    close($sock);

    my $stamp_str2 = &time_to_str (time);
    &send_email ("$stamp_str2 Exiting monitor due to disconnection");

}

######################################################################

sub send_email {
    my ($msg) = @_;
    push (@lines, $msg);

    print "send_email\n";

    my $app = &getProperty ("app", "application");
    my $to = &getProperty ("to", "to");
    my $from = &getProperty ("from", "from");
    my $subject_prefix = &getProperty ("subject_prefix", "[log4]");
    my $body_header = &getProperty ("body_header", "Problem with $app\n\n");
    my $body_footer = &getProperty ("body_footer", "\n\nEOM\n");

    my $body = "$body_header";
    foreach my $line (@lines) {
	$body .= "$line\n";
    }
    $body .= "$body_footer";

    my $email = Email::Simple->create(
	header => [
	    To    => $to,
	    From      => $from,
	    Subject => "$subject_prefix $app",
	],
	body => "$body",
	);

    sendmail($email);

    print "body $body\n";

}

######################################################################

sub time_to_str {
    my ($stamp) = @_;

    my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime($stamp);
    my $stamp_str = sprintf 
	("%4d-%02d-%02d %02d:%02d:%02d\n",
	 $year+1900,$mon+1,$mday,$hour,$min,$sec);
    return $stamp_str;
}

######################################################################

sub read_config {

    my %para = ();
    getopts('c:', \%para);
    my $cfg_file = 'log4-monitor.cfg';
    if (defined($para{'c'})) {
	$cfg_file = $para{'c'};
    } else {
	warn ("Using config file $cfg_file\n");
    }

    $cfg = Config::Tiny->read ($cfg_file);
    if (!defined ($cfg)) {
	die ("Cannot open config file $cfg_file.  Try log4-monitor.pl -c config_file");
    }

    $max_elapsed = &getProperty ("max_elapsed", $max_elapsed);

    $max_line_count = &getProperty ("max_line_count", $max_line_count);

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

sub main {

    &read_config ();

    my $thr = threads->create(\&monitor_socket);

    &app_listener ();

}

&main ();

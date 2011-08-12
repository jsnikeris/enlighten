#!/usr/bin/perl -w

# Recursively looks through directories, zips, ears, wars, and jars
# for files which match the given regular expression
#
# By: Joe Snikeris <joe@snikeris.com>

package Zip_Search;

use strict;
use Archive::Extract;
use File::Path;

our @EXTENSIONS = ("zip", "ear", "war", "jar");

if ($#ARGV < 0) {
    print "usage: ./zip_search.pl <file-to-look-for> <directory-to-look-in>\n";
    exit 1;
}

my $query = $ARGV[0];
my $dir = $ARGV[1];

search($dir);

sub search {
    my ($folder) = @_;

    opendir(my $dh, $folder);
    while (defined(my $file = readdir($dh))) {
        next if $file =~ /^\.\.?$/;                   # skip . and ..
        next if $file =~ /^\._/;                      # skip ._*
        if (opendir(my $dh2, "$folder/$file")) {      # folder?
            closedir($dh2);
            search("$folder/$file");
        } elsif (unzippable($file)) {                 # zip?
            my $zip_folder = unzip("$folder/$file");
            search($zip_folder);
            File::Path->remove_tree($zip_folder);
        } elsif ($file =~ $query) {
            print "$folder/$file\n";
        }
    }
    closedir($dh);
}

sub unzip {
    my ($file) = @_;
    my $extension = get_extension($file);
    my $folder = $file;
    $folder =~ s/\.$extension//;    # strip extension

    my $ae = Archive::Extract->new(archive => $file, type => 'zip');
    $ae->extract(to => $folder);
    return $folder;
}

sub unzippable {
    my ($file) = @_;
    return grep {uc($_) eq uc(get_extension($file))} @EXTENSIONS;
}

sub get_extension {
    my ($file) = @_;
    my @file_split = split(/\./, $file);
    return $file_split[-1];
}
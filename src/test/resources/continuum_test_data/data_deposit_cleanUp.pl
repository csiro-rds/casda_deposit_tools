#!/usr/bin/perl 
# Brooke Smith (smi9b6) 18/9/14
# Written to replace the manual steps in https://jira.csiro.au/browse/CASDA-2063?focusedCommentId=244582
# Because manual steps are not ideal when  you can script them, and the cont_name had LOTS of names
# that were > 15 chars so spent so much time changing, running, seeing error & repeat.

use XML::Parser;

use strict;
use warnings;
 
# initialize the parser
my $parser = XML::Parser->new( Handlers => 
                                     {
                                      Start=>\&handle_start,
                                      End=>\&handle_end,
				      Char=>\&handle_char,
                                     });
$parser->parsefile( shift @ARGV );
 
my @element_stack;                # remember which elements are open
my $cellNo;
my $nameCellNo=2;
my $raCellNo=3;
my $decCellNo=4;

sub printElementStart {
	my ($element, %attrs)=@_;

	print "<$element";
	if (keys %attrs) {
		while (my($key, $value)=each(%attrs)) {
			print " $key=\"$value\"";
		}
	}
	print ">";
}

# process a start-of-element event: print message about element
#
sub handle_start {
    my( $expat, $element, %attrs ) = @_;

	# I don't know why I need to repeat the defines here - shouldn't be necessary but is
	$nameCellNo=2;
	$raCellNo=3;
	$decCellNo=4;
    	# ask the expat object about our position
    	my $line = $expat->current_line;

    	printElementStart($element,%attrs);
    	if ($element =~ /tr/i) {
		print STDERR "TR\n";
		$cellNo=0;
		print "\n";
    	} elsif ($element =~ /td/i) {
       	 	$cellNo++;
		#print "Found a Td - cellNo:".$cellNo.", contents: \n";
    	} else {
		#		print "Found other element: $expat\n";
    	}
	
    	# remember this element and its starting position by pushing a
    	# little hash onto the element stack
    	push( @element_stack, { element=>$element, line=>$line });
}

sub outputNameCell {
	my $contents = shift;

	# Need to make sure the string is at most 15 charsa
	my $len=length($contents); 
	my $lenAdj=$len - 15; 
	print substr($contents, $lenAdj > 0 ? $lenAdj : 0, $len);
}

sub outputRADecCell {
	my $value = shift;

	#print "ra/dec: $value";
	printf("%.6f",$value);
}
 
sub handle_char {
	my ($expat,$string) = @_;
	$string=~ s/^\s+|\s+$//g;
	
	if ($string && defined($cellNo)) {
#		print "Char - cell:$cellNo, nameCell:$nameCellNo, raCell:$raCellNo, string: $string\n";
#
		if ($cellNo == $nameCellNo) {
			outputNameCell($string);
		} elsif ($cellNo == $raCellNo) {
			outputRADecCell($string);
		} elsif ($cellNo == $decCellNo) {
			outputRADecCell($string);
		} else {
			print "$string";
		}
	} else {
		#print "\n";
	}
}
# process an end-of-element event
#
sub handle_end {
    	my( $expat, $element ) = @_;
 
    	# We'll just pop from the element stack with blind faith that
    	# we'll get the correct closing element, unlike what our
    	# homebrewed well-formedness did, since XML::Parser will scream
    	# bloody murder if any well-formedness errors creep in.
    	my $element_record = pop( @element_stack );
    	#print "I see that $element element that started on line ",
    	#      $$element_record{ line }, " is closing now.\n";
    	print "</$element>\n";
}


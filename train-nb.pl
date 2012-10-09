#!/usr/bin/perl -w

use strict;
use threads;
use threads::shared;
use List::Util qw(shuffle first);
use Algorithm::NaiveBayes;
use URI::Escape;


srand(1541);

my$train_file="/path/to/train.tsv";
my$model_dir="/path/to/empty/folder";
my$THR_NUM_FEAT=3;
my$THR_NUM=2;
my@tasks;
my@threads;

open(TRAIN,"$train_file")or die"$!";

#my@corpus :shared = ();
#my%terms :shared = ();
my@corpus = ();
my%terms = ();
my$count=0;

print "Reading data into memory...\n";

my$choose=0;
while(<TRAIN>){
    chomp;
    $choose++;
    #next unless($choose % 3==0);
    
    my@parts=split(/\t/,$_);
    my$class=shift@parts;
    my$key=shift@parts;
    my$method=shift@parts;
    my$comment=shift@parts;
    my@code #:shared
	= @parts;
    #$comment=<BRACKET>;
    next unless(@code);
    chomp($comment);
    $comment=~s/^\[//;
    $comment=~s/\]$//;
    next if($comment=~m/^ERROR/);
    my@terms 
	#:shared 
	=split(/\] \[/,$comment);
    my %v # :shared 
	= (
	id=>$count,
	class=>$class,
	method=>$method,
	terms=> \@terms,
	code=> \@code 
	);
    push@corpus, \%v;
    my%done=();
    foreach my$term(@terms){
	next if(defined($done{$term}));
	$done{$term}=1;
	if(!defined($terms{$term})){
	    my@postings # :shared 
		= ( $#corpus );
	    $terms{$term} = \@postings;
	}else{
	    push@{$terms{$term}}, $#corpus;
	}
    }
}
close TRAIN;

print "Read\n";

# keep terms that appear at least 1000 times

print "Total terms: ".scalar(keys %terms)."\n";

my%pruned_terms=();

while( my($term, $postings) = each %terms) {
    #print $term."\t".scalar(@$postings)."\n";
    if(scalar(@{$postings}) > 1000){
	$pruned_terms{$term}=$postings;
    }
}
%terms=%pruned_terms;

print "Pruned terms: ".scalar(keys %terms)."\n";

# split into training and testing
my@random_ids = shuffle(0..$#corpus);
my$final_test_size #:shared 
    = int(scalar(@corpus)/5); # 20%
my@final_test_ids = splice(@random_ids,0,$final_test_size);
my%final_test_ids #:shared
    = map { $_=>1 } @final_test_ids;

print "Training size: ".(scalar(@corpus) - $final_test_size)."\n";
print "Test size: ".$final_test_size."\n";


# transform the bytecodes into features

sub bytecodes2features{
    my@code=@_;
    my%features #:shared
	= ();
    foreach my$bytecode(@code){
	$bytecode =~ s/^[0-9]+ //;
	$bytecode =~ s/ldc \"/ldc /;
	#$bytecode =~ s/ldc \".*/ldc/;
	$bytecode =~ s/\"\s*$//;
	if(0){
	my@coarse = split(/\s+/, $bytecode);
	foreach my$coarse(@coarse){
	    my@fine=split(/[^a-zA-Z0-9]+/,$coarse);
	    if(scalar(@fine)>1){
		$features{$coarse}=0 unless(defined($features{$coarse}));
		$features{$coarse}+= 0.5;
		foreach my$fine(@fine){
		    $features{$fine}=0 unless(defined($features{$fine}));
		    $features{$fine}+= 0.5 / scalar(@fine);
		}
	    }else{
		$features{$coarse}=0 unless(defined($features{$coarse}));
		$features{$coarse}+= 1.0;
	    }
	}
	}else{
	    my@coarse = split(/\s+/, $bytecode);
	    foreach my$coarse(@coarse){
		my@fine=split(/[^a-zA-Z0-9]+/,$coarse);
		foreach my$fine(@fine){
		    $features{$fine}=0 unless(defined($features{$fine}));
		    $features{$fine}+= 1;
		}
	    }
	}
    }
    return \%features;
}

print "Featurising bytecodes...\n";

my$part=0;

$count=0;
foreach my$entry(@corpus){
    if($count > scalar(@corpus) / 10.0 * $part){
	print "\t$part\n";
	$part++;
    }
    $count++;
    $entry->{features} = &bytecodes2features(@{$entry->{code}});
    delete $entry->{code};
}

if(0){
@tasks= map { [] } (1..$THR_NUM_FEAT);

$count=0;
foreach my$i(0..$#corpus){
    push@{$tasks[$count % $THR_NUM_FEAT]}, $i;
    $count++;
}
@threads=();
foreach my$task(@tasks){
        my$thr=
	    threads->create(
		sub {
		    foreach my$i(@_){
			my$entry=$corpus[$i];
			$entry->{features} = &bytecodes2features(@{$entry->{code}});
			delete $entry->{code};
		    }
		},@{$task});
	push@threads,$thr;
}

foreach my$thr(@threads){
    $thr->join();
}
}
print "Done\n";

if(0){
print "Computing priors...\n";
my%priors=();
my$factor = 1.0 / (scalar(@corpus) - $final_test_size);
while( my($term, $postings) = each %terms) {
    $priors{$term} = scalar(grep {!$final_test_ids{$_}} @$postings) * $factor;
}
print "Done...\n";
}

print "Training...\n";

@tasks= map { [] } (1..$THR_NUM);
$count=0;
foreach my$term(keys %terms){
    push@{$tasks[$count % $THR_NUM]}, $term;
    $count++;
}

# this is slow and wastes a lot of memory. It should be replaced by fork()
@threads=();
foreach my$task(@tasks){
        my$thr=
	    threads->create(
		sub {
		    foreach my$term(@_){
			my$model=Algorithm::NaiveBayes->new;
			my$part=0;
			foreach my$i(0..$#corpus){
			    next if(defined($final_test_ids{$i}));
			    if($i > $#corpus / 10.0 * $part){
				print "\t$term\t$part\n";
				$part++;
			    }
			    my%these_terms=map {$_ => 1} @{$corpus[$i]->{terms} };
			    my$these_features= #&bytecodes2features(@{$corpus[$i]->{code}});
				$corpus[$i]->{features};
			    $model->add_instance(attributes=>$these_features, 
						 label=>(defined($these_terms{$term})?"yes":"no"));
			}
		    
			$model->train();
			$model->purge();
			#$model->save_state("$model_dir/" . uri_escape($term) . ".model");
			my$term_counts = { tp=>0, tn=>0, fp=>0, fn=> 0};
			$part=0;
			foreach my$i(0..$#corpus){
			    next unless(defined($final_test_ids{$i}));
			    if($i > $#corpus / 10.0 * $part){
				print "\t$term\tT\t$part\n";
				$part++;
			    }
			    my%these_terms=map {$_ => 1} @{$corpus[$i]->{terms} };
			    my$these_features= #&bytecodes2features(@{$corpus[$i]->{code}}); 
				$corpus[$i]->{features};

			    my$res=$model->predict(attributes=>$these_features);
			    my$system= $res->{"yes"} > $res->{"no"};
			    my$key = defined($these_terms{$term});
			    if($system){
				if($key){
				    $term_counts->{tp}++;
				}else{
				    $term_counts->{fp}++;
				}
			    }else{
				if($key){
				    $term_counts->{fn}++;
				}else{
				    $term_counts->{tn}++;
				}
			    }
			}
			open(EVAL,">$model_dir/". uri_escape($term).".eval.tsv")or die"$!";
			my$tp=$term_counts->{"tp"};
			my$fp=$term_counts->{"fp"};
			my$tn=$term_counts->{"tn"};
			my$fn=$term_counts->{"fn"};
			my$prec = $tp+$fp > 0 ? $tp / ($tp+$fp) : 1.0;
			my$rec = $tp+$fn > 0 ? $tp / ($tp + $fn) : 0.0;
			my$f = $prec+$rec > 0 ? 2*$prec*$rec / ($prec + $rec) : 0.0;
			
			print EVAL "$term\t$tp\t$fp\t$fn\t$tn\t$prec\t$rec\t$f\n";
			close EVAL;
		    }
		}, @{$task});
	push@threads,$thr;
}

foreach my$thr(@threads){
    $thr->join();
}

if(0){
my%models=();
foreach my$term(keys %terms){
    $models{$term} = Algorithm::NaiveBayes->new;
}

my$part = 0;
foreach my$i(0..$#corpus){
    next if(defined($final_test_ids{$i}));
    if($i > $#corpus / 10.0 * $part){
	print "\t$part\n";
	$part++;
    }
    my%these_terms=map {$_ => 1} @{$corpus[$i]->{terms} };
    my$these_features= #&bytecodes2features(@{$corpus[$i]->{code}});
	$corpus[$i]->{features};
    while( my($term, $model) = each %models) {
	$model->add_instance(attributes=>$these_features, 
			     label=>(defined($these_terms{$term})?"yes":"no"));
    }
}
while( my($term, $model) = each %models) {
    $model->purge();
    $model->save_state("$model_dir/" . uri_escape($term));
}

print "Done.\n";

print "Testing...\n";
my%term_counts = map { $_ => { tp=>0, tn=>0, fp=>0, fn=> 0} } keys %terms;
my$per_method_accuracy=0.0;

$part=0;
foreach my$i(0..$#corpus){
    next unless(defined($final_test_ids{$i}));
    if($i > $#corpus / 10.0 * $part){
	print "\t$part\n";
	$part++;
    }
    my%these_terms=map {$_ => 1} @{$corpus[$i]->{terms} };
    my$these_features= #&bytecodes2features(@{$corpus[$i]->{code}}); 
	$corpus[$i]->{features};

    my$good=0;
    my$bad=0;
    while( my($term, $model) = each %models) {
	my$res=$model->predict(attributes=>$these_features);
	my$system= $res->{"yes"} > $res->{"no"};
	my$key = defined($these_terms{$term});
	if($system){
	    if($key){
		$term_counts{$term}->{tp}++;
		$good++;
	    }else{
		$term_counts{$term}->{fp}++;
		$bad++;
	    }
	}else{
	    if($key){
		$term_counts{$term}->{fn}++;
	    }else{
		$term_counts{$term}->{tn}++;
	    }
	}
    }
    if($good+$bad>0){
	$per_method_accuracy += 1.0 * $good / ($good + $bad) * (1.0 / $final_test_size);
    }
}
print "Done.\n";

open(EVAL,">$model_dir/eval.tsv")or die"$!";
my$total={ tp=>0, tn=>0, fp=>0, fn=> 0};

while( my($term, $counts) = each %models) {
    my$tp=$counts->{"tp"};
    my$fp=$counts->{"fp"};
    my$tn=$counts->{"tn"};
    my$fn=$counts->{"fn"};
    $total->{"tp"}+=$tp;
    $total->{"fp"}+=$fp;
    $total->{"tn"}+=$tn;
    $total->{"fn"}+=$fn;

    my$prec = $tp+$fp > 0 ? $tp / ($tp+$fp) : 1.0;
    my$rec = $tp+$fn > 0 ? $tp / ($tp + $fn) : 0.0;
    my$f = $prec+$rec > 0 ? 2*$prec*$rec / ($prec + $rec) : 0.0;

    print EVAL "$term\t$tp\t$fp\t$fn\t$tn\t$prec\t$rec\t$f\n";
}

my$tp=$total->{"tp"};
my$fp=$total->{"fp"};
my$tn=$total->{"tn"};
my$fn=$total->{"fn"};

my$prec = $tp+$fp > 0 ? $tp / ($tp+$fp) : 1.0;
my$rec = $tp+$fn > 0 ? $tp / ($tp + $fn) : 0.0;
my$f = $prec+$rec > 0 ? 2*$prec*$rec / ($prec + $rec) : 0.0;

print EVAL "TOTAL\t$tp\t$fp\t$fn\t$tn\t$prec\t$rec\t$f\n";
close EVAL;

print "Prec/Rec/F: $prec/$rec/$f\n";

}

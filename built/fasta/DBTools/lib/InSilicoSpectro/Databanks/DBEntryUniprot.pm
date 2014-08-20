use strict;

package InSilicoSpectro::Databanks::DBEntryUniprot;
require Exporter;
use Carp;

=head1 NAME

InSilicoSpectro::Databanks::DBEntryUniprot - Uniprot formated entry

=head1 SYNOPSIS


=head1 DESCRIPTION

Inherit from DBEntry, but can parse a uniprot format

=head1 FUNCTIONS

=head3 useInSilicoSpectro()

determine if InSilicoSpectro lib is used in the current environment. The main difference will be the tracnslation from uniprot MOD_RES nomenclature to InSilciSpectro one

=head1 METHODS


=head3 my $dbu=InSilicoSpectro::Databanks::DBEntryUniprot;

=head2 Accessors/Setters

=head3 $dbu->chains()

get a ref to an array of arrays for chain delimiter [from, to]

=head3 $dbu->add_chain([$from, $to])

set chain (through a reference a to an array)

=head3 $dbu->clear_chains()

Reset the chain arrays

=head3 $dbu->signals(); $dbu->add_signal([$from, $to]); $dbu->clear_signals()

Idem than for the *chain subs

=head2 Derived sequence generation

=head3 $dbu->generateIsoforms(shortName)

returns an array of InSilicoSpectro::Databanks::DBEntry containing all the isoforms generated by a swissprot entry

=head3 $dbu->generateChains(shortName)

Retuns an array of InSilicoSpectro::Databanks::DBEntry containing entries from FT CHAIN lines

=head3 $dbu->generatePeptides(shortName)

Retuns an array of InSilicoSpectro::Databanks::DBEntry containing entries from FT PEPTIDE lines

=head3 $dbu->generateDerivedForms([skipIsoforms=>1][, skipChains=>1][, skipPeptides=>1][, shortName=>1])

Retuns an array of InSilicoSpectro::Databanks::DBEntry containing entries from the concatenation of the above methods

skip* argument will skip the creaion of the mentionned form

=head3 $dbu->seqSubstr(from=>int, to=>int [, subseq=>AAstring]);

=head3 $dbu->seqSubstr(pos=>int, len=>int [, subseq=>AAstring]);

Replace a piece of the sequence by a a subseq (or remove it if sebseq is unfdefined). All annotation will be updated (or remove if they inerfer with the substitued sequence.

from=>int notation starts sequence at position 1;

pos=>int notation starts sequence at position 0;

=head3 $dbu->seqExtract(from=>int, to=>int);

=head3 $dbu->seqExtract(pos=>int, len=>int);

Keep only the sub sequence described par the given delimiters (see seqSubstr(...) for description)

=head2 I/O

=head3 $dbe->readDat($fastacontent);

read info from fasta contents (fitrs line with '>' and info + remaining is sequence.

=head1 EXAMPLES

=head1 EXPORT

=head3 $VERBOSE

verbose level

=head1 SEE ALSO

=head1 COPYRIGHT

Copyright (C) 2004-2005  Geneva Bioinformatics www.genebio.com

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

=head1 AUTHORS

Alexandre Masselot, www.genebio.com

=cut

use InSilicoSpectro::Databanks::DBEntry;
use File::Basename;

{
  use Object::InsideOut qw(Exporter InSilicoSpectro::Databanks::DBEntry);
  our $VERBOSE=0;
  BEGIN{
    our (@EXPORT, @EXPORT_OK);
    @EXPORT = qw(&useInSilicoSpectro $VERBOSE);
    @EXPORT_OK = ();
  }

  our $isUsingInSilicoSpectro;

  our @attrArrays=qw(chain signal peptide);
  our $attrArraysStr=join '|', @attrArrays;
  our $attrArraysRE=qr/($attrArraysStr)/;

  my @chains :Field(Accessor => 'chains', Type=>'List' );
  my @signals :Field(Accessor => 'signals', Type=>'List' );
  my @peptides :Field(Accessor => 'peptides', Type=>'List' );
  my @__FTLines :Field(Accessor => '__FTLines', Type=>'Hash', Permission=>'Private' );
  my @__VAR_SEQ :Field(Accessor => '__VAR_SEQ', Type=>'Hash', Permission=>'Private' );
  my @__seqSubstr :Field(Accessor => '__seqSubstr', Type=>'List', Permission=>'Private' );


  my %init_args :InitArgs = (
			    );
  sub _init :Init{
    my ($self, $h) = @_;
    $self->chains([]);
    $self->signals([]);
    $self->peptides([]);
    return $self;
  }



  sub _automethod :Automethod{
    my ($self, $val) = @_;

    my $set=exists $_[1];
    my $name=$_;

    if ($name=~/add_($attrArraysStr)/) {
      $name=$1."s";
      return sub {
	croak "must give a value to add to the array [$name]" unless defined $val;
	push @{$self->$name()}, $val;
      };
      return sub {return $self->{$name}};
    } elsif ($name=~/clear_((?:$attrArraysStr)(?:s)?)$/) {
      $name=$1;
      
      return sub {$self->$name([]);return $self;}
    }
  }

  ################ Functions

  sub useInSilicoSpectro{
    return $isUsingInSilicoSpectro if defined $isUsingInSilicoSpectro;
    eval{
      require InSilicoSpectro;
      InSilicoSpectro::init();
      $isUsingInSilicoSpectro=1;
    };
    if ($@) {
      warn "will not use InSilicoSpectro module & definitions";
      warn "$@";
      $isUsingInSilicoSpectro=0;
    }
    return $isUsingInSilicoSpectro;
  }

  # I/O
  sub readDat{
    my $self=shift;
    my $dat=shift;
    my ($seq, $recSeq, $curFTLine, $recFT);

    $self->clear_chains();
    $self->clear_signals();
    $self->clear_annotatedModRes();
    $self->clear_variants();
    $self->__FTLines({});
    my $acRead;
    my @descr;
    foreach (split /\n/, $dat) {
      last if /^\/\//;
      undef $curFTLine if substr($_, 3, 10)=~/\S/;

      if ($recSeq) {
	$seq.=$_;
	next;
      }
      if ($curFTLine) {
	croak "recording FT line is on and line does not match /^FT\\s+/" unless s/^FT\s+//;
	if(($curFTLine->{comment}=~/isoform \S+$/) && ($_!~/^and/) && ($_=~/^\w/)){
	  $curFTLine->{comment}.="$_";
	}else{
	  $curFTLine->{comment}.=" $_";
	}
	next;
      }

      if (/^ID\s+(\w+)/) {
	my $v=$1;
	$self->ID($v);
      } elsif (/^AC\s+(\w+)/ && ! $acRead) {
	my $v=$1;
	$self->AC($v);
	$acRead=1;
      } elsif (/^DE\s+(.*)\s*/) {
	push @descr, $1;
      } elsif (/^OX\s+NCBI_TaxID=(\d+);/) {
	my $v=$1;
	$self->ncbiTaxid($v);
      } elsif (/^FT\s+CHAIN\s+(\d+)\s+(\d+)/) {
	my($to, $from)=($1, $2);
	$self->add_chain([$to, $from]);
      } elsif (/^FT\s+SIGNAL\s+(\d+)\s+(\d+)/) {
	my($to, $from)=($1, $2);
	$self->add_signal([$to, $from]);
      } elsif (/^FT\s+PEPTIDE\s+(\d+)\s+(\d+)/) {
	my($to, $from)=($1, $2);
	$self->add_peptide([$to, $from]);
      } elsif (/^FT\s+MOD_RES\s+(\d+)\s+(\d+)\s+(.*)/) {
	if ($2!=$1) {
	  carp "cannot handle multi-position FT MOD_RES: $_";
	  next;
	}
	my ($p, $str)=($1, $3);
	if (useInSilicoSpectro) {
	  my $mr=InSilicoSpectro::InSilico::ModRes::getModifFromSprotFT($str);
	  unless ($mr){
	    carp "cannot retrieve mod res from annotation [$str]" if $VERBOSE>=1;
	    next;
	  }
	  $self->add_annotatedModRes($p, $mr->get('name'));
	} else {
	  $self->add_annotatedModRes($p, $str);
	}
      } elsif (/^FT\s+(VAR_SEQ|VARIANT)\s+(\d+)\s+(\d+)\s+(.*)/) {
	my ($ft, $p1, $p2, $com)=($1, $2, $3, $4);
	$curFTLine={
		    from=>$2,
		    to=>$3,
		    comment=>$4
		   };
	push @{$self->__FTLines->{$1}}, $curFTLine;
      } elsif (/^SQ/) {
	$recSeq=1;
      }
    }
    $self->sequence($seq);

    #parse description for multine uniprot dat format;
    my $descr;
    foreach(@descr){
      last if /^Includes:/;
      $descr=$1 if /RecName: Full=(.*);/;
      $descr.=" ($1)" if /Short=(.*);/;
    }
    $descr=join (" ", @descr) unless $descr;
    $self->description($descr);

    #rescan $self->{FTLines}
    #to put back $self->{seqSubstr} info together with isoform labels
    $self->__VAR_SEQ({});
    $self->__seqSubstr([]);
    if ($self->__FTLines->{VAR_SEQ}) {
      foreach my $ftl (@{$self->__FTLines->{VAR_SEQ}}) {
	my @isof= $ftl->{comment}=~/(?<=isoform)\s+([\w\-]+)/gi;
	carp "cannot parse VAR_SPLIC labels from [$ftl->{comment}] for ".$self->AC unless @isof;
	my $substr;
	if ($ftl->{comment}=~/Missing/i) {
	
	} elsif ($ftl->{comment}=~/\w+\s*\->\s*([A-Z ]+)/) {
	  $substr=$1;
	  $substr=~s/\s+//g;
	} else {
	  carp "unparsable for Missing or subst FT VAR_SEQ comment: [$ftl->{comment}] for ".$self->AC;
	}
	my $h={
	       from=> $ftl->{from},
	       to=>$ftl->{to},
	       substr=>$substr,
	      };
	my $idx=scalar @{$self->__seqSubstr};
	foreach (@isof) {
	  push @{$self->__VAR_SEQ->{$_}{seqSubstrIndex}}, $idx;
	}
	push @{$self->__seqSubstr}, $h;
      }
      #reorder sub annotation to be in position decreasing order (to keep coherence)
      foreach (values %{$self->__VAR_SEQ}) {
	my @tmp=@{$_->{seqSubstrIndex}};
	@tmp=sort {$self->__seqSubstr->[$b]{from} <=> $self->__seqSubstr->[$a]{from}} @tmp;
	$_->{seqSubstrIndex}=\@tmp;
      }
      #@{$self->{seqSubstr}}=sort {$b->{from} <=> $a->{from}} @{$self->{seqSubstr}};
    }

    if ($self->__FTLines->{VARIANT}) {
      foreach my $ftl (@{$self->__FTLines->{VARIANT}}) {
	if ($ftl->{comment}=~/([A-Z]+)\s+\->\s+([\*A-Z]+)/) {
	  $self->add_variant($ftl->{from}, $1, $2);
	} elsif ($ftl->{comment}=~/Missing/i) {
	  my $tmp='';
	  foreach ($ftl->{from}..$ftl->{to}) {
	    $tmp.='.';
	  }
	  $self->add_variant($ftl->{from}, $tmp, '');
	} else {
	  carp "cannot parse VARIANT info from [$ftl->{comment}] for ".$self->AC;
	}
      }
    }

    $self->__FTLines({});
  }


  ################### Derived sequence generation ################3

  sub generateDerivedForms{
    my $self=shift;
    my %hprms=@_;

    my @ret;
    my @tmp;
    @tmp=$self->generateChains($hprms{shortName}) unless $hprms{skipChains};
    push @ret, @tmp;
    @tmp=$self->generateIsoforms($hprms{shortName}) unless $hprms{skipIsoforms};
    push @ret, @tmp;
    @tmp=$self->generatePeptides($hprms{shortName}) unless $hprms{skipPeptides};
    push @ret, @tmp;
    return @ret;
  }

  sub generateIsoforms{
    my $self=shift;
    my $shortName=shift;
    my @isoforms;

    my $counter=0;
    foreach my $isoform (sort {$a <=> $b}  keys %{$self->__VAR_SEQ}) {
      my $isoseq=$self->clone(1);
      $isoseq->ACorig($self->AC());
      $isoseq->AC($shortName ? $self->AC()."_I$counter" : $self->AC()."_ISOFORM_$isoform");
      $counter++;
      $isoseq->description($self->description()." [ISOFORM $isoform]");


      $isoseq->__VAR_SEQ({});
      #duplicate seqSubstr not to alter the original sequence
      $isoseq->__seqSubstr([]);
      if ($self->__seqSubstr) {
	foreach (@{$self->__seqSubstr}) {
	  my %h=%$_;
	  push @{$isoseq->__seqSubstr}, \%h;
	}
      }

      #    #remove seq outside the chain
      #    if($isoseq->chain){
      #      if($isoseq->chain()->[0]>1){
      #	$isoseq->seqSubstr(from=>1, to=>$isoseq->chain()->[0]-1);
      #      }
      #    }
      foreach my $idx (@{$self->__VAR_SEQ->{$isoform}{seqSubstrIndex}}) {
	my %h=%{$isoseq->__seqSubstr->[$idx]};
	$isoseq->seqSubstr(from=> $h{from}, to=>$h{to}, substr=>$h{substr});
      }

      #bless up to InSilicoSpectro::Databanks::DBEntry
      #bless $isoseq, "InSilicoSpectro::Databanks::DBEntry";
      my $dbe=InSilicoSpectro::Databanks::DBEntry->new(COPY=>$isoseq);
      push @isoforms, $dbe;
    }
    return @isoforms;
  }

  sub generatePeptides{
    my $self=shift;
    my $shortName=shift;
    my @peptides;
    my $i=0;
    foreach my $pp (@{$self->peptides()}) {
      my $pseq=$self->clone(1);
      $pseq->description($self->description()." [PEPTIDE $i])");
      $pseq->ACorig($self->AC());
      $pseq->AC($shortName ? $self->AC()."_P$i" : $self->AC()."_PEPT_$i");
      $pseq->seqExtract(from=>$pp->[0], to=>$pp->[1]);
      $i++;
      push @peptides, $pseq;
    }
    return @peptides;
  }


  sub generateChains{
    my $self=shift;
    my $shortName=shift;
    my @chains;
    my $i=0;
    foreach my $c (@{$self->chains()}) {
      my $cseq=$self->clone(1);
      $cseq->ACorig($self->AC());
      $cseq->AC($shortName ? $self->AC()."_C$i" : $self->AC()."_CHAIN_$i");
      $cseq->description($self->description()." [CHAIN $i]");
      $cseq->seqExtract(from=>$c->[0], to=>$c->[1]);
      $i++;
      push @chains, $cseq;
    }
    return @chains;
  }


  sub seqSubstr{
    my $self=shift;
    my %hprm=@_;
    my ($pos, $len, $substr);
    if ($hprm{from} && $hprm{to}) {
      $pos=$hprm{from}-1;
      $len=$hprm{to}-$hprm{from}+1;
    } elsif ((defined $hprm{pos}) && $hprm{len}) {
      ($pos, $len)=($hprm{pos}, $hprm{len});
    } else {
      croak "cannot DBEntryUniprot::seqSubstr with paramer [@_] (either (from=>x, to=>y) or (pos=>x, len=>y) for ".$self->AC;
    }
    $substr=$hprm{substr} || '';
    my $seq=$self->sequence;
    $pos|=0;
    $seq=~s/(.{$pos}).{$len}/$1$substr/;
    $self->sequence($seq);
    #CHANGE 2007/12/19
    $self->updateAnnotPos($pos, $len-length($substr));
  }


  sub seqExtract{
    my $self=shift;
    my %hprm=@_;
    my ($pos, $len, $substr);
    if ($hprm{from} && $hprm{to}) {
      $pos=$hprm{from}-1;
      $len=$hprm{to}-$hprm{from}+1;
    } elsif ((defined $hprm{pos}) && $hprm{len}) {
      ($pos, $len)=($hprm{pos}, $hprm{len});
    } else {
      croak "cannot DBEntryUniprot::seqSubstr with paramer [@_] (either (from=>x, to=>y) or (pos=>x, len=>y) for ".$self->AC;
    }
    my $lseq=length $self->sequence();
    if ($lseq>($pos+$len)) {
      $self->seqSubstr(from=>$pos+$len+1, to=>$lseq);
    }
    if ($pos>0) {
      $self->seqSubstr(from=>1, to =>$pos);
    }
  }

  sub updateAnnotPos{
    my ($self, $pos, $len)=@_;
    #shift or remove all annotated PTM
    my @amr=$self->annotatedModRes;
    if (@amr) {
      $self->clear_annotatedModRes;
      foreach (@amr) {
	my @tmp=@$_;
	my $p=$tmp[0];
	if ($p>=$pos) {
	  $tmp[0]-=$len;
	  $self->add_annotatedModRes(@tmp) if $tmp[0]>$pos;
	} else {
	  $self->add_annotatedModRes(@tmp);
	}
      }
    }
    @amr=$self->variants;
    if (@amr) {
      $self->clear_variants;
      foreach (@amr) {
	my @tmp=@$_;
	my $p=$tmp[0];
	if ($p>=$pos) {
	  $tmp[0]-=$len;
	  $self->add_variant(@tmp) if $tmp[0]>0;
	} else {
	  $self->add_variant(@tmp);
	}
      }
    }
  }

}
return 1;

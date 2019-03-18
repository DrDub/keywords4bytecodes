keywords4bytecodes
==================

Code behind the keywords4bytecodes.org project.



Generating the corpus of bytecodes / comments
---------------------------------------------

This can be done only in a Debian-compatible GNU/Linux system with a Debian mirror in ./debian-mirror.

Packages that need to be installed:

* openjdk
* libdox-java
* jclassinfo
* apt-file
* (eclipse)

Compile the eclipse project under workspace. (You will need to have
the package libqdox-java.)

Get the packages with jar files:

  apt-file search --package-only .jar > packages-with-jar

Get their source packages

  for i in `cat packages-with-jars`; do dpkg-query -p $i | perl -ne 'chomp; ($k,$v)=m/^([^:]+): (.*)$/; if($k eq "Source"){print "\t$v"};if($k eq "Filename"){print "\t$v\n"}' >> packages.tsv; done

Process all the relevant source packages in packages.tsv using create-corpus.pl:

  cat packages.tsv | ./create-corpus.pl > corpus.tsv

The final output will have a method per line, with the following <TAB> delimited columns:

* full class name
* method signature (from bytecode)
* long method signature (from source code)
* comment
* [each byte code separated by a <TAB>...]


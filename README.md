keywords4bytecodes - mahout branch
==================================

Predict the first word in a Java method from its bytecodes.

Case study for http://aprendizajengrande.net


Running the code
----------------

*you need to have Mahout 1.0 installed from source in your local repo,
configured for Hadoop 2.0, see below*

mvn clean package assembly:single

Then run the hadoop job org.keywords4bytecodes.firstclass.Driver
pointing to the tsv file training file (see below) and an output
directory.

Getting the data
----------------

An extract of the bytecodes and first word in a Java method is available at

http://aprendizajengrande.net/clases/material/firstclass_training20120314.tsv.bz2 (24Mb, 251Mb decompressed, 357k instances)


Installing Mahout from source
-----------------------------

$ git clone https://github.com/apache/mahout.git
$ cd mahout
$ mvn clean package -DskipTests -Drelease -Dmahout.skip.distribution=false -Dhadoop.profile=200 -Dhadoop2.version=2.4.1 -Dhbase.version=0.98.0-hadoop2


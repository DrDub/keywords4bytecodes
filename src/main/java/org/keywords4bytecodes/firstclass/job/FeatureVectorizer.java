package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.mahout.math.VectorWritable;

public class FeatureVectorizer {

	public static Job buildJob(Configuration conf, String[] args)
			throws IOException {
		Path inputPath = new Path(args[0]);
		Path featurePath = new Path(args[1]);
		Path targetLabelsPath = new Path(args[2]);
		Path outputDir = new Path(args[3]);

		FileSystem fs = FileSystem.get(conf);

		if (!fs.exists(featurePath)) {
			System.err.println("Feature file must exist: " + args[1]);
			System.exit(1);
		}

		if (!fs.exists(targetLabelsPath)) {
			System.err.println("Labels file must exist: " + args[2]);
			System.exit(1);
		}

		conf.set(AbstractMapperWithFeatureGenerator.TARGET_FEATURES_PARAM,
				args[1]);
		conf.set(AbstractMapperWithFeatureGenerator.TARGET_LABELS_PARAM,
				args[2]);

		// Create job
		Job job = Job.getInstance(conf, "FeatureVectorizer");
		job.setJarByClass(FeatureVectorizer.class);

		// Setup MapReduce
		job.setMapperClass(FeatureVectorizerMapper.class);
		job.setReducerClass(FeatureVectorizerReducer.class);
		job.setNumReduceTasks(1);

		// Specify key / value
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(VectorWritable.class);

		// Input
		FileInputFormat.addInputPath(job, inputPath);
		job.setInputFormatClass(TextInputFormat.class);

		// Output
		FileOutputFormat.setOutputPath(job, outputDir);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		return job;
	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		Path outputDir = new Path(args[3]);

		// Create configuration
		Configuration conf = new Configuration(true);
		FileSystem fs = FileSystem.get(conf);

		// see
		// http://stackoverflow.com/questions/17265002/hadoop-no-filesystem-for-scheme-file
		conf.set("fs.hdfs.impl",
				org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
		conf.set("fs.file.impl",
				org.apache.hadoop.fs.LocalFileSystem.class.getName());

		Job job = buildJob(conf, args);

		// Delete output if exists
		if (fs.exists(outputDir))
			fs.delete(outputDir, true);

		// Execute job
		int code = job.waitForCompletion(true) ? 0 : 1;
		System.exit(code);
	}
}
package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class LabelSelector {

	public static Job buildJob(Configuration conf, String[] args)
			throws IOException {
		Path inputPath = new Path(args[0]);
		Path outputDir = new Path(args[1]);

		// Create job
		Job job = Job.getInstance(conf, "LabelSelector");
		job.setJarByClass(LabelSelector.class);

		// Setup MapReduce
		job.setMapperClass(LabelFeatureCounterMapper.class);
		job.setReducerClass(LabelFeatureCounterReducer.class);
		job.setNumReduceTasks(1);

		// Specify key / value
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		// Input
		FileInputFormat.addInputPath(job, inputPath);
		job.setInputFormatClass(TextInputFormat.class);

		// Output
		FileOutputFormat.setOutputPath(job, outputDir);
		job.setOutputFormatClass(TextOutputFormat.class);

		return job;
	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		Path outputDir = new Path(args[1]);

		// Create configuration
		Configuration conf = new Configuration(true);

		// see
		// http://stackoverflow.com/questions/17265002/hadoop-no-filesystem-for-scheme-file
		conf.set("fs.hdfs.impl",
				org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
		conf.set("fs.file.impl",
				org.apache.hadoop.fs.LocalFileSystem.class.getName());

		// Create job
		Job job = buildJob(conf, args);

		// Delete output if exists
		FileSystem hdfs = FileSystem.get(conf);
		if (hdfs.exists(outputDir))
			hdfs.delete(outputDir, true);

		// Execute job
		int code = job.waitForCompletion(true) ? 0 : 1;
		System.exit(code);
	}
}
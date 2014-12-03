package org.keywords4bytecodes.firstclass;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.classifier.naivebayes.test.TestNaiveBayesDriver;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.utils.SplitInput;
import org.keywords4bytecodes.firstclass.job.FeatureSelector;
import org.keywords4bytecodes.firstclass.job.FeatureVectorizer;
import org.keywords4bytecodes.firstclass.job.LabelSelector;

public class Driver {

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);

		// take the text data from HDFS and split them into train and test
		System.out.println("Train Input: " + args[0]);
		System.out.println("Output Folder: " + args[1]);

		Path input = new Path(args[0]);
		Path output = new Path(args[1]);
		if (!fs.exists(input)) {
			System.err.println("Input not found.");
			System.exit(1);
		}
		if (fs.exists(output)) {
			fs.delete(output, true);
		}
		fs.mkdirs(output);
		String trainText = args[1] + "/train/" + input.getName();
		String testText = args[1] + "/test/" + input.getName();

		if (args.length > 2) {
			trainText = args[0];
			testText = args[2];
		} else {
			Path trainTextFolder = new Path(args[1] + "/train");
			Path testTextFolder = new Path(args[1] + "/test");
			fs.mkdirs(trainTextFolder);
			fs.mkdirs(testTextFolder);

			SplitInput split = new SplitInput();
			split.setMapRedOutputDirectory(new Path(args[1] + "/split"));
			split.setTrainingOutputDirectory(trainTextFolder);
			split.setTestOutputDirectory(testTextFolder);
			split.setTestSplitPct(20);
			split.splitFile(input);
		}

		// compute labels from train
		String labelsFolder = args[1] + "/labels";
		String labelsFile = args[1] + "/labels/part-r-00000";
		Job labelsJob = LabelSelector.buildJob(conf, new String[] { trainText,
				labelsFolder });
		if (!labelsJob.waitForCompletion(true))
			System.exit(1);

		// select features from train
		String featuresFolder = args[1] + "/features";
		String featuresFile = args[1] + "/features/part-r-00000";
		Job featuresJob = FeatureSelector.buildJob(conf, new String[] {
				trainText, labelsFile, featuresFolder });
		if (!featuresJob.waitForCompletion(true))
			System.exit(1);

		// transform train and test into vectors
		String trainFolder = args[1] + "/train-vector";
		String testFolder = args[1] + "/test-vector";
		Job trainVectorizerJob = FeatureVectorizer.buildJob(conf, new String[] {
				trainText, featuresFile, labelsFile, trainFolder });
		if (!trainVectorizerJob.waitForCompletion(true))
			System.exit(1);
		Job testVectorizerJob = FeatureVectorizer.buildJob(conf, new String[] {
				testText, featuresFile, labelsFile, testFolder });
		if (!testVectorizerJob.waitForCompletion(true))
			System.exit(1);

		String modelFile = args[1] + "/model";
		String labelIndex = args[1] + "/labelIndex";
		ToolRunner.run(conf, new TrainNaiveBayesJob(), new String[] { "-i",
				trainFolder, //
				"-o", modelFile, //
				"-el", //
				"-li", labelIndex, //
				"-ow", "-c" });

		ToolRunner.run(conf, new TestNaiveBayesDriver(), new String[] { "-i",
				testFolder, //
				"-o", args[1] + "/classified-test", //
				"-m", modelFile, //
				"-l", labelIndex, //
				"-ow", "-c" });
	}
}

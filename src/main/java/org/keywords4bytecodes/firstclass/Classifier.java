package org.keywords4bytecodes.firstclass;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.classifier.naivebayes.test.TestNaiveBayesDriver;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.utils.SplitInput;

public class Classifier {

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();

		// take vectors in HDFS and split them into train and test
		System.out.println("Input: " + args[0]);
		System.out.println("Output: " + args[1]);

		new SplitInput().run(new String[] { "-i",
				args[0], //
				"--trainingOutput",
				args[1] + "/train-vectors", //
				"--testOutput", args[1] + "/test-vectors",
				"--randomSelectionPct", "40", //
				"--overwrite", "--sequenceFiles" });

		ToolRunner.run(conf, new TrainNaiveBayesJob(), new String[] { "-i",
				args[1] + "/train-vectors", //
				"-o", args[1] + "/model", //
				"-el", //
				"-li", args[1] + "/labelIndex", //
				"-ow", "-c" });

		ToolRunner.run(conf, new TestNaiveBayesDriver(), new String[] { "-i",
				args[1] + "/test-vectors", //
				"-o", args[1] + "/test", //
				"-m", args[1] + "/model", //
				"-l", args[1] + "/labelIndex", //
				"-ow", "-c" });
	}
}

package org.keywords4bytecodes.firstclass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.classifier.naivebayes.test.TestNaiveBayesDriver;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.VectorWritable;

public class Classifier {

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();

		// crear vectores en HDFS
		System.out.println("Input: " + args[1]);
		BufferedReader br = new BufferedReader(new FileReader(args[1]));
		Path input = new Path(args[2] + "/input");
		Path testInputFolder = new Path(args[2] + "/testInput");
		testInputFolder.getFileSystem(conf).mkdirs(testInputFolder);
		Path testInput = new Path(args[2] + "/testInput/part-r-00000");

		SequenceFile.Writer writer = SequenceFile.createWriter(conf,
				Writer.file(input), Writer.keyClass(Text.class),
				Writer.valueClass(VectorWritable.class));
		SequenceFile.Writer testWriter = SequenceFile.createWriter(conf,
				Writer.file(testInput), Writer.keyClass(Text.class),
				Writer.valueClass(VectorWritable.class));

		String line = br.readLine();
		int itemNum = 0;
		Map<Integer, Integer> labels = new HashMap<Integer, Integer>();
		while (line != null) {
			String[] parts = line.split(",");
			DenseVector d = new DenseVector(parts.length - 1);
			int label = 0;
			for (int i = 0; i < parts.length - 1; i++) {
				d.set(i, Math.abs(Double.parseDouble(parts[i])));
			}
			label = Integer.parseInt(parts[parts.length - 1]);

			if (!labels.containsKey(label)) {
				labels.put(label, labels.size());
			}
			VectorWritable v = new VectorWritable(d);
			if (Math.random() < 0.1)
				testWriter.append(new Text("/" + label + "/" + itemNum), v);
			else
				writer.append(new Text("/" + label + "/" + itemNum), v);
			itemNum++;
			line = br.readLine();
		}
		testWriter.close();
		writer.close();
		br.close();

		ToolRunner.run(conf, new TrainNaiveBayesJob(), new String[] { "-i",
				args[2] + "/input", "-o", args[3] + "/model", "-el", "-li",
				args[3] + "/li", "-ow", "-c" });

		ToolRunner.run(conf, new TestNaiveBayesDriver(), new String[] { "-i",
				args[2] + "/testInput", "-o", args[3] + "/test", "-m",
				args[3] + "/model", "-l", args[3] + "/li", "-ow", "-c" /*"--runSequential"*/ });

		// new TrainNaiveBayesJob().run(new String[] { "-i", args[2], "-o",
		// args[3] + "/model", "-el", "-li", args[3] + "/li", "-ow" });

	}
}

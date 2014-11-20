package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class LabelFeatureCounterReducer extends
		Reducer<Text, IntWritable, Text, Text> {

	public void reduce(Text label, Iterable<IntWritable> featCounts,
			Context context) throws IOException, InterruptedException {
		if (label.toString().isEmpty())
			return; // skip empty labels

		int instanceCount = 0;
		int featureCount = 0;
		for (IntWritable fc : featCounts) {
			featureCount += fc.get();
			instanceCount++;
		}

		context.write(label, new Text("" + instanceCount + " " + featureCount));
	}
}
package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.math.VectorWritable;

public class FeatureVectorizerReducer extends
		Reducer<Text, VectorWritable, Text, VectorWritable> {

	public void reduce(Text label, Iterable<VectorWritable> vectors,
			Context context) throws IOException, InterruptedException {
		int count = 0;
		for (VectorWritable v : vectors) {
			count++;
			context.write(
					new Text("/" + label.toString() + "/" + label.toString()
							+ count), v);
		}
	}
}
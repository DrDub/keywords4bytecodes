package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.io.Text;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class FeatureVectorizerMapper extends
		AbstractMapperWithFeatureGenerator<Object, Text, Text, VectorWritable> {

	private VectorWritable vectorWritable = new VectorWritable();
	private Text label = new Text();

	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {
		String[] parts = value.toString().split("\\t+");

		String instanceLabel = labeler.extract(parts[1]);

		Map<String, AtomicInteger> table = new HashMap<>();
		for (int i = 2; i < parts.length; i++)
			generator.extract(parts[i], table);

		label.set(instanceLabel);
		Vector v = new RandomAccessSparseVector(featToPos.size());
		for (Map.Entry<String, AtomicInteger> e : table.entrySet())
			v.set(featToPos.get(e.getKey()), e.getValue().get());
		vectorWritable.set(v);
		context.write(label, vectorWritable);
	}
}
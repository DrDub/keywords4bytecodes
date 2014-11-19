package org.keywords4bytecodes.firstclass.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.keywords4bytecodes.firstclass.FeatureGenerator;

public class FeatureVectorizerMapper extends
		Mapper<Object, Text, Text, VectorWritable> {

	private VectorWritable vectorWritable = new VectorWritable();
	private Text label = new Text();

	private FeatureGenerator generator;

	public static final String TARGET_FEATURES_PARAM = "TargetFeatures";

	private Map<String, Integer> featToPos = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	protected void setup(
			@SuppressWarnings("rawtypes") org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		String targetFeatures = conf.get(TARGET_FEATURES_PARAM);
		if (targetFeatures == null)
			throw new IllegalStateException("Must specify target features");
		else {
			Path featurePath = new Path(targetFeatures);
			FSDataInputStream fsdis = featurePath.getFileSystem(conf).open(
					featurePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fsdis));
			String line = br.readLine();

			int index = 0;
			while (line != null) {
				String[] parts = line.split("\\s+");
				featToPos.put(parts[0], index);
				index++;
				line = br.readLine();
			}
			br.close();

			this.generator = new FeatureGenerator(featToPos.keySet());
		}
	}

	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {

		String[] parts = value.toString().split("\\t+");
		Map<String, AtomicInteger> table = new HashMap<>();
		for (int i = 1; i < parts.length; i++)
			generator.extract(parts[i], table);

		label.set(parts[0]);
		Vector v = new RandomAccessSparseVector(featToPos.size());
		for (Map.Entry<String, AtomicInteger> e : table.entrySet())
			v.set(featToPos.get(e.getKey()), e.getValue().get());
		vectorWritable.set(v);
		context.write(label, vectorWritable);
	}
}
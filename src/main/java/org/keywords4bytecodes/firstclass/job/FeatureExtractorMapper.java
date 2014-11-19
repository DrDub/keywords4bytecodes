package org.keywords4bytecodes.firstclass.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.keywords4bytecodes.firstclass.FeatureGenerator;

public class FeatureExtractorMapper extends Mapper<Object, Text, Text, Text> {

	private Text feature = new Text();
	private Text label = new Text();

	private FeatureGenerator generator;

	public static final String TARGET_FEATURES_PARAM = "TargetFeatures";

	@SuppressWarnings("unchecked")
	@Override
	protected void setup(
			@SuppressWarnings("rawtypes") org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		String targetFeatures = conf.get(TARGET_FEATURES_PARAM);
		if (targetFeatures == null)
			this.generator = new FeatureGenerator();
		else {
			Path featurePath = new Path(targetFeatures);
			// TODO check it exists
			FSDataInputStream fsdis = featurePath.getFileSystem(conf).open(
					featurePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fsdis));
			String line = br.readLine();

			Set<String> features = new HashSet<String>();

			while (line != null) {
				features.add(line);
				line = br.readLine();
			}
			br.close();

			this.generator = new FeatureGenerator(features);
		}
	}

	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {

		String[] parts = value.toString().split("\\t+");
		Map<String, AtomicInteger> table = new HashMap<>();
		for (int i = 1; i < parts.length; i++)
			generator.extract(parts[i], table);

		label.set(parts[0]);
		for (Map.Entry<String, AtomicInteger> e : table.entrySet()) {
			feature.set(e.getKey());
			for (int i = 0; i < e.getValue().get(); i++)
				context.write(feature, label);
		}
	}
}
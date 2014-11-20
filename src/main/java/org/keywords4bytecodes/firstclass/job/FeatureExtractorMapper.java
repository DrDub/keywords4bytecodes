package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.io.Text;

public class FeatureExtractorMapper extends
		AbstractMapperWithFeatureGenerator<Object, Text, Text, Text> {

	private Text feature = new Text();
	private Text labelAndCounts = new Text();

	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {

		String[] parts = value.toString().split("\\t+");
		Map<String, AtomicInteger> table = new HashMap<>();
		for (int i = 1; i < parts.length; i++)
			generator.extract(parts[i], table);

		for (Map.Entry<String, AtomicInteger> e : table.entrySet()) {
			feature.set(e.getKey());
			labelAndCounts.set(parts[0] + " " + e.getValue().get());
			context.write(feature, labelAndCounts);
		}
	}
}
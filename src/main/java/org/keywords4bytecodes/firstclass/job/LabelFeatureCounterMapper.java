package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.keywords4bytecodes.firstclass.FeatureGenerator;
import org.keywords4bytecodes.firstclass.LabelGenerator;

public class LabelFeatureCounterMapper extends
		Mapper<Object, Text, Text, IntWritable> {

	private Text label = new Text();
	private IntWritable featureCounts = new IntWritable();

	private FeatureGenerator generator = new FeatureGenerator();
	private LabelGenerator labeler = new LabelGenerator();

	public void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {
		String[] parts = value.toString().split("\\t+");
		Map<String, AtomicInteger> table = new HashMap<>();
		for (int i = 2; i < parts.length; i++)
			generator.extract(parts[i], table);

		label.set(labeler.extract(parts[1]));
		int count = 0;
		for (AtomicInteger a : table.values())
			count += a.get();
		featureCounts.set(count);
		context.write(label, featureCounts);
	}
}
package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class FeatureExtractorReducer extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text feat, Iterable<Text> labels, Context context)
			throws IOException, InterruptedException {
		Map<String, AtomicInteger> counts = new HashMap<>();
		for (Text textLabel : labels) {
			String label = textLabel.toString();
			AtomicInteger a = counts.get(label);
			if (a == null)
				counts.put(label, new AtomicInteger(1));
			else
				a.incrementAndGet();
		}

		// see if any class is more represented

		double bestScore = 0;
		double allCounts = 0;
		double totalLabels = 0;

		for (AtomicInteger a : counts.values()) {
			allCounts += a.get();
			if (a.get() >= 100)
				totalLabels++;
		}

		for (String label : counts.keySet()) {
			double labelCounts = counts.get(label).get();

			// TODO move this threshold to conf
			if (labelCounts < 100)
				continue;

			double score = (labelCounts - allCounts / totalLabels) / allCounts;
			score = Math.abs(score);
			if (score > bestScore)
				bestScore = score;
		}

		System.out.println(feat.toString() + "\t" + bestScore);
		if (bestScore > 0.25)
			// TODO move this threshold to conf
			context.write(feat, new Text(Double.toString(bestScore)));
	}
}
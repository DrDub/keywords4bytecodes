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
		int count = 0;
		for (Text textLabel : labels) {
			count++;
			String label = textLabel.toString();
			AtomicInteger a = counts.get(label);
			if (a == null)
				counts.put(label, new AtomicInteger(1));
			else
				a.incrementAndGet();
		}
		if (count > 100) {
			// TODO move this threshold to conf

			// see if any class is more represented

			double bestScore = 0;
			double allCounts = 0;
			double totalLabels = counts.size();

			for (AtomicInteger a : counts.values())
				allCounts += a.get();

			for (String label : counts.keySet()) {
				double labelCounts = counts.get(label).get();
				double score = (labelCounts - allCounts / totalLabels)
						/ allCounts;
				score = Math.sqrt(score * score);
				if (score > bestScore)
					bestScore = score;
			}

			System.out.println(feat.toString() + "\t" + bestScore);
			if (bestScore > 0.1)
				// TODO move this threshold to conf
				context.write(feat, new Text(Double.toString(bestScore)));
		}
	}
}
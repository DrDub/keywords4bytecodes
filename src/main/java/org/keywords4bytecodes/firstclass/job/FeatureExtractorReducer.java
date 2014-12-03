package org.keywords4bytecodes.firstclass.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.keywords4bytecodes.firstclass.LabelGenerator;

public class FeatureExtractorReducer extends Reducer<Text, Text, Text, Text> {

	private Map<String, int[]> labelCounts;

	private long totalInstances = 0;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		labelCounts = AbstractMapperWithFeatureGenerator
				.readLabelCounts(context.getConfiguration());
		for (int[] i : labelCounts.values())
			totalInstances += i[0];
	}

	public void reduce(Text feat, Iterable<Text> labels, Context context)
			throws IOException, InterruptedException {
		Map<String, AtomicInteger> featCounts = new HashMap<>();
		Map<String, AtomicInteger> instanceCounts = new HashMap<>();
		int totalInstanceCount = 0;
		for (Text labelAndCount : labels) {
			String[] parts = labelAndCount.toString().split("\\s");
			String label = parts[0];
			if (!this.labelCounts.containsKey(label))
				label = LabelGenerator.OTHER;
			int count = Integer.parseInt(parts[1]);
			AtomicInteger a = instanceCounts.get(label);
			if (a == null) {
				featCounts.put(label, new AtomicInteger(count));
				instanceCounts.put(label, new AtomicInteger(1));
			} else {
				a.incrementAndGet();
				featCounts.get(label).addAndGet(count);
			}
			totalInstanceCount++;
		}

		// first, does it appear in at least 100 instances (with any labels)
		if (totalInstanceCount < 100)
			return; // skip feature

		// filter by Chi Square
		double bestScore = 0;
		String bestLabel = "";

		int rowCount = 0;
		for (AtomicInteger a : instanceCounts.values())
			rowCount += a.get();

		for (String label : labelCounts.keySet()) {
			long o11, o12, o21, o22;
			// with the feature and with the label
			o11 = instanceCounts.containsKey(label) ? instanceCounts.get(label)
					.get() : 0;
			if (o11 < 50) // skip label if two few positive data
				continue;
			// with the feature and without the label
			o12 = rowCount - o11;
			// without the feature and with the label
			o21 = labelCounts.get(label)[0] - o11;
			// without the feature and without the label
			o22 = totalInstances - o11 - o12 - o21;

			double chiSquare = totalInstances
					* Math.pow(o11 * o22 - o21 * o12, 2)
					/ ((o11 + o12) * (o11 + o21) * (o12 + o22) * (o21 + o22) * 1.0);
			if (chiSquare > bestScore) {
				bestScore = chiSquare;
				bestLabel = label;
			}
		}

		if (bestScore > 7.88) // 1800)
			// TODO move this threshold to conf
			context.write(feat, new Text("" + bestScore + " " + bestLabel));
	}
}
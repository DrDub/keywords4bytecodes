package org.keywords4bytecodes.firstclass.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper;
import org.keywords4bytecodes.firstclass.FeatureGenerator;

public class AbstractMapperWithFeatureGenerator<KEYIN, VALUEIN, KEYOUT, VALUEOUT>
		extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {

	public static final String TARGET_FEATURES_PARAM = "TargetFeatures";

	public static final String TARGET_LABELS_PARAM = "TargetLabels";

	public static final String OTHER = "K4B_OTHER";

	protected Map<String, Integer> featToPos = new HashMap<>();

	protected Set<String> targetLabels = new HashSet<>();

	protected FeatureGenerator generator;

	@SuppressWarnings("unchecked")
	@Override
	protected void setup(
			@SuppressWarnings("rawtypes") org.apache.hadoop.mapreduce.Mapper.Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		FileSystem fs = FileSystem.get(conf);
		String targetFeatures = conf.get(TARGET_FEATURES_PARAM);
		if (targetFeatures != null) {
			Path featurePath = new Path(targetFeatures);
			FSDataInputStream fsdis = fs.open(featurePath);
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
		}
		this.generator = new FeatureGenerator(featToPos.keySet());

		String targetLabelsName = conf.get(TARGET_LABELS_PARAM);
		if (targetLabelsName != null)
			targetLabels.addAll(readLabelCounts(conf).keySet());

	}

	public static Map<String, int[]> readLabelCounts(Configuration conf)
			throws IOException {
		String targetLabelsName = conf.get(TARGET_LABELS_PARAM);
		FileSystem fs = FileSystem.get(conf);
		Map<String, int[]> result = new HashMap<>();
		int[] other = new int[2];
		result.put(OTHER, other);

		if (targetLabelsName != null) {
			Path labelsPath = new Path(targetLabelsName);
			FSDataInputStream fsdis = fs.open(labelsPath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fsdis));
			String line = br.readLine();

			while (line != null) {
				String[] parts = line.split("\\s+");
				int instanceCount = Integer.parseInt(parts[1]);
				int featCount = Integer.parseInt(parts[2]);
				if (instanceCount < 500) {
					// TODO move this threshold to conf
					other[0] += instanceCount;
					other[1] += featCount;
				} else
					result.put(parts[0], new int[] { instanceCount, featCount });
				line = br.readLine();
			}
			br.close();
		}
		return result;
	}
}
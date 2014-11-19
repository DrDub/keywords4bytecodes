package org.keywords4bytecodes.firstclass;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FeatureGenerator {

	private Set<String> target;

	@SuppressWarnings("unchecked")
	public FeatureGenerator() {
		this((Collection<String>) Collections.EMPTY_SET);
	}

	public FeatureGenerator(Collection<String> target) {
		this.target = new HashSet<String>(target);
	}

	private void observe(String feat, Map<String, AtomicInteger> table) {
		if (!target.isEmpty() && !target.contains(feat))
			return;

		AtomicInteger a = table.get(feat);
		if (a == null)
			table.put(feat, new AtomicInteger(1));
		else
			a.incrementAndGet();
	}

	public Map<String, AtomicInteger> extract(List<String> code) {
		Map<String, AtomicInteger> result = new HashMap<>();
		for (String line : code)
			extract(line, result);
		return result;
	}

	public void extract(String line, Map<String, AtomicInteger> table) {
		StringBuilder telescope = new StringBuilder();
		String[] parts1 = line.split("\\s+");
		String prev = "START";
		for (int i = 0; i < parts1.length; i++) {
			observe("T:" + parts1[i], table);
			observe("B:" + prev + ":" + parts1[i], table);
			String[] parts2 = parts1[i].split("[^A-Za-z0-9]+");
			telescope.setLength(0);
			telescope.append("TB:").append(prev);
			for (int j = 0; j < parts2.length; j++) {
				observe("P:" + parts2[j], table);
				observe("PB:" + prev + ":" + parts2[j], table);
				telescope.append(':').append(parts2[j]);
				observe(telescope.toString(), table);
			}
			prev = parts1[i];
		}
	}
}

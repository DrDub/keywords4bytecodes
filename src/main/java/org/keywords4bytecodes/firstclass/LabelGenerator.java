package org.keywords4bytecodes.firstclass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate labels from a method signature
 */
public class LabelGenerator {
	private Set<String> known = null;

	public static final String OTHER = "K4B_OTHER";

	public LabelGenerator(Collection<String> known) {
		this.known = new HashSet<String>(known);
	}

	public LabelGenerator() {
	}

	public String extract(String methodSignature) {
		String methodName = methodSignature.replaceFirst("\\(.*", "")
				.replaceFirst(".* ", "");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < methodName.length(); i++) {
			char ch = methodName.charAt(i);
			if (ch == '_' && sb.length() == 0)
				continue; // ignore C convention of private members
			if (ch == '_') // C convention for multi-word
				break;
			if (Character.isUpperCase(ch) && sb.length() > 0)
				break;
			sb.append(ch);
		}

		String firstWord = sb.toString();
		if (known != null && !known.isEmpty() && !known.contains(firstWord))
			return OTHER;

		return firstWord;
	}

	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line = br.readLine();
		LabelGenerator labeler = new LabelGenerator();
		while (line != null) {
			String[] parts = line.split("\\t");
			System.out.println(parts[0] + "\t" + parts[1] + "\t"
					+ labeler.extract(parts[1]));
			line = br.readLine();
		}
		br.close();
	}

}

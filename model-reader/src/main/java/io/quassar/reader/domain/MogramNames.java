package io.quassar.reader.domain;

import java.util.HashSet;
import java.util.Set;

final class MogramNames {
	private static final int LENGTH = 12;
	private final Set<String> assigned;

	MogramNames(Set<String> explicitIdentifiers) {
		assigned = new HashSet<>(explicitIdentifiers);
	}

	String identifier(String type, String fingerprint) {
		StringBuilder name = new StringBuilder(type.substring(type.lastIndexOf('.') + 1))
			.append(fingerprint, 0, LENGTH);
		int index = LENGTH;
		while (!assigned.add(name.toString()))
			name.append(index < fingerprint.length() ? fingerprint.charAt(index++) : '0');
		return name.toString();
	}
}

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
		int first = Character.digit(fingerprint.charAt(0), 16) % 10;
		StringBuilder name = new StringBuilder(type.substring(type.lastIndexOf('.') + 1))
			.append(first).append(fingerprint, 1, LENGTH);
		int index = LENGTH;
		while (!assigned.add(name.toString()))
			name.append(index < fingerprint.length() ? fingerprint.charAt(index++) : '0');
		return name.toString();
	}
}

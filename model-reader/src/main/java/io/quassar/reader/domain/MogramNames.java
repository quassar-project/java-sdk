package io.quassar.reader.domain;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

final class MogramNames {
	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());
	private static final int LENGTH = 12;
	private final Set<String> assigned;

	MogramNames(Set<String> explicitIdentifiers) {
		assigned = new HashSet<>(explicitIdentifiers);
	}

	String identifier(String type, String fingerprint) {
		StringBuilder name = new StringBuilder(type.substring(type.lastIndexOf('.') + 1)).append('-');
		BigInteger remaining = new BigInteger(fingerprint, 16);
		for (int index = 0; index < LENGTH; index++) remaining = append(name, remaining);
		while (!assigned.add(name.toString())) remaining = append(name, remaining);
		return name.toString();
	}

	private BigInteger append(StringBuilder name, BigInteger remaining) {
		BigInteger[] parts = remaining.divideAndRemainder(BASE);
		int index = parts[1].intValueExact();
		name.append(ALPHABET.charAt(index));
		return parts[0];
	}
}

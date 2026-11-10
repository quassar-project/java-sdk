package io.quassar.javacode.domain;

import javax.lang.model.SourceVersion;
import java.util.HashSet;
import java.util.Set;

public final class JavaNames {
	private final Set<String> used;

	JavaNames(Set<String> reserved) {
		used = new HashSet<>(reserved);
	}

	String claim(String suggestion) {
		String name = identifier(suggestion);
		String candidate = name;
		int suffix = 2;
		while (!used.add(candidate)) candidate = name + suffix++;
		return candidate;
	}

	public static String className(String type) {
		String name = identifier(type);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	static String methodName(String name) {
		String identifier = identifier(name);
		return Character.toLowerCase(identifier.charAt(0)) + identifier.substring(1);
	}

	static String plural(String name) {
		if (name.endsWith("y") && name.length() > 1 && "aeiou".indexOf(name.charAt(name.length() - 2)) < 0)
			return name.substring(0, name.length() - 1) + "ies";
		if (name.endsWith("s") || name.endsWith("x") || name.endsWith("ch") || name.endsWith("sh")) return name + "es";
		return name + "s";
	}

	private static String identifier(String text) {
		StringBuilder result = new StringBuilder();
		boolean uppercase = false;
		for (int point : text.codePoints().toArray()) {
			if (!Character.isJavaIdentifierPart(point)) {
				uppercase = true;
				continue;
			}
			if (result.isEmpty() && !Character.isJavaIdentifierStart(point)) result.append('_');
			result.appendCodePoint(uppercase ? Character.toUpperCase(point) : point);
			uppercase = false;
		}
		String name = result.isEmpty() ? "value" : result.toString();
		return SourceVersion.isKeyword(name) || name.equals("_") || Set.of("record", "var", "yield", "sealed", "permits").contains(name)
			? name + "Value" : name;
	}
}

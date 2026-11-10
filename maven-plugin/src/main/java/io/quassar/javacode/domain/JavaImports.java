package io.quassar.javacode.domain;

import io.quassar.javacode.model.JavaCode;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class JavaImports {
	private static final Pattern TOKEN = Pattern.compile(
		"\"(?:[^\"\\\\]|\\\\.)*\"|(?<![\\p{javaJavaIdentifierPart}.])((?:java\\.|io\\.quassar\\.)(?:[a-z]\\w*\\.)*[A-Z]\\w*)");

	private JavaImports() {
	}

	static JavaCode format(JavaCode unit, Set<String> declaredTypes) {
		Map<String, String> types = new HashMap<>();
		StringBuilder body = new StringBuilder();
		Matcher tokens = TOKEN.matcher(unit.content());
		while (tokens.find()) {
			String qualified = tokens.group(1);
			if (qualified == null) continue;
			String simple = qualified.substring(qualified.lastIndexOf('.') + 1);
			if (declaredTypes.contains(simple)) continue;
			String previous = types.putIfAbsent(simple, qualified);
			if (previous == null || previous.equals(qualified)) tokens.appendReplacement(body, simple);
		}
		tokens.appendTail(body);

		StringBuilder header = new StringBuilder();
		if (!unit.packageName().isEmpty()) header.append("package ").append(unit.packageName()).append(";\n\n");
		String imports = types.values().stream()
			.filter(type -> !type.substring(0, type.lastIndexOf('.')).equals("java.lang"))
			.sorted()
			.map(type -> "import " + type + ";")
			.collect(Collectors.joining("\n"));
		if (!imports.isEmpty()) header.append(imports).append("\n\n");
		return new JavaCode(unit.packageName(), unit.className(), header.append(body).toString());
	}
}

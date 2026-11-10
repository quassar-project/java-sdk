package io.quassar.javacode.domain;

import io.quassar.templating.model.Rule;
import io.quassar.templating.model.outputs.Expression;
import io.quassar.templating.model.outputs.Literal;
import io.quassar.templating.model.outputs.Output;
import io.quassar.templating.model.outputs.Placeholder;
import io.quassar.templating.model.predicates.AttributePredicate;
import io.quassar.templating.model.predicates.BinaryExpression;
import io.quassar.templating.model.predicates.BinaryOperator;
import io.quassar.templating.model.predicates.LogicalExpression;
import io.quassar.templating.model.predicates.NotExpression;
import io.quassar.templating.model.predicates.TriggerPredicate;
import io.quassar.templating.model.predicates.TypePredicate;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TemplateRules {
	String rule(Rule rule) {
		String body = rule.outputs().map(this::output).collect(Collectors.joining(",\n\t\t\t\t"));
		String declaration = "new Rule().condition(" + condition(rule.condition()) + ")";
		return body.isEmpty() ? declaration : declaration + "\n\t\t\t.output(\n\t\t\t\t" + body + ")";
	}

	private String condition(LogicalExpression condition) {
		if (condition instanceof BinaryExpression binary) {
			String operator = binary.operator() == BinaryOperator.AND ? "all" : "any";
			return operator + "(" + condition(binary.left()) + ", " + condition(binary.right()) + ")";
		}
		if (condition instanceof NotExpression not) return "not(" + condition(not.expression()) + ")";
		if (condition instanceof TypePredicate type)
			return "allTypes(" + Arrays.stream(type.types()).map(this::literal).collect(Collectors.joining(", ")) + ")";
		if (condition instanceof TriggerPredicate trigger) return "trigger(" + literal(trigger.name()) + ")";
		if (condition instanceof AttributePredicate attribute) {
			String value = attribute.value() == null ? "" : ", " + literal(attribute.value().toString());
			return "attribute(" + literal(attribute.attribute()) + value + ")";
		}
		throw new IllegalArgumentException("Unsupported template predicate " + condition);
	}

	private String output(Output output) {
		if (output instanceof Literal text) return "literal(" + literal(text.toString()) + ")";
		if (output instanceof Placeholder placeholder) return placeholder(placeholder);
		if (output instanceof Expression expression) {
			String children = expression.outputs().map(this::output).collect(Collectors.joining(", "));
			String result = "expression(" + children + ")";
			if (expression.next() != null) result += ".next(" + output(expression.next()) + ")";
			return result;
		}
		throw new IllegalArgumentException("Unsupported template output " + output.getClass().getName());
	}

	private String placeholder(Placeholder placeholder) {
		String path = placeholder.targetPath() == null ? "" : Arrays.stream(placeholder.targetPath())
			.map(this::literal).collect(Collectors.joining(", ", "new String[]{", "}, "));
		String arguments = Stream.concat(Stream.of(placeholder.name()), Arrays.stream(placeholder.formatters()))
			.map(this::literal).collect(Collectors.joining(", "));
		String result = "placeholder(" + path + arguments + ")";
		return placeholder.separator() == null ? result : result + ".multiple(" + literal(placeholder.separator()) + ")";
	}

	private String literal(String text) {
		if (text.length() <= 8000) return quoted(text);
		StringBuilder chunks = new StringBuilder("String.join(\"\", ");
		for (int start = 0; start < text.length();) {
			int end = Math.min(start + 8000, text.length());
			if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
			if (start != 0) chunks.append(", ");
			chunks.append(quoted(text.substring(start, end)));
			start = end;
		}
		return chunks.append(')').toString();
	}

	private String quoted(String text) {
		StringBuilder result = new StringBuilder("\"");
		for (char character : text.toCharArray()) {
			result.append(switch (character) {
				case '\\' -> "\\\\";
				case '"' -> "\\\"";
				case '\n' -> "\\n";
				case '\r' -> "\\r";
				case '\t' -> "\\t";
				default -> character < ' ' || character == 127
					? "\\" + String.format("%03o", (int) character) : String.valueOf(character);
			});
		}
		return result.append('"').toString();
	}
}

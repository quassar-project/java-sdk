package io.quassar.reader.domain;

import io.quassar.reader.model.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class PropertyValues {
	private static final Pattern NUMBER = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");

	private PropertyValues() {
	}

	static Value normalize(Value value, String declaredType, boolean multiple) {
		if (value.isEmpty()) return value;
		if (multiple) {
			var values = value.isSequence() ? value.asSequence() : List.of(value);
			return new Value.Sequence(values.stream().map(item -> normalize(item, declaredType, false)).toList());
		}
		String scalar = declaredType.split("[:\\[ ]", 2)[0].toLowerCase(Locale.ROOT);
		return switch (scalar) {
			case "integer", "int", "long", "number", "double", "float", "real", "decimal" -> number(value);
			case "word" -> value.isSequence()
				? new Value.Sequence(value.asSequence().stream().map(item -> normalize(item, "word", false)).toList())
				: new Value.Text(NodeSupport.word(value));
			default -> value;
		};
	}

	private static Value number(Value value) {
		if (value.isNumber()) return value;
		if (value.isSequence()) return number(value.asSequence().get(0));
		var matcher = NUMBER.matcher(value.isReference() ? value.asReference() : value.asString());
		if (!matcher.find()) throw new IllegalArgumentException("Not a numeric value: " + value);
		return new Value.Number(new BigDecimal(matcher.group()));
	}
}

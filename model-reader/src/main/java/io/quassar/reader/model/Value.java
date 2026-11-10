package io.quassar.reader.model;

import java.math.BigDecimal;
import java.util.List;

public sealed interface Value {
	default boolean isString() {
		return this instanceof Text;
	}

	default boolean isNumber() {
		return this instanceof Number;
	}

	default boolean isBoolean() {
		return this instanceof Bool;
	}

	default boolean isReference() {
		return this instanceof Reference;
	}

	default boolean isSequence() {
		return this instanceof Sequence;
	}

	default boolean isEmpty() {
		return this instanceof Empty;
	}

	default String asString() {
		return this instanceof Text text ? text.value() : "";
	}

	default BigDecimal asNumber() {
		return this instanceof Number number ? number.value() : BigDecimal.ZERO;
	}

	default boolean asBoolean() {
		return this instanceof Bool bool && bool.value();
	}

	default String asReference() {
		return this instanceof Reference reference ? reference.path() : "";
	}

	default List<Value> asSequence() {
		return this instanceof Sequence sequence ? sequence.values() : List.of();
	}

	record Text(String value) implements Value {
	}

	record Number(BigDecimal value) implements Value {
	}

	record Bool(boolean value) implements Value {
	}

	record Reference(String path) implements Value {
	}

	record Sequence(List<Value> values) implements Value {
		public Sequence {
			values = List.copyOf(values);
		}
	}

	record Empty() implements Value {
	}
}

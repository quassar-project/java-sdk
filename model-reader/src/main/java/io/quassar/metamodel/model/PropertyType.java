package io.quassar.metamodel.model;

public record PropertyType(Kind kind, Kind element) {
	public enum Kind {
		EMPTY, STRING, NUMBER, BOOLEAN, REFERENCE, SEQUENCE, VALUE
	}

	public static PropertyType scalar(Kind kind) {
		return new PropertyType(kind, Kind.EMPTY);
	}

}

package io.quassar.reader.model;

public record Property(String name, Value value, Origin origin, String constructorType) {
	public enum Origin { EXPLICIT, DEFAULT }

	public Property(String name, Value value, Origin origin) {
		this(name, value, origin, "");
	}

	public Property(String name, Value value) {
		this(name, value, Origin.EXPLICIT);
	}
}

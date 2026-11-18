package io.quassar.reader.model;

public record Property(String name, Value value, Origin origin, String constructorType, int position) {
	public enum Origin { EXPLICIT, DEFAULT }

	public Property(String name, Value value, Origin origin, String constructorType) {
		this(name, value, origin, constructorType, -1);
	}

	public Property(String name, Value value, Origin origin) {
		this(name, value, origin, "", -1);
	}

	public Property(String name, Value value) {
		this(name, value, Origin.EXPLICIT);
	}
}

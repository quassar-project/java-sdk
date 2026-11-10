package io.quassar.metamodel.model;

public record Cardinality(int min, int max) {
	public static final Cardinality MANY = new Cardinality(0, -1);

	public boolean isSingle() {
		return max == 1;
	}
}

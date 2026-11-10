package io.quassar.metamodel.model;

import java.util.List;

public record Composition(String type, Cardinality cardinality, List<String> modifiers) {
	public Composition {
		modifiers = List.copyOf(modifiers);
	}
}

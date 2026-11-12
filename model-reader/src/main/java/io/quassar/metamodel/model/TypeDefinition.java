package io.quassar.metamodel.model;

import java.util.List;

public record TypeDefinition(String type, boolean root, Cardinality rootCardinality, List<PropertyDefinition> properties,
		List<Composition> children, List<String> facets) {
	public TypeDefinition {
		properties = List.copyOf(properties);
		children = List.copyOf(children);
		facets = List.copyOf(facets);
	}
}

package io.quassar.metamodel.model;

import java.util.Arrays;
import java.util.List;

public record MetamodelType(String name, boolean isFacet, List<PropertyDefinition> properties,
		List<String> facets, List<String> children, Declaration declaration, String scope,
		String parent, String role, Cardinality cardinality, List<String> modifiers,
		List<Composition> compositions) {
	public enum Declaration {
		CONCEPT, FACET, SUB
	}

	public MetamodelType {
		properties = List.copyOf(properties);
		facets = List.copyOf(facets);
		children = List.copyOf(children);
		modifiers = List.copyOf(modifiers);
		compositions = List.copyOf(compositions);
	}

	public MetamodelType(String name, boolean isFacet, List<PropertyDefinition> properties,
			List<String> facets, List<String> children) {
		this(name, isFacet, properties, facets, children, isFacet ? Declaration.FACET : Declaration.CONCEPT,
			"", "", "", Cardinality.MANY, List.of(), List.of());
	}

	public boolean isRoot() {
		return !isFacet && scope.isEmpty() && Arrays.stream(role.split("\\s+")).noneMatch("component"::equalsIgnoreCase);
	}
}

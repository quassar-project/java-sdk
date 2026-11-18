package io.quassar.metamodel.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ResolvedMetamodel(List<TypeDefinition> types, List<TypeDefinition> facets,
		List<MetamodelType> declarations, Map<String, String> scopes, Map<String, String> parents,
		Optional<String> dsl) {
	public ResolvedMetamodel {
		types = List.copyOf(types);
		facets = List.copyOf(facets);
		declarations = List.copyOf(declarations);
		scopes = Map.copyOf(scopes);
		parents = Map.copyOf(parents);
	}
}

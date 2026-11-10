package io.quassar.metamodel.domain;

import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.Cardinality;
import io.quassar.metamodel.model.Composition;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.TypeDefinition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MetamodelResolver {
	public List<TypeDefinition> resolve(Metamodel metamodel) {
		return resolve(metamodel, false);
	}

	public List<TypeDefinition> resolveFacets(Metamodel metamodel) {
		return resolve(metamodel, true);
	}

	private List<TypeDefinition> resolve(Metamodel metamodel, boolean facets) {
		Map<String, MetamodelType> types = metamodel.types().stream().collect(Collectors.toMap(MetamodelType::name, type -> type));
		return metamodel.types().stream()
			.filter(type -> type.isFacet() == facets)
			.sorted(Comparator.comparing(MetamodelType::name))
			.map(type -> resolve(type, types))
			.toList();
	}

	private TypeDefinition resolve(MetamodelType type, Map<String, MetamodelType> types) {
		Map<PropertyKey, PropertyDefinition> properties = new LinkedHashMap<>();
		for (PropertyDefinition property : type.properties())
			properties.put(new PropertyKey(property.name(), property.position()), property);
		Set<String> facets = new LinkedHashSet<>();
		Map<String, Composition> children = new LinkedHashMap<>();
		collect(type, types, facets, children);
		return new TypeDefinition(type.name(), type.isRoot(), type.cardinality(), List.copyOf(properties.values()),
			List.copyOf(children.values()), List.copyOf(facets));
	}

	private void collect(MetamodelType type, Map<String, MetamodelType> types, Set<String> facets, Map<String, Composition> children) {
		for (String facet : type.facets())
			if (facets.add(facet)) collect(types.get(facet), types, facets, children);
		for (String child : type.children())
			children.putIfAbsent(child, new Composition(child, Cardinality.MANY, List.of()));
		for (Composition composition : type.compositions()) children.put(composition.type(), composition);
	}

	private record PropertyKey(String name, int position) {
	}
}

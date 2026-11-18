package io.quassar.metamodel.domain;

import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.ResolvedMetamodel;
import io.quassar.metamodel.model.Cardinality;
import io.quassar.metamodel.model.Composition;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.TypeDefinition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MetamodelResolver {
	public ResolvedMetamodel describe(Metamodel metamodel) {
		var scope = new MetamodelScope(metamodel);
		var declarations = scope.types();
		var scopes = declarations.stream().filter(type -> !type.scope().isEmpty())
			.collect(Collectors.toMap(MetamodelType::name, MetamodelType::scope));
		var parents = declarations.stream().filter(type -> type.declaration() == MetamodelType.Declaration.SUB)
			.collect(Collectors.toMap(MetamodelType::name, MetamodelType::parent));
		var dsl = metamodel.manifest()
			.filter(manifest -> manifest.name() != null && !manifest.name().isBlank()
				&& manifest.version() != null && !manifest.version().isBlank())
			.map(manifest -> manifest.name().strip().toLowerCase(Locale.ROOT).replace('.', ':') + ":" + manifest.version().strip());
		return new ResolvedMetamodel(scope.resolve(resolve(metamodel)), scope.resolve(resolveFacets(metamodel)),
			declarations, scopes, parents, dsl);
	}

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
		for (PropertyDefinition property : properties(type, types, new LinkedHashSet<>()))
			properties.put(new PropertyKey(property.name(), property.position()), resolve(property, type, types));
		Set<String> facets = new LinkedHashSet<>();
		Map<String, Composition> children = new LinkedHashMap<>();
		collect(type, types, facets, children);
		return new TypeDefinition(type.name(), type.isRoot(), type.cardinality(), List.copyOf(properties.values()),
			List.copyOf(children.values()), List.copyOf(facets));
	}

    private List<PropertyDefinition> properties(MetamodelType type, Map<String, MetamodelType> types, Set<String> visiting) {
        if (!visiting.add(type.name())) throw new IllegalArgumentException("Inheritance cycle: " + visiting);
        Map<PropertyKey, PropertyDefinition> result = new LinkedHashMap<>();
        if (type.declaration() != MetamodelType.Declaration.SUB && !type.parent().isBlank()) {
            var candidates = new LinkedHashMap<>(types);
            candidates.remove(type.name());
            String parentName = resolveName(type.parent(), type.scope(), candidates);
            MetamodelType parent = types.get(parentName);
            if (parent == null) throw new IllegalArgumentException("Unknown parent " + type.parent());
            for (var property : properties(parent, types, visiting))
                result.put(new PropertyKey(property.name(), property.position()), resolve(property, parent, types));
        }
        for (var property : type.properties()) result.put(new PropertyKey(property.name(), property.position()), property);
        visiting.remove(type.name());
        return List.copyOf(result.values());
    }

	private void collect(MetamodelType type, Map<String, MetamodelType> types, Set<String> facets, Map<String, Composition> children) {
		for (String facet : type.facets())
			if (facets.add(facet)) collect(types.get(facet), types, facets, children);
		for (String child : type.children()) {
			String name = resolveName(child, type.name(), types);
			children.putIfAbsent(name, new Composition(name, Cardinality.MANY, List.of()));
		}
		for (Composition composition : type.compositions()) {
			String name = resolveName(composition.type(), type.name(), types);
			children.put(name, new Composition(name, composition.cardinality(), composition.modifiers()));
		}
	}

	private PropertyDefinition resolve(PropertyDefinition property, MetamodelType owner, Map<String, MetamodelType> types) {
		if (property.referenceTypes().isEmpty()) return property;
		return new PropertyDefinition(property.name(), property.position(), property.type(),
			property.referenceTypes().stream().map(name -> resolveName(name, owner.name(), types)).toList(),
			property.declaredType(), property.constraints(), property.defaultValue());
	}

	private String resolveName(String name, String scope, Map<String, MetamodelType> types) {
		if (name.indexOf('.') >= 0 && types.containsKey(name)) return name;
		String[] path = name.split("\\.");
		while (true) {
			String prefix = scope.isEmpty() ? "" : scope + ".";
			if (types.containsKey(prefix + name)) return prefix + name;
			List<String> matches = types.keySet().stream()
				.filter(candidate -> candidate.startsWith(prefix))
				.filter(candidate -> matchesPath(candidate.substring(prefix.length()), path))
				.limit(2).toList();
			if (matches.size() == 1) return matches.get(0);
			if (scope.isEmpty()) return name;
			int separator = scope.lastIndexOf('.');
			scope = scope.substring(0, Math.max(0, separator));
		}
	}

	private boolean matchesPath(String candidate, String[] path) {
		String[] segments = candidate.split("\\.");
		if (!segments[segments.length - 1].equals(path[path.length - 1])) return false;
		if (path.length > 1 && !segments[0].equals(path[0])) return false;
		int matched = 0;
		for (int i = 0; i < segments.length - 1 && matched < path.length - 1; i++)
			if (segments[i].equals(path[matched])) matched++;
		return matched == path.length - 1;
	}

	private record PropertyKey(String name, int position) {
	}
}

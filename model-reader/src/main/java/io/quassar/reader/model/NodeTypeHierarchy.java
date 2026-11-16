package io.quassar.reader.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NodeTypeHierarchy {
	private final Map<String, Type> types;
	private final Map<String, String> parents;

	public NodeTypeHierarchy(List<Type> declarations) {
		Map<String, Type> names = new LinkedHashMap<>();
		for (Type type : declarations) names.put(type.name(), type);
		types = Map.copyOf(names);
		Map<String, String> parentNames = new LinkedHashMap<>();
		for (Type type : declarations)
			if (!type.parent().isEmpty()) parentNames.put(type.name(), resolveParent(type));
		parents = Map.copyOf(parentNames);
	}

	public List<String> types(String actual, String declared, List<String> facets) {
		String owner = concept(declared);
		String concrete = resolve(actual, false, owner);
		if (concrete == null) concrete = actual;
		Set<String> classTypes = lineage(concrete);
		Set<String> result = new LinkedHashSet<>(classTypes);
		for (String facet : facets) {
			String resolved = null;
			for (String type : classTypes) {
				resolved = resolve(facet, true, type);
				if (resolved != null) break;
			}
			result.addAll(lineage(resolved == null ? facet : resolved));
		}
		return List.copyOf(result);
	}

	private String concept(String name) {
		Type type = types.get(name);
		while (type != null && type.facet()) {
			name = type.scope();
			type = types.get(name);
		}
		return name;
	}

	private String resolve(String name, boolean facet, String context) {
		Type exact = types.get(name);
		if (exact != null && exact.facet() == facet && applies(exact, context)) return name;
		List<String> matches = types.values().stream()
			.filter(type -> type.facet() == facet)
			.filter(type -> type.name().endsWith("." + name))
			.filter(type -> applies(type, context))
			.map(Type::name).limit(2).toList();
		return matches.size() == 1 ? matches.get(0) : null;
	}

	private boolean applies(Type type, String context) {
		if (context.isEmpty()) return true;
		return type.facet() ? concept(type.name()).equals(context)
			: lineage(type.name()).contains(context);
	}

	private Set<String> lineage(String name) {
		Set<String> lineage = new LinkedHashSet<>();
		for (String current = name; current != null && lineage.add(current); current = parents.get(current)) {
		}
		return lineage;
	}

	private String resolveParent(Type type) {
		for (String scope = type.scope(); !scope.isEmpty(); scope = scope.substring(0, Math.max(0, scope.lastIndexOf('.')))) {
			String qualified = scope + "." + type.parent();
			if (!qualified.equals(type.name()) && types.containsKey(qualified)) return qualified;
		}
		return type.parent();
	}

	public record Type(String name, String parent, String scope, boolean facet) {
	}
}

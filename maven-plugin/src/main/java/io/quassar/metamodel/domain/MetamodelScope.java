package io.quassar.metamodel.domain;

import io.quassar.metamodel.model.Composition;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.TypeDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MetamodelScope {
	private final Map<String, MetamodelType> declarations;
	private final Map<String, String> names = new LinkedHashMap<>();

	public MetamodelScope(Metamodel metamodel) {
		declarations = metamodel.types().stream().collect(Collectors.toMap(MetamodelType::name, type -> type));
		for (var type : metamodel.types()) name(type.name());
	}

	private String scope(MetamodelType type) {
		if (type.declaration() == MetamodelType.Declaration.SUB) return scope(declarations.get(type.parent()));
		return type.scope().isEmpty() ? "" : name(type.scope());
	}

	private String name(String original) {
		if (names.containsKey(original)) return names.get(original);
		MetamodelType type = declarations.get(original);
		if (type == null) return original;
		String scope = scope(type);
		String simple = original.substring(original.lastIndexOf('.') + 1);
		String name = scope.isEmpty() ? simple : scope + "." + simple;
		names.put(original, name);
		return name;
	}

	private boolean root(MetamodelType type) {
		if (type.declaration() == MetamodelType.Declaration.SUB) return root(declarations.get(type.parent()));
		return type.isRoot();
	}

	public List<TypeDefinition> resolve(List<TypeDefinition> types) {
		return types.stream().map(type -> new TypeDefinition(name(type.type()), root(declarations.get(type.type())), type.rootCardinality(),
			type.properties().stream().map(property -> new PropertyDefinition(property.name(), property.position(), property.type(),
				property.referenceTypes().stream().map(this::name).toList(), property.declaredType(), property.constraints(), property.defaultValue())).toList(),
			type.children().stream().map(child -> new Composition(name(child.type()), child.cardinality(), child.modifiers())).toList(),
			type.facets().stream().map(this::name).toList())).toList();
	}

	public List<MetamodelType> types() {
		return declarations.values().stream().map(type -> new MetamodelType(name(type.name()), type.isFacet(),
			type.properties(), type.facets(), type.children(), type.declaration(), scope(type),
			name(type.parent()), type.role(), type.cardinality(), type.modifiers(), type.compositions())).toList();
	}
}

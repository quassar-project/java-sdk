package tests.fixtures;

import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.ResolvedMetamodel;
import io.quassar.metamodel.model.TypeDefinition;
import io.quassar.reader.Models;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public final class VisoraExpectations {
	private static final Pattern NUMBER = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
	private final Map<String, MetamodelType> declarations = new LinkedHashMap<>();
	private final Map<String, TypeDefinition> definitions = new LinkedHashMap<>();

	public VisoraExpectations(ResolvedMetamodel schema) {
		for (var type : schema.declarations()) declarations.put(type.name(), type);
		Stream.concat(schema.types().stream(), schema.facets().stream()).forEach(type -> definitions.put(type.type(), type));
	}

	public TypeDefinition definition(String type) { return definitions.get(type); }

	public void collect(Mogram mogram, List<String> parentTypes, IdentityHashMap<Mogram, List<String>> result) {
		String type = concept(mogram.type(), parentTypes);
		List<String> active = new ArrayList<>();
		if (type != null) {
			active.addAll(lineage(type));
			List<String> concepts = List.copyOf(active);
			for (String facet : mogram.facets()) {
				String resolved = facet(facet, concepts);
				assertThat(resolved).as("Facet %s on %s %s", facet, type, mogram.identifier()).isNotNull();
				for (String ancestor : lineage(resolved)) if (!active.contains(ancestor)) active.add(ancestor);
			}
		}
		result.put(mogram, List.copyOf(active));
		for (Mogram child : mogram.children()) collect(child, active, result);
	}

	private String concept(String name, List<String> parentTypes) {
		for (String parent : parentTypes) {
			String scoped = parent + "." + name;
			if (declarations.containsKey(scoped) && !declarations.get(scoped).isFacet()) return scoped;
		}
		if (declarations.containsKey(name) && !declarations.get(name).isFacet()) return name;
		return null;
	}

	private String facet(String name, List<String> concepts) {
		for (String owner : concepts)
			for (MetamodelType candidate : declarations.values())
				if (candidate.isFacet() && (candidate.name().equals(name) || candidate.name().endsWith("." + name))
						&& declaringConcept(candidate).equals(owner)) return candidate.name();
		return null;
	}

	private String declaringConcept(MetamodelType type) {
		while (type.isFacet()) type = declarations.get(type.scope());
		return type.name();
	}

	private List<String> lineage(String name) {
		List<String> result = new ArrayList<>();
		for (MetamodelType type = declarations.get(name); type != null; type = declarations.get(type.parent())) result.add(type.name());
		return result;
	}

	public Map<String, Value> values(Mogram mogram, List<String> active) throws Exception {
		Map<String, Value> values = new LinkedHashMap<>();
		Map<String, Integer> positions = new LinkedHashMap<>();
		for (Property property : mogram.properties()) {
			if (property.name() != null) continue;
			String constructor = property.constructorType().isEmpty() ? mogram.type() : property.constructorType();
			String type = active.stream().filter(candidate -> candidate.equals(constructor) || candidate.endsWith("." + constructor)).findFirst().orElseThrow();
			int index = property.position() >= 0 ? property.position() : positions.merge(type, 1, Integer::sum) - 1;
			List<PropertyDefinition> properties = constructorProperties(type);
			assertThat(index).as("Constructor %s on %s", type, mogram.identifier()).isLessThan(properties.size());
			values.put(properties.get(index).suggestedName(), property.value());
		}
		for (Property property : mogram.properties())
			if (property.name() != null) values.put(property.name(), property.value());
		for (String type : active)
			for (PropertyDefinition property : definition(type).properties())
				if (!values.containsKey(property.suggestedName()) && property.hasDefault())
					values.put(property.suggestedName(), Models.create("Value holder\n\tvalue = " + property.defaultValue()).get("holder").property("value"));
		return values;
	}

	private List<PropertyDefinition> constructorProperties(String type) {
		Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
		for (String ancestor : lineage(type))
			for (PropertyDefinition property : declarations.get(ancestor).properties()) properties.putIfAbsent(property.suggestedName(), property);
		return List.copyOf(properties.values());
	}

	public static String text(Value value) {
		if (value.isString()) return value.asString();
		if (value.isReference()) return value.asReference();
		if (value.isNumber()) return value.asNumber().toPlainString();
		if (value.isBoolean()) return Boolean.toString(value.asBoolean());
		if (value instanceof Value.Expression expression) return expression.code();
		return "";
	}

	public static BigDecimal number(Value value) {
		if (value.isNumber()) return value.asNumber();
		if (value.isSequence()) return number(value.asSequence().get(0));
		if (value.isEmpty()) return BigDecimal.ZERO;
		var matcher = NUMBER.matcher(text(value));
		assertThat(matcher.find()).as("Numeric value %s", value).isTrue();
		return new BigDecimal(matcher.group());
	}
}

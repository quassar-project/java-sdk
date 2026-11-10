package io.quassar.javacode.domain;

import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyType.Kind;
import io.quassar.metamodel.model.TypeDefinition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JsonTemplateGenerator {
	private static final Pattern SLOT_NAME = Pattern.compile("[a-z][a-z0-9_-]*[a-z0-9]");

	public String generate(Metamodel metamodel) {
		var resolver = new MetamodelResolver();
		var facets = resolver.resolveFacets(metamodel).stream().collect(Collectors.toMap(TypeDefinition::type, type -> type));
		StringBuilder template = new StringBuilder(rootRule(metamodel));
		resolver.resolve(metamodel).stream()
			.sorted(Comparator.comparing((TypeDefinition type) -> !type.type().contains(".")))
			.forEach(type -> template.append(objectRule(type, facets)));
		return template.toString();
	}

	private String rootRule(Metamodel metamodel) {
		String fields = metamodel.types().stream()
			.filter(MetamodelType::isRoot)
			.map(MetamodelType::name)
			.map(this::slotName)
			.filter(this::isSlot)
			.distinct().sorted()
			.map(name -> "\t\t\"" + name + "\": [$" + name + "...[, ]]")
			.collect(Collectors.joining(",\n"));
		String object = fields.isEmpty() ? "\t{}" : "\t{\n" + fields + "\n\t}";
		return "rule type(Model) and trigger(root)\n" + object + "\n";
	}

	private String objectRule(TypeDefinition type, Map<String, TypeDefinition> facets) {
		Map<String, String> fields = new LinkedHashMap<>();
		addProperties(fields, type);
		for (String facet : type.facets()) addProperties(fields, facets.get(facet));
		for (var child : type.children()) {
			String name = slotName(child.type().substring(child.type().lastIndexOf('.') + 1));
			if (isSlot(name)) fields.putIfAbsent(name, field(name, "", !child.cardinality().isSingle()));
		}
		StringBuilder rule = new StringBuilder("\nrule type(").append(type.type()).append(") and attribute(identifier)\n\t{\n");
		fields.values().forEach(field -> rule.append("\t\t").append(field).append('\n'));
		return rule.append("\t\t\"id\": $identifier+JsonString\n\t}\n").toString();
	}

	private void addProperties(Map<String, String> fields, TypeDefinition type) {
		for (var property : type.properties()) {
			String name = slotName(property.suggestedName());
			if (isSlot(name)) fields.putIfAbsent(name, field(name, "+JsonValue", property.type().kind() == Kind.SEQUENCE));
		}
	}

	private String field(String name, String formatter, boolean many) {
		String key = "\"" + name + "\": ";
		String value = "$" + name + formatter;
		return many ? key + "[" + value + "...[, ]]," : "<<" + key + value + ",>>";
	}

	private String slotName(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	private boolean isSlot(String name) {
		return !name.equals("this") && !name.equals("id") && SLOT_NAME.matcher(name).matches();
	}
}

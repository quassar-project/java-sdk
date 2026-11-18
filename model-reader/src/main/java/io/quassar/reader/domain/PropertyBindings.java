package io.quassar.reader.domain;

import io.quassar.reader.domain.NodeSupport.PropertyDefinition;
import io.quassar.reader.domain.NodeSupport.Type;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PropertyBindings {
	private final Map<String, Type> declarations;

	PropertyBindings(Map<String, Type> declarations) {
		this.declarations = declarations;
	}

	List<Property> effective(Mogram mogram, List<String> types) {
		Map<String, Property> values = explicitValues(mogram, types);
		Map<String, PropertyDefinition> definitions = definitions(types);
		addDefaults(values, definitions);
		return values.values().stream().map(property -> normalize(property, definitions.get(property.name()))).toList();
	}

	List<Property> explicit(Mogram mogram, List<String> types) {
		List<Property> properties = new ArrayList<>();
		Map<String, Integer> positions = new LinkedHashMap<>();
		for (Property property : mogram.properties()) {
			if (property.name() != null) properties.add(property);
			else properties.add(bind(property, constructorType(mogram, types, property.constructorType()), positions));
		}
		return List.copyOf(properties);
	}

	private Property bind(Property property, String constructor, Map<String, Integer> positions) {
		int position = property.position() >= 0 ? property.position() : positions.merge(constructor, 1, Integer::sum) - 1;
		List<String> names = propertyNames(constructor);
		String name = position < names.size() ? names.get(position) : null;
		return new Property(name, property.value(), property.origin(), property.constructorType(), property.position());
	}

	private Map<String, Property> explicitValues(Mogram mogram, List<String> types) {
		Map<String, Property> values = new LinkedHashMap<>();
		int position = 0;
		for (Property property : explicit(mogram, types))
			values.put(property.name() == null ? "#" + position++ : property.name(), property);
		for (Property property : mogram.properties())
			if (property.name() != null) values.put(property.name(), property);
		return values;
	}

	private Map<String, PropertyDefinition> definitions(List<String> types) {
		Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
		for (String type : types) {
			Type declaration = declarations.get(type);
			if (declaration != null)
				for (PropertyDefinition property : declaration.properties()) properties.putIfAbsent(property.name(), property);
		}
		return properties;
	}

	private void addDefaults(Map<String, Property> values, Map<String, PropertyDefinition> definitions) {
		for (PropertyDefinition property : definitions.values())
			if (!values.containsKey(property.name()) && !property.defaultValue().isEmpty())
				values.put(property.name(), new Property(property.name(), ModelReader.value(property.defaultValue()), Property.Origin.DEFAULT));
	}

	private Property normalize(Property property, PropertyDefinition definition) {
		Value value = definition == null ? property.value() : PropertyValues.normalize(property.value(), definition.declaredType(), definition.multiple());
		return new Property(property.name(), value, property.origin(), property.constructorType(), property.position());
	}

	private String constructorType(Mogram mogram, List<String> types, String constructor) {
		if (constructor.isEmpty() || constructor.equals(mogram.type())) return types.get(0);
		return types.stream().filter(type -> type.equals(constructor) || type.endsWith("." + constructor)).findFirst().orElse(constructor);
	}

	private List<String> propertyNames(String name) {
		Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
		for (Type type = declarations.get(name); type != null; type = declarations.get(type.parent()))
			for (PropertyDefinition property : type.properties()) properties.putIfAbsent(property.name(), property);
		return List.copyOf(properties.keySet());
	}
}

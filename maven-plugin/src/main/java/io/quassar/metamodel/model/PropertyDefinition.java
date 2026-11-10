package io.quassar.metamodel.model;

import java.util.List;

public record PropertyDefinition(String name, int position, PropertyType type, List<String> referenceTypes, String declaredType, List<String> constraints, String defaultValue) {
	public PropertyDefinition {
		referenceTypes = List.copyOf(referenceTypes);
		constraints = List.copyOf(constraints);
	}

	public PropertyDefinition(String name, int position, PropertyType type, List<String> referenceTypes) {
		this(name, position, type, referenceTypes, "", List.of(), "");
	}

	public boolean hasDefault() {
		return !defaultValue.isEmpty();
	}

	public String suggestedName() {
		return name == null ? "value" + (position + 1) : name;
	}
}

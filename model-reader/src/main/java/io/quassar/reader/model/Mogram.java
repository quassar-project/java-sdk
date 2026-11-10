package io.quassar.reader.model;

import java.util.List;
import java.util.Objects;

public record Mogram(String type, String identifier, List<Property> properties,
					 List<String> facets, List<Mogram> children) {
	public static final Mogram NULL = new Mogram("", "", List.of(), List.of(), List.of());

	public boolean isNull() {
		return this == NULL;
	}

	public Mogram {
		identifier = Objects.requireNonNull(identifier);
		properties = List.copyOf(properties);
		facets = List.copyOf(facets);
		children = List.copyOf(children);
	}

	public Value property(String name) {
		for (Property property : properties) if (Objects.equals(property.name(), name)) return property.value();
		return new Value.Empty();
	}

}

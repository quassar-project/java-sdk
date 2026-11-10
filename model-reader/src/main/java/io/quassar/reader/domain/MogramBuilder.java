package io.quassar.reader.domain;

import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;

import java.util.ArrayList;
import java.util.List;

final class MogramBuilder {
	private final String type;
	private final String identifier;
	private final List<String> facets;
	private final List<Property> properties;
	private final List<MogramBuilder> children = new ArrayList<>();

	MogramBuilder(String type, String identifier, List<Property> properties, List<String> facets) {
		this.type = type;
		this.identifier = identifier;
		this.properties = new ArrayList<>(properties);
		this.facets = List.copyOf(facets);
	}

	void addProperty(Property property) {
		properties.add(property);
	}

	void addChild(MogramBuilder child) {
		children.add(child);
	}

	Mogram build() {
		List<Mogram> mograms = new ArrayList<>();
		for (MogramBuilder child : children) mograms.add(child.build());
		return new Mogram(type, identifier, properties, facets, mograms);
	}
}

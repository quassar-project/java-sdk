package io.quassar.reader.domain;

import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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

	static List<Mogram> build(List<MogramBuilder> roots) {
		Set<String> explicit = new HashSet<>();
		for (MogramBuilder root : roots) root.collectIdentifiers(explicit);
		return identify(roots.stream().map(MogramBuilder::buildTree).toList(), "m1:1", new MogramNames(explicit));
	}

	private void collectIdentifiers(Set<String> identifiers) {
		if (!identifier.isEmpty()) identifiers.add(identifier);
		for (MogramBuilder child : children) child.collectIdentifiers(identifiers);
	}

	private Built buildTree() {
		List<Built> built = children.stream().map(MogramBuilder::buildTree).toList();
		List<MogramIdentity.Child> fingerprints = built.stream()
			.map(child -> new MogramIdentity.Child(child.identifier(), child.fingerprint())).toList();
		String fingerprint = MogramIdentity.fingerprint(type, properties, facets, fingerprints);
		return new Built(type, identifier, List.copyOf(properties), facets, built, fingerprint);
	}

	private static List<Mogram> identify(List<Built> siblings, String parentContext, MogramNames names) {
		Map<MogramIdentity.Declaration, Integer> occurrences = new LinkedHashMap<>();
		List<Mogram> mograms = new ArrayList<>();
		for (Built child : siblings) {
			boolean anonymous = child.identifier().isEmpty();
			MogramIdentity.Declaration declaration = new MogramIdentity.Declaration(child.type(), child.identifier(), anonymous ? child.fingerprint() : "");
			int occurrence = occurrences.merge(declaration, 1, Integer::sum) - 1;
			String context = MogramIdentity.context(parentContext, declaration, occurrence);
			String id = anonymous ? names.identifier(child.type(), context) : child.identifier();
			mograms.add(new Mogram(child.type(), id, child.properties(), child.facets(), identify(child.children(), context, names), anonymous));
		}
		return List.copyOf(mograms);
	}

	private record Built(String type, String identifier, List<Property> properties, List<String> facets,
			List<Built> children, String fingerprint) {
	}
}

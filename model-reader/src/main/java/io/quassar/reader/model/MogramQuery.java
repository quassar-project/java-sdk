package io.quassar.reader.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record MogramQuery(Model model, List<Mogram> mograms) {
	public MogramQuery {
		Objects.requireNonNull(model);
		mograms = List.copyOf(mograms);
	}

	public MogramQuery type(String type) {
		return where(mogram -> mogram.type().equals(type));
	}

	public MogramQuery identifier(String identifier) {
		return where(mogram -> mogram.identifier().equals(identifier));
	}

	public MogramQuery facet(String facet) {
		return where(mogram -> mogram.facets().contains(facet));
	}

	public MogramQuery withProperty(String name) {
		return where(mogram -> mogram.properties().stream()
				.anyMatch(property -> Objects.equals(property.name(), name)));
	}

	public MogramQuery withProperty(String name, Value value) {
		return withProperty(name).where(mogram -> mogram.property(name).equals(value));
	}

	public MogramQuery where(Predicate<Mogram> condition) {
		List<Mogram> matches = new ArrayList<>();
		for (Mogram mogram : mograms) if (condition.test(mogram)) matches.add(mogram);
		return new MogramQuery(model, matches);
	}

	public MogramQuery children() {
		List<Mogram> children = new ArrayList<>();
		for (Mogram mogram : mograms) children.addAll(mogram.children());
		return distinct(children);
	}

	public MogramQuery descendants() {
		List<Mogram> descendants = new ArrayList<>();
		for (Mogram mogram : mograms) collectDescendants(mogram, descendants);
		return distinct(descendants);
	}

	public MogramQuery parents() {
		List<Mogram> parents = new ArrayList<>();
		for (Mogram mogram : mograms) {
			Mogram parent = model.parentOf(mogram);
			if (!parent.isNull()) parents.add(parent);
		}
		return distinct(parents);
	}

	public List<Mogram> list() {
		return mograms;
	}

	public Stream<Mogram> stream() {
		return mograms.stream();
	}

	public Mogram first() {
		return mograms.isEmpty() ? Mogram.NULL : mograms.get(0);
	}

	public int count() {
		return mograms.size();
	}

	public boolean isEmpty() {
		return mograms.isEmpty();
	}

	private MogramQuery distinct(List<Mogram> candidates) {
		Set<Mogram> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Mogram> unique = new ArrayList<>();
		for (Mogram mogram : candidates) if (seen.add(mogram)) unique.add(mogram);
		return new MogramQuery(model, unique);
	}

	private static void collectDescendants(Mogram mogram, List<Mogram> descendants) {
		for (Mogram child : mogram.children()) {
			descendants.add(child);
			collectDescendants(child, descendants);
		}
	}
}

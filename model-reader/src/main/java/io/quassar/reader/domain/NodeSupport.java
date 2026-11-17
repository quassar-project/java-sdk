package io.quassar.reader.domain;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Node;
import io.quassar.reader.model.NodeTypeHierarchy;
import io.quassar.reader.model.Value;
import io.quassar.templating.ports.FrameView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class NodeSupport {
	private final Map<String, Type> declarations;
	private final Map<String, String> aliases;
	private final NodeTypeHierarchy hierarchy;

	public NodeSupport(List<Type> types) {
		Map<String, Type> names = new LinkedHashMap<>();
		Map<String, String> aliases = new LinkedHashMap<>();
		for (Type type : types) {
			names.put(type.name(), type);
			Set<String> children = new HashSet<>(type.children());
			for (String child : type.children()) {
				String simple = child.substring(child.lastIndexOf('.') + 1);
				if (children.add(simple)) aliases.put(type.name() + "." + simple, child);
			}
		}
		declarations = Map.copyOf(names);
		this.aliases = Map.copyOf(aliases);
		hierarchy = new NodeTypeHierarchy(types.stream()
			.map(type -> new NodeTypeHierarchy.Type(type.name(), type.parent(), type.scope(), type.facet())).toList());
	}

	public record Type(String name, String parent, String scope, boolean facet,
			List<String> children, Class<? extends Node> decorator) {
		public Type {
			children = List.copyOf(children);
		}
	}

	public String resolve(Mogram mogram, Model model) {
		String type = mogram.type();
		if (mogram.isNull() || type.indexOf('.') >= 0) return type;
		Mogram parent = model.parentOf(mogram);
		if (!parent.isNull())
			for (String scope : types(parent, model)) {
				String scoped = resolve(type, scope);
				if (!scoped.equals(type)) return scoped;
			}
		return type;
	}

	public String resolve(String type, String parentType) {
		if (type.indexOf('.') >= 0) return type;
		for (Type parent = declarations.get(parentType); parent != null; parent = declarations.get(parent.parent())) {
			String contextual = aliases.get(parent.name() + "." + type);
			if (contextual != null) return contextual;
			String qualified = parent.name() + "." + type;
			if (declarations.containsKey(qualified)) return qualified;
		}
		return type;
	}

	public boolean matches(String actual, String expected) {
		if (actual.equals(expected)) return true;
		List<Type> matches = declarations.values().stream()
			.filter(type -> (type.name().equals(actual) || type.name().endsWith("." + actual)) && inherits(type, expected))
			.limit(2).toList();
		return matches.size() == 1;
	}

	private boolean inherits(Type type, String expected) {
		for (Type current = type; current != null; current = declarations.get(current.parent()))
			if (current.name().equals(expected)) return true;
		return false;
	}

	public <T extends Node> T as(Class<T> type, Mogram mogram, Model model) {
		if (type.equals(Node.class)) return type.cast(wrap(mogram, model));
		String name = NodeFactory.type(type);
		Type declaration = declarations.get(name);
		if (declaration == null || declaration.decorator() != type)
			throw new IllegalArgumentException("Unknown node type: " + type.getName());
		boolean match = !mogram.isNull() && types(mogram, model).contains(name);
		return NodeFactory.create(type, match ? mogram : Mogram.NULL, model);
	}

	public List<String> types(Mogram mogram, Model model) {
		return hierarchy.types(resolve(mogram, model), mogram.facets());
	}

	public FrameView frame(Mogram mogram, Model model, List<String> types) {
		return new MogramFrame(mogram, model, types, this);
	}

	public FrameView frame(Model model) {
		return new ModelFrame(model, this);
	}

	public List<FrameView.Slot> groupSlots(List<Mogram> members, Model model) {
		Map<String, List<Node>> groups = new LinkedHashMap<>();
		for (Mogram member : members) {
			String name = member.type().substring(member.type().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
			groups.computeIfAbsent(name, key -> new ArrayList<>()).add(of(member, model));
		}
		List<FrameView.Slot> slots = new ArrayList<>();
		groups.forEach((name, group) -> slots.add(new FrameView.ValueSlot(name, List.copyOf(group))));
		return List.copyOf(slots);
	}

	public static List<Value> values(Value value) {
		if (value.isSequence()) return value.asSequence();
		return value.isEmpty() ? List.of() : List.of(value);
	}

	public static String word(Value value) {
		if (value instanceof Value.Number number) return number.value().toPlainString();
		if (value instanceof Value.Bool bool) return Boolean.toString(bool.value());
		return value.isReference() ? value.asReference() : value.asString();
	}

	public Node of(Mogram mogram, Model model) {
		return of(mogram, model, Mogram.NULL);
	}

	public Node wrap(Mogram mogram, Model model) {
		return of(mogram, model, mogram);
	}

	private Node of(Mogram mogram, Model model, Mogram fallback) {
		Type type = declarations.get(resolve(mogram, model));
		if (type == null || type.facet()) return new GenericNode(fallback, model);
		return NodeFactory.create(type.decorator(), mogram, model);
	}

	private static final class GenericNode extends Node {
		private GenericNode(Mogram mogram, Model model) {
			super(mogram, model);
		}
	}

	private record ModelFrame(Model model, NodeSupport support) implements FrameView {
		public Object frameIdentity() {
			return model;
		}

		public Object frameParent() {
			return null;
		}

		public List<String> frameTypes() {
			return List.of("Model");
		}

		public List<Slot> frameSlots() {
			return support.groupSlots(model.mograms(), model);
		}
	}

	private record MogramFrame(Mogram mogram, Model model, java.util.List<java.lang.String> types, NodeSupport support) implements io.quassar.templating.ports.FrameView {
		public java.lang.Object frameIdentity() {
			return mogram.isNull() ? null : mogram;
		}

		public io.quassar.reader.model.Node frameParent() {
			return support.of(model.parentOf(mogram), model);
		}

		public java.util.List<java.lang.String> frameTypes() {
			var names = new java.util.LinkedHashSet<java.lang.String>();
			names.add(mogram.type());
			names.add(support.resolve(mogram, model));
			names.addAll(mogram.facets());
			for (var type : types) {
				names.add(type);
				names.add(type.substring(type.lastIndexOf('.') + 1));
			}
			return java.util.List.copyOf(names);
		}

		public java.util.List<Slot> frameSlots() {
			var properties = new java.util.LinkedHashMap<java.lang.String, Slot>();
			properties.put("identifier", new ValueSlot("identifier", mogram.identifier()));
			int position = 0;
			for (var property : mogram.properties()) {
				java.lang.String name = property.name() == null ? "value" + ++position : property.name();
				name = name.toLowerCase(java.util.Locale.ROOT);
				properties.put(name, new ValueSlot(name, frameValues(property.value())));
			}
			java.util.List<Slot> slots = new java.util.ArrayList<>(properties.values());
			slots.addAll(support.groupSlots(mogram.children(), model));
			return java.util.List.copyOf(slots);
		}

		private java.util.List<java.lang.Object> frameValues(Value value) {
			if (value.isSequence()) return value.asSequence().stream().flatMap(item -> frameValues(item).stream()).toList();
			if (value.isString()) return java.util.List.of(value.asString());
			if (value.isNumber()) return java.util.List.of(value.asNumber());
			if (value.isBoolean()) return java.util.List.of(value.asBoolean());
			if (value.isReference()) return java.util.List.of(support.of(model.get(value, mogram), model));
			return java.util.List.of();
		}
	}
}

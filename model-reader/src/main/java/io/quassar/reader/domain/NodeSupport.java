package io.quassar.reader.domain;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Node;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.NodeTypeHierarchy;
import io.quassar.reader.model.Value;
import io.quassar.templating.ports.FrameView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class NodeSupport {
	private final Map<String, Type> declarations;
	private final Map<String, String> aliases;
	private final NodeTypeHierarchy hierarchy;
	private final PropertyBindings propertyBindings;
	private final Model model;

	public NodeSupport(List<Type> types) {
		model = null;
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
		propertyBindings = new PropertyBindings(declarations);
		this.aliases = Map.copyOf(aliases);
		hierarchy = new NodeTypeHierarchy(types.stream()
			.map(type -> new NodeTypeHierarchy.Type(type.name(), type.parent(), type.scope(), type.facet())).toList());
	}

	private NodeSupport(NodeSupport schema, Model model) {
		declarations = schema.declarations;
		aliases = schema.aliases;
		hierarchy = schema.hierarchy;
		propertyBindings = schema.propertyBindings;
		this.model = model;
	}

	public NodeSupport forModel(Model model) {
		return this.model == model ? this : new NodeSupport(this, model);
	}

	public boolean sameSchema(NodeSupport other) {
		return declarations == other.declarations;
	}

	public record Type(String name, String parent, String scope, boolean facet,
			List<String> children, List<PropertyDefinition> properties, Class<? extends Node> decorator) {
		public Type {
			children = List.copyOf(children);
			properties = List.copyOf(properties);
		}

		public Type(String name, String parent, String scope, boolean facet, List<String> children, Class<? extends Node> decorator) {
			this(name, parent, scope, facet, children, List.of(), decorator);
		}
	}

	public record PropertyDefinition(String name, String declaredType, boolean multiple, String defaultValue) {
	}

	public boolean hasProperty(Mogram mogram, String name) {
		return propertyBindings.explicit(mogram, types(mogram)).stream().anyMatch(property -> name.equals(property.name()));
	}

	public Value property(Mogram mogram, String name) {
		for (Property property : propertyBindings.effective(mogram, types(mogram)))
			if (name.equals(property.name())) return property.value();
		return new Value.Empty();
	}

	public String resolve(Mogram mogram) {
		String type = mogram.type();
		if (mogram.isNull() || type.indexOf('.') >= 0) return type;
		Mogram parent = model.parentOf(mogram);
		if (!parent.isNull())
			for (String scope : types(parent)) {
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

	public boolean is(Class<? extends Node> type, Mogram mogram) {
		if (type.equals(Node.class)) return !mogram.isNull();
		var definition = definition(type);
		return definition != null && matches(mogram, definition);
	}

	public <T extends Node> T as(Class<T> type, Mogram mogram) {
		if (type.equals(Node.class)) return type.cast(wrap(mogram));
		var definition = definition(type);
		if (definition == null) throw new IllegalArgumentException("Unknown node type: " + type.getName());
		return type.cast(definition.create(matches(mogram, definition) ? mogram : Mogram.NULL, model));
	}

	private NodeFactory.Definition definition(Class<? extends Node> type) {
		var definition = NodeFactory.definition(type);
		Type declaration = declarations.get(definition.name());
		return declaration != null && declaration.decorator() == type ? definition : null;
	}

	private boolean matches(Mogram mogram, NodeFactory.Definition definition) {
		return !mogram.isNull() && types(mogram).contains(definition.name());
	}

	public <T extends Node> T ancestor(Class<T> type, Mogram mogram) {
		Mogram parent = model.parentOf(mogram);
		while (!parent.isNull()) {
			if (is(type, parent)) return as(type, parent);
			parent = model.parentOf(parent);
		}
		return as(type, parent);
	}

	public List<String> types(Mogram mogram) {
		return hierarchy.types(resolve(mogram), mogram.facets());
	}

	public FrameView frame(Mogram mogram) {
		return new MogramFrame(mogram, types(mogram), this);
	}

	public FrameView frame(Model model) {
		return new ModelFrame(forModel(model));
	}

	public List<FrameView.Slot> groupSlots(List<Mogram> members) {
		Map<String, List<Node>> groups = new LinkedHashMap<>();
		for (Mogram member : members) {
			String name = member.type().substring(member.type().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
			groups.computeIfAbsent(name, key -> new ArrayList<>()).add(of(member));
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
		return forModel(model).of(mogram);
	}

	public Node of(Mogram mogram) {
		return of(mogram, Mogram.NULL);
	}

	public Node wrap(Mogram mogram) {
		return of(mogram, mogram);
	}

	private Node of(Mogram mogram, Mogram fallback) {
		Type type = declarations.get(resolve(mogram));
		if (type == null || type.facet()) return new GenericNode(fallback, model);
		return NodeFactory.create(type.decorator(), mogram, model);
	}

	private static final class GenericNode extends Node {
		private GenericNode(Mogram mogram, Model model) {
			super(mogram, model);
		}
	}

	private record ModelFrame(NodeSupport support) implements FrameView {
		public Object frameIdentity() {
			return support.model;
		}

		public Object frameParent() {
			return null;
		}

		public List<String> frameTypes() {
			return List.of("Model");
		}

		public List<Slot> frameSlots() {
			return support.groupSlots(support.model.mograms());
		}
	}

	private record MogramFrame(Mogram mogram, List<String> types, NodeSupport support) implements FrameView {
		public Object frameIdentity() {
			return mogram.isNull() ? null : mogram;
		}

		public Node frameParent() {
			return support.of(support.model.parentOf(mogram));
		}

		public List<String> frameTypes() {
			var names = new LinkedHashSet<String>();
			names.add(mogram.type());
			names.add(support.resolve(mogram));
			names.addAll(mogram.facets());
			for (var type : types) {
				names.add(type);
				names.add(type.substring(type.lastIndexOf('.') + 1));
			}
			return List.copyOf(names);
		}

		public List<Slot> frameSlots() {
			var properties = new LinkedHashMap<String, Slot>();
			properties.put("identifier", new ValueSlot("identifier", mogram.identifier()));
			int position = 0;
			for (var property : support.propertyBindings.effective(mogram, types)) {
				String name = property.name() == null ? "value" + ++position : property.name();
				name = name.toLowerCase(Locale.ROOT);
				properties.put(name, new ValueSlot(name, frameValues(property.value())));
			}
			List<Slot> slots = new ArrayList<>(properties.values());
			slots.addAll(support.groupSlots(mogram.children()));
			return List.copyOf(slots);
		}

		private List<Object> frameValues(Value value) {
			if (value.isSequence()) return value.asSequence().stream().flatMap(item -> frameValues(item).stream()).toList();
			if (value.isString()) return List.of(value.asString());
			if (value.isNumber()) return List.of(value.asNumber());
			if (value.isBoolean()) return List.of(value.asBoolean());
			if (value.isReference()) return List.of(support.of(support.model.get(value, mogram)));
			return List.of();
		}
	}
}

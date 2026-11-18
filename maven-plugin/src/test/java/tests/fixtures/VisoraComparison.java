package tests.fixtures;

import io.quassar.javacode.domain.JavaNames;
import io.quassar.metamodel.model.Composition;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.ResolvedMetamodel;
import io.quassar.metamodel.model.TypeDefinition;
import io.quassar.reader.domain.NodeFactory;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Node;
import io.quassar.reader.model.Resource;
import io.quassar.reader.model.Value;
import io.quassar.templating.ports.FrameView;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public final class VisoraComparison {
	private final VisoraArchive archive;
	private final Model raw;
	private final GeneratedDecorators generated;
	private final ResolvedMetamodel schema;
	private final VisoraExpectations expected;
	private final Map<String, Class<? extends Node>> classes;
	private final IdentityHashMap<Mogram, List<String>> types = new IdentityHashMap<>();
	private final IdentityHashMap<Mogram, Boolean> reached = new IdentityHashMap<>();
	private final List<String> failures = new ArrayList<>();
	private final Set<String> missingResources = new TreeSet<>();
	private final Set<String> unresolvedReferences = new TreeSet<>();
	private int nodes;
	private int properties;
	private int childGroups;
	private int references;
	private int resolvedReferences;

	public VisoraComparison(VisoraArchive archive, GeneratedDecorators generated, ResolvedMetamodel schema) throws Exception {
		this.archive = archive;
		this.raw = generated.model();
		this.generated = generated;
		this.schema = schema;
		expected = new VisoraExpectations(schema);
		classes = classes(generated);
	}

	public void verify() throws Exception {
		for (Mogram root : raw.mograms()) expected.collect(root, List.of(), types);
		verifySourceCoverage();
		verifyRoots();
		for (Mogram mogram : raw.allMograms())
			if (!types.get(mogram).isEmpty()) verifyNode(mogram);
		assertThat(failures).as("All node and value comparisons").isEmpty();
		assertThat(reached).as("Visora nodes reachable through generated root and child getters").hasSize(nodes);
	}

	public Coverage coverage() { return new Coverage(nodes, properties, childGroups, references, resolvedReferences); }
	public Set<String> missingResources() { return Set.copyOf(missingResources); }
	public Set<String> unresolvedReferences() { return Set.copyOf(unresolvedReferences); }

	private void verifySourceCoverage() throws Exception {
		var origins = archive.origins(raw);
		for (Mogram mogram : raw.allMograms())
			assertThat(!types.get(mogram).isEmpty()).as("%s: %s %s", origins.get(mogram), mogram.type(), mogram.identifier())
				.isEqualTo(origins.get(mogram).startsWith("views/visora/"));
	}

	private void verifyRoots() throws Exception {
		Object facade = generated.modelDecorator("VisoraModel");
		for (TypeDefinition type : schema.types()) {
			if (!type.root()) continue;
			check("model root " + type.type(), () -> {
				String name = lower(simple(type.type()));
				List<Node> actual = nodes(call(facade, type.rootCardinality().isSingle() ? name : plural(name)));
				List<Node> wanted = raw.mograms().stream().filter(root -> types.get(root).contains(type.type()))
					.map(root -> create(type.type(), root)).toList();
				assertThat(actual).containsExactlyElementsOf(wanted);
				reached(actual);
			});
		}
	}

	private void verifyNode(Mogram mogram) throws Exception {
		nodes++;
		List<String> active = types.get(mogram);
		Node node = create(active.get(0), mogram);
		String label = active.get(0) + " " + mogram.identifier();
		check(label + " types", () -> assertThat(node.types()).containsExactlyElementsOf(active));
		Map<String, Value> values = expected.values(mogram, active);
		Set<String> names = active.stream().flatMap(type -> expected.definition(type).properties().stream())
			.map(PropertyDefinition::suggestedName).collect(Collectors.toSet());
		for (String name : values.keySet()) check(label + " declared property " + name, () -> assertThat(names).contains(name));
		for (String type : active) verifyView(mogram, node, type, values);
	}

	private void verifyView(Mogram mogram, Node original, String type, Map<String, Value> values) {
		Node view = create(type, mogram);
		String label = type + " " + mogram.identifier();
		check(label + " type conversion", () -> {
			assertThat(original.is(classes.get(type))).isTrue();
			assertThat(original.as(classes.get(type))).isEqualTo(view);
			assertThat(view.types()).containsExactlyElementsOf(types.get(mogram));
			Node parent = (Node) FrameView.from(view).frameParent();
			Mogram parentMogram = raw.parentOf(mogram);
			assertThat(parent.isNull()).isEqualTo(parentMogram.isNull());
			if (!parentMogram.isNull()) assertThat(FrameView.from(parent).frameIdentity()).isSameAs(parentMogram);
		});
		for (PropertyDefinition property : expected.definition(type).properties()) {
			properties++;
			String name = property.suggestedName();
			check(label + " property " + name, () -> compare(property, values.getOrDefault(name, new Value.Empty()), call(view, name), mogram));
		}
		for (Composition child : expected.definition(type).children()) verifyChildren(mogram, view, child);
	}

	private void verifyChildren(Mogram mogram, Node view, Composition child) {
		childGroups++;
		check(mogram.type() + " " + mogram.identifier() + " children " + child.type(), () -> {
			List<Mogram> children = mogram.children().stream().filter(candidate -> types.get(candidate).contains(child.type())).toList();
			String name = lower(simple(child.type()));
			List<Node> actual = nodes(call(view, child.cardinality().isSingle() ? name : plural(name)));
			List<Node> wanted = children.stream().map(candidate -> create(child.type(), candidate)).toList();
			assertThat(actual).containsExactlyElementsOf(wanted);
			reached(actual);
		});
	}

	private void compare(PropertyDefinition property, Value value, Object actual, Mogram owner) throws Exception {
		if (property.declaredType().equals("resource")) compareResource(value, actual);
		else if (actual instanceof List<?> list) compareList(property, value, list, owner);
		else if (actual instanceof Node node) compareReference(value, node, owner);
		else if (actual instanceof Number number)
			assertThat(new BigDecimal(number.toString())).isEqualByComparingTo(VisoraExpectations.number(value));
		else if (actual instanceof Enum<?> enumeration)
			assertThat(enumeration.name()).isEqualTo(JavaNames.className(VisoraExpectations.text(value)));
		else if (actual instanceof Boolean flag) assertThat(flag).isEqualTo(value.asBoolean());
		else if (actual instanceof String text) assertThat(text).isEqualTo(value.isString() ? value.asString() : "");
		else assertThat(value.isEmpty()).isTrue();
	}

	private void compareList(PropertyDefinition property, Value value, List<?> actual, Mogram owner) throws Exception {
		List<Value> values = value.isEmpty() ? List.of() : value.isSequence() ? value.asSequence() : List.of(value);
		assertThat(actual).hasSize(values.size());
		for (int i = 0; i < actual.size(); i++) compare(property, values.get(i), actual.get(i), owner);
	}

	private void compareReference(Value value, Node actual, Mogram owner) {
		references++;
		Mogram target = raw.get(value, owner);
		assertThat(actual.isNull()).isEqualTo(target.isNull());
		if (target.isNull()) {
			if (value.isReference()) unresolvedReferences.add(value.asReference());
			return;
		}
		resolvedReferences++;
		assertThat(actual).isEqualTo(NodeFactory.create(actual.getClass(), target, generated.model()));
	}

	private void compareResource(Value value, Object actual) throws Exception {
		Resource wanted = raw.resource(value);
		if (wanted == null) {
			if (!value.isEmpty()) missingResources.add(VisoraExpectations.text(value));
			assertThat(actual).isNull();
			return;
		}
		assertThat(actual).isInstanceOf(Resource.class);
		assertThat(((Resource) actual).name()).isEqualTo(wanted.name());
		assertThat(((Resource) actual).bytes()).isEqualTo(wanted.bytes());
	}

	private Node create(String type, Mogram mogram) {
		assertThat(classes).as("Generated class for %s", type).containsKey(type);
		return NodeFactory.create(classes.get(type), mogram, generated.model());
	}

	private void reached(List<Node> nodes) {
		for (Node node : nodes) reached.put((Mogram) FrameView.from(node).frameIdentity(), true);
	}

	private void check(String label, Checked check) {
		try { check.run(); }
		catch (Exception | AssertionError failure) { failures.add(label + ": " + failure); }
	}

	private static Map<String, Class<? extends Node>> classes(GeneratedDecorators generated) throws Exception {
		Map<String, Class<? extends Node>> classes = new LinkedHashMap<>();
		Path directory = Path.of(generated.loader().getURLs()[0].toURI());
		try (var files = Files.walk(directory)) {
			for (Path file : files.filter(file -> file.toString().endsWith(".class")).toList()) {
				String name = directory.relativize(file).toString().replace('/', '.').replace(".class", "");
				Class<?> type = generated.loader().loadClass(name);
				if (Node.class.isAssignableFrom(type)) classes.put(NodeFactory.type(type.asSubclass(Node.class)), type.asSubclass(Node.class));
			}
		}
		return classes;
	}

	private static List<Node> nodes(Object result) {
		if (result instanceof List<?> list) return list.stream().map(Node.class::cast).toList();
		return ((Node) result).isNull() ? List.of() : List.of((Node) result);
	}

	private static Object call(Object target, String name) throws Exception { return target.getClass().getMethod(name).invoke(target); }
	private static String simple(String name) { return name.substring(name.lastIndexOf('.') + 1); }
	private static String lower(String name) { return Character.toLowerCase(name.charAt(0)) + name.substring(1); }
	private static String plural(String name) {
		if (name.endsWith("y") && "aeiou".indexOf(name.charAt(name.length() - 2)) < 0) return name.substring(0, name.length() - 1) + "ies";
		return name.endsWith("s") || name.endsWith("x") || name.endsWith("ch") || name.endsWith("sh") ? name + "es" : name + "s";
	}

	public record Coverage(int nodes, int properties, int childGroups, int references, int resolvedReferences) {
	}

	@FunctionalInterface
	private interface Checked { void run() throws Exception; }
}

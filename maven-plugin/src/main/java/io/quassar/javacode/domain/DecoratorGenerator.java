package io.quassar.javacode.domain;

import io.quassar.javacode.ports.SourceTemplates;

import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.PropertyType;
import io.quassar.metamodel.model.PropertyType.Kind;
import io.quassar.metamodel.model.TypeDefinition;
import io.quassar.metamodel.model.Composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DecoratorGenerator {
	private final SourceTemplates templates;

	public DecoratorGenerator(SourceTemplates templates) {
		this.templates = templates;
	}
	private static final String VALUE = "io.quassar.reader.model.Value";
	private static final String MOGRAM = "io.quassar.reader.model.Mogram";
	private static final String MODEL = "io.quassar.reader.model.Model";
	private static final String DECORATOR = "MogramDecorator";
	private static final String MOGRAM_TYPES = "MogramTypes";
	private static final Set<String> RESERVED_TYPES = Set.of(DECORATOR, MOGRAM_TYPES, "NullObject", "MogramFrame", "ModelFrame", "Slot", "Getter", "ValueSlot");
	private static final Set<String> RESERVED_METHODS = Set.of("mogram", "model", "identifier", "isNull", "frameView", "frameIdentity", "frameParent", "frameTypes", "frameSlots", "getClass",
		"hashCode", "toString", "equals", "clone", "finalize", "wait", "notify", "notifyAll");

	public List<JavaCode> generate(List<TypeDefinition> types, List<TypeDefinition> facets,
			Map<String, String> enclosingTypes, String packageName, String modelName) {
		JavaNames names = new JavaNames(RESERVED_TYPES);
		String facadeName = names.claim(modelClassName(modelName));
		Set<String> exposedFacets = new HashSet<>(enclosingTypes.keySet());
		exposedFacets.addAll(enclosingTypes.values());
		List<TypeDefinition> declarations = Stream.concat(types.stream(), facets.stream()).toList();
		for (TypeDefinition declaration : declarations) {
			exposedFacets.addAll(declaration.facets());
			addTargets(exposedFacets, declaration);
		}

		List<TypeDefinition> decorators = Stream.concat(types.stream(),
			facets.stream().filter(facet -> exposedFacets.contains(facet.type()))).toList();
		Map<String, String> classNames = new LinkedHashMap<>();
		for (TypeDefinition type : decorators) {
			if (enclosingTypes.containsKey(type.type())) continue;
			classNames.put(type.type(), names.claim(JavaNames.className(type.type())));
		}
		Map<String, List<TypeDefinition>> nested = decorators.stream()
			.filter(type -> enclosingTypes.containsKey(type.type()))
			.sorted(Comparator.comparing(TypeDefinition::type))
			.collect(Collectors.groupingBy(type -> enclosingTypes.get(type.type()), LinkedHashMap::new, Collectors.toList()));
		Set<String> topLevel = new HashSet<>(classNames.values());
		topLevel.add(facadeName);
		Set<String> nestedNames = new HashSet<>();
		nested.forEach((owner, children) -> {
			String parent = classNames.get(owner);
			Set<String> reserved = new HashSet<>(RESERVED_TYPES);
			reserved.addAll(Arrays.asList(parent.split("\\.")));
			if (packageName.isEmpty()) reserved.addAll(topLevel);
			JavaNames scoped = new JavaNames(reserved);
			for (TypeDefinition child : children) {
				String name = scoped.claim(JavaNames.className(simpleName(child.type())));
				nestedNames.add(name);
				classNames.put(child.type(), parent + "." + name);
			}
		});

		Map<String, String> targets = new LinkedHashMap<>();
		classNames.forEach((type, name) -> targets.put(type, referenceName(name, packageName, nestedNames)));
		Map<String, String> classes = new LinkedHashMap<>();
		for (TypeDefinition type : types) classes.put(type.type(), targets.get(type.type()));
		List<JavaCode> code = new ArrayList<>();
		code.add(decoratorBase(packageName, classes));
		code.add(mogramTypes(packageName, declarations));
		code.add(modelDecorator(types, packageName, facadeName, classes));
		for (TypeDefinition type : decorators) {
			if (enclosingTypes.containsKey(type.type())) continue;
			code.add(new JavaCode(packageName, classNames.get(type.type()),
				render(type, classNames, targets, nested, false)));
		}
		topLevel.addAll(nestedNames);
		return code.stream().map(source -> JavaImports.format(source, topLevel)).toList();
	}

	private String referenceName(String name, String packageName, Set<String> nestedNames) {
		int separator = name.indexOf('.');
		String topLevel = separator < 0 ? name : name.substring(0, separator);
		return nestedNames.contains(topLevel) ? packageName + "." + name : name;
	}

	private void addTargets(Set<String> targets, TypeDefinition type) {
		for (PropertyDefinition property : type.properties()) targets.addAll(property.referenceTypes());
		for (Composition child : type.children()) targets.add(child.type());
	}

	public static String modelClassName(String modelName) {
		return new JavaNames(RESERVED_TYPES).claim(JavaNames.className(modelName));
	}

	private JavaCode modelDecorator(List<TypeDefinition> types, String packageName, String name,
			Map<String, String> classes) {
		StringBuilder source = new StringBuilder();
		for (String sourceType : List.of("java.nio.file.Path", "java.net.URL", "java.net.URI"))
			source.append(loadMethod(name, sourceType));
		JavaNames methods = new JavaNames(RESERVED_METHODS);
		for (TypeDefinition type : types) {
			if (!type.root()) continue;
			String decorator = classes.get(type.type());
			boolean single = type.rootCardinality().isSingle();
			String getter = memberName(methods, type.type(), single);
			String filter = ".type(" + literal(type.type()) + ")";
			if (single)
				source.append(method(decorator, getter, "new " + decorator + "(model.roots()" + filter + ".first(), model)"));
			else source.append(collectionMethod(decorator, getter, "model.roots()" + filter + ".stream()"));
		}
		return new JavaCode(packageName, name, templates.render("model", Map.of(
			"name", name, "modelType", MODEL, "methods", source.toString())));
	}

	private String loadMethod(String modelName, String sourceType) {
		return templates.render("load", Map.of("name", modelName, "sourceType", sourceType));
	}

	private String render(TypeDefinition type, Map<String, String> classNames, Map<String, String> classes,
			Map<String, List<TypeDefinition>> nested, boolean isNested) {
		StringBuilder source = new StringBuilder();
		JavaNames methods = new JavaNames(RESERVED_METHODS);
		for (PropertyDefinition property : type.properties()) {
			String getter = methods.claim(JavaNames.methodName(property.suggestedName()));
			source.append(method(returnType(property, classes), getter, expression(property, classes)));
		}
		for (String facet : type.facets()) addFacetMethods(source, methods, facet, classes.get(facet));
		for (Composition composition : type.children()) {
			String child = composition.type();
			String decorator = classes.get(child);
			if (decorator == null) throw new IllegalArgumentException("Unknown component " + child + " in " + type.type());
			boolean single = composition.cardinality().isSingle();
			String simple = simpleName(child);
			String getter = memberName(methods, single ? simple : child, single);
			String match = MOGRAM_TYPES + ".resolve(child.type(), " + literal(type.type()) + ").equals(" + literal(child) + ")";
			String stream = "mogram.children().stream()\n\t.filter(child -> " + match + ")";
			if (single)
				source.append(method(decorator, getter, "new " + decorator + "(" + stream
					+ ".findFirst().orElse(" + MOGRAM + ".NULL), model)"));
			else source.append(collectionMethod(decorator, getter, stream));
		}
		for (TypeDefinition child : nested.getOrDefault(type.type(), List.of())) {
			String declaration = render(child, classNames, classes, nested, true);
			source.append('\n').append(declaration.lines()
				.map(line -> line.isEmpty() ? line : "\t" + line)
				.collect(Collectors.joining("\n", "", "\n")));
		}
		return templates.render("mogram", Map.of(
			"name", simpleName(classNames.get(type.type())), "modifier", isNested ? "static " : "",
			"mogramType", MOGRAM, "modelType", MODEL, "methods", source.toString()));
	}

	private void addFacetMethods(StringBuilder source, JavaNames methods, String facet, String decorator) {
		String simple = simpleName(facet);
		String suffix = JavaNames.className(simple);
		String check = methods.claim("is" + suffix);
		String condition = "mogram.facets().contains(" + literal(facet) + ")";
		if (!simple.equals(facet)) condition += " || mogram.facets().contains(" + literal(simple) + ")";
		source.append(method("boolean", check, condition));
		source.append(method(decorator, methods.claim("as" + suffix),
			"new " + decorator + "(" + check + "() ? mogram : " + MOGRAM + ".NULL, model)"));
	}

	private String simpleName(String name) {
		return name.substring(name.lastIndexOf('.') + 1);
	}

	private String memberName(JavaNames methods, String type, boolean single) {
		String name = JavaNames.methodName(type);
		return methods.claim(single ? name : JavaNames.plural(name));
	}

	private String collectionMethod(String decorator, String getter, String stream) {
		return method("java.util.List<" + decorator + ">", getter, stream
			+ "\n\t.map(mogram -> new " + decorator + "(mogram, model))\n\t.toList()");
	}

	private JavaCode mogramTypes(String packageName, List<TypeDefinition> types) {
		StringBuilder aliases = new StringBuilder();
		for (TypeDefinition type : types) {
			Set<String> names = new HashSet<>();
			for (Composition child : type.children()) names.add(child.type());
			for (Composition child : type.children()) {
				String simple = simpleName(child.type());
				if (names.add(simple))
					aliases.append("\t\t\tcase ").append(literal(type.type() + "." + simple))
						.append(" -> ").append(literal(child.type())).append(";\n");
			}
		}
		return new JavaCode(packageName, MOGRAM_TYPES, templates.render("types",
			Map.of("mogramType", MOGRAM, "modelType", MODEL, "aliases", aliases.toString())));
	}

	private JavaCode decoratorBase(String packageName, Map<String, String> classes) {
		StringBuilder cases = new StringBuilder();
		classes.forEach((type, name) -> cases.append(templates.render("factory-case", Map.of("type", literal(type), "name", name))));
		return new JavaCode(packageName, DECORATOR, templates.render("decorator", Map.of(
			"mogramType", MOGRAM, "modelType", MODEL,
			"valueType", VALUE, "cases", cases.toString())));
	}

	private String expression(PropertyDefinition property, Map<String, String> classes) {
		String value = property.name() == null
			? "mogram.properties().stream().filter(property -> property.name() == null)"
				+ ".skip(" + property.position() + ").findFirst().map(io.quassar.reader.model.Property::value)"
				+ ".orElse(new " + VALUE + ".Empty())"
			: "mogram.property(" + literal(property.name()) + ")";
		PropertyType type = property.type();
		if (type.kind() == Kind.REFERENCE) return wrapReference(value, property, classes);
		if (type.kind() != Kind.SEQUENCE) return value + accessor(type.kind());
		String expression = DECORATOR + ".values(" + value + ")";
		if (type.element() == Kind.REFERENCE)
			return expression + ".stream().map(value -> " + wrapReference("value", property, classes) + ").toList()";
		String accessor = accessor(type.element());
		if (accessor.isEmpty()) return expression;
		return expression + ".stream().map(" + VALUE + "::" + accessor.substring(1, accessor.length() - 2) + ").toList()";
	}

	private String wrapReference(String value, PropertyDefinition property, Map<String, String> classes) {
		String target = referenceType(property, classes);
		String mogram = "model.get(" + value + ", mogram)";
		return target.equals(DECORATOR) ? DECORATOR + ".of(" + mogram + ", model)"
			: "new " + target + "(" + mogram + ", model)";
	}

	private String returnType(PropertyDefinition property, Map<String, String> classes) {
		PropertyType type = property.type();
		if (type.kind() == Kind.REFERENCE) return referenceType(property, classes);
		if (type.kind() != Kind.SEQUENCE) return scalarType(type.kind(), false);
		String element = type.element() == Kind.REFERENCE ? referenceType(property, classes) : scalarType(type.element(), true);
		return "java.util.List<" + element + ">";
	}

	private String referenceType(PropertyDefinition property, Map<String, String> classes) {
		return property.referenceTypes().size() == 1 ? classes.getOrDefault(property.referenceTypes().get(0), DECORATOR) : DECORATOR;
	}

	private String scalarType(Kind kind, boolean boxed) {
		return switch (kind) {
			case STRING -> "java.lang.String";
			case NUMBER -> "java.math.BigDecimal";
			case BOOLEAN -> boxed ? "java.lang.Boolean" : "boolean";
			default -> VALUE;
		};
	}

	private String accessor(Kind kind) {
		return switch (kind) {
			case STRING -> ".asString()";
			case NUMBER -> ".asNumber()";
			case BOOLEAN -> ".asBoolean()";
			default -> "";
		};
	}

	private String method(String type, String name, String expression) {
		return templates.render("method", Map.of("type", type, "name", name, "expression", expression));
	}

	private String literal(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
			.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
	}
}

package io.quassar.metamodel.domain;

import io.quassar.metamodel.model.Cardinality;
import io.quassar.metamodel.model.Composition;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.MetamodelType.Declaration;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.PropertyType;
import io.quassar.metamodel.model.PropertyType.Kind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MetamodelReader {
	public Metamodel read(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		LineReader lines = new LineReader();
		String line;
		while ((line = reader.readLine()) != null) lines.read(line);
		return lines.build();
	}

	private static final class LineReader {
		private final Map<String, TypeBuilder> types = new LinkedHashMap<>();
		private final Deque<Frame> scope = new ArrayDeque<>();

		private void read(String line) {
			int offset = 0;
			int indent = 0;
			while (offset < line.length() && Character.isWhitespace(line.charAt(offset)))
				indent = line.charAt(offset++) == '\t' ? (indent / 4 + 1) * 4 : indent + 1;
			StringBuilder statement = new StringBuilder();
			char quote = 0;
			int braces = 0;
			int inlineIndent = indent;
			for (int i = offset; i < line.length(); i++) {
				char character = line.charAt(i);
				if (quote != 0) {
					statement.append(character);
					if (character == '\\' && i + 1 < line.length()) statement.append(line.charAt(++i));
					else if (character == quote) quote = 0;
					continue;
				}
				if (line.startsWith("//", i) || line.startsWith("!!", i)) break;
				if (character == '"' || character == '\'') quote = character;
				if (character == '{') braces++;
				if (character == '}') braces--;
				if (braces == 0 && (character == '>' || character == ';')) {
					readStatement(statement.toString().strip(), inlineIndent);
					statement.setLength(0);
					if (character == '>') inlineIndent += 4;
				} else statement.append(character);
			}
			readStatement(statement.toString().strip(), inlineIndent);
		}

		private void readStatement(String statement, int indent) {
			if (statement.isEmpty() || statement.startsWith("dsl ")) return;
			while (!scope.isEmpty() && scope.peek().indent() >= indent) scope.pop();
			if (statement.startsWith("def ")) addProperty(property(statement.substring(4)));
			else if (statement.startsWith("has")) readComposition(statement);
			else if (statement.matches("(?:Concept|facet|sub)(?:[:\\s].*)?")) readType(statement, indent);
		}

        private void addProperty(PropertyDefinition property) {
            TypeBuilder owner = scope.peek().type();
            var previous = owner.properties.stream().filter(candidate -> candidate.name().equals(property.name())).findFirst();
            if (previous.isPresent()) {
                if (!previous.get().equals(property)) throw new IllegalArgumentException("Conflicting property definition: " + owner.name + "." + property.name());
                return;
            }
            owner.properties.add(property);
        }

		private void readComposition(String statement) {
			Header header = header(statement);
			TypeBuilder owner = scope.peek().type();
			owner.children.add(header.name());
			owner.compositions.add(new Composition(header.name(), header.cardinality(), header.modifiers()));
		}

		private void readType(String statement, int indent) {
			Header header = header(statement);
			TypeBuilder owner = scope.isEmpty() ? null : scope.peek().type();
			Declaration declaration = switch (header.keyword()) {
				case "Concept" -> Declaration.CONCEPT;
				case "facet" -> Declaration.FACET;
				default -> Declaration.SUB;
			};
			String path = owner == null ? header.name() : owner.name + "." + header.name();
			TypeBuilder type = types.computeIfAbsent(path, key -> new TypeBuilder(key, header, declaration, owner));
			if (owner != null && declaration == Declaration.CONCEPT) {
				owner.children.add(path);
				owner.compositions.add(new Composition(path, header.cardinality(), header.modifiers()));
			}
			if (owner != null && declaration == Declaration.FACET) owner.facets.add(path);
			scope.push(new Frame(indent, type));
		}

		private Metamodel build() {
			return new Metamodel(types.values().stream().map(TypeBuilder::build).toList());
		}
	}

	private static PropertyDefinition property(String statement) {
		int boundary = typeBoundary(statement);
		String declaration = statement.substring(0, boundary);
		String remainder = withoutAnnotations(statement.substring(boundary).strip());
		int equals = remainder.indexOf('=');
		String name = (equals < 0 ? remainder : remainder.substring(0, equals)).strip();
		String defaultValue = equals < 0 ? "" : remainder.substring(equals + 1).strip();
		int constraint = declaration.indexOf(":{");
		String declaredType = constraint < 0 ? declaration : declaration.substring(0, constraint);
		List<String> constraints = constraint < 0 ? List.of() : words(declaration.substring(constraint + 2, declaration.length() - 1));
		int bracket = declaredType.indexOf('[');
        String scalar = bracket < 0 ? declaredType : declaredType.substring(0, bracket);
        String cardinality = bracket < 0 ? "" : declaredType.substring(bracket + 1, declaredType.indexOf(']', bracket));
        String maximum = cardinality.contains("..") ? cardinality.substring(cardinality.indexOf("..") + 2) : cardinality;
        boolean multiple = bracket >= 0 && !maximum.equals("1") && !maximum.equals("0");
		Kind kind = switch (scalar) {
			case "string", "word", "resource", "date", "time", "datetime" -> Kind.STRING;
			case "integer", "long", "double", "float", "decimal", "number" -> Kind.NUMBER;
			case "boolean" -> Kind.BOOLEAN;
			default -> Kind.REFERENCE;
		};
		PropertyType type = multiple ? new PropertyType(Kind.SEQUENCE, kind) : PropertyType.scalar(kind);
		return new PropertyDefinition(name, -1, type, kind == Kind.REFERENCE ? List.of(scalar) : List.of(),
			declaredType, constraints, defaultValue);
	}

    private static String withoutAnnotations(String text) {
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (text.startsWith("as", i) && i > 0 && Character.isWhitespace(text.charAt(i - 1)) &&
                    i + 2 < text.length() && Character.isWhitespace(text.charAt(i + 2))) return text.substring(0, i).strip();
        }
        return text;
    }

	private static int typeBoundary(String statement) {
		int braces = 0;
		for (int i = 0; i < statement.length(); i++) {
			char character = statement.charAt(i);
			if (character == '{') braces++;
			if (character == '}') braces--;
			if (braces == 0 && Character.isWhitespace(character)) return i;
		}
		return statement.length();
	}

	private static Header header(String statement) {
		int boundary = typeBoundary(statement);
		String declaration = statement.substring(0, boundary);
		String[] names = statement.substring(boundary).strip().split("\\s+as\\s+", 2);
		String[] inheritance = names[0].split("\\s+extends\\s+", 2);
		String[] tokens = declaration.split(":");
		List<String> modifiers = new ArrayList<>();
		int min = 0;
		int max = -1;
		for (int i = 1; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.startsWith("{")) {
				String[] bounds = token.substring(1, token.length() - 1).split("\\.\\.");
				min = Integer.parseInt(bounds[0]);
				max = bounds[1].equals("*") ? -1 : Integer.parseInt(bounds[1]);
			} else {
				modifiers.add(token);
				if (token.equals("Single")) max = 1;
				if (token.equals("Required")) min = 1;
				if (token.equals("Optional")) min = 0;
			}
		}
		return new Header(tokens[0], inheritance[0], inheritance.length == 1 ? "" : inheritance[1],
			names.length == 1 ? "" : names[1], new Cardinality(min, max), modifiers);
	}

	private static List<String> words(String text) {
		return Arrays.stream(text.strip().split("[\\s,]+")).filter(word -> !word.isEmpty()).toList();
	}

	private record Header(String keyword, String name, String parent, String role, Cardinality cardinality, List<String> modifiers) {
	}

	private record Frame(int indent, TypeBuilder type) {
	}

	private static final class TypeBuilder {
		private final String name;
		private final Header header;
		private final Declaration declaration;
		private final String scope;
		private final String parent;
		private final boolean facet;
		private final List<PropertyDefinition> properties = new ArrayList<>();
		private final List<String> facets = new ArrayList<>();
		private final List<String> children = new ArrayList<>();
		private final List<Composition> compositions = new ArrayList<>();

		private TypeBuilder(String name, Header header, Declaration declaration, TypeBuilder owner) {
			this.name = name;
			this.header = header;
			this.declaration = declaration;
			this.scope = owner == null ? "" : owner.name;
			this.parent = declaration == Declaration.SUB ? scope : header.parent();
			this.facet = declaration == Declaration.FACET || declaration == Declaration.SUB && owner.facet;
		}

		private MetamodelType build() {
			return new MetamodelType(name, facet, properties, facets, children, declaration, scope, parent,
				header.role(), header.cardinality(), header.modifiers(), compositions);
		}
	}
}

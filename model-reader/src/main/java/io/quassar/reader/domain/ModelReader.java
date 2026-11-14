package io.quassar.reader.domain;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class ModelReader {
	public Model read(InputStream input) throws IOException {
		return readTree(linesOf(input)).build();
	}

	private static LineNumberReader linesOf(InputStream input) {
		return new LineNumberReader(new InputStreamReader(input, UTF_8));
	}

	private MogramTree readTree(LineNumberReader lines) throws IOException {
		MogramTree tree = new MogramTree();
		for (String line = lines.readLine(); line != null; line = lines.readLine())
			tree.readLine(ModelTokenizer.readLine(line), lines.getLineNumber());
		return tree;
	}

	static final class LineReader {
		private final List<String> tokens;
		private int position;

		LineReader(List<String> tokens) {
			this.tokens = tokens;
		}

		private String currentToken() {
			return position < tokens.size() ? tokens.get(position) : "";
		}

		private String nextToken() {
			return tokens.get(position++);
		}

		boolean take(String token) {
			if (!currentToken().equals(token)) return false;
			position++;
			return true;
		}

		boolean isProperty() {
			return position + 1 < tokens.size() && tokens.get(position + 1).equals("=");
		}

		private boolean isValueEnd() {
			return currentToken().isEmpty() || List.of(",", ")", ">").contains(currentToken());
		}

		Property readProperty() {
			String name = nextToken();
			nextToken();
			return new Property(name, readValue());
		}

		MogramBuilder readMogram() {
			String type = nextToken();
			List<Property> properties = readProperties();
			String identifier = readIdentifier();
			List<String> facets = readFacets(properties);
			return new MogramBuilder(type, identifier, properties, facets);
		}

		private List<String> readFacets(List<Property> properties) {
			List<String> facets = new ArrayList<>();
			if (take("is")) {
				while (!currentToken().isEmpty() && !currentToken().equals(">")) {
					String name = nextToken();
					facets.add(name);
					properties.addAll(readProperties());
				}
			}
			return facets;
		}

		private String readIdentifier() {
			if (currentToken().isEmpty() || currentToken().equals("is") || currentToken().equals(">")) return "";
			return nextToken();
		}

		private List<Property> readProperties() {
			List<Property> result = new ArrayList<>();
			if (!take("(")) return result;
			while (!take(")")) {
				String name = null;
				if (isProperty()) {
					name = nextToken();
					nextToken();
				}
				result.add(new Property(name, readValue()));
				take(",");
			}
			return result;
		}

		private Value readValue() {
			List<Value> values = new ArrayList<>();
			while (!isValueEnd()) values.add(readAtom(nextToken()));
			if (values.isEmpty()) return new Value.Empty();
			return values.size() == 1 ? values.get(0) : new Value.Sequence(values);
		}

		private Value readAtom(String text) {
			if (text.equals("empty")) return new Value.Empty();
			if (text.startsWith("\'")) return new Value.Expression(unquote(text));
			if (text.startsWith("\"")) return new Value.Text(unquote(text));
			if (text.equals("true") || text.equals("false")) return new Value.Bool(Boolean.parseBoolean(text));
			try {
				return new Value.Number(new BigDecimal(text));
			} catch (NumberFormatException ignored) {
				return new Value.Reference(text);
			}
		}

		private String unquote(String text) {
			StringBuilder result = new StringBuilder();
			for (int i = 1; i < text.length() - 1; i++) {
				char c = text.charAt(i);
				if (c == '\\' && i + 1 < text.length() - 1) {
					char escaped = text.charAt(++i);
					switch (escaped) {
						case 'n':
							result.append('\n');
							break;
						case 'r':
							result.append('\r');
							break;
						case 't':
							result.append('\t');
							break;
						case '\'':
						case '"':
						case '\\':
							result.append(escaped);
							break;
						default:
							result.append('\\').append(escaped);
							break;
					}
				} else result.append(c);
			}
			return result.toString();
		}
	}
}

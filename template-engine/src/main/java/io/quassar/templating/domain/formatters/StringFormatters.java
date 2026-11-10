package io.quassar.templating.domain.formatters;

import io.quassar.templating.model.Frame;
import io.quassar.templating.ports.Formatter;
import io.quassar.templating.domain.formatters.inflectors.EnglishPluralInflector;
import io.quassar.templating.domain.formatters.inflectors.SpanishPluralInflector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class StringFormatters {

	public static Map<String, Formatter> get(Locale locale) {
		Map<String, Formatter> map = map();
		map.put("UpperCase", upperCase());
		map.put("LowerCase", lowerCase());
		map.put("CamelCase", camelCase());
		map.put("SnakeCase", snakeCase());
		map.put("KebabCase", kebabCase());
		map.put("PascalCase", pascalCase());
		map.put("FirstUpperCase", firstUpperCase());
		map.put("FirstLowerCase", firstLowerCase());
		map.put("Capitalize", capitalize());
		map.put("Length", length());
		map.put("OnlyDigits", onlyDigits());
		map.put("OnlyLetters", onlyLetters());
		map.put("OnlyDigitsAndLetters", onlyDigitsAndLetters());
		map.put("JsonString", StringFormatters::jsonString);
		map.put("JsonValue", StringFormatters::jsonValue);
		map.put("Plural", plural(locale));
		return map;
	}

	private static String jsonValue(Object value) {
		if (value instanceof Number || value instanceof Boolean) return value.toString();
		if (value instanceof Frame frame) {
			var identifiers = frame.frames("identifier");
			return jsonString(identifiers.hasNext() ? identifiers.next().value() : "");
		}
		return jsonString(value);
	}

	private static String jsonString(Object value) {
		StringBuilder quoted = new StringBuilder("\"");
		for (char character : value.toString().toCharArray()) {
			quoted.append(switch (character) {
				case '"' -> "\\\"";
				case '\\' -> "\\\\";
				case '\b' -> "\\b";
				case '\f' -> "\\f";
				case '\n' -> "\\n";
				case '\r' -> "\\r";
				case '\t' -> "\\t";
				default -> character < ' ' || Character.isSurrogate(character)
					? String.format("\\u%04x", (int) character) : String.valueOf(character);
			});
		}
		return quoted.append('"').toString();
	}

	public static Formatter onlyDigitsAndLetters() {
		return value -> value.toString().replaceAll("[^0-9A-Za-z]", "");
	}

	public static Formatter onlyLetters() {
		return value -> value.toString().replaceAll("[^A-Za-z]", "");
	}

	public static Formatter onlyDigits() {
		return value -> value.toString().replaceAll("[^0-9]", "");
	}

	public static HashMap<String, Formatter> map() {
		return new HashMap<String, Formatter>() {
			@Override
			public Formatter put(String key, Formatter value) {
				return super.put(key.toLowerCase(), value);
			}
		};
	}

	public static Formatter upperCase() {
		return value -> value.toString().toUpperCase();
	}

	public static Formatter lowerCase() {
		return value -> value.toString().toLowerCase();
	}

	public static Formatter camelCase() {
		return value -> {
			String source = value.toString();
			String[] parts = source.split("[ _\\-]");
			if (parts.length > 1) {
				String result = "";
				for (String part : parts) result = result + capitalize().format(part);
				return result.substring(0, 1).toLowerCase() + result.substring(1);
			}
			return source.substring(0, 1).toLowerCase() + source.substring(1);
		};
	}

	public static Formatter pascalCase() {
		return value -> {
			String source = value.toString();
			String[] parts = source.split("[ _\\-]");
			if (parts.length > 1) {
				String result = "";
				for (String part : parts) result = result + capitalize().format(part);
				return result.substring(0, 1).toUpperCase() + result.substring(1);
			}
			return source.substring(0, 1).toUpperCase() + source.substring(1);
		};
	}

	public static Formatter snakeCase() {
		return value -> value.toString().toLowerCase().replaceAll(" -", "_");
	}

	public static Formatter kebabCase() {
		return value -> value.toString().toLowerCase().replaceAll(" _", "-");
	}

	public static Formatter firstUpperCase() {
		return value -> value.toString().isEmpty() ? "" : value.toString().substring(0, 1).toUpperCase() + value.toString().substring(1);
	}

	public static Formatter firstLowerCase() {
		return value -> value.toString().isEmpty() ? "" : value.toString().substring(0, 1).toLowerCase() + value.toString().substring(1);
	}

	public static Formatter capitalize() {
		return value -> value.toString().isEmpty() ? "" : value.toString().substring(0, 1).toUpperCase() + value.toString().substring(1).toLowerCase();
	}

	public static Formatter length() {
		return value -> String.valueOf(value.toString().length());
	}

	private static Formatter plural(Locale locale) {
		PluralInflector pluralInflector = (locale.getLanguage().equals("es")) ?
				new SpanishPluralInflector() :
				new EnglishPluralInflector();
		return value -> pluralInflector.plural(value.toString());
	}

	public abstract static class PluralInflector {
		private final Map<String, String> irregulars = new HashMap<>();
		private final Map<String, String> replaces = new HashMap<>();

		public PluralInflector() {
			setIrregulars();
			setReplaces();
		}

		public abstract String plural(String word);

		protected abstract void setReplaces();

		protected abstract void setIrregulars();

		protected void addReplace(String from, String to) {
			replaces.put(from, to);
		}

		protected void addIrregular(String from, String to) {
			irregulars.put(from, to);
		}

		protected String doReplaces(String word) {
			for (String ending : replaces.keySet()) {
				if (!word.endsWith(ending)) continue;
				return replaceLast(word, ending);
			}
			return word;
		}

		protected char[] consonants() {
			return "bcdfghjklmnpqrstvwxyz".toCharArray();
		}

		protected boolean isIrregular(String word) {
			return irregulars.containsKey(word);
		}

		protected String irregularPlural(String word) {
			return irregulars.get(word);
		}

		private String replaceLast(String word, String ending) {
			return removeEnding(word, ending) + replaces.get(ending);
		}

		private String removeEnding(String word, String ending) {
			return word.substring(0, word.length() - ending.length());
		}
	}
}

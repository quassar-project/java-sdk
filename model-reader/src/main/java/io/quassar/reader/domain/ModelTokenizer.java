package io.quassar.reader.domain;

import java.util.ArrayList;
import java.util.List;

final class ModelTokenizer {
	private static final int TAB_WIDTH = 4;
	private static final String DELIMITERS = "()=,>";

	private final String text;
	private int position;

	private ModelTokenizer(String text) {
		this.text = text;
	}

	static Line readLine(String text) {
		ModelTokenizer tokenizer = new ModelTokenizer(text);
		return new Line(indentation(text), tokenizer.readTokens());
	}

	private List<String> readTokens() {
		List<String> tokens = new ArrayList<>();
		while (position < text.length()) {
			if (Character.isWhitespace(text.charAt(position))) {
				position++;
			} else if (text.startsWith("//", position)) {
				break;
			} else tokens.add(readToken());
		}
		return tokens;
	}

	private String readToken() {
		int start = position;
		char first = text.charAt(position++);
		if (first == '"' || first == '\'') {
			skipQuotedText(first);
		} else if (!isDelimiter(first)) skipUnquotedText();
		return text.substring(start, position);
	}

	private void skipQuotedText(char quote) {
		while (position < text.length()) {
			char character = text.charAt(position++);
			if (character == '\\' && position < text.length()) {
				position++;
			} else if (character == quote) return;
		}
	}

	private void skipUnquotedText() {
		while (position < text.length()) {
			char character = text.charAt(position);
			if (Character.isWhitespace(character) || isDelimiter(character)
					|| text.startsWith("//", position)) {
				return;
			}
			position++;
		}
	}

	private static boolean isDelimiter(char character) {
		return DELIMITERS.indexOf(character) >= 0;
	}

	private static int indentation(String text) {
		int column = 0;
		for (char character : text.toCharArray()) {
			if (character == ' ') {
				column++;
			} else if (character == '\t') {
				column += TAB_WIDTH - column % TAB_WIDTH;
			} else break;
		}
		return column;
	}

	record Line(int indentation, List<String> tokens) {
		Line {
			tokens = List.copyOf(tokens);
		}
	}
}

package io.quassar.reader.adapters;

import java.io.InputStream;
import java.util.List;

public final class TextSource extends AbstractSource {
	private final List<String> contents;

	public TextSource(List<String> contents) {
		this.contents = List.copyOf(contents);
	}

	@Override
	public InputStream open() {
		return combine(contents);
	}
}

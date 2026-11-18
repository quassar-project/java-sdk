package io.quassar.reader.adapters;

import io.quassar.reader.model.ModelManifest;
import io.quassar.reader.ports.ManifestReader;

import java.util.List;
import java.util.regex.Pattern;

public final class ModelHeaderReader implements ManifestReader {
	private static final Pattern DSL_HEADER = Pattern.compile("(?m)^[\\t ]*dsl[\\t ]+(\\S+)");

	@Override
	public ModelManifest read(String content) {
		String text = content.startsWith("\uFEFF") ? content.substring(1) : content;
		var header = DSL_HEADER.matcher(text);
		return new ModelManifest(header.find() ? List.of(header.group(1)) : List.of());
	}
}

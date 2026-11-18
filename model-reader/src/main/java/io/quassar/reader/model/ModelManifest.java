package io.quassar.reader.model;

import java.util.List;

public record ModelManifest(List<String> views) {
	public ModelManifest {
		views = List.copyOf(views);
	}
}

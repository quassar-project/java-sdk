package io.quassar.reader.domain;

import io.quassar.reader.model.ModelManifest;

import java.io.IOException;

public final class ModelCompatibility {
	private ModelCompatibility() {
	}

	public static void require(String expected, ModelManifest manifest, String source) throws IOException {
		if (manifest.views().isEmpty()) throw new IOException("Expected DSL " + expected + " but no DSL is declared in " + source);
		if (!manifest.views().contains(expected))
			throw new IOException("Expected DSL " + expected + " but found " + String.join(", ", manifest.views()) + " in " + source);
	}
}

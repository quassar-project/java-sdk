package io.quassar.reader.ports;

import io.quassar.reader.model.ModelManifest;

import java.io.IOException;

@FunctionalInterface
public interface ManifestReader {
	ModelManifest read(String content) throws IOException;
}

package io.quassar.metamodel.ports;

import io.quassar.metamodel.model.MetamodelManifest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface MetamodelSource {
	InputStream open() throws IOException;

	default Content openContent() throws IOException {
		return new Content(open(), Optional.empty());
	}

	record Content(InputStream input, Optional<MetamodelManifest> manifest) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			input.close();
		}
	}
}

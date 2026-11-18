package io.quassar.reader.ports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@FunctionalInterface
public interface ReleaseSource {
	Content read() throws IOException;

	record Content(InputStream input, String manifest, Map<String, byte[]> resources) implements AutoCloseable {
		public Content {
			resources = Map.copyOf(resources);
		}

		@Override
		public void close() throws IOException {
			input.close();
		}
	}
}

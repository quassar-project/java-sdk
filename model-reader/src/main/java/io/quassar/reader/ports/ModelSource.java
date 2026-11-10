package io.quassar.reader.ports;

import java.io.IOException;
import java.io.InputStream;

public interface ModelSource {
	InputStream open() throws IOException;
}

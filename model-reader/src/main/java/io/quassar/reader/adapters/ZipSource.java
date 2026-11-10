package io.quassar.reader.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZipSource extends AbstractSource {
	private final Path path;

	public ZipSource(Path path) {
		this.path = path;
	}

	public Path path() {
		return path;
	}

	@Override
	public InputStream open() throws IOException {
		try (InputStream input = Files.newInputStream(path)) {
			return unzip(input);
		}
	}

}

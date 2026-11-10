package io.quassar.reader;

import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.ports.ModelSource;
import io.quassar.reader.adapters.FolderSource;
import io.quassar.reader.adapters.UrlSource;
import io.quassar.reader.adapters.ZipSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Models {
	private Models() {
	}

	public static Model load(Path path) throws IOException {
		return read(Files.isDirectory(path) ? new FolderSource(path) : new ZipSource(path));
	}

	public static Model load(URL url) throws IOException {
		return read(new UrlSource(url));
	}

	public static Model load(URI uri) throws IOException {
		return load(uri.toURL());
	}

	private static Model read(ModelSource source) throws IOException {
		try (InputStream input = source.open()) {
			return new ModelReader().read(input);
		}
	}
}

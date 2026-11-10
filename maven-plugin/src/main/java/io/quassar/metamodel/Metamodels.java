package io.quassar.metamodel;

import io.quassar.metamodel.domain.MetamodelReader;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.ports.MetamodelSource;
import io.quassar.metamodel.adapters.FolderSource;
import io.quassar.metamodel.adapters.UrlSource;
import io.quassar.metamodel.adapters.ZipSource;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Metamodels {
	private Metamodels() {
	}

	public static Metamodel load(Path path) throws IOException {
		return read(Files.isDirectory(path) ? new FolderSource(path) : new ZipSource(path));
	}

	public static Metamodel load(URL url) throws IOException {
		return read(new UrlSource(url));
	}

	public static Metamodel load(URI uri) throws IOException {
		return load(uri.toURL());
	}

	private static Metamodel read(MetamodelSource source) throws IOException {
		try (var content = source.openContent()) {
			return new Metamodel(new MetamodelReader().read(content.input()).types(), content.manifest());
		}
	}
}

package io.quassar.metamodel.adapters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZipSource extends AbstractMetamodelSource {
	private final Path path;

	public ZipSource(Path path) {
		this.path = path;
	}

	@Override
	public Content openContent() throws IOException {
		return unzip(Files.newInputStream(path));
	}
}

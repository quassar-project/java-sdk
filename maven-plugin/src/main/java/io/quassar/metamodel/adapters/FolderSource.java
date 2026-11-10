package io.quassar.metamodel.adapters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FolderSource extends AbstractMetamodelSource {
	private final Path path;

	public FolderSource(Path path) {
		this.path = path;
	}

	@Override
	public Content openContent() throws IOException {
		List<String> contents = new ArrayList<>();
		try (var paths = Files.walk(path)) {
			for (Path file : paths.filter(Files::isRegularFile).filter(this::isTara).sorted().toList())
				contents.add(Files.readString(file));
		}
		if (contents.isEmpty()) throw new IOException("Expected .tara files in M2 folder " + path);
		Path manifest = path.resolve(MANIFEST);
		return combine(contents, Files.isRegularFile(manifest) ? Files.readString(manifest) : null);
	}

	private boolean isTara(Path file) {
		return file.getFileName().toString().endsWith(".tara");
	}
}

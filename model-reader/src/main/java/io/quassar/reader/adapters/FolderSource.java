package io.quassar.reader.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class FolderSource extends AbstractSource {
	private final Path path;

	public FolderSource(Path path) {
		this.path = path;
	}

	public Path path() {
		return path;
	}

	@Override
	public InputStream open() throws IOException {
		List<String> contents = new ArrayList<>();
		for (Path file : taraFiles()) contents.add(Files.readString(file));
		return combine(contents);
	}

	private List<Path> taraFiles() throws IOException {
		List<Path> files = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(path)) {
			Iterator<Path> iterator = paths.iterator();
			while (iterator.hasNext()) {
				Path file = iterator.next();
				if (Files.isRegularFile(file) && file.toString().endsWith(".tara")) files.add(file);
			}
		}
		Collections.sort(files);
		return files;
	}
}

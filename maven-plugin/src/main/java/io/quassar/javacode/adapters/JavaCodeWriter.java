package io.quassar.javacode.adapters;

import io.quassar.javacode.model.JavaCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JavaCodeWriter {
	public List<Path> write(List<JavaCode> code, Path output) throws IOException {
		List<Path> files = new ArrayList<>();
		for (JavaCode unit : code) {
			Path directory = output.resolve(unit.packageName().replace('.', '/'));
			Files.createDirectories(directory);
			Path file = directory.resolve(unit.className() + ".java");
			Files.writeString(file, unit.content());
			files.add(file);
		}
		return List.copyOf(files);
	}
}

package io.quassar.javacode.adapters;

import io.quassar.javacode.model.TemplateDefinition;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.ports.TemplateReadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TemplateFiles {
	public List<TemplateDefinition> read(Path directory) throws IOException, TemplateReadException {
		if (!Files.exists(directory)) return List.of();
		List<TemplateDefinition> definitions = new ArrayList<>();
		try (var paths = Files.walk(directory)) {
			for (Path path : paths.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".itr")).sorted().toList())
				definitions.add(read(directory, path));
		}
		return definitions;
	}

	private TemplateDefinition read(Path directory, Path path) throws IOException, TemplateReadException {
		String filename = directory.relativize(path).toString();
		try (var input = Files.newInputStream(path)) {
			return new TemplateDefinition(filename, new TemplateReader(input).read().ruleSet());
		} catch (TemplateReadException exception) {
			throw new TemplateReadException(filename + ": " + exception.getMessage(), exception);
		}
	}
}

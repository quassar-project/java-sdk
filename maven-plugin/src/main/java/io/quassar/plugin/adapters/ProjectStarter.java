package io.quassar.plugin.adapters;

import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.domain.JsonTemplateGenerator;
import io.quassar.javacode.domain.StarterGenerator;
import io.quassar.javacode.model.JavaCode;
import io.quassar.javacode.model.StarterDefinition;
import io.quassar.metamodel.model.Metamodel;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public record ProjectStarter(Path sourceDirectory, String packageName) {
	public void writeInterpreter(String modelClass, List<JavaCode> templates) throws IOException {
		write(generator().interpreter(new StarterDefinition(packageName, Optional.empty(), modelClass, templates)));
	}

	public void writeMojo(Optional<String> modelName, String modelClass, List<JavaCode> templates) throws IOException {
		write(generator().mojo(new StarterDefinition(packageName, modelName, modelClass, templates)));
	}

	public void writeJsonTemplate(Path directory, Metamodel metamodel) throws IOException {
		write(directory.resolve("model-json.itr"), new JsonTemplateGenerator().generate(metamodel));
	}

	public boolean hasJavaSources() throws IOException {
		if (Files.notExists(sourceDirectory)) return false;
		try (var sources = Files.walk(sourceDirectory)) {
			return sources.anyMatch(path -> path.getFileName().toString().endsWith(".java") && Files.isRegularFile(path));
		}
	}

	private StarterGenerator generator() {
		return new StarterGenerator(new ResourceTemplates(ProjectStarter.class, List.of("interpreter", "starter-mojo")));
	}

	private void write(JavaCode code) throws IOException {
		write(sourceDirectory.resolve(code.packageName().replace('.', '/')).resolve(code.className() + ".java"), code.content());
	}

	private void write(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		try {
			Files.writeString(path, content, CREATE_NEW, WRITE);
		} catch (FileAlreadyExistsException ignored) {
		}
	}
}

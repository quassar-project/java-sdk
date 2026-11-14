package io.quassar.plugin.adapters;

import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.domain.JavaNames;
import io.quassar.javacode.domain.JsonTemplateGenerator;
import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.Metamodel;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public record ProjectStarter(Path sourceDirectory, String packageName) {
	private static final String INTERPRETER_CLASS = "Interpreter";
	private static final Set<String> INTERPRETER_NAMES = Set.of(INTERPRETER_CLASS, "String", "System", "IOException", "Path");
	private static final Set<String> MOJO_NAMES = Set.of("AbstractMojo", "MojoExecutionException",
		"LifecyclePhase", "Mojo", "Parameter", "IOException", "File", "Files", "MavenProject", "URL", "Input");

	public void writeInterpreter(String modelClass, List<JavaCode> templates) throws IOException {
		var values = sourceValues();
		values.put("className", INTERPRETER_CLASS);
		if (modelClass != null) addModel(values, modelClass, templates, INTERPRETER_NAMES);
		writeJava(INTERPRETER_CLASS, "interpreter", values);
	}

	public void writeMojo(Optional<String> modelName, String modelClass, List<JavaCode> templates) throws IOException {
		String className = modelName.map(name -> JavaNames.className(name) + "Mojo").orElse("GenerateMojo");
		var values = sourceValues();
		values.put("className", className);
		String baseClass = "org.apache.maven.plugin.AbstractMojo";
		if (className.equals("AbstractMojo")) values.put("baseClass", baseClass);
		else {
			values.put("baseClass", "AbstractMojo");
			values.put("baseImport", baseClass);
		}
		if (modelClass != null) addModel(values, modelClass, templates, MOJO_NAMES);
		writeJava(className, "starter-mojo", values);
	}

	private Map<String, String> sourceValues() {
		Map<String, String> values = new HashMap<>();
		if (!packageName.isEmpty()) values.put("package", packageName);
		return values;
	}

	private void writeJava(String className, String template, Map<String, String> values) throws IOException {
		String content = new ResourceTemplates(ProjectStarter.class, List.of(template)).render(template, values);
		Path path = sourceDirectory.resolve(packageName.replace('.', '/')).resolve(className + ".java");
		write(path, content);
	}

	public void writeJsonTemplate(Path directory, Metamodel metamodel) throws IOException {
		write(directory.resolve("model-json.itr"), new JsonTemplateGenerator().generate(metamodel));
	}

	private void write(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		try {
			Files.writeString(path, content, CREATE_NEW, WRITE);
		} catch (FileAlreadyExistsException ignored) {
		}
	}

	public boolean hasJavaSources() throws IOException {
		if (Files.notExists(sourceDirectory)) return false;
		try (var sources = Files.walk(sourceDirectory)) {
			return sources.anyMatch(path -> path.getFileName().toString().endsWith(".java") && Files.isRegularFile(path));
		}
	}

	private void addModel(Map<String, String> values, String modelClass, List<JavaCode> templates, Set<String> reservedNames) {
		String name = modelClass.substring(modelClass.lastIndexOf('.') + 1);
		if (templates.size() == 1) {
			JavaCode template = templates.get(0);
			values.put("renderer", template.className());
			values.put("rendererImport", template.packageName() + "." + template.className());
		}
		if (reservedNames.contains(name) || name.equals(values.get("renderer"))) values.put("model", modelClass);
		else {
			values.put("model", name);
			values.put("modelImport", modelClass);
		}
	}
}

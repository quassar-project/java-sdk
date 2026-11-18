package io.quassar.javacode.domain;

import io.quassar.javacode.model.JavaCode;
import io.quassar.javacode.model.StarterDefinition;
import io.quassar.javacode.ports.SourceTemplates;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class StarterGenerator {
	private static final String INTERPRETER = "Interpreter";
	private static final Set<String> INTERPRETER_NAMES = Set.of(INTERPRETER, "String", "System", "IOException", "Path");
	private static final Set<String> MOJO_NAMES = Set.of("AbstractMojo", "MojoExecutionException",
		"LifecyclePhase", "Mojo", "Parameter", "IOException", "File", "Files", "MavenProject", "URL", "Input");
	private final SourceTemplates templates;

	public StarterGenerator(SourceTemplates templates) {
		this.templates = templates;
	}

	public JavaCode interpreter(StarterDefinition definition) {
		var values = values(definition, INTERPRETER, INTERPRETER_NAMES);
		return code(definition, INTERPRETER, "interpreter", values);
	}

	public JavaCode mojo(StarterDefinition definition) {
		String name = definition.modelName().map(model -> JavaNames.className(model) + "Mojo").orElse("GenerateMojo");
		var values = values(definition, name, MOJO_NAMES);
		String base = "org.apache.maven.plugin.AbstractMojo";
		if (name.equals("AbstractMojo")) values.put("baseClass", base);
		else {
			values.put("baseClass", "AbstractMojo");
			values.put("baseImport", base);
		}
		return code(definition, name, "starter-mojo", values);
	}

	private JavaCode code(StarterDefinition definition, String name, String template, Map<String, String> values) {
		return new JavaCode(definition.packageName(), name, templates.render(template, values));
	}

	private Map<String, String> values(StarterDefinition definition, String name, Set<String> reserved) {
		Map<String, String> values = new HashMap<>();
		if (!definition.packageName().isEmpty()) values.put("package", definition.packageName());
		values.put("className", name);
		if (definition.modelClass() != null) addModel(values, definition, reserved);
		return values;
	}

	private void addModel(Map<String, String> values, StarterDefinition definition, Set<String> reserved) {
		String name = definition.modelClass().substring(definition.modelClass().lastIndexOf('.') + 1);
		if (definition.templates().size() == 1) {
			JavaCode template = definition.templates().get(0);
			values.put("renderer", template.className());
			values.put("rendererImport", template.packageName() + "." + template.className());
		}
		if (reserved.contains(name) || name.equals(values.get("renderer"))) values.put("model", definition.modelClass());
		else {
			values.put("model", name);
			values.put("modelImport", definition.modelClass());
		}
	}
}

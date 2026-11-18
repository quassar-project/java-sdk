package io.quassar.javacode;

import io.quassar.javacode.adapters.JavaCodeWriter;
import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.domain.DecoratorGenerator;
import io.quassar.javacode.domain.JavaNames;
import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.javacode.model.JavaCode;
import io.quassar.javacode.model.DecoratorOutput;
import io.quassar.metamodel.model.Metamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public record MogramDecoratorGenerator(Metamodel metamodel, String packageName, String modelName) {
	public static MogramDecoratorGenerator from(Metamodel metamodel) {
		String name = metamodel.name().map(MogramDecoratorGenerator::facadeName).orElse("DecoratedModel");
		return new MogramDecoratorGenerator(metamodel, "generated", name);
	}

	private static String facadeName(String name) {
		String localName = name.substring(name.lastIndexOf('.') + 1).strip();
		if (localName.equals(localName.toUpperCase(Locale.ROOT))) localName = localName.toLowerCase(Locale.ROOT);
		return JavaNames.className(localName) + "Model";
	}

	public MogramDecoratorGenerator inPackage(String name) {
		return new MogramDecoratorGenerator(metamodel, name, modelName);
	}

	public MogramDecoratorGenerator modelNamed(String name) {
		return new MogramDecoratorGenerator(metamodel, packageName, name);
	}

	public String modelClassName() {
		return DecoratorGenerator.modelClassName(modelName);
	}

	public List<JavaCode> code() {
		var templates = new ResourceTemplates(MogramDecoratorGenerator.class, List.of("model", "mogram", "enum", "method", "load", "decorator"));
		var schema = new MetamodelResolver().describe(metamodel);
		return new DecoratorGenerator(templates).generate(schema, new DecoratorOutput(packageName, modelName));
	}

	public List<Path> writeTo(Path directory) throws IOException {
		return new JavaCodeWriter().write(code(), directory);
	}
}

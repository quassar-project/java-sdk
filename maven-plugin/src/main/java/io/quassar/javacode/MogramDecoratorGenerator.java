package io.quassar.javacode;

import io.quassar.javacode.adapters.JavaCodeWriter;
import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.domain.DecoratorGenerator;
import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.Metamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public record MogramDecoratorGenerator(Metamodel metamodel, String packageName, String modelName) {
	public static MogramDecoratorGenerator from(Metamodel metamodel) {
		String name = metamodel.name().map(value -> value + "Model").orElse("DecoratedModel");
		return new MogramDecoratorGenerator(metamodel, "generated", name);
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
		var templates = new ResourceTemplates(MogramDecoratorGenerator.class, List.of("model", "mogram", "method", "load", "factory-case", "decorator", "types"));
		var resolver = new MetamodelResolver();
		return new DecoratorGenerator(templates).generate(resolver.resolve(metamodel),
			resolver.resolveFacets(metamodel), packageName, modelName);
	}

	public List<Path> writeTo(Path directory) throws IOException {
		return new JavaCodeWriter().write(code(), directory);
	}
}

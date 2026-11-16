package io.quassar.javacode;

import io.quassar.javacode.adapters.JavaCodeWriter;
import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.domain.DecoratorGenerator;
import io.quassar.javacode.domain.JavaNames;
import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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
		var templates = new ResourceTemplates(MogramDecoratorGenerator.class, List.of("model", "mogram", "enum", "method", "load", "factory-case", "decorator", "types"));
		var resolver = new MetamodelResolver();
		var enclosingTypes = metamodel.types().stream()
			.filter(type -> !type.scope().isEmpty())
			.collect(Collectors.toMap(MetamodelType::name, MetamodelType::scope));
		var subtypeParents = metamodel.types().stream()
			.filter(type -> type.declaration() == MetamodelType.Declaration.SUB)
			.collect(Collectors.toMap(MetamodelType::name, MetamodelType::parent));
		return new DecoratorGenerator(templates).generate(resolver.resolve(metamodel),
			resolver.resolveFacets(metamodel), enclosingTypes, subtypeParents, packageName, modelName, dsl(), metamodel.types());
	}

	private Optional<String> dsl() {
		return metamodel.manifest()
			.filter(manifest -> manifest.name() != null && !manifest.name().isBlank()
				&& manifest.version() != null && !manifest.version().isBlank())
			.map(manifest -> manifest.name().strip().toLowerCase(Locale.ROOT).replace('.', ':') + ":" + manifest.version().strip());
	}

	public List<Path> writeTo(Path directory) throws IOException {
		return new JavaCodeWriter().write(code(), directory);
	}
}

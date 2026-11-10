package tests.fixtures;

import io.quassar.javacode.MogramDecoratorGenerator;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.templating.TemplateEngine;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public record GeneratedDecorators(URLClassLoader loader, Model model) implements AutoCloseable {
	public static GeneratedDecorators compile(Model model, Metamodel metamodel, Path directory) throws Exception {
		List<Path> files = MogramDecoratorGenerator.from(metamodel).inPackage("test.generated").modelNamed("LibraryModel")
			.writeTo(directory.resolve("sources"));
		Path classes = Files.createDirectories(directory.resolve("classes"));
		String classpath = String.join(File.pathSeparator, location(Mogram.class), location(TemplateEngine.class));
		List<String> args = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-classpath",
			classpath, "-d", classes.toString()));
		for (Path file : files) args.add(file.toString());
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		int result = ToolProvider.getSystemJavaCompiler().run(null, null, errors, args.toArray(String[]::new));
		assertThat(result).withFailMessage(errors.toString()).isZero();
		return new GeneratedDecorators(new URLClassLoader(new URL[]{classes.toUri().toURL()}, Mogram.class.getClassLoader()), model);
	}

	public Object wrap(String type, Mogram mogram) throws Exception {
		var constructor = loader.loadClass("test.generated." + type).getDeclaredConstructor(Mogram.class, Model.class);
		constructor.setAccessible(true);
		return constructor.newInstance(mogram, model);
	}

	public Object modelDecorator() throws Exception {
		return loader.loadClass("test.generated.LibraryModel").getConstructor(Model.class).newInstance(model);
	}

	private static String location(Class<?> type) throws Exception {
		return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
	}

	@Override
	public void close() throws Exception {
		loader.close();
	}
}

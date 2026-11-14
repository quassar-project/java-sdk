package io.quassar.plugin;

import io.quassar.metamodel.Metamodels;
import io.quassar.javacode.MogramDecoratorGenerator;
import io.quassar.javacode.adapters.JavaCodeWriter;
import io.quassar.javacode.adapters.ResourceTemplates;
import io.quassar.javacode.adapters.TemplateFiles;
import io.quassar.javacode.domain.TemplateGenerator;
import io.quassar.javacode.model.JavaCode;
import io.quassar.javacode.model.TemplateDefinition;
import io.quassar.plugin.adapters.MavenDependencies;
import io.quassar.plugin.adapters.ProjectStarter;
import io.quassar.templating.ports.TemplateReadException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class TranslatorMojo extends AbstractMojo {
	@Parameter(alias = "package", defaultValue = "${project.groupId}")
	private String packageName;

	@Parameter
	private URL metamodel;

	@Parameter
	private String mode;

	@Parameter(defaultValue = "${project.basedir}/src/main/gen", required = true)
	private File outputDirectory;

	@Parameter
	private String templateDirectory;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		MavenDependencies dependencies = new MavenDependencies();
		if (!dependencies.isConfigured(project))
			throw new MojoExecutionException("Enable <extensions>true</extensions> on quassar.io:maven-plugin to add the required dependencies automatically");
		try {
			generate();
			dependencies.writePom(project);
		} catch (IOException | TemplateReadException exception) {
			throw new MojoExecutionException("Could not generate Quassar code: " + exception.getMessage(), exception);
		}
	}

	private void generate() throws IOException, TemplateReadException, MojoExecutionException {
		Mode generationMode = generationMode();
		if (generationMode == Mode.COMPILER && !"maven-plugin".equals(project.getPackaging()))
			throw new MojoExecutionException("Set <packaging>maven-plugin</packaging> when using compiler mode");
		var resources = project.getBasedir().toPath().resolve("src/main/resources");
		var templatePath = templateDirectory == null ? resources : resources.resolve(templateDirectory);
		var templateFiles = new TemplateFiles();
		List<TemplateDefinition> definitions = templateFiles.read(templatePath);
		if (definitions.isEmpty() && metamodel == null && generationMode != Mode.COMPILER)
			throw new MojoExecutionException("Configure <metamodel> or add .itr templates to " + templatePath);

		var sourceDirectory = project.getBasedir().toPath().resolve(project.getBuild().getSourceDirectory());
		var starter = new ProjectStarter(sourceDirectory, packageName);
		boolean createStarter = generationMode != null && !starter.hasJavaSources();
		List<JavaCode> code = new ArrayList<>();
		String modelClass = null;
		Optional<String> modelName = Optional.empty();
		if (metamodel != null) {
			String modelPackage = packageName.isEmpty() ? "model" : packageName + ".model";
			var schema = Metamodels.load(metamodel);
			modelName = schema.name();
			var generator = MogramDecoratorGenerator.from(schema).inPackage(modelPackage);
			code.addAll(generator.code());
			modelClass = modelPackage + "." + generator.modelClassName();
			if (createStarter && generationMode == Mode.INTERPRETER && definitions.isEmpty()) {
				starter.writeJsonTemplate(templatePath, schema);
				definitions = templateFiles.read(templatePath);
			}
		}
		List<JavaCode> templates = definitions.isEmpty() ? List.of()
			: new TemplateGenerator(new ResourceTemplates(TranslatorMojo.class, List.of("template-class")))
				.generate(packageName, definitions);
		code.addAll(templates);
		if (createStarter) {
			if (generationMode == Mode.COMPILER) starter.writeMojo(modelName, modelClass, templates);
			else starter.writeInterpreter(modelClass, templates);
		}
		new JavaCodeWriter().write(code, outputDirectory.toPath());
		project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
		getLog().info("Generated " + code.size() + " Java classes in " + outputDirectory);
	}

	private Mode generationMode() throws MojoExecutionException {
		if (mode == null) return null;
		return switch (mode) {
			case "interpreter" -> Mode.INTERPRETER;
			case "compiler" -> Mode.COMPILER;
			default -> throw new MojoExecutionException("Unknown mode '" + mode + "'. Use compiler or interpreter.");
		};
	}

	private enum Mode {
		COMPILER, INTERPRETER
	}
}

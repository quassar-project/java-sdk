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
	@Parameter(alias = "package", defaultValue = "generated")
	private String packageName;

	@Parameter
	private URL metamodel;

	@Parameter
	private String scaffold;

	@Deprecated
	@Parameter(defaultValue = "false")
	private boolean mavenPlugin;

	@Parameter(defaultValue = "${project.basedir}/src/main/gen", required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project.basedir}/src/main/resources", required = true)
	private File templatesDirectory;

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
		Scaffold mode = scaffoldMode();
		if (mode == Scaffold.MAVEN_PLUGIN && !"maven-plugin".equals(project.getPackaging()))
			throw new MojoExecutionException("Set <packaging>maven-plugin</packaging> to scaffold a Maven plugin");
		var templateDirectory = templatesDirectory.toPath();
		var templateFiles = new TemplateFiles();
		List<TemplateDefinition> definitions = templateFiles.read(templateDirectory);
		if (definitions.isEmpty() && metamodel == null && mode != Scaffold.MAVEN_PLUGIN)
			throw new MojoExecutionException("Configure <metamodel> or add .itr templates to " + templatesDirectory);

		var sourceDirectory = project.getBasedir().toPath().resolve(project.getBuild().getSourceDirectory());
		var starter = new ProjectStarter(sourceDirectory, packageName);
		boolean createStarter = mode != Scaffold.NONE && !starter.hasJavaSources();
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
			if (createStarter && definitions.isEmpty()) {
				starter.writeJsonTemplate(templateDirectory, schema);
				definitions = templateFiles.read(templateDirectory);
			}
		}
		List<JavaCode> templates = definitions.isEmpty() ? List.of()
			: new TemplateGenerator(new ResourceTemplates(TranslatorMojo.class, List.of("template-class")))
				.generate(packageName, definitions);
		code.addAll(templates);
		if (createStarter) {
			if (mode == Scaffold.MAVEN_PLUGIN) starter.writeMojo(modelName, modelClass, templates);
			else starter.writeMain(modelClass, templates);
		}
		new JavaCodeWriter().write(code, outputDirectory.toPath());
		project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
		getLog().info("Generated " + code.size() + " Java classes in " + outputDirectory);
	}

	private Scaffold scaffoldMode() throws MojoExecutionException {
		if (scaffold == null) return mavenPlugin ? Scaffold.MAVEN_PLUGIN : Scaffold.APPLICATION;
		return switch (scaffold) {
			case "application" -> Scaffold.APPLICATION;
			case "maven-plugin" -> Scaffold.MAVEN_PLUGIN;
			case "none" -> Scaffold.NONE;
			default -> throw new MojoExecutionException("Unknown scaffold '" + scaffold + "'. Use application, maven-plugin or none.");
		};
	}

	private enum Scaffold {
		APPLICATION, MAVEN_PLUGIN, NONE
	}
}

package tests;

import io.quassar.plugin.TranslatorMojo;
import io.quassar.plugin.adapters.MavenDependencies;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tests.fixtures.GeneratedDecorators;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TranslatorMojoTest {
	private static final String METAMODEL_RESOURCES = "/metamodels/";
	private static final String MODEL_PACKAGE = "library.model.model.";
	private static final String JAVA_EXPECTATION = "// Java: ";

	@TempDir
	Path directory;

	@ParameterizedTest(name = "{0}")
	@MethodSource("metamodelCases")
	void generatesCompilableClassesForMetamodel(String name) throws Exception {
		Path metamodel = metamodelDirectory().resolve(name);
		MavenProject project = project();
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "interpreter");
		setModelInput(mojo, metamodel.toUri().toURL());
		Path source = Files.createDirectories(directory.resolve("src/main/java"));
		Files.writeString(source.resolve("Existing.java"), "class Existing {}");
		assertDoesNotThrow(mojo::execute, () -> "Generate Java for " + name);

		Path generated = directory.resolve("src/main/gen");
		assertThat(project.getCompileSourceRoots()).contains(generated.toString());
		List<Path> sources;
		try (var files = Files.walk(generated)) {
			sources = files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).sorted().toList();
		}
		Path classes = directory.resolve("classes");
		assertDoesNotThrow(() -> GeneratedDecorators.compileSources(sources, classes),
			() -> "Compile Java for " + name);
		assertThat(classes.resolve("library/model/model/DecoratedModel.class")).exists();
		assertDoesNotThrow(() -> verifyJavaApi(metamodel, classes), () -> "Check Java API for " + name);
	}

	private static void verifyJavaApi(Path metamodel, Path classes) throws Exception {
		try (var files = Files.list(metamodel);
			 var loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, TranslatorMojoTest.class.getClassLoader())) {
			for (Path file : files.filter(TranslatorMojoTest::isTaraSource).sorted().toList()) {
				for (String line : Files.readAllLines(file)) {
					if (!line.startsWith(JAVA_EXPECTATION)) continue;
					// Expectations name a zero-argument getter and its generic Java return type.
					String[] expectation = line.substring(JAVA_EXPECTATION.length()).split(" ", 2);
					String[] member = expectation[0].split("#", 2);
					Class<?> type = loader.loadClass(MODEL_PACKAGE + member[0]);
					String result = type.getMethod(member[1]).getGenericReturnType().getTypeName().replace(MODEL_PACKAGE, "");
					assertThat(result).as(expectation[0]).isEqualTo(expectation[1]);
				}
			}
		}
	}

	private static Stream<String> metamodelCases() throws Exception {
		try (var cases = Files.list(metamodelDirectory())) {
			return cases.filter(Files::isDirectory).sorted().map(path -> path.getFileName().toString()).toList().stream();
		}
	}

	private static boolean isTaraSource(Path path) {
		return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".tara");
	}

	private static Path metamodelDirectory() throws Exception {
		var resource = TranslatorMojoTest.class.getResource(METAMODEL_RESOURCES);
		assertThat(resource).as("Metamodel fixtures at %s", METAMODEL_RESOURCES).isNotNull();
		return Path.of(resource.toURI());
	}

	@Test
	void compilesTemplatesAndRegistersTheirSources() throws Exception {
		copyTemplate();

		MavenProject project = project();
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "interpreter");
		mojo.execute();
		assertThat(project.getCompileSourceRoots()).contains(directory.resolve("src/main/gen").toString());

		assertThat(directory.resolve("src/main/gen/library/model/templates/BookSummaryTemplate.java")).exists();
		assertThat(directory.resolve("target/generated-resources/metta/library/model/templates/BookSummaryTemplate.itr")).doesNotExist();
		assertThat(directory.resolve("src/main/gen/library/model/model")).doesNotExist();
		assertThat(directory.resolve("src/main/java/library/model/Interpreter.java")).exists();
	}

	@Test
	void limitsTemplatesToTheConfiguredResourcesSubdirectory() throws Exception {
		copyTemplate();
		Path resources = directory.resolve("src/main/resources");
		Path templates = Files.createDirectory(resources.resolve("templates"));
		Files.move(resources.resolve("book-summary.itr"), templates.resolve("book-summary.itr"));
		Files.writeString(resources.resolve("outside.itr"), "rule type(Book)\n\t$identifier\n");
		TranslatorMojo mojo = configuredMojo(project());
		var input = new TranslatorMojo.Input();
		input.setTemplates("templates");
		parameter(mojo, "mode", "interpreter");
		parameter(mojo, "input", input);

		mojo.execute();

		Path generated = directory.resolve("src/main/gen/library/model/templates");
		assertThat(generated.resolve("BookSummaryTemplate.java")).exists();
		assertThat(generated.resolve("OutsideTemplate.java")).doesNotExist();
	}

	@Test
	void generatesInterpreterOnceForAnEmptyCustomSourceDirectory() throws Exception {
		Path source = directory.resolve("app");
		MavenProject project = project();
		project.getBuild().setSourceDirectory(source.toString());
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "interpreter");
		setModelInput(mojo);
		copyTemplate();

		mojo.execute();
		Path main = source.resolve("library/model/Interpreter.java");
		assertThat(main).content().contains("package library.model;", "import library.model.model.LibraryModel;",
			"LibraryModel.load(", "BookSummaryTemplate.render(model)");
		assertThat(directory.resolve("src/main/resources/model-json.itr")).doesNotExist();

		Files.writeString(main, "custom main");
		mojo.execute();
		assertThat(main).content().isEqualTo("custom main");
	}

	@Test
	void createsAndCompilesAnEditableJsonTemplateForANewModelProject() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "interpreter");
		setModelInput(mojo);

		mojo.execute();
		Path template = directory.resolve("src/main/resources/model-json.itr");
		assertThat(template).content().contains("rule type(Model)", "\"book\": [$book...[, ]]", "rule type(Book)",
			"\"title\": $title+JsonValue");
		assertThat(directory.resolve("src/main/gen/library/model/templates/ModelJsonTemplate.java")).exists();
		assertThat(directory.resolve("src/main/java/library/model/Interpreter.java"))
			.content().contains("ModelJsonTemplate.render(model)");

		String edited = "rule type(Model)\n\t{\"edited\": true}\n";
		Files.writeString(template, edited);
		mojo.execute();
		assertThat(template).content().isEqualTo(edited);
	}

	@Test
	void createsAMojoInsteadOfInterpreterForANewMavenPluginProject() throws Exception {
		MavenProject project = project();
		project.getModel().setPackaging("maven-plugin");
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "generator");
		setModelInput(mojo);

		mojo.execute();
		Path source = directory.resolve("src/main/java/library/model");
		assertThat(source.resolve("LibraryMojo.java")).content().contains("class LibraryMojo extends AbstractMojo",
			"@Mojo(name = \"generate\"", "LibraryModel.load(input.model)", "private File output;");
		assertThat(source.resolve("Interpreter.java")).doesNotExist();
	}

	@Test
	void usesGenerateMojoWhenThereIsNoMetamodelName() throws Exception {
		MavenProject project = project();
		project.getModel().setPackaging("maven-plugin");
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "generator");

		mojo.execute();

		assertThat(directory.resolve("src/main/java/library/model/GenerateMojo.java"))
			.content().contains("class GenerateMojo extends AbstractMojo");
	}

	@Test
	void rejectsMavenPluginModeForJarProjects() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "generator");

		assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
			.hasMessageContaining("<packaging>maven-plugin</packaging>");
	}

	@Test
	void requiresAnExplicitMode() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());

		assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
			.hasMessageContaining("Configure <mode> as generator or interpreter");
	}

	@Test
	void reportsSupportedModesForUnknownValues() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "none");

		assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
			.hasMessageContaining("Unknown mode 'none'").hasMessageContaining("generator or interpreter");
	}

	@Test
	void skipsInterpreterWhenAnyJavaSourceExists() throws Exception {
		Path source = directory.resolve("src/main/java/another");
		Files.createDirectories(source);
		Files.writeString(source.resolve("Existing.java"), "package another; class Existing {}");
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "interpreter");
		setModelInput(mojo);
		mojo.execute();
		assertThat(directory.resolve("src/main/java/library/model/Interpreter.java")).doesNotExist();
		assertThat(directory.resolve("src/main/resources/model-json.itr")).doesNotExist();
	}

	private void copyTemplate() throws Exception {
		Path templates = Files.createDirectories(directory.resolve("src/main/resources"));
		try (var input = getClass().getResourceAsStream("/maven/consumer/src/main/resources/book-summary.itr")) {
			Files.copy(input, templates.resolve("book-summary.itr"));
		}
	}

	private MavenProject project() throws Exception {
		Path pom = directory.resolve("pom.xml");
		try (var input = getClass().getResourceAsStream("/maven/project.xml")) {
			Files.copy(input, pom);
		}
		MavenProject project;
		try (var input = Files.newBufferedReader(pom)) {
			project = new MavenProject(new MavenXpp3Reader().read(input));
		}
		project.setOriginalModel(project.getModel().clone());
		project.setFile(pom.toFile());
		project.getBuild().setDirectory(directory.resolve("target").toString());
		project.getBuild().setSourceDirectory(directory.resolve("src/main/java").toString());
		new MavenDependencies().addTo(project, "1.0.0");
		return project;
	}

	private TranslatorMojo configuredMojo(MavenProject project) throws Exception {
		TranslatorMojo mojo = new TranslatorMojo();
		parameter(mojo, "project", project);
		var output = new TranslatorMojo.Output();
		output.setPackage("library.model");
		parameter(mojo, "output", output);
		return mojo;
	}

	private void setModelInput(TranslatorMojo mojo) throws Exception {
		setModelInput(mojo, getClass().getResource("/maven/consumer/metamodel/"));
	}

	private static void setModelInput(TranslatorMojo mojo, URL model) throws Exception {
		var input = new TranslatorMojo.Input();
		input.setModel(model);
		parameter(mojo, "input", input);
	}

	private static void parameter(TranslatorMojo mojo, String name, Object value) throws Exception {
		Field field = TranslatorMojo.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(mojo, value);
	}
}

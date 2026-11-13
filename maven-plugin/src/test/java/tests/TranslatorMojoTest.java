package tests;

import io.quassar.plugin.TranslatorMojo;
import io.quassar.plugin.adapters.MavenDependencies;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslatorMojoTest {
	@TempDir
	Path directory;

	@Test
	void compilesTemplatesAndRegistersTheirSources() throws Exception {
		copyTemplate();

		MavenProject project = project();
		configuredMojo(project).execute();
		assertThat(project.getCompileSourceRoots()).contains(directory.resolve("src/main/gen").toString());

		assertThat(directory.resolve("src/main/gen/library/model/templates/BookSummaryTemplate.java")).exists();
		assertThat(directory.resolve("target/generated-resources/metta/library/model/templates/BookSummaryTemplate.itr")).doesNotExist();
		assertThat(directory.resolve("src/main/gen/library/model/model")).doesNotExist();
		assertThat(directory.resolve("src/main/java/library/model/Interpreter.java"))
			.content().contains("public final class Interpreter", "public static void main(String[] args)");
	}

	@Test
	void limitsTemplatesToTheConfiguredResourcesSubdirectory() throws Exception {
		copyTemplate();
		Path resources = directory.resolve("src/main/resources");
		Path templates = Files.createDirectory(resources.resolve("templates"));
		Files.move(resources.resolve("book-summary.itr"), templates.resolve("book-summary.itr"));
		Files.writeString(resources.resolve("outside.itr"), "rule type(Book)\n\t$identifier\n");
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "templateDirectory", "templates");

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
		parameter(mojo, "metamodel", getClass().getResource("/maven/consumer/metamodel/"));
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
		parameter(mojo, "metamodel", getClass().getResource("/maven/consumer/metamodel/"));

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
		parameter(mojo, "mode", "compiler");
		parameter(mojo, "metamodel", getClass().getResource("/maven/consumer/metamodel/"));

		mojo.execute();
		Path source = directory.resolve("src/main/java/library/model");
		assertThat(source.resolve("LibraryMojo.java")).content().contains("class LibraryMojo extends AbstractMojo",
			"@Mojo(name = \"generate\"", "LibraryModel.load(model)", "ModelJsonTemplate.render(loadedModel)");
		assertThat(source.resolve("Interpreter.java")).doesNotExist();
	}

	@Test
	void usesGenerateMojoWhenThereIsNoMetamodelName() throws Exception {
		MavenProject project = project();
		project.getModel().setPackaging("maven-plugin");
		TranslatorMojo mojo = configuredMojo(project);
		parameter(mojo, "mode", "compiler");

		mojo.execute();

		assertThat(directory.resolve("src/main/java/library/model/GenerateMojo.java"))
			.content().contains("class GenerateMojo extends AbstractMojo");
	}

	@Test
	void rejectsMavenPluginModeForJarProjects() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "compiler");

		assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
			.hasMessageContaining("<packaging>maven-plugin</packaging>");
	}

	@Test
	void generatesDecoratorsWithoutScaffoldingWhenDisabled() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "none");
		parameter(mojo, "metamodel", getClass().getResource("/maven/consumer/metamodel/"));

		mojo.execute();
		assertThat(directory.resolve("src/main/gen/library/model/model/LibraryModel.java")).exists();
		assertThat(directory.resolve("src/main/java")).doesNotExist();
		assertThat(directory.resolve("src/main/resources/model-json.itr")).doesNotExist();
	}

	@Test
	void reportsSupportedModesForUnknownValues() throws Exception {
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "mode", "unknown");

		assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
			.hasMessageContaining("unknown").hasMessageContaining("compiler, interpreter or none");
	}

	@Test
	void skipsInterpreterWhenAnyJavaSourceExists() throws Exception {
		Path source = directory.resolve("src/main/java/another");
		Files.createDirectories(source);
		Files.writeString(source.resolve("Existing.java"), "package another; class Existing {}");
		TranslatorMojo mojo = configuredMojo(project());
		parameter(mojo, "metamodel", getClass().getResource("/maven/consumer/metamodel/"));
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
		parameter(mojo, "outputDirectory", directory.resolve("src/main/gen").toFile());
		parameter(mojo, "packageName", "library.model");
		return mojo;
	}

	private static void parameter(TranslatorMojo mojo, String name, Object value) throws Exception {
		Field field = TranslatorMojo.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(mojo, value);
	}
}

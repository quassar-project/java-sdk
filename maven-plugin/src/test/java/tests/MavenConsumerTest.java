package tests;

import com.google.gson.JsonParser;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "quassar.test.maven", matches = ".+")
class MavenConsumerTest {
	@TempDir
	Path directory;

	@Test
	void compilesConsumersWithTheGeneratedDependencies() throws Exception {
		Path repository = isolatedRepository();

		Path consumer = project("consumer", "Library.java");
		Path downstream = project("downstream", "Consumer.java");
		byte[] sourcePom = Files.readAllBytes(consumer.resolve("pom.xml"));
		build(repository, consumer, "install");
		assertThat(consumer.resolve("pom.xml")).hasBinaryContent(sourcePom);

		Path installedPom = repository.resolve("io/quassar/integration/translated-library/1.0.0/translated-library-1.0.0.pom");
		Map<String, String> expected = Map.of("template-engine", "1.0.0", "model-reader", "1.0.0");
		try (InputStream input = Files.newInputStream(installedPom)) {
			assertThat(dependencies(input)).isEqualTo(expected);
		}
		Path artifact = consumer.resolve("target/translated-library-1.0.0.jar");
		try (ZipFile jar = new ZipFile(artifact.toFile())) {
			assertThat(jar.getEntry("example/model/Book.class")).isNotNull();
			assertThat(jar.getEntry("example/model/LibraryModel.class")).isNotNull();
			assertThat(jar.getEntry("example/model/DecoratedModel.class")).isNull();
			assertThat(jar.getEntry("example/Book.class")).isNull();
			assertThat(jar.getEntry("book-summary.itr")).isNotNull();
			assertThat(jar.stream().map(ZipEntry::getName).filter(name -> name.endsWith(".itr")))
				.containsExactly("book-summary.itr");
			assertThat(jar.stream().map(ZipEntry::getName)
				.filter(name -> name.startsWith("example/templates/") && name.endsWith(".class")))
				.containsExactly("example/templates/BookSummaryTemplate.class");
			var packagedPom = jar.getEntry("META-INF/maven/io.quassar.integration/translated-library/pom.xml");
			assertThat(packagedPom).isNotNull();
			try (InputStream input = jar.getInputStream(packagedPom)) {
				assertThat(dependencies(input)).isEqualTo(expected);
			}
		}
		renderFromJar(withoutTemplates(artifact));
		build(repository, downstream, "compile");
	}

	@Test
	void scaffoldsAnInstallableMavenGeneratorAndRunsItsGoal() throws Exception {
		Path repository = isolatedRepository();
		Path generator = directory.resolve("generator");
		Files.createDirectories(generator.resolve("metamodel"));
		Files.write(generator.resolve("pom.xml"), resource("/maven/generator/pom.xml"));
		Files.write(generator.resolve("metamodel/library.tara"), resource("/maven/generator/metamodel/library.tara"));
		Files.write(generator.resolve("metamodel/manifest.json"), resource("/maven/consumer/metamodel/manifest.json"));
		Path modelFile = Files.write(directory.resolve("library.tara"), resource("/maven/generator/m1/library.tara"));

		build(repository, generator, "install");
		assertThat(generator.resolve("src/main/java/io/quassar/integration/LibraryMojo.java")).exists();
		assertThat(generator.resolve("src/main/java/io/quassar/integration/Interpreter.java")).doesNotExist();
		Path consumer = Files.createDirectory(directory.resolve("compiler-consumer"));
		Files.writeString(consumer.resolve("pom.xml"), """
			<project>
				<modelVersion>4.0.0</modelVersion>
				<groupId>io.quassar.integration</groupId>
				<artifactId>compiler-consumer</artifactId>
				<version>1.0.0</version>
				<build>
					<plugins>
						<plugin>
							<groupId>io.quassar.integration</groupId>
							<artifactId>library-generator-maven-plugin</artifactId>
							<version>1.0.0</version>
							<executions>
								<execution>
									<goals><goal>generate</goal></goals>
								</execution>
							</executions>
							<configuration>
								<model>%s</model>
							</configuration>
						</plugin>
					</plugins>
				</build>
			</project>
			""".formatted(modelFile.toUri()));
		build(repository, consumer, "generate-sources");
		var model = JsonParser.parseString(Files.readString(consumer.resolve("target/generated-resources/model.json"))).getAsJsonObject();
		var books = model.getAsJsonArray("book");
		assertThat(books.size()).isEqualTo(2);
		var atlas = books.get(0).getAsJsonObject();
		assertThat(atlas.get("id").getAsString()).isEqualTo("Atlas");
		assertThat(atlas.get("title").getAsString()).isEqualTo("Atlas \"illustrated\"");
		assertThat(atlas.get("year").getAsInt()).isEqualTo(2024);
		assertThat(atlas.get("available").getAsBoolean()).isFalse();
		assertThat(atlas.get("isbn").getAsString()).isEqualTo("123");
		assertThat(atlas.get("related").getAsString()).isEqualTo("Nocturne");
		assertThat(atlas.getAsJsonArray("companions").get(0).getAsString()).isEqualTo("Nocturne");
		assertThat(atlas.getAsJsonObject("chapter").get("heading").getAsString()).isEqualTo("The beginning");
		var nocturne = books.get(1).getAsJsonObject();
		assertThat(nocturne.get("related").getAsString()).isEqualTo("Atlas");
		assertThat(nocturne.has("isbn")).isFalse();
		assertThat(nocturne.has("chapter")).isFalse();
	}

	private Path isolatedRepository() throws Exception {
		Path localRepository = Path.of(System.getProperty("maven.repo.local",
			Path.of(System.getProperty("user.home"), ".m2", "repository").toString()));
		Path repository = directory.resolve("repository");
		linkRepository(localRepository.toAbsolutePath(), repository, Path.of("io", "quassar", "integration"));
		return repository;
	}

	private Path withoutTemplates(Path artifact) throws IOException {
		Path compiled = directory.resolve("compiled-templates.jar");
		try (ZipFile jar = new ZipFile(artifact.toFile());
			 ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(compiled))) {
			for (var entry : jar.stream().filter(file -> !file.getName().endsWith(".itr")).toList()) {
				output.putNextEntry(new ZipEntry(entry.getName()));
				try (var input = jar.getInputStream(entry)) {
					input.transferTo(output);
				}
				output.closeEntry();
			}
		}
		return compiled;
	}

	private void renderFromJar(Path artifact) throws Exception {
		try (URLClassLoader loader = new URLClassLoader(new URL[]{artifact.toUri().toURL()}, getClass().getClassLoader())) {
			var summary = loader.loadClass("example.Library").getMethod("summary", String.class);
			assertThat(summary.invoke(null, "A generated book")).isEqualTo("Book: A GENERATED BOOK (édition \"papier\")");
		}
	}

	private Path project(String name, String source) throws Exception {
		Path project = directory.resolve(name);
		String javaFile = "src/main/java/example/" + source;
		Path output = project.resolve(javaFile);
		Files.createDirectories(output.getParent());
		Files.write(output, resource("/maven/" + name + "/" + javaFile));
		if (name.equals("consumer")) {
			String template = "src/main/resources/book-summary.itr";
			Path file = project.resolve(template);
			Files.createDirectories(file.getParent());
			Files.write(file, resource("/maven/consumer/" + template));
			Path metamodel = project.resolve("metamodel/library.tara");
			Files.createDirectories(metamodel.getParent());
			Files.write(metamodel, resource("/maven/consumer/metamodel/library.tara"));
			Files.write(metamodel.resolveSibling("manifest.json"), resource("/maven/consumer/metamodel/manifest.json"));
		}
		String pom = new String(resource("/maven/" + name + "/pom.xml"), UTF_8);
		Files.writeString(project.resolve("pom.xml"), pom.replace("@VERSION@", "1.0.0"));
		return project;
	}

	private byte[] resource(String name) throws Exception {
		try (InputStream input = getClass().getResourceAsStream(name)) {
			assertThat(input).as("Test resource %s", name).isNotNull();
			return input.readAllBytes();
		}
	}

	private void linkRepository(Path source, Path target, Path excluded) throws Exception {
		Files.createDirectories(target);
		try (var entries = Files.list(source)) {
			for (Path entry : entries.toList()) {
				Path name = entry.getFileName();
				if (!name.equals(excluded.getName(0))) Files.createSymbolicLink(target.resolve(name), entry);
				else if (excluded.getNameCount() > 1)
					linkRepository(entry, target.resolve(name), excluded.subpath(1, excluded.getNameCount()));
			}
		}
	}

	private void build(Path repository, Path project, String goal) throws Exception {
		List<String> command = new ArrayList<>(List.of(System.getProperty("quassar.test.maven"), "-B", "-ntp",
			"-Dmaven.repo.local=" + repository, "-f", project.resolve("pom.xml").toString(), goal));
		if (Boolean.getBoolean("quassar.test.maven.offline")) command.add("-o");
		Path log = project.resolve("maven.log");
		Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			assertThat(process.waitFor(2, TimeUnit.MINUTES)).as("Maven %s in %s finished", goal, project).isTrue();
			assertThat(process.exitValue()).withFailMessage("%s", Files.readString(log)).isZero();
		} finally {
			if (process.isAlive()) process.destroyForcibly();
		}
	}

	private Map<String, String> dependencies(InputStream pom) throws Exception {
		return new MavenXpp3Reader().read(pom).getDependencies().stream()
			.collect(Collectors.toMap(Dependency::getArtifactId, Dependency::getVersion));
	}
}

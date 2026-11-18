package tests;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HexagonalArchitectureTest {
	private static final Pattern IMPORT = Pattern.compile("(?m)^import (?:static )?([^;]+);");
	private static final List<String> INFRASTRUCTURE = List.of(".adapters.", "com.google.gson.", "org.antlr.",
		"org.apache.maven.", "java.net.", "java.nio.file.");
	private static final List<String> FACADES = List.of("io.quassar.reader.Models", "io.quassar.reader.ModelRelease",
		"io.quassar.templating.TemplateEngine", "io.quassar.javacode.MogramDecoratorGenerator");

	@Test
	void keepsInfrastructureOutsideTheCoreOfEveryModule() throws Exception {
		List<String> violations = new ArrayList<>();
		for (Path source : coreSources()) {
			var imports = IMPORT.matcher(Files.readString(source));
			while (imports.find())
				if (INFRASTRUCTURE.stream().anyMatch(imports.group(1)::contains))
					violations.add(source + " imports " + imports.group(1));
		}
		assertThat(violations).as("Domain, model and ports must not depend on adapters or infrastructure APIs").isEmpty();
	}

	@Test
	void keepsDomainServicesIndependentOfApplicationFacades() throws Exception {
		List<String> violations = new ArrayList<>();
		for (Path source : coreSources()) {
			if (!source.toString().replace('\\', '/').contains("/domain/")) continue;
			var imports = IMPORT.matcher(Files.readString(source));
			while (imports.find())
				if (FACADES.stream().anyMatch(imports.group(1)::startsWith))
					violations.add(source + " imports " + imports.group(1));
		}
		assertThat(violations).as("Application facades orchestrate domain services, not the reverse").isEmpty();
	}

	private List<Path> coreSources() throws Exception {
		Path root = repository();
		List<Path> sources = new ArrayList<>();
		for (String module : List.of("model-reader", "template-engine", "maven-plugin")) {
			Path directory = root.resolve(module).resolve("src/main/java");
			try (var files = Files.walk(directory)) {
				List<Path> core = files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> path.toString().matches(".*[\\\\/](domain|model|ports)[\\\\/].*")).toList();
				assertThat(core).as("Core sources in %s", module).isNotEmpty();
				sources.addAll(core);
			}
		}
		return List.copyOf(sources);
	}

	private Path repository() throws Exception {
		Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		while (path != null && !Files.isRegularFile(path.resolve("model-reader/pom.xml"))) path = path.getParent();
		assertThat(path).as("SDK repository root").isNotNull();
		return path;
	}
}

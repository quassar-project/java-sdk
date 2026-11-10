package tests;

import io.quassar.plugin.adapters.MavenDependencies;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MavenDependenciesTest {
	@TempDir
	Path directory;

	@Test
	void addsCompileDependenciesOnceAndPublishesThemWithoutChangingTheSourcePom() throws Exception {
		MavenProject project = project("project.xml");
		Path source = project.getFile().toPath();
		String original = Files.readString(source);
		MavenDependencies dependencies = new MavenDependencies();
		assertThat(dependencies.isConfigured(project)).isFalse();
		dependencies.addTo(project, "1.0.0");
		dependencies.addTo(project, "1.0.0");
		assertThat(dependencies.isConfigured(project)).isTrue();
		assertThat(versions(project.getModel())).containsExactlyInAnyOrderEntriesOf(
			Map.of("template-engine", "1.0.0", "model-reader", "1.0.0"));
		assertThat(project.getDependencies()).extracting(Dependency::getScope).containsOnly("compile");
		dependencies.writePom(project);
		dependencies.writePom(project);
		assertThat(project.getBasedir()).isEqualTo(directory.toFile());
		assertThat(Files.readString(source)).isEqualTo(original);
		try (Reader input = Files.newBufferedReader(project.getFile().toPath())) {
			assertThat(versions(new MavenXpp3Reader().read(input))).isEqualTo(versions(project.getModel()));
		}
	}

	@Test
	void respectsExplicitAndManagedVersions() throws Exception {
		MavenProject project = project("managed.xml");
		new MavenDependencies().addTo(project, "1.0.0");
		assertThat(versions(project.getModel())).containsExactlyInAnyOrderEntriesOf(
			Map.of("model-reader", "2.0.0", "template-engine", "3.0.0"));
	}

	private MavenProject project(String resource) throws Exception {
		Path pom = directory.resolve("pom.xml");
		try (var input = getClass().getResourceAsStream("/maven/" + resource)) {
			Files.copy(input, pom);
		}
		Model model;
		try (Reader input = Files.newBufferedReader(pom)) {
			model = new MavenXpp3Reader().read(input);
		}
		MavenProject project = new MavenProject(model);
		project.setOriginalModel(model.clone());
		project.setFile(pom.toFile());
		project.getBuild().setDirectory(directory.resolve("target").toString());
		return project;
	}

	private Map<String, String> versions(Model model) {
		return model.getDependencies().stream().collect(Collectors.toMap(Dependency::getArtifactId, Dependency::getVersion));
	}
}

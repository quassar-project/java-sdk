package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ModelsTest {
	@TempDir
	Path directory;

	@Test
	void loadsASecondaryDslFromAZipUrl() throws Exception {
		Path zip = directory.resolve("multi-dsl.zip");
		try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
			for (String name : List.of("manifest.json", "model.tara")) {
				output.putNextEntry(new ZipEntry(name));
				Files.copy(TestResources.path("multi-dsl/" + name), output);
				output.closeEntry();
			}
		}
		Model model = Models.loadForDsl(zip.toUri(), "monet:visora:1.8.2");
		assertThat(model.get("OrderFulfillment").children()).hasSize(1);
	}

	@Test
	void loadsAnyDslDeclaredInTheManifest() throws Exception {
		for (String dsl : List.of("monet:woven:1.0.0", "monet:visora:1.8.2", "monet:orfeo:2.8.1")) {
			Model model = Models.loadForDsl(TestResources.path("multi-dsl"), dsl);
			assertThat(model.get("OrderFulfillment").type()).isEqualTo("Workflow");
		}
	}

	@Test
	void requiresTheExactDslCoordinateAndVersion() {
		for (String dsl : List.of("monet:visora:1.8.1", "visora", "other:visora:1.8.2"))
			assertThatThrownBy(() -> Models.loadForDsl(TestResources.path("multi-dsl"), dsl))
				.isInstanceOf(IOException.class).hasMessageContaining("Expected DSL " + dsl);
	}

	@Test
	void loadsAFolderRecursively() throws Exception {
		Model model = Models.load(TestResources.path("folder"));
		assertThat(model.roots().count()).isEqualTo(5);
		Mogram extra = model.get("CollectedStories");
		assertThat(model.get(extra.property("relatedBook"), extra)).isSameAs(model.get("DonQuixote"));
	}

	@Test
	void loadsALocalZip() throws Exception {
		Model zip = Models.load(TestResources.path("folder.zip"));
		Model folder = Models.load(TestResources.path("folder"));
		assertThat(zip.mograms()).isEqualTo(folder.mograms());
		assertThat(zip.allMograms()).hasSize(folder.allMograms().size());
	}

	@Test
	void loadsAUrl() throws Exception {
		Model model = Models.load(TestResources.path("folder.zip").toUri().toURL());
		assertThat(model.get("CollectedStories").identifier()).isEqualTo("CollectedStories");
		assertThat(model.select().type("Book").count()).isEqualTo(2);
	}

	@Test
	void loadsAUri() throws Exception {
		Model model = Models.load(TestResources.path("folder.zip").toUri());
		assertThat(model.mograms()).isEqualTo(Models.load(TestResources.path("folder.zip")).mograms());
	}

	@Test
	void loadsATaraResource() throws Exception {
		Model model = Models.load(getClass().getResource("/tara/library.tara"));
		assertThat(model.mograms()).isEqualTo(TestResources.model("library.tara").mograms());
		assertThat(model.get("DonQuixote").type()).isEqualTo("Book");
	}
}

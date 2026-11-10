package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;

import static org.assertj.core.api.Assertions.assertThat;

public final class ModelsTest {
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

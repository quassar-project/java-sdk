package tests;

import io.quassar.metamodel.Metamodels;
import io.quassar.metamodel.domain.MetamodelReader;
import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.reader.Models;
import io.quassar.reader.model.Model;
import io.quassar.templating.ports.FrameView;
import io.quassar.reader.adapters.TextSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tests.fixtures.GeneratedDecorators;
import tests.fixtures.VisoraArchive;
import tests.fixtures.VisoraComparison;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VisoraModelTest {
	@TempDir
	Path directory;

	@Test
	void readsEverySourceAndResourceFromTheCompleteZip() throws Exception {
		var archive = new VisoraArchive(resource("model.zip"));
		var model = archive.model();
		assertThat(archive.sources()).hasSize(35);
		var concatenated = Models.read(new TextSource(List.copyOf(archive.sources().values())));
		assertThat(model.mograms()).containsExactlyElementsOf(concatenated.mograms());
		assertThat(model.allMograms()).hasSize(638);
		assertThat(model.mograms()).hasSize(77);
		assertThat(model.resources()).hasSize(archive.resources().size());
		for (var resource : archive.resources().entrySet())
			assertThat(model.resource(resource.getKey()).bytes()).as(resource.getKey()).isEqualTo(resource.getValue());
		assertThat(model.allMograms().stream().mapToInt(mogram -> mogram.properties().size()).sum()).isEqualTo(960);
	}

	@Test
	void readsEveryMetamodelDefinitionFromTheCompleteZip() throws Exception {
		var metamodel = Metamodels.load(resource("metamodel.zip"));
		var sources = VisoraArchive.sources(resource("metamodel.zip")).entrySet().stream()
			.filter(source -> source.getKey().startsWith("views/metta/")).toList();
		assertThat(sources).hasSize(5);
		String text = String.join("\n", sources.stream().map(Map.Entry::getValue).toList());
		try (var input = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
			assertThat(metamodel.types()).containsExactlyElementsOf(new MetamodelReader().read(input).types());
		}
		assertThat(metamodel.types()).hasSize(167);
	}

	@Test
	void comparesEveryVisoraNodeAndValueWithTheGeneratedApi() throws Exception {
		var archive = new VisoraArchive(resource("model.zip"));
		var metamodel = Metamodels.load(resource("metamodel.zip"));
		var schema = new MetamodelResolver().describe(metamodel);
		try (var generated = GeneratedDecorators.compile(archive.model(), metamodel, directory, "VisoraModel")) {
			Object facade = generated.modelDecorator("VisoraModel");
			Object loaded = facade.getClass().getMethod("load", Path.class).invoke(null, resource("model.zip"));
			Model loadedModel = (Model) FrameView.from(loaded).frameIdentity();
			assertThat(loadedModel.mograms()).containsExactlyElementsOf(archive.model().mograms());
			var loadedDecorators = new GeneratedDecorators(generated.loader(), loadedModel);
			var comparison = new VisoraComparison(archive, loadedDecorators, schema);
			comparison.verify();
			assertThat(comparison.coverage()).isEqualTo(new VisoraComparison.Coverage(608, 1742, 1205, 182, 173));
			assertThat(comparison.unresolvedReferences()).containsExactly("main.person");
			assertThat(comparison.missingResources()).containsExactly("document-overlay.pdf");
		}
	}

	private static Path resource(String name) throws Exception {
		return Path.of(VisoraModelTest.class.getResource("/visora/" + name).toURI());
	}
}

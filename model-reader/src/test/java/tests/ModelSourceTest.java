package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.ports.ModelSource;
import io.quassar.reader.adapters.FolderSource;
import io.quassar.reader.adapters.UrlSource;
import io.quassar.reader.adapters.ZipSource;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ModelSourceTest {
	@ParameterizedTest
	@MethodSource("sources")
	void preparesTheSameLibrary(ModelSource source) throws Exception {
		Model expected = Models.load(TestResources.path("folder"));
		try (InputStream input = source.open()) {
			assertThat(new ModelReader().read(input).mograms()).isEqualTo(expected.mograms());
		}
	}

	@ParameterizedTest
	@MethodSource("sources")
	void producesTextWithoutDslHeaders(ModelSource source) throws Exception {
		try (InputStream input = source.open()) {
			String text = TestResources.text(input);
			assertThat(text.lines()).noneMatch(line -> line.stripLeading().startsWith("dsl "));
			assertThat(text).contains("Book DonQuixote", "Book CollectedStories");
		}
	}

	static Stream<ModelSource> sources() throws Exception {
		return Stream.of(new FolderSource(TestResources.path("folder")),
			new ZipSource(TestResources.path("folder.zip")),
			new UrlSource(TestResources.path("folder.zip").toUri().toURL()));
	}
}

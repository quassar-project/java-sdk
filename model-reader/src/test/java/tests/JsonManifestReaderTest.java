package tests;

import io.quassar.reader.Models;
import io.quassar.reader.adapters.JsonManifestReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonManifestReaderTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(strings = {"null", "true", "1", "\"coordinate\"", "{}", "[null]", "[true]", "[1]", "[{}]", "[[]]"})
	void rejectsInvalidViewsAsIoErrorsWithoutUsingLegacyDeclarations(String views) throws Exception {
		String content = "{\"views\":" + views + ",\"dsl\":\"monet:library:1.0.0\"}";
		assertThatThrownBy(() -> new JsonManifestReader().read(content))
			.isInstanceOf(IOException.class).hasMessageContaining("manifest.json");
		writeRelease(content);
		assertThatThrownBy(() -> Models.loadForDsl(directory, "monet:library:1.0.0"))
			.isInstanceOf(IOException.class).hasMessageContaining("manifest.json");
	}

	@Test
	void preservesExactCoordinatesAndIgnoresConflictingLegacyDeclarations() throws Exception {
		String content = """
			{"views":["monet:library:1.0.0","monet:catalog:2.0.0"],
			 "dsl":"other:library:0.0.1","dsls":["library|other:library:0.0.1"]}
			""";
		assertThat(new JsonManifestReader().read(content).views())
			.containsExactly("monet:library:1.0.0", "monet:catalog:2.0.0");
		writeRelease(content);
		assertThat(Models.loadForDsl(directory, "monet:library:1.0.0").get("item").type()).isEqualTo("Item");
		assertThatThrownBy(() -> Models.loadForDsl(directory, "monet:library:2.0.0"))
			.isInstanceOf(IOException.class).hasMessageContaining("Expected DSL");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"views\":[]}", "{\"views\":[\"library|monet:library:1.0.0\"]}"})
	void doesNotFallBackToLegacyCoordinates(String content) throws Exception {
		content = content.substring(0, content.length() - 1) +
			(content.equals("{}") ? "" : ",") + "\"dsl\":\"monet:library:1.0.0\"}";
		writeRelease(content);
		assertThatThrownBy(() -> Models.loadForDsl(directory, "monet:library:1.0.0"))
			.isInstanceOf(IOException.class).hasMessageContaining("Expected DSL");
	}

	private void writeRelease(String manifest) throws IOException {
		Files.writeString(directory.resolve("manifest.json"), manifest);
		Files.writeString(directory.resolve("model.tara"), "Item item\n");
	}
}

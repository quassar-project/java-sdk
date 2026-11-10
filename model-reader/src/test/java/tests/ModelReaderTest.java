package tests;

import io.quassar.reader.Models;

import io.quassar.reader.adapters.TextSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelReaderTest {
	@Test
	void readsTheCompleteLibrary() throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.mograms()).hasSize(4);
		assertThat(model.allMograms()).hasSize(13);
		assertThat(model.get("DonQuixote").children()).hasSize(3);
	}

	@Test
	void preservesQuotedSeparators() throws Exception {
		Mogram book = TestResources.model("library.tara").get("DonQuixote");
		assertThat(book.property("summary")).isEqualTo(new Value.Text("a > b, // c = d"));
	}

	@Test
	void readsUnnamedAndInlineMograms() throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.get("Subjects").children().get(0).identifier()).isEmpty();
		assertThat(model.get("Recommendations").children().get(1).children().get(0).type())
			.isEqualTo("Transform");
		assertThat(model.get("Recommendations").children().get(0).property("isbn"))
			.isInstanceOf(Value.Empty.class);
	}

	@Test
	void readsPropertiesFromConstructorAndInlineBody() throws Exception {
		Mogram book = TestResources.model("inline-properties.tara").get("PocketEdition");
		assertThat(book.properties()).hasSize(4);
		assertThat(book.properties().get(0).name()).isNull();
		assertThat(book.property("title")).isEqualTo(new Value.Text("Title"));
	}

	@Test
	void readsTabIndentedSiblings() throws Exception {
		Mogram book = TestResources.model("tab-indentation.tara").get("PocketEdition");
		assertThat(book.children()).extracting(Mogram::identifier)
			.containsExactly("FirstEdition", "SecondEdition");
	}

	@Test
	void leavesTheSuppliedStreamOpen() throws Exception {
		byte[] content;
		try (InputStream input = new TextSource(List.of(TestResources.text("library.tara"))).open()) {
			content = input.readAllBytes();
		}
		try (TrackingInputStream input = new TrackingInputStream(content)) {
			assertThat(new ModelReader().read(input).mograms()).hasSize(4);
			assertThat(input.closed).isFalse();
		}
	}

	@Test
	void propagatesReadFailures() {
		InputStream input = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("read failed");
			}
		};
		assertThatThrownBy(() -> new ModelReader().read(input))
			.isInstanceOf(IOException.class).hasMessage("read failed");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "TARA_MODEL_ZIP", matches = ".+")
	void readsTheOriginalInventory() throws Exception {
		Model model = Models.load(Path.of(System.getenv("TARA_MODEL_ZIP")));
		assertThat(model.mograms()).hasSize(583);
		assertThat(model.allMograms()).hasSize(11137);
		assertThat(model.select().type("Entidad").count()).isEqualTo(5);
		assertThat(model.get("Persona.Juridica").identifier()).isEqualTo("Juridica");
	}

	private static final class TrackingInputStream extends ByteArrayInputStream {
		private boolean closed;

		private TrackingInputStream(byte[] bytes) {
			super(bytes);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}
}

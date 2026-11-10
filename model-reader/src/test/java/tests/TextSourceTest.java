package tests;

import io.quassar.reader.adapters.TextSource;
import org.junit.jupiter.api.Test;

import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class TextSourceTest {
	@Test
	void normalizesEachSource() throws Exception {
		try (InputStream input = new TextSource(List.of(
				TestResources.text("normalization/first.tara"),
				TestResources.text("normalization/second.tara"))).open()) {
			String prepared = TestResources.text(input);
			assertThat(prepared).doesNotContain("\uFEFF");
			assertThat(prepared.lines()).noneMatch(line -> line.stripLeading().startsWith("dsl "));
			assertThat(prepared).doesNotContain("dsl\tlibrary");
			assertThat(prepared).contains("Book DonQuixote\n");
			assertThat(prepared).contains("Book TheTrial\n");
		}
	}

	@Test
	void keepsQuotedDslText() throws Exception {
		try (InputStream input = new TextSource(List.of(TestResources.text("normalization/first.tara"))).open()) {
			String text = TestResources.text(input);
			assertThat(text).contains("summary = \"dsl library\"");
		}
	}

	@Test
	void joinsFilesWithoutFinalNewlines() throws Exception {
		try (InputStream input = new TextSource(List.of(
				TestResources.text("cross-file/first.tara"),
				TestResources.text("cross-file/nested/second.tara"))).open()) {
			Model model = new ModelReader().read(input);
			assertThat(model.roots().count()).isEqualTo(2);
			assertThat(model.get("FirstEdition").identifier()).isEqualTo("FirstEdition");
			assertThat(model.get("SecondEdition").identifier()).isEqualTo("SecondEdition");
		}
	}

}

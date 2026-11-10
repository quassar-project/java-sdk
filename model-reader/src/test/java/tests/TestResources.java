package tests;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quassar.reader.adapters.TextSource;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;

final class TestResources {
	private TestResources() {
	}

	static InputStream open(String name) {
		return TestResources.class.getResourceAsStream("/tara/" + name);
	}

	static String text(String name) throws Exception {
		return Files.readString(path(name));
	}

	static String text(InputStream input) throws IOException {
		return new String(input.readAllBytes(), UTF_8);
	}

	static Path path(String name) throws Exception {
		return Path.of(TestResources.class.getResource("/tara/" + name).toURI());
	}

	static Model model(String name) throws Exception {
		try (InputStream input = new TextSource(List.of(text(name))).open()) {
			return new ModelReader().read(input);
		}
	}

	static void immutable(Runnable mutation) {
		assertThatThrownBy(mutation::run).isInstanceOf(UnsupportedOperationException.class);
	}
}

package tests;

import io.quassar.templating.TemplateEngine;

import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.model.Template;
import io.quassar.templating.ports.TemplateReadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TestTemplates {
	private TestTemplates() {
	}

	public static InputStream input(String path) {
		return TestTemplates.class.getResourceAsStream("/" + path + ".itr");
	}

	public static String text(String path) {
		try (InputStream input = input(path)) {
			return new String(input.readAllBytes(), UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static String expected(String name) {
		try (InputStream input = TestTemplates.class.getResourceAsStream("/expected/" + name + ".txt")) {
			return new String(input.readAllBytes(), UTF_8).stripTrailing();
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static Template read(String path) {
		try (InputStream input = input(path)) {
			return new TemplateReader(input).read();
		} catch (IOException | TemplateReadException exception) {
			throw new IllegalStateException("Could not read test template " + path, exception);
		}
	}

	public static TemplateEngine engine(String path) {
		return TemplateEngine.with(read(path));
	}
}

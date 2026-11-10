package tests;

import io.quassar.templating.TemplateEngine;

import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import io.quassar.templating.adapters.reflection.ObjectFrameFactory;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.ports.TemplateReadException;
import io.quassar.templating.ports.TemplateLoader;
import io.quassar.templating.model.Template;
import org.junit.jupiter.api.Test;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineFactoryTest {
	@Test
	public void readsAndRendersAnInputStreamWithoutClosingIt() throws Exception {
		try (TrackingInput input = new TrackingInput(bookLabel())) {
			TemplateEngine engine = TemplateEngine.with(input);
			assertThat(engine.render(new ReflectiveFrameBuilder("Book").add("title", "Don Quixote"))).isEqualTo("Book: Don Quixote");
			assertThat(input.closed).isFalse();
		}
	}

	@Test
	public void loadsThroughTheTemplatePort() throws Exception {
		TemplateLoader loader = () -> TestTemplates.read("rendering/book-label");
		assertThat(TemplateEngine.with(loader).render(new ReflectiveFrameBuilder("Book").add("title", "Don Quixote")))
			.isEqualTo("Book: Don Quixote");
	}

	@Test
	public void usesTheInjectedFrameAdapter() throws Exception {
		Template template;
		try (InputStream input = bookLabel()) {
			template = new TemplateReader(input).read();
		}
		TemplateEngine engine = TemplateEngine.with(template).using(
			(object, adapters) -> new ReflectiveFrameBuilder("Book").add("title", object).toFrame());
		assertThat(engine.render("Don Quixote")).isEqualTo("Book: Don Quixote");
	}

	@Test
	public void configuresTheFactoryAfterLoadingAndKeepsRegisteredFormatters() throws Exception {
		try (InputStream input = TestTemplates.input("rendering/labeled-book")) {
			TemplateEngine engine = TemplateEngine.with(input).add("Label", value -> "[" + value + "]");
			assertThat(engine.using((object, mappers) -> object instanceof StringBuilder
				? new ReflectiveFrameBuilder("Book").add("title", object.toString()).toFrame()
				: new ObjectFrameFactory().create(object, mappers))).isSameAs(engine);
			assertThat(engine.render(new StringBuilder("Don Quixote"))).isEqualTo("[Don Quixote]");
		}
	}

	@Test
	public void resolvesEachStepAgainstTheCurrentFrame() {
		Template template = TestTemplates.read("rendering/author-path");
		ReflectiveFrameBuilder author = new ReflectiveFrameBuilder("Author").add("name", "Ada");
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book").add("author", author);
		ReflectiveFrameBuilder library = new ReflectiveFrameBuilder("Library").add("book", book);
		assertThat(TemplateEngine.with(template).render(library)).isEqualTo("Ada");
	}

	@Test
	public void reportsAnUnreadableTemplateInsteadOfRenderingNothing() {
		InputStream input = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("Unavailable template");
			}
		};
		assertThatThrownBy(() -> TemplateEngine.with(input))
			.isInstanceOf(TemplateReadException.class)
			.hasCauseInstanceOf(IOException.class);
	}

	private InputStream bookLabel() {
		return TestTemplates.input("rendering/book-label");
	}

	private static final class TrackingInput extends FilterInputStream {
		private boolean closed;

		private TrackingInput(InputStream input) {
			super(input);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}
}

package tests;

import tests.fixtures.MetamodelFixtures;
import io.quassar.metamodel.Metamodels;
import io.quassar.metamodel.model.Metamodel;
import tests.fixtures.GeneratedDecorators;
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.templating.TemplateEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DecoratorRenderingTest {
	@TempDir
	Path directory;

	@Test
	void rendersDecoratorsAndPreservesTheirRelationships() throws Exception {
		Metamodel metamodel = MetamodelFixtures.library();
		TemplateEngine engine;
		try (InputStream input = getClass().getResourceAsStream("/templates/rendering.itr")) {
			engine = TemplateEngine.with(input);
		}
		Model model = model();
		try (GeneratedDecorators generated = GeneratedDecorators.compile(model, metamodel, directory)) {
			Object decorated = generated.wrap("Book", model.get("DonQuixote"));
			assertThat(decorated.getClass().getAnnotations()).isEmpty();
			String book = engine.render(decorated);
			assertThat(book).contains("Book DonQuixote", "title: Don Quixote", "isbn: 9780000000001",
				"authors: Cervantes, Kafka", "related: Nocturne", "tags: novel, adventure",
				"mixed: classic, true, 42, Cervantes",
				"Chapter Opening", "previous: Ending", "Chapter Ending");
			assertThat(engine.render(decorated)).isEqualTo(book);
			assertThat(engine.render(generated.modelDecorator())).contains("Book DonQuixote", "Book Nocturne", "Author Cervantes", "Author Kafka");
			assertThat(engine.render(generated.wrap("Author", model.get("Cervantes"))))
				.contains("Author Cervantes", "name: Miguel de Cervantes");
			assertThat(engine.render(generated.wrap("Note", model.get("DonQuixote.Anniversary").children().get(0))))
				.contains("Note", "value1: Appendix", "value2: 3", "value3: true");
			assertThat(engine.render(generated.wrap("Book", Mogram.NULL))).isEmpty();
			Frame frame = Frames.from(decorated);
			assertThat(frame.is("printed")).isTrue();
			assertThat(frame.frames("related").next().frames("related").next()).isSameAs(frame);
			Frame opening = frame.frames("chapter").next();
			assertThat(opening.container()).isSameAs(frame);
			Frame ending = opening.frames("previous").next();
			assertThat(ending.container()).isSameAs(frame);
			assertThat(ending.frames("previous").next()).isSameAs(opening);
			assertThat(frame.contains("rating")).isFalse();
			assertThat(frame.container()).isNull();
			assertThat(frame.contains("mogram")).isFalse();
			assertThat(frame.contains("model")).isFalse();
			assertThat(Frames.from(generated.modelDecorator()).frames("chapter")).isExhausted();
		}
	}

	@Test
	void rendersScopedChildrenAndReferences() throws Exception {
		Metamodel metamodel = Metamodels.load(MetamodelFixtures.libraryArchive(directory));
		try (InputStream template = getClass().getResourceAsStream("/templates/scoped-references.itr");
			 InputStream source = getClass().getResourceAsStream("/m1/cardinality.tara");
			 GeneratedDecorators generated = GeneratedDecorators.compile(new ModelReader().read(source), metamodel, directory)) {
			String result = TemplateEngine.with(template).render(generated.modelDecorator());
			assertThat(result).contains("Catalogue Main", "Opening: First chapter", "Notes: true");
		}
	}

	private Model model() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/m1/rendering.tara")) {
			return new ModelReader().read(input);
		}
	}
}

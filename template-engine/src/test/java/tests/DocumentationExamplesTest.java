package tests;

import io.quassar.templating.TemplateEngine;

import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentationExamplesTest {
	@Test
	public void rendersBooksAuthorsAndOptionalIsbn() throws Exception {
		Library library = new Library("City Library", List.of(
			new Book("Don Quixote", 1605, new Author("Miguel de Cervantes"), "9788420412146", true),
			new Book("The Trial", 1925, new Author("Franz Kafka"), null, false)));
		String result = render("catalogue", library);
		assertThat(result).isEqualTo(TestTemplates.expected("catalogue"));
	}

	@Test
	public void selectsTheFirstMatchingRule() throws Exception {
		assertThat(render("availability", book(true))).isEqualTo("Don Quixote: available");
		assertThat(render("availability", book(false))).isEqualTo("Don Quixote: on loan");
	}

	@Test
	public void rendersAFallbackForAnAbsentSlot() throws Exception {
		assertThat(render("fallback", book(true))).isEqualTo("Don Quixote: ISBN 9788420412146");
		Book book = new Book("The Trial", 1925, new Author("Franz Kafka"), null, false);
		assertThat(render("fallback", book)).isEqualTo("The Trial: ISBN unavailable");
	}

	@Test
	public void rendersRecursiveChapters() throws Exception {
		Chapter chapter = new Chapter("Contents", List.of(
			new Chapter("Beginnings", List.of()),
			new Chapter("Journey", List.of(new Chapter("Home", List.of())))));
		assertThat(render("contents", chapter)).isEqualTo("Contents\n\tBeginnings\n\tJourney\n\t\tHome");
	}

	@Test
	public void generatesAJavaRecord() throws Exception {
		Entity entity = new Entity("Book", List.of(new Property("String", "title"), new Property("int", "year")));
		assertThat(render("record", entity)).isEqualTo("public record Book(String title, int year) {}");
	}

	@Test
	public void configuresAFormatterAndAnAdapter() throws Exception {
		try (InputStream input = template("label")) {
			TemplateEngine engine = TemplateEngine.with(input).add("Label", value -> "[" + value + "]");
			assertThat(engine.render(book(true))).isEqualTo("[Don Quixote]");
		}
		try (InputStream input = template("label")) {
			TemplateEngine engine = TemplateEngine.with(input)
				.add("Label", value -> "[" + value + "]")
				.add(ExternalBook.class, (source, frame) -> frame.add("Book").add("title", source.label()));
			assertThat(engine.render(new ExternalBook("The Trial"))).isEqualTo("[The Trial]");
		}
	}

	@Test
	public void rendersAFrameAndItsContainer() throws Exception {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote");
		ReflectiveFrameBuilder library = new ReflectiveFrameBuilder("Library").add("name", "City Library").add("books", book);
		assertThat(render("context", library)).isEqualTo("Don Quixote (City Library)");
	}

	private Book book(boolean available) {
		return new Book("Don Quixote", 1605, new Author("Miguel de Cervantes"), "9788420412146", available);
	}

	private String render(String name, Object object) throws Exception {
		try (InputStream input = template(name)) {
			return TemplateEngine.with(input).render(object);
		}
	}

	private InputStream template(String name) {
		return TestTemplates.input("examples/" + name);
	}

	public record Author(String name) {
	}

	public record Book(String title, int year, Author author, String isbn, boolean available) {
	}

	public record Library(String name, List<Book> books) {
		public Library {
			books = List.copyOf(books);
		}
	}

	public record Chapter(String title, List<Chapter> children) {
		public Chapter {
			children = List.copyOf(children);
		}
	}

	public record Entity(String name, List<Property> properties) {
		public Entity {
			properties = List.copyOf(properties);
		}
	}

	public record Property(String type, String name) {
	}

	public record ExternalBook(String label) {
	}
}

package tests;

import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import io.quassar.templating.model.Template;
import tests.fixtures.Author;
import tests.fixtures.Chapter;
import tests.fixtures.Kind;
import tests.fixtures.Library;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static tests.TestTemplates.engine;
import static tests.TestTemplates.expected;
import static tests.fixtures.LibraryFixtures.book;
import static tests.fixtures.LibraryFixtures.chapters;
import static tests.fixtures.LibraryFixtures.trial;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplatesTest {
	@Test
	public void addsRulesInOrderAndReturnsTheTemplate() {
		var rules = TestTemplates.read("rendering/library-xml").ruleSet();
		Template template = new Template();
		assertThat(template.add(rules.get(0)).add(rules.get(1))).isSameAs(template);
		assertThat(template.ruleSet()).containsExactly(rules.get(0), rules.get(1));
		assertThat(Template.EMPTY.ruleSet()).isEmpty();
	}

	@Test
	public void quotesAndEscapesJsonStrings() {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book")
			.add("title", "L'édition \"spéciale\"\\A\nB\rC\tD\bE\fF\u0001");
		assertThat(engine("rendering/book-json").render(book))
			.isEqualTo("{\"title\": \"L'édition \\\"spéciale\\\"\\\\A\\nB\\rC\\tD\\bE\\fF\\u0001\"}");
	}

	@Test
	public void formatsBookDatesAndNumbers() {
		ReflectiveFrameBuilder book = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote")
			.add("published", LocalDate.of(1605, 1, 16)).add("pages", 208).add("copies", 240000);
		assertThat(engine("rendering/book-formatting").render(book)).isEqualTo(expected("book-formatting"));
	}

	@Test
	public void rendersALoanNoticeWithAnOptionalSubject() {
		ReflectiveFrameBuilder notice = new ReflectiveFrameBuilder("Notice").add("from", "loans@library.example")
			.add("to", new String[]{"ada@example.org", "lin@example.org"})
			.add("subject", "Book reservation").add("lines", new String[]{"Don Quixote is ready.", "Collect it before Friday."});
		assertThat(engine("rendering/loan-notice").render(notice)).isEqualTo(expected("loan-notice"));
		ReflectiveFrameBuilder withoutSubject = new ReflectiveFrameBuilder("Notice").add("from", "loans@library.example")
			.add("lines", new String[]{"Don Quixote is ready.", "Collect it before Friday."});
		assertThat(engine("rendering/loan-notice").render(withoutSubject)).isEqualTo(expected("loan-notice-no-subject"));
	}

	@Test
	public void rendersARecursiveChapterTreeAsXml() {
		Chapter tree = new Chapter("Contents", Kind.Main, List.of(new Chapter("Preface", Kind.Introduction),
			new Chapter("Journey", Kind.Main, List.of(new Chapter("Home", Kind.Main), new Chapter("Afterword", Kind.Appendix)))));
		assertThat(engine("rendering/chapter-tree").render(tree)).isEqualTo(expected("chapter-tree"));
	}

	@Test
	public void rendersALibraryWithItsLibrarian() {
		Library library = new Library("City Library", new Author("Ada"), List.of(book()));
		assertThat(engine("rendering/library-xml").render(library)).isEqualTo(expected("library-with-librarian"));
	}

	@Test
	public void omitsAnAbsentLibrarian() {
		Library library = new Library("City Library", null, List.of(book()));
		assertThat(engine("rendering/library-xml").render(library)).isEqualTo(expected("library-without-librarian"));
	}

	@Test
	public void rendersBooksAndTheirChaptersWithConsistentIndentation() {
		Library library = new Library("City Library", new Author("Ada"), List.of(book().withChapters(chapters()), trial()));
		assertThat(engine("rendering/library-xml").render(library)).isEqualTo(expected("library-chapters"));
	}
}

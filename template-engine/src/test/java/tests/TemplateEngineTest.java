package tests;

import io.quassar.templating.TemplateEngine;

import tests.fixtures.Book;
import tests.fixtures.Chapter;
import tests.fixtures.Kind;
import tests.fixtures.Genre;
import io.quassar.templating.model.Template;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static tests.TestTemplates.engine;
import static tests.TestTemplates.expected;
import static tests.fixtures.LibraryFixtures.book;
import static tests.fixtures.LibraryFixtures.editions;
import static tests.fixtures.LibraryFixtures.chapters;
import static io.quassar.templating.model.Template.Configuration.LineSeparator.CRLF;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineTest {
	@Test
	public void rendersPrimitiveValuesWithoutMatchingRules() {
		TemplateEngine engine = engine("rendering/primitive");
		assertThat(engine.render("Library")).isEqualTo("Library");
		assertThat(engine.render(1605)).isEqualTo("1605");
		assertThat(engine.render(2.5)).isEqualTo("2.5");
		assertThat(engine.render(Genre.Novel)).isEqualTo("Novel");
	}

	@Test
	public void rendersABookAndAppliesATriggerRule() {
		assertThat(engine("rendering/summary").render(book()))
			.isEqualTo("DON QUIXOTE was published by Acme on 16/01/1605");
	}

	@Test
	public void matchesTypesSlotsAndFormattersIgnoringCase() {
		assertThat(engine("rendering/case-insensitive").render(book()))
			.isEqualTo("Don Quixote was published by Acme on 16/01/1605");
	}

	@Test
	public void leavesUnknownFormattersAsIdentityAndChainsKnownOnes() {
		assertThat(engine("rendering/unknown-formatter").render(book())).isEqualTo("Don quixote");
	}

	@Test
	public void appliesACustomFormatter() {
		assertThat(engine("rendering/custom-formatter").add("Label", value -> "[" + value + "]").render(book()))
			.isEqualTo("[Don Quixote]");
	}

	@Test
	public void appliesACustomObjectAdapter() {
		assertThat(engine("rendering/summary").add(Book.class,
			(source, frame) -> frame.add("title", "Unknown").add("publisher", "Library")).render(book()))
			.isEqualTo("UNKNOWN was published by Library on ");
	}

	@Test
	public void omitsExpressionsForAbsentSlots() {
		Book book = new Book("Don Quixote", null, null, Genre.Novel, List.of(), List.of());
		assertThat(engine("rendering/optional-publication").render(book)).isEqualTo("Don Quixote");
	}

	@Test
	public void rendersAnEnum() {
		assertThat(engine("rendering/genre").render(book())).isEqualTo("Don Quixote is Novel");
	}

	@Test
	public void combinesRootAndPlaceholderRules() {
		assertThat(engine("rendering/genre-with-title-rule").render(book())).isEqualTo("*Don Quixote* is Novel");
	}

	@Test
	public void rendersMultipleChaptersAndTheirCount() {
		assertThat(engine("rendering/chapter-list").render(book().withChapters(chapters())))
			.isEqualTo("Don Quixote has 3 chapters: Preface, The Journey, Notes");
	}

	@Test
	public void replacesAZeroCountThroughAConditionalRule() {
		assertThat(engine("rendering/chapter-list").render(book())).isEqualTo("Don Quixote has no chapters: ");
	}

	@Test
	public void omitsTheListLabelWhenThereAreNoChapters() {
		assertThat(engine("rendering/optional-chapter-list").render(book())).isEqualTo("Don Quixote has no chapters");
	}

	@Test
	public void indentsEachChapterAndItsKind() {
		assertThat(engine("rendering/indented-chapters").render(book().withChapters(chapters())))
			.isEqualTo("Don Quixote has 3 chapters\n\tPreface\n\t\tIntroduction\n\n\tThe Journey\n\t\tMain\n\n\tNotes\n\t\tAppendix");
	}

	@Test
	public void preservesIndentationInsideAnExpression() {
		assertThat(engine("rendering/optional-indented-chapters").render(book().withChapters(chapters())))
			.isEqualTo(expected("indented-chapters"));
	}

	@Test
	public void removesIndentationForAnEmptyExpression() {
		assertThat(engine("rendering/optional-indented-chapters").render(book())).isEqualTo("Don Quixote has no chapters");
	}

	@Test
	public void omitsNonPrimitiveValuesWithoutAMatchingRule() {
		assertThat(engine("rendering/unmatched-chapters").render(book().withChapters(chapters()))).isEqualTo("Don Quixote has 3 chapters");
	}

	@Test
	public void rendersIndependentOptionalSections() {
		TemplateEngine engine = engine("rendering/nested-sections");
		assertThat(engine.render(book())).isEqualTo("Don Quixote");
		assertThat(engine.render(book().withChapters(chapters()))).contains("Chapters", "Preface").doesNotContain("Editions");
		assertThat(engine.render(book().withEditions(editions()))).contains("Editions", "First edition").doesNotContain("Chapters");
		assertThat(engine.render(book().withChapters(chapters()).withEditions(editions())))
			.isEqualTo("Don Quixote\n\tChapters\n\t\tPreface\n\t\tThe Journey\n\t\tNotes\n\tEditions\n\t\tFirst edition\n\t\tIllustrated edition");
	}

	@Test
	public void filtersChaptersByTheirKind() {
		assertThat(engine("rendering/introductions").render(book().withChapters(chapters())))
			.isEqualTo("Don Quixote has these introductions:\n\tPreface");
	}

	@Test
	public void filtersChaptersWithANegatedCondition() {
		assertThat(engine("rendering/other-chapters").render(book().withChapters(chapters())))
			.isEqualTo("Don Quixote contains The Journey, Notes");
	}

	@Test
	public void usesAFallbackForEmptyAndFilteredLists() {
		assertThat(engine("rendering/introductions").render(book())).isEqualTo("Don Quixote has no introductions");
		assertThat(engine("rendering/introductions").render(book().withChapters(List.of(new Chapter("Journey", Kind.Main)))))
			.isEqualTo("Don Quixote has no introductions");
	}

	@Test
	public void usesConfiguredLineEndings() {
		Template template = TestTemplates.read("rendering/indented-chapters");
		TemplateEngine engine = TemplateEngine.with(template.ruleSet(), new Template.Configuration(Locale.ENGLISH, CRLF));
		assertThat(engine.render(book().withChapters(chapters()))).contains("\r\n\tPreface").doesNotContain("\n\tPreface\n");
	}
}

package tests;

import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;
import org.junit.jupiter.api.Test;
import tests.fixtures.AnnotatedBook;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static tests.fixtures.LibraryFixtures.book;

public class AnnotatedFramesTest {
	@Test
	public void rendersOnlyAnnotatedMethods() {
		AnnotatedBook book = new AnnotatedBook(book());
		assertThat(TestTemplates.engine("rendering/annotated-book").render(book))
			.isEqualTo("DonQuixote: Don Quixote — Acme (novel, classic)");
		Frame frame = Frames.from(book);
		assertThat(frame.is("book")).isTrue();
		assertThat(frame.is("printed")).isTrue();
		assertThat(frame.contains("source")).isFalse();
		assertThat(frame.contains("facets")).isFalse();
		assertThat(frame.contains("notes")).isTrue();
		assertThat(frame.frames("notes")).isExhausted();
		assertThat(frame.container()).isNull();
	}

	@Test
	public void allowsExplicitMappersToOverrideAnnotationMapping() {
		var engine = TestTemplates.engine("rendering/book-label")
			.add(AnnotatedBook.class, (book, frame) -> frame.add("Book").add("title", "Custom projection"));
		assertThat(engine.render(new AnnotatedBook(book()))).isEqualTo("Book: Custom projection");
	}

	@Test
	public void preservesIdentityAcrossFreshWrappersAndIndependentConversions() {
		AnnotatedBook book = new AnnotatedBook(book());
		Frame frame = Frames.from(book);
		assertThat(frame.frames("related").next()).isSameAs(frame);
		assertThat(Frames.from(book)).isNotSameAs(frame);
		assertThat(frame.frames("title").next().container()).isSameAs(frame);
		List<Frame> tags = new ArrayList<>();
		frame.frames("tags").forEachRemaining(tags::add);
		assertThat(tags.stream().map(Frame::value).toList()).containsExactly("novel", "classic");
	}
}

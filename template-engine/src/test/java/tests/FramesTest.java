package tests;

import io.quassar.templating.Frames;

import tests.fixtures.Book;
import io.quassar.templating.model.Frame;
import tests.fixtures.PrintRun;
import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static tests.fixtures.LibraryFixtures.book;
import static tests.fixtures.LibraryFixtures.editions;
import static tests.fixtures.LibraryFixtures.chapters;
import static org.assertj.core.api.Assertions.assertThat;

public class FramesTest {
	@Test
	public void createsAFrameFromABookRecord() {
		Frame frame = Frames.from(book().withChapters(chapters()).withEditions(editions()));
		assertThat(frame.is("book")).isTrue();
		assertThat(frame.frames("title").next().value()).isEqualTo("Don Quixote");
		assertThat(frame.frames("chaptersCount").next().value()).isEqualTo(3);
		Frame chapter = frame.frames("chapters").next();
		assertThat(chapter.frames("title").next().value()).isEqualTo("Preface");
		assertThat(chapter.container()).isSameAs(frame);
		assertThat(frame.frames("editions").next().frames("title").next().value()).isEqualTo("First edition");
	}

	@Test
	public void keepsAnExistingFrame() {
		Frame frame = Frames.from(book());
		assertThat(Frames.from(frame)).isSameAs(frame);
	}

	@Test
	public void createsPrimitiveFrames() {
		assertThat(Frames.from("Don Quixote").value()).isEqualTo("Don Quixote");
		assertThat(Frames.from(1605).value()).isEqualTo(1605);
		assertThat(Frames.from(true).value()).isEqualTo(true);
	}

	@Test
	public void treatsArbitraryPrecisionNumbersAsScalarValues() {
		BigDecimal decimal = new BigDecimal("3.50");
		BigInteger integer = new BigInteger("12345678901234567890");
		assertThat(Frames.from(decimal).value()).isEqualTo(decimal);
		assertThat(Frames.from(integer).value()).isEqualTo(integer);
	}

	@Test
	public void adaptsPrimitiveArrayFields() {
		Frame frame = Frames.from(new PrintRun(new int[]{250, 500}, new boolean[]{true, false}));
		assertThat(values(frame, "copies")).containsExactly(250, 500);
		assertThat(values(frame, "approved")).containsExactly(true, false);
		assertThat(frame.frames("copiesCount").next().value()).isEqualTo(2);
	}

	@Test
	public void createsSlotsFromPrimitiveArrays() {
		Frame frame = new ReflectiveFrameBuilder("PrintRun").add("copies", new int[]{250, 500}).toFrame();
		assertThat(values(frame, "copies")).containsExactly(250, 500);
	}

	@Test
	public void keepsSeparateParentLinksAcrossConversions() {
		Book book = book().withChapters(chapters());
		Frame first = Frames.from(book);
		Frame second = Frames.from(book);
		assertThat(first.frames("chapters").next()).isNotSameAs(second.frames("chapters").next());
		assertThat(first.frames("chapters").next().container()).isSameAs(first);
		assertThat(second.frames("chapters").next().container()).isSameAs(second);
	}

	private List<Object> values(Frame frame, String slot) {
		List<Object> values = new ArrayList<>();
		frame.frames(slot).forEachRemaining(value -> values.add(value.value()));
		return values;
	}

}

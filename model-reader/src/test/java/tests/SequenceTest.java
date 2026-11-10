package tests;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class SequenceTest {
	@Test
	void readsBooleanSequencesInline() throws Exception {
		Mogram book = model().get("DonQuixote");
		assertThat(sequence(book, "flags")).containsExactly(
			new Value.Bool(true),
			new Value.Bool(false),
			new Value.Bool(true));
	}

	@Test
	void preservesQuotedTexts() throws Exception {
		Mogram book = model().get("DonQuixote");
		assertThat(sequence(book, "labels")).containsExactly(
			new Value.Text("novel"),
			new Value.Text("true"),
			new Value.Text("12"),
			new Value.Text("Cervantes"),
			new Value.Text("a > b, // c = d"),
			new Value.Text("quoted \"word\""));
	}

	@Test
	void readsNumericSequences() throws Exception {
		Mogram book = model().get("DonQuixote");
		assertThat(sequence(book, "numbers")).containsExactly(
			new Value.Number(new BigDecimal("-2")),
			new Value.Number(new BigDecimal("0")),
			new Value.Number(new BigDecimal("3.50")),
			new Value.Number(new BigDecimal("1e3")));
	}

	@Test
	void resolvesForwardReferences() throws Exception {
		Model model = model();
		List<Value> authors = sequence(model.get("DonQuixote"), "authors");
		assertThat(authors).containsExactly(new Value.Reference("Cervantes"), new Value.Reference("Kafka"));
		assertThat(model.get(authors.get(0))).isSameAs(model.get("Cervantes"));
		assertThat(model.get(authors.get(1))).isSameAs(model.get("Kafka"));
	}

	@Test
	void resolvesRelativeReferences() throws Exception {
		Model model = model();
		Mogram book = model.get("DonQuixote");
		List<Value> chapters = sequence(book, "chapters");
		assertThat(model.get(chapters.get(0), book)).isSameAs(model.get("DonQuixote.Intro"));
		assertThat(model.get(chapters.get(1), book)).isSameAs(model.get("DonQuixote.Ending"));
	}

	@Test
	void preservesTypesInMixedSequences() throws Exception {
		Model model = model();
		List<Value> mixed = sequence(model.get("DonQuixote"), "mixed");
		assertThat(mixed).containsExactly(
			new Value.Text("true"),
			new Value.Bool(false),
			new Value.Number(new BigDecimal("42")),
			new Value.Reference("Cervantes"));
		assertThat(model.get(mixed.get(3))).isSameAs(model.get("Cervantes"));
		TestResources.immutable(() -> mixed.clear());
	}

	@Test
	void readsSequencesInConstructorAndFacet() throws Exception {
		Mogram book = model().get("DonQuixote");
		assertThat(sequence(book, "years")).containsExactly(
			new Value.Number(new BigDecimal("1605")),
			new Value.Number(new BigDecimal("1615")));
		assertThat(sequence(book, "formats")).containsExactly(new Value.Text("paperback"), new Value.Text("hardcover"));
		assertThat(book.facets()).containsExactly("Printed");
	}

	@Test
	void keepsSingleValuesAsScalars() throws Exception {
		Mogram book = model().get("DonQuixote");
		assertThat(book.property("available")).isEqualTo(new Value.Bool(true));
		assertThat(book.property("title")).isEqualTo(new Value.Text("Don Quijote de la Mancha"));
		assertThat(book.children().get(0).property("number")).isEqualTo(new Value.Number(BigDecimal.ONE));
	}

	private static List<Value> sequence(Mogram mogram, String property) {
		return ((Value.Sequence) mogram.property(property)).values();
	}

	private static Model model() throws Exception {
		return TestResources.model("model/sequences.tara");
	}
}

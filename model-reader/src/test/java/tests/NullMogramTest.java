package tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NullMogramTest {
	@Test
	void getsReferencePropertiesWithoutCasting() throws Exception {
		Model model = TestResources.model("model/critical-edition.tara");
		Mogram book = model.get("Atlas");
		assertThat(model.get(book.property("publisher"))).isSameAs(model.get("NorthPress"));
		assertThat(model.get(new Value.Reference("Journey.Arrival"), book))
			.isSameAs(model.get("Atlas.Journey.Arrival"));
	}

	@ParameterizedTest
	@MethodSource("nonReferences")
	void nonReferencePropertiesReturnNullMogram(Value value) throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.get(value)).isSameAs(Mogram.NULL);
		assertThat(model.get(value, model.get("DonQuixote"))).isSameAs(Mogram.NULL);
	}

	@Test
	void missingTargetsReturnNullMogram() throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.get("Unknown")).isSameAs(Mogram.NULL);
		assertThat(model.get(new Value.Reference("Unknown"))).isSameAs(Mogram.NULL);
		assertThat(model.get(new Value.Reference("Unknown"), model.get("DonQuixote")))
			.isSameAs(Mogram.NULL);
	}

	@Test
	void parentIsAMogramInsteadOfAnOptional() throws Exception {
		Model model = TestResources.model("library.tara");
		Mogram root = model.get("DonQuixote");
		assertThat(model.parentOf(model.get("DonQuixote.PartOne"))).isSameAs(root);
		assertThat(model.parentOf(root)).isSameAs(Mogram.NULL);
		assertThat(model.parentOf(Mogram.NULL)).isSameAs(Mogram.NULL);
	}

	@Test
	void nullMogramSupportsChaining() throws Exception {
		Model model = TestResources.model("library.tara");
		Mogram missing = model.parentOf(model.get("DonQuixote"));
		assertThat(missing.isNull()).isTrue();
		assertThat(missing.type()).isEmpty();
		assertThat(missing.identifier()).isEmpty();
		assertThat(missing.children()).isEmpty();
		assertThat(missing.facets()).isEmpty();
		assertThat(missing.properties()).isEmpty();
		assertThat(missing.property("author")).isInstanceOf(Value.Empty.class);
		assertThat(model.get(missing.property("author"))).isSameAs(Mogram.NULL);
		assertThat(model.get("DonQuixote").isNull()).isFalse();
	}

	@Test
	void queriesExcludeNullParentsAndSelections() throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.roots().parents().list()).isEmpty();
		assertThat(model.select(Mogram.NULL).stream()).isEmpty();
		assertThat(model.select(model.get("DonQuixote")).children().parents().list())
			.containsExactly(model.get("DonQuixote"));
	}

	static Stream<Value> nonReferences() {
		return Stream.of(new Value.Text("DonQuixote"), new Value.Number(BigDecimal.ONE),
			new Value.Bool(true), new Value.Sequence(List.of(new Value.Reference("DonQuixote"))),
			new Value.Empty());
	}
}

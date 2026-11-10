package tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValueTest {
	@ParameterizedTest
	@MethodSource("values")
	void identifiesExactlyOneType(Value value, String expected) {
		List<String> types = new ArrayList<>();
		if (value.isString()) types.add("string");
		if (value.isNumber()) types.add("number");
		if (value.isBoolean()) types.add("boolean");
		if (value.isReference()) types.add("reference");
		if (value.isSequence()) types.add("sequence");
		if (value.isEmpty()) types.add("empty");
		assertThat(types).containsExactly(expected);
	}

	@ParameterizedTest
	@MethodSource("values")
	void returnsEmptyDefaultsForOtherTypes(Value value, String type) {
		if (!value.isString()) assertThat(value.asString()).isEmpty();
		if (!value.isNumber()) assertThat(value.asNumber()).isEqualByComparingTo(BigDecimal.ZERO);
		if (!value.isBoolean()) assertThat(value.asBoolean()).isFalse();
		if (!value.isReference()) assertThat(value.asReference()).isEmpty();
		if (!value.isSequence()) assertThat(value.asSequence()).isEmpty();
	}

	@Test
	void extractsParsedPropertiesAndReferencesWithoutCasts() throws Exception {
		Model model = TestResources.model("model/sequences.tara");
		Mogram book = model.get("DonQuixote");
		assertThat(book.property("title").asString()).isEqualTo("Don Quijote de la Mancha");
		assertThat(book.property("available").asBoolean()).isTrue();
		assertThat(book.property("flags").asSequence()).extracting(Value::asBoolean)
			.containsExactly(true, false, true);
		assertThat(book.property("numbers").asSequence().get(2).asNumber()).isEqualTo(new BigDecimal("3.50"));
		Value author = book.property("authors").asSequence().get(0);
		assertThat(author.asReference()).isEqualTo("Cervantes");
		assertThat(model.get(author)).isSameAs(model.get("Cervantes"));
		TestResources.immutable(() -> book.property("authors").asSequence().clear());
	}

	@Test
	void chainsMissingPropertiesFromNullMograms() throws Exception {
		Model model = TestResources.model("library.tara");
		Value missing = model.select().type("Unknown").first().property("title");
		assertThat(missing.isEmpty()).isTrue();
		assertThat(missing.asString()).isEmpty();
		assertThat(missing.asNumber()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(missing.asBoolean()).isFalse();
		assertThat(missing.asReference()).isEmpty();
		assertThat(missing.asSequence()).isEmpty();
	}

	@Test
	void doesNotCoerceQuotedBooleansOrReferenceNames() throws Exception {
		Mogram book = TestResources.model("model/sequences.tara").get("DonQuixote");
		Value quotedBoolean = book.property("labels").asSequence().get(1);
		assertThat(quotedBoolean.isString()).isTrue();
		assertThat(quotedBoolean.asString()).isEqualTo("true");
		assertThat(quotedBoolean.asBoolean()).isFalse();
		assertThat(book.property("authors").asSequence().get(0).asString()).isEmpty();
	}

	static Stream<Arguments> values() throws Exception {
		Mogram book = TestResources.model("model/sequences.tara").get("DonQuixote");
		return Stream.of(Arguments.of(book.property("title"), "string"),
			Arguments.of(book.property("numbers").asSequence().get(0), "number"),
			Arguments.of(book.property("available"), "boolean"),
			Arguments.of(book.property("authors").asSequence().get(0), "reference"),
			Arguments.of(book.property("mixed"), "sequence"),
			Arguments.of(book.property("missing"), "empty"));
	}
}

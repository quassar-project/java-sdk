package tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PropertySyntaxTest {
	@ParameterizedTest
	@ValueSource(strings = {"Named", "Body"})
	void readsNamedPropertiesFromTypeFacetsOrBody(String identifier) throws Exception {
		Model model = TestResources.model("model/property-syntax.tara");
		Mogram book = model.get(identifier);
		assertThat(book.facets()).containsExactly("Printed", "Edited");
		assertThat(book.property("title").asString()).isEqualTo("Atlas");
		assertThat(book.property("year").asNumber()).isEqualTo(new BigDecimal("2024"));
		assertThat(book.property("isbn").asString()).isEqualTo("9780000000001");
		assertThat(book.property("available").asBoolean()).isTrue();
		assertThat(model.get(book.property("editor"))).isSameAs(model.get("Lucia"));
	}

	@Test
	void preservesPositionalValuesFromTypeAndFacetsInOrder() throws Exception {
		Model model = TestResources.model("model/property-syntax.tara");
		Mogram book = model.get("Positional");
		assertThat(book.properties()).extracting(Property::name).containsOnlyNulls();
		assertThat(book.properties()).extracting(Property::value).containsExactly(
			new Value.Text("Atlas"), new Value.Number(new BigDecimal("2024")),
			new Value.Text("9780000000001"), new Value.Bool(true), new Value.Reference("Lucia"));
		assertThat(book.facets()).containsExactly("Printed", "Edited");
	}

	@Test
	void namedAndBodyFormsProduceTheSameProperties() throws Exception {
		Model model = TestResources.model("model/property-syntax.tara");
		assertThat(model.get("Named").properties()).isEqualTo(model.get("Body").properties());
		assertThat(model.get("Named").facets()).isEqualTo(model.get("Body").facets());
	}
}

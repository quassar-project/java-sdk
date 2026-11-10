package tests;

import tests.fixtures.MetamodelFixtures;
import io.quassar.metamodel.domain.MetamodelResolver;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyType.Kind;
import io.quassar.metamodel.model.TypeDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetamodelTest {
	@Test
	void retainsFacetDeclarationsSeparatelyFromTypeProperties() throws Exception {
		Metamodel metamodel = MetamodelFixtures.library();
		MetamodelType book = type(metamodel, "Book");
		MetamodelType printed = type(metamodel, "Printed");
		assertThat(book.isFacet()).isFalse();
		assertThat(printed.isFacet()).isTrue();
		assertThat(book.properties()).extracting(property -> property.name()).contains("title", "unseen").doesNotContain("isbn", "pages");
		assertThat(printed.properties()).extracting(property -> property.name()).containsExactly("isbn", "pages");
	}

	@Test
	void resolvesNestedFacetsSeparatelyFromTypeProperties() {
		List<TypeDefinition> types = new MetamodelResolver().resolve(MetamodelFixtures.library());
		TypeDefinition book = types.stream().filter(type -> type.type().equals("Book")).findFirst().orElseThrow();
		assertThat(book.properties()).extracting(property -> property.name())
			.contains("title", "unseen").doesNotContain("isbn", "format", "pages", "catalogueId");
		assertThat(book.facets()).contains("Printed", "Catalogued", "Digital");
		assertThat(types).extracting(TypeDefinition::type).doesNotContain("Printed", "Digital", "Translated", "Catalogued");
		TypeDefinition author = types.stream().filter(type -> type.type().equals("Author")).findFirst().orElseThrow();
		assertThat(author.properties()).extracting(property -> property.name()).doesNotContain("isbn", "pages");
	}

	@Test
	void readsDeclaredSequencesAndReferenceTargets() throws Exception {
		Metamodel metamodel = MetamodelFixtures.library();
		var authors = type(metamodel, "Book").properties().stream().filter(property -> "authors".equals(property.name())).findFirst().orElseThrow();
		assertThat(authors.type().kind()).isEqualTo(Kind.SEQUENCE);
		assertThat(authors.type().element()).isEqualTo(Kind.REFERENCE);
		assertThat(authors.referenceTypes()).containsExactly("Author");
		var translator = type(metamodel, "Translated").properties().stream().filter(property -> "translator".equals(property.name())).findFirst().orElseThrow();
		assertThat(translator.type().kind()).isEqualTo(Kind.REFERENCE);
		assertThat(translator.referenceTypes()).containsExactly("Author");
	}

	private MetamodelType type(Metamodel metamodel, String name) {
		return metamodel.types().stream().filter(type -> type.name().equals(name)).findFirst().orElseThrow();
	}
}

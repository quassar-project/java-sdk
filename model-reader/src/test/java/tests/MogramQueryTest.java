package tests;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.MogramQuery;
import io.quassar.reader.model.Value;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class MogramQueryTest {
	@Test
	void firstPreservesSelectionOrderWithoutConsumingTheQuery() throws Exception {
		Model model = TestResources.model("model/critical-edition.tara");
		MogramQuery books = model.select().type("Book");
		assertThat(books.first()).isSameAs(model.get("Atlas"));
		assertThat(books.count()).isEqualTo(2);
		assertThat(books.first()).isSameAs(model.get("Atlas"));
	}

	@Test
	void firstOfAnEmptyQuerySupportsNullMogramChaining() throws Exception {
		Model model = TestResources.model("library.tara");
		Mogram first = model.select().type("Unknown").first();
		assertThat(first).isSameAs(Mogram.NULL);
		assertThat(first.isNull()).isTrue();
		assertThat(first.property("author")).isInstanceOf(Value.Empty.class);
		assertThat(model.get(first.property("author"))).isSameAs(Mogram.NULL);
		assertThat(model.parentOf(first)).isSameAs(Mogram.NULL);
		assertThat(model.select(first).list()).isEmpty();
	}

	@Test
	void supportsStreamPipelinesWithoutConsumingTheQuery() throws Exception {
		Model model = TestResources.model("model/critical-edition.tara");
		MogramQuery books = model.select().type("Book");
		assertThat(books.stream()
			.filter(book -> book.facets().contains("Printed"))
			.map(Mogram::identifier)
			.toList()).containsExactly("Atlas");
		assertThat(books.stream().map(Mogram::identifier).toList())
			.containsExactly("Atlas", "Nocturne");
		assertThat(books.count()).isEqualTo(2);
	}

	@Test
	void branchesQueriesWithoutChangingThem() throws Exception {
		Model model = TestResources.model("library.tara");
		MogramQuery descendants = model.select(model.get("DonQuixote")).descendants();
		List<Mogram> original = descendants.list();
		MogramQuery facets = descendants.type("Part");
		MogramQuery attributes = descendants.type("Chapter");
		assertThat(facets.count()).isEqualTo(1);
		assertThat(attributes.count()).isEqualTo(3);
		assertThat(descendants.list()).isEqualTo(original);
		assertThat(facets.facet("Illustrated").identifier("PartOne").count()).isEqualTo(1);
		TestResources.immutable(() -> descendants.list().clear());
	}

	@Test
	void navigatesWithoutRepeatingTheSameMogram() throws Exception {
		Model model = TestResources.model("library.tara");
		MogramQuery parents = model.select(model.get("DonQuixote")).children().parents();
		assertThat(parents.count()).isEqualTo(1);
		assertThat(parents.first()).isSameAs(model.get("DonQuixote"));
		List<Mogram> descendants = model.select(model.get("DonQuixote")).descendants().list();
		assertThat(descendants.get(0).identifier()).isEqualTo("FrontMatter");
		assertThat(descendants.get(1).identifier()).isEqualTo("PartOne");
		assertThat(descendants.get(2).identifier()).isEqualTo("Windmills");
		assertThat(descendants.get(3).identifier()).isEqualTo("Glossary");
	}

	@Test
	void preservesAnonymousMogramsFromDifferentParents() throws Exception {
		Model model = TestResources.model("model/anonymous-mograms.tara");
		List<Mogram> notes = model.roots().children().list();
		assertThat(notes).hasSize(2);
		assertThat(notes.get(0).identifier()).isEmpty();
		assertThat(notes.get(0)).isEqualTo(notes.get(1));
		assertThat(notes.get(0)).isNotSameAs(notes.get(1));
		assertThat(model.parentOf(notes.get(0)).identifier()).isEqualTo("DonQuixote");
		assertThat(model.parentOf(notes.get(1)).identifier()).isEqualTo("TheTrial");
	}

	@Test
	void filtersEmptyAndPositionalProperties() throws Exception {
		Model model = TestResources.model("library.tara");
		assertThat(model.select().withProperty("isbn", new Value.Empty()).count()).isEqualTo(1);
		assertThat(model.select().withProperty(null, new Value.Text("Subjects")).identifier("Subjects").count()).isEqualTo(1);
		assertThat(model.select().withProperty("summary", new Value.Text("Illustrated")).count()).isEqualTo(1);
	}

}

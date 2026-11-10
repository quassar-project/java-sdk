package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public final class CriticalEditionTest {

	private static Model model() throws Exception {
		return TestResources.model("model/critical-edition.tara");
	}

	@Test
	void readsFiveLevelsOfNesting() throws Exception {
		Model model = model();
		Mogram paragraph = model.get("Atlas.Journey.Arrival.Harbor.Opening");
		assertThat(paragraph.type()).isEqualTo("Paragraph");
		assertThat(model.parentOf(paragraph)).isSameAs(model.get("Atlas.Journey.Arrival.Harbor"));
		assertThat(paragraph.property("text"))
			.isEqualTo(new Value.Text("A lighthouse rose above the water, // brighter than the stars > and the city = a promise."));
		assertThat(model.allMograms()).hasSize(17);
	}

	@Test
	void keepsPropertiesFromSeveralFacets() throws Exception {
		Mogram edition = model().get("Atlas.Anniversary");
		assertThat(edition.facets()).containsExactly("Translated", "Limited");
		assertThat(edition.property("language")).isEqualTo(new Value.Text("en"));
		assertThat(edition.property("copies")).isEqualTo(new Value.Number(new BigDecimal("500")));
		assertThat(edition.property("translator")).isEqualTo(new Value.Reference("Mateo"));
		assertThat(((Value.Sequence) edition.property("prices")).values()).containsExactly(
			new Value.Number(new BigDecimal("19.95")),
			new Value.Number(new BigDecimal("24.50")),
			new Value.Number(new BigDecimal("49.00")));
		assertThat(edition.children().get(0).identifier()).isEmpty();
	}

	@Test
	void resolvesForwardAndCircularReferences() throws Exception {
		Model model = model();
		Mogram atlas = model.get("Atlas");
		assertThat(model.get(atlas.property("publisher"))).isSameAs(model.get("NorthPress"));
		Value.Sequence authors = (Value.Sequence) atlas.property("authors");
		assertThat(model.get(authors.values().get(0))).isSameAs(model.get("Lucia"));
		Mogram related = model.get(atlas.property("related"));
		assertThat(related).isSameAs(model.get("Nocturne"));
		assertThat(model.get(related.property("related"))).isSameAs(atlas);
	}

	@Test
	void resolvesReferencesBetweenParts() throws Exception {
		Model model = model();
		Mogram departure = model.get("Atlas.Journey.Departure");
		assertThat(model.get(departure.property("previous"), departure))
			.isSameAs(model.get("Atlas.Journey.Arrival"));
		assertThat(model.get(departure.property("next"), departure))
			.isSameAs(model.get("Atlas.Return.Homecoming"));
		assertThat(model.get(model.get("ReadingList").property("featured")))
			.isSameAs(model.get("Atlas.Anniversary"));
	}

	@Test
	void queriesNestedCitationsAndEditions() throws Exception {
		Model model = model();
		assertThat(model.select().type("Chapter").count()).isEqualTo(4);
		assertThat(model.select().type("Edition").facet("Translated").facet("Limited").withProperty("language", new Value.Text("en")).first())
			.isSameAs(model.get("Atlas.Anniversary"));
		Mogram citation = model.select(model.get("Atlas")).descendants().type("Citation").first();
		assertThat(model.get(citation.property("book"), citation)).isSameAs(model.get("Nocturne"));
		assertThat(model.select(model.get("Atlas")).descendants().count()).isEqualTo(10);
	}

	@Test
	void loadsTheSameLibraryFromFilesAndZip() throws Exception {
		Model folder = Models.load(TestResources.path("critical-library"));
		Model zip = Models.load(TestResources.path("critical-library.zip"));
		assertThat(folder.mograms()).isEqualTo(model().mograms());
		assertThat(zip.mograms()).isEqualTo(folder.mograms());
		assertThat(folder.get(folder.get("Atlas").property("publisher"))).isSameAs(folder.get("NorthPress"));
	}
}

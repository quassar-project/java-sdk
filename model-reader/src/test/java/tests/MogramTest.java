package tests;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Property;
import io.quassar.reader.model.Value;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class MogramTest {
	@Test
	void ownsItsCollections() throws Exception {
		Mogram source = TestResources.model("library.tara").get("DonQuixote");
		List<Property> properties = new ArrayList<>(source.properties());
		List<String> facets = new ArrayList<>(source.facets());
		List<Mogram> children = new ArrayList<>(source.children());
		Mogram copy = new Mogram(source.type(), source.identifier(), properties, facets, children);
		properties.clear();
		facets.add("CollectedStories");
		children.clear();
		assertThat(copy).isEqualTo(source);
		TestResources.immutable(() -> copy.properties().clear());
		TestResources.immutable(() -> copy.facets().clear());
		TestResources.immutable(() -> copy.children().clear());
	}

	@Test
	void keepsOnePropertyWithSeveralValues() throws Exception {
		Mogram collection = TestResources.model("library.tara").get("Recommendations");
		Value.Sequence objectives = (Value.Sequence) collection.property("selections");
		assertThat(collection.properties()).hasSize(1);
		assertThat(objectives.values()).containsExactly(
			new Value.Reference("Classics.Introduction"),
			new Value.Reference("Classics"));
		Mogram unnamed = TestResources.model("library.tara").get("Subjects").children().get(0);
		assertThat(unnamed.identifier()).isEqualTo("");
	}

	@Test
	void keepsFacetPropertiesOnTheMogram() throws Exception {
		Model model = TestResources.model("facet-properties.tara");
		Mogram mogram = model.get("PocketEdition");
		assertThat(mogram.facets()).containsExactly("Printed", "Tagged");
		assertThat(mogram.properties()).hasSize(4);
		assertThat(mogram.property("active")).isEqualTo(new Value.Bool(true));
		assertThat(mogram.property("title")).isEqualTo(new Value.Text("Title"));
	}

	@Test
	void copiesSequences() {
		List<Value> values = new ArrayList<>(List.of(new Value.Text("first"), new Value.Text("second")));
		Value.Sequence sequence = new Value.Sequence(values);
		values.clear();
		assertThat(sequence.values()).hasSize(2);
		TestResources.immutable(() -> sequence.values().clear());
	}
}

package tests;

import org.junit.jupiter.api.Test;

import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class ModelTest {
	@Test
	void resolvesTheNearestScope() throws Exception {
		Model model = TestResources.model("model/scopes.tara");
		Value.Reference reference = new Value.Reference("Editor");
		Mogram link = model.get("Anthology.VolumeOne.Preface");
		assertThat(model.get(reference, link)).isSameAs(model.get("Anthology.VolumeOne.Editor"));
		assertThat(model.get(reference, model.get("Anthology"))).isSameAs(model.get("Anthology.Editor"));
		assertThat(model.get(reference)).isSameAs(model.get("Editor"));
		assertThat(model.get("Anthology.VolumeOne.Editor").identifier()).isEqualTo("Editor");
	}

	@Test
	void resolvesPathsThroughUnnamedMograms() throws Exception {
		Model model = TestResources.model("model/scopes.tara");
		Mogram root = model.get("CollectedWorks");
		Mogram unnamed = root.children().get(0);
		Mogram leaf = model.get("CollectedWorks.Appendix");
		assertThat(unnamed.identifier()).isEmpty();
		assertThat(model.parentOf(leaf)).isSameAs(unnamed);
		assertThat(model.parentOf(unnamed)).isSameAs(root);
		assertThat(model.parentOf(root)).isSameAs(Mogram.NULL);
	}

	@Test
	void resolvesCircularReferences() throws Exception {
		Model model = TestResources.model("model/scopes.tara");
		Mogram a = model.get("FirstEdition");
		Mogram b = model.get(a.property("relatedEdition"), a);
		assertThat(b).isSameAs(model.get("SecondEdition"));
		assertThat(model.get(b.property("relatedEdition"), b)).isSameAs(a);
	}

	@Test
	void keepsRootsAndTraversalOrder() throws Exception {
		Model source = TestResources.model("library.tara");
		List<Mogram> roots = new ArrayList<>(source.mograms());
		Model model = new Model(roots);
		roots.clear();
		Mogram book = model.get("DonQuixote");
		assertThat(model.mograms()).hasSize(4);
		assertThat(model.allMograms().get(0)).isSameAs(book);
		assertThat(model.allMograms().get(1)).isSameAs(book.children().get(0));
		assertThat(model.allMograms().get(2)).isSameAs(book.children().get(1));
		TestResources.immutable(() -> model.mograms().clear());
		TestResources.immutable(() -> model.allMograms().clear());
	}
}

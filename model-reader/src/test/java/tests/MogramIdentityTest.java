package tests;

import io.quassar.reader.Models;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MogramIdentityTest {
	@Test
	void assignsARepeatableAlphanumericIdToAnonymousMograms() throws Exception {
		Mogram first = anonymous("base.tara");
		Mogram second = anonymous("base.tara");
		assertThat(first.identifier()).matches("Step-[a-z0-9]{12,}");
		assertThat(second.identifier()).isEqualTo(first.identifier());
		assertThat(Models.create("Step(label=\"Validate\")").mograms().get(0).identifier()).containsPattern("[0-9]");
	}

	@Test
	void ignoresFormattingCommentsAndNamedPropertyOrder() throws Exception {
		assertThat(anonymous("equivalent.tara").identifier()).isEqualTo(anonymous("base.tara").identifier());
	}

	@ParameterizedTest
	@MethodSource("changes")
	void changesTheIdWhenContentOrCompositionChanges(Change change) throws Exception {
		String source = TestResources.text("identity/base.tara");
		var changed = Models.create(source.replace(change.before(), change.after()));
		assertThat(changed.get("Pipeline").children().get(0).identifier()).isNotEqualTo(anonymous("base.tara").identifier());
		assertThat(changed.get("Pipeline").identifier()).isEqualTo("Pipeline");
	}

	@Test
	void preservesExplicitIdsAndExistingPathsThroughAnonymousNodes() throws Exception {
		var model = TestResources.model("identity/base.tara");
		Mogram step = model.get("Pipeline").children().get(0);
		Mogram action = model.get("Pipeline.Primary");
		assertThat(action.identifier()).isEqualTo("Primary");
		assertThat(model.parentOf(action)).isSameAs(step);
		assertThat(model.get(step.identifier())).isSameAs(step);
		assertThat(model.get(new Value.Reference(step.identifier()), action)).isSameAs(step);
		assertThat(Mogram.NULL.identifier()).isEmpty();
	}

	@Test
	void keepsEveryOccurrenceOfIdenticalAnonymousMograms() throws Exception {
		var model = TestResources.model("identity/duplicates.tara");
		assertThat(model.mograms()).hasSize(2);
		assertThat(model.mograms().get(1).identifier()).isNotEqualTo(model.mograms().get(0).identifier());
		assertThat(model.find(model.mograms().get(0).identifier())).containsExactly(model.mograms().get(0));
		assertThat(TestResources.model("identity/duplicates.tara").mograms()).isEqualTo(model.mograms());
	}

	@Test
	void includesTheContainingDeclarationInTheSeed() throws Exception {
		String source = TestResources.text("identity/base.tara").replace("Pipeline", "Other");
		assertThat(Models.create(source).get("Other").children().get(0).identifier()).isNotEqualTo(anonymous("base.tara").identifier());
	}

	@Test
	void keepsIdsWhenAnUnrelatedSiblingIsInserted() throws Exception {
		String source = TestResources.text("identity/duplicates.tara");
		var original = Models.create(source);
		var expanded = Models.create("Step(label=\"Different\")\n" + source);
		assertThat(expanded.mograms().get(1).identifier()).isEqualTo(original.mograms().get(0).identifier());
		assertThat(expanded.mograms().get(2).identifier()).isEqualTo(original.mograms().get(1).identifier());
	}

	@Test
	void distinguishesIdenticalNodesThroughoutTheContainmentTree() throws Exception {
		var model = TestResources.model("identity/nested-duplicates.tara");
		var anonymous = model.allMograms().stream().filter(Mogram::anonymous).toList();
		assertThat(anonymous).hasSize(6);
		assertThat(anonymous).extracting(Mogram::identifier).doesNotHaveDuplicates();
		assertThat(TestResources.model("identity/nested-duplicates.tara").mograms()).isEqualTo(model.mograms());
		for (Mogram mogram : anonymous) {
			assertThat(model.get(mogram.identifier())).isSameAs(mogram);
			assertThat(model.get(new Value.Reference(mogram.identifier()), model.get("Other"))).isSameAs(mogram);
		}
	}

	@ParameterizedTest
	@MethodSource("valueChanges")
	void includesEveryValueKindInTheFingerprint(Change change) throws Exception {
		String source = TestResources.text("identity/values.tara");
		String original = Models.create(source).mograms().get(0).identifier();
		String changed = Models.create(source.replace(change.before(), change.after())).mograms().get(0).identifier();
		assertThat(changed).isNotEqualTo(original);
	}

	private static Stream<Change> valueChanges() {
		return Stream.of(new Change("Confirmación", "Notificación"), new Change("enabled = true", "enabled = false"),
			new Change("weight = 1.5", "weight = 2"), new Change("action = External", "action = Another"),
			new Change("\"one\" 2 false External empty", "2 \"one\" false External empty"),
			new Change("amount > 0", "amount > 1"), new Change("emptyValue = empty", "emptyValue = \"\""));
	}

	@Test
	void avoidsCollisionsWithExplicitIdentifiers() throws Exception {
		String source = TestResources.text("identity/duplicates.tara");
		String candidate = Models.create(source).mograms().get(0).identifier();
		var model = Models.create("Step " + candidate + "\n" + source);
		assertThat(model.mograms().get(0).identifier()).isEqualTo(candidate);
		assertThat(model.mograms()).extracting(Mogram::identifier).doesNotHaveDuplicates();
		assertThat(model.mograms().get(1).identifier()).startsWith(candidate).matches("Step-[a-z0-9]{13,}");
		assertThat(Models.create("Step " + candidate + "\n" + source).mograms()).isEqualTo(model.mograms());
		var explicitLast = Models.create(source + "\nStep " + candidate + "\n");
		assertThat(explicitLast.mograms().subList(0, 2)).isEqualTo(model.mograms().subList(1, 3));
	}

	private Mogram anonymous(String file) throws Exception {
		return TestResources.model("identity/" + file).get("Pipeline").children().get(0);
	}

	private static Stream<Change> changes() {
		return Stream.of(new Change("Step is", "Task is"), new Change("Enabled", "Disabled"),
			new Change("Validate", "Validate again"), new Change("retries = 2", "retries = 3"),
			new Change("Call service", "Call another service"), new Change("Primary", "Secondary"),
			new Change("description = \"Call service\"", "description = \"Call service\"\n\t\tAction(\"notify\")"));
	}

	private record Change(String before, String after) {
	}
}

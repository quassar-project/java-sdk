package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.ModelRelease;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EffectiveValueResolverTest {
    @TempDir Path folder;
    private static final String META = "dsl metta\nConcept Settings\n\tdef integer attempts = 3\n";

    @Test void retainsOriginAndDoesNotMutateTheInput() throws Exception {
        var resolver = new EffectiveValueResolver(META);
        var input = Models.create("dsl settings\nSettings one\nSettings two\n\tattempts = 3\n");
        var effective = resolver.resolve(input);
        assertTrue(input.get("one").property("attempts").isEmpty());
        assertEquals(Property.Origin.DEFAULT, effective.get("one").properties().get(0).origin());
        assertEquals(Property.Origin.EXPLICIT, effective.get("two").properties().get(0).origin());
        assertEquals(effective.mograms(), resolver.resolve(effective).mograms());
    }

    @Test void allEntryPointsUseTheSameResolution() throws Exception {
        Path file = Files.writeString(folder.resolve("main.tara"), "dsl settings\nSettings one\n");
        var expected = Models.create(Files.readString(file), META);
        assertEquals(expected.mograms(), Models.load(file, META).mograms());
        assertEquals(expected.mograms(), Models.load(folder, META).mograms());
        assertEquals(expected.mograms(), Models.load(file.toUri(), META).mograms());
        Path resources = Files.createDirectories(folder.resolve("resources"));
        Files.writeString(resources.resolve("logo.svg"), "<svg/>");
        Files.writeString(folder.resolve("manifest.json"), "{}");
        var release = ModelRelease.open(folder).withDefaults(new EffectiveValueResolver(META));
        assertEquals(expected.mograms(), release.model().mograms());
        assertEquals("{}", release.metadata());
        try (var input = release.resource("logo.svg")) { assertEquals("<svg/>", new String(input.readAllBytes())); }
    }

    @Test void appliedFacetDefaultsAreUsedButUnappliedOnesAreNot() throws Exception {
        String meta = "dsl metta\nConcept Book\n\tfacet Printed\n\t\tdef integer pages = 100\n";
        var model = Models.create("dsl library\nBook first is Printed\nBook second\n", meta);
        assertEquals(new Value.Number(new BigDecimal("100")), model.get("first").property("pages"));
        assertTrue(model.get("second").property("pages").isEmpty());
    }

    @Test void explicitFacetValueOverridesTheDefault() throws Exception {
        String meta = "dsl metta\nConcept Book\n\tfacet Printed\n\t\tdef integer pages = 100\n";
        var model = Models.create("dsl library\nBook first is Printed(pages = 250)\n", meta);
        assertEquals(new Value.Number(new BigDecimal("250")), model.get("first").property("pages"));
    }

    @Test void defaultReferenceResolvesAgainstModelInstances() throws Exception {
        String meta = "dsl metta\nConcept Person\nConcept Task\n\tdef Person owner = john\n";
        var model = Models.create("dsl jobs\nPerson john\nTask first\n", meta);
        assertSame(model.get("john"), model.get(model.get("first").property("owner"), model.get("first")));
    }

    @Test void boundedListsKeepTheirSequenceShape() throws Exception {
        var model = DefaultsContract.settings("\tdef integer[0..3] attempts = 1\n", "");
        assertEquals(new Value.Sequence(List.of(new Value.Number(BigDecimal.ONE))), model.get("first").property("attempts"));
    }

    @Test void childDeclarationWithoutDefaultShadowsParentDefault() throws Exception {
        String meta = "dsl metta\nConcept Base\n\tdef string title = \"base\"\nConcept Child extends Base\n\tdef string[0..1] title\n";
        assertTrue(Models.create("dsl test\nChild one\n", meta).get("one").property("title").isEmpty());
    }

    @Test void cyclesAndUnknownParentsFailInformatively() {
        var cycle = assertThrows(IllegalArgumentException.class, () -> new EffectiveValueResolver("dsl metta\nConcept A extends B\nConcept B extends A\n"));
        assertTrue(cycle.getMessage().contains("cycle"));
        var unknown = assertThrows(IllegalArgumentException.class, () -> new EffectiveValueResolver("dsl metta\nConcept A extends Missing\n"));
        assertTrue(unknown.getMessage().contains("Missing"));
    }

    @Test void unknownModelTypesDoNotReceiveUnrelatedDefaults() {
        var error = assertThrows(IllegalArgumentException.class, () -> Models.create("dsl settings\nUnknown one\n", META));
        assertTrue(error.getMessage().contains("Unknown"));
    }
}

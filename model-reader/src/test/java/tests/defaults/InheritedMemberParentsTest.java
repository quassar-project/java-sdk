package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InheritedMemberParentsTest {
    private static final String META = """
            dsl metta
            Concept Base as generalization
                facet Config
                    def word[]:{Open Closed} states = Open Closed
            Concept Child extends Base
                facet Custom extends Config
            """;
    @Test void facetCanExtendAnInheritedFacet() throws Exception {
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Open"), new Value.Reference("Closed"))),
                Models.create("dsl test\nChild first is Custom\n", META).get("first").property("states"));
    }
    @Test void derivedFacetOverridesInheritedList() throws Exception {
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Closed"))),
                Models.create("dsl test\nChild first is Custom\n", META + "        def word[]:{Open Closed} states = Closed\n").get("first").property("states"));
    }
    @Test void explicitEmptyOverridesDefaultsThroughBothInheritanceChains() throws Exception {
        assertEquals(new Value.Sequence(List.of()), Models.create("dsl test\nChild first is Custom\n\tstates = empty\n", META).get("first").property("states"));
    }
    @Test void inheritedParentBeatsAnUnrelatedGlobalType() throws Exception {
        String meta = "dsl metta\nConcept Config\n\tdef string label = \"wrong\"\nConcept Base\n\tfacet Config\n\t\tdef string label = \"right\"\nConcept Child extends Base\n\tfacet Custom extends Config\n";
        assertEquals(new Value.Text("right"), Models.create("dsl test\nChild first is Custom\n", meta).get("first").property("label"));
    }
    @Test void parentResolutionIsIndependentOfDeclarationOrder() throws Exception {
        String meta = "dsl metta\nConcept Child extends Base\n\tfacet Custom extends Config\nConcept Base\n\tfacet Config\n\t\tdef string label = \"right\"\n";
        assertEquals(new Value.Text("right"), Models.create("dsl test\nChild first is Custom\n", meta).get("first").property("label"));
    }
    @Test void cyclesStillFailWithoutHangingDuringScopedParentLookup() {
        assertThrows(IllegalArgumentException.class, () -> new EffectiveValueResolver("dsl metta\nConcept A extends B\n\tfacet Custom extends Missing\nConcept B extends A\n"));
    }
}

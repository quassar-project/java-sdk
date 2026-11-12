package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NestedFacetListsTest {
    private static final String META = """
            dsl metta
            Concept Base as generalization
                facet Outer
                    def string[] labels = "outer"
                    facet Inner
                        def word[]:{Open Closed} states = Open Closed
                        Concept Target
                        def Target[] targets = first second
            Concept Group extends Base
            """;
    @Test void qualifiedNestedFacetUsesItsOwnDefaults() throws Exception {
        var group = Models.create("dsl nested\nGroup group is Outer.Inner\n", META).get("group");
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Open"), new Value.Reference("Closed"))), group.property("states"));
        assertTrue(group.property("labels").isEmpty());
    }
    @Test void parentAndNestedFacetContributeIndependentLists() throws Exception {
        var group = Models.create("dsl nested\nGroup group is Outer Outer.Inner\n", META).get("group");
        assertEquals(new Value.Sequence(List.of(new Value.Text("outer"))), group.property("labels"));
        assertEquals(2, group.property("states").asSequence().size());
    }
    @Test void explicitEmptyNestedListDoesNotEmptyParentList() throws Exception {
        var group = Models.create("dsl nested\nGroup group is Outer Outer.Inner\n\tstates = empty\n", META).get("group");
        assertEquals(new Value.Sequence(List.of()), group.property("states"));
        assertEquals(1, group.property("labels").asSequence().size());
    }
    @Test void nestedFacetReferenceDefaultsResolveItsComponents() throws Exception {
        var model = Models.create("dsl nested\nGroup group is Outer.Inner\n\tTarget first\n\tTarget second\n", META);
        var group = model.get("group");
        assertSame(model.get("group.first"), model.get(group.property("targets").asSequence().get(0), group));
        assertSame(model.get("group.second"), model.get(group.property("targets").asSequence().get(1), group));
    }
    @Test void nestedFacetListOverrideDoesNotAffectAnotherInstance() throws Exception {
        var model = Models.create("dsl nested\nGroup first is Outer.Inner(states = Closed)\nGroup second is Outer.Inner\n", META);
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Closed"))), model.get("first").property("states"));
        assertEquals(2, model.get("second").property("states").asSequence().size());
    }
    @Test void facetOnAComponentInsideAnotherFacetUsesTheComponentScope() throws Exception {
        String meta = "dsl metta\nConcept Group\n\tfacet Outer\n\t\tConcept Part\n\t\t\tfacet Inner\n\t\t\t\tdef string[] labels = \"part\"\n";
        var model = Models.create("dsl nested\nGroup group is Outer\n\tPart part is Inner\n", meta);
        assertEquals(new Value.Sequence(List.of(new Value.Text("part"))), model.get("group.part").property("labels"));
        assertTrue(model.get("group").property("labels").isEmpty());
    }
}

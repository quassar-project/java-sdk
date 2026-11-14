package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InheritedReferenceListsTest {
    private static final String META = """
            dsl metta
            Concept Base as generalization
                facet Linked
                    Concept Target
                    def Target[] targets = first second
            Concept Group extends Base
            """;
    private static final String BRANCH = "\n\tTarget first\n\tTarget second\n";

    @Test void inheritedListReferencesResolveInEachInstance() throws Exception {
        var model = Models.create("dsl links\nGroup left is Linked" + BRANCH + "Group right is Linked" + BRANCH, META);
        for (var group : model.mograms()) {
            var values = group.property("targets").asSequence();
            assertEquals(2, values.size());
            assertSame(group.children().get(0), model.get(values.get(0), group));
            assertSame(group.children().get(1), model.get(values.get(1), group));
        }
    }
    @Test void explicitListReplacesInheritedReferencesInOrder() throws Exception {
        var model = Models.create("dsl links\nGroup group is Linked" + BRANCH + "\ttargets = second first second\n", META);
        var group = model.get("group");
        assertEquals(List.of(new Value.Reference("second"), new Value.Reference("first"), new Value.Reference("second")), group.property("targets").asSequence());
        assertSame(group.children().get(1), model.get(group.property("targets").asSequence().get(2), group));
    }
    @Test void emptyListDoesNotModifyDefaultsInAnotherInstance() throws Exception {
        var model = Models.create("dsl links\nGroup left is Linked" + BRANCH + "\ttargets = empty\nGroup right is Linked" + BRANCH, META);
        assertEquals(new Value.Sequence(List.of()), model.get("left").property("targets"));
        assertEquals(2, model.get("right").property("targets").asSequence().size());
    }
    @Test void explicitQualifiedReferenceListCanCrossBranches() throws Exception {
        var model = Models.create("dsl links\nGroup left is Linked" + BRANCH + "Group right is Linked" + BRANCH + "\ttargets = left.second right.first\n", META);
        var group = model.get("right");
        var values = group.property("targets").asSequence();
        assertSame(model.get("left.second"), model.get(values.get(0), group));
        assertSame(model.get("right.first"), model.get(values.get(1), group));
    }
    @Test void resolvingTwiceKeepsReferenceOrderAndOrigins() throws Exception {
        var resolver = new EffectiveValueResolver(META);
        var once = resolver.resolve(Models.create("dsl links\nGroup first is Linked" + BRANCH));
        assertEquals(once.mograms(), resolver.resolve(once).mograms());
        assertEquals(Property.Origin.DEFAULT, once.get("first").properties().get(0).origin());
    }
    @Test void missingReferenceDoesNotResolveToAnotherBranch() throws Exception {
        var model = Models.create("dsl links\nGroup left is Linked" + BRANCH + "Group right is Linked\n\tTarget second\n", META);
        var right = model.get("right");
        assertTrue(model.get(right.property("targets").asSequence().get(0), right).isNull());
    }
    @Test void referenceListsRemainImmutable() throws Exception {
        var model = Models.create("dsl links\nGroup group is Linked" + BRANCH, META);
        assertThrows(UnsupportedOperationException.class, () -> model.get("group").property("targets").asSequence().clear());
    }
}

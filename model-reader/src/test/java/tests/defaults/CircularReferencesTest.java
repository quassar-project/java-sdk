package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class CircularReferencesTest {
    @Test void explicitSelfReferenceReturnsTheSameInstance() throws Exception {
        var model = Models.create("Node first\n\tnext = first\n");
        var node = model.get("first");
        assertSame(node, model.get(node.property("next"), node));
        assertEquals(0, model.select(node).descendants().count());
    }
    @Test void twoNodesCanReferToEachOtherWithoutDuplicatingInstances() throws Exception {
        var model = Models.create("Node first\n\tnext = second\nNode second\n\tnext = first\n");
        var first = model.get("first");
        var second = model.get(first.property("next"), first);
        assertSame(model.get("second"), second);
        assertSame(first, model.get(second.property("next"), second));
        assertEquals(2, model.allMograms().size());
    }
    @Test void inheritedDefaultListsCanContainSelfAndMutualReferences() throws Exception {
        String meta = "dsl metta\nConcept Base as generalization\n\tdef Node[] next = first second\nConcept Node extends Base\n";
        var model = Models.create("dsl graph\nNode first\nNode second\n", meta);
        for (var node : model.mograms()) {
            var refs = node.property("next").asSequence();
            assertSame(model.get("first"), model.get(refs.get(0), node));
            assertSame(model.get("second"), model.get(refs.get(1), node));
        }
        assertEquals(2, model.allMograms().size());
    }
    @Test void emptyOverrideBreaksOnlyOneSideOfACycle() throws Exception {
        String meta = "dsl metta\nConcept Node\n\tdef Node[] next = first second\n";
        var model = Models.create("dsl graph\nNode first\n\tnext = empty\nNode second\n", meta);
        assertEquals(new Value.Sequence(List.of()), model.get("first").property("next"));
        assertEquals(2, model.get("second").property("next").asSequence().size());
    }
    @Test void cyclicFacetReferencesSurviveRepeatedDefaultResolution() throws Exception {
        String meta = "dsl metta\nConcept Node\n\tfacet Linked\n\t\tdef Node[] next = first second\n";
        var resolver = new EffectiveValueResolver(meta);
        var once = resolver.resolve(Models.create("dsl graph\nNode first is Linked\nNode second is Linked\n"));
        var twice = resolver.resolve(once);
        assertEquals(once.mograms(), twice.mograms());
        assertSame(twice.get("first"), twice.get(twice.get("second").property("next").asSequence().get(0), twice.get("second")));
    }
    @Test void circularInheritanceIsRejectedButCircularValuesAreAllowed() {
        var error = assertThrows(IllegalArgumentException.class, () -> new EffectiveValueResolver("dsl metta\nConcept Node\n\tfacet First extends Second\n\tfacet Second extends First\n"));
        assertTrue(error.getMessage().contains("cycle"));
    }
}

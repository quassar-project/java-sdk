package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceBranchIsolationTest {
    @Test void anonymousSiblingBranchesResolveTheirOwnTargets() throws Exception {
        var model = Models.create("Group\n\tTarget local\n\tLink link\n\t\ttarget = local\nGroup\n\tTarget local\n\tLink link\n\t\ttarget = local\n");
        for (var group : model.mograms()) {
            var target = group.children().get(0);
            var link = group.children().get(1);
            assertSame(target, model.get(link.property("target"), link));
        }
    }
    @Test void namedParentDoesNotMergeItsAnonymousChildrenScopes() throws Exception {
        var model = Models.create("Root root\n\tGroup\n\t\tTarget local\n\t\tLink link\n\t\t\ttarget = local\n\tGroup\n\t\tTarget local\n\t\tLink link\n\t\t\ttarget = local\n");
        for (var group : model.get("root").children()) {
            var link = group.children().get(1);
            assertSame(group.children().get(0), model.get(link.property("target"), link));
        }
    }
    @Test void unresolvedLocalReferenceDoesNotLeakIntoAnotherAnonymousRoot() throws Exception {
        var model = Models.create("Group\n\tTarget local\nGroup\n\tLink link\n\t\ttarget = local\n");
        var link = model.mograms().get(1).children().get(0);
        assertTrue(model.get(link.property("target"), link).isNull());
    }
    @Test void defaultReferencesResolveSeparatelyInsideRepeatedFacetBranches() throws Exception {
        String meta = "dsl metta\nConcept Group\n\tfacet Linked\n\t\tConcept Target\n\t\tConcept Link\n\t\t\tdef Target target = local\n";
        var model = Models.create("dsl links\nGroup first is Linked\n\tTarget local\n\tLink link\nGroup second is Linked\n\tTarget local\n\tLink link\n", meta);
        for (var group : model.mograms()) {
            var link = group.children().get(1);
            assertSame(group.children().get(0), model.get(link.property("target"), link));
        }
    }
    @Test void explicitQualifiedReferenceCanCrossNamedBranches() throws Exception {
        var model = Models.create("Group first\n\tTarget local\nGroup second\n\tTarget local\n\tLink link\n\t\ttarget = first.local\n");
        var link = model.get("second.link");
        assertSame(model.get("first.local"), model.get(link.property("target"), link));
    }
    @Test void localReferenceWinsOverGlobalHomonym() throws Exception {
        var model = Models.create("Target local\nGroup group\n\tTarget local\n\tLink link\n\t\ttarget = local\n");
        var link = model.get("group.link");
        assertSame(model.get("group.local"), model.get(link.property("target"), link));
    }
    @Test void explicitEmptyReferenceDoesNotRestoreFacetDefault() throws Exception {
        String meta = "dsl metta\nConcept Group\n\tfacet Linked\n\t\tdef Target target = local\nConcept Target\n";
        var model = Models.create("dsl links\nTarget local\nGroup first is Linked\n\ttarget = empty\n", meta);
        assertTrue(model.get("first").property("target").isEmpty());
        assertTrue(model.get(model.get("first").property("target"), model.get("first")).isNull());
    }
}

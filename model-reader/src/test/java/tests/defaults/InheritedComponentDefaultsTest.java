package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InheritedComponentDefaultsTest {
    @Test void nestedComponentInheritedFromAncestorRetainsItsDefaults() throws Exception {
        String meta = "dsl metta\nConcept Base as generalization\n\tConcept Item\n\t\tdef integer stock = 5\nConcept Warehouse extends Base\n";
        assertEquals(number(5), Models.create("dsl inventory\nWarehouse central\n\tItem keyboard\n", meta).get("central.keyboard").property("stock"));
    }
    @Test void inheritedComponentDefaultsCanBeExplicitlyOverridden() throws Exception {
        String meta = "dsl metta\nConcept Base as generalization\n\tConcept Item\n\t\tdef integer stock = 5\nConcept Warehouse extends Base\n";
        assertEquals(number(0), Models.create("dsl inventory\nWarehouse central\n\tItem keyboard\n\t\tstock = 0\n", meta).get("central.keyboard").property("stock"));
    }
    @Test void componentDeclaredByAnAppliedFacetReceivesDefaults() throws Exception {
        String meta = "dsl metta\nConcept Base\n\tfacet Digital\n\t\tConcept Output\n\t\t\tdef integer size = 10\nConcept Job extends Base\n";
        assertEquals(number(10), Models.create("dsl jobs\nJob first is Digital\n\tOutput result\n", meta).get("first.result").property("size"));
    }
    @Test void sameComponentNameInDifferentAppliedFacetsUsesItsOwnContext() throws Exception {
        String meta = "dsl metta\nConcept Job\n\tfacet Image\n\t\tConcept Output\n\t\t\tdef integer size = 10\n\tfacet Audio\n\t\tConcept Output\n\t\t\tdef integer size = 20\n";
        var model = Models.create("dsl jobs\nJob image is Image\n\tOutput result\nJob audio is Audio\n\tOutput result\n", meta);
        assertEquals(number(10), model.get("image.result").property("size"));
        assertEquals(number(20), model.get("audio.result").property("size"));
    }
    @Test void inheritedComponentWinsOverUnrelatedGlobalType() throws Exception {
        String meta = "dsl metta\nConcept Item\n\tdef integer stock = 99\nConcept Base\n\tConcept Item\n\t\tdef integer stock = 5\nConcept Warehouse extends Base\n";
        assertEquals(number(5), Models.create("dsl inventory\nWarehouse central\n\tItem keyboard\n", meta).get("central.keyboard").property("stock"));
    }
    @Test void unknownComponentDoesNotBorrowDefaultsFromUnappliedFacet() {
        String meta = "dsl metta\nConcept Job\n\tfacet Image\n\t\tConcept Output\n\t\t\tdef integer size = 10\n";
        assertThrows(IllegalArgumentException.class, () -> Models.create("dsl jobs\nJob first\n\tOutput result\n", meta));
    }
    @Test void ambiguousFacetComponentsRequireQualification() {
        String meta = "dsl metta\nConcept Job\n\tfacet Image\n\t\tConcept Output\n\t\t\tdef integer size = 10\n\tfacet Audio\n\t\tConcept Output\n\t\t\tdef integer size = 20\n";
        var error = assertThrows(IllegalArgumentException.class, () -> Models.create("dsl jobs\nJob first is Image Audio\n\tOutput result\n", meta));
        assertTrue(error.getMessage().contains("Ambiguous"));
        assertTrue(error.getMessage().contains("Job.Image.Output"));
        assertTrue(error.getMessage().contains("Job.Audio.Output"));
    }
    @Test void qualificationDisambiguatesComponentsOfTwoAppliedFacets() throws Exception {
        String meta = "dsl metta\nConcept Job\n\tfacet Image\n\t\tConcept Output\n\t\t\tdef integer size = 10\n\tfacet Audio\n\t\tConcept Output\n\t\t\tdef integer size = 20\n";
        var model = Models.create("dsl jobs\nJob first is Image Audio\n\tJob.Audio.Output result\n", meta);
        assertEquals(number(20), model.get("first.result").property("size"));
    }

    private static Value number(int value) {return new Value.Number(BigDecimal.valueOf(value));}
}

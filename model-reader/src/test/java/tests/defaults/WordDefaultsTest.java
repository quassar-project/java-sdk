package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WordDefaultsTest {
    @Test void inheritedWordListIsReplacedRatherThanMerged() throws Exception {
        String meta = "dsl metta\nConcept Base as generalization\n\tdef word[]:{Open Closed Pending} states = Open Closed\nConcept Item extends Base\n";
        var model = Models.create("dsl inventory\nItem first\nItem second\n\tstates = Pending\n", meta);
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Open"), new Value.Reference("Closed"))), model.get("first").property("states"));
        assertEquals(new Value.Sequence(List.of(new Value.Reference("Pending"))), model.get("second").property("states"));
    }
    @Test void childWordDefaultOverridesInheritedDefault() throws Exception {
        String meta = "dsl metta\nConcept Base as generalization\n\tdef word:{Open Closed} state = Open\nConcept Item extends Base\n\tdef word:{Open Closed} state = Closed\n";
        assertEquals(new Value.Reference("Closed"), Models.create("dsl inventory\nItem first\n", meta).get("first").property("state"));
    }
    @Test void facetSuppliesWordDefaultAndAcceptsExplicitOverride() throws Exception {
        String meta = "dsl metta\nConcept Book\n\tfacet Digital\n\t\tdef word:{PDF EPUB} format = PDF\n";
        var model = Models.create("dsl library\nBook first is Digital\nBook second is Digital(format = EPUB)\n", meta);
        assertEquals(new Value.Reference("PDF"), model.get("first").property("format"));
        assertEquals(new Value.Reference("EPUB"), model.get("second").property("format"));
    }
    @Test void explicitEmptyWordDoesNotRestoreDefault() throws Exception {
        assertTrue(DefaultsContract.settings("\tdef word:{Open Closed} state = Open\n", "\tstate = empty\n").get("first").property("state").isEmpty());
    }
    @Test void annotationsAreNotPartOfTheDefaultValue() throws Exception {
        assertEquals(new Value.Reference("Open"), DefaultsContract.settings("\tdef word:{Open Closed} state = Open as private\n", "").get("first").property("state"));
    }
}

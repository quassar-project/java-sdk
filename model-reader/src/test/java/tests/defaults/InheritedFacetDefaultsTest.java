package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InheritedFacetDefaultsTest {
    private static final String META = """
            dsl metta
            Concept Base as generalization
                facet Digital
                    def word:{PDF EPUB} format = PDF
                    def string[] labels = "base" "digital"
            Concept Branch extends Base as generalization
            Concept Book extends Branch
            """;

    @Test void facetDeclaredOnGrandparentSuppliesDefaults() throws Exception {
        var book = Models.create("dsl library\nBook first is Digital\n", META).get("first");
        assertEquals(new Value.Reference("PDF"), book.property("format"));
        assertEquals(new Value.Sequence(List.of(new Value.Text("base"), new Value.Text("digital"))), book.property("labels"));
    }
    @Test void explicitFacetArgumentsOverrideInheritedDefaults() throws Exception {
        var book = Models.create("dsl library\nBook first is Digital(format = EPUB)\n\tlabels = \"custom\"\n", META).get("first");
        assertEquals(new Value.Reference("EPUB"), book.property("format"));
        assertEquals(new Value.Sequence(List.of(new Value.Text("custom"))), book.property("labels"));
    }
    @Test void emptyFacetListOverridesInheritedDefault() throws Exception {
        var book = Models.create("dsl library\nBook first is Digital\n\tlabels = empty\n", META).get("first");
        assertEquals(new Value.Sequence(List.of()), book.property("labels"));
    }
    @Test void unappliedInheritedFacetDoesNotContributeValues() throws Exception {
        assertTrue(Models.create("dsl library\nBook first\n", META).get("first").properties().isEmpty());
    }
    @Test void localFacetDefinitionWinsOverInheritedFacetOfSameName() throws Exception {
        String meta = META + "    facet Digital\n        def word:{PDF EPUB} format = EPUB\n";
        assertEquals(new Value.Reference("EPUB"), Models.create("dsl library\nBook first is Digital\n", meta).get("first").property("format"));
    }
    @Test void facetCanInheritAnotherFacetDefaults() throws Exception {
        String meta = "dsl metta\nConcept Book\n\tfacet Printable\n\t\tdef integer pages = 100\n\tfacet Digital extends Printable\n\t\tdef integer pages = 200\n";
        assertEquals(new Value.Number(new BigDecimal("200")), Models.create("dsl library\nBook first is Digital\n", meta).get("first").property("pages"));
    }
    @Test void facetSubtypeInheritsAndOverridesDefaults() throws Exception {
        String meta = "dsl metta\nConcept Book\n\tfacet Digital\n\t\tdef integer copies = 1\n\t\tsub Downloadable\n\t\t\tdef integer copies = 2\n";
        assertEquals(new Value.Number(new BigDecimal("2")), Models.create("dsl library\nBook first is Digital.Downloadable\n", meta).get("first").property("copies"));
    }
    @Test void repeatedResolutionPreservesExplicitOriginAndDoesNotLeakBetweenInstances() throws Exception {
        var resolver = new EffectiveValueResolver(META);
        var raw = Models.create("dsl library\nBook first is Digital(format = EPUB)\nBook second is Digital\n");
        var result = resolver.resolve(raw);
        assertEquals(result.mograms(), resolver.resolve(result).mograms());
        assertEquals(new Value.Reference("PDF"), result.get("second").property("format"));
        assertTrue(raw.get("second").properties().isEmpty());
        assertEquals(Property.Origin.EXPLICIT, result.get("first").properties().stream().filter(p -> p.name().equals("format")).findFirst().orElseThrow().origin());
    }
}

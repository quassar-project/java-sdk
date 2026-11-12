package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepeatedFacetNamesTest {
    private static final String META = """
            dsl metta
            Concept ImageBase as generalization
                facet Config
                    def string label = "image"
            Concept AudioBase as generalization
                facet Config
                    def string label = "audio"
            Concept Image extends ImageBase
            Concept Audio extends AudioBase
            """;
    @Test void inheritedSameNamedFacetsStayInTheirBranch() throws Exception {
        var model = Models.create("dsl media\nImage image is Config\nAudio audio is Config\n", META);
        assertEquals(new Value.Text("image"), model.get("image").property("label"));
        assertEquals(new Value.Text("audio"), model.get("audio").property("label"));
    }
    @Test void overrideInOneBranchDoesNotAffectAnotherBranch() throws Exception {
        var model = Models.create("dsl media\nImage image is Config(label = \"custom\")\nAudio audio is Config\n", META);
        assertEquals(new Value.Text("custom"), model.get("image").property("label"));
        assertEquals(new Value.Text("audio"), model.get("audio").property("label"));
    }
    @Test void facetParentReferenceResolvesInItsOwnBranch() throws Exception {
        String meta = "dsl metta\nConcept Image\n\tfacet Base\n\t\tdef string label = \"image\"\n\tfacet Config extends Base\nConcept Audio\n\tfacet Base\n\t\tdef string label = \"audio\"\n\tfacet Config extends Base\n";
        var model = Models.create("dsl media\nImage image is Config\nAudio audio is Config\n", meta);
        assertEquals(new Value.Text("image"), model.get("image").property("label"));
        assertEquals(new Value.Text("audio"), model.get("audio").property("label"));
    }
    @Test void conflictingDefaultsInRepeatedFacetDeclarationsAreRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new EffectiveValueResolver("dsl metta\nConcept Image\n\tfacet Config\n\t\tdef string label = \"first\"\n\tfacet Config\n\t\tdef string label = \"second\"\n"));
        assertTrue(error.getMessage().contains("Image.Config"));
    }
    @Test void compatibleFacetExtensionsPreserveBothProperties() throws Exception {
        String meta = "dsl metta\nConcept Image\n\tfacet Config\n\t\tdef string label = \"image\"\nConcept Image\n\tfacet Config\n\t\tdef string suffix = \"png\"\n";
        var model = Models.create("dsl media\nImage image is Config\n", meta);
        assertEquals(new Value.Text("image"), model.get("image").property("label"));
        assertEquals(new Value.Text("png"), model.get("image").property("suffix"));
    }
}

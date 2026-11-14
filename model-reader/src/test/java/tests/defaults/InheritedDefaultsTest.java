package tests.defaults;

import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InheritedDefaultsTest {
    private static final String META = """
            dsl metta
            Concept Base as generalization
                def integer attempts = 3
            Concept Branch extends Base as generalization
                def integer attempts = 7
            Concept Job extends Branch
            """;

    @Test void inheritsTheClosestAncestorsDefault() throws Exception {
        assertEquals(new Value.Number(new BigDecimal("7")), DefaultsContract.read(META, "dsl jobs\nJob first\n").get("first").property("attempts"));
    }
    @Test void childDefinitionOverridesAncestorDefault() throws Exception {
        assertEquals(new Value.Number(new BigDecimal("11")), DefaultsContract.read(META + "    def integer attempts = 11\n", "dsl jobs\nJob first\n").get("first").property("attempts"));
    }
    @Test void modelValueOverridesAllAncestorDefaults() throws Exception {
        assertEquals(new Value.Number(new BigDecimal("2")), DefaultsContract.read(META, "dsl jobs\nJob first\n\tattempts = 2\n").get("first").property("attempts"));
    }
    @Test void overridesInOneInstanceDoNotModifyAnother() throws Exception {
        var model = DefaultsContract.read(META, "dsl jobs\nJob first\n\tattempts = 2\nJob second\n");
        assertEquals(new Value.Number(new BigDecimal("2")), model.get("first").property("attempts"));
        assertEquals(new Value.Number(new BigDecimal("7")), model.get("second").property("attempts"));
    }
    @Test void readingAnotherModelDoesNotMutateMetamodelDefaults() throws Exception {
        DefaultsContract.read(META, "dsl jobs\nJob first\n\tattempts = 2\n");
        assertEquals(new Value.Number(new BigDecimal("7")), DefaultsContract.read(META, "dsl jobs\nJob first\n").get("first").property("attempts"));
    }
    @Test void samePropertyNameInDifferentTypesHasIndependentDefaults() throws Exception {
        var model = DefaultsContract.read("dsl metta\nConcept Car\n\tdef integer wheels = 4\nConcept Bike\n\tdef integer wheels = 2\n", "dsl transport\nCar car\nBike bike\n");
        assertEquals(new Value.Number(new BigDecimal("4")), model.get("car").property("wheels"));
        assertEquals(new Value.Number(new BigDecimal("2")), model.get("bike").property("wheels"));
    }
    @Test void nestedInstancesReceiveTheirOwnDefaults() throws Exception {
        var model = DefaultsContract.read("dsl metta\nConcept Warehouse\n\tConcept Item\n\t\tdef integer stock = 5\n", "dsl inventory\nWarehouse central\n\tItem keyboard\n");
        assertEquals(new Value.Number(new BigDecimal("5")), model.get("central.keyboard").property("stock"));
    }
}

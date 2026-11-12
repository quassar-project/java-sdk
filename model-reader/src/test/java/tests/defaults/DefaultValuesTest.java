package tests.defaults;

import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DefaultValuesTest {
    @Test void omittedIntegerUsesItsDefault() throws Exception {
        assertEquals(new Value.Number(BigDecimal.ONE), DefaultsContract.settings("\tdef integer attempts = 1\n", "").get("first").property("attempts"));
    }
    @Test void omittedStringUsesItsDefault() throws Exception {
        assertEquals(new Value.Text("Keyboard"), DefaultsContract.settings("\tdef string title = \"Keyboard\"\n", "").get("first").property("title"));
    }
    @Test void falseIsADefaultRatherThanAnAbsentValue() throws Exception {
        assertEquals(new Value.Bool(false), DefaultsContract.settings("\tdef boolean enabled = false\n", "").get("first").property("enabled"));
    }
    @Test void zeroIsADefaultRatherThanAnAbsentValue() throws Exception {
        assertEquals(new Value.Number(BigDecimal.ZERO), DefaultsContract.settings("\tdef integer count = 0\n", "").get("first").property("count"));
    }
    @Test void emptyStringRemainsText() throws Exception {
        assertEquals(new Value.Text(""), DefaultsContract.settings("\tdef string title = \"\"\n", "").get("first").property("title"));
    }
    @Test void decimalPrecisionIsPreserved() throws Exception {
        assertEquals(new Value.Number(new BigDecimal("49.99")), DefaultsContract.settings("\tdef double price = 49.99\n", "").get("first").property("price"));
    }
    @Test void listDefaultPreservesAllValuesAndTheirOrder() throws Exception {
        assertEquals(new Value.Sequence(List.of(new Value.Text("red"), new Value.Text("blue"))),
                DefaultsContract.settings("\tdef string[] tags = \"red\" \"blue\"\n", "").get("first").property("tags"));
    }
    @Test void singletonListDoesNotBecomeAScalar() throws Exception {
        assertEquals(new Value.Sequence(List.of(new Value.Text("red"))),
                DefaultsContract.settings("\tdef string[] tags = \"red\"\n", "").get("first").property("tags"));
    }
    @Test void wordDefaultRetainsTheReadersWordRepresentation() throws Exception {
        assertEquals(new Value.Reference("Open"), DefaultsContract.settings("\tdef word:{Open Closed} state = Open\n", "").get("first").property("state"));
    }
    @Test void propertyWithoutDefaultIsNotInvented() throws Exception {
        var element = DefaultsContract.settings("\tdef string[0..1] note\n", "").get("first");
        assertTrue(element.property("note").isEmpty());
        assertTrue(element.properties().stream().noneMatch(property -> property.name().equals("note")));
    }
}

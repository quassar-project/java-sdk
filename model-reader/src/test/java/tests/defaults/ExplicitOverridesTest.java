package tests.defaults;

import io.quassar.reader.model.Value;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExplicitOverridesTest {
    @Test void explicitValueOverridesDefault() throws Exception {
        var element = DefaultsContract.settings("\tdef integer attempts = 3\n", "\tattempts = 9\n").get("first");
        assertEquals(new Value.Number(new BigDecimal("9")), element.property("attempts"));
        assertEquals(1, element.properties().stream().filter(p -> p.name().equals("attempts")).count());
    }
    @Test void explicitFalseOverridesTrue() throws Exception {
        assertEquals(new Value.Bool(false), DefaultsContract.settings("\tdef boolean enabled = true\n", "\tenabled = false\n").get("first").property("enabled"));
    }
    @Test void explicitZeroOverridesNonzero() throws Exception {
        assertEquals(new Value.Number(BigDecimal.ZERO), DefaultsContract.settings("\tdef integer attempts = 3\n", "\tattempts = 0\n").get("first").property("attempts"));
    }
    @Test void explicitEmptyStringOverridesNonemptyText() throws Exception {
        assertEquals(new Value.Text(""), DefaultsContract.settings("\tdef string title = \"default\"\n", "\ttitle = \"\"\n").get("first").property("title"));
    }
    @Test void explicitEmptyDoesNotReactivateDefault() throws Exception {
        var element = DefaultsContract.settings("\tdef string[0..1] title = \"default\"\n", "\ttitle = empty\n").get("first");
        assertTrue(element.property("title").isEmpty());
        assertTrue(element.properties().stream().anyMatch(p -> p.name().equals("title")));
    }
    @Test void explicitListReplacesRatherThanAppendsDefaults() throws Exception {
        assertEquals(new Value.Sequence(List.of(new Value.Text("green"), new Value.Text("yellow"))),
                DefaultsContract.settings("\tdef string[] tags = \"red\" \"blue\"\n", "\ttags = \"green\" \"yellow\"\n").get("first").property("tags"));
    }
    @Test void explicitEmptyListDoesNotRestoreDefaultItems() throws Exception {
        assertEquals(new Value.Sequence(List.of()), DefaultsContract.settings("\tdef string[] tags = \"red\"\n", "\ttags = empty\n").get("first").property("tags"));
    }
    @Test void inlineParameterOverridesDefault() throws Exception {
        var model = DefaultsContract.read("dsl metta\nConcept Settings\n\tdef integer attempts = 3\n", "dsl settings\nSettings(attempts = 9) first\n");
        assertEquals(new Value.Number(new BigDecimal("9")), model.get("first").property("attempts"));
    }
}

package tests.defaults;

import io.quassar.reader.model.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.util.stream.Stream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ValueTypesTest {
    static Stream<Arguments> values() {
        return Stream.of(
            Arguments.of("integer", "-2147483648", new Value.Number(new BigDecimal("-2147483648")), "0", new Value.Number(BigDecimal.ZERO)),
            Arguments.of("long", "9223372036854775807", new Value.Number(new BigDecimal("9223372036854775807")), "-9223372036854775808", new Value.Number(new BigDecimal("-9223372036854775808"))),
            Arguments.of("double", "1.25e-12", new Value.Number(new BigDecimal("1.25e-12")), "-49.99", new Value.Number(new BigDecimal("-49.99"))),
            Arguments.of("boolean", "true", new Value.Bool(true), "false", new Value.Bool(false)),
            Arguments.of("string", "\"España 東京 > as // text\"", new Value.Text("España 東京 > as // text"), "\"\"", new Value.Text("")),
            Arguments.of("word:{Open Closed}", "Open", new Value.Reference("Open"), "Closed", new Value.Reference("Closed")),
            Arguments.of("word:{4 8}", "4", new Value.Reference("4"), "8", new Value.Reference("8")),
            Arguments.of("date", "\"2024-02-29\"", new Value.Text("2024-02-29"), "\"2025-01-01\"", new Value.Text("2025-01-01")),
            Arguments.of("time", "\"12:30:00\"", new Value.Text("12:30:00"), "\"00:00:00\"", new Value.Text("00:00:00")),
            Arguments.of("instant", "\"2024-01-01T00:00:00Z\"", new Value.Text("2024-01-01T00:00:00Z"), "\"2024-01-01T01:00:00+01:00\"", new Value.Text("2024-01-01T01:00:00+01:00")),
            Arguments.of("resource:{png svg}", "\"assets/default.png\"", new Value.Text("assets/default.png"), "\"icons/logo.svg\"", new Value.Text("icons/logo.svg")),
            Arguments.of("Person", "john", new Value.Reference("john"), "jane", new Value.Reference("jane")),
            Arguments.of("object:java.lang.String", "empty", new Value.Empty(), "empty", new Value.Empty()),
            Arguments.of("function:Action", "@run", new Value.Reference("@run"), "@stop", new Value.Reference("@stop"))
        );
    }

    @ParameterizedTest(name = "default {0}") @MethodSource("values")
    void readsEachScalarDefault(String type, String literal, Value expected, String override, Value overridden) throws Exception {
        assertEquals(expected, DefaultsContract.settings("\tdef " + type + " value = " + literal + "\n", "").get("first").property("value"));
    }

    @ParameterizedTest(name = "override {0}") @MethodSource("values")
    void explicitScalarOverridesDefault(String type, String literal, Value expected, String override, Value overridden) throws Exception {
        assertEquals(overridden, DefaultsContract.settings("\tdef " + type + " value = " + literal + "\n", "\tvalue = " + override + "\n").get("first").property("value"));
    }

    @ParameterizedTest(name = "list {0}") @MethodSource("values")
    void typedListDefaultsAndExplicitReplacement(String type, String literal, Value expected, String override, Value overridden) throws Exception {
        int colon = type.indexOf(':');
        String list = colon < 0 ? type + "[]" : type.substring(0, colon) + "[]" + type.substring(colon);
        String definition = "\tdef " + list + " value = " + literal + "\n";
        assertEquals(new Value.Sequence(expected.isEmpty() ? List.of() : List.of(expected)), DefaultsContract.settings(definition, "").get("first").property("value"));
        assertEquals(new Value.Sequence(overridden.isEmpty() ? List.of() : List.of(overridden)), DefaultsContract.settings(definition, "\tvalue = " + override + "\n").get("first").property("value"));
        assertEquals(new Value.Sequence(List.of()), DefaultsContract.settings(definition, "\tvalue = empty\n").get("first").property("value"));
    }
}

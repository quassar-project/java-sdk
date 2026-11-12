package tests.defaults;

import io.quassar.reader.Models;
import io.quassar.reader.model.Model;

final class DefaultsContract {
    private DefaultsContract() {}

    static Model read(String metamodel, String model) throws Exception {
        return Models.create(model, metamodel);
    }

    static Model settings(String definitions, String assignments) throws Exception {
        return read("dsl metta\nConcept Settings\n" + definitions,
                "dsl settings\nSettings first\n" + assignments);
    }
}

package tests;

import io.quassar.reader.Models;
import io.quassar.reader.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReaderIT {
	@Test
	void readsTheOriginalInventory() throws Exception {
		Model model = Models.load(Path.of(System.getenv("TARA_MODEL_ZIP")));
		assertThat(model.mograms()).hasSize(583);
		assertThat(model.allMograms()).hasSize(11137);
		assertThat(model.select().type("Entidad").count()).isEqualTo(5);
		assertThat(model.get("Persona.Juridica").identifier()).isEqualTo("Juridica");
	}
}

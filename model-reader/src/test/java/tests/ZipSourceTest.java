package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.api.Test;

import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public final class ZipSourceTest {
	@Test
	void preparesZipAndFolderAsTheSameModel() throws Exception {
		Model folder = Models.load(TestResources.path("folder"));
		try (InputStream input = new io.quassar.reader.adapters.ZipSource(TestResources.path("folder.zip")).open()) {
			Model zip = new ModelReader().read(input);
			assertThat(zip.mograms()).isEqualTo(folder.mograms());
			assertThat(zip.allMograms()).hasSize(folder.allMograms().size());
		}
	}
}

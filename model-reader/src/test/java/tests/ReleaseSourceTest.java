package tests;

import io.quassar.reader.ModelRelease;
import io.quassar.reader.adapters.TextSource;
import io.quassar.reader.ports.ReleaseSource;
import org.junit.jupiter.api.Test;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseSourceTest {
	@Test
	void loadsAReleaseThroughThePortAndClosesItsInput() throws Exception {
		AtomicBoolean closed = new AtomicBoolean();
		byte[] resource = {1, 2, 3};
		var input = new FilterInputStream(new TextSource(List.of(TestResources.text("library.tara"))).open()) {
			@Override
			public void close() throws IOException {
				closed.set(true);
				super.close();
			}
		};
		ReleaseSource source = () -> new ReleaseSource.Content(input, "{\"views\":[\"library:1.0.0\"]}", Map.of("cover.bin", resource));
		ModelRelease release = ModelRelease.read(source);
		resource[0] = 99;
		assertThat(release.model().get("DonQuixote").type()).isEqualTo("Book");
		assertThat(release.metadata()).isEqualTo("{\"views\":[\"library:1.0.0\"]}");
		assertThat(release.resource("cover.bin").bytes()).containsExactly(1, 2, 3);
		assertThat(closed.get()).isTrue();
	}
}

package tests;

import io.quassar.reader.Models;

import org.junit.jupiter.api.Test;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.adapters.UrlSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlSourceTest {
	@Test
	void loadsZipAndClosesTheResponse() throws Exception {
		TrackingInputStream response = zipResponse();
		assertThat(Models.load(url(response, 10000, 30000)).mograms())
			.isEqualTo(TestResources.model("library.tara").mograms());
		assertThat(response.closed).isTrue();
	}

	@Test
	void usesCustomTimeouts() throws Exception {
		TrackingInputStream response = zipResponse();
		UrlSource source = new UrlSource(url(response, 500, 750), 500, 750);
		try (InputStream input = source.open()) {
			assertThat(new ModelReader().read(input).mograms()).hasSize(4);
		}
		assertThat(response.closed).isTrue();
	}

	@Test
	void closesTheResponseWhenReadingFails() throws Exception {
		AtomicBoolean closed = new AtomicBoolean();
		InputStream response = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("source failed");
			}

			@Override
			public void close() {
				closed.set(true);
			}
		};
		assertThatThrownBy(() -> Models.load(url(response, 10000, 30000)))
			.isInstanceOf(IOException.class).hasMessage("source failed");
		assertThat(closed.get()).isTrue();
	}

	private static TrackingInputStream zipResponse() throws IOException {
		try (InputStream input = TestResources.open("library.zip")) {
			return new TrackingInputStream(input.readAllBytes());
		}
	}

	private static URL url(InputStream response, int connectTimeout, int readTimeout) throws Exception {
		return new URL(null, "memory://library.zip", new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL url) {
				return new URLConnection(url) {
					@Override
					public void connect() {
					}

					@Override
					public InputStream getInputStream() {
						assertThat(getConnectTimeout()).isEqualTo(connectTimeout);
						assertThat(getReadTimeout()).isEqualTo(readTimeout);
						return response;
					}
				};
			}
		});
	}

	private static final class TrackingInputStream extends ByteArrayInputStream {
		private boolean closed;

		private TrackingInputStream(byte[] bytes) {
			super(bytes);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}
}

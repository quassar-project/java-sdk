package io.quassar.reader.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public final class UrlSource extends AbstractSource {
	private final URL url;
	private final int connectTimeout;
	private final int readTimeout;

	public UrlSource(URL url) {
		this(url, 10000, 30000);
	}

	public UrlSource(URL url, int connectTimeout, int readTimeout) {
		this.url = url;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public URL url() {
		return url;
	}

	public int connectTimeout() {
		return connectTimeout;
	}

	public int readTimeout() {
		return readTimeout;
	}

	@Override
	public InputStream open() throws IOException {
		return read(input -> url.getPath().endsWith(".tara") ? combine(List.of(readText(input))) : unzip(input));
	}

	public String text() throws IOException {
		return read(this::readText);
	}

	private <T> T read(SourceReader<T> reader) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		try (InputStream input = connection.getInputStream()) {
			return reader.read(input);
		} finally {
			if (connection instanceof HttpURLConnection http) http.disconnect();
		}
	}

	@FunctionalInterface
	private interface SourceReader<T> {
		T read(InputStream input) throws IOException;
	}
}

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

	public UrlSource(URL url) {
		this(url, 10000, 30000);
	}

	@Override
	public InputStream open() throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		try {
			try (InputStream input = connection.getInputStream()) {
				if (url.getPath().endsWith(".tara")) return combine(List.of(readText(input)));
				return unzip(input);
			}
		} finally {
			if (connection instanceof HttpURLConnection) ((HttpURLConnection) connection).disconnect();
		}
	}
}

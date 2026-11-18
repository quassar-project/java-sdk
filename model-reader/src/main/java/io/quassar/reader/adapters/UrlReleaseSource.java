package io.quassar.reader.adapters;

import io.quassar.reader.ports.ReleaseSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public record UrlReleaseSource(URL url, String authToken) implements ReleaseSource {
	@Override
	public Content read() throws IOException {
		URLConnection connection = connection();
		try {
			if (connection instanceof HttpURLConnection http && http.getResponseCode() != 200)
				throw new IOException("Release download failed: HTTP " + http.getResponseCode());
			try (InputStream input = connection.getInputStream()) {
				return new ZipReleaseSource(input).read();
			}
		} finally {
			if (connection instanceof HttpURLConnection http) http.disconnect();
		}
	}

	private URLConnection connection() throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(30000);
		if (connection instanceof HttpURLConnection http) {
			http.setInstanceFollowRedirects(false);
			if (authToken != null && !authToken.isBlank()) http.setRequestProperty("Authorization", "Bearer " + authToken);
		} else if (authToken != null && !authToken.isBlank()) throw new IOException("Authentication requires HTTP(S)");
		return connection;
	}
}

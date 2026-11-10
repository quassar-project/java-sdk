package io.quassar.metamodel.adapters;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UrlSource extends AbstractMetamodelSource {
	private final URL url;

	public UrlSource(URL url) {
		this.url = url;
	}

	@Override
	public Content openContent() throws IOException {
		if (url.getProtocol().equals("file") && url.getHost().isEmpty()) {
			Path path = localPath();
			if (Files.isDirectory(path)) return new FolderSource(path).openContent();
		}
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(30000);
		try {
			return unzip(connection.getInputStream());
		} finally {
			if (connection instanceof HttpURLConnection) ((HttpURLConnection) connection).disconnect();
		}
	}

	private Path localPath() {
		try {
			return Path.of(url.toURI());
		} catch (URISyntaxException | IllegalArgumentException exception) {
			return Path.of(url.getPath());
		}
	}
}

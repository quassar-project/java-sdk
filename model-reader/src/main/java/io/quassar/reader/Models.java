package io.quassar.reader;

import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.domain.ModelCompatibility;
import io.quassar.reader.adapters.JsonManifestReader;
import io.quassar.reader.adapters.ModelHeaderReader;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.Model;
import io.quassar.reader.ports.ModelSource;
import io.quassar.reader.adapters.UrlSource;
import io.quassar.reader.adapters.TextSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Models {
	private Models() {
	}

	public static Model load(Path path) throws IOException {
		if (Files.isDirectory(path)) return ModelRelease.open(path).model();
		if (path.toString().endsWith(".tara")) return create(Files.readString(path));
		return ModelRelease.open(path).model();
	}

	public static Model load(URL url) throws IOException {
		return url.getPath().endsWith(".tara") ? read(new UrlSource(url)) : ModelRelease.download(url, null).model();
	}

	public static Model load(URI uri) throws IOException {
		return load(uri.toURL());
	}

	public static Model loadForDsl(Path path, String expectedDsl) throws IOException {
		if (Files.isDirectory(path) || !path.toString().endsWith(".tara"))
			return checkedRelease(ModelRelease.open(path), expectedDsl);
		return checkedText(Files.readString(path), expectedDsl);
	}

	public static Model loadForDsl(URL url, String expectedDsl) throws IOException {
		if (url.getProtocol().equals("file")) {
			try {
				return loadForDsl(Path.of(url.toURI()), expectedDsl);
			} catch (URISyntaxException exception) {
				throw new IOException("Invalid model URL: " + url, exception);
			}
		}
		if (!url.getPath().endsWith(".tara")) return checkedRelease(ModelRelease.download(url, null), expectedDsl);
		return checkedText(new UrlSource(url).text(), expectedDsl);
	}

	public static Model loadForDsl(URI uri, String expectedDsl) throws IOException {
		return loadForDsl(uri.toURL(), expectedDsl);
	}

	private static Model checkedRelease(ModelRelease release, String expectedDsl) throws IOException {
		ModelCompatibility.require(expectedDsl, new JsonManifestReader().read(release.metadata()), "manifest.json");
		return release.model();
	}

	private static Model checkedText(String text, String expectedDsl) throws IOException {
		ModelCompatibility.require(expectedDsl, new ModelHeaderReader().read(text), "the Tara dsl header");
		return create(text);
	}

	public static Model create(String text) throws IOException {
		return read(new TextSource(List.of(text)));
	}

	public static Model create(String text, String metamodelText) throws IOException {
		return new EffectiveValueResolver(metamodelText).resolve(create(text));
	}

	public static Model load(Path path, String metamodelText) throws IOException {
		return new EffectiveValueResolver(metamodelText).resolve(load(path));
	}

	public static Model load(URL url, String metamodelText) throws IOException {
		return new EffectiveValueResolver(metamodelText).resolve(load(url));
	}

	public static Model load(URI uri, String metamodelText) throws IOException {
		return load(uri.toURL(), metamodelText);
	}

	public static Model read(ModelSource source) throws IOException {
		try (InputStream input = source.open()) {
			return new ModelReader().read(input);
		}
	}
}

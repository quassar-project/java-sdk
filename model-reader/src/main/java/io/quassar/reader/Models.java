package io.quassar.reader;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.Model;
import io.quassar.reader.ports.ModelSource;
import io.quassar.reader.adapters.UrlSource;
import io.quassar.reader.adapters.TextSource;
import java.util.List;
import java.util.LinkedHashSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
		var connection = url.openConnection();
		var source = new UrlSource(url);
		connection.setConnectTimeout(source.connectTimeout());
		connection.setReadTimeout(source.readTimeout());
		try (InputStream input = connection.getInputStream()) {
			return checkedText(new String(input.readAllBytes(), StandardCharsets.UTF_8), expectedDsl);
		} finally {
			if (connection instanceof HttpURLConnection http) http.disconnect();
		}
	}

	public static Model loadForDsl(URI uri, String expectedDsl) throws IOException {
		return loadForDsl(uri.toURL(), expectedDsl);
	}

	private static Model checkedRelease(ModelRelease release, String expectedDsl) throws IOException {
		requireDsl(expectedDsl, declaredDsls(release.metadata()), "manifest.json");
		return release.model();
	}

	private static List<String> declaredDsls(String manifest) throws IOException {
		if (manifest.isBlank()) return List.of();
		try {
			var metadata = JsonParser.parseString(manifest);
			if (!metadata.isJsonObject()) throw new IOException("Invalid model manifest.json. Expected a JSON object.");
			var object = metadata.getAsJsonObject();
			var dsls = new LinkedHashSet<String>();
			var primary = object.get("dsl");
			if (primary != null && primary.isJsonPrimitive() && primary.getAsJsonPrimitive().isString()
					&& !primary.getAsString().isBlank()) dsls.add(primary.getAsString());
			var declarations = object.getAsJsonArray("dsls");
			if (declarations != null)
				for (var declaration : declarations) {
					String value = declaration.getAsString();
					dsls.add(value.substring(value.indexOf('|') + 1));
				}
			return List.copyOf(dsls);
		} catch (JsonParseException exception) {
			throw new IOException("Invalid model manifest.json", exception);
		}
	}

	private static final Pattern DSL_HEADER = Pattern.compile("(?m)^[\\t ]*dsl[\\t ]+(\\S+)");

	private static Model checkedText(String text, String expectedDsl) throws IOException {
		var header = DSL_HEADER.matcher(text.startsWith("\uFEFF") ? text.substring(1) : text);
		requireDsl(expectedDsl, header.find() ? header.group(1) : "", "the Tara dsl header");
		return create(text);
	}

	private static void requireDsl(String expected, String actual, String source) throws IOException {
		requireDsl(expected, actual.isBlank() ? List.of() : List.of(actual), source);
	}

	private static void requireDsl(String expected, List<String> actual, String source) throws IOException {
		if (actual.isEmpty()) throw new IOException("Expected DSL " + expected + " but no DSL is declared in " + source);
		if (!actual.contains(expected))
			throw new IOException("Expected DSL " + expected + " but found " + String.join(", ", actual) + " in " + source);
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

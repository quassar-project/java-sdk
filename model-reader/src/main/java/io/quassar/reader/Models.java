package io.quassar.reader;

import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.model.Model;
import io.quassar.reader.ports.ModelSource;
import io.quassar.reader.adapters.UrlSource;
import io.quassar.reader.adapters.TextSource;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

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

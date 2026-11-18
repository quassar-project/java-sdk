package io.quassar.reader;

import io.quassar.reader.adapters.FileReleaseSource;
import io.quassar.reader.adapters.UrlReleaseSource;
import io.quassar.reader.adapters.ZipReleaseSource;
import io.quassar.reader.domain.EffectiveValueResolver;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Resource;
import io.quassar.reader.ports.ReleaseSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

public record ModelRelease(Model model, String metadata) {
	public ModelRelease withDefaults(EffectiveValueResolver resolver) throws IOException {
		return new ModelRelease(resolver.resolve(model), metadata);
	}

	public Set<String> resourceNames() { return model.resourceNames(); }
	public Resource resource(String name) throws FileNotFoundException { return model.resource(name); }

	public static ModelRelease open(Path path) throws IOException {
		return read(new FileReleaseSource(path));
	}

	public static ModelRelease download(URL url, String authToken) throws IOException {
		return read(new UrlReleaseSource(url, authToken));
	}

	public static ModelRelease read(InputStream input) throws IOException {
		return read(new ZipReleaseSource(input));
	}

	public static ModelRelease read(ReleaseSource source) throws IOException {
		try (var content = source.read()) {
			Model model = new ModelReader().read(content.input()).withResources(content.resources());
			return new ModelRelease(model, content.manifest());
		}
	}
}

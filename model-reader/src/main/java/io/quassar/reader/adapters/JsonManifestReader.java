package io.quassar.reader.adapters;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.quassar.reader.model.ModelManifest;
import io.quassar.reader.ports.ManifestReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JsonManifestReader implements ManifestReader {
	@Override
	public ModelManifest read(String content) throws IOException {
		if (content.isBlank()) return new ModelManifest(List.of());
		try {
			var metadata = JsonParser.parseString(content);
			if (!metadata.isJsonObject()) throw new IOException("Invalid model manifest.json. Expected a JSON object.");
			var declarations = metadata.getAsJsonObject().getAsJsonArray("views");
			List<String> views = new ArrayList<>();
			if (declarations != null)
				for (var declaration : declarations) views.add(declaration.getAsString());
			return new ModelManifest(views);
		} catch (JsonParseException exception) {
			throw new IOException("Invalid model manifest.json", exception);
		}
	}
}

package io.quassar.metamodel.adapters;

import com.google.gson.Gson;
import io.quassar.metamodel.model.MetamodelManifest;
import io.quassar.metamodel.ports.MetamodelSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractMetamodelSource implements MetamodelSource {
	protected static final String MANIFEST = "manifest.json";
	private static final Gson JSON = new Gson();

	@Override
	public final InputStream open() throws IOException {
		return openContent().input();
	}

	@Override
	public abstract Content openContent() throws IOException;

	protected Content unzip(InputStream input) throws IOException {
		Map<String, String> sources = new TreeMap<>();
		String manifest = null;
		try (ZipInputStream zip = new ZipInputStream(input)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				if (!entry.isDirectory() && name.endsWith(".tara")) sources.put(name, new String(zip.readAllBytes(), UTF_8));
				if (name.equals(MANIFEST)) manifest = new String(zip.readAllBytes(), UTF_8);
				zip.closeEntry();
			}
		}
		if (sources.isEmpty()) throw new ZipException("Expected an M2 ZIP containing .tara files");
		return combine(sources.values(), manifest);
	}

	protected Content combine(Iterable<String> sources, String manifest) {
		StringBuilder content = new StringBuilder();
		for (String source : sources) append(source, content);
		var input = new ByteArrayInputStream(content.toString().getBytes(UTF_8));
		return new Content(input, Optional.ofNullable(JSON.fromJson(manifest, MetamodelManifest.class)));
	}

	private void append(String source, StringBuilder content) {
		if (source.startsWith("\uFEFF")) source = source.substring(1);
		for (String line : source.split("\\R")) if (!isDslHeader(line)) content.append(line).append('\n');
	}

	private boolean isDslHeader(String line) {
		String text = line.stripLeading();
		return text.equals("dsl")
				|| (text.startsWith("dsl") && text.length() > 3
				&& Character.isWhitespace(text.charAt(3)));
	}
}

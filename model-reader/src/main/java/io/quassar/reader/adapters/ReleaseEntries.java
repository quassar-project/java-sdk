package io.quassar.reader.adapters;

import io.quassar.reader.ports.ReleaseSource.Content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

final class ReleaseEntries {
	private ReleaseEntries() {
	}

	static boolean includes(String name) {
		return name.endsWith(".tara") || name.equals("manifest.json") || name.startsWith("resources/");
	}

	static Content content(Map<String, byte[]> entries) throws IOException {
		Map<String, String> sources = new TreeMap<>();
		Map<String, byte[]> resources = new TreeMap<>();
		String manifest = "";
		for (var entry : entries.entrySet()) {
			String name = entry.getKey();
			if (name.startsWith("resources/")) resources.put(name.substring(10), entry.getValue());
			else if (name.equals("manifest.json")) manifest = new String(entry.getValue(), StandardCharsets.UTF_8);
			else if (name.endsWith(".tara")) sources.put(name, new String(entry.getValue(), StandardCharsets.UTF_8));
		}
		if (sources.isEmpty()) throw new IOException("Release contains no .tara model files");
		return new Content(new TextSource(new ArrayList<>(sources.values())).open(), manifest, resources);
	}
}

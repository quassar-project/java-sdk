package io.quassar.reader.adapters;

import io.quassar.reader.ports.ReleaseSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public record ZipReleaseSource(InputStream input) implements ReleaseSource {
	@Override
	public Content read() throws IOException {
		Map<String, byte[]> entries = new TreeMap<>();
		Set<String> names = new HashSet<>();
		try (ZipInputStream zip = new ZipInputStream(input)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				validate(name, names);
				if (!entry.isDirectory() && ReleaseEntries.includes(name)) entries.put(name, zip.readAllBytes());
				zip.closeEntry();
			}
		}
		return ReleaseEntries.content(entries);
	}

	private void validate(String name, Set<String> names) throws IOException {
		if (name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains(".."))
			throw new IOException("Invalid ZIP entry: " + name);
		if (!names.add(name)) throw new IOException("Duplicate ZIP entry: " + name);
	}
}

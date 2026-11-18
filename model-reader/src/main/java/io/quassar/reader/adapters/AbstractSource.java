package io.quassar.reader.adapters;

import io.quassar.reader.ports.ModelSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractSource implements ModelSource {
	protected InputStream combine(Iterable<String> sources) {
		StringBuilder content = new StringBuilder();
		for (String source : sources) append(source, content);
		return new ByteArrayInputStream(content.toString().getBytes(UTF_8));
	}

	protected InputStream unzip(InputStream input) throws IOException {
		Map<String, String> sources = new TreeMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(input.readAllBytes()))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().endsWith(".tara"))
					sources.put(entry.getName(), readText(zip));
				zip.closeEntry();
			}
		}
		return combine(sources.values());
	}

	protected String readText(InputStream input) throws IOException {
		return new String(input.readAllBytes(), UTF_8);
	}

	protected void append(String source, StringBuilder content) {
		if (source.startsWith("\uFEFF")) source = source.substring(1);
		for (String line : source.split("\\R")) if (!isDirective(line)) content.append(line).append('\n');
	}

	protected boolean isDirective(String line) {
		String[] parts = line.stripLeading().split("\\s+", 2);
		return (parts[0].equals("dsl") || parts[0].equals("use"))
			&& (parts.length == 1 || !parts[1].stripLeading().startsWith("="));
	}
}

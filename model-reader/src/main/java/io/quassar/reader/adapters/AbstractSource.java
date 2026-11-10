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
		for (String line : source.split("\\R")) if (!isDslHeader(line)) content.append(line).append('\n');
	}

	protected boolean isDslHeader(String line) {
		String text = line.stripLeading();
		return text.equals("dsl")
				|| (text.startsWith("dsl") && text.length() > 3
				&& Character.isWhitespace(text.charAt(3)));
	}
}

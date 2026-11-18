package io.quassar.reader.adapters;

import io.quassar.reader.ports.ReleaseSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public record FileReleaseSource(Path path) implements ReleaseSource {
	@Override
	public Content read() throws IOException {
		if (Files.isDirectory(path)) return readFolder();
		try (InputStream input = Files.newInputStream(path)) {
			return new ZipReleaseSource(input).read();
		}
	}

	private Content readFolder() throws IOException {
		Map<String, byte[]> entries = new TreeMap<>();
		try (var files = Files.walk(path)) {
			for (Path file : files.filter(candidate -> Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)).toList()) {
				String name = path.relativize(file).toString().replace(File.separatorChar, '/');
				if (ReleaseEntries.includes(name)) entries.put(name, Files.readAllBytes(file));
			}
		}
		return ReleaseEntries.content(entries);
	}
}

package tests.fixtures;

import io.quassar.reader.Models;
import io.quassar.reader.adapters.TextSource;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

public final class VisoraArchive {
	private final Model model;
	private final Map<String, String> sources;
	private final Map<String, byte[]> resources = new TreeMap<>();

	public VisoraArchive(Path path) throws Exception {
		model = Models.load(path);
		sources = sources(path);
		try (var zip = new ZipFile(path.toFile())) {
			for (var entry : zip.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("resources/")).toList())
				try (var input = zip.getInputStream(entry)) { resources.put(entry.getName().substring(10), input.readAllBytes()); }
		}
	}

	public Model model() { return model; }
	public Map<String, String> sources() { return sources; }
	public Map<String, byte[]> resources() { return resources; }

	public static Map<String, String> sources(Path path) throws Exception {
		Map<String, String> sources = new TreeMap<>();
		try (var zip = new ZipFile(path.toFile())) {
			for (var entry : zip.stream().filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".tara")).toList())
				try (var input = zip.getInputStream(entry)) {
					sources.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
				}
		}
		return sources;
	}

	public IdentityHashMap<Mogram, String> origins(Model model) throws Exception {
		IdentityHashMap<Mogram, String> result = new IdentityHashMap<>();
		int root = 0;
		for (var source : sources.entrySet()) {
			Model file = Models.read(new TextSource(List.of(source.getValue())));
			for (Mogram expected : file.mograms()) {
				Mogram actual = model.mograms().get(root++);
				assertThat(actual).as(source.getKey()).isEqualTo(expected);
				origin(actual, source.getKey(), result);
			}
		}
		assertThat(root).isEqualTo(model.mograms().size());
		return result;
	}

	private void origin(Mogram mogram, String file, IdentityHashMap<Mogram, String> origins) {
		origins.put(mogram, file);
		for (Mogram child : mogram.children()) origin(child, file, origins);
	}
}

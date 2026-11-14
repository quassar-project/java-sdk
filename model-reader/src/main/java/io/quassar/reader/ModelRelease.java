package io.quassar.reader;

import io.quassar.reader.model.Model;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class ModelRelease {
    private final Model model;
    private final String metadata;
    private final Map<String, byte[]> resources;

    private ModelRelease(Map<String, String> sources, String metadata, Map<String, byte[]> resources) throws IOException {
        if (sources.isEmpty()) throw new IOException("Release contains no .tara model files");
        model = Models.read(new io.quassar.reader.adapters.TextSource(new ArrayList<>(sources.values())));
        this.metadata = metadata;
        this.resources = resources;
    }

    private ModelRelease(Model model, String metadata, Map<String, byte[]> resources) {
        this.model = model;
        this.metadata = metadata;
        this.resources = resources;
    }

    public ModelRelease withDefaults(io.quassar.reader.domain.EffectiveValueResolver resolver) throws IOException {
        return new ModelRelease(resolver.resolve(model), metadata, resources);
    }

    public Model model() { return model; }
    public String metadata() { return metadata; }
    public Set<String> resourceNames() { return Collections.unmodifiableSet(resources.keySet()); }
    public InputStream resource(String name) throws IOException {
        byte[] bytes = resources.get(name);
        if (bytes == null) throw new FileNotFoundException("Resource not found: " + name);
        return new ByteArrayInputStream(bytes);
    }

    public static ModelRelease open(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            try (InputStream input = Files.newInputStream(path)) { return read(input); }
        }
        Map<String, byte[]> entries = new TreeMap<>();
        try (var files = Files.walk(path)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                String name = path.relativize(file).toString().replace(File.separatorChar, '/');
                if (relevant(name)) entries.put(name, Files.readAllBytes(file));
            }
        }
        return from(entries);
    }

    public static ModelRelease download(URL url, String authToken) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        if (connection instanceof HttpURLConnection http) {
            http.setInstanceFollowRedirects(false);
            if (authToken != null && !authToken.isBlank()) connection.setRequestProperty("Authorization", "Bearer " + authToken);
        } else if (authToken != null && !authToken.isBlank()) throw new IOException("Authentication requires HTTP(S)");
        try {
            if (connection instanceof HttpURLConnection http && http.getResponseCode() != 200)
                throw new IOException("Release download failed: HTTP " + http.getResponseCode());
            try (InputStream input = connection.getInputStream()) { return read(input); }
        } finally {
            if (connection instanceof HttpURLConnection http) http.disconnect();
        }
    }

    public static ModelRelease read(InputStream input) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains(".."))
                    throw new IOException("Invalid ZIP entry: " + name);
                if (!names.add(name)) throw new IOException("Duplicate ZIP entry: " + name);
                if (!entry.isDirectory() && relevant(name)) entries.put(name, zip.readAllBytes());
                zip.closeEntry();
            }
        }
        return from(entries);
    }

    private static boolean relevant(String name) {
        return name.endsWith(".tara") || name.equals("manifest.json") || name.startsWith("resources/");
    }

    private static ModelRelease from(Map<String, byte[]> entries) throws IOException {
        Map<String, String> sources = new TreeMap<>();
        Map<String, byte[]> resources = new TreeMap<>();
        String metadata = "";
        for (var entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("resources/")) resources.put(name.substring(10), entry.getValue());
            else if (name.equals("manifest.json")) metadata = new String(entry.getValue(), StandardCharsets.UTF_8);
            else if (name.endsWith(".tara")) sources.put(name, new String(entry.getValue(), StandardCharsets.UTF_8));
        }
        return new ModelRelease(sources, metadata, resources);
    }
}

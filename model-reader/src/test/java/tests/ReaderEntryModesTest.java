package tests;

import io.quassar.reader.Models;
import io.quassar.reader.ModelRelease;
import io.quassar.reader.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ReaderEntryModesTest {
    @TempDir Path home;
    private static final String TEXT = "dsl inventory\nProduct keyboard\n\tprice = 49.99\n";

    @Test void textAndFileProduceTheSameGenericModel() throws Exception {
        Path file = Files.writeString(home.resolve("main.tara"), TEXT);
        assertEquals(Models.create(TEXT).mograms(), Models.load(file).mograms());
        assertEquals("Product", Models.load(file).get("keyboard").type());
    }

    @Test void releaseRetainsMetadataAndBinaryResourcesWithoutParsingResourceTaraFiles() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("views/inventory/main.tara", TEXT.getBytes());
        entries.put("views/inventory/second.tara", "dsl inventory\nProduct mouse\n".getBytes());
        entries.put("resources/example.tara", "this is a resource".getBytes());
        entries.put("resources/icon.bin", new byte[]{0, -1, 3});
        entries.put("manifest.json", "{\"id\":\"inventory\"}".getBytes());
        for (var entry : entries.entrySet()) {
            Path file = home.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.write(file, entry.getValue());
        }
        Path zip = home.resolve("release.zip");
        Files.write(zip, archive(entries));
        var release = ModelRelease.open(zip);
        assertEquals(2, release.model().mograms().size());
        assertEquals(ModelRelease.open(home).model().mograms(), release.model().mograms());
        assertEquals("{\"id\":\"inventory\"}", release.metadata());
        try (var input = release.resource("icon.bin")) { assertArrayEquals(new byte[]{0,-1,3}, input.readAllBytes()); }
        assertThrows(FileNotFoundException.class, () -> release.resource("absent"));
        for (var model : List.of(Models.load(home), Models.load(zip), Models.load(zip.toUri()),
                Models.load(zip, "Concept Product\n\tdef double price"))) {
            assertEquals(Set.of("example.tara", "icon.bin"), model.resourceNames());
            assertEquals(List.of("example.tara", "icon.bin"), model.resources().stream().map(Resource::name).toList());
            Resource icon = model.resource("icon.bin");
            assertEquals("icon.bin", icon.name());
            byte[] content = icon.bytes();
            assertArrayEquals(new byte[]{0, -1, 3}, content);
            content[0] = 42;
            assertArrayEquals(new byte[]{0, -1, 3}, icon.bytes());
            try (var first = icon.open(); var second = icon.open()) {
                assertEquals(0, first.read());
                assertArrayEquals(new byte[]{0, -1, 3}, second.readAllBytes());
            }
            assertThrows(FileNotFoundException.class, () -> model.resource("absent"));
        }
    }

    @Test void malformedEmptyAndTraversalArchivesFail() throws Exception {
        assertThrows(IOException.class, () -> ModelRelease.read(new ByteArrayInputStream("invalid".getBytes())));
        assertThrows(IOException.class, () -> ModelRelease.read(new ByteArrayInputStream(archive(Map.of("manifest.json", "{}".getBytes())))));
        assertThrows(IOException.class, () -> ModelRelease.read(new ByteArrayInputStream(archive(Map.of("../escape.tara", TEXT.getBytes())))));
        assertFalse(Files.exists(home.resolve("escape.tara")));
    }

    private static byte[] archive(Map<String, byte[]> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue()); zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}

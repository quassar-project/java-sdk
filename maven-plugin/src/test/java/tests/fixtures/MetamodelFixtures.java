package tests.fixtures;

import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.PropertyType;
import io.quassar.metamodel.model.PropertyType.Kind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.quassar.metamodel.model.PropertyType.Kind.*;

public final class MetamodelFixtures {
	private MetamodelFixtures() {
	}

	public static Path libraryArchive(Path directory) throws IOException {
		Path zip = directory.resolve("library-m2.zip");
		try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
			for (String name : List.of("main.tara", "extensions.tara")) {
				output.putNextEntry(new ZipEntry("metamodel/" + name));
				try (InputStream input = MetamodelFixtures.class.getResourceAsStream("/m2/" + name)) {
					input.transferTo(output);
				}
				output.closeEntry();
			}
		}
		return zip;
	}

	public static Metamodel library() {
		return new Metamodel(List.of(
			type("Author",
				property("name", STRING)),
			type("Book", List.of("Digital", "Illustrated", "Printed", "Edited"), List.of("Chapter", "Edition", "Part", "Author"),
				property("year", NUMBER),
				property("available", BOOLEAN),
				property("rating", NUMBER),
				property("title", STRING),
				sequence("authors", REFERENCE, "Author"),
				property("publisher", REFERENCE, "Publisher"),
				property("related", REFERENCE, "Book"),
				sequence("tags", STRING),
				property("author", REFERENCE, "Author"),
				property("unseen", STRING),
				sequence("flags", BOOLEAN),
				sequence("prices", NUMBER),
				sequence("contributors", REFERENCE, "Author"),
				sequence("mixed", VALUE, "Author"),
				property("note", VALUE),
				sequence("subjects", REFERENCE, "Author", "Book"),
				property("link", REFERENCE, "Author", "Book"),
				property("unresolved", REFERENCE),
				positional(0, STRING),
				positional(1, NUMBER),
				positional(2, STRING),
				positional(3, BOOLEAN),
				positional(4, REFERENCE, "Author")),
			type("Chapter", List.of("Indexed"), List.of("Section"),
				property("number", NUMBER),
				property("title", STRING),
				property("previous", REFERENCE, "Chapter"),
				property("next", REFERENCE, "Chapter"),
				property("editor", REFERENCE, "Author"),
				positional(0, REFERENCE, "Index")),
			type("Citation",
				property("note", STRING),
				property("book", REFERENCE, "Book")),
			type("Collection",
				sequence("books", REFERENCE, "Book"),
				property("featured", REFERENCE, "Edition")),
			type("Edition", List.of("Limited", "Translated"), List.of("Note"),
				property("year", NUMBER),
				sequence("prices", NUMBER),
				property("source", REFERENCE, "Book")),
			type("Note",
				positional(0, STRING),
				positional(1, NUMBER),
				positional(2, BOOLEAN)),
			type("Paragraph",
				property("text", STRING)),
			type("Part", List.of(), List.of("Chapter"),
				property("number", NUMBER),
				property("title", STRING)),
			type("Publisher",
				property("name", STRING)),
			type("Section", List.of(), List.of("Citation", "Paragraph"),
				property("number", NUMBER)),
			facet("Digital",
				property("format", STRING)),
			facet("Illustrated"),
			facet("Limited",
				property("copies", NUMBER)),
			facet("Printed", List.of("Catalogued"),
				property("isbn", STRING),
				property("pages", NUMBER)),
			facet("Translated",
				property("language", STRING),
				property("translator", REFERENCE, "Author")),
			facet("Catalogued",
				property("catalogueId", STRING)),
			facet("Edited",
				property("editor", REFERENCE, "Author")),
			type("Index"),
			facet("Indexed")));
	}

	public static Metamodel names() {
		return new Metamodel(List.of(
			type("Book", List.of("Printed"), List.of("Chapter"),
				property("class", STRING),
				property("wait", STRING),
				property("mogram", STRING),
				property("chapters", STRING),
				property("identifier", STRING),
				property("getClass", STRING)),
			type("Chapter"),
			type("EmptyField",
				property("value", EMPTY)),
			type("String",
				property("value", STRING)),
			type("Mogram",
				property("value", STRING),
				property("java.math.BigDecimal", STRING)),
			type("book",
				property("title", STRING)),
			facet("Printed")));
	}

	private static MetamodelType type(String name, PropertyDefinition... properties) {
		return type(name, List.of(), List.of(), properties);
	}

	private static MetamodelType type(String name, List<String> facets, List<String> children, PropertyDefinition... properties) {
		return new MetamodelType(name, false, List.of(properties), facets, children);
	}

	private static MetamodelType facet(String name, PropertyDefinition... properties) {
		return facet(name, List.of(), properties);
	}

	private static MetamodelType facet(String name, List<String> facets, PropertyDefinition... properties) {
		return new MetamodelType(name, true, List.of(properties), facets, List.of());
	}

	private static PropertyDefinition property(String name, Kind kind, String... targets) {
		return new PropertyDefinition(name, -1, PropertyType.scalar(kind), List.of(targets));
	}

	private static PropertyDefinition sequence(String name, Kind kind, String... targets) {
		return new PropertyDefinition(name, -1, new PropertyType(SEQUENCE, kind), List.of(targets));
	}

	private static PropertyDefinition positional(int position, Kind kind, String... targets) {
		return new PropertyDefinition(null, position, PropertyType.scalar(kind), List.of(targets));
	}
}

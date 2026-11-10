package tests;

import io.quassar.metamodel.Metamodels;

import io.quassar.metamodel.domain.MetamodelReader;
import io.quassar.metamodel.model.Cardinality;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelManifest;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.PropertyType.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tests.fixtures.MetamodelFixtures;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static java.nio.charset.StandardCharsets.UTF_8;

class MetamodelReaderTest {
	@TempDir
	Path temporary;

	@Test
	void readsTypesFacetsAndInheritanceWithoutMixingTheirProperties() throws Exception {
		Metamodel model = load();
		MetamodelType book = type(model, "Book");
		assertThat(book.properties()).extracting(PropertyDefinition::name)
			.containsExactly("title", "authors", "available", "year", "subjects");
		assertThat(book.facets()).containsExactly("Book.Translated", "Book.Edition");
		MetamodelType printed = type(model, "Book.Edition.Printed");
		assertThat(printed.isFacet()).isTrue();
		assertThat(printed.parent()).isEqualTo("Book.Edition");
		assertThat(printed.properties()).extracting(PropertyDefinition::name).containsExactly("isbn", "length");
		assertThat(type(model, "Book.Novel").parent()).isEqualTo("Book");
		assertThat(type(model, "Book.Novel").isFacet()).isFalse();
		assertThat(type(model, "Author").role()).isEqualTo("component");
		assertThat(type(model, "Subject").role()).isEqualTo("generalization");
	}

	@Test
	void readsExplicitInheritanceAndTabStops() throws Exception {
		Metamodel model = load();
		assertThat(type(model, "Magazine").parent()).isEqualTo("Publication");
		assertThat(type(model, "Publication").role()).isEqualTo("component generalization");
		assertThat(type(model, "Magazine.Monthly").parent()).isEqualTo("Magazine");
		assertThat(type(model, "Magazine.Quarterly").parent()).isEqualTo("Magazine");
	}

	@Test
	void preservesScopesAndCompositions() throws Exception {
		Metamodel model = load();
		assertThat(type(model, "Book.Chapter").scope()).isEqualTo("Book");
		assertThat(type(model, "Journal.Chapter").scope()).isEqualTo("Journal");
		assertThat(type(model, "Book.Chapter").cardinality()).isEqualTo(new Cardinality(1, -1));
		assertThat(type(model, "Library.Catalogue").cardinality()).isEqualTo(new Cardinality(1, 1));
		assertThat(type(model, "Library.Catalogue").modifiers()).containsExactly("Single", "Required");
		assertThat(type(model, "Library").compositions()).anySatisfy(composition -> {
			assertThat(composition.type()).isEqualTo("Book");
			assertThat(composition.cardinality()).isEqualTo(new Cardinality(1, -1));
		});
		assertThat(type(model, "Journal").compositions()).anySatisfy(composition -> {
			assertThat(composition.type()).isEqualTo("Book");
			assertThat(composition.cardinality()).isEqualTo(new Cardinality(0, 1));
		});
	}

	@Test
	void preservesPropertyTypesConstraintsAndDefaults() throws Exception {
		Metamodel model = load();
		assertThat(property(model, "Book", "authors").type().kind()).isEqualTo(Kind.SEQUENCE);
		assertThat(property(model, "Book", "authors").type().element()).isEqualTo(Kind.REFERENCE);
		assertThat(property(model, "Book", "authors").referenceTypes()).containsExactly("Author");
		assertThat(property(model, "Book", "subjects").constraints()).containsExactly("Fiction", "History", "Science");
		assertThat(property(model, "Book", "subjects").type().element()).isEqualTo(Kind.STRING);
		assertThat(property(model, "Book", "available").type().kind()).isEqualTo(Kind.BOOLEAN);
		assertThat(property(model, "Book", "available").defaultValue()).isEqualTo("true");
		assertThat(property(model, "Book", "year").declaredType()).isEqualTo("integer");
		assertThat(property(model, "Book", "year").defaultValue()).isEqualTo("empty");
		assertThat(property(model, "Book", "title").hasDefault()).isFalse();
		assertThat(property(model, "Book.Edition.Printed", "length").constraints()).containsExactly("pages");
		assertThat(property(model, "Book.Edition.Printed", "length").defaultValue()).isEqualTo("120 pages");
		assertThat(property(model, "Book.Edition.Digital", "size").type().kind()).isEqualTo(Kind.NUMBER);
		assertThat(property(model, "Journal", "opening").referenceTypes()).containsExactly("Book.Chapter");
	}

	@Test
	void readsInlineDeclarationsAndQuotedSeparators() throws Exception {
		Metamodel model = load();
		assertThat(type(model, "Book.Chapter.Index").properties()).extracting(PropertyDefinition::name).containsExactly("min", "max");
		assertThat(property(model, "Book.Novel", "synopsis").defaultValue()).isEqualTo("\"A story > another; // not a comment\"");
		assertThat(property(model, "Library.Catalogue", "endpoint").defaultValue()).isEqualTo("\"https://library.example/catalogue?a=b\"");
		assertThat(type(model, "Book.Chapter.Indexed").isFacet()).isTrue();
	}

	@Test
	void rejectsAnIndividualTaraFile() throws Exception {
		var url = getClass().getResource("/m2/main.tara");
		assertThatThrownBy(() -> Metamodels.load(Path.of(url.toURI()))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> Metamodels.load(url)).isInstanceOf(IOException.class);
	}

	@Test
	void loadsAFolderFromAPathOrFileUrl() throws Exception {
		Path folder = Path.of(getClass().getResource("/m2/").toURI());
		Metamodel archive = Metamodels.load(MetamodelFixtures.libraryArchive(temporary));
		assertThat(Metamodels.load(folder)).isEqualTo(archive);
		assertThat(Metamodels.load(folder.toUri().toURL())).isEqualTo(archive);
	}

	@Test
	void loadsAZipFromAPathOrAnExtensionlessUrl() throws Exception {
		Path zip = MetamodelFixtures.libraryArchive(temporary);
		Metamodel local = Metamodels.load(zip);
		Path endpoint = Files.createDirectory(temporary.resolve("models")).resolve("rHC2rto");
		Files.copy(zip, endpoint);
		assertThat(local.types()).extracting(MetamodelType::name).contains("Book", "Journal.Chapter");
		assertThat(Metamodels.load(endpoint.toUri().toURL())).isEqualTo(local);
		assertThat(Metamodels.load(endpoint.toUri())).isEqualTo(local);
	}

	@Test
	void normalizesMetamodelFilesBeforeReadingTheirDeclarations() throws Exception {
		Path zip = temporary.resolve("metamodel.zip");
		Path folder = Files.createDirectory(temporary.resolve("metamodel folder"));
		Map<String, String> files = Map.of(
			"main.tara", "\uFEFFdsl metta\nConcept Book\n\tdef string title\n",
			"nested/author.tara", "dsl Metta\nConcept Author as component > def string name\n",
			"manifest.json", Files.readString(Path.of(getClass().getResource("/maven/consumer/metamodel/manifest.json").toURI())),
			"ignore.txt", "Concept Ignored");
		try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
			for (var file : files.entrySet()) {
				Path path = folder.resolve(file.getKey());
				Files.createDirectories(path.getParent());
				Files.writeString(path, file.getValue());
				output.putNextEntry(new ZipEntry(file.getKey()));
				output.write(file.getValue().getBytes(UTF_8));
				output.closeEntry();
			}
		}
		Metamodel model = Metamodels.load(zip);
		assertThat(Metamodels.load(folder.toUri())).isEqualTo(model);
		assertThat(Metamodels.load(zip.toUri())).isEqualTo(model);
		assertThat(model.manifest()).hasValue(new MetamodelManifest(
			"liCoJN5b", "Library", "1.0.0", "86bca6a8-0fe0-4e55-8a6a-2880e24d606a", "metta:2.9.4", "library@example.com"));
		assertThat(model.types()).extracting(MetamodelType::name).containsExactly("Book", "Author");
		assertThat(property(model, "Book", "title").type().kind()).isEqualTo(Kind.STRING);
		assertThat(property(model, "Author", "name").type().kind()).isEqualTo(Kind.STRING);
	}

	@Test
	void leavesInputOpenAndReturnsImmutableCollections() throws Exception {
		boolean[] closed = {false};
		InputStream input = new ByteArrayInputStream("Concept Book\n\tdef string title\n".getBytes()) {
			@Override
			public void close() throws IOException {
				closed[0] = true;
				super.close();
			}
		};
		Metamodel model = new MetamodelReader().read(input);
		assertThat(closed[0]).isFalse();
		assertThatThrownBy(() -> model.types().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> model.types().get(0).properties().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	private Metamodel load() throws Exception {
		return Metamodels.load(MetamodelFixtures.libraryArchive(temporary));
	}

	private MetamodelType type(Metamodel model, String name) {
		return model.types().stream().filter(type -> type.name().equals(name)).findFirst().orElseThrow();
	}

	private PropertyDefinition property(Metamodel model, String type, String name) {
		return type(model, type).properties().stream().filter(property -> property.name().equals(name)).findFirst().orElseThrow();
	}
}

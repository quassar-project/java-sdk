package tests;

import io.quassar.javacode.MogramDecoratorGenerator;
import io.quassar.metamodel.Metamodels;
import io.quassar.templating.Frames;
import tests.fixtures.GeneratedDecorators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.quassar.reader.domain.ModelReader;
import io.quassar.reader.model.Model;
import io.quassar.reader.model.Mogram;
import io.quassar.reader.model.Value;
import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.Metamodel;
import tests.fixtures.MetamodelFixtures;
import io.quassar.reader.adapters.TextSource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecoratorGeneratorTest {
	@TempDir
	Path directory;

	@Test
	void compilesDecoratorsFromANativeMetamodel() throws Exception {
		Metamodel metamodel = Metamodels.load(MetamodelFixtures.libraryArchive(directory));
		Model model = model("observations.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			assertThat(call(book, "title")).isEqualTo("First book");
			assertThat(call(book, "year")).isEqualTo(new BigDecimal("2020"));
		}
	}

	@Test
	void generatesSingularGettersForSingleRootsAndChildren() throws Exception {
		Metamodel metamodel = Metamodels.load(MetamodelFixtures.libraryArchive(directory));
		Model model = model("cardinality.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object facade = generated.modelDecorator();
			assertThat(facade.getClass().getMethods()).extracting(Method::getName)
				.doesNotContain("libraryCatalogues", "bookChapters", "journalChapters");
			assertThat((List<?>) call(facade, "books")).isEmpty();
			Object library = call(facade, "library");
			assertThat(library.getClass().getSimpleName()).isEqualTo("Library");
			assertThat(call(library, "identifier")).isEqualTo("Central");
			Object catalogue = call(library, "catalogue");
			assertThat(catalogue.getClass().getSimpleName()).isEqualTo("LibraryCatalogue");
			assertThat(call(catalogue, "endpoint")).isEqualTo("https://library.example/catalogue");
			assertThat((List<?>) call(library, "books")).hasSize(2);
			assertThat(Frames.from(facade).frames("library").next().frames("catalogue").hasNext()).isTrue();

			Object journal = generated.wrap("Journal", model.get("Review"));
			assertThat(call(call(journal, "book"), "identifier")).isEqualTo("Featured");
			var review = Frames.from(journal);
			var opening = review.frames("opening").next();
			assertThat(opening.frames("title").next().value()).isEqualTo("First chapter");
			assertThat(opening.container().frames("identifier").next().value()).isEqualTo("First");
			assertThat(opening.container().frames("chapter").next()).isSameAs(opening);
			assertThat(review.frames("chapter").next().frames("reviewed").next().value()).isEqualTo(true);
			Object empty = generated.wrap("Journal", model.get("Empty"));
			assertThat(call(call(empty, "book"), "isNull")).isEqualTo(true);
		}
	}

	@Test
	void exposesComponentsThroughTheirParentOnly() throws Exception {
		Metamodel metamodel = Metamodels.load(Path.of(getClass().getResource("/metamodels/printed-book/").toURI()));
		Model model = model("printed-books.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object facade = generated.modelDecorator();
			assertThat(facade.getClass().getMethods()).extracting(Method::getName).doesNotContain("chapters");
			List<?> books = (List<?>) call(facade, "books");
			List<?> chapters = (List<?>) call(books.get(0), "chapters");
			assertThat(chapters).hasSize(1);
			assertThat(call(chapters.get(0), "identifier")).isEqualTo("Opening");
		}
	}

	@Test
	void exposesScopedFacetsAsTypedDecorators() throws Exception {
		Metamodel metamodel = Metamodels.load(Path.of(getClass().getResource("/metamodels/printed-book/").toURI()));
		Model model = model("printed-books.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("Paper"));
			assertThat(book.getClass().getMethods()).extracting(Method::getName).doesNotContain("isbn");
			assertThat(call(book, "isPrinted")).isEqualTo(true);
			Object printed = call(book, "asPrinted");
			assertThat(printed.getClass().getMethods()).extracting(Method::getName).doesNotContain("title");
			assertThat(printed.getClass().getSimpleName()).isEqualTo("Printed");
			assertThat(call(printed, "isbn")).isEqualTo("9780000000001");
			assertThat(call(printed, "identifier")).isEqualTo("Paper");

			Object digital = generated.wrap("Book", model.get("Digital"));
			assertThat(call(digital, "isPrinted")).isEqualTo(false);
			Object missing = call(digital, "asPrinted");
			assertThat(call(missing, "isNull")).isEqualTo(true);
			assertThat(call(missing, "isbn")).isEqualTo("");
		}
	}

	@Test
	void readsPropertiesWithoutCastsOrExposingRenderingDetails() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			assertThat(book.getClass().getMethods()).extracting(Method::getName)
				.doesNotContain("mogram", "model", "frameView", "frameSlots", "frameIdentity", "frameTypes", "frameParent", "of", "values", "groupSlots");
			assertThat(book.getClass().getConstructors()).isEmpty();
			assertThat(call(book, "title")).isEqualTo("First book");
			assertThat(call(book, "year")).isEqualTo(new BigDecimal("2020"));
			assertThat(call(book, "available")).isEqualTo(true);
			assertThat(call(call(book, "author"), "identifier")).isEqualTo("Lucia");
			assertThat(call(call(book, "asPrinted"), "isbn")).isEqualTo("9780000000001");
			assertThat(call(book, "isPrinted")).isEqualTo(true);
			assertThat(call(book, "isDigital")).isEqualTo(false);
		}
	}

	@Test
	void readsTypedAndMixedSequences() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			assertThat(call(book, "tags")).isEqualTo(List.of("novel", "travel"));
			assertThat(call(book, "prices")).isEqualTo(List.of(new BigDecimal("19.95"), new BigDecimal("24.50")));
			List<?> contributors = (List<?>) call(book, "contributors");
			assertThat(call(contributors.get(0), "identifier")).isEqualTo("Lucia");
			assertThat(call(contributors.get(1), "identifier")).isEqualTo("Mateo");
			assertThat(call(book, "mixed")).isEqualTo(model.get("FirstBook").property("mixed").asSequence());
		}
	}

	@Test
	void usesTheMetamodelForAbsentPropertiesAndSingleValuedSequences() throws Exception {
		Model model = model("single-values.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("Single"));
			assertThat(call(book, "unseen")).isEqualTo("");
			assertThat(call(call(book, "asPrinted"), "pages")).isEqualTo(BigDecimal.ZERO);
			assertThat(call(book, "tags")).isEqualTo(List.of("novel"));
			assertThat(call(book, "flags")).isEqualTo(List.of(true));
			List<?> authors = (List<?>) call(book, "authors");
			assertThat(authors).hasSize(1);
			assertThat(call(authors.get(0), "identifier")).isEqualTo("Lucia");
			Object missing = generated.wrap("Book", Mogram.NULL);
			assertThat(call(missing, "authors")).isEqualTo(List.of());
			assertThat(call(missing, "flags")).isEqualTo(List.of());
		}
	}

	@Test
	void wrapsChildrenInTheirOwnDecorators() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			List<?> chapters = (List<?>) call(book, "chapters");
			assertThat(chapters).hasSize(1);
			assertThat(chapters.get(0).getClass().getSimpleName()).isEqualTo("Chapter");
			assertThat(call(chapters.get(0), "identifier")).isEqualTo("Opening");
			assertThat(call(chapters.get(0), "number")).isEqualTo(BigDecimal.ONE);
		}
	}

	@Test
	void preservesNullMogramAndMissingPropertyDefaults() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object missing = generated.wrap("Book", Mogram.NULL);
			assertThat(call(missing, "isNull")).isEqualTo(true);
			assertThat(call(missing, "title")).isEqualTo("");
			assertThat(call(missing, "year")).isEqualTo(BigDecimal.ZERO);
			assertThat(call(missing, "available")).isEqualTo(false);
			assertThat(call(missing, "chapters")).isEqualTo(List.of());
			Object second = generated.wrap("Book", model.get("SecondBook"));
			assertThat(call(call(second, "asPrinted"), "isbn")).isEqualTo("");
			assertThat(call(call(second, "author"), "isNull")).isEqualTo(true);
		}
	}

	@Test
	void preservesPropertiesExplicitlyDeclaredAsValue() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object first = generated.wrap("Book", model.get("FirstBook"));
			Object second = generated.wrap("Book", model.get("SecondBook"));
			assertThat(call(first, "note")).isEqualTo(new Value.Text("A textual note"));
			assertThat(call(second, "note")).isEqualTo(new Value.Number(new BigDecimal("7")));
		}
	}

	@Test
	void numbersUnnamedPropertiesWithoutInventingNames() throws Exception {
		Model model = model("positional.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object chapter = generated.wrap("Chapter", model.get("Glossary"));
			assertThat(call(call(chapter, "value1"), "identifier")).isEqualTo("Subjects");
			assertThat(call(chapter, "isIndexed")).isEqualTo(true);
			Object note = generated.wrap("Note", model.get("Annotation"));
			assertThat(call(note, "value1")).isEqualTo("Opening note");
			assertThat(call(note, "value2")).isEqualTo(new BigDecimal("3"));
			assertThat(call(note, "value3")).isEqualTo(true);
		}
	}

	@Test
	void compilesJavaKeywordsAndCollidingNames() throws Exception {
		Model model = model("names.tara");
		Metamodel metamodel = MetamodelFixtures.names();
		assertThat(MogramDecoratorGenerator.from(metamodel).inPackage("books").code())
			.filteredOn(source -> source.className().equals("Mogram"))
			.singleElement().extracting(JavaCode::content).satisfies(content -> assertThat(content)
				.contains("io.quassar.reader.model.Mogram mogram", "mogram.property(\"java.math.BigDecimal\")")
				.doesNotContain("import io.quassar.reader.model.Mogram;", "import java.math.BigDecimal;"));
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("Named"));
			assertThat(call(book, "classValue")).isEqualTo("edition");
			assertThat(call(book, "wait2")).isEqualTo("ready");
			assertThat(call(book, "mogram2")).isEqualTo("source");
			assertThat(call(book, "identifier2")).isEqualTo("label");
			assertThat(call(book, "getClass2")).isEqualTo("kind");
			assertThat((List<?>) call(book, "chapters2")).hasSize(1);
			var frame = Frames.from(book);
			assertThat(frame.frames("identifier").next().value()).isEqualTo("label");
			assertThat(frame.frames("chapters").next().value()).isEqualTo("index");
			assertThat(frame.frames("chapter").next().container()).isSameAs(frame);
			assertThat(call(generated.wrap("Book2", model.get("Lowercase")), "title")).isEqualTo("Lowercase type");
			assertThat(call(generated.wrap("String", model.get("TextBox")), "value")).isEqualTo("A string-like domain type");
			assertThat(call(generated.wrap("Mogram", model.get("Raw")), "value")).isEqualTo("io.quassar.reader.model.Mogram");
			assertThat(call(generated.wrap("EmptyField", model.get("Unknown")), "value")).isInstanceOf(Value.Empty.class);
		}
	}

	@Test
	void generatesDeterministicSourcesWithoutWritingFiles() throws Exception {
		Model model = model("critical-edition.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		MogramDecoratorGenerator generator = MogramDecoratorGenerator.from(metamodel).inPackage("books");
		List<JavaCode> code = generator.code();
		assertThat(generator.code()).isEqualTo(code);
		assertThat(code).extracting(JavaCode::className).contains("Book", "Chapter", "Author", "Note", "Edition");
		assertThat(code).allMatch(source -> source.packageName().equals("books"));
		assertThat(code).allSatisfy(source -> assertThat(source.content())
			.doesNotContain("io.quassar.templating.annotations", "FRAME_CHILDREN", "FRAME_PROPERTIES",
				"frameChildren()", "frameProperties()"));
		assertThat(code).filteredOn(source -> source.className().equals("Book"))
			.singleElement().extracting(JavaCode::content).satisfies(content -> assertThat(content)
				.contains("import io.quassar.reader.model.Mogram;", "import io.quassar.reader.model.Model;",
					"import java.util.List;", "public final class Book extends MogramDecorator", "public String title()")
				.doesNotContain("java.lang.String", "io.quassar.reader.model.Mogram mogram"));
	}

	@Test
	void supportsPositionalNamedAndBodyDeclarations() throws Exception {
		Model model = model("property-syntax.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object positional = generated.wrap("Book", model.get("Positional"));
			assertThat(call(positional, "value1")).isEqualTo("Atlas");
			assertThat(call(positional, "value2")).isEqualTo(new BigDecimal("2024"));
			assertThat(call(positional, "value3")).isEqualTo("9780000000001");
			assertThat(call(positional, "value4")).isEqualTo(true);
			assertThat(call(call(positional, "value5"), "identifier")).isEqualTo("Lucia");
			for (String identifier : List.of("Named", "Body")) {
				Object named = generated.wrap("Book", model.get(identifier));
				assertThat(call(named, "title")).isEqualTo("Atlas");
				assertThat(call(named, "year")).isEqualTo(new BigDecimal("2024"));
				assertThat(call(call(named, "asPrinted"), "isbn")).isEqualTo("9780000000001");
				assertThat(call(named, "available")).isEqualTo(true);
				assertThat(call(call(call(named, "asEdited"), "editor"), "identifier")).isEqualTo("Lucia");
				assertThat(call(named, "isPrinted")).isEqualTo(true);
				assertThat(call(named, "isEdited")).isEqualTo(true);
			}
		}
	}

	@Test
	void returnsConcreteDecoratorsAndPropagatesContextToChildren() throws Exception {
		Model model = model("references.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			assertThat(book.getClass().getMethod("author").getReturnType().getSimpleName()).isEqualTo("Author");
			assertThat(book.getClass().getMethod("publisher").getReturnType().getSimpleName()).isEqualTo("Publisher");
			Object author = call(book, "author");
			assertThat(call(author, "name")).isEqualTo("Lucia Vega");
			assertThat(call(call(book, "publisher"), "name")).isEqualTo("North Press");
			List<?> chapters = (List<?>) call(book, "chapters");
			assertThat(call(call(chapters.get(0), "editor"), "name")).isEqualTo("Local editor");
			assertThat(call(chapters.get(1), "previous")).isEqualTo(chapters.get(0));
		}
	}

	@Test
	void decoratesReferenceSequencesIncludingHeterogeneousTargets() throws Exception {
		Model model = model("references.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			List<?> authors = (List<?>) call(book, "contributors");
			assertThat(authors).hasSize(2);
			assertThat(authors.get(0).getClass().getSimpleName()).isEqualTo("Author");
			assertThat(call(authors.get(1), "name")).isEqualTo("Mateo Sol");
			List<?> subjects = (List<?>) call(book, "subjects");
			assertThat(subjects).extracting(value -> value.getClass().getSimpleName()).containsExactly("Author", "Book");
			assertThat(call(subjects.get(1), "identifier")).isEqualTo("SecondBook");
		}
	}

	@Test
	void handlesCircularNullAndPolymorphicReferencesLazily() throws Exception {
		Model model = model("references.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object first = generated.wrap("Book", model.get("FirstBook"));
			Object second = call(first, "related");
			assertThat(call(second, "related")).isEqualTo(first);
			Object missingAuthor = call(second, "author");
			assertThat(call(missingAuthor, "isNull")).isEqualTo(true);
			assertThat(call(missingAuthor, "name")).isEqualTo("");
			assertThat(call(first, "link").getClass().getSimpleName()).isEqualTo("Author");
			assertThat(call(second, "link").getClass().getSimpleName()).isEqualTo("Book");
			assertThat(first.getClass().getMethod("link").getReturnType().getSimpleName()).isEqualTo("MogramDecorator");
			assertThat(call(call(first, "unresolved"), "isNull")).isEqualTo(true);
		}
	}

	@Test
	void generatesATypedModelFacadeWithContextAwareDecorators() throws Exception {
		Model model = model("critical-edition.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object library = generated.loader().loadClass("test.generated.LibraryModel").getConstructor(Model.class).newInstance(model);
			assertThat(library.getClass().getMethods()).extracting(Method::getName)
				.doesNotContain("model", "frameView", "frameSlots", "frameIdentity", "frameTypes", "frameParent");
			List<?> books = (List<?>) call(library, "books");
			assertThat(books).hasSize(2);
			Object atlas = books.get(0);
			assertThat(call(atlas, "identifier")).isEqualTo("Atlas");
			assertThat(call(call(atlas, "publisher"), "name")).isEqualTo("North Press");
			assertThat((List<?>) call(library, "chapters")).isEmpty();
			assertThat((List<?>) call(library, "authors")).hasSize(2);
			assertThat((List<?>) call(library, "notes")).isEmpty();
			assertThat(call(books.get(1), "identifier")).isEqualTo("Nocturne");
		}
	}

	@Test
	void loadsModelsDirectlyThroughTheGeneratedFacade() throws Exception {
		Model schema = model("critical-edition.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		Path folder = Files.createDirectories(directory.resolve("m1"));
		Path tara = folder.resolve("books.tara");
		try (InputStream input = getClass().getResourceAsStream("/m1/critical-edition.tara")) {
			Files.copy(input, tara);
		}
		Path zip = directory.resolve("library.zip");
		try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
			output.putNextEntry(new java.util.zip.ZipEntry("m1/books.tara"));
			Files.copy(tara, output);
			output.closeEntry();
		}
		try (GeneratedDecorators generated = compile(schema, metamodel)) {
			Class<?> facade = generated.loader().loadClass("test.generated.LibraryModel");
			for (Object source : List.of(folder, zip, zip.toUri(), zip.toUri().toURL())) {
				Class<?> sourceType = source instanceof Path ? Path.class : source.getClass();
				Object library = facade.getMethod("load", sourceType).invoke(null, source);
				List<?> books = (List<?>) call(library, "books");
				assertThat(books).hasSize(2);
				assertThat(call(books.get(0), "identifier")).isEqualTo("Atlas");
				assertThat(call(call(books.get(0), "publisher"), "name")).isEqualTo("North Press");
			}
		}
	}

	@Test
	void modelFacadeReturnsEmptyListsForAnEmptyModel() throws Exception {
		Model schema = model("critical-edition.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		try (GeneratedDecorators generated = compile(schema, metamodel)) {
			Model empty = new Model(List.of());
			Object library = generated.loader().loadClass("test.generated.LibraryModel").getConstructor(Model.class).newInstance(empty);
			assertThat(call(library, "books")).isEqualTo(List.of());
			assertThat(call(library, "chapters")).isEqualTo(List.of());
		}
	}

	@Test
	void modelFacadeNameIsConfigurableAndDoesNotChangeEarlierConfiguration() throws Exception {
		Model model = model("observations.tara");
		Metamodel metamodel = MetamodelFixtures.library();
		MogramDecoratorGenerator defaults = MogramDecoratorGenerator.from(metamodel).inPackage("books");
		MogramDecoratorGenerator library = defaults.modelNamed("LibraryModel");
		assertThat(defaults.code()).extracting(JavaCode::className).contains("DecoratedModel").doesNotContain("LibraryModel");
		assertThat(library.code()).extracting(JavaCode::className).contains("LibraryModel").doesNotContain("DecoratedModel");
	}

	private GeneratedDecorators compile(Model model, Metamodel metamodel) throws Exception {
		return GeneratedDecorators.compile(model, metamodel, directory);
	}

	private static Object call(Object decorator, String method) throws Exception {
		return decorator.getClass().getMethod(method).invoke(decorator);
	}

	private static Model model(String name) throws Exception {
		Path path = Path.of(DecoratorGeneratorTest.class.getResource("/m1/" + name).toURI());
		String text = Files.readString(path);
		try (InputStream content = new TextSource(List.of(text)).open()) {
			return new ModelReader().read(content);
		}
	}

}

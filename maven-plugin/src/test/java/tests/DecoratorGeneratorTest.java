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
import io.quassar.reader.model.Node;
import io.quassar.reader.model.Resource;
import io.quassar.reader.model.Value;
import io.quassar.javacode.model.JavaCode;
import io.quassar.metamodel.model.Metamodel;
import io.quassar.metamodel.model.MetamodelManifest;
import tests.fixtures.MetamodelFixtures;
import io.quassar.reader.adapters.TextSource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecoratorGeneratorTest {
	@TempDir
	Path directory;

	@Test
	void castsCommandsDirectlyToTheirConcreteSubtype() throws Exception {
		Metamodel metamodel = Metamodels.load(Path.of(getClass().getResource("/metamodels/scoped-types/").toURI()));
		Model model = model("scoped-types.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object toolbar = generated.wrap("Toolbar", model.get("MainToolbar"));
			List<?> commands = (List<?>) call(toolbar, "commands");
			assertThat(commands).hasSize(3);
			Object grouping = generated.as(commands.get(0), "Grouping");
			assertThat(((Node) commands.get(0)).is("CollectionCommand")).isEqualTo(true);
			assertThat(((Node) commands.get(0)).is("Grouping")).isEqualTo(true);
			assertThat(grouping.getClass().getCanonicalName()).isEqualTo("test.generated.Grouping");
			assertThat(call(grouping, "label")).isEqualTo("By status");
			assertThat(grouping.getClass().getSuperclass().getSimpleName()).isEqualTo("CollectionCommand");
			assertThat(commands.get(0).getClass().isInstance(grouping)).isTrue();
			assertThat(call(grouping, "enabled")).isEqualTo(true);
			assertThat(call(grouping, "isNotNull")).isEqualTo(true);
			assertThat(call(grouping, "isNotNull2")).isEqualTo(false);
			assertThat(call(commands.get(0), "types")).isEqualTo(List.of(
				"Grouping", "CollectionCommand", "Command",
				"Command.Deferred", "Command.Scheduling"));
			assertThat(call(grouping, "types")).isEqualTo(call(commands.get(0), "types"));
			assertThat(call(generated.as(commands.get(0), "Command.Deferred"), "types")).isEqualTo(call(grouping, "types"));
			var frame = Frames.from(grouping);
			for (String type : List.of("Grouping", "CollectionCommand", "Command", "Deferred", "Scheduling", "Command.Deferred"))
				assertThat(frame.is(type)).as("frame type %s", type).isTrue();
			assertThat(frame.is("Toolbar")).isFalse();
			assertThat(call(generated.as(commands.get(2), "Grouping"), "types")).isEqualTo(List.of());
			assertThat(grouping.getClass().getMethod("limit").getReturnType()).isEqualTo(int.class);
			assertThat(call(grouping, "limit")).isEqualTo(3);
			assertThat((List<?>) call(grouping, "thresholds")).isEqualTo(List.of(2, 5));
			assertThat(call(generated.as(commands.get(1), "Grouping"), "label")).isEqualTo("Qualified");
			assertThat(((Node) commands.get(2)).is("Grouping")).isEqualTo(false);
			Object missing = generated.as(commands.get(2), "Grouping");
			assertThat(call(missing, "isNull")).isEqualTo(true);
			assertThat(call(missing, "isNotNull")).isEqualTo(false);
		}
	}

	@Test
	void generatesTypedAccessorsForNestedTypesNamedByShortPaths() throws Exception {
		Metamodel metamodel = Metamodels.load(Path.of(getClass().getResource("/metamodels/scoped-types/").toURI()));
		Model model = model("scoped-types.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object view = generated.wrap("View", model.get("Main"));
			assertThat(((Node) view).is("ViewType")).isEqualTo(true);
			Object viewType = generated.as(view, "View.ViewType");
			assertThat(((Node) viewType).is("Projection")).isEqualTo(true);
			assertThat(((Node) viewType).is("Container")).isEqualTo(true);
			Object container = generated.as(viewType, "View.Container");
			assertThat(view.getClass().getMethods()).extracting(Method::getName)
				.contains("as", "is").doesNotContain("asContainer", "asProjection", "asViewType", "isContainer", "isProjection", "isViewType");
			assertThat(((Node) view).is("Container")).isEqualTo(true);
			assertThat(generated.as(view, "View.Container")).isEqualTo(container);
			assertThat(((Node) view).is(container.getClass().asSubclass(Node.class))).isTrue();
			assertThat(((Node) view).is("View.Container")).isTrue();
			assertThat(((Node) view).is("Other.Container")).isFalse();
			assertThat(((Node) view).is(Node.class)).isTrue();
			assertThat(generated.as(view, "View.Reference").is("Reference")).isFalse();
			assertThat(((Node) view).is("Projection")).isEqualTo(true);
			assertThat(((Node) view).is("Reference")).isEqualTo(false);
			assertThat(call(generated.as(view, "View.Reference"), "isNull")).isEqualTo(true);
			assertThat(view.getClass().getMethods()).extracting(Method::getName).contains("is").doesNotContain("asShared", "isShared");
			assertThat(viewType.getClass().getMethods()).extracting(Method::getName).contains("is").doesNotContain("asShared", "isShared");
			assertThat(container.getClass().getCanonicalName()).isEqualTo("test.generated.View.Container");
			var types = List.of("View", "View.Container", "View.Projection",
				"View.ViewType", "View.Inherited");
			assertThat(call(view, "types")).isEqualTo(types);
			assertThat(call(container, "types")).isEqualTo(types);
			Object remote = generated.wrap("RemoteView", model.get("Remote"));
			assertThat(call(remote, "types")).isEqualTo(List.of("RemoteView", "View", "View.Inherited"));
			Object composition = call(container, "composition");
			assertThat(composition).isInstanceOf(Enum.class).hasToString("Tabs");
			assertThat(composition.getClass().getEnclosingClass()).isEqualTo(container.getClass());
			assertThat((List<?>) call(container, "modes")).extracting(Object::toString).containsExactly("One", "DirectUpdate");
			Object defaultContainer = generated.as(generated.wrap("View", model.get("DefaultContainer")), "View.Container");
			assertThat(call(defaultContainer, "composition")).hasToString("Stack");
			Object plainContainer = generated.as(generated.wrap("View", model.get("PlainView")), "View.Container");
			assertThat(call(plainContainer, "composition")).isNull();
			assertThat(viewType.getClass().isInstance(container)).isTrue();
			assertThat((List<?>) call(container, "panels")).hasSize(1);
			assertThat(((Node) generated.as(viewType, "View.Projection")).is("Container")).isEqualTo(true);
			Object inheritedView = generated.as(view, "View.Inherited");
			Object inheritedDashboard = generated.as(generated.wrap("Dashboard", model.get("Admin")), "Dashboard.Inherited");
			assertThat(inheritedView.getClass().getCanonicalName()).isEqualTo("test.generated.View.Inherited");
			assertThat(inheritedDashboard.getClass().getCanonicalName()).isEqualTo("test.generated.Dashboard.Inherited");
			assertThat(call(inheritedView, "label")).isEqualTo("View label");
			assertThat(call(inheritedDashboard, "caption")).isEqualTo("Dashboard caption");
			Object header = generated.wrap("Header", model.get("Heading"));
			assertThat(header.getClass().getSuperclass().getSimpleName()).isEqualTo("Widget");
			assertThat(call(header, "types")).isEqualTo(List.of("Header", "Widget"));
			assertThat((List<?>) call(generated.modelDecorator(), "headers")).hasSize(1);
			Object widget = generated.wrap("Widget", model.get("Summary"));
			Object target = call(widget, "view");
			assertThat(target.getClass().getCanonicalName()).isEqualTo("test.generated.View.Projection");
			assertThat(call(target, "identifier")).isEqualTo("Main");
			Object panel = ((List<?>) call(target, "panels")).get(0);
			assertThat(panel.getClass().getCanonicalName()).isEqualTo("test.generated.View.Projection.Panel");
			assertThat(call(panel, "identifier")).isEqualTo("SummaryPanel");
			Object preview = generated.wrap("Preview", model.get("AdminPreview"));
			Object dashboard = call(preview, "view");
			assertThat(dashboard.getClass().getCanonicalName()).isEqualTo("test.generated.Dashboard.Projection");
			assertThat(call(dashboard, "identifier")).isEqualTo("Admin");
			Object commands = call(target, "commands");
			Node command = (Node) call(commands, "grouping");
			var asMethod = Node.class.getMethod("as", Class.class);
			assertThat(asMethod.invoke(view, container.getClass())).isEqualTo(container);
			assertThat(((Node) asMethod.invoke(command, view.getClass())).isNull()).isTrue();
			var ancestorMethod = Node.class.getMethod("ancestor", Class.class);
			assertThat(ancestorMethod.invoke(command, commands.getClass())).isEqualTo(commands);
			assertThat(ancestorMethod.invoke(command, view.getClass())).isEqualTo(view);
			assertThat(ancestorMethod.invoke(command, container.getClass())).isEqualTo(container);
			Class<?> toolbar = generated.loader().loadClass("test.generated.Toolbar");
			Node missingAncestor = (Node) ancestorMethod.invoke(command, toolbar);
			assertThat(missingAncestor.getClass()).isEqualTo(toolbar);
			assertThat(missingAncestor.isNull()).isTrue();
			assertThat(((Node) ancestorMethod.invoke(view, view.getClass())).isNull()).isTrue();
			Object grouping = call(commands, "grouping");
			assertThat(grouping.getClass().getCanonicalName())
				.isEqualTo("test.generated.Grouping");
			assertThat(call(grouping, "label")).isEqualTo("Group by status");
		}
	}

	@Test
	void compilesDecoratorsFromANativeMetamodel() throws Exception {
		Metamodel metamodel = Metamodels.load(MetamodelFixtures.libraryArchive(directory));
		Model model = model("observations.tara");
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object book = generated.wrap("Book", model.get("FirstBook"));
			assertThat(call(book, "title")).isEqualTo("First book");
			assertThat(call(book, "year")).isEqualTo(2020);
		}
	}

	@Test
	void resolvesResourcePropertiesByTheirBundledNames() throws Exception {
		var metamodel = Metamodels.load(MetamodelFixtures.libraryArchive(directory));
		Model model = model("resource-properties.tara").withResources(java.util.Map.of(
			"images/cover.bin", new byte[]{1, 2}, "appendix.bin", new byte[]{3}));
		try (GeneratedDecorators generated = compile(model, metamodel)) {
			Object atlas = generated.as(generated.wrap("Book", model.get("Atlas")), "Book.Digital");
			assertThat(atlas.getClass().getMethod("file").getReturnType()).isEqualTo(Resource.class);
			assertThat(call(atlas, "file")).isSameAs(model.resource("images/cover.bin"));
			assertThat((List<?>) call(atlas, "attachments")).isEqualTo(
				List.of(model.resource("images/cover.bin"), model.resource("appendix.bin")));
			Object missing = generated.as(generated.wrap("Book", model.get("Missing")), "Book.Digital");
			assertThat(call(missing, "file")).isNull();
			Object empty = generated.as(generated.wrap("Book", model.get("Empty")), "Book.Digital");
			assertThat(call(empty, "file")).isNull();
			assertThat((List<?>) call(empty, "attachments")).isEmpty();
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
			assertThat(catalogue.getClass().getCanonicalName()).isEqualTo("test.generated.Library.Catalogue");
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
			assertThat(((Node) book).is("Printed")).isEqualTo(true);
			Object printed = generated.as(book, "Book.Printed");
			assertThat(printed.getClass().getMethods()).extracting(Method::getName).doesNotContain("title");
			assertThat(printed.getClass().getSimpleName()).isEqualTo("Printed");
			assertThat(call(printed, "isbn")).isEqualTo("9780000000001");
			assertThat(call(printed, "identifier")).isEqualTo("Paper");

			Object digital = generated.wrap("Book", model.get("Digital"));
			assertThat(((Node) digital).is("Printed")).isEqualTo(false);
			Object missing = generated.as(digital, "Book.Printed");
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
			assertThat(call(generated.as(book, "Printed"), "isbn")).isEqualTo("9780000000001");
			assertThat(((Node) book).is("Printed")).isEqualTo(true);
			assertThat(((Node) book).is("Digital")).isEqualTo(false);
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
			assertThat(call(generated.as(book, "Printed"), "pages")).isEqualTo(BigDecimal.ZERO);
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
			assertThat(call(generated.as(second, "Printed"), "isbn")).isEqualTo("");
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
			assertThat(((Node) chapter).is("Indexed")).isEqualTo(true);
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
		assertThat(code).extracting(JavaCode::className)
			.contains("Book", "Chapter", "Author", "Note", "Edition")
			.doesNotContain("MogramTypes", "Node", "NodeSupport");
		assertThat(code).filteredOn(source -> source.className().equals("ModelTypes"))
			.singleElement().extracting(JavaCode::content).asString().contains("NodeSupport", "NodeSupport.Type")
			.doesNotContain("switch (", "frameSlots()", " as(", " wrap(", " of(");
		assertThat(code).allMatch(source -> source.packageName().equals("books"));
		assertThat(code).allSatisfy(source -> assertThat(source.content())
			.doesNotContain("io.quassar.templating.annotations", "FRAME_CHILDREN", "FRAME_PROPERTIES",
				"frameChildren()", "frameProperties()"));
		assertThat(code).filteredOn(source -> source.className().equals("Book"))
			.singleElement().extracting(JavaCode::content).satisfies(content -> assertThat(content)
				.contains("import io.quassar.reader.model.Mogram;", "import io.quassar.reader.model.Model;",
					"import io.quassar.reader.model.Node;", "import java.util.List;", "public final class Book extends Node", "public String title()")
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
				assertThat(call(generated.as(named, "Printed"), "isbn")).isEqualTo("9780000000001");
				assertThat(call(named, "available")).isEqualTo(true);
				assertThat(call(call(generated.as(named, "Edited"), "editor"), "identifier")).isEqualTo("Lucia");
				assertThat(((Node) named).is("Printed")).isEqualTo(true);
				assertThat(((Node) named).is("Edited")).isEqualTo(true);
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
			assertThat(first.getClass().getMethod("link").getReturnType().getName()).isEqualTo("io.quassar.reader.model.Node");
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

	@Test
	void namesTheFacadeAfterTheMetamodelLocalName() {
		var manifest = new MetamodelManifest("id", "MONET.VISORA", "1.8.2", "", "", "");
		var metamodel = new Metamodel(MetamodelFixtures.library().types(), Optional.of(manifest));
		var generator = MogramDecoratorGenerator.from(metamodel);
		assertThat(generator.modelClassName()).isEqualTo("VisoraModel");
		assertThat(generator.code()).extracting(JavaCode::className).contains("VisoraModel");
	}

	@Test
	void generatedLoadsRequireTheDslAndVersionUsedForGeneration() throws Exception {
		var manifest = new MetamodelManifest("id", "MONET.VISORA", "1.8.2", "", "quassar:metta:1.0.0", "");
		var metamodel = new Metamodel(MetamodelFixtures.library().types(), Optional.of(manifest));
		Path release = Files.createDirectory(directory.resolve("m1"));
		Files.writeString(release.resolve("books.tara"), "Book Atlas\n");
		Path images = Files.createDirectories(release.resolve("resources/images"));
		Files.write(images.resolve("icon.bin"), new byte[]{0, -1, 3});
		Path metadata = release.resolve("manifest.json");
		Files.writeString(metadata, "{\"dsl\":\"monet:visora:1.8.2\"}");
		try (GeneratedDecorators generated = compile(model("observations.tara"), metamodel)) {
			var facade = generated.modelDecorator().getClass();
			var load = facade.getMethod("load", Path.class);
			Object loaded = load.invoke(null, release);
			assertThat((List<?>) call(loaded, "books")).hasSize(1);
			assertThat(call(loaded, "resourceNames")).isEqualTo(java.util.Set.of("images/icon.bin"));
			var resourceMethod = facade.getMethod("resource", String.class);
			assertThat(resourceMethod.getReturnType()).isEqualTo(Resource.class);
			Resource resource = (Resource) resourceMethod.invoke(loaded, "images/icon.bin");
			assertThat(resource.name()).isEqualTo("images/icon.bin");
			assertThat(call(loaded, "resources")).isEqualTo(List.of(resource));
			assertThat(resource.bytes()).containsExactly(0, -1, 3);
			Path zip = directory.resolve("m1.zip");
			try (var archive = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
				for (Path file : List.of(release.resolve("books.tara"), metadata, images.resolve("icon.bin"))) {
					archive.putNextEntry(new java.util.zip.ZipEntry(release.relativize(file).toString()));
					Files.copy(file, archive);
					archive.closeEntry();
				}
			}
			assertThat((List<?>) call(facade.getMethod("load", URL.class).invoke(null, zip.toUri().toURL()), "books")).hasSize(1);
			assertThat((List<?>) call(facade.getMethod("load", URI.class).invoke(null, release.toUri()), "books")).hasSize(1);
			for (String mismatch : List.of("monet:orfeo:1.8.2", "monet:visora:1.8.3")) {
				Files.writeString(metadata, "{\"dsl\":\"" + mismatch + "\"}");
				assertThatThrownBy(() -> load.invoke(null, release))
					.isInstanceOf(InvocationTargetException.class)
					.cause().isInstanceOf(IOException.class).hasMessageContaining("monet:visora:1.8.2", mismatch);
			}
			Files.delete(metadata);
			assertThatThrownBy(() -> load.invoke(null, release))
				.isInstanceOf(InvocationTargetException.class)
				.cause().isInstanceOf(IOException.class).hasMessageContaining("monet:visora:1.8.2", "manifest.json");
		}
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

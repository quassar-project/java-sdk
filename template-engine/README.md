# Template Engine

Use an `.itr` template to turn Java objects or models from your DSL into text, JSON, or source code. The engine needs Java 17 or later.

Follow the catalogue example to see how rules select objects and how placeholders read their data.

## Add the dependency

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>template-engine</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Render a library catalogue

Save this as `catalogue.itr` in the directory from which you run the example.

```itr
rule type(Library)
	$name
	$books...[$NL$NL]

rule type(Book)
	$title+UpperCase ($year)
	By $author
	<<ISBN: $isbn>>

rule type(Author)
	$name
```

Save the Java example as `CatalogueExample.java`. The catalogue contains two books so you can see how optional data affects the output.

```java
import io.quassar.templating.TemplateEngine;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CatalogueExample {
	public static void main(String[] args) throws Exception {
		Library library = new Library("City Library", List.of(
			new Book("Don Quixote", 1605, new Author("Miguel de Cervantes"), "9788420412146", true),
			new Book("The Trial", 1925, new Author("Franz Kafka"), null, false)));

		try (InputStream input = Files.newInputStream(Path.of("catalogue.itr"))) {
			String catalogue = TemplateEngine.with(input).render(library);
			System.out.println(catalogue);
		}
	}

	public record Library(String name, List<Book> books) {
		public Library {
			books = List.copyOf(books);
		}
	}

	public record Book(String title, int year, Author author, String isbn, boolean available) {
	}

	public record Author(String name) {
	}
}
```

The output shows both books and the ISBN for the printed edition.

```text
City Library
DON QUIXOTE (1605)
By Miguel de Cervantes
ISBN: 9788420412146

THE TRIAL (1925)
By Franz Kafka
```

- A rule such as `type(Book)` selects the object to render. The first matching rule wins.
- A placeholder such as `$title` reads a named value from the object. When the value is another object, `$author` renders it with its own rule.
- `$books...[$NL$NL]` repeats the book rule with a blank line between books.
- `+UpperCase` formats the title. `<<ISBN: $isbn>>` prints the whole line only when the ISBN exists.

## Load a template from resources

Put `catalogue.itr` in `src/main/resources/templates`:

```java
TemplateEngine engine;
try (InputStream input = CatalogueExample.class.getResourceAsStream("/templates/catalogue.itr")) {
	engine = TemplateEngine.with(input);
}
String catalogue = engine.render(library);
```

Templates are read and executed directly. The caller closes the stream. A created engine can render several objects:

```java
String first = engine.render(firstLibrary);
String second = engine.render(secondLibrary);
```

## Annotated objects

Without annotations, the engine reads object fields as before. Annotated types expose only annotated methods, keeping backing fields internal.

```java
import io.quassar.templating.annotations.FrameSlot;
import io.quassar.templating.annotations.FrameType;

@FrameType("Book")
public record Book(@FrameSlot String isbn) {
}
```

The `isbn()` accessor supplies the `$isbn` slot. `@FrameSlot("chapter")` can give a method a different slot name. Lists and arrays supply multiple values.

| Annotation | Meaning |
| --- | --- |
| `@FrameType("Book")` on a class | Frame type, independent of its Java class name |
| `@FrameType` on a method | Additional types from a string or iterable of names |
| `@FrameSlot` on a method | A named slot; defaults to the method name |
| `@FrameSlots` on a method | Dynamic slots from a `Map<String, ?>` |
| `@FrameIdentity` on a method | Backing object identity shared by fresh decorators; null represents an absent object |
| `@FrameParent` on a method | Structural parent, preserved when another slot references this object |

Annotated methods take no arguments. Default methods on interfaces can provide shared annotations and behavior. Each conversion uses its own identity graph, supporting shared and cyclic references without expanding the graph recursively during construction. Explicit object mappers take precedence over annotations.

## Generated decorators

Generated decorators provide an internal `FrameView` with the backing identity, structural parent, frame types and slot values from the model. Their public methods expose the domain model. A template uses the model name for each slot, so `$chapter` renders children of type `Chapter` even though the Java getter is `chapters()`. Each conversion has its own identity graph for shared and cyclic references.

The Maven plugin generates the contract alongside the getters. Place a template with rules for `Model` and `Book` in the application's resources.

```java
LibraryModel library = LibraryModel.load(Path.of("library.zip"));
try (InputStream input = LibraryModel.class.getResourceAsStream("/library-model.itr")) {
	TemplateEngine engine = TemplateEngine.with(input);
	String text = engine.render(library);
	String book = engine.render(library.books().get(0));
}
```

## Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(library);
String catalogue = engine.render(frame);
```

`Frames.from(...)` adapts records and regular Java objects. It includes inherited fields and converts nested object, list, map and array fields. Static and transient fields are excluded. Each conversion creates its own frame tree and parent links. Passing an existing `Frame` returns that frame.

For explicit slots, use `ReflectiveFrameBuilder`:

```java
import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;

Frame book = new ReflectiveFrameBuilder("Book")
	.add("title", "Don Quixote")
	.add("year", 1605)
	.toFrame();
```

## Customize rendering

Register formatters and object adapters fluently:

```java
TemplateEngine engine = TemplateEngine.with(input)
	.add("Label", value -> "[" + value + "]")
	.add(ExternalBook.class, (source, frame) -> frame
		.add("Book")
		.add("title", source.label()));
```

See [examples](docs/examples.md) for matching templates, conditions, recursive chapters, explicit frames and Java generation.

## Factories

| Factory | Input |
| --- | --- |
| `TemplateEngine.with(input)` | An `.itr` input stream, read as UTF-8 |
| `TemplateEngine.with(template)` | An existing template |
| `TemplateEngine.with(rules)` | An iterable of rendering rules |
| `TemplateEngine.with(rules, configuration)` | Rules with explicit locale and line endings |
| `TemplateEngine.with(template).using(factory)` | A template with custom frame adaptation |

Unreadable or malformed templates raise `io.quassar.templating.ports.TemplateReadException`.

## More documentation

- [Examples: conditions, formatters, adapters, recursion and Java generation](docs/examples.md).
- [Template reference: rules, slots, expressions, lists and context](docs/templates.md).
- [Executable examples](src/test/java/tests/DocumentationExamplesTest.java) and their [template resources](src/test/resources/examples).

Distributed under the [MIT license](LICENSE).

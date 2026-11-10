# Template Engine

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert.

From Java, load the template with `TemplateEngine.with(Path)` and pass the data to `render`. For example, to turn a library into a catalogue:

```java
String catalogue = TemplateEngine.with(Path.of("catalogue.itr")).render(library());
```

## Add the dependency

Use Java 17 or later.

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

import java.nio.file.Path;
import java.util.List;

public class CatalogueExample {
	public static void main(String[] args) throws Exception {
		System.out.println(TemplateEngine.with(Path.of("catalogue.itr")).render(library()));
	}

	private static Library library() {
		return new Library("City Library", List.of(
			new Book("Don Quixote", 1605, new Author("Miguel de Cervantes"), "9788420412146", true),
			new Book("The Trial", 1925, new Author("Franz Kafka"), null, false)));
	}

	public record Library(String name, List<Book> books) {
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

## Write your own templates

Browse the [template examples](examples.md) for recipes with expected output, or use the [syntax reference](templates.md) to look up a construct.

Save each example as a separate `.itr` file and reuse the Java records from the catalogue. Load the file with `TemplateEngine.with(Path.of("example.itr"))` and pass the object indicated in each example to `render`.

### Choose a rule based on a property

Render a `Book` with a different label depending on its availability. Put the more specific rule first so the general rule can handle the remaining books.

```itr
rule type(Book) and attribute(available, true)
	$title: available

rule type(Book)
	$title: on loan
```

Rendering the two books separately produces these lines.

```text
Don Quixote: available
The Trial: on loan
```

### Provide fallback text

Use `?` inside `<<...>>` to supply an alternative when a value is missing. Render the two books separately to see both cases.

```itr
rule type(Book)
	$title — <<ISBN: $isbn?ISBN unavailable>>
```

```text
Don Quixote — ISBN: 9788420412146
The Trial — ISBN unavailable
```

### Render a list with a separator

Render a `Library`. Each item in `books` uses the `Book` rule. The text in brackets goes between items.

```itr
rule type(Library)
	$books...[, ]

rule type(Book)
	$title
```

```text
Don Quixote, The Trial
```

### Write JSON with optional fields

Render a `Book` as JSON. `JsonString` quotes and escapes strings. Put the comma inside the optional expression so it disappears with the ISBN.

```itr
rule type(Book)
	{"title": $title+JsonString, "year": $year<<, "isbn": $isbn+JsonString>>}
```

Each book produces its own JSON object.

```json
{"title": "Don Quixote", "year": 1605, "isbn": "9788420412146"}
```

```json
{"title": "The Trial", "year": 1925}
```

## Load a template from the project directory

Put `catalogue.itr` in `src/main/resources/templates` and run the example from the project root.

```java
var engine = TemplateEngine.with(Path.of("src/main/resources/templates/catalogue.itr"));
String catalogue = engine.render(library);
```

The engine reads and closes the file when you create it. Reuse the engine to render several objects.

```java
String first = engine.render(firstLibrary);
String second = engine.render(secondLibrary);
```

## Generated decorators

Pass a model or one of its decorators directly to `render`. Use the model's names in the template. For example, `$chapter` renders children of type `Chapter` even when the Java getter is `chapters()`.

Follow the [Maven Plugin quickstart](../maven-plugin/getting-started.md) to generate `WebServicesModel` and save `endpoint-summary.itr`. Replace the example URL with the M1 download URL from Quassar.

```java
import example.model.WebServicesModel;
import io.quassar.templating.TemplateEngine;
import java.net.URI;
import java.nio.file.Path;

var services = WebServicesModel.load(URI.create("https://quassar.io/models/[id]"));
var engine = TemplateEngine.with(Path.of("src/main/resources/endpoint-summary.itr"));
String route = engine.render(services.endpoints().get(0));
```

## Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(library);
String catalogue = engine.render(frame);
```

`Frames.from(...)` adapts records and regular Java objects. It includes inherited fields and converts nested object, list, map and array fields. Static and transient fields are excluded. Each conversion creates its own frame tree and parent links. Passing an existing `Frame` returns that frame.

Use `Frames.builder(...)` when you want to name the type and add slots yourself. Scalar values such as strings and numbers are added directly.

```java
Frame book = Frames.builder("Book")
	.add("title", "Don Quixote")
	.add("year", 1605)
	.toFrame();
```

## Customize rendering

Register formatters and object adapters fluently:

```java
TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]")
	.add(ExternalBook.class, (source, frame) -> frame
		.add("Book")
		.add("title", source.label()));
```

See [examples](examples.md) for matching templates, conditions, recursive chapters, explicit frames and Java generation.

## Factories

| Factory | Input |
| --- | --- |
| `TemplateEngine.with(path)` | A local `.itr` file that the engine opens and closes |
| `TemplateEngine.with(input)` | An `.itr` input stream, read as UTF-8 |
| `TemplateEngine.with(template)` | An existing template |
| `TemplateEngine.with(rules)` | An iterable of rendering rules |
| `TemplateEngine.with(rules, configuration)` | Rules with explicit locale and line endings |
| `TemplateEngine.with(template).using(factory)` | A template with custom frame adaptation |

Unreadable or malformed templates raise `io.quassar.templating.ports.TemplateReadException`.

## Executable examples

See the [example tests](../../template-engine/src/test/java/tests/DocumentationExamplesTest.java) and their [template resources](../../template-engine/src/test/resources/examples).

Distributed under the [MIT license](../../LICENSE).

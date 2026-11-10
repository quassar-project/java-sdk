# Template examples

[Start here](../README.md) · [Template reference](templates.md)

Reuse the `Book`, `Author` and `Library` records from the [catalogue example](../README.md#render-a-library-catalogue). Save a snippet as an `.itr` file, load it with `TemplateEngine.with(Path.of("example.itr"))`, and pass the object named in the rule to `render`.

- **Rules** · [property conditions](#choose-a-rule-from-the-data) · [boolean conditions](#combine-conditions) · [the calling slot](#choose-output-based-on-the-calling-slot)
- **Output** · [optional values](#optional-values-and-fallback-text) · [lists and separators](#render-a-list-and-format-each-value) · [JSON](#produce-json-with-optional-fields) · [literal text](#escape-template-characters) · [Java source](#generate-a-java-record)
- **Navigation** · [root and named paths](#read-values-from-the-root-and-a-named-path) · [containers](#build-frames-explicitly-and-access-their-container) · [recursion](#render-a-recursive-table-of-contents)
- **Java API** · [custom formatters](#use-a-custom-formatter) · [object adapters](#adapt-an-existing-object) · [frames](#create-frames-from-the-model) · [rules in Java](#build-a-template-in-java) · [verification](#verify-a-template-against-sample-data)

## Choose a rule from the data

```itr
rule type(Book) and attribute(available, true)
	$title: available

rule type(Book)
	$title: on loan
```

The specific rule comes first. The second rule handles all other books.

```java
String available = engine.render(new Book("Don Quixote", 1605, author, isbn, true));
String onLoan = engine.render(new Book("Don Quixote", 1605, author, isbn, false));
```

Output:

```text
Don Quixote: available
Don Quixote: on loan
```

## Combine conditions

Render the two catalogue books separately. A book gets a short label when it has an ISBN and is available, or when its year is 1925.

```itr
rule type(Book) and ((attribute(isbn) and not attribute(available, false)) or attribute(year, 1925))
	$title: featured

rule type(Book)
	$title: catalogue
```

```text
Don Quixote: featured
The Trial: featured
```

## Choose output based on the calling slot

Render the first book. The author rule checks that it was reached through `$author`.

```itr
rule type(Book)
	$title by $author

rule type(Author) and trigger(author)
	$name+UpperCase

rule type(Author)
	$name
```

```text
Don Quixote by MIGUEL DE CERVANTES
```

## Optional values and fallback text

```itr
rule type(Book)
	$title: <<ISBN $isbn?ISBN unavailable>>
```

With an ISBN:

```text
Don Quixote: ISBN 9788420412146
```

Without an ISBN:

```text
The Trial: ISBN unavailable
```

Use `<<ISBN: $isbn>>` to omit the whole expression when the slot has no value. A record field set to `null` does not create a slot.

## Render a list and format each value

Render a frame built with `Frames.builder("Book").add("tags", new String[]{"historical fiction", "classic"}).toFrame()`. The `tags` rule uses `$this` to format the current list item. The text between brackets after `...` separates items.

```itr
rule type(Book)
	Tags: $tags...[, ]

rule trigger(tags)
	[$this+OnlyLetters+UpperCase]
```

```text
Tags: [HISTORICALFICTION], [CLASSIC]
```

## Produce JSON with optional fields

Render the catalogue library. `JsonString` quotes and escapes text. The optional expression contains the comma so a missing ISBN leaves valid JSON.

```itr
rule type(Library)
	{"books": [$books...[, ]]}

rule type(Book)
	{"title": $title+JsonString, "year": $year<<, "isbn": $isbn+JsonString>>}
```

```json
{"books": [{"title": "Don Quixote", "year": 1605, "isbn": "9788420412146"}, {"title": "The Trial", "year": 1925}]}
```

## Escape template characters

Render the first book. Use `~` to end a placeholder before adjacent text, `$$` for a literal dollar sign, and `$<<` and `$>>` for literal expression delimiters.

```itr
rule type(Book)
	$year~AD $$5 $<<preview$>>
```

```text
1605AD $5 <<preview>>
```

## Read values from the root and a named path

Render the catalogue library. A named path such as `books.author` follows the first value at each step. Inside the book rule, `root` takes you back to the library.

```itr
rule type(Library)
	First author: $<books.author>name
	$books...[$NL]

rule type(Book)
	$title — $<root>name
```

```text
First author: Miguel de Cervantes
Don Quixote — City Library
The Trial — City Library
```

## Render a recursive table of contents

```itr
rule type(Chapter)
	$title
		<<$children...[$NL]>>
```

```java
public record Chapter(String title, List<Chapter> children) {
	public Chapter {
		children = List.copyOf(children);
	}
}

Chapter contents = new Chapter("Contents", List.of(
	new Chapter("Beginnings", List.of()),
	new Chapter("Journey", List.of(new Chapter("Home", List.of())))));

String result = engine.render(contents);
```

Output:

```text
Contents
	Beginnings
	Journey
		Home
```

Every child invokes the same rule. The template's extra tab indents the rendered children.

## Use a custom formatter

Save this as `label.itr`.

```itr
rule type(Book)
	$title+Label
```

```java
TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]");

String result = engine.render(book);
```

Output:

```text
[Don Quixote]
```

## Adapt an existing object

Use the same `label.itr` template with data that has different field names:

```java
public record ExternalBook(String label) {
}

TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]")
	.add(ExternalBook.class, (source, frame) -> frame
		.add("Book")
		.add("title", source.label()));

String result = engine.render(new ExternalBook("The Trial"));
```

Output:

```text
[The Trial]
```

The adapter supplies the `Book` type and the `title` slot expected by the template.

## Create frames from the model

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame libraryFrame = Frames.from(library);
Frame bookFrame = libraryFrame.frames("books").next();
Object title = bookFrame.frames("title").next().value();
String result = engine.render(libraryFrame);
```

The frame keeps the object's type and slots. The book's `container()` is `libraryFrame`.

## Build frames explicitly and access their container

```itr
rule type(Library)
	$books...[$NL]

rule type(Book)
	$title ($<container>name)
```

```java
import io.quassar.templating.Frames;

var book = Frames.builder("Book").add("title", "Don Quixote").toFrame();
var library = Frames.builder("Library")
	.add("name", "City Library")
	.add("books", book)
	.toFrame();

String result = engine.render(library);
```

Output:

```text
Don Quixote (City Library)
```

`$<container>name` reads the library's name while rendering its book.

## Generate a Java record

```itr
rule type(Entity)
	public record $name($properties...[, ]) {}

rule type(Property)
	$type $name
```

```java
public record Property(String type, String name) {
}

public record Entity(String name, List<Property> properties) {
	public Entity {
		properties = List.copyOf(properties);
	}
}

Entity entity = new Entity("Book", List.of(
	new Property("String", "title"),
	new Property("int", "year")));

String java = engine.render(entity);
Files.writeString(Path.of("Book.java"), java);
```

Generated code:

```java
public record Book(String title, int year) {}
```

## Build a template in Java

Use the same rule model as an `.itr` template when you need to assemble rules in code.

```java
import io.quassar.templating.TemplateEngine;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

var template = new Template()
	.add(new Rule().condition(allTypes("Book")).output(placeholder("title")));

String result = TemplateEngine.with(template).render(book);
```

For the first catalogue book, `result` is `Don Quixote`. To set a specific locale or line separator, see the [configuration example](templates.md#rendering-configuration).

## Verify a template against sample data

Use a representative input model to check that a template has matching rules, valid references, and a traversal that terminates.

```java
import io.quassar.templating.Frames;
import io.quassar.templating.TemplateVerifier;
import io.quassar.templating.adapters.antlr.TemplateReader;
import java.nio.file.Path;

var template = TemplateReader.read(Path.of("catalogue.itr"));
var report = TemplateVerifier.of(template).verify(Frames.from(library));
System.out.println(report.isOk());
```

## Executable versions

The [example resources](../src/test/resources/examples) and [DocumentationExamplesTest](../src/test/java/tests/DocumentationExamplesTest.java) exercise the public API with catalogues, recursive chapters, custom formatters, and Java generation.

Run these examples from the repository root:

```sh
mvn -f template-engine/pom.xml -Dtest=DocumentationExamplesTest test
```

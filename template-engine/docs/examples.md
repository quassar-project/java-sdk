# Template examples

[Start here](../README.md) · [Template reference](templates.md)

Reuse the `Book`, `Author` and `Library` records from the [catalogue example](../README.md#render-a-library-catalogue). Save a snippet as an `.itr` file, load it with `TemplateEngine.with(input)`, and pass the object named in the rule to `render`.

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

```itr
rule type(Book)
	$title+Label
```

```java
TemplateEngine engine = TemplateEngine.with(input)
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

TemplateEngine engine = TemplateEngine.with(input)
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
import io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder;

var book = new ReflectiveFrameBuilder("Book").add("title", "Don Quixote");
var library = new ReflectiveFrameBuilder("Library")
	.add("name", "City Library")
	.add("books", book);

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

## Executable versions

All templates above are [test resources](../src/test/resources/examples). [DocumentationExamplesTest](../src/test/java/tests/DocumentationExamplesTest.java) renders them using the public API and checks the shown output.

Run these examples from the repository root:

```sh
mvn -f quassar-java-sdk/template-engine/pom.xml -Dtest=DocumentationExamplesTest test
```

# Model Reader

Load models written in your Tara DSL and query them from Java. The reader needs Java 17 or later.

## 1. Add the dependency

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>model-reader</artifactId>
	<version>1.0.0</version>
</dependency>
```

## 2. Create a model in Quassar

Create a model in Quassar Studio with the following content. Copy its model URL and use it in the Java example. [Open Quassar Studio](https://quassar.io/studio)

```tara
Author(name = "Franz Kafka") Kafka

Book(year = 1915, author = Kafka) TheMetamorphosis
	title = "The Metamorphosis"
	tags = "fiction" "novella"
	Chapter(number = 1) Opening > title = "The transformation"
```

Each object is a *mogram*. `Book` is its type and `TheMetamorphosis` is its identifier. Indentation makes `Opening` a child of the book. The value `Kafka` refers to the author by identifier.

## 3. Load the model and follow a reference

```java
import io.quassar.reader.Models;
import java.net.URI;

public class LibraryExample {
	public static void main(String[] args) throws Exception {
		var model = Models.load(URI.create("https://quassar.io/models/[code]"));
		var book = model.get("TheMetamorphosis");
		var author = model.get(book.property("author"), book);

		System.out.println(book.property("title").asString());
		System.out.println(author.property("name").asString());
	}
}
```

This prints `The Metamorphosis` and `Franz Kafka`. Passing `book` as context lets the reader resolve references relative to the book before looking from the root. Use `model.get(value)` when the reference is already rooted.

## 4. Find and navigate mograms

Use an identifier path when you know the exact object. Use a query when you want to filter or navigate.

```java
var chapter = model.get("TheMetamorphosis.Opening");
var parent = model.parentOf(chapter);

var books = model.roots().type("Book").list();
var chapters = model.select(book).children().type("Chapter").list();
var allChapters = model.select(book).descendants().type("Chapter").list();
```

`model.roots()` starts at the top level, `model.select()` starts with every mogram, and `model.select(book)` starts with one mogram. Add filters such as `type("Book")`, `facet("Printed")`, or `withProperty("title")`. Finish with `list()`, `stream()`, `first()`, `count()`, or `isEmpty()`. Queries return new selections and their result lists are immutable.

## Read values and handle missing data

Use a typed accessor for the property you expect.

```java
import io.quassar.reader.model.Value;

var year = book.property("year").asNumber();
var tags = book.property("tags").asSequence().stream()
	.map(Value::asString)
	.toList();

if (book.property("isbn").isEmpty()) {
	System.out.println("ISBN unavailable");
}
```

| Value | Check | Read |
| --- | --- | --- |
| Text | `isString()` | `asString()` |
| Number | `isNumber()` | `asNumber()` returns a `BigDecimal` |
| Boolean | `isBoolean()` | `asBoolean()` |
| Reference | `isReference()` | `asReference()` returns the path |
| Sequence | `isSequence()` | `asSequence()` returns an immutable list |

A missing property returns `Value.Empty`. Typed accessors return an empty string, zero, `false`, or an empty list when the value has a different type. Use the checks when you need to distinguish a missing value from a real zero or empty string.

A missing mogram, an unresolved reference, the parent of a root mogram, and `first()` on an empty query return `Mogram.NULL`. Check the result with `isNull()`.

## Load from another source

```java
import java.nio.file.Path;

var archive = Models.load(Path.of("library.zip"));
var folder = Models.load(Path.of("m1"));
var bundled = Models.load(LibraryExample.class.getResource("/library.tara"));
```

A folder or ZIP can contain multiple Tara files and subfolders. The reader loads them into one model so references can cross file boundaries. For typed methods such as `book.title()`, see the [Maven Plugin guide](../maven-plugin/getting-started.md).

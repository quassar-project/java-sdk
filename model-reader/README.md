# Model Reader

[All projects and documentation](../README.md)

Java 17+ library for loading Tara models and querying their mograms.

## Example Tara model

```tara
dsl library

Author(name = "Miguel de Cervantes") Cervantes
Author(name = "Franz Kafka") Kafka

Book(year = 1605, available = true, author = Cervantes) DonQuixote is Printed(isbn = "9788420412146") Classic
	title = "Don Quijote de la Mancha"
	tags = "novel" "classic"

	Chapter(number = 1) FirstChapter
		title = "Que trata de la condición y ejercicio del famoso hidalgo"

	Chapter(number = 2) SecondChapter > title = "De la primera salida"

Book(year = 1925, author = Kafka) TheTrial is Digital(format = "EPUB")
	title = "El proceso"

	Chapter(number = 1) Arrest > title = "La detención"
```

`Book` is the type and `DonQuixote` is the identifier. `Printed` and `Classic` are facets. All properties belong to the mogram, including `isbn`, declared alongside a facet.

Chapters are children of books. `author = Cervantes` references the author.

## Maven dependency

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>model-reader</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Load a model

```java
import io.quassar.reader.Models;
import io.quassar.reader.model.Model;

import java.nio.file.Path;

Model model = Models.load(Path.of("library.zip"));
```

You can also load a folder with subfolders, a ZIP URL, or a `.tara` URL.

```java
Model folder = Models.load(Path.of("library"));
Model remote = Models.load(URI.create("https://example.org/library.zip"));
Model bundled = Models.load(Main.class.getResource("/library.tara"));
```

`Models.load(...)` accepts `Path`, `URL` and `URI`, and declares `IOException`. All `.tara` files become part of a single model and can reference each other.

## Get a mogram

`get(path)` returns the mogram identified by a path from the root.

```java
import io.quassar.reader.model.Mogram;

Mogram book = model.get("DonQuixote");
Mogram chapter = model.get("DonQuixote.FirstChapter");
```

To search by type, use `select().type(...)`:

```java
List<Mogram> books = model.select().type("Book").list();
```

## Access mogram data

```java
String type = book.type();
String identifier = book.identifier();
List<String> facets = book.facets();
List<Mogram> children = book.children();
Value title = book.property("title");
Value isbn = book.property("isbn");
Mogram parent = model.parentOf(chapter);
```

`identifier()` returns `""` when absent. `property(name)` returns `Value.Empty` when the property is absent. For a root mogram, `parentOf(...)` returns `Mogram.NULL`.

Value types are `Value.Text`, `Value.Number`, `Value.Bool`, `Value.Reference`, `Value.Sequence` and `Value.Empty`:

```java
if (title.isString()) System.out.println(title.asString());
BigDecimal year = book.property("year").asNumber();
boolean available = book.property("available").asBoolean();
```

Each name identifies a single property. Multiple values are grouped in `Value.Sequence`:

```java
List<String> tags = book.property("tags").asSequence().stream()
	.map(Value::asString)
	.toList();
```

Type checks and accessors:

| Check | Accessor | Return type |
| --- | --- | --- |
| `isString()` | `asString()` | `String` |
| `isNumber()` | `asNumber()` | `BigDecimal` |
| `isBoolean()` | `asBoolean()` | `boolean` |
| `isReference()` | `asReference()` | Reference path as `String` |
| `isSequence()` | `asSequence()` | Immutable `List<Value>` |
| `isEmpty()` | — | Checks for `Value.Empty` |

Accessors do not convert between types. For a different type, including `Value.Empty`, they return `""`, zero, `false` or an empty list. `asReference()` returns the path; use `model.get(value)` to obtain its target mogram. Import `java.math.BigDecimal` when declaring numeric variables.

## Filter and navigate

Printed books:

```java
List<Mogram> printedBooks = model.select()
		.type("Book")
		.facet("Printed")
		.list();
```

Chapters of a book:

```java
List<Mogram> chapters = model.select(book)
		.children()
		.type("Chapter")
		.list();
```

Chapters at any nesting level:

```java
List<Mogram> chapters = model.select(book)
		.descendants()
		.type("Chapter")
		.list();
```

Books by Cervantes:

```java
List<Mogram> books = model.select()
		.type("Book")
		.withProperty("author", new Value.Reference("Cervantes"))
		.list();
```

Other queries:

```java
List<Mogram> titled = model.select().withProperty("title").list();
List<Mogram> named = model.select().where(mogram -> !mogram.identifier().isEmpty()).list();
List<Mogram> parents = model.select(book).children().parents().list();
```

Starting points:

| Method | Selection |
| --- | --- |
| `model.select()` | All mograms |
| `model.roots()` | Root mograms only |
| `model.select(mogram)` | A specific mogram |

Chain `type`, `identifier`, `facet`, `withProperty`, `where`, `children`, `descendants` and `parents`. Finish with `list()`, `stream()`, `first()`, `count()` or `isEmpty()`:

```java
int total = model.select().type("Book").count();
Mogram first = model.select().type("Book").first();
boolean empty = model.select(book).children().isEmpty();
```

Each operation returns a new selection without changing the previous one. Returned lists are immutable.

Use `stream()` to continue with Java stream operations:

```java
List<String> identifiers = model.select()
	.type("Book")
	.stream()
	.map(Mogram::identifier)
	.toList();
```

Each call to `stream()` returns a new stream without consuming or modifying the query.


To access the collections directly:

```java
List<Mogram> roots = model.mograms();
List<Mogram> all = model.allMograms();
```

## Get the target of a reference

`get(...)` also accepts a reference:

```java
Mogram author = model.get(book.property("author"));
```

Provide a context to look up a relative path:

```java
Mogram chapter = model.get(new Value.Reference("FirstChapter"), book);
```

Without a context, the path is looked up from the root. With a context, the lookup checks its enclosing scopes first, then the root. Each call returns a single mogram. If the value is not a reference or no target exists, it returns `Mogram.NULL`.

## Run tests

From this folder:

```sh
mvn test
```

Tests use JUnit 5, AssertJ and the book models in `src/test/resources/tara`. Java assertion flags are not required.

To include the optional original-inventory test:

```sh
TARA_MODEL_ZIP="/path/to/inventory.zip" mvn test
```


## Null mograms

`get(...)` accepts a property value directly and checks whether it is a reference. Non-reference values, missing targets and missing parents return the immutable null object `Mogram.NULL`:

```java
Mogram author = model.get(book.property("author"));
Mogram parent = model.parentOf(book);
boolean absent = author.isNull();
```

The null mogram has empty type, identifier, properties, facets and children. Its properties return `Value.Empty`, so lookups can be chained safely. Null mograms do not appear in query results. Query `first()` also returns `Mogram.NULL` when the selection is empty.


## Generate domain decorators

The [Quassar Maven Plugin](../maven-plugin/README.md) generates records during the Maven build, such as `Book(Mogram mogram, Model model)` with typed property getters, decorated references, facet checks and decorated child lists.

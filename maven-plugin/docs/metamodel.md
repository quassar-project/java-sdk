# Generate decorators from M2

[Quassar Maven Plugin](../README.md)

Use this guide to see how an M2 declaration becomes part of the generated Java API. Start with the Maven Plugin tutorial if you want a complete M2 and M1 example.

## Generate from Java

Load an M2 metamodel from `https://quassar.io/models/[id]`. Replace `[id]` with your metamodel ID.

```java
import io.quassar.javacode.MogramDecoratorGenerator;
import io.quassar.metamodel.Metamodels;

import java.net.URI;
import java.nio.file.Path;

var metamodel = Metamodels.load(URI.create("https://quassar.io/models/[id]"));
MogramDecoratorGenerator.from(metamodel)
	.inPackage("example.library")
	.writeTo(Path.of("src/main/gen"));
```

The generator uses the `name` from the root `manifest.json` with a `Model` suffix. A manifest named `Library` generates `LibraryModel`. Use `.modelNamed(...)` to override the name.

Use `.code()` to obtain the generated `JavaCode` records without writing files. Java keywords and name collisions receive safe generated names; lookups retain the original names from the metamodel.

## Property types

| Metamodel declaration | Java getter |
| --- | --- |
| `def string title` | `String` |
| `def integer year` | `BigDecimal` |
| `def boolean available` | `boolean` |
| `def Author author` | `Author` decorator |
| `def Author[] authors` | `List<Author>` |

A reference without a declared target uses `MogramDecorator`. References resolve lazily against the loaded model, using their source mogram as context. Missing targets return null decorators with `isNull()` and empty property defaults. Relative references and cycles are supported.

A multiple property returns an empty list for an absent value, a singleton list for one value, and a list of all elements for a sequence.

## Use the generated model

A root or child with a maximum cardinality of one has a singular getter that returns its decorator. For example, `Concept:{1..1} Library` generates `model.library()` and `Concept:Single:Required Catalogue` nested under it generates `library.catalogue()`. If a single mogram is absent, the getter returns a decorator with `isNull()` set to `true`. Repeated children return lists.

The model facade exposes only root mograms. Concepts declared as components and concepts declared inside another concept are reached through their parent decorators.

```java
LibraryModel library = LibraryModel.load(Path.of("library.zip"));
Book book = library.books().get(0);
String title = book.title();
BigDecimal year = book.year();
List<Author> authors = book.authors();
boolean available = book.available();
```

`LibraryModel.load(...)` accepts `Path`, `URL` and `URI`, and declares `IOException`. It loads model instances using `model-reader`. The M2 metamodel supplies the declarations used to generate the Java API.

Model collection methods return root mograms of the declared type. Child collection methods return direct children of the declared type. References resolve against the loaded model.

For a book with the `Printed` facet, `book.isPrinted()` checks whether it is active and `book.asPrinted()` returns its typed view. Facet property getters belong to that view, so read the ISBN with `book.asPrinted().isbn()`. When the facet is absent, the view returns `true` from `isNull()`.

## Read a local M2 metamodel

```java
import io.quassar.metamodel.Metamodels;
import io.quassar.metamodel.model.Metamodel;

import java.nio.file.Path;

Metamodel metamodel = Metamodels.load(Path.of("library-metamodel.zip"));
Metamodel folder = Metamodels.load(Path.of("m2"));
```

Both sources can contain declarations in several `.tara` files, including files in subdirectories. To read M1 from a local folder, use `LibraryModel.load(Path.of("m1"))`.

```tara
Concept Book
	def string title
	def Author[] authors
	facet Edition
		sub Printed
			def string isbn
	Concept:{1..*} Chapter
		def string title
	sub Novel
		def string synopsis = empty

Concept Author as component > def string name
```

`metamodel.types()` contains the immutable declarations. Names include their declaration scope, such as `Book.Chapter` and `Book.Edition.Printed`. Each type retains its own properties, facets, compositions, cardinality and modifiers. `scope()` identifies its enclosing declaration; `parent()` records inheritance through `sub` or `extends`. Facet alternatives are also facet types. Repeated declarations in the same scope are combined.

Properties retain their declared type, sequence element type, reference target spelling, constraints and default value. Defaults retain their source expression, including quotes, units or `empty`. Inherited properties and reference targets are not resolved by this reader.

## Build and test

From the repository root:

```sh
mvn -pl maven-plugin -am test
```

Tests use JUnit 5 and AssertJ with M2 metamodel resources and Java fixtures. They compile and execute generated decorators against model instances, covering facets, references, sequences, positional properties, null objects, model loading and Java name collisions.

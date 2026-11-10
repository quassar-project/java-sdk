# Quassar Java SDK

Create your own domain-specific language (DSL) in [Quassar](https://quassar.io). Use the Java SDK to turn the models people write in that language into applications, reports, or generated source code.

## From Quassar to a running application

| Where | What you do | What you get |
| --- | --- | --- |
| **Forge** | Create a DSL and define its concepts, properties, and relationships. | A metamodel (M2), such as a library DSL with books and chapters. |
| **Studio** | Create a model using that DSL. | A model (M1), such as a catalogue with actual books. |
| **Your Java project** | Give the Maven plugin the M2 download URL and load M1 using its own URL. | Typed Java classes for your DSL and access to the model's data. |

With a metamodel named `Library` and a `book-summary.itr` template, the plugin generates `LibraryModel` and `BookSummaryTemplate`. The application can then load a model and render its first book.

```java
var library = LibraryModel.load(URI.create("https://quassar.io/models/[id]"));
System.out.println(BookSummaryTemplate.render(library.books().get(0)));
```

Replace the example URL with your M1 download URL. The [Maven Plugin quickstart](maven-plugin/README.md) shows the M2 configuration, the template, and a complete Java application.

## Find the right guide

| I want to… | Start here |
| --- | --- |
| Generate typed classes from a metamodel or scaffold a Maven generator. | [Maven Plugin](maven-plugin/README.md) |
| Write templates for text, JSON, or code. | [Template Engine](template-engine/README.md) · [Template examples](template-engine/docs/examples.md) |
| Load and query a Tara model through a generic Java API. | [Model Reader](model-reader/README.md) |

## Build the SDK from source

Use Java 17 or later and Maven 3.9.9 or later. From the repository root, run:

```sh
mvn clean install
```

This runs the tests and installs the SDK modules into your local Maven repository.

[MIT License](LICENSE)

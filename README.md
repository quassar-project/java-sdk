# Quassar Java SDK

Build your own domain-specific language (DSL) in [Quassar](https://quassar.io) and use this SDK to turn models written in that language into Java applications, code generators, and documents.

## From a DSL to Java

1. **Define your language in Forge.** Choose **New DSL** and describe its concepts in a metamodel (M2). A library DSL might define a `Book` with a title and a collection of chapters.
2. **Create a model in Studio.** Choose **New model** and select your DSL. Add a book and its chapters. This is your M1 model, the data that your Java application will load.
3. **Connect your Java project.** Copy the two download URLs from Quassar. The Maven plugin uses M2 at build time to generate classes such as `LibraryModel` and `Book`. Your Java code uses M1 at runtime to read the actual books.

```java
var library = LibraryModel.load(URI.create("https://quassar.io/models/[id]"));
var book = library.books().get(0);
System.out.println(book.title());
```

Replace the example URL with your M1 download URL. Follow the [Maven Plugin quickstart](maven-plugin/README.md) to generate `LibraryModel` and render the book with an `.itr` template.

## Choose a guide

### [Maven Plugin](maven-plugin/README.md)

Turn your DSL's metamodel into a typed Java API, compile `.itr` templates, and scaffold an application or a Maven code generator.

### [Template Engine](template-engine/README.md)

Write `.itr` templates for source code, JSON, and other text formats. The [template examples](template-engine/docs/examples.md) cover conditions, lists, and optional fields.

### [Model Reader](model-reader/README.md)

Load and query models written in your DSL directly from Java, with access to mograms, children, and references.

## Build the SDK locally

With Java 17 or later and Maven 3.9.9 or later, run `mvn clean install` from this repository. This runs the tests and installs the SDK modules in your local Maven repository.

---

Java 17+ · [MIT License](LICENSE)

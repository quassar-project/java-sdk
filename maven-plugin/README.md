# Quassar Maven Plugin

Turn your DSL's metamodel into typed Java classes and compile `.itr` templates during the Maven build. Use Java 17 and Maven 3.9.9 or later.

## 1. Define the language and save a model

M2 describes the language. M1 contains the objects written in that language.

Save the metamodel as `m2/library.tara`.

```tara
dsl metta

Concept Book
	def string title
	has Chapter

Concept Chapter as component
	def string title
```

Name the metamodel in `m2/manifest.json`. The name `Library` gives the generated model class its name, `LibraryModel`.

```json
{"name":"Library"}
```

Save one book and its chapter as `m1/library.tara`.

```tara
Book(title = "The Metamorphosis") TheMetamorphosis
	Chapter Opening > title = "The transformation"
```

## 2. Configure Maven

In your project's POM, set `maven.compiler.release` to `17` or later and add this build configuration.

```xml
<build>
	<plugins>
		<plugin>
			<groupId>quassar.io</groupId>
			<artifactId>maven-plugin</artifactId>
			<version>1.0.0</version>
			<extensions>true</extensions>
			<executions>
				<execution>
					<goals><goal>generate</goal></goals>
				</execution>
			</executions>
			<configuration>
				<package>example</package>
				<metamodel>${project.baseUri}m2/</metamodel>
				<scaffold>none</scaffold>
			</configuration>
		</plugin>
	</plugins>
</build>
```

The path points to your local M2 directory. For a remote metamodel, use `https://quassar.io/models/[id]` and replace `[id]` with its ID. The plugin also accepts a ZIP. With `extensions` enabled, Maven adds the reader and template engine as project dependencies.

The tutorial sets `scaffold` to `none` because you supply the model and template files in the following steps.

## 3. Generate and use the Java classes

Run `mvn compile`. The plugin writes generated Java under `src/main/gen`. With the package in this example, model classes are under `example.model`.

```java
import example.model.LibraryModel;
import java.nio.file.Path;

var library = LibraryModel.load(Path.of("m1"));
var book = library.books().get(0);

System.out.println(book.title());
System.out.println(book.chapters().get(0).title());
```

The model exposes the root books. Chapters are components, so you access them through a book.

## 4. Render with a template

Save this as `src/main/resources/book-summary.itr` and run `mvn compile` again.

```itr
rule type(Book)
	Book: $title
```

```java
import example.templates.BookSummaryTemplate;

String summary = BookSummaryTemplate.render(book);
```

The result is `Book: The Metamorphosis`. Maven compiles each template into its own Java class, so the generated renderer can run without the original `.itr` file.

## Choose what to scaffold

Set `scaffold` in the plugin's `configuration`. Starter files are created when the main Java source directory is empty. Existing sources and templates take precedence.

| Value | What it creates |
| --- | --- |
| `application` (default) | An editable `Main.java`. It loads `m1` by default and prints the result when one template is available. |
| `maven-plugin` | An editable Mojo with a `generate` goal. Set the project's packaging to `maven-plugin`. A metamodel named `Library` produces `LibraryMojo.java`. |
| `none` | Only generated Java from the configured metamodel and existing templates. |

If M2 is configured and the templates directory has no `.itr` files, an application or Maven plugin scaffold also creates `model-json.itr`. To generate Java from templates alone, omit `metamodel`.

The generated Maven goal reads `m1` and writes `target/generated-resources/model.json` when a metamodel and one template are available. Set `-Dquassar.source` and `-Dquassar.output` to use other paths.

## Configuration reference

| Parameter | Default |
| --- | --- |
| `package` | `generated` |
| `metamodel` | Optional M2 folder or ZIP URL |
| `scaffold` | `application` |
| `templatesDirectory` | `${project.basedir}/src/main/resources` |
| `outputDirectory` | `${project.basedir}/src/main/gen` |

For property types, cardinality, and the generated Java API, see the [M2 guide](docs/metamodel.md).

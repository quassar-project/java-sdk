# Quassar Maven Plugin

Generate Java decorators from an M2 metamodel and compile `.itr` templates during the Maven build. Requires Java 17 and Maven 3.9.9 or newer.

## Configure Maven

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
				<metamodel>https://quassar.io/models/[id]</metamodel>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[id]` with your metamodel ID. The URL serves the metamodel ZIP. The plugin generates the decorators and the model class in the configured package plus `.model`. Omit `metamodel` to generate only template classes.

To read a local M2 folder, use `<metamodel>${project.baseUri}m2/</metamodel>`. The reader includes `.tara` files in subfolders.

Place `manifest.json` at the root of the M2 folder or ZIP. Its `name` sets the generated model class name. For example, `Library` generates `LibraryModel`. If the manifest has no name or is absent, the class is named `DecoratedModel`.

See [Generate decorators from M2](docs/metamodel.md) for the metamodel format and Java API.

With `<extensions>true</extensions>`, Maven includes `quassar.io:model-reader` and `quassar.io:template-engine` as compile dependencies, including in projects that only compile templates. The plugin adds them to the Maven project and the published POM. Their default versions match the plugin version, and explicit declarations and versions from `dependencyManagement` are preserved.

| Parameter | Default |
| --- | --- |
| `package` | `generated` |
| `metamodel` | Optional URL of the M2 ZIP or a local M2 folder |
| `scaffold` | `application`. Also accepts `maven-plugin` or `none` |
| `templatesDirectory` | `${project.basedir}/src/main/resources` |
| `outputDirectory` | `${project.basedir}/src/main/gen` |

With `<scaffold>application</scaffold>`, the plugin creates an editable `Main.java` in the configured package when the main source directory contains no Java files. With a metamodel configured, it loads M1 from the first command-line argument or from `m1` by default. If the project has no `.itr` files yet, the plugin also creates an editable `model-json.itr` in the templates directory and compiles it in the same build. It groups root mograms by type, renders their properties and nested children, and writes references as identifiers. Missing scalar fields are omitted. If there is exactly one template, `Main` prints its output for the loaded model. A template-only project gets an empty entry point.

Use `<scaffold>none</scaffold>` to generate decorators and compile existing templates without creating starter files.

## Scaffold a Maven generator

Set `<packaging>maven-plugin</packaging>` on the project and add `<scaffold>maven-plugin</scaffold>` to the Quassar plugin configuration. Set `maven.compiler.release` to 17 or later. The Quassar extension supplies the Maven plugin API and annotations as `provided` dependencies.

In an empty Java source directory, Quassar creates an editable Mojo with a `generate` goal. A metamodel named `Library` in `manifest.json` generates `LibraryMojo.java`. If the manifest has no name or is absent, Quassar generates `GenerateMojo.java`. Existing sources and templates take precedence. If a metamodel and exactly one template are available, the goal reads `m1` from the consuming project and writes the rendered result to `target/generated-resources/model.json`. Set `-Dquassar.source` and `-Dquassar.output` to change those paths. With other configurations, the generated goal has an empty body to customize.

Run `mvn install` to install your generator and invoke its `generate` goal from the consuming project.

## Render from Java

Save the template as `src/main/resources/book-summary.itr`.

```itr
rule type(Book)
	Book: $title
```

Run `mvn compile`. Each `.itr` file generates one Java class in the configured package plus `.templates`.

```java
import example.model.LibraryModel;
import example.templates.BookSummaryTemplate;

var library = LibraryModel.load(java.nio.file.Path.of("library.zip"));
String summary = BookSummaryTemplate.render(library.books().get(0));
```

The generated class renders with Java rules compiled during the build, so `render` works without the `.itr` file at runtime.

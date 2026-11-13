# Quassar Java SDK

Use the Quassar Java SDK to build a compiler or an interpreter for models written in your own domain-specific language (DSL).

## Create your DSL and models

Open Quassar Forge and select **New DSL**. To create the DSL, define its metamodel (M2) with the concepts, properties, and relationships that models can express. [Open Quassar Forge](https://quassar.io/forge)

Once your DSL is available, select it in Quassar Studio and create a model (M1). Use the concepts defined in M2 to describe a specific case in your domain. The same DSL can support many models. [Open Quassar Studio](https://quassar.io/studio)

## Use your models from Java

Decide how your tool should process M1. Choose compilation to generate source code for a later build, or interpretation to have a Java application act directly on the model at runtime. Both use Java decorators generated from M2 to load and navigate models. [Generated Model API](docs/generated-model-api.md)
The generated API uses Model Reader to load M1. You can also use Model Reader directly to load and query models through its generic Java API. [Model Reader API](docs/model-reader-api.md)

Add the Quassar Maven plugin to your `pom.xml`. Set `maven.compiler.release` to `17` or later and use Maven 3.9.9 or later. With `<extensions>true</extensions>`, the plugin adds `model-reader` and `template-engine` automatically.

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
				<metamodel>https://quassar.io/models/[code]</metamodel>
				<mode>[mode]</mode>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[code]` with the M2 code so the plugin can generate the Java code for your DSL. Use the M1 URL in your compiler or interpreter to load a specific model. Choose the value for `[mode]` below.

### Compiler

Build a Maven plugin for your DSL that turns each model (M1) into source code in the consuming project's generated sources directory.

```text
Model (M1) → Your DSL's Maven plugin → Source code in the generated sources directory
```

1. **Configure the POM.** In the DSL plugin project's `pom.xml`, add `<packaging>maven-plugin</packaging>` directly under `<project>`, alongside `<artifactId>` and `<version>`.

	```xml
	<project>
		<modelVersion>4.0.0</modelVersion>
		<groupId>example</groupId>
		<artifactId>my-dsl-maven-plugin</artifactId>
		<version>1.0.0</version>
		<packaging>maven-plugin</packaging>
		<!-- Add the <build> block shown above here. -->
	</project>
	```

   In that same POM, set `<mode>compiler</mode>` inside the Quassar plugin's `<configuration>` in the `<build>` block above.

2. **Generate the plugin starter.** Run `mvn generate-sources`. The Quassar Maven plugin generates the typed model API from M2 and creates an editable Mojo with a `generate` goal if the Java source directory is empty.

3. **Write the code generation templates.** Choose any template engine for your compiler. The Quassar SDK includes its own Template Engine, which uses `.itr` files. If you use it, put your templates in `src/main/resources` of the DSL plugin project. This guide assumes Java output, and you can write templates for any other programming language. [Generated Template API](docs/generated-template-api.md) · [Template examples](docs/template-engine-examples.md) · [Template syntax reference](docs/template-engine-syntax.md)

4. **Load the model and render the templates.** In the Mojo’s `generate` goal, load M1 through the generated model API using the URL supplied by the `model` parameter. With Quassar Template Engine, call the generated template class’s `render` method with that model, then write the resulting files to the consumer project’s generated sources directory.

5. **Install your DSL plugin.** Run `mvn install` in the DSL plugin project to install it in your local Maven repository.

6. **Create a Maven project that uses your DSL plugin.** Create a new Maven project for the application that will use the generated code. In its `pom.xml`, add your DSL plugin under `<build><plugins>` using the coordinates from step 1. Set `<model>` to the M1 download URL from Quassar.

	```xml
	<build>
		<plugins>
			<plugin>
				<groupId>example</groupId>
				<artifactId>my-dsl-maven-plugin</artifactId>
				<version>1.0.0</version>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals><goal>generate</goal></goals>
					</execution>
				</executions>
				<configuration>
					<model>https://quassar.io/models/[code]</model>
				</configuration>
			</plugin>
		</plugins>
	</build>
	```

   Replace `[code]` with the M1 code and run `mvn compile` in the consumer project. Maven runs your DSL plugin before compiling the Java sources.

   The generated compiler starter also accepts an output path when it includes a model and a template.

   | Parameter | Default | Command-line property |
   | --- | --- | --- |
   | `model` | Required M1 download URL | `-Dquassar.model` |
   | `outputFile` | `${project.build.directory}/generated-resources/model.json` | `-Dquassar.output` |

### Interpreter

Build a Java application that loads a model (M1) and executes the behavior it describes at runtime.

```text
Model (M1) → Your Java interpreter → Runtime behavior
```

1. **Generate the interpreter starter.** Set `<mode>interpreter</mode>` in the Quassar plugin configuration and run `mvn generate-sources`. The plugin generates a typed Java API from M2 and creates an editable `Interpreter.java` if the Java source directory is empty.
2. **Load and navigate the model.** In `Interpreter`, pass the M1 download URI to the generated model class's `load` method. Use the generated getters to access the model's root concepts, then follow their children and references to read the data your interpreter needs.

   For example, a `Workflows` DSL might define `Workflow` as a repeatable root concept, `Step` as a repeated child, and `action` as a reference from each step to an `Action`. With `<package>example</package>`, the generated API lets you inspect the workflow in `Interpreter.java`.

	```java
	import example.model.WorkflowsModel;
	import java.net.URI;

	public final class Interpreter {
		public static void main(String[] args) throws Exception {
			var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));

			for (var workflow : model.workflows()) {
				System.out.println(workflow.identifier());
				for (var step : workflow.steps())
					System.out.println(step.action().identifier());
			}
		}
	}
	```

   Replace `[code]` with the code of your M1 model in Quassar.

3. **Implement and run the interpreter.** Define what each concept does in Java. For example, if a concept declares a sequence of steps, your interpreter reads those steps from M1 and calls the Java operation for each one in order. Run the application to interpret the model at that URL.

For direct template loading and frame customization, see the advanced API. [Template Engine API](docs/template-engine-api.md)

## Build the SDK from source

Use Java 17 or later and Maven 3.9.9 or later. From the repository root, run:

```sh
mvn clean install
```

This runs the tests and installs the SDK modules into your local Maven repository. The Model Reader tests cover nested models, references, and several input formats. [Model Reader test fixtures](model-reader/src/test/resources/tara/README.md)

## Quassar Maven plugin parameters

Set these parameters inside the Quassar plugin's `<configuration>`.

| Parameter | Default | Purpose |
| --- | --- | --- |
| `package` | Project `groupId` (`${project.groupId}`) | Base package for generated Java classes. Model classes go in `<package>.model` and template classes in `<package>.templates`. |
| `metamodel` | Unset | Download URL for M2, or a local M2 folder or ZIP URL. Omit it to generate Java classes from templates alone. |
| `mode` | `interpreter` | Use `compiler` to scaffold a DSL Maven plugin, `interpreter` to scaffold a Java application, or `none` to skip starter files. |
| `templateDirectory` | `src/main/resources` | Search for `.itr` files from the resources root. Set a relative path such as `templates` to search from `src/main/resources/templates`. |
| `outputDirectory` | `${project.basedir}/src/main/gen` | Directory where the plugin writes generated Java source files. |

[MIT License](LICENSE)

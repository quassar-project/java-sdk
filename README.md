# Quassar Java SDK

Use the Quassar Java SDK to build a compiler or an interpreter for models written in your own domain-specific language (DSL).

## Create your DSL and models

1. **Create your DSL.** Open [Quassar Forge](https://quassar.io/forge) and select **New DSL**. To create the DSL, define its metamodel (M2) with the concepts, properties, and relationships that models can express.

2. **Create a model.** Once your DSL is available, open [Quassar Studio](https://quassar.io/studio) and select **New Model**, then choose your DSL. Use the concepts defined in M2 to describe a specific case in your domain. The same DSL can support many models.

<a id="use-your-models-from-java"></a>

3. **Add the Quassar Maven plugin.** Open your existing Maven project in your IDE and add the configuration below to its `pom.xml`. Set `maven.compiler.release` to `17` or later and use Maven 3.9.9 or later.

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

4. **Set the metamodel.** In `<metamodel>`, replace `[code]` with your M2 code from Quassar.
5. **Choose how to process models.** Set `<mode>` to `compiler` to start a Maven plugin that generates source code from M1 before the consumer project compiles. Use `interpreter` to start a Java application that loads M1 and executes its behavior at runtime.

### Compiler

If you choose `compiler`, you will build a Maven plugin for your DSL. It takes a model (M1) and writes the generated source code to the consuming project's generated sources directory.

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

3. **Write the code generation templates.** Choose any template engine for your compiler. The Quassar SDK includes its own Template Engine, which uses `.itr` files. If you use it, put your templates in `src/main/resources` of the DSL plugin project. This guide assumes Java output, and you can write templates for any other programming language. The [Generated Template API](docs/generated-template-api.md) shows how to render an M1 model. Use the [template examples](docs/template-engine-examples.md) and the [syntax reference](docs/template-engine-syntax.md) to write your own templates.

4. **Load the model and render the templates.** In the Mojo’s `generate` goal, load M1 through the generated model API using the URL supplied by the `model` parameter. With Quassar Template Engine, call the generated template class’s `render` method with that model, then write the resulting files to the consumer project’s generated sources directory.

   Maven injects the M1 URL from the consumer POM’s `<model>` element in step 6 into the Mojo’s `model` field. Maven also supplies `outputDirectory` and injects the current consumer project into `project` through `${project}`. The Mojo uses it to register the generated sources. This implementation writes one Java file per workflow using the [generated workflow template](docs/generated-template-api.md#example-generate-java-from-a-workflow).

	```java
	@Parameter(property = "quassar.model", required = true)
	private URL model;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			var workflows = WorkflowsModel.load(model);
			var output = outputDirectory.toPath().toAbsolutePath();
			Files.createDirectories(output);

			for (var workflow : workflows.workflows()) {
				var filename = workflow.identifier() + "Workflow.java";
				String source = WorkflowTemplate.render(workflow);
				Files.writeString(output.resolve(filename), source);
			}

			project.addCompileSourceRoot(output.toString());
		} catch (IOException exception) {
			throw new MojoExecutionException("Could not generate workflow sources", exception);
		}
	}
	```

   Import `example.model.WorkflowsModel`, `example.templates.WorkflowTemplate`, `java.io.IOException`, `java.net.URL`, and `java.nio.file.Files` in the Mojo. Import `MavenProject` from `org.apache.maven.project` and `Parameter` from `org.apache.maven.plugins.annotations`.

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
					<outputDirectory>${project.basedir}/src/main/gen</outputDirectory>
				</configuration>
			</plugin>
		</plugins>
	</build>
	```

   Replace `[code]` with the M1 code and run `mvn compile` in the consumer project. Maven runs your DSL plugin before compiling the Java sources.

   The generated compiler starter accepts an output directory. With a single template, it writes `GeneratedModel.java` there. Customize the Mojo to choose the filenames and templates for your compiler.

   Pass the required M1 download URL through `<model>`. Set `<outputDirectory>` to change the default output directory, `src/main/gen`.

### Interpreter

If you choose `interpreter`, you will build a Java application for your DSL. It loads a model (M1) and executes the behavior it describes at runtime.

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

For direct template loading and frame customization, see the [Template Engine API](docs/template-engine-api.md).

## Quassar Maven plugin parameters

The table lists the available parameters. Add only the ones your project needs to the Quassar plugin's `<configuration>`.

| Parameter | Default | Purpose |
| --- | --- | --- |
| `package` | Project `groupId` (`${project.groupId}`) | Base package for generated Java classes. Model classes go in `<package>.model` and template classes in `<package>.templates`. |
| `metamodel` | Unset | Download URL for M2, or a local M2 folder or ZIP URL. Omit it to generate Java classes from templates alone. |
| `mode` | Unset | Use `compiler` to scaffold a DSL Maven plugin, `interpreter` to scaffold a Java application, or `none` to skip starter files. Leaving it unset also skips starter files. |
| `templateDirectory` | `src/main/resources` | Search for `.itr` files from the resources root. Set a relative path such as `templates` to search from `src/main/resources/templates`. |
| `outputDirectory` | `${project.basedir}/src/main/gen` | Directory where the plugin writes generated Java source files. |

The SDK is distributed under the [MIT License](LICENSE).

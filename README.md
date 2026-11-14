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
					<mode>[mode]</mode>
					<input>
						<model>https://quassar.io/models/[code]</model>
					</input>
					<output>example</output>
				</configuration>
			</plugin>
		</plugins>
	</build>
	```

4. **Set the metamodel.** In `<model>` inside `<input>`, replace `[code]` with your DSL metamodel code from Quassar.
5. **Choose how to process models.** Set `<mode>` to `compiler` to build a Maven plugin that generates source code from M1 before the consumer project compiles. Set it to `interpreter` to build a Java application that loads M1 and executes its behavior at runtime.

During `generate-sources`, the Quassar plugin writes both APIs under `outputDirectory` (`src/main/gen` by default). The `output` parameter sets their base Java package and defaults to the project's `groupId`.

| Generated API | Input | Java package with `<output>example</output>` |
| --- | --- | --- |
| [Generated Model API](docs/generated-model-api.md) | The M2 metamodel supplied through `input.model` | `example.model` |
| [Generated Template API](docs/generated-template-api.md) | The `.itr` files found under `input.templates` | `example.templates` |

Your compiler or interpreter supplies M1 when it loads the model through the Generated Model API.

### Compiler

If you choose `compiler`, you will build a Maven plugin for your DSL. It takes a model (M1) and writes the generated source code to the consuming project's generated sources directory.

```text
Model (M1) → Your Maven plugin → Source code in the generated sources directory
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

2. **Generate the plugin starter.** Run `mvn generate-sources`. The Quassar Maven plugin generates the typed model API from M2 and creates an editable Mojo with a `generate` goal if the Java source directory is empty.

3. **Write the code generation templates.** Choose any template engine for your compiler. The following example uses the Workflows DSL and the `example` base package from the POM above. The Quassar SDK includes its own Template Engine, which uses `.itr` files. If you use it, save `workflow.itr` in the DSL plugin project’s resources and run `mvn generate-sources` again to generate `WorkflowTemplate`. You can write templates for Java or other target languages. The [Generated Template API](docs/generated-template-api.md) shows how to render an M1 model. Use the [template examples](docs/template-engine-examples.md) and the [syntax reference](docs/template-engine-syntax.md) to write your own templates.

4. **Load the model and render the templates.** In the Mojo’s `generate` goal, load M1 through the generated model API using the URL supplied by `input.model`. With Quassar Template Engine, call the generated template class’s `render` method with that model, then write the resulting files to the consumer project’s generated sources directory.

   Maven injects the M1 URL from the consumer POM’s `<input><model>` configuration in step 6 into the Mojo’s `input.model` field. Maven also supplies `output` and injects the current consumer project into `project` through `${project}`. The Mojo uses it to register the generated sources. The snippet uses the fields and nested `Input` class already declared in the generated Mojo. It writes one Java file per workflow using the [generated workflow template](docs/generated-template-api.md#example-generate-java-from-a-workflow).

	```java
	@Parameter(required = true)
	private Input input;

	@Parameter(property = "quassar.output", defaultValue = "${project.basedir}/src/main/gen", required = true)
	private File output;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	private void generateSources() throws IOException {
		var workflows = WorkflowsModel.load(input.model);
		var directory = output.toPath().toAbsolutePath();
		Files.createDirectories(directory);

		for (var workflow : workflows.workflows()) {
			var filename = workflow.identifier() + "Workflow.java";
			String code = WorkflowTemplate.render(workflow);
			Files.writeString(directory.resolve(filename), code);
		}

		project.addCompileSourceRoot(directory.toString());
	}
	```

   In the generated Mojo’s `execute()`, replace the statements inside the existing `try` block with a call to `generateSources()`. Keep the generated exception handling.

   Import `example.model.WorkflowsModel`, `example.templates.WorkflowTemplate`, `java.io.File`, `java.io.IOException`, and `java.nio.file.Files` in the Mojo. Import `MavenProject` from `org.apache.maven.project` and `Parameter` from `org.apache.maven.plugins.annotations`.

5. **Install your DSL plugin.** Run `mvn install` in the DSL plugin project to install it in your local Maven repository.

6. **Create a Maven project that uses your DSL plugin.** Create a new Maven project for the application that will use the generated code. In its `pom.xml`, add your DSL plugin under `<build><plugins>` using the coordinates from step 1. Set `<model>` inside `<input>` to the M1 download URL from Quassar.

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
					<input>
						<model>https://quassar.io/models/[code]</model>
					</input>
					<output>${project.basedir}/src/main/gen</output>
				</configuration>
			</plugin>
		</plugins>
	</build>
	```

   Replace `[code]` with the M1 code and run `mvn compile` in the consumer project. Maven runs your DSL plugin before compiling the Java sources.

   In your DSL plugin, `<output>` sets the generated sources directory and defaults to `src/main/gen`.

### Interpreter

If you choose `interpreter`, you will build a Java application for your DSL. It loads a model (M1) and executes the behavior it describes at runtime.

```text
Model (M1) → Your Java interpreter → Runtime behavior
```

1. **Generate the interpreter starter.** Set `<mode>interpreter</mode>` in the Quassar plugin configuration and run `mvn generate-sources`. The plugin generates a typed Java API from M2 and creates an editable `Interpreter.java` if the Java source directory is empty.
2. **Load the workflow.** Use `WorkflowsModel` to load the M1 model from Quassar. The sample model defines an `OrderFulfillment` workflow with a `Confirm` step that runs `SendConfirmation`. Its fallback actions are `QueueForRetry` and `LogFailure`. [Sample M1 model](docs/model-reader-api.md#create-a-model-in-quassar)

3. **Register and execute actions.** Map each action identifier to a Java handler. Each handler returns whether it succeeded. If the primary action fails, run its fallbacks in the order declared in M1. If the primary action fails and has no fallbacks, or if a fallback fails, the interpreter returns `false`.

	```java
	public interface ActionHandler {
		boolean execute(String orderId);
	}

	boolean run(Workflow workflow, String orderId, Map<String, ActionHandler> handlers) {
		for (var step : workflow.steps()) {
			if (!step.enabled()) continue;
			if (handlers.get(step.action().identifier()).execute(orderId)) continue;

			var fallbacks = step.fallbackActions();
			if (fallbacks.isEmpty()) return false;
			for (var fallback : fallbacks)
				if (!handlers.get(fallback.identifier()).execute(orderId)) return false;
		}
		return true;
	}
	```

   Add these members to `Interpreter.java` and import `example.model.Workflow` and `java.util.Map`. Register a handler for each action used by the model. This example assumes the service methods return a boolean and the caller handles errors when loading M1.

	```java
	var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
	Map<String, Interpreter.ActionHandler> handlers = Map.of(
		"SendConfirmation", notifications::sendConfirmation,
		"QueueForRetry", retryQueue::enqueue,
		"LogFailure", auditLog::recordConfirmationFailure);

	boolean success = new Interpreter().run(model.workflows().get(0), orderId, handlers);
	```

   Replace `[code]` with your M1 code. The sample model contains one workflow, `OrderFulfillment`.

For direct template loading and frame customization, see the [Template Engine API](docs/template-engine-api.md).

## Quassar Maven plugin parameters

The table lists the available parameters. Add only the ones your project needs to the Quassar plugin's `<configuration>`.

| Parameter | Default | Purpose |
| --- | --- | --- |
| `mode` | Unset | Use `compiler` to scaffold a DSL Maven plugin, or `interpreter` to scaffold a Java application. Leave it unset to skip starter files. |
| `input.model` | Unset | Download URL for M2, or a local M2 folder or ZIP URL. Omit it to generate Java classes from templates alone. |
| `input.templates` | `resources root` | Path relative to the project’s resources root, such as `templates`. The Quassar plugin uses it during `generate-sources`. |
| `output` | `${project.groupId}` | Base package for generated Java classes. Model classes go in `<output>.model` and template classes in `<output>.templates`. |
| `outputDirectory` | `${project.basedir}/src/main/gen` | Directory where the plugin writes generated Java source files. |

The SDK is distributed under the [MIT License](LICENSE).

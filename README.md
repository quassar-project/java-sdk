# Quassar Java SDK

Quassar is a platform for model-driven engineering (MDE). Define a domain-specific language (DSL), create models with it, and use the Quassar Java SDK to build a code generator or an interpreter for those models. The examples below use Workflows, a DSL for defining workflows and their steps and actions. Its [sample metamodel (M2)](docs/sample-m2-model.md) defines the language, and its [sample model (M1)](docs/sample-m1-model.md) describes an order fulfillment workflow.

## Create your DSL and models

1. **Create your DSL.** Open [Quassar Forge](https://quassar.io/forge) and select **New DSL**. To create the DSL, define its metamodel (M2) with the concepts, properties, and relationships that models can express.

2. **Create a model.** Once your DSL is available, open [Quassar Studio](https://quassar.io/studio) and select **New Model**, then choose your DSL. Use the concepts defined in M2 to describe a specific case in your domain. The same DSL can support many models.

<a id="use-your-models-from-java"></a>

3. **Add the Quassar Maven plugin.** Open your existing Maven project in your IDE and add the configuration below to its `pom.xml`. The examples use `org.workflows` as the project’s `groupId`. Set `maven.compiler.release` to `17` or later and use Maven 3.9.9 or later.

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
					<input-model>https://quassar.io/models/[code]</input-model>
					<mode>[mode]</mode>
				</configuration>
			</plugin>
		</plugins>
	</build>
	```

4. **Set the metamodel.** In `<input-model>`, replace `[code]` with your DSL metamodel code from Quassar.
5. **Choose how to process models.** Set `<mode>` to `generator` to build a Maven plugin that generates source code from M1 before the consumer project compiles. Set it to `interpreter` to build a Java application that loads M1 and executes its behavior at runtime.

To generate source code from an M1 model, you can write templates that define the target code and use placeholders for values from the model. If you use Quassar Template Engine, write the templates as `.itr` files in your project’s resources. By default, the plugin searches from the project’s resources root. Set `input-templates` to a relative path to limit the search to a subdirectory.

During `generate-sources`, the Quassar plugin generates a Java API from the configured M2 metamodel and any `.itr` files it finds and writes the classes under `output-directory` (`src/main/gen` by default). The `output-package` parameter sets their base Java package and defaults to the project's `groupId`. The table shows the generated packages when the project’s `groupId` is `org.workflows` and `output-package` is omitted.

| Generated API | Input | Generated Java package |
| --- | --- | --- |
| [Generated Model API](docs/generated-model-api.md) | The M2 metamodel supplied through `input-model` | `org.workflows.model` |
| [Generated Template API](docs/generated-template-api.md) | The `.itr` files found in the configured resources directory | `org.workflows.templates` |

Your code generator or interpreter loads M1 through the Generated Model API. For template-based code generation, your generator then passes model objects to the Generated Template API to produce source code.

### Code generator

If you choose `generator`, you will build a Maven plugin for your DSL. It takes a model (M1) and writes the generated source code to the consuming project's generated sources directory.

```text
Model (M1) → Your Maven plugin → Source code in the generated sources directory
```

1. **Configure the POM.** Set `<mode>generator</mode>` in the Quassar plugin configuration above. In the same `pom.xml`, add `<packaging>maven-plugin</packaging>` directly under `<project>`, alongside `<artifactId>` and `<version>`.

	```xml
	<project>
		<modelVersion>4.0.0</modelVersion>
		<groupId>org.workflows</groupId>
		<artifactId>code-generator</artifactId>
		<version>1.0.0</version>
		<packaging>maven-plugin</packaging>
		<!-- Add the <build> block shown above here. -->
	</project>
	```

2. **Generate the plugin starter.** Run `mvn generate-sources`. The Quassar Maven plugin generates the typed model API from M2 and creates an editable Mojo with a `generate` goal if the Java source directory is empty.

3. **Write the code generation templates.** Choose any template engine for your generator. Your templates define the source code to produce in Java or another target language.

   If you use Quassar Template Engine, save your `.itr` files in the DSL plugin project’s resources. Run `mvn generate-sources` again to generate their Java classes.

   Use the [template cookbook](docs/template-engine-cookbook.md) and [syntax reference](docs/template-engine-syntax.md) to write templates. The [Generated Template API](docs/generated-template-api.md) shows how to render a model with the generated classes.

4. **Load the model and render the templates.** In the Mojo’s `generate` goal, load M1 through the generated model API using the M1 URL from the consumer POM. With Quassar Template Engine, call the generated template class’s `render` method with that model, then write the resulting files to the consumer project’s generated sources directory.

   Maven injects the M1 URL from the consumer POM’s `<model>` element inside `<input>` in step 6 into the Mojo’s `input.model` field. Maven also supplies `output` and injects the current consumer project into `project` through `${project}`. The Mojo uses it to register the generated sources. Keep the fields and nested `Input` class from the generated Mojo. The snippet shows those fields for context.

   **Workflows example.** Set `input-model` to the download URL of the [sample metamodel (M2)](docs/sample-m2-model.md). Save the example’s `workflow.itr` in your project’s resources and run `mvn generate-sources` to obtain `org.workflows.templates.WorkflowTemplate` with the POM above. The method below uses that class to write one Java file per workflow. [Sample templates](docs/sample-templates.md#workflowitr)

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

   Import `org.workflows.model.WorkflowsModel`, `org.workflows.templates.WorkflowTemplate`, `java.io.File`, `java.io.IOException`, and `java.nio.file.Files` in the Mojo. Import `MavenProject` from `org.apache.maven.project` and `Parameter` from `org.apache.maven.plugins.annotations`.

5. **Install your DSL plugin.** Run `mvn install` in the DSL plugin project to install it in your local Maven repository.

6. **Create a Maven project that uses your DSL plugin.** Create a new Maven project for the application that will use the generated code. In its `pom.xml`, add your DSL plugin under `<build><plugins>` using the coordinates from step 1. Set `<model>` inside `<input>` to the M1 download URL from Quassar.

	```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.workflows</groupId>
				<artifactId>code-generator</artifactId>
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
2. **Load the workflow.** Use `WorkflowsModel` to load the M1 model from Quassar. The sample `OrderFulfillment` workflow confirms the order, reserves stock, and has a disabled warehouse notification step. If `SendConfirmation` fails, the interpreter runs `QueueForRetry` and `LogFailure`. [Sample M1 model](docs/sample-m1-model.md)

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

   Add these members to `Interpreter.java`, then put the following call in your application’s order-processing code. Use `org.workflows.model.Workflow`, `org.workflows.model.WorkflowsModel`, `java.net.URI`, and `java.util.Map` where referenced. Register a handler for each action used by the model. This example assumes the service methods return a boolean and the caller handles errors when loading M1.

	```java
	var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
	Map<String, Interpreter.ActionHandler> handlers = Map.of(
		"SendConfirmation", notifications::sendConfirmation,
		"QueueForRetry", retryQueue::enqueue,
		"LogFailure", auditLog::recordConfirmationFailure,
		"ReserveStock", inventory::reserve,
		"NotifyWarehouse", warehouse::notifyOrder);

	boolean success = new Interpreter().run(model.workflows().get(0), orderId, handlers);
	```

   Replace `[code]` with your M1 code. The sample model contains one workflow, `OrderFulfillment`.

For direct template loading and frame customization, see the [Template Engine API](docs/template-engine-api.md).

## Quassar Maven plugin parameters

The table lists the available parameters. Only `mode` is required in the Quassar plugin’s `<configuration>`. Use these names as XML elements, such as `<input-model>`. Nested `<input>` and `<output>` blocks are also supported. When both forms set the same value, the hyphenated element takes precedence.

| Parameter | Default | Purpose |
| --- | --- | --- |
| `input-model` | Unset | M2 URL or local folder or ZIP URL. |
| `input-templates` | Resources root | Template path relative to the resources root. |
| `mode` | Required | `generator` for a Maven plugin, `interpreter` for a Java app. |
| `output-directory` | `src/main/gen` | Directory for generated Java sources. |
| `output-package` | Project `groupId` | Base package for the `.model` and `.templates` APIs. |

Omit `input-model` to generate from templates alone.

The SDK is distributed under the [MIT License](LICENSE).

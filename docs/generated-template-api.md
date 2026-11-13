# Generated Template API

The Quassar Maven plugin compiles each `.itr` template from the configured template directory into a Java class under `src/main/gen` during `generate-sources`. Call the generated class's static `render` method with a model decorator to produce the text defined by the template. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Place templates under resources

The plugin searches `src/main/resources` and its subdirectories for `.itr` files by default. To group workflow templates, save them under `src/main/resources/workflows/`. The plugin discovers them with its default configuration.

## Generate Java from a workflow

The example turns the `OrderFulfillment` workflow into a Java class whose `actions()` method lists the actions in execution order. The DSL definition (M2) determines what your Java code can read, and the workflow model (M1) supplies the values to render. [Workflow metamodel](generated-model-api.md#workflow-metamodel) · [Sample M1 model](model-reader-api.md#create-a-model-in-quassar)

1. **Configure the Quassar plugin.** In your project's `pom.xml`, set `<package>example</package>` and point `<metamodel>` to the download URL of the Workflows metamodel (M2). [Maven setup](../README.md#use-your-models-from-java)
2. **Add the template.** Save the template below as `src/main/resources/workflows/workflow.itr`. It defines the Java class to produce for a workflow.
3. **Generate the Java API.** Run `mvn generate-sources`. The plugin generates `example.model.WorkflowsModel` from M2 and compiles the template into `example.templates.WorkflowTemplate`. Both classes are written under `src/main/gen`.
4. **Render a workflow.** In the Java program below, load M1 through `WorkflowsModel`, select a workflow, and pass it to `WorkflowTemplate.render(workflow)`. Save the returned source as `OrderFulfillmentWorkflow.java` for the sample model.

```itr
rule type(Workflow)
	import java.util.List;

	public final class $identifier~Workflow {
		public List<String> actions() {
			return List.of($step...[, ]);
		}
	}

rule type(Step)
	$action

rule type(Action)
	$identifier+JsonString
```

The workflow rule renders each child `Step`. The step rule follows its `action` reference, and the action rule emits its identifier as a quoted string.

Save this program as `GenerateWorkflow.java` and replace `[code]` with the code of your M1 model in Quassar.

```java
import example.model.WorkflowsModel;
import example.templates.WorkflowTemplate;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenerateWorkflow {
	public static void main(String[] args) throws Exception {
		var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
		var workflow = model.workflows().get(0);
		String source = WorkflowTemplate.render(workflow);

		Files.writeString(Path.of(workflow.identifier() + "Workflow.java"), source);
	}
}
```

For the sample M1, this writes `OrderFulfillmentWorkflow.java`.

```java
import java.util.List;

public final class OrderFulfillmentWorkflow {
	public List<String> actions() {
		return List.of("SendConfirmation");
	}
}
```

`WorkflowTemplate.java` contains the compiled template rules. `OrderFulfillmentWorkflow.java` is the source file produced when you render M1. The generated template class can render without reading the original `.itr` file at runtime.

## Write your own templates

Use a rule to select the model concept and write the desired output in the rule body. Insert placeholders for model values and add rules for children or references that need their own output. [Workflow template examples](template-engine-examples.md) · [Template syntax reference](template-engine-api.md#template-syntax-reference)

For runtime template loading, custom adapters, or direct frame construction, see the advanced API. [Template Engine API](template-engine-api.md)

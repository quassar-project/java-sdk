# Generated Template API

The Quassar Maven plugin compiles each `.itr` template from the configured template directory into a Java class under `src/main/gen` during `generate-sources`. Call the generated class's static `render` method with a model decorator to produce the text defined by the template. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Place templates under resources

By default, the plugin searches the root of Maven resources, `src/main/resources`, and its subdirectories. To search only within `src/main/resources/templates`, add this setting inside the Quassar plugin's `<configuration>` in your POM.

```xml
<templateDirectory>templates</templateDirectory>
```

Use a path relative to `src/main/resources`, such as `solution/templates` for a nested directory.

## Generate Java from a workflow

The example turns the `OrderFulfillment` workflow into a Java class whose `actions()` method lists the actions in execution order. The DSL definition (M2) determines what your Java code can read, and the workflow model (M1) supplies the values to render. [Workflow metamodel](generated-model-api.md#workflow-metamodel) · [Sample M1 model](model-reader-api.md#create-a-model-in-quassar)

With the generated classes `example.model.WorkflowsModel` and `example.templates.WorkflowTemplate` already available, load M1 and pass a workflow to `WorkflowTemplate.render(workflow)`. Here are the rules from `workflow.itr` that the generated template applies.

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

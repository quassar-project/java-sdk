# Generated Template API

The Quassar Maven plugin compiles each `.itr` template from the configured template directory into a Java class under `src/main/gen` during `generate-sources`. Call the generated class's static `render` method with a model decorator to produce the text defined by the template. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Place templates under resources

During `generate-sources`, the Quassar plugin searches from the project’s resources root. Set `templateDirectory` to a path relative to that root to limit the search to a subdirectory.

```xml
<templateDirectory>templates</templateDirectory>
```

Use `solution/templates` for a nested directory. The generated Java class contains the template rules and can render from the JAR without opening a template resource.

## Example: Generate Java from a workflow

This example uses a DSL called Workflows. Its metamodel (M2) defines workflows, steps, and actions. The sample model (M1) contains a workflow named `OrderFulfillment`. Rendering that workflow produces a Java class whose `actions()` method lists the actions in execution order. [Workflow metamodel](generated-model-api.md#workflow-metamodel) · [Sample M1 model](model-reader-api.md#create-a-model-in-quassar)

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

Use a rule to select the model concept and write the desired output in the rule body. Insert placeholders for model values and add rules for children or references that need their own output. [Template examples](template-engine-examples.md) · [Template syntax reference](template-engine-syntax.md)

For runtime template loading, custom adapters, or direct frame construction, see the advanced API. [Template Engine API](template-engine-api.md)

# Generated Template API

The Quassar Maven plugin compiles each `.itr` template in `src/main/resources` into a Java class under `src/main/gen` during `generate-sources`. Call the generated class's static `render` method with a model decorator to produce the text defined by the template. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Generate Java from a workflow

With `<package>example</package>`, save the following template as `src/main/resources/workflow.itr`. Run `mvn generate-sources` to generate `src/main/gen/example/templates/WorkflowTemplate.java`. The example uses the `WorkflowsModel` decorators generated from M2. [Generated Model API](generated-model-api.md) · [Sample M1 model](model-reader-api.md#create-a-model-in-quassar)

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

# Generated Template API

Call a generated template class's static `render` method with a model decorator to produce the text defined by the template. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Generation inputs and output

During `generate-sources`, the Quassar Maven plugin compiles `.itr` files found under `input-templates` into Java classes under `output-directory` (`src/main/gen` by default).

The plugin adds the `templates` subpackage to the base package set by `output-package`, which defaults to the project's `groupId`. With the default package and a project `groupId` of `org.workflows`, `workflow.itr` generates `org.workflows.templates.WorkflowTemplate` at `src/main/gen/org/workflows/templates/WorkflowTemplate.java` by default.

## Place templates under resources

During `generate-sources`, the Quassar plugin searches from the project’s resources root. Set `input-templates` to a path relative to that root to limit the search to a subdirectory.

```xml
<input-templates>templates</input-templates>
```

Use `solution/templates` for a nested directory. The generated Java class contains the template rules and can render from the JAR without opening a template resource.

## Example: Generate Java from a workflow

This example uses a DSL called Workflows. Its metamodel (M2) defines workflows, steps, and actions. The sample model (M1) contains a workflow named `OrderFulfillment`. Rendering that workflow produces a Java class whose `actions()` method lists all primary actions in model order, including those of disabled steps. [Workflow metamodel](sample-m2-model.md) · [Sample M1 model](sample-m1-model.md)

With the generated classes `org.workflows.model.WorkflowsModel` and `org.workflows.templates.WorkflowTemplate` already available, load M1 and pass a workflow to `WorkflowTemplate.render(workflow)`. The generated class applies the rules from the [sample workflow template](sample-templates.md#workflowitr).

Save this program as `GenerateWorkflow.java` and replace `[code]` with the code of your M1 model in Quassar.

```java
import org.workflows.model.WorkflowsModel;
import org.workflows.templates.WorkflowTemplate;

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
		return List.of("SendConfirmation", "ReserveStock", "NotifyWarehouse");
	}
}
```

`WorkflowTemplate.java` contains the compiled template rules. `OrderFulfillmentWorkflow.java` is the source file produced when you render M1. The generated template class can render without reading the original `.itr` file at runtime.

## Write your own templates

Use a rule to select the model concept and write the desired output in the rule body. Insert placeholders for model values and add rules for children or references that need their own output. [Template cookbook](template-engine-cookbook.md) · [Template syntax reference](template-engine-syntax.md)

For runtime template loading, custom adapters, or direct frame construction, see the advanced API. [Template Engine API](template-engine-api.md)

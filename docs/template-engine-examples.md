# Workflow template examples

These examples render the `OrderFulfillment` workflow from the Model Reader guide. It has a `Confirm` step that calls `SendConfirmation` and lists `QueueForRetry` and `LogFailure` as fallback actions. [M1 example](model-reader-api.md#create-a-model-in-quassar) · [Workflow metamodel](generated-model-api.md#workflow-metamodel)

Save each template in its own `.itr` file. Load M1 using the generated API, then pass the workflow or step to the template's `render` method.

```java
import example.model.WorkflowsModel;
import io.quassar.templating.TemplateEngine;
import java.net.URI;
import java.nio.file.Path;

var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
var workflow = model.workflows().get(0);
var step = workflow.steps().get(0);
var engine = TemplateEngine.with(Path.of("example.itr"));
```

Replace `[code]` with the code of the M1 model. Generated decorators expose children by their concept name in templates, so use `$step` for the Java getter `steps()`. [Template syntax reference](template-engine-api.md#template-syntax-reference)

## Choose a rule from the data

Render `step`. The first matching rule determines the label.

```itr
rule type(Step) and attribute(enabled, true)
	$label: enabled

rule type(Step)
	$label: disabled
```

```text
Confirm order: enabled
```

## Combine conditions

Render `step`. The first rule matches a step with a 30-second timeout that is enabled or has fallback actions.

```itr
rule type(Step) and attribute(timeoutSeconds, 30) and (attribute(enabled, true) or attribute(fallbackActions))
	$label: ready

rule type(Step)
	$label: review required
```

```text
Confirm order: ready
```

## Choose output based on the calling slot

Render `step`. The action reached through `$action` is formatted differently from actions reached through `$fallbackActions`.

```itr
rule type(Step)
	Primary: $action
	Fallback: $fallbackActions...[, ]

rule type(Action) and trigger(action)
	$name+UpperCase

rule type(Action)
	$name
```

```text
Primary: SEND CONFIRMATION
Fallback: Queue for retry, Log failure
```

## Optional values and fallback text

Render `step`. The sample step has no schedule, so the optional expression renders its fallback text.

```itr
rule type(Step)
	$label: <<$schedule?Run immediately>>
```

```text
Confirm order: Run immediately
```

## Render a list and format each value

Render `step`. The brackets after `...` specify the separator between rendered actions.

```itr
rule type(Step)
	Fallbacks: $fallbackActions...[, ]

rule type(Action)
	[$name+UpperCase]
```

```text
Fallbacks: [QUEUE FOR RETRY], [LOG FAILURE]
```

## Produce JSON with optional fields

Render `workflow`. Put the comma inside the optional expression so the result stays valid JSON when a step has no schedule.

```itr
rule type(Workflow)
	{"steps": [$step...[, ]]}

rule type(Step)
	{"label": $label+JsonString, "timeoutSeconds": $timeoutSeconds<<, "schedule": $schedule+JsonString>>}
```

```json
{"steps": [{"label": "Confirm order", "timeoutSeconds": 30}]}
```

## Escape template characters

Render `step`. Use `~` to end a placeholder before adjacent text, `$$` for a literal dollar sign, and `$<<` and `$>>` for literal expression delimiters.

```itr
rule type(Step)
	Timeout: $timeoutSeconds~s · Token: $$TIMEOUT · $<<preview$>>
```

```text
Timeout: 30s · Token: $TIMEOUT · <<preview>>
```

## Read values from the root and a named path

Render `workflow`. The named path `step.action` follows the first step to its action. Inside the step rule, `root` refers to the workflow being rendered.

```itr
rule type(Workflow)
	First action: $<step.action>name
	$step...[$NL]

rule type(Step)
	$label — $<root>identifier
```

```text
First action: Send confirmation
Confirm order — OrderFulfillment
```

## Access the containing workflow

Render `workflow`. Each step reads the identifier of its containing workflow.

```itr
rule type(Workflow)
	$step...[$NL]

rule type(Step)
	$label ($<container>identifier)
```

```text
Confirm order (OrderFulfillment)
```

## Generate a Java record

Save this as `step-config.itr` and render `step` with it. The generated record holds the step settings that a Java workflow runner can read through typed accessors.

```itr
rule type(Step)
	public record $identifier~Config() {
		public String actionName() { return $action; }
		public int timeoutSeconds() { return $timeoutSeconds; }
	}

rule type(Action)
	$name+JsonString
```

```java
public record ConfirmConfig() {
	public String actionName() { return "Send confirmation"; }
	public int timeoutSeconds() { return 30; }
}
```

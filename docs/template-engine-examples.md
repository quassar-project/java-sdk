# Template examples

These examples render the `OrderFulfillment` workflow from the Model Reader guide. It has a `Confirm` step that calls `SendConfirmation` and lists `QueueForRetry` and `LogFailure` as fallback actions. [M1 example](model-reader-api.md#create-a-model-in-quassar) · [Workflow metamodel](generated-model-api.md#workflow-metamodel)

Save each template in its own `.itr` file under `src/main/resources` and run `mvn generate-sources`. Call the corresponding generated template class's `render` method with `workflow` or `step`. [Generated Template API](generated-template-api.md)

```java
import example.model.WorkflowsModel;
import java.net.URI;

var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
var workflow = model.workflows().get(0);
var step = workflow.steps().get(0);
```

Replace `[code]` with your M1 model's code. Use `$step` in the template for the children exposed by `workflow.steps()`. [Template syntax reference](template-engine-syntax.md)

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

## Generate Java from a workflow

The generated API guide shows the workflow template, the call to its generated Java class, and the resulting Java source. [Generate Java from a workflow](generated-template-api.md#generate-java-from-a-workflow)

## Generate a Java step configuration

Save this as `step-config.itr` and render a step with `StepConfigTemplate.render(step)`. The first rule handles enabled steps. The second supplies a zero timeout for disabled steps.

```itr
rule type(Step) and attribute(enabled, true)
	public record $identifier~Config() {
		public int timeoutSeconds() { return $timeoutSeconds; }
	}

rule type(Step)
	public record $identifier~Config() {
		public int timeoutSeconds() { return 0; }
	}
```

The enabled `Confirm` step produces this Java source.

```java
public record ConfirmConfig() {
	public int timeoutSeconds() { return 30; }
}
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

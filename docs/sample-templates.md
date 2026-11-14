# Sample templates

These templates use the Workflows DSL and its sample model. Save each block in a separate `.itr` file in the project’s resources and run `mvn generate-sources` to generate the Java template classes. [Sample metamodel (M2)](sample-m2-model.md) · [Sample M1 model](sample-m1-model.md) · [Generated Template API](generated-template-api.md)

## workflow.itr

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

## step-config.itr

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

## step-action-config.itr

```itr
rule type(Step)
	public record $identifier~ActionConfig() {
		public String actionName() { return $action; }
		public int timeoutSeconds() { return $timeoutSeconds; }
	}

rule type(Action)
	$name+JsonString
```

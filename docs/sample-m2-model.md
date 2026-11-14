# Sample M2 metamodel

Create a DSL named `Workflows` in [Quassar Forge](https://quassar.io/forge) using this metamodel.

The `Workflows` DSL defines workflows with an entry point and a sequence of steps. Each step references an action and can specify fallback actions.

```tara
dsl metta

Concept Workflow
	Concept:Single:Required EntryPoint
		def string name
	has:{1..*} Step

Concept Step as component
	def string label
	def integer timeoutSeconds
	def boolean enabled
	def Action action
	def Action[] fallbackActions
	facet Execution
		sub Scheduled
			def string schedule

Concept Action
	def string name
```

Use the published DSL to create the [sample M1 model](sample-m1-model.md).

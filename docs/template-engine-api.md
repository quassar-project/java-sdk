# Template Engine API for advanced use

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert. The engine needs Java 17 or later. Use this guide to load templates directly at runtime or customize rendering. For templates compiled by the Quassar Maven plugin, start with the generated API. [Generated Template API](generated-template-api.md)

## Add the dependency for standalone use

Add the dependency below **only if your project does not use the Quassar Maven plugin**. With `<extensions>true</extensions>`, the plugin adds `template-engine` to your project automatically. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>template-engine</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Load a template

Use `TemplateEngine.with(...)` to load an `.itr` file at runtime. You can also define a template in Java and pass it to the same factory. [Build templates explicitly](#build-templates-explicitly)

| Factory | Input |
| --- | --- |
| `TemplateEngine.with(path)` | A local `.itr` file that the engine opens and closes |
| `TemplateEngine.with(input)` | An `.itr` input stream, read as UTF-8 |
| `TemplateEngine.with(template)` | An existing template |
| `TemplateEngine.with(rules)` | An iterable of rendering rules |
| `TemplateEngine.with(rules, configuration)` | Rules with explicit locale and line endings |

The engine opens and closes files passed as a `Path`. If you pass an `InputStream`, close it in your code after the engine loads the template. Unreadable or malformed templates raise `TemplateReadException`.

This example assumes `workflow.itr` is in the current directory. [Template examples](template-engine-examples.md) · [Template syntax reference](template-engine-syntax.md)

```java
import io.quassar.templating.TemplateEngine;
import java.nio.file.Path;

var engine = TemplateEngine.with(Path.of("workflow.itr"));
```

## Render an object

Pass an input to `render` to get the result as a string. Here, `workflow` is a decorator obtained from a loaded M1 model. [Generated Model API](generated-model-api.md) · [Workflow template and output](generated-template-api.md#generate-java-from-a-workflow)

```java
String output = engine.render(workflow);
```

The engine uses reflection to convert generated models, model decorators, Java class instances, and records into frames. You can also pass a frame directly to `render`. [Build frames explicitly](#build-frames-explicitly)

## Advanced usage

### Read a template

Use `TemplateReader.read(Path)` to get a `Template` from an `.itr` file. You can inspect its rules or pass the template to `TemplateEngine.with(template)` to render it.

```java
import io.quassar.templating.TemplateEngine;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.model.Template;
import java.nio.file.Path;

Template template = TemplateReader.read(Path.of("workflow.itr"));
var engine = TemplateEngine.with(template);
```

### Build templates explicitly

```java
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

Template template = new Template()
	.add(new Rule().condition(allTypes("Step")).output(placeholder("identifier")))
	.add(new Rule().condition(allTypes("Action")).output(placeholder("name")));
```

### Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(workflow);
String output = engine.render(frame);
```

`Frames.from(...)` adapts records and regular Java objects. It includes inherited fields and converts nested object, list, map and array fields. Static and transient fields are excluded. Each conversion creates its own frame tree and parent links. Passing an existing `Frame` returns that frame.

### Build frames explicitly

Use `Frames.builder(...)` to name the type and add slots yourself. Scalar values such as strings and numbers are added directly.

```java
Frame step = Frames.builder("Step")
	.add("identifier", "Confirm")
	.add("timeoutSeconds", 30)
	.add("enabled", true)
	.toFrame();
String output = engine.render(step);
```

### Rendering configuration

```java
import io.quassar.templating.TemplateEngine;
import io.quassar.templating.model.Template.Configuration;
import io.quassar.templating.model.Template.Configuration.LineSeparator;

var configuration = new Configuration(Locale.ENGLISH, LineSeparator.LF);
TemplateEngine engine = TemplateEngine.with(template.ruleSet(), configuration);
```

Configuration controls locale-sensitive built-in formatting and output line endings. The default is English with LF line endings.

### Register an object adapter

The `step-config.itr` template expects a `Step` with the slots `identifier`, `timeoutSeconds`, and `enabled`.

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

`ExternalStep` exposes `id` and `timeout`. Register an adapter to present it as a `Step`, map its fields to the names the template expects, and set `enabled` to `true` for these external steps.

```java
record ExternalStep(String id, int timeout) {}

TemplateEngine engine = TemplateEngine.with(Path.of("step-config.itr"))
	.add(ExternalStep.class, (source, frame) -> frame
		.add("Step")
		.add("identifier", source.id())
		.add("timeoutSeconds", source.timeout())
		.add("enabled", true));

String output = engine.render(new ExternalStep("Confirm", 30));
```

The adapter makes the enabled rule match, so `output` contains this Java source.

```java
public record ConfirmConfig() {
	public int timeoutSeconds() { return 30; }
}
```

### Register a formatter

Use `engine.add(name, formatter)` to register a formatter before rendering. Save this template as `step-wrapped.itr`. The `+Wrapped` suffix applies the formatter named `Wrapped` to the step's identifier.

```itr
rule type(Step)
	$identifier+Wrapped
```

```java
var engine = TemplateEngine.with(Path.of("step-wrapped.itr"))
	.add("Wrapped", value -> "[" + value + "]");

String output = engine.render(workflow.steps().get(0));
```

For the `Confirm` step, the formatter produces this output.

```text
[Confirm]
```

### Frame context

Use frame context when a rule needs a value from another object while rendering the current one. For example, a step's output can include the identifier of the workflow it belongs to.

| Expression | When to use it |
| --- | --- |
| `$<container>identifier` | Read the identifier of the object that contains the current one. In a step rule, this is the containing workflow's identifier. |
| `$<root>identifier` | Read the identifier of the object passed to `render`. When rendering a workflow, its identifier is available even from a rule several levels deep. |
| `$<step.action>name` | From a workflow rule, follow its first step to the referenced action and read the action's name. This lets you show the first action in a workflow summary. |

When you call `engine.render(workflow)`, the workflow rule renders its steps. Each step rule reads the identifier of its containing workflow.

```itr
rule type(Workflow)
	$step...[$NL]

rule type(Step)
	$label ($<container>identifier)
```

For the sample workflow, the output is `Confirm order (OrderFulfillment)`. [Read values from the root and a named path](template-engine-examples.md#read-values-from-the-root-and-a-named-path)

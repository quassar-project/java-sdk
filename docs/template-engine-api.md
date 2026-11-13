# Template Engine API for advanced use

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert. The engine needs Java 17 or later. Use this guide to load templates directly at runtime or customize rendering. For templates compiled by Maven, start with the generated API. [Generated Template API](generated-template-api.md)

## Add the dependency for standalone use

Add the dependency below **only if your project does not use the Quassar Maven plugin**. With `<extensions>true</extensions>`, the plugin adds `template-engine` to your project automatically. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>template-engine</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Load and render a template

Use `TemplateEngine.with(Path)` to load an existing `.itr` file at runtime. This example assumes `workflow` is a decorator loaded from M1 and `workflow.itr` is in the current directory. [Generated Model API](generated-model-api.md) · [Workflow template and output](generated-template-api.md#generate-java-from-a-workflow)

```java
import io.quassar.templating.TemplateEngine;
import java.nio.file.Path;

var engine = TemplateEngine.with(Path.of("workflow.itr"));
String source = engine.render(workflow);
```

The engine reads and closes the file when you create it. You can reuse the engine to render other workflows with the same template.

## Objects and frames

The engine can render a generated model, a model decorator, an instance of a Java class, or a record. It uses reflection to convert these objects into frames.

You can also pass a frame directly to `render`. [Build frames explicitly](#build-frames-explicitly)

## Factories

| Factory | Input |
| --- | --- |
| `TemplateEngine.with(path)` | A local `.itr` file that the engine opens and closes |
| `TemplateEngine.with(input)` | An `.itr` input stream, read as UTF-8 |
| `TemplateEngine.with(template)` | An existing template |
| `TemplateEngine.with(rules)` | An iterable of rendering rules |
| `TemplateEngine.with(rules, configuration)` | Rules with explicit locale and line endings |
| `TemplateEngine.with(template).using(factory)` | A template with custom frame adaptation |

Unreadable or malformed templates raise `io.quassar.templating.ports.TemplateReadException`.

## Rendering options

### Build a template in Java

```java
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

Template template = new Template()
	.add(new Rule().condition(allTypes("Step")).output(placeholder("identifier")))
	.add(new Rule().condition(allTypes("Action")).output(placeholder("name")));
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

### Input ownership and errors

`TemplateEngine.with(Path.of("workflow.itr"))` opens and closes a local template file for you.

`TemplateEngine.with(input)` reads UTF-8 and leaves the stream open. Close it in the calling code, typically with try-with-resources. The engine retains the resulting template, so it can render after the stream has been closed.

Unreadable or malformed input raises `io.quassar.templating.ports.TemplateReadException`. Rendering does not require generating or compiling a Java template class.

## Advanced frame usage

### Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(workflow);
String source = engine.render(frame);
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
String source = engine.render(step);
```

### Customize rendering

Register formatters and object adapters on the engine.

```java
record ExternalStep(String id, int timeout) {}

TemplateEngine engine = TemplateEngine.with(Path.of("step-config.itr"))
	.add(ExternalStep.class, (source, frame) -> frame
		.add("Step")
		.add("identifier", source.id())
		.add("timeoutSeconds", source.timeout())
		.add("enabled", true));
```

### Frame context

```itr
$<container>name
$<root>name
$<step.action>name
```

`container` addresses the enclosing frame; `root` addresses the root frame. Named path segments traverse slots in order. The final placeholder selects a slot from the reached frame.

# Template Engine API

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert. The engine needs Java 17 or later.

## Add the dependency for standalone use

Add the dependency below **only if your project does not use the Quassar Maven plugin**. With `<extensions>true</extensions>`, the plugin adds `template-engine` to your project automatically. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>template-engine</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Write your own templates

Use the syntax reference to define rules that select model objects and describe the output. The workflow examples show complete templates alongside their rendered output, including Java source files. [Template syntax reference](#template-syntax-reference) · [Workflow template examples](template-engine-examples.md)

## Generated decorators

Pass a model or one of its decorators directly to `render`. Use the model's names in the template. For example, `$step` renders children of type `Step` exposed by the Java getter `steps()`.

Generate the Java model API from M2 before loading M1 and passing a decorator to the engine. [Generated Model API](generated-model-api.md)

## Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(workflow);
String source = engine.render(frame);
```

`Frames.from(...)` adapts records and regular Java objects. It includes inherited fields and converts nested object, list, map and array fields. Static and transient fields are excluded. Each conversion creates its own frame tree and parent links. Passing an existing `Frame` returns that frame.

Use `Frames.builder(...)` when you want to name the type and add slots yourself. Scalar values such as strings and numbers are added directly.

```java
Frame step = Frames.builder("Step")
	.add("identifier", "Confirm")
	.add("timeoutSeconds", 30)
	.add("enabled", true)
	.toFrame();
```

## Customize rendering

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

See the workflow recipes for conditions, lists, and Java generation. [Template examples](template-engine-examples.md)

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

## Executable examples

See the [example tests](../template-engine/src/test/java/tests/DocumentationExamplesTest.java) and their [template resources](../template-engine/src/test/resources/examples).

Distributed under the [MIT license](../LICENSE).

## Template syntax reference

Read a rule from top to bottom. Its condition selects the object and its indented body describes the output.

For complete input and output examples, see [conditions](template-engine-examples.md#combine-conditions), [lists](template-engine-examples.md#render-a-list-and-format-each-value), [JSON](template-engine-examples.md#produce-json-with-optional-fields), and [escaping](template-engine-examples.md#escape-template-characters).

### Rules

```itr
rule type(Step)
	$identifier
```

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current frame. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

### Data and slots

The engine represents input objects as frames. For a plain Java record, the class name is the frame type and each field supplies a named value called a slot. An object-valued slot can invoke another rule, and a list supplies multiple values under the same name.

[Generated decorators](#generated-decorators) supply types and slots from the model through an internal frame view.

```itr
rule type(Step)
	$identifier calls $action

rule type(Action)
	$name
```

Use `Frames.builder(...)` to supply slots explicitly or register a custom adapter with `.add(Class, ObjectMapper)`.

### Conditions

| Condition | Matches |
| --- | --- |
| `type(Step)` | A frame of type `Step` |
| `attribute(schedule)` | A frame with a `schedule` slot |
| `attribute(enabled, true)` | A frame whose `enabled` slot contains `true` |
| `trigger(action)` | Rendering triggered by the `action` placeholder |

Combine conditions with `and`, `or`, `not` and parentheses:

```itr
rule type(Step) and attribute(schedule)
	String schedule = $schedule+JsonString;

rule type(Step) and not attribute(schedule)
	String schedule = "";
```

### Lists and separators

```itr
$step...[$NL$NL]
$fallbackActions...[, ]
$properties...[, ]
```

`...` renders all values of a slot. The text inside `[...]` separates their rendered outputs.

| Mark | Meaning |
| --- | --- |
| `$NL` | Newline |
| `$TAB` | Tab |

### Optional expressions

```itr
<<String schedule = $schedule+JsonString;>>
```

The expression appears when its placeholders produce content. If `schedule` is absent, the declaration disappears with it.

Use `?` to supply an alternative:

```itr
String schedule = <<$schedule+JsonString?"immediate">>;
```

### Formatters

```itr
$identifier+UpperCase
$name+FirstUpperCase
$name+OnlyLetters+UpperCase
```

Formatters transform values before rendering and can be chained. Register custom formatters with `engine.add("Label", formatter)` and use `$identifier+Label`.

Some built-in formatters:

| Kind | Names |
| --- | --- |
| Text | `UpperCase`, `LowerCase`, `FirstUpperCase`, `FirstLowerCase`, `Capitalize` |
| JSON strings | `JsonString` adds quotes and escapes the value |
| Identifiers | `CamelCase`, `PascalCase`, `SnakeCase`, `KebabCase` |
| Filtering | `OnlyDigits`, `OnlyLetters`, `OnlyDigitsAndLetters` |
| Numbers | `Words`, `Separators`, `TwoDecimals` |
| Dates | `Year`, `YearMonth`, `YearMonthDay`, `ShortDate`, `FullDate` |

### Frame context

```itr
$<container>name
$<root>name
$<step.action>name
```

`container` addresses the enclosing frame; `root` addresses the root frame. Named path segments traverse slots in order. The final placeholder selects a slot from the reached frame.

### Join a placeholder to literal text

Use `~` to separate a placeholder name from an adjacent suffix:

```itr
public class $identifier~Workflow {}
```

For `identifier = OrderFulfillment`, this produces `public class OrderFulfillmentWorkflow {}`.

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

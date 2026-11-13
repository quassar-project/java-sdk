# Template Engine API

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert.

From Java, load the template with `TemplateEngine.with(Path)` and pass the data to `render`. For example, to turn an order batch into a shipping summary:

```java
String summary = TemplateEngine.with(Path.of("order-batch.itr")).render(batch());
```

## Add the dependency for standalone use

Use Java 17 or later.

Add the dependency below only if your project does not use the Quassar Maven plugin. With `<extensions>true</extensions>`, the plugin adds `template-engine` to your project automatically. [Quassar Maven plugin setup](../README.md#2-use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>template-engine</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Render an order batch

Save this as `order-batch.itr` in the directory from which you run the example.

```itr
rule type(OrderBatch)
	$name
	$orders...[$NL$NL]

rule type(Order)
	$number+UpperCase ($totalInCents)
	Customer: $customer
	<<Tracking: $trackingCode>>

rule type(Customer)
	$name
```

Save the Java example as `OrderBatchExample.java`. The batch contains two orders so you can see how optional data affects the output.

```java
import io.quassar.templating.TemplateEngine;

import java.nio.file.Path;
import java.util.List;

public class OrderBatchExample {
	public static void main(String[] args) throws Exception {
		System.out.println(TemplateEngine.with(Path.of("order-batch.itr")).render(batch()));
	}

	private static OrderBatch batch() {
		return new OrderBatch("Daily orders", List.of(
			new Order("ORD-1042", 2599, new Customer("Alex Morgan"), "TRK-1042", true),
			new Order("ORD-1043", 1499, new Customer("Sam Rivera"), null, false)));
	}

	public record OrderBatch(String name, List<Order> orders) {
	}

	public record Order(String number, long totalInCents, Customer customer, String trackingCode, boolean shipped) {
	}

	public record Customer(String name) {
	}
}
```

The output shows both orders and the tracking code for the shipped order.

```text
Daily orders
ORD-1042 (2599)
Customer: Alex Morgan
Tracking: TRK-1042

ORD-1043 (1499)
Customer: Sam Rivera
```

- A rule such as `type(Order)` selects the object to render. The first matching rule wins.
- A placeholder such as `$number` reads a named value from the object. When the value is another object, `$customer` renders it with its own rule.
- `$orders...[$NL$NL]` repeats the order rule with a blank line between orders.
- `+UpperCase` formats the number. `<<Tracking: $trackingCode>>` prints the whole line only when the tracking code exists.

## Write your own templates

Browse the [template examples](template-engine-examples.md) for recipes with expected output, or use the [syntax reference](#template-syntax-reference) to look up a construct.

Save each example as a separate `.itr` file and reuse the Java records from the order batch example. Load the file with `TemplateEngine.with(Path.of("example.itr"))` and pass the object indicated in each example to `render`.

### Choose a rule based on a property

Render an `Order` with a different label depending on whether it has shipped. Put the more specific rule first so the general rule can handle the remaining orders.

```itr
rule type(Order) and attribute(shipped, true)
	$number: shipped

rule type(Order)
	$number: pending
```

Rendering the two orders separately produces these lines.

```text
ORD-1042: shipped
ORD-1043: pending
```

### Provide fallback text

Use `?` inside `<<...>>` to supply an alternative when a value is missing. Render the two orders separately to see both cases.

```itr
rule type(Order)
	$number — <<Tracking: $trackingCode?Tracking unavailable>>
```

```text
ORD-1042 — Tracking: TRK-1042
ORD-1043 — Tracking unavailable
```

### Render a list with a separator

Render a `OrderBatch`. Each item in `orders` uses the `Order` rule. The text in brackets goes between items.

```itr
rule type(OrderBatch)
	$orders...[, ]

rule type(Order)
	$number
```

```text
ORD-1042, ORD-1043
```

### Write JSON with optional fields

Render an `Order` as JSON. `JsonString` quotes and escapes strings. Put the comma inside the optional expression so it disappears when the tracking code is missing.

```itr
rule type(Order)
	{"number": $number+JsonString, "totalInCents": $totalInCents<<, "trackingCode": $trackingCode+JsonString>>}
```

Each order produces its own JSON object.

```json
{"number": "ORD-1042", "totalInCents": 2599, "trackingCode": "TRK-1042"}
```

```json
{"number": "ORD-1043", "totalInCents": 1499}
```

## Load a template from the project directory

Put `order-batch.itr` in `src/main/resources/templates` and run the example from the project root.

```java
var engine = TemplateEngine.with(Path.of("src/main/resources/templates/order-batch.itr"));
String summary = engine.render(batch);
```

The engine reads and closes the file when you create it. Reuse the engine to render several objects.

```java
String first = engine.render(firstBatch);
String second = engine.render(secondBatch);
```

## Generated decorators

Pass a model or one of its decorators directly to `render`. Use the model's names in the template. For example, `$task` renders children of type `Task` even when the Java getter is `tasks()`.

Generate the Java model API from M2 before loading M1 and passing a decorator to the engine. [Generated Model API](generated-model-api.md)

## Create a frame from an object

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame frame = Frames.from(batch);
String summary = engine.render(frame);
```

`Frames.from(...)` adapts records and regular Java objects. It includes inherited fields and converts nested object, list, map and array fields. Static and transient fields are excluded. Each conversion creates its own frame tree and parent links. Passing an existing `Frame` returns that frame.

Use `Frames.builder(...)` when you want to name the type and add slots yourself. Scalar values such as strings and numbers are added directly.

```java
Frame order = Frames.builder("Order")
	.add("number", "ORD-1042")
	.add("totalInCents", 2599)
	.toFrame();
```

## Customize rendering

Register formatters and object adapters fluently:

```java
TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]")
	.add(ExternalOrder.class, (source, frame) -> frame
		.add("Order")
		.add("number", source.label()));
```

See [examples](template-engine-examples.md) for matching templates, conditions, recursive tasks, explicit frames and Java generation.

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
rule type(Order)
	$number
```

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current frame. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

### Data and slots

The engine represents input objects as frames. For a plain Java record, the class name is the frame type and each field supplies a named value called a slot. An object-valued slot can invoke another rule, and a list supplies multiple values under the same name.

[Generated decorators](#generated-decorators) supply types and slots from the model through an internal frame view.

```itr
rule type(Order)
	$number for $customer

rule type(Customer)
	$name
```

Use `Frames.builder(...)` to supply slots explicitly or register a custom adapter with `.add(Class, ObjectMapper)`.

### Conditions

| Condition | Matches |
| --- | --- |
| `type(Order)` | A frame of type `Order` |
| `attribute(trackingCode)` | A frame with an `trackingCode` slot |
| `attribute(shipped, true)` | A frame whose `shipped` slot contains `true` |
| `trigger(number)` | Rendering triggered by the `number` placeholder |

Combine conditions with `and`, `or`, `not` and parentheses:

```itr
rule type(Order) and attribute(trackingCode)
	$number — Tracking $trackingCode

rule type(Order) and not attribute(trackingCode)
	$number
```

### Lists and separators

```itr
$orders...[$NL$NL]
$tags...[, ]
$properties...[, ]
```

`...` renders all values of a slot. The text inside `[...]` separates their rendered outputs.

| Mark | Meaning |
| --- | --- |
| `$NL` | Newline |
| `$TAB` | Tab |

### Optional expressions

```itr
<<Tracking: $trackingCode>>
```

The expression appears when its placeholders produce content. If `trackingCode` is absent, the label disappears with it.

Use `?` to supply an alternative:

```itr
<<Tracking: $trackingCode?Tracking unavailable>>
```

### Formatters

```itr
$number+UpperCase
$name+FirstUpperCase
$name+OnlyLetters+UpperCase
```

Formatters transform values before rendering and can be chained. Register custom formatters with `engine.add("Label", formatter)` and use `$number+Label`.

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
$<order.customer>name
```

`container` addresses the enclosing frame; `root` addresses the root frame. Named path segments traverse slots in order. The final placeholder selects a slot from the reached frame.

### Join a placeholder to literal text

Use `~` to separate a placeholder name from an adjacent suffix:

```itr
public class $name~Service {}
```

For `name = OrderBatch`, this produces `public class OrderBatchService {}`.

### Build a template in Java

```java
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

Template template = new Template()
	.add(new Rule().condition(allTypes("Order")).output(placeholder("number")))
	.add(new Rule().condition(allTypes("Customer")).output(placeholder("name")));
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

`TemplateEngine.with(Path.of("order-batch.itr"))` opens and closes a local template file for you.

`TemplateEngine.with(input)` reads UTF-8 and leaves the stream open. Close it in the calling code, typically with try-with-resources. The engine retains the resulting template, so it can render after the stream has been closed.

Unreadable or malformed input raises `io.quassar.templating.ports.TemplateReadException`. Rendering does not require generating or compiling a Java template class.

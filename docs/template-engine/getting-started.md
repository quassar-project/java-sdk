# Template Engine

Template Engine turns Java objects and Quassar models into text, JSON, or source code. An `.itr` file defines what to output for each type of object and which values to insert.

From Java, load the template with `TemplateEngine.with(Path)` and pass the data to `render`. For example, to turn an order batch into a shipping summary:

```java
String summary = TemplateEngine.with(Path.of("order-batch.itr")).render(batch());
```

## Add the dependency for standalone use

Use Java 17 or later.

Add the dependency below only if your project does not use the Quassar Maven plugin. With `<extensions>true</extensions>`, the plugin adds `template-engine` to your project automatically. [Quassar Maven plugin setup](../../README.md#2-use-your-models-from-java)

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

Browse the [template examples](examples.md) for recipes with expected output, or use the [syntax reference](templates.md) to look up a construct.

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

Generate the Java model API from M2 before loading M1 and passing a decorator to the engine. [Generated Model API](../maven-plugin/generated-model-api.md)

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

See [examples](examples.md) for matching templates, conditions, recursive tasks, explicit frames and Java generation.

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

See the [example tests](../../template-engine/src/test/java/tests/DocumentationExamplesTest.java) and their [template resources](../../template-engine/src/test/resources/examples).

Distributed under the [MIT license](../../LICENSE).

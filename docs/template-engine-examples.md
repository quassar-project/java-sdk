# Template examples

[Template Engine API](template-engine-api.md) · [Template reference](template-engine-api.md#template-syntax-reference)

Reuse the `Order`, `Customer` and `OrderBatch` records from the [order batch example](template-engine-api.md#render-an-order-batch). Save a snippet as an `.itr` file, load it with `TemplateEngine.with(Path.of("example.itr"))`, and pass the object named in the rule to `render`.

- **Rules** · [property conditions](#choose-a-rule-from-the-data) · [boolean conditions](#combine-conditions) · [the calling slot](#choose-output-based-on-the-calling-slot)
- **Output** · [optional values](#optional-values-and-fallback-text) · [lists and separators](#render-a-list-and-format-each-value) · [JSON](#produce-json-with-optional-fields) · [literal text](#escape-template-characters) · [Java source](#generate-a-java-record)
- **Navigation** · [root and named paths](#read-values-from-the-root-and-a-named-path) · [containers](#build-frames-explicitly-and-access-their-container) · [recursion](#render-a-recursive-table-of-tasks)
- **Java API** · [custom formatters](#use-a-custom-formatter) · [object adapters](#adapt-an-existing-object) · [frames](#create-frames-from-the-model) · [rules in Java](#build-a-template-in-java) · [verification](#verify-a-template-against-sample-data)

## Choose a rule from the data

```itr
rule type(Order) and attribute(shipped, true)
	$number: shipped

rule type(Order)
	$number: pending
```

The specific rule comes first. The second rule handles all other orders.

```java
String shipped = engine.render(new Order("ORD-1042", 2599, customer, trackingCode, true));
String pending = engine.render(new Order("ORD-1042", 2599, customer, trackingCode, false));
```

Output:

```text
ORD-1042: shipped
ORD-1042: pending
```

## Combine conditions

Render the two orders separately. An order gets a short label when it has a tracking code and is shipped, or when its total is 1499 cents.

```itr
rule type(Order) and ((attribute(trackingCode) and not attribute(shipped, false)) or attribute(totalInCents, 1499))
	$number: featured

rule type(Order)
	$number: standard
```

```text
ORD-1042: featured
ORD-1043: featured
```

## Choose output based on the calling slot

Render the first order. The customer rule checks that it was reached through `$customer`.

```itr
rule type(Order)
	$number for $customer

rule type(Customer) and trigger(customer)
	$name+UpperCase

rule type(Customer)
	$name
```

```text
ORD-1042 for ALEX MORGAN
```

## Optional values and fallback text

```itr
rule type(Order)
	$number: <<Tracking $trackingCode?Tracking unavailable>>
```

With a tracking code:

```text
ORD-1042: Tracking TRK-1042
```

Without a tracking code:

```text
ORD-1043: Tracking unavailable
```

Use `<<Tracking: $trackingCode>>` to omit the whole expression when the slot has no value. A record field set to `null` does not create a slot.

## Render a list and format each value

Render a frame built with `Frames.builder("Order").add("tags", new String[]{"express shipping", "fragile"}).toFrame()`. The `tags` rule uses `$this` to format the current list item. The text between brackets after `...` separates items.

```itr
rule type(Order)
	Tags: $tags...[, ]

rule trigger(tags)
	[$this+OnlyLetters+UpperCase]
```

```text
Tags: [EXPRESSSHIPPING], [FRAGILE]
```

## Produce JSON with optional fields

Render the order batch. `JsonString` quotes and escapes text. The optional expression contains the comma so a missing tracking code leaves valid JSON.

```itr
rule type(OrderBatch)
	{"orders": [$orders...[, ]]}

rule type(Order)
	{"number": $number+JsonString, "totalInCents": $totalInCents<<, "trackingCode": $trackingCode+JsonString>>}
```

```json
{"orders": [{"number": "ORD-1042", "totalInCents": 2599, "trackingCode": "TRK-1042"}, {"number": "ORD-1043", "totalInCents": 1499}]}
```

## Escape template characters

Render the first order. Use `~` to end a placeholder before adjacent text, `$$` for a literal dollar sign, and `$<<` and `$>>` for literal expression delimiters.

```itr
rule type(Order)
	$totalInCents~c $$5 $<<preview$>>
```

```text
2599c $5 <<preview>>
```

## Read values from the root and a named path

Render the order batch. A named path such as `orders.customer` follows the first value at each step. Inside the order rule, `root` takes you back to the batch.

```itr
rule type(OrderBatch)
	First customer: $<orders.customer>name
	$orders...[$NL]

rule type(Order)
	$number — $<root>name
```

```text
First customer: Alex Morgan
ORD-1042 — Daily orders
ORD-1043 — Daily orders
```

## Render a recursive task tree

```itr
rule type(Task)
	$number
		<<$children...[$NL]>>
```

```java
public record Task(String number, List<Task> children) {
	public Task {
		children = List.copyOf(children);
	}
}

Task tasks = new Task("Fulfil order", List.of(
	new Task("Pack items", List.of()),
	new Task("Ship order", List.of(new Task("Confirm delivery", List.of())))));

String result = engine.render(tasks);
```

Output:

```text
Fulfil order
	Pack items
	Ship order
		Confirm delivery
```

Every child invokes the same rule. The template's extra tab indents the rendered children.

## Use a custom formatter

Save this as `label.itr`.

```itr
rule type(Order)
	$number+Label
```

```java
TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]");

String result = engine.render(order);
```

Output:

```text
[ORD-1042]
```

## Adapt an existing object

Use the same `label.itr` template with data that has different field names:

```java
public record ExternalOrder(String label) {
}

TemplateEngine engine = TemplateEngine.with(Path.of("label.itr"))
	.add("Label", value -> "[" + value + "]")
	.add(ExternalOrder.class, (source, frame) -> frame
		.add("Order")
		.add("number", source.label()));

String result = engine.render(new ExternalOrder("ORD-1043"));
```

Output:

```text
[ORD-1043]
```

The adapter supplies the `Order` type and the `number` slot expected by the template.

## Create frames from the model

```java
import io.quassar.templating.Frames;
import io.quassar.templating.model.Frame;

Frame batchFrame = Frames.from(batch);
Frame orderFrame = batchFrame.frames("orders").next();
Object number = orderFrame.frames("number").next().value();
String result = engine.render(batchFrame);
```

The frame keeps the object's type and slots. The order's `container()` is `batchFrame`.

## Build frames explicitly and access their container

```itr
rule type(OrderBatch)
	$orders...[$NL]

rule type(Order)
	$number ($<container>name)
```

```java
import io.quassar.templating.Frames;

var order = Frames.builder("Order").add("number", "ORD-1042").toFrame();
var batch = Frames.builder("OrderBatch")
	.add("name", "Daily orders")
	.add("orders", order)
	.toFrame();

String result = engine.render(batch);
```

Output:

```text
ORD-1042 (Daily orders)
```

`$<container>name` reads the batch's name while rendering its order.

## Generate a Java record

An event schema can define the Java payload that producers and consumers share. This example takes the name and fields of an order event from the schema and generates a record for it.

Save the template as `record.itr`.

```itr
rule type(Entity)
	public record $name($properties...[, ]) {}

rule type(Property)
	$type $name
```

```java
public record Property(String type, String name) {
}

public record Entity(String name, List<Property> properties) {
	public Entity {
		properties = List.copyOf(properties);
	}
}

Entity event = new Entity("OrderPlaced", List.of(
	new Property("String", "orderId"),
	new Property("long", "totalInCents")));

String source = TemplateEngine.with(Path.of("record.itr")).render(event);
Files.writeString(Path.of(event.name() + ".java"), source);
```

Generated code:

```java
public record OrderPlaced(String orderId, long totalInCents) {}
```

The producer can create an `OrderPlaced` payload, and the consumer can read its fields through the generated accessors.

```java
var event = new OrderPlaced("ORD-1042", 2599);
long total = event.totalInCents();
```

## Build a template in Java

Use the same rule model as an `.itr` template when you need to assemble rules in code.

```java
import io.quassar.templating.TemplateEngine;
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

var template = new Template()
	.add(new Rule().condition(allTypes("Order")).output(placeholder("number")));

String result = TemplateEngine.with(template).render(order);
```

For the first order in the batch, `result` is `ORD-1042`. To set a specific locale or line separator, see the [configuration example](template-engine-api.md#rendering-configuration).

## Verify a template against sample data

Use a representative input model to check that a template has matching rules, valid references, and a traversal that terminates.

```java
import io.quassar.templating.Frames;
import io.quassar.templating.TemplateVerifier;
import io.quassar.templating.adapters.antlr.TemplateReader;
import java.nio.file.Path;

var template = TemplateReader.read(Path.of("order-batch.itr"));
var report = TemplateVerifier.of(template).verify(Frames.from(batch));
System.out.println(report.isOk());
```

## Executable versions

The [example resources](../template-engine/src/test/resources/examples) and [DocumentationExamplesTest](../template-engine/src/test/java/tests/DocumentationExamplesTest.java) exercise the public API with order batches, recursive tasks, custom formatters, and Java generation.

Run these examples from the repository root:

```sh
mvn -f template-engine/pom.xml -Dtest=DocumentationExamplesTest test
```

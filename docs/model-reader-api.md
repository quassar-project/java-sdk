# Model Reader API

Load models written in your Tara DSL and query them from Java. The reader needs Java 17 or later.

## 1. Add the dependency for standalone use

Add the dependency below **only if your project does not use the Quassar Maven plugin**. With `<extensions>true</extensions>`, the plugin adds `model-reader` to your project automatically. [Quassar Maven plugin setup](../README.md#2-use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>model-reader</artifactId>
	<version>1.0.0</version>
</dependency>
```

## 2. Create a model in Quassar

Create an M1 model in Quassar Studio using the Workflows DSL. Copy its download URL for the Java example. [Open Quassar Studio](https://quassar.io/studio) · [Workflow metamodel](generated-model-api.md#workflow-metamodel)

```tara
Action(name = "Send confirmation") SendConfirmation
Action(name = "Queue for retry") QueueForRetry
Action(name = "Log failure") LogFailure

Workflow OrderFulfillment
	EntryPoint(name = "Order placed") OrderPlaced
	Step(label = "Confirm order", timeoutSeconds = 30, enabled = true, action = SendConfirmation) Confirm
		fallbackActions = QueueForRetry LogFailure
```

Each object is a *mogram*. `Workflow` is a type and `OrderFulfillment` is its identifier. Indentation makes `Confirm` a child of the workflow. The value `SendConfirmation` refers to an action by identifier.

## 3. Load the model and follow a reference

```java
import io.quassar.reader.Models;
import java.net.URI;

public class WorkflowExample {
	public static void main(String[] args) throws Exception {
		var model = Models.load(URI.create("https://quassar.io/models/[code]"));
		var workflow = model.get("OrderFulfillment");
		var step = model.get("OrderFulfillment.Confirm");
		var action = model.get(step.property("action"), step);

		System.out.println(workflow.identifier());
		System.out.println(action.property("name").asString());
	}
}
```

This prints `OrderFulfillment` and `Send confirmation`. Passing `step` as context lets the reader resolve references relative to the step before looking from the root. Use `model.get(value)` when the reference is already rooted.

## 4. Find and navigate mograms

Use an identifier path when you know the exact object. Use a query when you want to filter or navigate.

```java
var step = model.get("OrderFulfillment.Confirm");
var parent = model.parentOf(step);

var workflows = model.roots().type("Workflow").list();
var steps = model.select(workflow).children().type("Step").list();
var allSteps = model.select(workflow).descendants().type("Step").list();
```

`model.roots()` starts at the top level, `model.select()` starts with every mogram, and `model.select(workflow)` starts with one mogram. Add filters such as `type("Step")`, `facet("Scheduled")`, or `withProperty("action")`. Finish with `list()`, `stream()`, `first()`, `count()`, or `isEmpty()`. Queries return new selections and their result lists are immutable.

## Read values and handle missing data

Use a typed accessor for the property you expect.

```java
import io.quassar.reader.model.Value;

var timeout = step.property("timeoutSeconds").asNumber();
var enabled = step.property("enabled").asBoolean();
var fallbackPaths = step.property("fallbackActions").asSequence().stream()
	.map(Value::asReference)
	.toList();

if (step.property("schedule").isEmpty()) {
	System.out.println("Schedule unavailable");
}
```

| Value | Check | Read |
| --- | --- | --- |
| Text | `isString()` | `asString()` |
| Number | `isNumber()` | `asNumber()` returns a `BigDecimal` |
| Boolean | `isBoolean()` | `asBoolean()` |
| Reference | `isReference()` | `asReference()` returns the path |
| Sequence | `isSequence()` | `asSequence()` returns an immutable list |

A missing property returns `Value.Empty`. Typed accessors return an empty string, zero, `false`, or an empty list when the value has a different type. Use the checks when you need to distinguish a missing value from a real zero or empty string.

A missing mogram, an unresolved reference, the parent of a root mogram, and `first()` on an empty query return `Mogram.NULL`. Check the result with `isNull()`.

## Load from another source

```java
import java.nio.file.Path;

var archive = Models.load(Path.of("workflows.zip"));
var folder = Models.load(Path.of("m1"));
var bundled = Models.load(WorkflowExample.class.getResource("/workflows.tara"));
```

A folder or ZIP can contain multiple Tara files and subfolders. The reader loads them into one model so references can cross file boundaries. For typed methods such as `step.label()`, see the [Generated Model API](generated-model-api.md).

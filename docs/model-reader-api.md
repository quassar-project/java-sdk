# Model Reader API

Load models written in your Tara DSL and query them from Java. The reader needs Java 17 or later.

## Add the dependency for standalone use

Add the dependency below **only if your project does not use the Quassar Maven plugin**. With `<extensions>true</extensions>`, the plugin adds `model-reader` to your project automatically. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

```xml
<dependency>
	<groupId>quassar.io</groupId>
	<artifactId>model-reader</artifactId>
	<version>1.0.0</version>
</dependency>
```

## Create a model in Quassar

Create the [sample model (M1)](sample-m1-model.md) and use its download URL in the Java examples.

## Load the model and follow a reference

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

## Find and navigate mograms

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

## Load from files, URLs, or text

Use `Models.create` for model text and `Models.load` for local files, folders, ZIP archives, and URLs.

```java
import java.nio.file.Path;

var fromText = Models.create("Workflow OrderFulfillment");
var fromFile = Models.load(Path.of("workflows.tara"));
var fromFolder = Models.load(Path.of("m1"));
var fromZip = Models.load(Path.of("workflows.zip"));
var fromUrl = Models.load(URI.create("https://quassar.io/models/[code]"));
var bundled = Models.load(WorkflowExample.class.getResource("/workflows.tara"));
```

Replace `[code]` with your M1 model code. A folder or ZIP can contain several Tara files. The reader combines them so references can cross file boundaries. For typed accessors such as `step.label()`, see the [Generated Model API](generated-model-api.md).

## Load a release

Use `ModelRelease` when you also need metadata, resources, or authenticated downloads.

```java
var release = io.quassar.reader.ModelRelease.open(Path.of("release.zip"));
var model = release.model();
var metadata = release.metadata();
try (var input = release.resource("icons/logo.svg")) {
	var bytes = input.readAllBytes();
}
var downloaded = io.quassar.reader.ModelRelease.download(url, token);
```

Releases are read in memory without extracting files. Model files under `views/` are combined; files under `resources/` remain resources even when their extension is `.tara`. Authenticated downloads do not follow redirects.

## Resolve defaults from M2

Pass the metamodel to resolve defaults alongside the values supplied in M1.

```java
var effective = Models.create(modelText, metamodelText);
var fromFile = Models.load(Path.of("main.tara"), metamodelText);
var resolver = new io.quassar.reader.domain.EffectiveValueResolver(metamodelText);
var another = resolver.resolve(Models.create(otherModelText));
var release = io.quassar.reader.ModelRelease.open(Path.of("release.zip")).withDefaults(resolver);
```

Explicit values take precedence over defaults, including `false`, `0`, `""` and `empty`. Explicit lists replace default lists. The closest ancestor supplies inherited defaults; a child property declaration replaces the inherited definition. Applied facets contribute defaults as well. Each property exposes `origin()` (`EXPLICIT` or `DEFAULT`). Resolution returns a new model and leaves the input untouched, so retain the original model when saving only explicit values.

The resolver does not execute semantic constraints or validate cardinalities. Compilation and validation remain backend responsibilities.

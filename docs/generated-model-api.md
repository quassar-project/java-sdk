# Generated Model API

Use the generated Java decorators to load an M1 model, read its properties, and navigate its children and references. The API follows the concepts and relationships defined in M2. [Maven Plugin setup](../README.md#use-your-models-from-java)

## Generate from Java

Load an M2 metamodel from `https://quassar.io/models/[code]`. Use the code of your metamodel in Quassar for `[code]`.

```java
import io.quassar.javacode.MogramDecoratorGenerator;
import io.quassar.metamodel.Metamodels;

import java.net.URI;
import java.nio.file.Path;

var metamodel = Metamodels.load(URI.create("https://quassar.io/models/[code]"));
MogramDecoratorGenerator.from(metamodel)
	.inPackage("example.model")
	.writeTo(Path.of("src/main/gen"));
```

The generator uses the `name` from the root `manifest.json` with a `Model` suffix. A manifest named `Workflows` generates `WorkflowsModel`, as in the [interpreter example](../README.md#interpreter). Use `.modelNamed(...)` to override the name.

Use `.code()` to obtain the generated `JavaCode` records without writing files. Java keywords and name collisions receive safe generated names; lookups retain the original names from the metamodel.

## Workflow metamodel

The `Workflows` DSL defines workflows with an entry point and a sequence of steps. Each step references an action and can specify fallback actions.

```tara
dsl metta

Concept Workflow
	Concept:Single:Required EntryPoint
		def string name
	Concept:{1..*} Step
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

## Property types

| Metamodel declaration | Java getter |
| --- | --- |
| `def string label` | `String` |
| `def integer timeoutSeconds` | `BigDecimal` |
| `def boolean enabled` | `boolean` |
| `def Action action` | `Action` decorator |
| `def Action[] fallbackActions` | `List<Action>` |

A reference without a declared target uses `MogramDecorator`. References resolve lazily against the loaded model, using their source mogram as context. Missing targets return null decorators with `isNull()` and empty property defaults. Relative references and cycles are supported.

A multiple property returns an empty list for an absent value, a singleton list for one value, and a list of all elements for a sequence.

## Use the generated model

A root or child with a maximum cardinality of one has a singular getter that returns its decorator. For example, `Concept:Single:Required EntryPoint` generates `workflow.entryPoint()`. If the root were declared as `Concept:{1..1} Workflow`, the model would expose `model.workflow()`. If a single mogram is absent, the getter returns a decorator with `isNull()` set to `true`. Repeated children return lists.

The model facade exposes only root mograms. Concepts declared as components and concepts declared inside another concept are reached through their parent decorators.

Replace the example URL with the M1 download URL from Quassar.

```java
import example.model.WorkflowsModel;
import java.net.URI;

var model = WorkflowsModel.load(URI.create("https://quassar.io/models/[code]"));
var workflow = model.workflows().get(0);
var entryPoint = workflow.entryPoint();
var step = workflow.steps().get(0);
String label = step.label();
var action = step.action();
var fallbackActions = step.fallbackActions();
```

`WorkflowsModel.load(...)` accepts `Path`, `URL` and `URI`, and declares `IOException`. It loads model instances using `model-reader`. The M2 metamodel supplies the declarations used to generate the Java API.

Model collection methods return root mograms of the declared type. Child collection methods return direct children of the declared type. References resolve against the loaded model.

For a step with the `Scheduled` facet, `step.isScheduled()` checks whether it is active and `step.asScheduled()` returns its typed view. Read the schedule with `step.asScheduled().schedule()`. When the facet is absent, the view returns `true` from `isNull()`.

## Read a local M2 metamodel

```java
import io.quassar.metamodel.Metamodels;
import io.quassar.metamodel.model.Metamodel;

import java.nio.file.Path;

Metamodel metamodel = Metamodels.load(Path.of("workflows-metamodel.zip"));
Metamodel folder = Metamodels.load(Path.of("m2"));
```

Both sources can contain declarations in several `.tara` files, including files in subdirectories. To read M1 from a local folder, use `WorkflowsModel.load(Path.of("m1"))`.

`metamodel.types()` contains the immutable declarations. Names include their declaration scope, such as `Workflow.Step` and `Workflow.Step.Execution.Scheduled`. Each type retains its own properties, facets, compositions, cardinality and modifiers. `scope()` identifies its enclosing declaration; `parent()` records inheritance through `sub` or `extends`. Facet alternatives are also facet types. Repeated declarations in the same scope are combined.

Properties retain their declared type, sequence element type, reference target spelling, constraints and default value. Defaults retain their source expression, including quotes, units or `empty`. Inherited properties and reference targets are not resolved by this reader.

## Build and test

From the repository root:

```sh
mvn -pl maven-plugin -am test
```

Tests use JUnit 5 and AssertJ with M2 metamodel resources and Java fixtures. They compile and execute generated decorators against model instances, covering facets, references, sequences, positional properties, null objects, model loading and Java name collisions.

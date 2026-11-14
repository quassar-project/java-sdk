# Generated Model API

Use the generated Java decorators to load an M1 model, read its properties, and navigate its children and references. The API follows the concepts and relationships defined in M2. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Generation inputs and output

Set `input-model` to the M2 download URL. During `generate-sources`, the Quassar Maven plugin generates the model API from M2 and writes Java files under `output-directory` (`src/main/gen` by default).

The plugin adds the `model` subpackage to the base package set by `output-package`, which defaults to the project's `groupId`. With the default package and a project `groupId` of `org.example` and the `Workflows` metamodel, the model class is `org.example.model.WorkflowsModel` and its default output path is `src/main/gen/org/example/model/WorkflowsModel.java`. Use it to load M1 when your generator or interpreter runs.

The examples below use the [sample Workflows metamodel](sample-m2-model.md).

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
import org.example.model.WorkflowsModel;
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

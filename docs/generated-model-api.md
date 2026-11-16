# Generated Model API

Use the generated Java decorators to load an M1 model, read its properties, and navigate its children and references. The API follows the concepts and relationships defined in M2. [Quassar Maven plugin setup](../README.md#use-your-models-from-java)

## Generation inputs and output

Set `input-model` to the M2 download URL. During `generate-sources`, the Quassar Maven plugin generates the model API from M2 and writes Java files under `output-directory` (`src/main/gen` by default).

The plugin adds the `model` subpackage to the base package set by `output-package`, which defaults to the project's `groupId`. With the default package and a project `groupId` of `org.workflows` and the `Workflows` metamodel, the model class is `org.workflows.model.WorkflowsModel` and its default output path is `src/main/gen/org/workflows/model/WorkflowsModel.java`. Use it to load M1 when your generator or interpreter runs. The facade uses the last segment of a qualified metamodel name. For example, `MONET.VISORA` generates `VisoraModel`.

The examples below use the [sample Workflows metamodel](sample-m2-model.md).

Concepts, facets, and subtypes declared inside another type generate static classes inside the enclosing decorator. For example, the `EntryPoint` declared inside `Workflow` generates `Workflow.EntryPoint`. The `workflow.entryPoint()` getter returns that type.

If you generated the API with an earlier SDK, delete the previously generated `model` package from the output directory before regenerating it.

## Property types

| Metamodel declaration | Java getter |
| --- | --- |
| `def string label` | `String` |
| `def integer timeoutSeconds` | `int` |
| `def integer[] retryDelays` | `List<Integer>` |
| `def boolean enabled` | `boolean` |
| `def Action action` | `Action` decorator |
| `def Action[] fallbackActions` | `List<Action>` |

A property with a closed set of words generates a nested Java enum. For example, `def word:{Stack Float} positioning = Stack` generates `Positioning` with `Stack` and `Float`. The getter returns `Positioning`, or a list of enum values for a multiple property. An absent scalar uses its declared default when present and returns `null` otherwise.

A reference without a declared target uses `io.quassar.reader.model.Node`, the base class supplied by Model Reader. References resolve lazily against the loaded model, using their source mogram as context. Missing targets return null decorators with `isNull()` and empty property defaults. Relative references and cycles are supported.

A multiple property returns an empty list for an absent value, a singleton list for one value, and a list of all elements for a sequence.

## Use the generated model

A root or child with a maximum cardinality of one has a singular getter that returns its decorator. For example, `Concept:Single:Required EntryPoint` generates `workflow.entryPoint()`. If the root were declared as `Concept:{1..1} Workflow`, the model would expose `model.workflow()`. If a single mogram is absent, the getter returns a decorator with `isNull()` set to `true`. Use `isNotNull()` to check whether a decorator represents an existing mogram. Repeated children return lists.

The model facade exposes only root mograms. Concepts declared as components and concepts declared inside another concept are reached through their parent decorators.

Replace the example URL with the M1 download URL from Quassar.

```java
import org.workflows.model.WorkflowsModel;
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

When the M2 manifest supplies a name and version, the generated API stores its expected DSL coordinate. For example, `MONET.VISORA` version `1.8.2` produces `monet:visora:1.8.2`. The `load(...)` methods compare it with the `dsl` field in the M1 `manifest.json` or with the `dsl` header for a standalone Tara file. A missing or different coordinate raises `IOException`. To enable this check when generating from a local M2, include its manifest with the DSL name and version.

Model collection methods return root mograms of the declared type. Child collection methods return direct children of the declared type. References resolve against the loaded model.

For a step with the `Scheduled` facet, `step.isScheduled()` checks whether it is active and `step.asScheduled()` returns its typed view. Read the schedule with `step.asScheduled().schedule()`. When the facet is absent, the view returns `true` from `isNull()`.

A decorator for a type declared with `sub` extends the parent decorator and inherits its accessors. These subtypes generate `is<Type>()` and `as<Type>()` on ancestor decorators, including ancestors several levels up. If `Download` is a subtype of `CollectionCommand` under `Command`, `command.asDownload()` returns a `Command.CollectionCommand.Download` decorator.

The same methods are available on facet decorators. If `Container` specializes `Projection` under `ViewType`, `view.asContainer()` returns the typed decorator directly. You can also use `view.asViewType().asContainer()`. If two facets declare a subtype with the same accessor name, select the facet first. When the mogram has a different type or facet, the result returns `true` from `isNull()`.

Call `node.types()` to get the concrete concept and its ancestors, followed by each active facet and its ancestors. Names are qualified as in M2, duplicates are removed, and a null decorator returns an empty list. For example, a grouping command returns `Command.CollectionCommand.Grouping`, `Command.CollectionCommand`, and `Command`, including when accessed through the base command decorator. The list also includes any active facet and its ancestors on that same command. Template type conditions can match these ancestors by their qualified or simple names.

Use `node.as(Workflow.class)` to view the current node as the requested type. Use `node.ancestor(Workflow.class)` to find the nearest ancestor of the requested type. The search starts at the parent and follows containment. A concept ancestor matches its own type, any of its supertypes, and its active facets and their supertypes. If no such ancestor exists, the method returns a null decorator of the requested type.


Use `model.resources()` to list the bundled resources or `model.resource("icons/logo.svg")` to get one by name. Each `Resource` has a `name()` relative to `resources/` and a `bytes()` method that returns a copy of its contents. A missing file raises `FileNotFoundException`.

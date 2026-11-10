# Package responsibilities

[Usage guide](../README.md) · [Examples](examples.md) · [Template reference](templates.md)

```text
io.quassar.templating
├── TemplateEngine, TemplateVerifier, Frames
├── annotations
│   └── FrameType, FrameSlot, FrameSlots, FrameIdentity, FrameParent
├── domain
│   ├── TemplateRenderer
│   ├── formatters
│   └── verification
│       ├── CompletenessVerifier
│       ├── DeterminacyVerifier
│       ├── ReferenceConsistencyVerifier
│       └── TerminationVerifier
├── model
│   ├── Template, Rule, Trigger, Frame
│   ├── frames
│   ├── predicates
│   ├── outputs
│   └── verification
│       ├── Issue
│       └── VerificationReport
├── ports
│   ├── TemplateLoader, ObjectMapper, FrameFactory, FrameBuilder
│   ├── Formatter
│   └── TemplateReadException
└── adapters
    ├── reflection
    │   ├── ReflectiveFrameBuilder
    │   ├── ReflectionMapper
    │   ├── ExcludingReflectionMapper
    │   ├── ObjectFrameFactory
    │   ├── MethodFrames (internal)
    │   └── FrameDefinition (internal)
    └── antlr
        ├── TemplateReader
        ├── TemplateModelBuilder (internal)
        ├── TemplateErrorStrategy (internal)
        └── generated
```

## Direction of dependencies

`model` defines the data and its intrinsic operations. It does not import `domain`, `adapters`, application facades, ANTLR or external libraries.

`domain` implements rendering, formatting and verification using model types and extension ports. It does not import adapters or application facades.

`ports` defines extension contracts. `adapters.reflection` converts objects into frames. `adapters.antlr` implements template loading with ANTLR. The root facades assemble these implementations and expose factory methods.

## Adapter contracts

`TemplateReader` implements `TemplateLoader`; `ObjectFrameFactory` implements `FrameFactory`; `ReflectionMapper` implements `ObjectMapper`; and `ReflectiveFrameBuilder` implements `FrameBuilder`. `ExcludingReflectionMapper` specializes the reflection mapper.

An adapter may contain private implementation helpers. Those helpers do not need their own ports: `TemplateModelBuilder` and `TemplateErrorStrategy` are package-private ANTLR details, and `ArrayValues` is an internal reflection helper. Ports describe capabilities used by the application or domain, rather than every implementation class.

The engine executes `.itr` templates directly. It does not serialize template definitions into Java classes.

## Verification

`TemplateVerifier` is a root application facade. Algorithms belong to `domain.verification`; issues and immutable reports belong to `model.verification`. `Template` no longer instantiates verification services.

```java
import io.quassar.templating.Frames;
import io.quassar.templating.adapters.antlr.TemplateReader;
import io.quassar.templating.TemplateVerifier;
import io.quassar.templating.model.verification.VerificationReport;

try (InputStream input = Files.newInputStream(Path.of("catalogue.itr"))) {
	var template = new TemplateReader(input).read();
	VerificationReport report = TemplateVerifier.of(template).verify(Frames.from(library));
	boolean valid = report.isOk();
}
```

A report's status is derived from its issues. Cycle detection uses frame identity instead of recursive frame hashes. A cyclic graph produces a termination issue before the verifier runs recursive slot checks. Completeness and reference consistency share the `FrameVerifier` traversal and configure different formatter policies.

## Reading a template

```text
TemplateEngine.with(input)
  → adapters.antlr.TemplateReader
      → adapters.antlr.generated.ItrLexer / ItrParser
      → adapters.antlr.TemplateModelBuilder
  → model.Template
  → domain.TemplateRenderer
```

ANTLR generates the lexer/parser from the `.g4` files in `src/main/resources/grammar`. `TemplateReader` runs the generated lexer/parser, and `TemplateModelBuilder` converts the syntax tree into model types.

## Object adaptation

`Frames.from(object)` and the default engine use `ObjectFrameFactory`. Object-field reflection is implemented by `ReflectionMapper`; explicit slots are assembled by `ReflectiveFrameBuilder`. `CompositeFrame` and `PrimitiveFrame` are model data types, separate from object adaptation.

The frame interfaces retain parent links; each ordinary object conversion creates its own tree. Frame graph traversal and verification do not depend on the reflection builder's concrete classes.

## Decorator reflection

The default ObjectFrameFactory adapts annotated methods through an identity graph in MethodFrames. Metadata is immutable and cached by Java class; source objects and frames are local to each conversion. Slots and structural parents are read lazily so references can share identity or form cycles. Registered object mappers remain the first choice.

Generated decorators implement `FrameView`. The engine resolves the declared slot getters by reflection and uses the same identity graph for annotated objects and views. The generated contract supplies the structural parent and model types. The translator converts reader values to Java scalars and decorated references and exposes model roots through the facade getters.

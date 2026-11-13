# Template reference

[Template Engine API](template-engine-api.md) · [Examples](template-engine-examples.md)

Read a rule from top to bottom. Its condition selects the object and its indented body describes the output.

For complete input and output examples, see [conditions](template-engine-examples.md#combine-conditions), [lists](template-engine-examples.md#render-a-list-and-format-each-value), [JSON](template-engine-examples.md#produce-json-with-optional-fields), and [escaping](template-engine-examples.md#escape-template-characters).

## Rules

```itr
rule type(Order)
	$number
```

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current frame. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

## Data and slots

The engine represents input objects as frames. For a plain Java record, the class name is the frame type and each field supplies a named value called a slot. An object-valued slot can invoke another rule, and a list supplies multiple values under the same name.

[Generated decorators](template-engine-api.md#generated-decorators) supply types and slots from the model through an internal frame view.

```itr
rule type(Order)
	$number for $customer

rule type(Customer)
	$name
```

Use `Frames.builder(...)` to supply slots explicitly or register a custom adapter with `.add(Class, ObjectMapper)`.

## Conditions

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

## Lists and separators

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

## Optional expressions

```itr
<<Tracking: $trackingCode>>
```

The expression appears when its placeholders produce content. If `trackingCode` is absent, the label disappears with it.

Use `?` to supply an alternative:

```itr
<<Tracking: $trackingCode?Tracking unavailable>>
```

## Formatters

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

## Frame context

```itr
$<container>name
$<root>name
$<order.customer>name
```

`container` addresses the enclosing frame; `root` addresses the root frame. Named path segments traverse slots in order. The final placeholder selects a slot from the reached frame.

## Join a placeholder to literal text

Use `~` to separate a placeholder name from an adjacent suffix:

```itr
public class $name~Service {}
```

For `name = OrderBatch`, this produces `public class OrderBatchService {}`.

## Build a template in Java

```java
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

Template template = new Template()
	.add(new Rule().condition(allTypes("Order")).output(placeholder("number")))
	.add(new Rule().condition(allTypes("Customer")).output(placeholder("name")));
```

## Rendering configuration

```java
import io.quassar.templating.TemplateEngine;
import io.quassar.templating.model.Template.Configuration;
import io.quassar.templating.model.Template.Configuration.LineSeparator;

var configuration = new Configuration(Locale.ENGLISH, LineSeparator.LF);
TemplateEngine engine = TemplateEngine.with(template.ruleSet(), configuration);
```

Configuration controls locale-sensitive built-in formatting and output line endings. The default is English with LF line endings.

## Input ownership and errors

`TemplateEngine.with(Path.of("order-batch.itr"))` opens and closes a local template file for you.

`TemplateEngine.with(input)` reads UTF-8 and leaves the stream open. Close it in the calling code, typically with try-with-resources. The engine retains the resulting template, so it can render after the stream has been closed.

Unreadable or malformed input raises `io.quassar.templating.ports.TemplateReadException`. Rendering does not require generating or compiling a Java template class.

# Template syntax reference

Read a rule from top to bottom. Its condition selects the object and its indented body describes the output.

For complete input and output examples, see [conditions](template-engine-examples.md#combine-conditions), [lists](template-engine-examples.md#render-a-list-and-format-each-value), [JSON](template-engine-examples.md#produce-json-with-optional-fields), and [escaping](template-engine-examples.md#escape-template-characters).

## Rules

```itr
rule type(Step)
	$identifier
```

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current object. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

## Placeholders

A placeholder is a reference to a value that the engine inserts into the output. Write `$name` to look up the slot named `name` on the object the current rule is rendering. A slot is a named value exposed by that object. Slot names are matched without regard to case. Generated decorators expose the properties, children, and references of the model as slots. [Render an object](template-engine-api.md#render-an-object)

For each value in the slot, the engine looks for the first rule that matches that value. When no rule matches, a scalar such as text or a number renders as text, and an object produces no output. A missing slot also produces no output. Use an optional expression for fallback text or a separator to join repeated values. [Optional expressions](#optional-expressions) · [Lists and separators](#lists-and-separators)

```itr
rule type(Step)
	$identifier calls $action

rule type(Action)
	$name
```

When rendering the sample `Confirm` step, `$identifier` reads its identifier. The `$action` placeholder resolves to the referenced action, so the engine applies the `Action` rule. Inside that rule, `$name` reads the name from the action.

```text
Confirm calls Send confirmation
```

Use a qualified placeholder such as `$<container>identifier` to read a slot from a related object. [Frame context](template-engine-api.md#frame-context)

## Conditions

| Condition | Matches |
| --- | --- |
| `type(Step)` | A frame of type `Step` |
| `attribute(schedule)` | A frame with a `schedule` slot |
| `attribute(enabled, true)` | A frame whose `enabled` slot contains `true` |
| `trigger(action)` | Rendering triggered by the `action` placeholder |

Combine conditions with `and`, `or`, `not` and parentheses:

```itr
rule type(Step) and attribute(schedule)
	String schedule = $schedule+JsonString;

rule type(Step) and not attribute(schedule)
	String schedule = "";
```

## Lists and separators

```itr
$step...[$NL$NL]
$fallbackActions...[, ]
$properties...[, ]
```

`...` renders all values of a slot. The text inside `[...]` separates their rendered outputs.

| Mark | Meaning |
| --- | --- |
| `$NL` | Newline |
| `$TAB` | Tab |

## Optional expressions

```itr
<<String schedule = $schedule+JsonString;>>
```

The expression appears when its placeholders produce content. If `schedule` is absent, the declaration disappears with it.

Use `?` to supply an alternative:

```itr
String schedule = <<$schedule+JsonString?"immediate">>;
```

## Formatters

```itr
$identifier+UpperCase
$name+FirstUpperCase
$name+OnlyLetters+UpperCase
```

Formatters transform values before rendering and can be chained.

### Create your own formatter

Define a formatter in Java and register it by name before calling `render`. This formatter surrounds a value with square brackets.

```java
var engine = TemplateEngine.with(Path.of("workflow.itr"))
	.add("Wrapped", value -> "[" + value + "]");
```

Use `$identifier+Wrapped` in the template. An identifier of `Confirm` renders as `[Confirm]`.

### Built-in formatters


| Kind | Names |
| --- | --- |
| Text | `UpperCase`, `LowerCase`, `FirstUpperCase`, `FirstLowerCase`, `Capitalize` |
| JSON strings | `JsonString` adds quotes and escapes the value |
| Identifiers | `CamelCase`, `PascalCase`, `SnakeCase`, `KebabCase` |
| Filtering | `OnlyDigits`, `OnlyLetters`, `OnlyDigitsAndLetters` |
| Numbers | `Words`, `Separators`, `TwoDecimals` |
| Dates | `Year`, `YearMonth`, `YearMonthDay`, `ShortDate`, `FullDate` |

## Join a placeholder to literal text

Use `~` to separate a placeholder name from an adjacent suffix:

```itr
public class $identifier~Workflow {}
```

For `identifier = OrderFulfillment`, this produces `public class OrderFulfillmentWorkflow {}`.

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

## Data and slots

A slot is a named value available to the template. A slot containing another object can invoke another rule, and a list supplies multiple values under the same name.

[Generated decorators](template-engine-api.md#generated-decorators) supply the types and slots defined by the model.

```itr
rule type(Step)
	$identifier calls $action

rule type(Action)
	$name
```

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

Formatters transform values before rendering and can be chained. Register custom formatters with `engine.add("Label", formatter)` and use `$identifier+Label`.

Some built-in formatters:

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

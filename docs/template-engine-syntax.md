# Template syntax reference

Read a rule from top to bottom. Its condition selects the object and its indented body describes the output.

For complete input and output examples, see [conditions](template-engine-examples.md#combine-conditions), [lists](template-engine-examples.md#render-a-list-and-format-each-value), [JSON](template-engine-examples.md#produce-json-with-optional-fields), and [escaping](template-engine-examples.md#escape-template-characters).

## Rules

Each rule starts with `rule`, followed by its condition.

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current object. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

## Placeholders

A placeholder tells the engine where to insert a value in the output. Write `$name` to read the slot named `name` from the current frame. The frame is the input the engine uses to apply a rule. It can be built from a Java object or supplied directly. Slots hold zero or more values, and the engine matches slot names without regard to case. [Render an object](template-engine-api.md#render-an-object)

For each value in the slot, the engine looks for the first rule that matches that value. When no rule matches, a scalar such as text or a number renders as text, and an object produces no output. A missing slot also produces no output. Use an optional expression for fallback text or a separator to join repeated values. [Optional expressions](#optional-expressions) · [Lists and separators](#lists-and-separators)

A qualified placeholder uses `$<path>slotName` to read a slot from another frame. [Frame context](template-engine-api.md#frame-context) · [Workflow example](template-engine-examples.md#read-values-from-the-root-and-a-named-path)

## Conditions

| Condition syntax | Matches |
| --- | --- |
| `type(typeName)` | The current frame has the specified type. |
| `attribute(slotName)` | The current frame has the specified slot. |
| `attribute(slotName, value)` | At least one value in the slot matches the specified value. |
| `trigger(placeholderName)` | The specified placeholder triggered the current rendering. |

Combine conditions with `and`, `or`, and `not`. Use parentheses to group conditions. [Workflow conditions](template-engine-examples.md#combine-conditions) · [Matching the calling placeholder](template-engine-examples.md#choose-output-based-on-the-calling-slot)

## Lists and separators

`$slotName...[separator]` renders the values of a slot and inserts the separator between rendered outputs. [Workflow list example](template-engine-examples.md#render-a-list-and-format-each-value)

| Mark | Meaning |
| --- | --- |
| `$NL` | Newline |
| `$TAB` | Tab |

## Optional expressions

Use `<<body>>` to emit a body when its placeholders produce content. Use `<<body?fallback>>` to emit fallback text otherwise. [Workflow fallback example](template-engine-examples.md#optional-values-and-fallback-text)

## Formatters

Append `+FormatterName` to a placeholder to format its value. Append more formatter names to apply them in order.

### Create your own formatter

Register a formatter by name with `engine.add(name, formatter)` before rendering, then use that name after `+` in the template. [Register a formatter](template-engine-api.md#register-a-formatter)

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

Use `~` to mark the end of a placeholder when literal text follows immediately. In `$slotName~suffix`, the engine reads `slotName` and appends the literal suffix. [Workflow escaping example](template-engine-examples.md#escape-template-characters)

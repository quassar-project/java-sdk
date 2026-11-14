# Template syntax reference

The EBNF below covers the full template parser. Uppercase names are lexer tokens. Square brackets mark optional elements, braces mark repetition, and `|` separates alternatives. [Lexer rules](../template-engine/src/main/resources/grammar/ItrLexer.g4) · [Parser rules](../template-engine/src/main/resources/grammar/ItrParser.g4)

```ebnf
template = { COMMENT }, rule, { COMMENT },
           { rule, { COMMENT } }, EOF ;
rule = BEGIN_RULE, header, body ;
header = condition, BEGIN_BODY ;
condition = term, { ( AND | OR ), term } ;
term = [ NOT ], ( predicate | LPAREN, condition, RPAREN ) ;
predicate = NAME, LPAREN, argument,
            { COMMA, argument }, RPAREN ;
argument = ( NAME | OTHER ), { NAME | OTHER } ;

body = { bodyPart } ;
bodyPart = TEXT | placeholder | optionalExpression ;
optionalExpression = BEGIN_EXPRESSION, branch,
                     { ELSE, branch }, END_EXPRESSION ;
branch = bodyPart, { bodyPart } ;
placeholder = TRIGGER, [ TARGET ], ID,
              { OPTION, ID }, [ LIST, SEPARATOR ] ;
```

`BEGIN_RULE` is the `rule` keyword. `BEGIN_BODY` marks the line break after the condition and can consume the first indentation tab or four spaces. In a placeholder, `TRIGGER` is `$`, `TARGET` is an optional path in angle brackets, `OPTION` is `+`, and `LIST` is `...`. The lexer reads `SEPARATOR` as the text enclosed in square brackets. Optional expressions use `<<` and `>>`, with `?` between branches.

The sections below use the Workflows sample model. [Sample M1 model](sample-m1-model.md) · [Template cookbook](template-engine-cookbook.md)

## Rules

A rule has a header and a body. The header starts with the keyword `rule`, followed by a condition built from predicates, logical operators, and parenthesized groups. The body starts on the next line.

This Workflows rule renders the identifier of the current step.

```itr
rule type(Step)
	$identifier
```

The `Confirm` step renders as `Confirm`.

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current object. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

## Placeholders

A placeholder tells the engine where to insert a value in the output. Write `$name` to read the slot named `name` from the current frame. The frame is the input the engine uses to apply a rule. It can be built from a Java object or supplied directly. Slots hold zero or more values, and the engine matches slot names without regard to case. [Render an object](template-engine-api.md#render-an-object)

For each value in the slot, the engine looks for the first rule that matches that value. When no rule matches, a scalar such as text or a number renders as text, and an object produces no output. A missing slot also produces no output. Use an optional expression for fallback text or a separator to join repeated values. [Optional expressions](#optional-expressions) · [Lists and separators](#lists-and-separators)

A qualified placeholder uses `$<path>slotName` to read a slot from another frame. [Frame context](template-engine-api.md#frame-context) · [Workflow example](template-engine-cookbook.md#read-values-from-the-root-and-a-named-path)

Render the `Confirm` step to follow its action reference. The `Step` rule reads `$action`, and the `Action` rule reads `$name` from the referenced action.

```itr
rule type(Step)
	$identifier calls $action

rule type(Action)
	$name
```

```text
Confirm calls Send confirmation
```

## Conditions

| Condition syntax | Matches |
| --- | --- |
| `type(typeName)` | The current frame has the specified type. |
| `attribute(slotName)` | The current frame has the specified slot. |
| `attribute(slotName, value)` | At least one value in the slot matches the specified value. |
| `trigger(placeholderName)` | The specified placeholder triggered the current rendering. |

Combine conditions with `and`, `or`, and `not`. Use parentheses to group conditions. For an enabled step, the following rule renders `Confirm order: enabled`.

```itr
rule type(Step) and attribute(enabled, true)
	$label: enabled
```

See complete examples with fallback rules. [Workflow conditions](template-engine-cookbook.md#combine-conditions) · [Matching the calling placeholder](template-engine-cookbook.md#choose-output-based-on-the-calling-slot)

## Lists and separators

`$slotName...[separator]` renders the values of a slot and inserts the separator between rendered outputs. [Workflow list example](template-engine-cookbook.md#render-a-list-and-format-each-value)

| Mark | Meaning |
| --- | --- |
| `$NL` | Newline |
| `$TAB` | Tab |

Render the `Confirm` step to list its fallback actions.

```itr
rule type(Step)
	$fallbackActions...[, ]

rule type(Action)
	$name
```

```text
Queue for retry, Log failure
```

## Optional expressions

Use `<<body>>` to emit a body when its placeholders produce content. Use `<<body?fallback>>` to emit fallback text otherwise.

```itr
rule type(Step)
	$label: <<$schedule?Run immediately>>
```

The sample step has no schedule, so it renders as `Confirm order: Run immediately`. [Workflow fallback example](template-engine-cookbook.md#optional-values-and-fallback-text)

## Formatters

Append `+FormatterName` to a placeholder to format its value. Append more formatter names to apply them in order.

Render `SendConfirmation` with this rule to uppercase its name.

```itr
rule type(Action)
	$name+UpperCase
```

```text
SEND CONFIRMATION
```

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

Use `~` to mark the end of a placeholder when literal text follows immediately. In `$slotName~suffix`, the engine reads `slotName` and appends the literal suffix. [Workflow escaping example](template-engine-cookbook.md#escape-template-characters)

```itr
rule type(Workflow)
	public class $identifier~Workflow {}
```

```java
public class OrderFulfillmentWorkflow {}
```

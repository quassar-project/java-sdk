# Template reference

[Quick start](../README.md) · [Examples](examples.md)

Read a rule from top to bottom. Its condition selects the object and its indented body describes the output.

## Rules

```itr
rule type(Book)
	$title
```

A template is an ordered collection of rules. The engine selects the first rule whose condition matches the current frame. Put specific conditions before general ones.

Indent the rule body with a tab. Additional indentation becomes part of the rendered text.

## Data and slots

The engine represents input objects as frames. For a plain Java record, the class name is the frame type and each field supplies a named value called a slot. An object-valued slot can invoke another rule, and a list supplies multiple values under the same name.

[Annotated methods](../README.md#annotated-objects) and [generated decorators](../README.md#generated-decorators) can supply their own types and slots.

```itr
rule type(Book)
	$title by $author

rule type(Author)
	$name
```

You can supply frames explicitly with `io.quassar.templating.adapters.reflection.ReflectiveFrameBuilder` or register a custom adapter with `.add(Class, ObjectMapper)`.

## Conditions

| Condition | Matches |
| --- | --- |
| `type(Book)` | A frame of type `Book` |
| `attribute(isbn)` | A frame with an `isbn` slot |
| `attribute(available, true)` | A frame whose `available` slot contains `true` |
| `trigger(title)` | Rendering triggered by the `title` placeholder |

Combine conditions with `and`, `or`, `not` and parentheses:

```itr
rule type(Book) and attribute(isbn)
	$title — ISBN $isbn

rule type(Book) and not attribute(isbn)
	$title
```

## Lists and separators

```itr
$books...[$NL$NL]
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
<<ISBN: $isbn>>
```

The expression appears when its placeholders produce content. If `isbn` is absent, the label disappears with it.

Use `?` to supply an alternative:

```itr
<<ISBN: $isbn?ISBN unavailable>>
```

## Formatters

```itr
$title+UpperCase
$name+FirstUpperCase
$name+OnlyLetters+UpperCase
```

Formatters transform values before rendering and can be chained. Register custom formatters with `engine.add("Label", formatter)` and use `$title+Label`.

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
$<book.author>name
```

`container` addresses the enclosing frame; `root` addresses the root frame. Named path segments traverse slots in order. The final placeholder selects a slot from the reached frame.

## Join a placeholder to literal text

Use `~` to separate a placeholder name from an adjacent suffix:

```itr
public class $name~Service {}
```

For `name = Library`, this produces `public class LibraryService {}`.

## Build a template in Java

```java
import io.quassar.templating.model.Rule;
import io.quassar.templating.model.Template;

import static io.quassar.templating.model.outputs.Outputs.placeholder;
import static io.quassar.templating.model.predicates.Predicates.allTypes;

Template template = new Template()
	.add(new Rule().condition(allTypes("Book")).output(placeholder("title")))
	.add(new Rule().condition(allTypes("Author")).output(placeholder("name")));
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

`TemplateEngine.with(input)` reads UTF-8 and leaves the stream open. Close it in the calling code, typically with try-with-resources. The engine retains the resulting template, so it can render after the stream has been closed.

Unreadable or malformed input raises `io.quassar.templating.ports.TemplateReadException`. Rendering does not require generating or compiling a Java template class.

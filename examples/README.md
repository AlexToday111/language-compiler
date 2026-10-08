# Project D Examples

Run from the repository root with JDK 11+ and Maven 3.6.3+:

```sh
mvn clean verify
bash src/lexer/run_demo.sh
```

The Bash script tokenizes all seven programs. For one file (Bash is optional):

```sh
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/02_nested_collections.d
```

Validate syntax with the separate parser CLI:

```sh
java -cp target/classes io.github.alextoday111.projectd.parser.ParserMain examples/valid/01_calculator.d
```

It prints `Parsing successful. Syntax is valid.` and exits with code `0`.
To see a syntax failure, use the preserved legacy calculator:

```sh
java -cp target/classes io.github.alextoday111.projectd.parser.ParserMain examples/valid/02_calculator.d
```

It reports `Syntax error at 13:32`, an expected expression and actual `NEWLINE`
on stderr, then exits with code `1`. These commands validate syntax only.

## Advanced Programs

| File | Purpose and constructs | Intended output values |
|---|---|---|
| [01_calculator.d](valid/01_calculator.d) | Four arithmetic operations stored as functions in nested tuples; a callback receives an array of arguments; results collected in a tuple. | `25`, `15`, `100`, `4` |
| [02_nested_collections.d](valid/02_nested_collections.d) | Tuples inside tuples, arrays inside arrays, tuples containing arrays, arrays containing tuples; positional/named access and mixed indexing, including `a.1.2.3`. | `30`, `1`, `3`, `60`, `2.5`, `beta`, `9` |
| [03_bubble_sort.d](valid/03_bubble_sort.d) | A function sorts a five-element array with nested loops, comparisons, swaps, and early return when no swaps occur. | `1 2 4 5 8` |
| [04_higher_order.d](valid/04_higher_order.d) | Array and tuple arguments, repeated callback calls, matrix processing, and returning an existing function value without requiring closure capture. | `25`, `7`, then `25 16 34`, then `11` |

Each advanced file is standalone. It uses `:=`, `func`, `is ... end` / `=>`,
`then ... end`, `loop ... end`, bracket arrays and brace tuples. Positions
start at one. Arrow expressions stay on one line; multiline aggregate layout
uses assumption A8 in [grammar notes](../docs/grammar-notes.md).

Bubble Sort takes the known contiguous array size (at least two); it assumes
ascending inclusive ranges. Its inner index stays in `1..(count - 1)`, so
`i + 1` never exceeds `count`. The higher-order range `1..3` also assumes
inclusive endpoints. No array-length builtin or closure behavior is invented.
Printed separators/formatting are unspecified; the table lists intended
values, not a verified console transcript.

## Original Lexer Inputs

| File | Demonstration | Intended output values |
|---|---|---|
| [01_simple.d](valid/01_simple.d) | A variable initialized with a real literal. | `12.5` |
| [02_calculator.d](valid/02_calculator.d) | Function arguments and nested tuple fields. | `42`, `360` |
| [03_nested_values.d](valid/03_nested_values.d) | An array of arrays inside a tuple, passed to a callback. | `7` |

These files are retained unchanged. The original `02_calculator.d` places a
newline after `=>`; the parser rejects this lexer input at line 13, column 32
under the current EBNF's continuation rules. The advanced replacement keeps its arrow
expressions on one line or uses an explicit function body.

## Validation Status

- **Lexer validation:** available now. JUnit compares complete token streams,
  lexemes, typed values, positions and EOF against committed fixtures for all
  seven files. Regression tests also assert numeric/access/range edge cases.
- **Parser validation:** all four advanced programs, `01_simple.d` and
  `03_nested_values.d` pass. Integration tests explicitly verify rejection of
  the unchanged original `02_calculator.d` at `13:32` (`NEWLINE`). These are
  checks against the grammar draft and its provisional conventions.
- **Runtime validation:** unavailable until Interpreter is implemented. Every
  output above is an intended result, not an observed execution result.

The old `src/lexer/project-d-lexer.jar` is a historical artifact and does not
include the tuple-access fix. Use the Maven-built JAR for these examples.

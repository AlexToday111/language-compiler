# Project D Examples

Run from the repository root with JDK 11+ and Maven 3.6.3+:

```sh
mvn clean verify
bash scripts/lexer-demo.sh
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

Use the Maven-built JAR in `target` for these examples.

## Semantic Analysis and AST Transformations

The new [semantic fixtures](semantic/) keep the seven original inputs unchanged.
After `mvn clean verify`, run:

```sh
java -cp target/classes io.github.alextoday111.projectd.semantic.SemanticMain examples/semantic/valid.d
```

`undeclared.d`, `duplicate.d`, `invalid-return.d`, and `invalid-exit.d` each fail
with the corresponding semantic diagnostic and exit code `1`. In `invalid-exit.d`,
the enclosing loop does not extend into the function's control-flow context.

Use `SemanticMain --compare examples/semantic/optimize.d` to display both trees.
The committed [snapshot](semantic/optimize.ast.txt) shows `5 + 3 * 2` becoming
`11`, the constant true branch becoming a `ScopedBlock`, real and Boolean folding,
and unreachable prints disappearing after `return` and `exit`. These are AST
transformations, not interpreter output. The PowerShell script
`scripts/semantic-demo.ps1` runs every fixture and checks expected exit codes.

## Presentation Scripts

Build once with `mvn clean verify`, then run from the repository root:

```powershell
powershell -File scripts/semantic-demo.ps1 -Pause
powershell -File scripts/calculator-demo.ps1 -Pause
```

The first script shows a trivial valid program, AST optimizations, and all four
semantic errors. The second uses [a small calculator](presentation/calculator.d)
with four arithmetic functions and constant expressions. Both print the source
and label each stage. `-Pause` waits for Enter between stages; omit it for an
unattended run. These demonstrations validate and transform ASTs; the compiler
does not execute programs yet.

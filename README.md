<h1 align="center">Project D</h1>

A Compiler Construction project for a dynamically typed language. Java 11+.

<h2 align="center">Build and Run</h2>

Requires JDK 11+ and Maven 3.6.3+. Run from the repository root:

```sh
mvn clean verify
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
java -cp target/classes io.github.alextoday111.projectd.parser.ParserMain examples/valid/01_calculator.d
java -cp target/classes io.github.alextoday111.projectd.parser.ParserMain --ast examples/valid/01_calculator.d
```

Parser success: `Parsing successful. Syntax is valid.` Exit codes: `0` success,
`1` lexical/syntax error, `2` usage error, `3` file error.
See [lexer instructions](docs/lexer.md) and [examples](examples/README.md).
AST output uses UTF-8 and Unicode tree branches. Use a UTF-8 terminal.
The AST is printed only after the entire input has parsed successfully;
errors go to stderr and leave stdout empty.

<h2 align="center">Architecture</h2>

```text
Source → Lexer → Tokens → Parser → AST
```

The handwritten recursive descent parser exposes `Parser(List<Token>)` and
`public Program parse()`. It validates complete input through EOF and stops at the first
error with `ParseException` (line, column, expected and actual token).
It supports statements, precedence tiers, functions, chained references,
nested arrays and tuples. It builds an immutable, typed AST using Java 11
classes and interfaces. Semantic analysis, interpreter and runtime are planned;
the parser does not evaluate expressions or execute programs.

`Program` and `Block` hold ordered statements. Statement nodes include
`VariableDeclaration` (or `VariableDeclarationGroup` for comma-separated
definitions), `Assignment`, `IfStatement` with an optional `Else`, `WhileLoop`,
`ForLoop`, `InfiniteLoop`, `ReturnStatement`, `ExitStatement` and `PrintStatement`.
Expression nodes include `BinaryExpression`, `UnaryExpression`, typed `Literal`
subclasses, `Identifier`, `FunctionLiteral`, `FunctionCall`, `ArrayLiteral`,
`TupleLiteral`, `ArrayIndex`, `TupleFieldAccess` and `TypeCheckExpression`.
`Parameters`, `TupleElement`, `TupleField` and `TypeIndicator` preserve parameter
order, named/positional fields and type tests. Calls, indices and fields nest
around their reference target, preserving chains such as `data.records[1].payload.score`.

Each node exposes typed getters and ordered `getChildren()` for traversal.
Lists are defensive immutable snapshots. Optional syntax uses nullable getters:
initializers, return values, else branches, for variables/range ends and tuple
element names. Functions have exactly one of `body` (a block) and `expressionBody`;
tuple fields have either a name or a numeric position. Parentheses are represented
by expression nesting; binary operators fold left within each grammar tier.
`AstPrinter.print(node)` returns a deterministic tree without evaluating it.

For `var x := 10 + 5 * 2`:

```text
Program
└── VariableDeclaration
    ├── Identifier: x
    └── BinaryExpression: +
        ├── IntegerLiteral: 10
        └── BinaryExpression: *
            ├── IntegerLiteral: 5
            └── IntegerLiteral: 2
```

<h2 align="center">Structure and Specification</h2>

```text
src/main/java/io/github/alextoday111/projectd/
  lexer/                        Lexer, tokens, diagnostics, Main
  parser/                       Parser, ParseException, ParserMain
  ast/                          Typed AST nodes and AstPrinter
  semantic/                     Planned
  interpreter/ runtime/         Planned
src/test/java/io/github/alextoday111/projectd/
  lexer/ parser/                Unit and integration tests
scripts/                        Lexer demo and test scripts
examples/valid/                 Seven preserved examples
docs/                           Grammar, notes, team PDFs
```

- [EBNF grammar](docs/grammar.ebnf)
- [Grammar decisions and parser limitations](docs/grammar-notes.md)
- [Team introduction](docs/language-spec/krutaya_komanda%28intro%29.pdf)
- [Lexer presentation](docs/language-spec/krutaya_komanda%28lexer%29.pdf)

The teacher's **Project D.pdf** is the primary source and is not redistributed.
A1/A3/A4/A8 remain provisional; short `if ... =>` takes one statement under
project convention N2. Prefixes cannot directly target references; calls and
explicit parameter lists are nonempty; value tuples are nonempty.

<h2 align="center">Validation</h2>

`mvn clean verify` runs 273 JUnit tests: the original 60 lexer and 176 parser
tests, plus 37 AST and CLI checks. AST tests assert node types, values, child
order, precedence, associativity, functions, collections, control flow,
references, type tests, diagnostics and exact tree output.
CI runs the same command on Temurin 17, compiling for Java 11.
Six examples pass syntax validation. The original `02_calculator.d` remains
Lexer-only: its newline after `=>` is rejected at **13:32**. Runtime results
are unverified.

Source comments have been removed without changing grammar rules or lexer
logic. Blank lines in examples preserve source line numbers. The reviewed
lexer fixtures change only newline columns on former comment lines.

<h1 align="center">Project D</h1>

A Compiler Construction project for a dynamically typed language. Java 11+.

<h2 align="center">Build and Run</h2>

Requires JDK 11+ and Maven 3.6.3+. Run from the repository root:

```sh
mvn clean verify
# Print tokens:
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
# Validate syntax:
java -cp target/classes io.github.alextoday111.projectd.parser.ParserMain examples/valid/01_calculator.d
```

Parser success: `Parsing successful. Syntax is valid.` Exit codes: `0` success,
`1` lexical/syntax error, `2` usage error, `3` file error.
See [lexer instructions](src/lexer/README.md) and [examples](examples/README.md).

<h2 align="center">Architecture</h2>

```text
Source -> Lexer -> List<Token> -> Parser -> Syntax validation
```

The handwritten recursive descent parser exposes `Parser(List<Token>)` and
`void parse()`. It validates complete input through EOF and stops at the first
error with `ParseException` (line, column, expected and actual token).
It supports statements, precedence tiers, functions, chained references,
nested arrays and tuples. It builds no AST/CST and performs no semantic checks
or execution. AST, semantic analysis, interpreter and runtime are planned.

<h2 align="center">Structure and Specification</h2>

```text
src/main/java/io/github/alextoday111/projectd/
  lexer/                        Lexer, tokens, diagnostics, Main
  parser/                       Parser, ParseException, ParserMain
  ast/ semantic/                Planned
  interpreter/ runtime/         Planned
src/test/java/io/github/alextoday111/projectd/
  lexer/ parser/                Unit and integration tests
src/lexer/                      Lexer scripts, README, historical JAR
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

`mvn clean verify` runs 236 JUnit tests: 60 lexer and 176 parser tests.
CI runs the same command on Temurin 17, compiling for Java 11.
Six examples pass syntax validation. The original `02_calculator.d` remains
Lexer-only: its newline after `=>` is rejected at **13:32**. Runtime results
are unverified.

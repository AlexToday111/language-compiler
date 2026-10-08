<h1 align="center">Project D</h1>

A Java interpreter project for a dynamically typed language, developed for
Compiler Construction. Only the lexer is implemented.

<h2 align="center">Architecture</h2>

```text
Source ? Lexer ? Tokens ? Parser ? AST ? Interpreter
```

Parser + AST will use handwritten recursive descent; the interpreter will
execute the AST directly, with semantic checks and runtime support.

<h2 align="center">Structure</h2>

```text
pom.xml
.github/workflows/ci.yml
docs/
  grammar.ebnf
  grammar-notes.md
  language-spec/                 Team PDFs
src/main/java/io/github/alextoday111/projectd/
  lexer/                         Lexer, tokens, diagnostics, Main
  parser/ ast/ semantic/         Planned (.gitkeep only)
  interpreter/ runtime/          Planned (.gitkeep only)
src/test/java/io/github/alextoday111/projectd/lexer/
  LexerTest.java
src/lexer/                       Scripts, README, original JAR
src/codegen/                     Reserved (.gitkeep only)
examples/valid/                  Three existing lexer examples
```

<h2 align="center">Language Specification</h2>

- [EBNF grammar](docs/grammar.ebnf)
- [Assumptions, source conflicts, and open questions](docs/grammar-notes.md)
- [Team introduction](docs/language-spec/krutaya_komanda%28intro%29.pdf)
- [Lexer presentation](docs/language-spec/krutaya_komanda%28lexer%29.pdf)

The teacher's **Project D.pdf** is the primary source. It is not redistributed
here. Team slides contain conflicting syntax; see the grammar notes.

<h2 align="center">Build and Run</h2>

Requires JDK 11+ and Maven 3.6.3+. Run from the repository root:

```sh
mvn clean verify
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
```

The lexer prints tokens, not the program's execution result. For all examples:

```sh
bash src/lexer/run_demo.sh
```

<h2 align="center">Testing</h2>

```sh
mvn test
```

JUnit 5 runs seven existing lexer scenarios (five token sequences and two
lexical errors). CI runs `mvn clean verify` on Temurin 17, compiling for Java 11,
for pushes and pull requests to `main`. See [lexer instructions](src/lexer/README.md).

<h2 align="center">Current Status</h2>

| Component | Status |
|---|---|
| Lexer | Implemented |
| Parser | Planned |
| AST | Planned |
| Semantic Analysis | Planned |
| Interpreter | Planned |
| Runtime | Planned |

<h2 align="center">Roadmap</h2>

Lexer ? Parser ? AST ? Interpreter ? runtime and semantic checks ? integration tests.
Resolve the grammar's open questions before implementing the parser.

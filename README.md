# Compiler Construction

## Project Overview

A university project for developing a compiler for Project D. The current
implementation provides lexical analysis in Java. Parsing, AST construction,
semantic analysis, and code generation are planned; programs cannot yet be
compiled or executed by this project.

## Project Structure

```text
.
|-- README.md
|-- .gitignore
|-- docs/
|   |-- language-spec/
|   |   `-- Project_D_Dynamic_Language (2).pptx
|   `-- architecture/.gitkeep
|-- src/
|   |-- lexer/
|   |   |-- src/                       # Existing Java sources; entry point: Main
|   |   |-- tests/LexerTest.java        # Existing lexer tests
|   |   |-- examples/                  # Three existing Project D programs
|   |   |-- run_demo.sh
|   |   |-- run_tests.sh
|   |   |-- project-d-lexer.jar         # Original prebuilt artifact
|   |   |-- README.md
|   |   |-- START_HERE_RU.txt
|   |   `-- DEMO_SCRIPT_RU.txt
|   |-- parser/.gitkeep
|   |-- ast/.gitkeep
|   |-- semantic/.gitkeep
|   `-- codegen/.gitkeep
|-- tests/.gitkeep
`-- examples/.gitkeep
```

The original lexer package retains its internal layout so its scripts and
relative paths continue to work. Root-level `tests/` and `examples/` reserve
space for future compiler tests and programs. `docs/architecture/` is reserved
for architecture documentation. Planned directories contain only `.gitkeep`.

## Current Implementation Status

| Component | Status |
|---|---|
| Lexer | Implemented |
| Parser | Planned |
| AST | Planned |
| Semantic Analyzer | Planned |
| Code Generator | Planned |

## Language Specification

The original [Project D presentation](docs/language-spec/Project_D_Dynamic_Language%20%282%29.pptx)
is the primary reference for language rules, syntax, and grammar until a
separate formal specification is available.

## Getting Started

Use JDK 11 or newer, with `java` and `javac` on `PATH`. The lexer uses the Java
standard library and requires no external dependencies. Compilation, the
existing tests, and all three examples were verified with JDK 17.

From the repository root (PowerShell, Bash, or a compatible terminal):

```sh
javac -encoding UTF-8 -d src/lexer/out src/lexer/src/*.java src/lexer/tests/*.java
java -cp src/lexer/out LexerTest
java -cp src/lexer/out Main src/lexer/examples/01_simple.d
```

Pass one or more source file paths to `Main` to print their tokens. With no
arguments, it tokenizes its existing built-in example.

On macOS/Linux or with Bash installed, the original scripts can also be used:

```sh
bash src/lexer/run_tests.sh
bash src/lexer/run_demo.sh
```

The original prebuilt JAR is preserved and can run with Java 11 or newer:

```sh
java -jar src/lexer/project-d-lexer.jar src/lexer/examples/01_simple.d
```

For the original lexer documentation, see [src/lexer/README.md](src/lexer/README.md).

## Development Roadmap

1. Implement syntax analysis in `src/parser/` using the language specification.
2. Define the abstract syntax tree in `src/ast/` and integrate it with the parser.
3. Add semantic analysis and type checking in `src/semantic/`.
4. Implement code generation in `src/codegen/`.
5. Add compiler tests, example programs, and architecture documentation as
   these components are developed.

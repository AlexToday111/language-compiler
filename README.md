<h1 align="center">Compiler Construction</h1>

A university compiler project for Project D. Currently implements a Java lexer.

<h2 align="center">Structure</h2>

```text
docs/           Language specification and architecture documentation
src/lexer/      Existing lexer, tests, and examples
src/parser/     Syntax analysis (planned)
src/ast/        Abstract syntax tree (planned)
src/semantic/   Semantic analysis (planned)
src/codegen/    Code generation (planned)
tests/          Future compiler tests
examples/       Future compiler examples
```

<h2 align="center">Status</h2>

| Component | Status |
|---|---|
| Lexer | Implemented |
| Parser | Planned |
| AST | Planned |
| Semantic Analyzer | Planned |
| Code Generator | Planned |

<h2 align="center">Language Specification</h2>

[Project D presentation](docs/language-spec/Project_D_Dynamic_Language%20%282%29.pptx) — the primary reference for language rules.

<h2 align="center">Quick Start</h2>

Requires JDK 11+. Run from the repository root:

```sh
cd src/lexer
javac -encoding UTF-8 -d out src/*.java tests/*.java
java -cp out LexerTest
java -cp out Main examples/01_simple.d
```

Expected: `All lexer tests passed: 45`, followed by the example's token list.
See [lexer instructions](src/lexer/README.md) for Bash and JAR commands.

<h2 align="center">Roadmap</h2>

Parser → AST → semantic analysis → code generation.

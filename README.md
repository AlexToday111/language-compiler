# Project D

A compiler for a dynamically typed language, written in Java 11.

Current stage: **Semantic Analysis and AST Optimization**.

The compiler reads source files, builds an immutable AST, checks names and control-flow
contexts, and produces an optimized AST. Interpreter and runtime execution are the next stage.

## Implemented Pipeline

```mermaid
flowchart LR
    Source[Source code] --> Lexer[Lexer]
    Lexer --> Tokens[Tokens]
    Tokens --> Parser[Parser]
    Parser --> AST[Immutable AST]
    AST --> Semantic[Semantic analysis]
    Semantic --> Optimizer[AST optimization]
    Optimizer --> Result[Optimized AST]
    Result -. Planned .-> Runtime[Interpreter and runtime]
```

- **Lexer:** tokenization and lexical diagnostics.
- **Parser and AST:** recursive descent parsing, immutable nodes, and tree printing.
- **Semantic analysis:** declaration before usage, lexical scopes, duplicate declarations,
  and validation of `return` and `exit` contexts, including nested functions and loops.
- **AST optimization:** conservative constant folding, constant branch simplification
  with scope preservation, and unreachable code elimination.

Semantic validation leaves the original AST unchanged. Optimization constructs replacement
nodes and runs only after successful validation in the CLI. Programs are not executed yet.

## Build and Demonstrate

Requires JDK 11+ and Maven. Run from the repository root:

```sh
mvn clean verify
```

The test suite covers the lexer, parser, AST, semantic checks, optimizations, and CLI behavior.

For a presentation in PowerShell:

```powershell
powershell -File scripts/semantic-demo.ps1 -Pause
powershell -File scripts/calculator-demo.ps1 -Pause
```

The first demonstration shows a simple program, semantic errors, and AST transformations.
The second shows a calculator with four arithmetic operations and constant folding.
Both display source examples and original/optimized trees. Use Enter to advance,
or omit `-Pause` for an unattended run. See [examples](examples/README.md) for details.

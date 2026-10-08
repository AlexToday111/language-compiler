# Project D Lexer

Java lexer that prints tokens from Project D source files. Requires JDK 11+.
Run these commands from `src/lexer/`.

## Build and Check

```sh
javac -encoding UTF-8 -d out src/*.java tests/*.java
java -cp out LexerTest
java -cp out Main examples/01_simple.d
```

Expected: `All lexer tests passed: 45` and a token list for the example.
Pass another file path to `Main` to tokenize it.

## Bash Scripts

Run the existing tests and all three examples:

```sh
bash run_tests.sh
bash run_demo.sh
```

## Prebuilt JAR

Requires only Java 11+:

```sh
java -jar project-d-lexer.jar examples/01_simple.d
```

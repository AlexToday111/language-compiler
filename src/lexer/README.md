# Project D Lexer

Prints tokens from Project D files. Requires JDK 11+ and Maven 3.6.3+.
Run from the repository root:

```sh
mvn clean verify
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
```

Expected: 60 passing tests and a token list for the example. To run only tests
or all seven examples (Bash required for the scripts):

```sh
mvn test
bash src/lexer/run_tests.sh
bash src/lexer/run_demo.sh
```

For one file, pass a path relative to the repository root:

```sh
bash src/lexer/run_demo.sh examples/valid/02_nested_collections.d
```

After an emitted `DOT`, digits form an `INTEGER` tuple position: `a.1.2.3`
contains three indices. Ordinary `3.14` stays `REAL`, and `..` stays `RANGE`.
See [ambiguity and limits](../../docs/grammar-notes.md#numeric-tuple-access-convention).

The original JAR is retained unchanged as a legacy artifact. It has neither
the new package names nor the tuple-access fix; use the Maven JAR above for
advanced examples. It can still tokenize the original simple input:

```sh
java -jar src/lexer/project-d-lexer.jar examples/valid/01_simple.d
```

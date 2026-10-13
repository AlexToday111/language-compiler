# Project D Lexer

Prints tokens from Project D files. Requires JDK 11+ and Maven 3.6.3+.
Run from the repository root:

```sh
mvn clean verify
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
```

Expected: 273 passing tests and a token list for the example. To run only tests
or all seven examples (Bash required for the scripts):

```sh
mvn test
bash scripts/lexer-tests.sh
bash scripts/lexer-demo.sh
```

For one file, pass a path relative to the repository root:

```sh
bash scripts/lexer-demo.sh examples/valid/02_nested_collections.d
```

After an emitted `DOT`, digits form an `INTEGER` tuple position: `a.1.2.3`
contains three indices. Ordinary `3.14` stays `REAL`, and `..` stays `RANGE`.
See [ambiguity and limits](grammar-notes.md#numeric-tuple-access-convention).

The lexer source lives in `src/main/java/io/github/alextoday111/projectd/lexer`.
Maven builds the executable JAR in `target`; compiled artifacts are not kept
alongside source files.

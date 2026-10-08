# Project D Lexer

Prints tokens from Project D files. Requires JDK 11+ and Maven 3.6.3+.
Run from the repository root:

```sh
mvn clean verify
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar examples/valid/01_simple.d
```

Expected: seven passing tests and a token list for the example. To run only tests
or all three examples (Bash required for the scripts):

```sh
mvn test
bash src/lexer/run_tests.sh
bash src/lexer/run_demo.sh
```

For one file, pass a path relative to the repository root:

```sh
bash src/lexer/run_demo.sh examples/valid/02_calculator.d
```

The original JAR is retained unchanged as a legacy artifact; it does not contain
the new Java package names. With Java 11+, it still runs from the repository root:

```sh
java -jar src/lexer/project-d-lexer.jar examples/valid/01_simple.d
```

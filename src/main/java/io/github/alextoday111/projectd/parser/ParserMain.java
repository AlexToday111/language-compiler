package io.github.alextoday111.projectd.parser;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.lexer.LexicalException;

/** Separate CLI; the executable JAR continues to launch the original lexer. */
public final class ParserMain {
    private ParserMain() {
    }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) {
            System.exit(code);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("Usage: ParserMain <file.d>");
            return 2;
        }
        try {
            String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
            new Parser(new Lexer(source).scanTokens()).parse();
            out.println("Parsing successful. Syntax is valid.");
            return 0;
        } catch (ParseException | LexicalException exception) {
            err.println(exception.getMessage());
            return 1;
        } catch (IOException | InvalidPathException exception) {
            err.println("Cannot read " + args[0] + ": " + exception.getMessage());
            return 3;
        }
    }
}

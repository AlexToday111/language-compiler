package io.github.alextoday111.projectd.parser;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import io.github.alextoday111.projectd.ast.AstPrinter;
import io.github.alextoday111.projectd.ast.Program;

import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.lexer.LexicalException;


public final class ParserMain {
    private ParserMain() {
    }

    public static void main(String[] args) {
        int code = run(args, new PrintStream(System.out, true, StandardCharsets.UTF_8),
                new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (code != 0) {
            System.exit(code);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        boolean printAst = args.length == 2 && "--ast".equals(args[0]);
        if (!printAst && (args.length != 1 || args[0].startsWith("--"))) {
            err.println("Usage: ParserMain <file.d>");
            err.println("       ParserMain --ast <file.d>");
            return 2;
        }
        String file = args[printAst ? 1 : 0];
        try {
            String source = Files.readString(Path.of(file), StandardCharsets.UTF_8);
            Program program = new Parser(new Lexer(source).scanTokens()).parse();
            if (printAst) {
                out.print(new AstPrinter().print(program));
            } else {
                out.println("Parsing successful. Syntax is valid.");
            }
            return 0;
        } catch (ParseException | LexicalException exception) {
            err.println(exception.getMessage());
            return 1;
        } catch (IOException | InvalidPathException exception) {
            err.println("Cannot read " + file + ": " + exception.getMessage());
            return 3;
        }
    }
}

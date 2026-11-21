package io.github.alextoday111.projectd.interpreter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import io.github.alextoday111.projectd.ast.Program;
import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.lexer.LexicalException;
import io.github.alextoday111.projectd.optimization.AstOptimizer;
import io.github.alextoday111.projectd.parser.ParseException;
import io.github.alextoday111.projectd.parser.Parser;
import io.github.alextoday111.projectd.runtime.RuntimeError;
import io.github.alextoday111.projectd.semantic.SemanticAnalyzer;
import io.github.alextoday111.projectd.semantic.SemanticException;

public final class InterpreterMain {
    private InterpreterMain() { }

    public static void main(String[] args) {
        int code = run(args, new PrintStream(System.out, true, StandardCharsets.UTF_8),
                new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        boolean optimize = args.length == 2 && args[0].equals("--optimize");
        if ((!optimize && args.length != 1) || args[args.length - 1].startsWith("--")) {
            err.println("Usage: InterpreterMain [--optimize] <file.d>");
            return 2;
        }
        String file = args[args.length - 1];
        try {
            String source = Files.readString(Path.of(file), StandardCharsets.UTF_8);
            Program program = new Parser(new Lexer(source).scanTokens()).parse();
            new SemanticAnalyzer().analyze(program);
            if (optimize) program = new AstOptimizer().optimize(program);
            new Interpreter(out).execute(program);
            return 0;
        } catch (IOException | InvalidPathException exception) {
            err.println("Cannot read " + file + ": " + exception.getMessage());
            return 3;
        } catch (LexicalException exception) {
            err.println(exception.getMessage());
            return 4;
        } catch (ParseException exception) {
            err.println(exception.getMessage());
            return 5;
        } catch (SemanticException exception) {
            err.println(exception.getMessage());
            return 6;
        } catch (RuntimeError exception) {
            err.println("Runtime error: " + exception.getMessage());
            return 7;
        }
    }
}

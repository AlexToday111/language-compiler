package io.github.alextoday111.projectd.semantic;

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
import io.github.alextoday111.projectd.optimization.AstOptimizer;
import io.github.alextoday111.projectd.parser.ParseException;
import io.github.alextoday111.projectd.parser.Parser;

public final class SemanticMain {
    private SemanticMain() { }

    public static void main(String[] args) {
        int code = run(args, new PrintStream(System.out, true, StandardCharsets.UTF_8),
                new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        String mode = args.length == 2 ? args[0] : "";
        if (!(args.length == 1 && !args[0].startsWith("--")) &&
                !(args.length == 2 && (mode.equals("--ast") || mode.equals("--optimize") || mode.equals("--compare")))) {
            err.println("Usage: SemanticMain [--ast | --optimize | --compare] <file.d>");
            return 2;
        }
        String file = args[args.length - 1];
        try {
            Program original = new Parser(new Lexer(Files.readString(Path.of(file), StandardCharsets.UTF_8)).scanTokens()).parse();
            new SemanticAnalyzer().analyze(original);
            AstPrinter printer = new AstPrinter();
            if (mode.equals("--ast")) out.print(printer.print(original));
            else if (mode.equals("--optimize") || mode.equals("--compare")) {
                Program optimized = new AstOptimizer().optimize(original);
                if (mode.equals("--compare")) out.print("Original AST:\n" + printer.print(original) + "Optimized AST:\n");
                out.print(printer.print(optimized));
            } else out.println("Semantic analysis successful.");
            return 0;
        } catch (LexicalException | ParseException | SemanticException exception) {
            err.println(exception.getMessage());
            return 1;
        } catch (IOException | InvalidPathException exception) {
            err.println("Cannot read " + file + ": " + exception.getMessage());
            return 3;
        }
    }
}

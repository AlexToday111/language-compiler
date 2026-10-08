import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private static final String DEFAULT_SOURCE = "var total := 12.5\nprint total";

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            runExample("built-in example", DEFAULT_SOURCE);
            return;
        }

        boolean failed = false;
        for (String fileName : args) {
            try {
                String source = Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
                runExample(fileName, source);
            } catch (IOException exception) {
                failed = true;
                System.err.println("Cannot read " + fileName + ": " + exception.getMessage());
            } catch (LexicalException exception) {
                failed = true;
                System.err.println(exception.getMessage());
            }
        }

        if (failed) {
            System.exit(1);
        }
    }

    private static void runExample(String name, String source) {
        System.out.println();
        System.out.println("=== " + name + " ===");
        System.out.println("SOURCE CODE:");
        System.out.println(source);
        System.out.println("TOKENS:");
        System.out.printf("%-8s %-17s %-24s %s%n", "POSITION", "TYPE", "LEXEME", "VALUE");
        System.out.println("----------------------------------------------------------------");

        List<Token> tokens = new Lexer(source).scanTokens();
        for (Token token : tokens) {
            System.out.printf(
                    "%-8s %-17s %-24s %s%n",
                    token.getLine() + ":" + token.getColumn(),
                    token.getType(),
                    "\"" + token.lexemeForDisplay() + "\"",
                    token.valueForDisplay()
            );
        }
    }
}

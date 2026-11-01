package io.github.alextoday111.projectd.semantic;

import java.util.Objects;
import io.github.alextoday111.projectd.ast.AstNode;

public final class SemanticDiagnostic {
    public enum Code { UNDECLARED_VARIABLE, DUPLICATE_DECLARATION, RETURN_OUTSIDE_FUNCTION, EXIT_OUTSIDE_LOOP }

    private final Code code;
    private final String message;
    private final String path;
    private final AstNode node;

    public SemanticDiagnostic(Code code, String message, String path, AstNode node) {
        this.code = Objects.requireNonNull(code);
        this.message = Objects.requireNonNull(message);
        this.path = Objects.requireNonNull(path);
        this.node = Objects.requireNonNull(node);
    }
    public Code getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public AstNode getNode() { return node; }
    @Override public String toString() { return "Semantic error [" + code + "] at " + path + ": " + message; }
}

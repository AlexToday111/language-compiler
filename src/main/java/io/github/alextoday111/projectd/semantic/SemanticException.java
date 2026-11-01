package io.github.alextoday111.projectd.semantic;

public final class SemanticException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final SemanticDiagnostic diagnostic;

    public SemanticException(SemanticDiagnostic diagnostic) {
        super(diagnostic.toString());
        this.diagnostic = diagnostic;
    }
    public SemanticDiagnostic getDiagnostic() { return diagnostic; }
}

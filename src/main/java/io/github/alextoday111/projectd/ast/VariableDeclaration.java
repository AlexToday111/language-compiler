package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class VariableDeclaration implements Statement {
    private final Identifier identifier;
    private final Expression initializer;

    public VariableDeclaration(Identifier identifier, Expression initializer) {
        this.identifier = Objects.requireNonNull(identifier);
        this.initializer = initializer;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Expression getInitializer() {
        return initializer;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return initializer == null ? List.of(identifier) : List.of(identifier, initializer);
    }
}

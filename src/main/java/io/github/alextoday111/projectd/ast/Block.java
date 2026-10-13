package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class Block implements AstNode {
    private final List<Statement> statements;

    public Block(List<Statement> statements) {
        this.statements = List.copyOf(statements);
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return statements;
    }
}

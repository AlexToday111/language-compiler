package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class VariableDeclarationGroup implements Statement {
    private final List<VariableDeclaration> declarations;

    public VariableDeclarationGroup(List<VariableDeclaration> declarations) {
        this.declarations = List.copyOf(declarations);
    }

    public List<VariableDeclaration> getDeclarations() {
        return declarations;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return declarations;
    }
}

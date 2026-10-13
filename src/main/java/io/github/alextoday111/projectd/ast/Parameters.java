package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class Parameters implements AstNode {
    private final List<Identifier> identifiers;

    public Parameters(List<Identifier> identifiers) {
        this.identifiers = List.copyOf(identifiers);
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return identifiers;
    }
}

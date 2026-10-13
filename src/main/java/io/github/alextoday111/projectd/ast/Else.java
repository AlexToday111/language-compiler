package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class Else implements AstNode {
    private final Block body;

    public Else(Block body) {
        this.body = Objects.requireNonNull(body);
    }

    public Block getBody() {
        return body;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(body);
    }
}

package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class WhileLoop implements Statement {
    private final Expression condition;
    private final Block body;

    public WhileLoop(Expression condition, Block body) {
        this.condition = Objects.requireNonNull(condition);
        this.body = Objects.requireNonNull(body);
    }

    public Expression getCondition() {
        return condition;
    }

    public Block getBody() {
        return body;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(condition, body);
    }
}

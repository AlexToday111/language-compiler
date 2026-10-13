package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class IfStatement implements Statement {
    private final Expression condition;
    private final Block thenBranch;
    private final Else elseBranch;
    private final boolean shortForm;

    public IfStatement(Expression condition, Block thenBranch, Else elseBranch, boolean shortForm) {
        this.condition = Objects.requireNonNull(condition);
        this.thenBranch = Objects.requireNonNull(thenBranch);
        this.elseBranch = elseBranch;
        this.shortForm = shortForm;
    }

    public Expression getCondition() {
        return condition;
    }

    public Block getThenBranch() {
        return thenBranch;
    }

    public Else getElseBranch() {
        return elseBranch;
    }

    public boolean isShortForm() {
        return shortForm;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return elseBranch == null ? List.of(condition, thenBranch) : List.of(condition, thenBranch, elseBranch);
    }
}

package io.github.alextoday111.projectd.ast;

import java.util.List;

public interface AstNode {
    default List<? extends AstNode> getChildren() {
        return List.of();
    }
}

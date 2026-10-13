package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public final class IntegerLiteral extends Literal<Long> {
    public IntegerLiteral(Long value, String lexeme) {
        super(Objects.requireNonNull(value), lexeme);
    }
}

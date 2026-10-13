package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public final class StringLiteral extends Literal<String> {
    public StringLiteral(String value, String lexeme) {
        super(Objects.requireNonNull(value), lexeme);
    }
}

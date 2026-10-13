package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public abstract class Literal<T> implements Expression {
    private final T value;
    private final String lexeme;

    protected Literal(T value, String lexeme) {
        this.value = value;
        this.lexeme = Objects.requireNonNull(lexeme);
    }

    public T getValue() { return value; }
    public String getLexeme() { return lexeme; }
}

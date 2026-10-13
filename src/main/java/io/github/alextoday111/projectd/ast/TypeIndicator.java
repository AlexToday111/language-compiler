package io.github.alextoday111.projectd.ast;

public enum TypeIndicator implements AstNode {
    INT("int"), REAL("real"), BOOL("bool"), STRING("string"), NONE("none"),
    ARRAY("[]"), TUPLE("{}"), FUNC("func");

    private final String spelling;

    TypeIndicator(String spelling) { this.spelling = spelling; }
    public String getSpelling() { return spelling; }
}

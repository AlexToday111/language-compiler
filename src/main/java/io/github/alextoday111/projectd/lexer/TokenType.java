package io.github.alextoday111.projectd.lexer;

public enum TokenType {
    // Keywords
    KW_VAR,
    KW_IF,
    KW_THEN,
    KW_ELSE,
    KW_END,
    KW_WHILE,
    KW_FOR,
    KW_IN,
    KW_LOOP,
    KW_EXIT,
    KW_RETURN,
    KW_PRINT,
    KW_FUNC,
    KW_IS,
    KW_INT,
    KW_REAL,
    KW_BOOL,
    KW_STRING,
    KW_NONE,
    KW_TRUE,
    KW_FALSE,
    KW_AND,
    KW_OR,
    KW_XOR,
    KW_NOT,

    // Names and literals
    IDENT,
    INTEGER,
    REAL,
    STRING,

    // Operators
    ASSIGN,       // :=
    FAT_ARROW,    // =>
    RANGE,        // ..
    PLUS,         // +
    MINUS,        // -
    STAR,         // *
    SLASH,        // /
    EQUAL,        // =
    NOT_EQUAL,    // /=
    LESS,         // <
    LESS_EQUAL,   // <=
    GREATER,      // >
    GREATER_EQUAL,// >=

    // Delimiters
    LPAREN,       // (
    RPAREN,       // )
    LBRACKET,     // [
    RBRACKET,     // ]
    LBRACE,       // {
    RBRACE,       // }
    COMMA,        // ,
    DOT,          // .
    SEMICOLON,    // ;
    NEWLINE,

    EOF
}

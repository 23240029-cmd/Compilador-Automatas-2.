package compilador;

public enum TokenType {
    // Palabras reservadas
    PROGRAM, VAR, INTEGER, REAL, BOOLEAN,
    BEGIN, END, IF, THEN, ELSE, WHILE, DO,
    READ, WRITE, OR, AND, NOT,

    // Identificadores y literales
    ID, NUM_INT, NUM_REAL,

    // Operadores
    ASSIGN,       // :=
    EQ, NEQ,      // = , <>
    LT, LTE,      // < , <=
    GT, GTE,      // > , >=
    PLUS, MINUS,  // + , -
    STAR, SLASH,  // * , /

    // Delimitadores
    SEMICOLON, COLON, COMMA, DOT,
    LPAREN, RPAREN,

    // Fin de archivo
    EOF
}
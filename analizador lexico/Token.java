package compilador;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("<%-12s, %-16s, Línea: %2d, Col: %2d>", 
            type, "'" + lexeme + "'", line, column);
    }
}
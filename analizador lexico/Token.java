package compilador;

public class Token {
    public final TokenType type;// es la categoria del token, program, id etc 
    public final String lexeme;// l texto exacto como fue escrito
    public final int line;// linea del archivo donde se encontro el token 
    public final int column;//columna exacta de donde se optuvo el token 

    public Token(TokenType type, String lexeme, int line, int column) {// asigna y fija los valores del token
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {// formato en como se imprime 
        return String.format("<%-12s, %-16s, Línea: %2d, Col: %2d>", 
            type, "'" + lexeme + "'", line, column);
    }
}

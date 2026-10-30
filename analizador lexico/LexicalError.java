package compilador;

public class LexicalError {
    public final String message;
    public final int line;
    public final int column;

    public LexicalError(String message, int line, int column) {
        this.message = message;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("[Error Léxico] Línea %2d, Col %2d: %s", line, column, message);
    }
}
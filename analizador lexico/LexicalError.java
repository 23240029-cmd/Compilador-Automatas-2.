package compilador;

public class LexicalError {
    public final String message;// mensaje de "fallo tal lao""
    public final int line;// linea donde sucuiedo
    public final int column;//columna donde ourrio el error 

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

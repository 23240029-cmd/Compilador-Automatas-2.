package compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source; //almacena el codigo
    private final SymbolTable symbolTable;// tabla de simbolos 
    private final List<LexicalError> errors = new ArrayList<>();//lista de los errores acumulados 

    private int cursor = 0;// puntero de la posicion exacta
    private int line = 1;//rastrea la linea actual 
    private int lineStart = 0;// posicion del curson donde comienza 

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();//mapeo de las palabras reservadas 

    static {//estan colocadas en en un bloque estatico 
        KEYWORDS.put("PROGRAM", TokenType.PROGRAM);
        KEYWORDS.put("VAR", TokenType.VAR);
        KEYWORDS.put("INTEGER", TokenType.INTEGER);
        KEYWORDS.put("REAL", TokenType.REAL);
        KEYWORDS.put("BOOLEAN", TokenType.BOOLEAN);
        KEYWORDS.put("BEGIN", TokenType.BEGIN);
        KEYWORDS.put("END", TokenType.END);
        KEYWORDS.put("IF", TokenType.IF);
        KEYWORDS.put("THEN", TokenType.THEN);
        KEYWORDS.put("ELSE", TokenType.ELSE);
        KEYWORDS.put("WHILE", TokenType.WHILE);
        KEYWORDS.put("DO", TokenType.DO);
        KEYWORDS.put("READ", TokenType.READ);
        KEYWORDS.put("WRITE", TokenType.WRITE);
        KEYWORDS.put("OR", TokenType.OR);
        KEYWORDS.put("AND", TokenType.AND);
        KEYWORDS.put("NOT", TokenType.NOT);
    }

    public Lexer(String source, SymbolTable symbolTable) { //recibe el texto fuente y la tabla de simbolos 
        this.source = source;
        this.symbolTable = symbolTable;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>(); //crea la lista donde se guardan tokens reconocidos 

        while (!isAtEnd()) {//lee todo el texto mientras tenga caracteres que leer 
            skipWhitespaceAndComments();// salta los espacios blancos, antes de extraer el sig token
            if (isAtEnd()) break;//salta al momento de llegar al fin del archivo 

            int col = cursor - lineStart + 1; //calcula la columna actual y le suma 1 (para que avance el cursor)
            char c = peek();// mira el caracter sin consumirlo 

            // 1. Identificadores y Palabras Reservadas
            if (Character.isLetter(c)) {
                tokens.add(scanIdentifier(line, col));
            }
            // 2. Números (Enteros y Reales)
            else if (Character.isDigit(c)) {
                Token numToken = scanNumber(line, col);
                if (numToken != null) {
                    tokens.add(numToken);
                }
            }
            // 3. Delimitadores, Operadores y Caracteres inválidos
            else {
                Token symbolToken = scanSymbol(line, col);
                if (symbolToken != null) {
                    tokens.add(symbolToken);
                }
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line, cursor - lineStart + 1));
        return tokens;// añade el token de fon de archivo 
    }

    private Token scanIdentifier(int startLine, int startCol) {
        int start = cursor;//posicion de inicial del lexema
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();//consume todo los caracteres mientaras tengan el todas las letras, num y _
        }

        String lexeme = source.substring(start, cursor);//extrae la subcadena leida 
        TokenType type = KEYWORDS.get(lexeme.toUpperCase());//busca las mayusculas de las palabras claves 

        if (type != null) {//retoma existe, toma un token de esa palabra reservada 
            return new Token(type, lexeme, startLine, startCol);
        } else {//si no existe en el, es un identificador de usuario
            if (symbolTable != null) {//lo colaca en la tabla de simbolos 
                symbolTable.insert(lexeme); 
            }
            return new Token(TokenType.ID, lexeme, startLine, startCol);//si no existe lo coloca en la tabla de simbolos 
        }
    }

    private Token scanNumber(int startLine, int startCol) {
        int start = cursor;// esta parte hace que miren si hay un digito  
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();//consume el digito y mueve el cursor 
        }

        // Revisar si viene parte decimal
        if (!isAtEnd() && peek() == '.') {
            // Caso número real válido: dígito inmediatamente tras el punto
            if (cursor + 1 < source.length() && Character.isDigit(source.charAt(cursor + 1))) {
                advance(); // Consumir '.'
                while (!isAtEnd() && Character.isDigit(peek())) {
                    advance();
                }
                return new Token(TokenType.NUM_REAL, source.substring(start, cursor), startLine, startCol);
            } 
            // Caso mal formado: letras u otro punto pegados al punto decimal (ej. 12.abc, 12..5)
            else if (cursor + 1 < source.length() && (Character.isLetter(source.charAt(cursor + 1)) || source.charAt(cursor + 1) == '.')) {
                advance(); // Consumir '.'
                while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '.')) {
                    advance(); // Consumir el residuo mal formado completo
                }
                errors.add(new LexicalError("Número real mal formado tras el punto decimal: '" + 
                    source.substring(start, cursor) + "'", startLine, startCol));
                return null;
            }
            // Si el '.' está seguido de espacio, delimitador o fin de archivo (ej. "10." al final),
            // no consumimos el '.' aquí; se retorna el entero y el '.' se procesará como delimitador DOT.
        }

        return new Token(TokenType.NUM_INT, source.substring(start, cursor), startLine, startCol);// entrega un numero entero
    }

    private Token scanSymbol(int startLine, int startCol) {
        char c = advance();
        switch (c) {
            case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startCol);
            case ',': return new Token(TokenType.COMMA, ",", startLine, startCol);
            case '.': return new Token(TokenType.DOT, ".", startLine, startCol);
            case '(': return new Token(TokenType.LPAREN, "(", startLine, startCol);
            case ')': return new Token(TokenType.RPAREN, ")", startLine, startCol);
            case '+': return new Token(TokenType.PLUS, "+", startLine, startCol);
            case '-': return new Token(TokenType.MINUS, "-", startLine, startCol);
            case '*': return new Token(TokenType.STAR, "*", startLine, startCol);
            case '/': return new Token(TokenType.SLASH, "/", startLine, startCol);
            case '=': return new Token(TokenType.EQ, "=", startLine, startCol);

            case ':':
                if (match('=')) return new Token(TokenType.ASSIGN, ":=", startLine, startCol);
                return new Token(TokenType.COLON, ":", startLine, startCol);

            case '<':
                if (match('=')) return new Token(TokenType.LTE, "<=", startLine, startCol);
                if (match('>')) return new Token(TokenType.NEQ, "<>", startLine, startCol);
                return new Token(TokenType.LT, "<", startLine, startCol);

            case '>':
                if (match('=')) return new Token(TokenType.GTE, ">=", startLine, startCol);
                return new Token(TokenType.GT, ">", startLine, startCol);

            default:
                errors.add(new LexicalError("Carácter inválido no reconocido: '" + c + "'", startLine, startCol));
                return null;
        }
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {// si encientra esto lo ignora 
                advance();
            } else if (c == '\n') {// aqui avanza y incrementa la linea 
                advance();
                line++;
                lineStart = cursor;// reinicia el cursor
            } else if (c == '{') {// aqui guarda y consume el texto que esta dentro de las llaves 
                int commentLine = line;
                int commentCol = cursor - lineStart + 1;
                advance(); // Consumir '{'

                while (!isAtEnd() && peek() != '}') {//lee todo hasta que encuentre la llave que cierra 
                    if (peek() == '\n') {
                        advance();
                        line++;
                        lineStart = cursor;
                    } else {
                        advance();
                    }
                }

                if (isAtEnd()) {
                    errors.add(new LexicalError("Comentario abierto con '{' no fue cerrado antes de fin de archivo", 
                        commentLine, commentCol));
                } else {
                    advance(); // Consumir '}'
                }
            } else {
                break;
            }
        }
    }

    private boolean match(char expected) {//verifica que el caracter coincida con los operadoes 
        if (isAtEnd() || source.charAt(cursor) != expected) return false;// avanza y retoma true o false dependiendo 
        cursor++;
        return true;
    }

    private char advance() {//toma el caracter y avanza a la derecha 
        return source.charAt(cursor++);
    }

    private char peek() {// mira si el cursor esta en el caracter actual, sin avanzar 
        return source.charAt(cursor);
    }

    private boolean isAtEnd() {// da true si el cursor llego al tamaño total del texto
        return cursor >= source.length();
    }

    public List<LexicalError> getErrors() {//devuelve la lista acumulada de errores lexicos 
        return errors;
    }
}

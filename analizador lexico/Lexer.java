package compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final SymbolTable symbolTable;
    private final List<LexicalError> errors = new ArrayList<>();

    private int cursor = 0;
    private int line = 1;
    private int lineStart = 0;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
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

    public Lexer(String source, SymbolTable symbolTable) {
        this.source = source;
        this.symbolTable = symbolTable;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespaceAndComments();
            if (isAtEnd()) break;

            int col = cursor - lineStart + 1;
            char c = peek();

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
        return tokens;
    }

    private Token scanIdentifier(int startLine, int startCol) {
        int start = cursor;
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();
        }

        String lexeme = source.substring(start, cursor);
        TokenType type = KEYWORDS.get(lexeme.toUpperCase());

        if (type != null) {
            return new Token(type, lexeme, startLine, startCol);
        } else {
            symbolTable.insert(lexeme);
            return new Token(TokenType.ID, lexeme, startLine, startCol);
        }
    }

    private Token scanNumber(int startLine, int startCol) {
        int start = cursor;
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }

        // Revisar si viene parte decimal
        if (!isAtEnd() && peek() == '.') {
            if (cursor + 1 < source.length() && Character.isDigit(source.charAt(cursor + 1))) {
                advance(); // Consumir '.'
                while (!isAtEnd() && Character.isDigit(peek())) {
                    advance();
                }
                return new Token(TokenType.NUM_REAL, source.substring(start, cursor), startLine, startCol);
            } else if (cursor + 1 < source.length() && (Character.isLetter(source.charAt(cursor + 1)) || source.charAt(cursor + 1) == '.')) {
                advance();
                errors.add(new LexicalError("Número real mal formado tras el punto decimal", startLine, startCol));
                return null;
            }
        }

        return new Token(TokenType.NUM_INT, source.substring(start, cursor), startLine, startCol);
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
                // Modo pánico léxico: reporta el error y sigue escaneando
                errors.add(new LexicalError("Carácter inválido no reconocido: '" + c + "'", startLine, startCol));
                return null;
        }
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                line++;
                advance();
                lineStart = cursor;
            } else if (c == '{') {
                int commentLine = line;
                int commentCol = cursor - lineStart + 1;
                advance(); // Consumir '{'

                while (!isAtEnd() && peek() != '}') {
                    if (peek() == '\n') {
                        line++;
                        advance();
                        lineStart = cursor;
                    } else {
                        advance();
                    }
                }

                if (isAtEnd()) {
                    errors.add(new LexicalError("Comentario abierto con '{' no fue cerrado antes de fin de archivo", commentLine, commentCol));
                } else {
                    advance(); // Consumir '}'
                }
            } else {
                break;
            }
        }
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(cursor) != expected) return false;
        cursor++;
        return true;
    }

    private char advance() {
        return source.charAt(cursor++);
    }

    private char peek() {
        return source.charAt(cursor);
    }

    private boolean isAtEnd() {
        return cursor >= source.length();
    }

    public List<LexicalError> getErrors() {
        return errors;
    }
}
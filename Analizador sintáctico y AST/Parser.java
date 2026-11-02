import java.util.List;
import java.util.ArrayList;

// ==========================================
// ESTRUCTURA DEL TOKEN (Recibido del analisis lexico)
// ==========================================
class Token {
    private String type;   // Tipo de token (Ej: "IDENTIFICADOR", "NUMERO", "SUMA")
    private String lexeme; // Texto literal que se leyo en el codigo fuente (Ej: "x", "5", "+")

    public Token(String type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    public String getType() { return type; }
    public String getLexeme() { return lexeme; }
}

// ==========================================
// MOTOR DEL ANALIZADOR SINTACTICO (Parser de Descenso Recursivo)
// Procesa el flujo de tokens provisto por el lexico y construye el AST.
// ==========================================
public class Parser {
    private List<Token> tokens; // Lista de tokens que viene del analizador lexico
    private int current = 0;    // Apuntador que rastrea el token actual que se esta analizando

    // Constructor para inicializar el parser con la lista de tokens
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Punto de entrada principal del parser: procesa instrucciones hasta el final
    public List<ASTNode> parse() {
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(parseAssignStatement());
        }
        return statements;
    }

    // Regla gramatical para asignaciones: variable = expresion;
    // NOTA DE INTEGRACION: Aqui se pueden conectar las validaciones de la tabla de simbolos.
    private ASTNode parseAssignStatement() {
        Token idToken = consume("IDENTIFICADOR", "Se espera el nombre de una variable.");
        consume("ASIGNACION", "Se espera el operador '='.");
        ASTNode expr = parseExpression();
        consume("PUNTO_Y_COMA", "Se espera ';' al final de la instruccion.");
        
        return new AssignNode(idToken.getLexeme(), expr);
    }

    // Regla gramatical para expresiones aditivas (+ y -) respetando precedencia
    private ASTNode parseExpression() {
        ASTNode node = parseTerm();

        while (match("SUMA", "RESTA")) {
            String operator = previous().getLexeme(); 
            ASTNode right = parseTerm();              
            node = new BinaryOpNode(node, operator, right); 
        }
        return node;
    }

    // Regla gramatical para expresiones multiplicativas (* y /) con mayor precedencia
    private ASTNode parseTerm() {
        ASTNode node = parseFactor();

        while (match("MULT", "DIV")) {
            String operator = previous().getLexeme();
            ASTNode right = parseFactor();
            node = new BinaryOpNode(node, operator, right);
        }
        return node;
    }

    // Regla gramatical para elementos base: numeros, identificadores o parentesis
    private ASTNode parseFactor() {
        // Si encontramos un numero o identificador, creamos un nodo hoja
        if (match("NUMERO", "IDENTIFICADOR")) {
            return new LiteralNode(previous().getLexeme());
        }
        // Manejo de parentesis para agrupacion explicita: ( expresion )
        if (match("PARENTESIS_IZQ")) {
            ASTNode expr = parseExpression(); 
            consume("PARENTESIS_DER", "Se espera ')' despues de la expresion."); 
            return expr;
        }
        throw new RuntimeException("Error sintactico en el token: " + peek().getLexeme());
    }

    // Verifica si el token actual coincide con algun tipo y avanza si es asi
    private boolean match(String... types) {
        for (String type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Consume el tipo de token esperado o lanza un error si no coincide
    private Token consume(String type, String errorMessage) {
        if (check(type)) return advance();
        throw new RuntimeException("Error sintactico: " + errorMessage);
    }

    // Revisa si el token actual coincide con el tipo especificado sin consumirlo
    private boolean check(String type) {
        if (isAtEnd()) return false;
        return peek().getType().equals(type);
    }

    // Consume el token actual y mueve el apuntador hacia adelante
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Verifica si ya llegamos al final de los tokens (EOF)
    private boolean isAtEnd() {
        return peek().getType().equals("EOF");
    }

    // Regresa el token actual que estamos viendo sin mover el apuntador
    private Token peek() {
        return tokens.get(current);
    }

    // Regresa el token que acabamos de procesar (el anterior)
    private Token previous() {
        return tokens.get(current - 1);
    }

    // ==========================================
    // METODO MAIN PARA PRUEBAS E IMPRESION DEL ARBOL
    // Simula la entrada de tokens e imprime la estructura jerarquica del AST
    // ==========================================
    public static void main(String[] args) {
        // Simulamos los tokens que pasara el componente de analisis lexico
        List<Token> tokensPrueba = new ArrayList<>();
        tokensPrueba.add(new Token("IDENTIFICADOR", "x"));
        tokensPrueba.add(new Token("ASIGNACION", "="));
        tokensPrueba.add(new Token("NUMERO", "10"));
        tokensPrueba.add(new Token("SUMA", "+"));
        tokensPrueba.add(new Token("NUMERO", "5"));
        tokensPrueba.add(new Token("PUNTO_Y_COMA", ";"));
        tokensPrueba.add(new Token("EOF", ""));

        try {
            Parser miParser = new Parser(tokensPrueba);
            List<ASTNode> arbol = miParser.parse();
            
            System.out.println("=== EXITO! ARBOL SINTACTICO GENERADO ===\n");
            for (ASTNode nodo : arbol) {
                System.out.println(nodo.toStringTree(""));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
package compilador;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program → PROGRAM identifier ; block .
    public ProgramNode parse() {
        Token progTok = consume(TokenType.PROGRAM, "Se esperaba 'PROGRAM' al inicio.");
        Token idTok = consume(TokenType.ID, "Se esperaba el nombre del programa.");
        consume(TokenType.SEMICOLON, "Se esperaba ';' después del nombre del programa.");

        List<VarDeclNode> declarations = declarationPart();
        BlockNode mainBlock = statementPart();

        consume(TokenType.DOT, "Se esperaba '.' al final del programa.");
        consume(TokenType.EOF, "Se encontraron tokens inesperados después de 'END.'.");

        return new ProgramNode(idTok.lexeme, declarations, mainBlock, progTok.line, progTok.column);
    }

    // declaration_part → VAR declaration_list | ε
    private List<VarDeclNode> declarationPart() {
        List<VarDeclNode> declarations = new ArrayList<>();
        if (match(TokenType.VAR)) {
            declarations.add(parseDeclaration());
            while (match(TokenType.SEMICOLON)) {
                if (check(TokenType.ID)) {
                    declarations.add(parseDeclaration());
                } else {
                    break;
                }
            }
        }
        return declarations;
    }

    // declaration → identifier_list : type
    private VarDeclNode parseDeclaration() {
        Token firstId = consume(TokenType.ID, "Se esperaba un identificador en la declaración.");
        List<String> ids = new ArrayList<>();
        ids.add(firstId.lexeme);

        while (match(TokenType.COMMA)) {
            Token nextId = consume(TokenType.ID, "Se esperaba un identificador después de ','.");
            ids.add(nextId.lexeme);
        }

        consume(TokenType.COLON, "Se esperaba ':' antes del tipo de dato.");

        String type;
        if (match(TokenType.INTEGER, TokenType.REAL, TokenType.BOOLEAN)) {
            type = previous().type.name();
        } else {
            throw error(peek(), "Se esperaba un tipo de dato válido (INTEGER, REAL o BOOLEAN).");
        }

        return new VarDeclNode(ids, type, firstId.line, firstId.column);
    }

    // statement_part → BEGIN statement_list END
    private BlockNode statementPart() {
        Token beginTok = consume(TokenType.BEGIN, "Se esperaba 'BEGIN'.");
        List<ASTNode> statements = parseStatementList();
        consume(TokenType.END, "Se esperaba 'END'.");
        return new BlockNode(statements, beginTok.line, beginTok.column);
    }

    // statement_list → statement ; statement_list | statement
    private List<ASTNode> parseStatementList() {
        List<ASTNode> statements = new ArrayList<>();
        ASTNode first = parseStatement();
        if (first != null) statements.add(first);

        while (match(TokenType.SEMICOLON)) {
            ASTNode next = parseStatement();
            if (next != null) statements.add(next);
        }
        return statements;
    }

    // statement → assignment | if_statement | while_statement | read_statement | write_statement | compound_statement | ε
    private ASTNode parseStatement() {
        if (check(TokenType.ID)) {
            return parseAssignment();
        }
        if (match(TokenType.IF)) {
            return parseIfStatement();
        }
        if (match(TokenType.WHILE)) {
            return parseWhileStatement();
        }
        if (match(TokenType.READ)) {
            return parseReadStatement();
        }
        if (match(TokenType.WRITE)) {
            return parseWriteStatement();
        }
        if (check(TokenType.BEGIN)) {
            return statementPart();
        }
        return null; // Producción ε
    }

    // assignment → identifier := expression
    private ASTNode parseAssignment() {
        Token idTok = consume(TokenType.ID, "Se esperaba un identificador.");
        consume(TokenType.ASSIGN, "Se esperaba ':=' en la asignación.");
        ASTNode expr = parseExpression();
        return new AssignNode(idTok.lexeme, expr, idTok.line, idTok.column);
    }

    // if_statement → IF expression THEN statement opt_else
    private ASTNode parseIfStatement() {
        Token ifTok = previous();
        ASTNode condition = parseExpression();
        consume(TokenType.THEN, "Se esperaba 'THEN' después de la condición del IF.");
        ASTNode thenBranch = parseStatement();
        ASTNode elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        return new IfNode(condition, thenBranch, elseBranch, ifTok.line, ifTok.column);
    }

    // while_statement → WHILE expression DO statement
    private ASTNode parseWhileStatement() {
        Token whileTok = previous();
        ASTNode condition = parseExpression();
        consume(TokenType.DO, "Se esperaba 'DO' después de la condición del WHILE.");
        ASTNode body = parseStatement();
        return new WhileNode(condition, body, whileTok.line, whileTok.column);
    }

    // read_statement → READ ( identifier_list )
    private ASTNode parseReadStatement() {
        Token readTok = previous();
        consume(TokenType.LPAREN, "Se esperaba '(' después de READ.");
        List<String> ids = new ArrayList<>();
        ids.add(consume(TokenType.ID, "Se esperaba un identificador en READ.").lexeme);
        while (match(TokenType.COMMA)) {
            ids.add(consume(TokenType.ID, "Se esperaba un identificador después de ','.").lexeme);
        }
        consume(TokenType.RPAREN, "Se esperaba ')' al cerrar READ.");
        return new ReadNode(ids, readTok.line, readTok.column);
    }

    // write_statement → WRITE ( expression_list )
    private ASTNode parseWriteStatement() {
        Token writeTok = previous();
        consume(TokenType.LPAREN, "Se esperaba '(' después de WRITE.");
        List<ASTNode> exprs = new ArrayList<>();
        exprs.add(parseExpression());
        while (match(TokenType.COMMA)) {
            exprs.add(parseExpression());
        }
        consume(TokenType.RPAREN, "Se esperaba ')' al cerrar WRITE.");
        return new WriteNode(exprs, writeTok.line, writeTok.column);
    }

    // expression → simple_expression relop simple_expression | simple_expression
    private ASTNode parseExpression() {
        ASTNode left = parseSimpleExpression();
        if (match(TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) {
            Token op = previous();
            ASTNode right = parseSimpleExpression();
            return new BinaryOpNode(left, op.type, right, op.line, op.column);
        }
        return left;
    }

    // simple_expression → simple_expression addop term | term
    private ASTNode parseSimpleExpression() {
        ASTNode node = parseTerm();
        while (match(TokenType.PLUS, TokenType.MINUS, TokenType.OR)) {
            Token op = previous();
            ASTNode right = parseTerm();
            node = new BinaryOpNode(node, op.type, right, op.line, op.column);
        }
        return node;
    }

    // term → term mulop factor | factor
    private ASTNode parseTerm() {
        ASTNode node = parseFactor();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.AND)) {
            Token op = previous();
            ASTNode right = parseFactor();
            node = new BinaryOpNode(node, op.type, right, op.line, op.column);
        }
        return node;
    }

    // factor → identifier | number | ( expression ) | NOT factor
    private ASTNode parseFactor() {
        if (match(TokenType.ID)) {
            Token tok = previous();
            return new VarNode(tok.lexeme, tok.line, tok.column);
        }
        if (match(TokenType.NUM_INT)) {
            Token tok = previous();
            return new LiteralNode(tok.lexeme, "INTEGER", tok.line, tok.column);
        }
        if (match(TokenType.NUM_REAL)) {
            Token tok = previous();
            return new LiteralNode(tok.lexeme, "REAL", tok.line, tok.column);
        }
        if (match(TokenType.LPAREN)) {
            ASTNode expr = parseExpression();
            consume(TokenType.RPAREN, "Se esperaba ')' después de la expresión.");
            return expr;
        }
        if (match(TokenType.NOT)) {
            Token op = previous();
            ASTNode operand = parseFactor();
            return new UnaryOpNode(op.type, operand, op.line, op.column);
        }
        throw error(peek(), "Factor inesperado en la expresión: '" + peek().lexeme + "'");
    }

    // Métodos auxiliares
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message + " (Se encontró: '" + peek().lexeme + "')");
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException(String.format("[Error Sintáctico] Línea %2d, Col %2d: %s", token.line, token.column, message));
    }
}

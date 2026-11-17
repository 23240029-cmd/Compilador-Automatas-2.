package compilador;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SemanticAnalyzer {

    private static final String INTEGER = "INTEGER";
    private static final String REAL = "REAL";
    private static final String BOOLEAN = "BOOLEAN";
    private static final String ERROR = "ERROR";       // tipo especial cuando algo ya fallo
    private static final String PROGRAM = "PROGRAM";   // el nombre del programa no es una variable

    private final SymbolTable symbolTable;
    private final AddressTable addressTable = new AddressTable();
    private final Deque<String> pila = new ArrayDeque<>(); // pila semantica (guarda tipos)
    private final List<String> errors = new ArrayList<>();

    private boolean trace = false; // si es true, imprime lo que pasa en la pila

    public SemanticAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<String> getErrors() { return errors; }
    public AddressTable getAddressTable() { return addressTable; }

    // Activa la impresion paso a paso de la pila (sirve para explicarlo)
    public void setTrace(boolean trace) { this.trace = trace; }

    // Los errores llevan linea y columna del nodo donde ocurren
    private void error(ASTNode where, String message) {
        errors.add(String.format("[Error Semántico] Línea %2d, Col %2d: %s",
                where.line, where.column, message));
    }

    public void analyze(ProgramNode program) {
        // El nombre del programa queda reservado en la tabla de simbolos
        SymbolTable.Symbol prog = symbolTable.lookup(program.programName);
        if (prog == null) prog = symbolTable.insert(program.programName);
        if (prog.type == null) prog.type = PROGRAM;

        for (VarDeclNode decl : program.declarations) {
            checkDeclaration(decl);
        }
        checkStatement(program.mainBlock);
    }

    // 1. Declaraciones
    private void checkDeclaration(VarDeclNode decl) {
        for (String name : decl.identifiers) {
            SymbolTable.Symbol symbol = symbolTable.lookup(name);
            if (symbol == null) {
                symbol = symbolTable.insert(name);
            }
            if (PROGRAM.equals(symbol.type)) {
                error(decl, "'" + name + "' ya se usa como nombre del programa");
            } else if (symbol.type != null) {
                error(decl, "La variable '" + name + "' ya fue declarada como " + symbol.type);
            } else {
                symbol.type = decl.dataType;                    // llena la tabla de simbolos
                addressTable.add(name, decl.dataType);          // le asigna direccion
            }
        }
        decl.type = decl.dataType;
    }

    // 2. Sentencias
    private void checkStatement(ASTNode node) {
        if (node == null || node instanceof EmptyNode) {
            return; // sentencia vacia: nada que revisar

        } else if (node instanceof AssignNode) {
            checkAssign((AssignNode) node);

        } else if (node instanceof IfNode) {
            IfNode n = (IfNode) node;
            checkCondition(n.condition, "IF");
            checkStatement(n.thenBranch);
            checkStatement(n.elseBranch);

        } else if (node instanceof WhileNode) {
            WhileNode n = (WhileNode) node;
            checkCondition(n.condition, "WHILE");
            checkStatement(n.body);

        } else if (node instanceof BlockNode) {
            for (ASTNode s : ((BlockNode) node).statements) {
                checkStatement(s);
            }

        } else if (node instanceof ReadNode) {
            ReadNode r = (ReadNode) node;
            for (String name : r.identifiers) {
                String t = getVariableType(name, r);
                if (t.equals(BOOLEAN)) {
                    error(r, "READ no puede leer la variable BOOLEAN '" + name + "'");
                }
            }

        } else if (node instanceof WriteNode) {
            for (ASTNode e : ((WriteNode) node).expressions) {
                getType(e); // solo verifica que la expresion sea valida
            }
        }
    }

    private void checkAssign(AssignNode node) {
        String varType = getVariableType(node.variableName, node);
        String exprType = getType(node.expression);
        node.type = varType.equals(ERROR) ? null : varType;

        if (varType.equals(ERROR) || exprType.equals(ERROR)) return; // ya se reporto

        if (!isCompatible(varType, exprType)) {
            error(node, "No se puede asignar un valor " + exprType + " a la variable '"
                  + node.variableName + "' de tipo " + varType);
        } else if (varType.equals(REAL) && exprType.equals(INTEGER)) {
            // conversion implicita: x_real := entero
            node.expression = new ConvertNode(node.expression, REAL);
        }
    }

    private void checkCondition(ASTNode condition, String keyword) {
        String t = getType(condition);
        if (!t.equals(ERROR) && !t.equals(BOOLEAN)) {
            error(condition, "La condición del " + keyword + " debe ser BOOLEAN pero es " + t);
        }
    }

    // 3. Tipos de las expresiones (con pila semantica) 
    // Regresa el tipo de una expresion: INTEGER, REAL, BOOLEAN o ERROR
    private String getType(ASTNode node) {
        pila.clear();
        if (trace) System.out.println("\n[pila] Nueva expresión");
        recorrer(node);
        return sacar();
    }

    // Recorrido en postorden: primero los hijos, luego el operador.
    // Asi, cuando llega el operador, los tipos de sus operandos ya estan en la pila.
    private void recorrer(ASTNode node) {
        if (node instanceof LiteralNode) {
            LiteralNode lit = (LiteralNode) node;
            lit.type = lit.literalType;
            meter(lit.type);

        } else if (node instanceof VarNode) {
            VarNode v = (VarNode) node;
            String t = getVariableType(v.name, v);
            v.type = t.equals(ERROR) ? null : t;
            meter(t);

        } else if (node instanceof UnaryOpNode) {
            UnaryOpNode u = (UnaryOpNode) node;
            recorrer(u.operand);
            String t = sacar();
            String r = aplicarUnario(u, t);
            u.type = r.equals(ERROR) ? null : r;
            meter(r);

        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) node;
            recorrer(b.left);
            recorrer(b.right);
            String derecho = sacar();   // el ultimo que entro es el derecho
            String izquierdo = sacar();
            String r = aplicarBinario(b, izquierdo, derecho);
            b.type = r.equals(ERROR) ? null : r;
            if (!r.equals(ERROR)) insertarConversiones(b, izquierdo, derecho);
            meter(r);

        } else {
            meter(ERROR);
        }
    }

    // Si se mezclan INTEGER y REAL (o hay una division), los INTEGER se convierten a REAL
    private void insertarConversiones(BinaryOpNode b, String izq, String der) {
        boolean aReal = b.operator == TokenType.SLASH || izq.equals(REAL) || der.equals(REAL);
        if (!aReal) return;
        if (izq.equals(INTEGER)) b.left = new ConvertNode(b.left, REAL);
        if (der.equals(INTEGER)) b.right = new ConvertNode(b.right, REAL);
    }

    private void meter(String tipo) {
        pila.push(tipo);
        if (trace) System.out.println("[pila] push " + tipo + "   -> " + pila);
    }

    private String sacar() {
        String tipo = pila.pop();
        if (trace) System.out.println("[pila] pop  " + tipo + "   -> " + pila);
        return tipo;
    }

    // Busca la variable en la tabla de simbolos y regresa su tipo
    private String getVariableType(String name, ASTNode where) {
        SymbolTable.Symbol symbol = symbolTable.lookup(name);
        if (symbol == null || symbol.type == null) {
            error(where, "La variable '" + name + "' no ha sido declarada");
            return ERROR;
        }
        if (PROGRAM.equals(symbol.type)) {
            error(where, "'" + name + "' es el nombre del programa, no una variable");
            return ERROR;
        }
        return symbol.type;
    }

    private String aplicarUnario(UnaryOpNode node, String t) {
        if (t.equals(ERROR)) return ERROR;
        if (!t.equals(BOOLEAN)) {
            error(node, "El operador " + node.operator + " necesita un operando BOOLEAN pero recibió " + t);
            return ERROR;
        }
        return BOOLEAN;
    }

    private String aplicarBinario(BinaryOpNode node, String left, String right) {
        if (left.equals(ERROR) || right.equals(ERROR)) return ERROR;

        switch (node.operator) {
            case PLUS:
            case MINUS:
            case STAR:
                if (!isNumeric(left) || !isNumeric(right)) {
                    return operatorError(node, left, right);
                }
                // Si alguno es REAL, el resultado es REAL
                return (left.equals(REAL) || right.equals(REAL)) ? REAL : INTEGER;

            case SLASH:
                if (!isNumeric(left) || !isNumeric(right)) {
                    return operatorError(node, left, right);
                }
                return REAL; // la division siempre da REAL

            case AND:
            case OR:
                if (!left.equals(BOOLEAN) || !right.equals(BOOLEAN)) {
                    return operatorError(node, left, right);
                }
                return BOOLEAN;

            case LT:
            case LTE:
            case GT:
            case GTE:
                if (!isNumeric(left) || !isNumeric(right)) {
                    return operatorError(node, left, right);
                }
                return BOOLEAN;

            case EQ:
            case NEQ:
                // Se pueden comparar dos numeros o dos booleanos
                if ((isNumeric(left) && isNumeric(right)) || (left.equals(BOOLEAN) && right.equals(BOOLEAN))) {
                    return BOOLEAN;
                }
                return operatorError(node, left, right);

            default:
                error(node, "Operador desconocido: " + node.operator);
                return ERROR;
        }
    }

    private String operatorError(BinaryOpNode node, String left, String right) {
        error(node, "El operador " + symbolOf(node.operator) + " no se puede usar entre " + left + " y " + right);
        return ERROR;
    }

    private static String symbolOf(TokenType t) {
        switch (t) {
            case PLUS: return "'+'";   case MINUS: return "'-'";
            case STAR: return "'*'";   case SLASH: return "'/'";
            case EQ:   return "'='";   case NEQ:   return "'<>'";
            case LT:   return "'<'";   case LTE:   return "'<='";
            case GT:   return "'>'";   case GTE:   return "'>='";
            default:   return t.name();
        }
    }

    // Reglas de tipos 
    private boolean isNumeric(String type) {
        return type.equals(INTEGER) || type.equals(REAL);
    }

    // Se puede guardar un valor de tipo "value" en una variable de tipo "target"?
    private boolean isCompatible(String target, String value) {
        if (target.equals(value)) return true;
        return target.equals(REAL) && value.equals(INTEGER); // INTEGER cabe en REAL
    }
}

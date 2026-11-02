// ==========================================
// DEFINICION DE LOS NODOS DEL AST (Arbol de Sintaxis Abstracta)
// ==========================================

public abstract class ASTNode {
    // Metodo abstracto para imprimir la estructura jerarquica del arbol
    public abstract String toStringTree(String indent);
}

// Nodo que representa una asignacion de variable (Ejemplo: x = 10 + 5;)
class AssignNode extends ASTNode {
    private String variableName; 
    private ASTNode expression;  

    public AssignNode(String variableName, ASTNode expression) {
        this.variableName = variableName;
        this.expression = expression;
    }

    public String getVariableName() { return variableName; }
    public ASTNode getExpression() { return expression; }

    @Override
    public String toStringTree(String indent) {
        return indent + "AssignNode: " + variableName + "\n" + expression.toStringTree(indent + "  ");
    }
}

// Nodo que representa una operacion matematica binaria de dos lados (Ejemplo: a + b)
class BinaryOpNode extends ASTNode {
    private ASTNode left;     
    private String operator;  
    private ASTNode right;    

    public BinaryOpNode(ASTNode left, String operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ASTNode getLeft() { return left; }
    public String getOperator() { return operator; }
    public ASTNode getRight() { return right; }

    @Override
    public String toStringTree(String indent) {
        return indent + "BinaryOpNode: " + operator + "\n" +
               left.toStringTree(indent + "  ") + "\n" +
               right.toStringTree(indent + "  ");
    }
}

// Nodo hoja o terminal para valores directos (ej. numeros o identificadores)
class LiteralNode extends ASTNode {
    private String value; 

    public LiteralNode(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public String toStringTree(String indent) {
        return indent + "LiteralNode: " + value;
    }
}
package compilador;

import java.util.List;

// Clase base para todos los nodos del Árbol de Sintaxis Abstracta
public abstract class ASTNode {
    public final int line;
    public final int column;

    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public abstract String toStringTree(String indent);
}

// Nodo raíz: PROGRAM id; declarations block.
class ProgramNode extends ASTNode {
    public final String programName;
    public final List<VarDeclNode> declarations;
    public final BlockNode mainBlock;

    public ProgramNode(String programName, List<VarDeclNode> declarations, BlockNode mainBlock, int line, int column) {
        super(line, column);
        this.programName = programName;
        this.declarations = declarations;
        this.mainBlock = mainBlock;
    }

    @Override
    public String toStringTree(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("ProgramNode: ").append(programName).append("\n");
        sb.append(indent).append("  Declarations:\n");
        for (VarDeclNode decl : declarations) {
            sb.append(decl.toStringTree(indent + "    ")).append("\n");
        }
        sb.append(mainBlock.toStringTree(indent + "  "));
        return sb.toString();
    }
}

// Nodo de declaración: x, y : INTEGER
class VarDeclNode extends ASTNode {
    public final List<String> identifiers;
    public final String dataType;

    public VarDeclNode(List<String> identifiers, String dataType, int line, int column) {
        super(line, column);
        this.identifiers = identifiers;
        this.dataType = dataType;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "VarDeclNode: " + identifiers + " : " + dataType;
    }
}

// Nodo de bloque compuesto: BEGIN ... END
class BlockNode extends ASTNode {
    public final List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements, int line, int column) {
        super(line, column);
        this.statements = statements;
    }

    @Override
    public String toStringTree(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("BlockNode:\n");
        for (ASTNode stmt : statements) {
            sb.append(stmt.toStringTree(indent + "  ")).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}

// Nodo de asignación: x := expr
class AssignNode extends ASTNode {
    public final String variableName;
    public final ASTNode expression;

    public AssignNode(String variableName, ASTNode expression, int line, int column) {
        super(line, column);
        this.variableName = variableName;
        this.expression = expression;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "AssignNode: " + variableName + " :=\n" + expression.toStringTree(indent + "  ");
    }
}

// Nodo IF-THEN-ELSE
class IfNode extends ASTNode {
    public final ASTNode condition;
    public final ASTNode thenBranch;
    public final ASTNode elseBranch;

    public IfNode(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public String toStringTree(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("IfNode:\n");
        sb.append(indent).append("  Condition:\n").append(condition.toStringTree(indent + "    ")).append("\n");
        sb.append(indent).append("  Then:\n").append(thenBranch.toStringTree(indent + "    "));
        if (elseBranch != null) {
            sb.append("\n").append(indent).append("  Else:\n").append(elseBranch.toStringTree(indent + "    "));
        }
        return sb.toString();
    }
}

// Nodo WHILE-DO
class WhileNode extends ASTNode {
    public final ASTNode condition;
    public final ASTNode body;

    public WhileNode(ASTNode condition, ASTNode body, int line, int column) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "WhileNode:\n" +
               indent + "  Condition:\n" + condition.toStringTree(indent + "    ") + "\n" +
               indent + "  Body:\n" + body.toStringTree(indent + "    ");
    }
}

// Nodo READ(x, y)
class ReadNode extends ASTNode {
    public final List<String> identifiers;

    public ReadNode(List<String> identifiers, int line, int column) {
        super(line, column);
        this.identifiers = identifiers;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "ReadNode: " + identifiers;
    }
}

// Nodo WRITE(expr1, expr2)
class WriteNode extends ASTNode {
    public final List<ASTNode> expressions;

    public WriteNode(List<ASTNode> expressions, int line, int column) {
        super(line, column);
        this.expressions = expressions;
    }

    @Override
    public String toStringTree(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("WriteNode:\n");
        for (ASTNode expr : expressions) {
            sb.append(expr.toStringTree(indent + "  ")).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}

// Nodo de operación binaria (+, -, *, /, =, <>, <, <=, >, >=, AND, OR)
class BinaryOpNode extends ASTNode {
    public final ASTNode left;
    public final TokenType operator;
    public final ASTNode right;

    public BinaryOpNode(ASTNode left, TokenType operator, ASTNode right, int line, int column) {
        super(line, column);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "BinaryOpNode (" + operator + "):\n" +
               left.toStringTree(indent + "  ") + "\n" +
               right.toStringTree(indent + "  ");
    }
}

// Nodo de operación unaria (NOT factor)
class UnaryOpNode extends ASTNode {
    public final TokenType operator;
    public final ASTNode operand;

    public UnaryOpNode(TokenType operator, ASTNode operand, int line, int column) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "UnaryOpNode (" + operator + "):\n" + operand.toStringTree(indent + "  ");
    }
}

// Nodo de variable usada en una expresión
class VarNode extends ASTNode {
    public final String name;

    public VarNode(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "VarNode: " + name;
    }
}

// Nodo literal constante (NUM_INT o NUM_REAL)
class LiteralNode extends ASTNode {
    public final String value;
    public final String literalType;

    public LiteralNode(String value, String literalType, int line, int column) {
        super(line, column);
        this.value = value;
        this.literalType = literalType;
    }

    @Override
    public String toStringTree(String indent) {
        return indent + "LiteralNode (" + literalType + "): " + value;
    }
}

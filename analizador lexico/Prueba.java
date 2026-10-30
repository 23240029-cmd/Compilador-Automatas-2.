package compilador;

import java.util.List;

public class Prueba {
    public static void main(String[] args) {
        String testProgram = 
            "PROGRAM TestCompilador;\n" +
            "VAR x, y : INTEGER;\n" +
            "    val : REAL;\n" +
            "BEGIN\n" +
            "    { Comentario valido }\n" +
            "    READ(x);\n" +
            "    y := x + 10;\n" +
            "    IF y >= 20 THEN\n" +
            "        WRITE(y)\n" +
            "    ELSE\n" +
            "        val := 15.5;\n" +
            "    $ { Simbolo invalido para probar el detector de errores }\n" +
            "END.";

        SymbolTable symbolTable = new SymbolTable();
        Lexer lexer = new Lexer(testProgram, symbolTable);

        List<Token> tokens = lexer.tokenize();

        System.out.println("LISTA DE TOKENS ");
        for (Token t : tokens) {
            System.out.println(t);
        }

        List<LexicalError> errors = lexer.getErrors();
        if (!errors.isEmpty()) {
            System.out.println("\nERRORES LÉXICOS");
            for (LexicalError err : errors) {
                System.out.println(err);
            }
        } else {
            System.out.println("\nSin errores léxicos.");
        }

        symbolTable.printTable();
    }
}
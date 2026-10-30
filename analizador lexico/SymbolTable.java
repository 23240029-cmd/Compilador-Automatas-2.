package compilador;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    public static class Symbol {
        public final String name;
        public String type; // Se llenará en la etapa semántica (INTEGER, REAL, BOOLEAN)

        public Symbol(String name) {
            this.name = name;
            this.type = null;
        }

        @Override
        public String toString() {
            return String.format("%-20s | Tipo: %s", name, (type != null ? type : "PENDIENTE"));
        }
    }

    private final Map<String, Symbol> table = new LinkedHashMap<>();

    public Symbol insert(String name) {
        String key = name.toLowerCase();
        if (!table.containsKey(key)) {
            table.put(key, new Symbol(name));
        }
        return table.get(key);
    }

    public Symbol lookup(String name) {
        return table.get(name.toLowerCase());
    }

    public Map<String, Symbol> getAllSymbols() {
        return Collections.unmodifiableMap(table);
    }

    public void printTable() {
        System.out.println("\n TABLA DE SÍMBOLOS");
        System.out.printf("%-20s | %s\n", "Identificador", "Tipo Semántico");
        if (table.isEmpty()) {
            System.out.println("(Vacía)");
        } else {
            for (Symbol s : table.values()) {
                System.out.println(s);
            }
        }
    }
}
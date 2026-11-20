package compilador;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    public static class Symbol {
        public final String name;//guarda el nombre original del identificador 
        public String type; // Se llenará en la etapa semántica (INTEGER, REAL, BOOLEAN)

        public Symbol(String name) {// crea la ficha con el nombre recibido y null
            this.name = name;// ya que el lexioco no sabe que variable es 
            this.type = null;
        }

        @Override
        public String toString() {// improme cada simbolo en la consla (pendiente por que es parte del semantico  
            return String.format("%-20s | Tipo: %s", name, (type != null ? type : "PENDIENTE"));
        }
    }

    private final Map<String, Symbol> table = new LinkedHashMap<>();// da las variables, string en minusculas para evitar duplicado y symbol info de la variable 

    public Symbol insert(String name) {
        String key = name.toLowerCase();// es para hacer las letras en minusculas (para como esten escritas den los mismo)
        if (!table.containsKey(key)) {//revisa que la variable exista en la tabla 
            table.put(key, new Symbol(name));// si existe, no la duplica 
        }
        return table.get(key);// debuelve el simbolo que es 
    }

    public Symbol lookup(String name) {
        return table.get(name.toLowerCase());// busca la tabla para convertirla a minucula , si existe retoma el symbol si no marca null
    }

    public Map<String, Symbol> getAllSymbols() {//deveulve solo la lectura del mapeo (asi ya no borran ni nada otras clase)
        return Collections.unmodifiableMap(table);
    }

    public void printTable() {// imprime la tabla 
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

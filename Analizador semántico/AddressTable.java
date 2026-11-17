package compilador;


import java.util.LinkedHashMap;
import java.util.Map;

// ==========================================
// TABLA DE DIRECCIONES
// A cada variable declarada se le asigna una direccion (desplazamiento)
// segun el tamaño de su tipo. La primera variable queda en 0 y las
// siguientes van una tras otra.
// ==========================================
public class AddressTable {

    public static class Entry {
        public final String name;
        public final String type;
        public final int size;
        public final int address;

        public Entry(String name, String type, int size, int address) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.address = address;
        }

        @Override
        public String toString() {
            return String.format("%-20s | %-8s | %6d | %9d", name, type, size, address);
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private int nextAddress = 0;

    public static int sizeOf(String type) {
        switch (type) {
            case "INTEGER": return 2;
            case "REAL":    return 4;
            case "BOOLEAN": return 1;
            default:        return 0;
        }
    }

    public Entry add(String name, String type) {
        String key = name.toLowerCase();
        if (entries.containsKey(key)) {
            return entries.get(key);
        }
        Entry entry = new Entry(name, type, sizeOf(type), nextAddress);
        nextAddress += entry.size;
        entries.put(key, entry);
        return entry;
    }

    public Entry lookup(String name) {
        return entries.get(name.toLowerCase());
    }

    public int getTotalSize() {
        return nextAddress;
    }

    public void printTable() {
        System.out.println("\n TABLA DE DIRECCIONES");
        System.out.printf("%-20s | %-8s | %6s | %9s\n", "Identificador", "Tipo", "Tamaño", "Dirección");
        if (entries.isEmpty()) {
            System.out.println("(Vacía)");
        } else {
            for (Entry e : entries.values()) {
                System.out.println(e);
            }
            System.out.println("Memoria total: " + nextAddress + " bytes");
        }
    }
}

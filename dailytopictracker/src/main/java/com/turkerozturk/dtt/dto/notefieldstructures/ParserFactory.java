package com.turkerozturk.dtt.dto.notefieldstructures;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ParserFactory {
    private static final Map<String, Supplier<NoteFieldStructure>> registry = new HashMap<>();

    public static void register(String name, Supplier<NoteFieldStructure> supplier) {
        registry.put(name, supplier);
    }

    public static NoteFieldStructure create(String className) {
        Supplier<NoteFieldStructure> supplier = registry.get(className);
        if (supplier == null) throw new IllegalArgumentException("Unknown parser: " + className);
        return supplier.get();
    }

    public static Set<String> getRegisteredNames() {
        return registry.keySet();
    }
}


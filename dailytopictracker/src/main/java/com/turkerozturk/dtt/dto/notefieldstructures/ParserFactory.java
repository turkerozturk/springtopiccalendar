package com.turkerozturk.dtt.dto.notefieldstructures;

import java.util.Map;
import java.util.function.Supplier;

public class ParserFactory {
    private static final Map<String, Supplier<NoteFieldStructure>> registry = Map.of(
            "MeasurementParser", MeasurementParser::new
            // Diger parserlar da buraya eklenebilir
    );

    public static NoteFieldStructure create(String className) {
        Supplier<NoteFieldStructure> supplier = registry.get(className);
        if (supplier == null) throw new IllegalArgumentException("Unknown parser: " + className);
        return supplier.get();
    }
}

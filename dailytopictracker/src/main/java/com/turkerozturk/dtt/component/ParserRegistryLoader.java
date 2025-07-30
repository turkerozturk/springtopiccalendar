package com.turkerozturk.dtt.component;

import com.turkerozturk.dtt.dto.notefieldstructures.NoteFieldStructure;
import jakarta.annotation.PostConstruct;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.turkerozturk.dtt.dto.notefieldstructures.ParserFactory;

@Component
public class ParserRegistryLoader {
    private final List<String> parserClassNames = new ArrayList<>();

    @PostConstruct
    public void loadParsers() {
        Reflections reflections = new Reflections("com.turkerozturk.dtt.dto.notefieldstructures");
        Set<Class<? extends NoteFieldStructure>> classes = reflections.getSubTypesOf(NoteFieldStructure.class);

        for (Class<?> clazz : classes) {
            if (!Modifier.isAbstract(clazz.getModifiers()) && hasDefaultConstructor(clazz)) {
                parserClassNames.add(clazz.getSimpleName());
                ParserFactory.register(clazz.getSimpleName(), () -> {
                    try {
                        return (NoteFieldStructure) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private boolean hasDefaultConstructor(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public List<String> getParserClassNames() {
        return parserClassNames;
    }
}


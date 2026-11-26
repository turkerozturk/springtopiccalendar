package com.turkerozturk.dtt.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class CatalogText {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private CatalogText() {}

    static String fold(String text, Locale locale) {
        if (text == null) return "";
        var normalized = Normalizer.normalize(text.toLowerCase(locale).replace('ı', 'i'), Normalizer.Form.NFD);
        return MARKS.matcher(normalized).replaceAll("");
    }

    static int rank(String name, String query) {
        if (query.isBlank() || name.equals(query)) return 0;
        if (name.startsWith(query)) return 1;
        if (name.contains(query)) return 2;

        int from = 0;
        for (int offset = 0; offset < query.length();) {
            int letter = query.codePointAt(offset);
            offset += Character.charCount(letter);
            if (Character.isWhitespace(letter)) continue;
            int position = name.indexOf(letter, from);
            if (position < 0) return -1;
            from = position + Character.charCount(letter);
        }
        return 3;
    }
}

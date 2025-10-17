/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.dtt.helper;



import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;





import java.util.*;


public class HtmlHighlighter {

    /**
     * HTML içeriğinde yalnızca metin düğümlerini hedefleyip
     * case-insensitive olarak <mark> ile vurgular.
     * Orijinal HTML etiket yapısını korur.
     */
    public static String highlight(String html, String term) {
        if (html == null || term == null) return html;
        String trimmed = term.trim();
        if (trimmed.isEmpty()) return html;

        // Unicode destekli, case-insensitive pattern
        String regex = "(?iu)" + Pattern.quote(trimmed);
        Pattern pattern = Pattern.compile(regex);

        // Gelen HTML zaten "body" içeriği ise, parseBodyFragment yeterli
        Document doc = Jsoup.parseBodyFragment(html);
        Element body = doc.body();

        // 1️⃣ — Bütün TextNode’ları topla
        List<TextNode> textNodes = new ArrayList<>();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    textNodes.add((TextNode) node);
                }
            }

            @Override
            public void tail(Node node, int depth) {
            }
        }, body);

        // 2️⃣ — Her TextNode’u sırayla işle
        for (TextNode textNode : textNodes) {
            String text = textNode.getWholeText();
            Matcher matcher = pattern.matcher(text);

            if (!matcher.find()) continue; // eşleşme yoksa geç

            StringBuffer sb = new StringBuffer();
            do {
                matcher.appendReplacement(sb,
                        "<mark>" + Matcher.quoteReplacement(matcher.group()) + "</mark>");
            } while (matcher.find());
            matcher.appendTail(sb);

            // Yeni fragmanı parse et ve orijinal düğümü değiştir
            List<Node> replacementNodes = Jsoup.parseBodyFragment(sb.toString()).body().childNodes();
            for (Node rn : replacementNodes) {
                textNode.before(rn);
            }
            textNode.remove();
        }

        return body.html();
    }
}




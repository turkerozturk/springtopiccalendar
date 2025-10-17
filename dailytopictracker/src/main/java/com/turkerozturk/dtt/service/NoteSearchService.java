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
package com.turkerozturk.dtt.service;


import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.entity.Note;
import com.turkerozturk.dtt.helper.HtmlHighlighter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * hibernate search kullanir
 */
@Service
public class NoteSearchService {


    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MarkdownService markdownService;

    public List<Map<String, Object>> search(String term, boolean exactMatch, int page, int size) {
        var searchSession = org.hibernate.search.mapper.orm.Search.session(entityManager);

        var session = searchSession.search(Note.class);

        var result = session.where(f -> {
                    if (exactMatch) {
                        // 🔒 Tam eşleşme modu (örnek: sadece "elma")
                        return f.match()
                                .fields("content")
                                .matching(term);
                               // .fuzzy(1);
                    } else {
                        // 🌐 Kapsayıcı / "partial" arama
                        // wildcard sorgusu, kelimenin geçtiği tüm biçimleri bulur
                        String pattern = "*" + term.toLowerCase() + "*";
                        return f.wildcard()
                                .fields("content")
                                .matching(pattern);
                    }
                })
                        .sort(f -> f.field("idSort").desc()) // burada "idSort" kullan
                        .fetchHits(page * size, size);

        return result.stream().map(note -> {
            String html = markdownService.render(note.getContent());
            String highlighted = HtmlHighlighter.highlight(html, term);

            Map<String, Object> map = new HashMap<>();
            map.put("id", note.getId());
            map.put("html", highlighted);
            return map;
        }).toList();
    }


}


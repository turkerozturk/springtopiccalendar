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

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.TopicEntryView;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntryModalService {

    private final TopicRepository topicRepository; // mevcut Topic repository
    private final EntryRepository entryRepository; // mevcut Entry repository
    // entryRepository içinde tarih bazlı sorgu ekleyeceğiz

    public EntryModalService(TopicRepository topicRepository,
                             EntryRepository entryRepository) {
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }

    public List<TopicEntryView> getTopicEntriesForDay(List<Long> topicIds, LocalDate day) {
        if (topicIds == null || topicIds.isEmpty()) return Collections.emptyList();

        // epoch millis aralığı (günün başı ve sonu) — AppTimeZoneProvider ile aynı zone'u kullan
        ZoneId zone = AppTimeZoneProvider.getZone();
        Instant start = day.atStartOfDay(zone).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1);

        long startMillis = start.toEpochMilli();
        long endMillis = end.toEpochMilli();

        // topic'leri getir (id -> name)
        List<Topic> topics = topicRepository.findAllById(topicIds);

        // İlgili tarih aralığındaki entryleri çek (topic id'ye göre)
        List<Entry> entries = entryRepository.findByTopicIdInAndDateMillisYmdBetween(topicIds, startMillis, endMillis);

        // Map topicId -> entry
        Map<Long, Entry> entryMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getTopic().getId(), Function.identity(), (a, b) -> a));

        // Build view list in the order of topicIds (korumak istersen)
        List<TopicEntryView> views = new ArrayList<>();
        Map<Long, Topic> topicById = topics.stream().collect(Collectors.toMap(Topic::getId, t -> t));

        for (Long tid : topicIds) {
            Topic t = topicById.get(tid);
            String name = t != null ? t.getName() : ("#" + tid);
            Entry e = entryMap.get(tid);
            TopicEntryView v = new TopicEntryView();
            v.setTopicId(tid);
            v.setTopicName(name);
            if (e != null) {
                v.setEntryStatus(e.getStatus());
                if (e.getNote() != null) v.setNoteContent(e.getNote().getContent());
            }
            views.add(v);
        }
        return views;
    }

    public byte[] createPdfForDay(List<Long> topicIds, LocalDate day) {
        // Basit placeholder: boş pdf üretimi (kullanıcı gerçek PDF motoru ekleyebilir).
        // Örnek: iText veya PDFBox ile render etmek iyi olur. Burada 1 byte döndürmeyelim.
        // Aşağıda minimal PDF header (geçici). Lütfen yerine gerçek PDF üret.
        return new byte[]{};
    }
}

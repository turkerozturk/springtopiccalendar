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
package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Note;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import com.turkerozturk.dtt.service.TopicService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryRestController {

    private final AppTimeZoneProvider timeZoneProvider;

    private final TopicService topicService;

    private final EntryRepository entryRepository;
    private final TopicRepository topicRepository;

    @PostMapping("/status")
    public ResponseEntity<EntryDto> setStatus(@RequestBody EntryStatusRequest req) {
        Entry entry;
        ZoneId zoneId = timeZoneProvider.getZoneId();

        Long entryId = req.getEntryId();
        Long topicId = req.getTopicId();
        Long categoryId = req.getCategoryId();
        Integer status = req.getStatus();
        Integer oldStatus = req.oldStatus;
        LocalDate ld = req.getLocalDate();

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        Long dateMillisYmd = ld.atStartOfDay(zoneId).toInstant().toEpochMilli();

        Entry saved;
        if (entryId != null && oldStatus != null && oldStatus != 3) {
            // update existing
            entry = entryRepository.findById(entryId)
                    .orElseThrow(() -> new RuntimeException("Entry not found"));
            entry.setStatus(status);
        } else {
            entry = new Entry();
            entry.setTopic(topic);
            entry.setDateMillisYmd(dateMillisYmd);
            entry.setStatus(status);

            // Note olustur ve iki yonlu bagla
            Note note = new Note();
            note.setContent("");
            note.setEntry(entry);   // iliskiyi kur
            entry.setNote(note);    // ters tarafi da bagla
        }

        saved = entryRepository.save(entry);

        // we need to update both of the topic statuses(for old and new topic) after saving the entry.
        topicService.updateTopicStatus(topicId);

        return ResponseEntity.ok(new EntryDto(saved.getId(), saved.getStatus()));

    }

    @Data
    public static class EntryStatusRequest {
        private Long entryId;
        private Long topicId;
        private Long categoryId;
        private LocalDate localDate;
        private Integer status;
        private Integer oldStatus;

    }

    @Data
    @AllArgsConstructor
    public static class EntryDto {
        private Long id;
        private Integer status;
    }
}


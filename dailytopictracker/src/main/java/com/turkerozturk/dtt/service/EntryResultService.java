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
import com.turkerozturk.dtt.dto.NoteSearchResultDTO;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.repository.EntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EntryResultService {

    private final EntryRepository entryRepository;
    private final MarkdownService markdownService;
    private final CategoryProfileService categoryProfileService;

    public Page<NoteSearchResultDTO> browse(Long topicId, Long categoryId, Long categoryGroupId,
                                            Long categoryProfileId, List<Long> topicIds, Pageable page) {
        Page<Entry> entries;
        if (topicId != null) {
            entries = entryRepository.findByTopicId(topicId, page);
        } else if (categoryId != null) {
            entries = entryRepository.findByTopic_Category_Id(categoryId, page);
        } else if (categoryGroupId != null) {
            entries = entryRepository.findByTopic_Category_CategoryGroup_Id(categoryGroupId, page);
        } else if (categoryProfileId != null) {
            List<Long> categoryIds = categoryProfileService.getCategoryIdsForProfile(categoryProfileId);
            entries = categoryIds.isEmpty() ? Page.empty(page) : entryRepository.findByTopic_Category_IdIn(categoryIds, page);
        } else if (topicIds != null && !topicIds.isEmpty()) {
            entries = entryRepository.findByTopic_IdIn(topicIds, page);
        } else {
            entries = entryRepository.findAll(page);
        }
        return entries.map(this::toDTO);
    }

    private NoteSearchResultDTO toDTO(Entry entry) {
        var note = entry.getNote();
        var topic = entry.getTopic();
        var category = topic != null ? topic.getCategory() : null;

        String html = note != null ? markdownService.render(note.getContent()) : "";

        return new NoteSearchResultDTO(
                note != null ? note.getId() : null,
                html,
                entry.getDate() != null ? entry.getDate().toString() : "",
                entry.getId(),
                entry.getStatus(),
                topic != null ? topic.getId() : null,
                topic != null ? topic.getName() : "",
                category != null ? category.getId() : null,
                category != null ? category.getName() : ""
        );
    }
}

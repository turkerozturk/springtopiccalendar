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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EntryResultService {

    private final EntryRepository entryRepository;
    private final MarkdownService markdownService;
    private final CategoryProfileService categoryProfileService;

    // --- Topic ---
    public List<NoteSearchResultDTO> getEntriesByTopic(Long topicId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> pageResult = entryRepository.findByTopicId(topicId, pageable);
        return toDTO(pageResult.getContent());
    }


    public boolean hasMoreByTopic(Long topicId, int page, int size) {
        long total = entryRepository.countByTopicId(topicId);
        return (long) (page + 1) * size < total;
    }

    // --- Category ---
    public List<NoteSearchResultDTO> getEntriesByCategoryId(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> pageResult = entryRepository.findByTopic_Category_Id(categoryId, pageable);
        return toDTO(pageResult.getContent());
    }

    public boolean hasMoreByCategory(Long categoryId, int page, int size) {
        long total = entryRepository.countByTopic_Category_Id(categoryId);
        return (long) (page + 1) * size < total;
    }

    // --- CategoryGroup ---
    public List<NoteSearchResultDTO> getEntriesByCategoryGroupId(Long categoryGroupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> pageResult = entryRepository.findByTopic_Category_CategoryGroup_Id(categoryGroupId, pageable);
        return toDTO(pageResult.getContent());
    }

    public boolean hasMoreByCategoryGroup(Long categoryGroupId, int page, int size) {
        long total = entryRepository.countByTopic_Category_CategoryGroup_Id(categoryGroupId);
        return (long) (page + 1) * size < total;
    }

    // --- CategoryProfile ---
    public List<NoteSearchResultDTO> getEntriesByCategoryProfileId(Long categoryProfileId, int page, int size) {
        List<Long> categoryIds = categoryProfileService.getCategoryIdsForProfile(categoryProfileId);
        if (categoryIds.isEmpty()) return List.of();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> pageResult = entryRepository.findByTopic_Category_IdIn(categoryIds, pageable);
        return toDTO(pageResult.getContent());
    }

    public boolean hasMoreByCategoryProfile(Long categoryProfileId, int page, int size) {
        long total = entryRepository.countByTopic_Category_IdIn(
                categoryProfileService.getCategoryIdsForProfile(categoryProfileId));
        return (long) (page + 1) * size < total;
    }

    // --- Topics listesi ---
    public List<NoteSearchResultDTO> getEntriesByTopics(List<Long> topicIds, int page, int size) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> pageResult = entryRepository.findByTopic_IdIn(topicIds, pageable);
        return toDTO(pageResult.getContent());
    }

    public boolean hasMoreByTopics(List<Long> topicIds, int page, int size) {
        long total = entryRepository.countByTopic_IdIn(topicIds);
        return (long) (page + 1) * size < total;
    }

    // --- Tümü ---
    public List<NoteSearchResultDTO> getAllEntries(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        var entries = entryRepository.findAll(pageable).getContent();
        return toDTO(entries);
    }

    public boolean hasMoreAll(int page, int size) {
        long total = entryRepository.count();
        return (long) (page + 1) * size < total;
    }

    // --- Ortak DTO dönüştürücü ---
    private List<NoteSearchResultDTO> toDTO(List<Entry> entries) {
        return entries.stream().map(entry -> {
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
        }).toList();
    }
}

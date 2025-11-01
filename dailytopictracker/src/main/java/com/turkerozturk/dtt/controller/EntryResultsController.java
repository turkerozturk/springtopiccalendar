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

import com.turkerozturk.dtt.dto.NoteSearchResultDTO;
import com.turkerozturk.dtt.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;



@Controller
@RequiredArgsConstructor
@RequestMapping("/entry-results")
public class EntryResultsController {

    private final EntryResultService entryResultService;
    private final TopicService topicService;
    private final CategoryService categoryService;
    private final CategoryGroupService categoryGroupService;
    private final CategoryProfileService categoryProfileService;

    /**
     * Sayfa görünümü (ilk yükleme)
     */
    @GetMapping("/page")
    public String showPage(@RequestParam(required = false) Long topicId,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long categoryGroupId,
                           @RequestParam(required = false) Long categoryProfileId,
                           @RequestParam(required = false) List<Long> topicIds,
                           Model model) {

        // Başlık veya açıklama için context nesnesi
        if (topicId != null) {
            topicService.findById(topicId).ifPresent(t -> model.addAttribute("topic", t));
        } else if (categoryId != null) {
            categoryService.findById(categoryId).ifPresent(c -> model.addAttribute("category", c));
        } else if (categoryGroupId != null) {
            categoryGroupService.findById(categoryGroupId).ifPresent(g -> model.addAttribute("categoryGroup", g));
        } else if (categoryProfileId != null) {
            categoryProfileService.findById(categoryProfileId).ifPresent(p -> model.addAttribute("categoryProfile", p));
        } else if (topicIds != null && !topicIds.isEmpty()) {
            model.addAttribute("topicIds", topicIds);
        }

        return "entry-results-page";
    }

    /**
     * Fragment (hx-get ile sonuçları çeken endpoint)
     */
    @GetMapping
    public String getEntries(@RequestParam(required = false) Long topicId,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(required = false) Long categoryGroupId,
                             @RequestParam(required = false) Long categoryProfileId,
                             @RequestParam(required = false) List<Long> topicIds,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {

        int pageSize = 10;
        List<NoteSearchResultDTO> results;
        boolean hasMore;


        // 0 değerlerini null’a dönüştür
        if (topicId != null && topicId == 0) topicId = null;
        if (categoryId != null && categoryId == 0) categoryId = null;
        if (categoryGroupId != null && categoryGroupId == 0) categoryGroupId = null;
        if (categoryProfileId != null && categoryProfileId == 0) categoryProfileId = null;
        if (topicIds != null && topicIds.isEmpty()) topicIds = null;

        // --- Öncelik sırasına göre hangi sorgu yapılacaksa ---
        if (topicId != null) {
            results = entryResultService.getEntriesByTopic(topicId, page, pageSize);
            hasMore = entryResultService.hasMoreByTopic(topicId, page, pageSize);
        } else if (categoryId != null) {
            results = entryResultService.getEntriesByCategoryId(categoryId, page, pageSize);
            hasMore = entryResultService.hasMoreByCategory(categoryId, page, pageSize);
        } else if (categoryGroupId != null) {
            results = entryResultService.getEntriesByCategoryGroupId(categoryGroupId, page, pageSize);
            hasMore = entryResultService.hasMoreByCategoryGroup(categoryGroupId, page, pageSize);
        } else if (categoryProfileId != null) {
            results = entryResultService.getEntriesByCategoryProfileId(categoryProfileId, page, pageSize);
            hasMore = entryResultService.hasMoreByCategoryProfile(categoryProfileId, page, pageSize);
        } else if (topicIds != null && !topicIds.isEmpty()) {
            results = entryResultService.getEntriesByTopics(topicIds, page, pageSize);
            hasMore = entryResultService.hasMoreByTopics(topicIds, page, pageSize);
        } else {
            results = entryResultService.getAllEntries(page, pageSize);
            hasMore = entryResultService.hasMoreAll(page, pageSize);
        }

        model.addAttribute("results", results);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("page", page);
        model.addAttribute("topicId", topicId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categoryGroupId", categoryGroupId);
        model.addAttribute("categoryProfileId", categoryProfileId);
        model.addAttribute("topicIds", topicIds);

        return "fragments/entry-results :: results";
    }
}

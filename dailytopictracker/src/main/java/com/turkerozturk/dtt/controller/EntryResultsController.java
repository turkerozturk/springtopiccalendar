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

import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.EntryResultService;
import com.turkerozturk.dtt.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/entry-results")
public class EntryResultsController {

    private final EntryResultService entryResultService;

    private final TopicService topicService;

    @GetMapping
    public String getEntriesByTopic(@RequestParam("topicId") Long topicId,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {

        int pageSize = 10; // ilk yükleme + infinite scroll adımı
        var results = entryResultService.getEntriesByTopic(topicId, page, pageSize);
        boolean hasMore = entryResultService.hasMore(topicId, page, pageSize);

        model.addAttribute("results", results);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("page", page);
        model.addAttribute("topicId", topicId);

        return "fragments/entry-results :: results";
    }

    // Sayfa görünümü (ilk yükleme)
    @GetMapping("/page")
    public String showPage(@RequestParam("topicId") Long topicId, Model model) {
        Topic topic = topicService.findById(topicId).get();

        model.addAttribute("topic", topic); // Bunu ekle
        return "entry-results-page";
    }
}

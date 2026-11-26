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

import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.entity.CategoryProfile;
import com.turkerozturk.dtt.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/entry-results")
public class EntryResultsController {
    private static final int PAGE_SIZE = 10;

    private final EntryResultService entryResultService;
    private final TopicService topicService;
    private final CategoryService categoryService;
    private final CategoryGroupService categoryGroupService;
    private final CategoryProfileService categoryProfileService;

    @GetMapping({"", "/page"})
    public String showPage(@ModelAttribute("filter") BlogFilter filter,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestHeader(name = "HX-Request", required = false) String htmx,
                           Model model) {
        int pageIndex = Math.max(0, page);
        if (pageIndex > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var request = PageRequest.of(pageIndex, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "dateMillisYmd", "id"));
        var result = entryResultService.browse(filter.topicId(), filter.categoryId(), filter.categoryGroupId(),
                filter.categoryProfileId(), filter.topicIds(), request);

        model.addAttribute("results", result.getContent());
        model.addAttribute("resultPage", pageIndex);
        model.addAttribute("resultReturnPath", filter.path(pageIndex));
        model.addAttribute("previousPath", result.hasPrevious() ? filter.path(pageIndex - 1) : null);
        model.addAttribute("nextPath", result.hasNext() ? filter.path(pageIndex + 1) : null);
        model.addAttribute("emptyMessage", "No entries on this page.");

        if ("true".equalsIgnoreCase(htmx)) {
            return "fragments/entry-results :: results";
        }

        if (filter.topicId() != null) {
            topicService.findById(filter.topicId()).ifPresent(topic -> {
                model.addAttribute("topic", topic);
                model.addAttribute("scopeName", topic.getName());
            });
        } else if (filter.categoryId() != null) {
            model.addAttribute("scopeName", categoryService.findById(filter.categoryId())
                    .map(Category::getName).orElse("Category"));
        } else if (filter.categoryGroupId() != null) {
            model.addAttribute("scopeName", categoryGroupService.findById(filter.categoryGroupId())
                    .map(CategoryGroup::getName).orElse("Category group"));
        } else if (filter.categoryProfileId() != null) {
            model.addAttribute("scopeName", categoryProfileService.findById(filter.categoryProfileId())
                    .map(CategoryProfile::getName).orElse("Category profile"));
        } else if (filter.topicIds() != null && !filter.topicIds().isEmpty()) {
            model.addAttribute("scopeName", "Selected topics");
        }
        return "entry-results-page";
    }

    public record BlogFilter(Long topicId, Long categoryId, Long categoryGroupId,
                             Long categoryProfileId, List<Long> topicIds) {
        public BlogFilter {
            topicId = normalize(topicId);
            categoryId = normalize(categoryId);
            categoryGroupId = normalize(categoryGroupId);
            categoryProfileId = normalize(categoryProfileId);
        }

        private String path(int page) {
            var uri = UriComponentsBuilder.fromPath("/entry-results/page")
                    .queryParamIfPresent("topicId", Optional.ofNullable(topicId))
                    .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                    .queryParamIfPresent("categoryGroupId", Optional.ofNullable(categoryGroupId))
                    .queryParamIfPresent("categoryProfileId", Optional.ofNullable(categoryProfileId));
            if (topicIds != null && !topicIds.isEmpty()) {
                uri.queryParam("topicIds", topicIds);
            }
            return uri.queryParam("page", page).build().toUriString();
        }

        private static Long normalize(Long value) {
            return Long.valueOf(0L).equals(value) ? null : value;
        }
    }
}

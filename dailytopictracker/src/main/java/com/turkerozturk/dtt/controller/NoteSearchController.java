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

import com.turkerozturk.dtt.service.NoteSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class NoteSearchController {
    private final NoteSearchService noteSearchService;

    @GetMapping({"", "/results"})
    public String search(@RequestParam(name = "q", defaultValue = "") String query,
                         @RequestParam(defaultValue = "false") boolean exactMatch,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int pageSize,
                         jakarta.servlet.http.HttpServletRequest servletRequest,
                         Model model) {
        query = query.strip();
        model.addAttribute("query", query);
        model.addAttribute("exactMatch", exactMatch);
        page = Math.max(0, page);
        pageSize = Math.max(1, Math.min(100, pageSize));
        model.addAttribute("pageSize", pageSize);
        if (page > (Integer.MAX_VALUE - pageSize) / pageSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!query.isBlank()) {
            var result = noteSearchService.search(query, exactMatch, PageRequest.of(page, pageSize));
            model.addAttribute("results", result.getContent());
            model.addAttribute("resultPage", page);
            model.addAttribute("resultReturnPath", path(query, exactMatch, page, pageSize));
            model.addAttribute("previousPath", result.hasPrevious() ? path(query, exactMatch, page - 1, pageSize) : null);
            model.addAttribute("nextPath", result.hasNext() ? path(query, exactMatch, page + 1, pageSize) : null);
            model.addAttribute("emptyMessage", "No matching notes on this page.");
        }
        return "/search/results".equals(servletRequest.getServletPath()) ? "fragments/entry-results :: results" : "search";
    }

    private String path(String query, boolean exactMatch, int page, int size) {
        return UriComponentsBuilder.fromPath("/search")
                .queryParam("q", "{query}").queryParam("exactMatch", exactMatch)
                .queryParam("page", page).queryParam("pageSize", size)
                .encode().buildAndExpand(query).toUriString();
    }
}

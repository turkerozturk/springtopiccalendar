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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/search")
public class NoteSearchController {

    @Autowired
    private NoteSearchService searchService;

    @GetMapping
    public String searchPage() {

        return "search"; // search.html Thymeleaf sayfası
    }

    @GetMapping("/results")
    public String searchResults(@RequestParam(name = "q") String searchTerm,
                                @RequestParam(defaultValue = "false") boolean exactMatch,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {


        //System.out.println(exactMatch + ", " + q);
        int pageSize = 10;
        List<Map<String, Object>> results = searchService.search(searchTerm, exactMatch, page, pageSize);

        long noteCount = results.size();
        long totalMatches = results.stream()
                .mapToLong(r -> ((String) r.get("html"))
                        .split("(?i)" + Pattern.quote(searchTerm)).length - 1)
                .sum();

        model.addAttribute("results", results);
        model.addAttribute("query", searchTerm);
        //model.addAttribute("noteCount", noteCount);
        //model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("page", page);
        model.addAttribute("hasMore", noteCount == pageSize);

        return "fragments/search-results :: results";
    }



}


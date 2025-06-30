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
package com.turkerozturk.dtt.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;

@Controller
public class IndexController {

    @Autowired
    CategoryGroupRepository categoryGroupRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    EntryRepository entryRepository;

    @GetMapping
    public String homePage(Model model) {


        model.addAttribute("categoryGroupsCount", categoryGroupRepository.count());
        model.addAttribute("categoriesCount", categoryRepository.count());
        model.addAttribute("topicsCount", topicRepository.count());
        model.addAttribute("entriesCount", entryRepository.count());

        return "index";
    }


}

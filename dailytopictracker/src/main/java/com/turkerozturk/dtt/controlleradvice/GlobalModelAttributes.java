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
package com.turkerozturk.dtt.controlleradvice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${html.background.color:linen}")
    private String htmlBackgroundColor;

    @Value("${daily.goal.score:100}")
    private int dailyGoalScore;

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    public GlobalModelAttributes(CategoryGroupRepository categoryGroupRepository,
                                 CategoryRepository categoryRepository,
                                 TopicRepository topicRepository,
                                 EntryRepository entryRepository) {
        this.categoryGroupRepository = categoryGroupRepository;
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("hasCategoryGroups", categoryGroupRepository.count() > 0);
        model.addAttribute("hasCategories", categoryRepository.count() > 0);
        model.addAttribute("hasTopics", topicRepository.count() > 0);
        model.addAttribute("hasEntries", entryRepository.count() > 0);

        model.addAttribute("htmlBackgroundColor", htmlBackgroundColor);
        model.addAttribute("dailyGoalScore", dailyGoalScore);
    }
}


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

import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.TopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Controller
@RequestMapping("/topics")
public class TopicInfoController {

    private final TopicService topicService;
    private final MarkdownService markdownService;

    public TopicInfoController(TopicService topicService, MarkdownService markdownService) {
        this.topicService = topicService;
        this.markdownService = markdownService;
    }

    @GetMapping("/info/{id}")
    public String getTopicInfo(@PathVariable Long id, Model model) {
        Optional<Topic> topicOptional = topicService.findById(id);
        if(topicOptional.isPresent()) {
            Topic topic = topicOptional.get();
            model.addAttribute("topic", topic);
            model.addAttribute("markdownDescription", convertToMarkdownHtml(topic.getDescription()));
            int entryCount = topic.getActivities().size();
            model.addAttribute("entryCount", entryCount);

        }
        return "fragments/topicInfoContent :: content";
    }

    private String convertToMarkdownHtml(String text) {
        if (text == null) return "";
        return markdownService.render(text);
    }
}


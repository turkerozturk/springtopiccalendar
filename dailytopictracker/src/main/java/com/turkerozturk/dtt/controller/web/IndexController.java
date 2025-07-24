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

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    TopicService topicService;

    @Autowired
    EntryService entryService;

    @GetMapping
    public String homePage(Model model) {


        model.addAttribute("categoryGroupsCount", categoryGroupRepository.count());
        model.addAttribute("categoriesCount", categoryRepository.count());
        model.addAttribute("topicsCount", topicRepository.count());
        model.addAttribute("entriesCount", entryRepository.count());

        ZoneId zoneId = AppTimeZoneProvider.getZone();

        LocalDate today = LocalDate.now();
        prepareCategoryPieChartForDate(today, zoneId, model, "today");

        LocalDate yesterday = today.minusDays(1);
        prepareCategoryPieChartForDate(yesterday, zoneId, model, "yesterday");

        LocalDate twoDaysAgo = today.minusDays(2);
        prepareCategoryPieChartForDate(twoDaysAgo, zoneId, model, "twoDaysAgo");

        return "index";
    }



    public void prepareCategoryPieChartForDate(
            LocalDate aDay,
            ZoneId zoneId,
            Model model,
            String modelPrefix
    ) {
        long dateMillisYmd = aDay.atStartOfDay(zoneId).toInstant().toEpochMilli();

        List<Category> categories = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        int totalCount = 0;

        for (Category c : categories) {
            List<Entry> doneEntries = entryService.findDonesByCategory(c.getId(), dateMillisYmd);

            List<Entry> weightedEntries = new ArrayList<>();
            for(Entry e : doneEntries) {
                if(e.getTopic().getWeight() >= 0) {
                    weightedEntries.add(e);
                }
            }


            if (!weightedEntries.isEmpty()) {
                labels.add(c.getName());
                counts.add(weightedEntries.size());
                ids.add(c.getId());
                totalCount += weightedEntries.size();
            }
        }

        model.addAttribute(modelPrefix + "CategoryLabels", labels);
        model.addAttribute(modelPrefix + "CategoryCounts", counts);
        model.addAttribute(modelPrefix + "CategoryIds", ids);
        model.addAttribute(modelPrefix + "CategoryTotalCount", totalCount);
        model.addAttribute(modelPrefix + "CategoryTotalCategories", labels.size());
    }




}

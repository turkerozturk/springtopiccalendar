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

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.TopicDto;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.CategoryService;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
//@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class DayViewRestController {

    @Autowired
    private TopicService topicService;

    @GetMapping("/api/dayview-topics")
    @ResponseBody
    public List<TopicDto> searchDayViewTopics(@RequestParam String q) {

        return topicService.searchDayViewTopics(q);
    }


    @Autowired
    private EntryService entryService;

    @Autowired
    private CategoryService categoryService;

    ZoneId zoneId = AppTimeZoneProvider.getZone();


    @GetMapping("/api/dayview-radar")
    public Map<String, Object> getRadarChartData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {

        StringBuilder stringFormatSb = new StringBuilder();


        stringFormatSb.append("<div style='text-align: left;'>");
        stringFormatSb.append("%s");

        stringFormatSb.append("<span class=\"status-%s\">");
        stringFormatSb.append("ⓘ");
        stringFormatSb.append("</span>");
        stringFormatSb.append("<a class='topic-name' ")
                .append("hx-get='/topics/info/")
                .append("%s")
                .append("' hx-target='#topicInfoContent' ")
                .append("hx-trigger='click' ")
                .append("data-bs-toggle='modal' data-bs-target='#topicInfoModal'>")
                .append("%s")
                .append("</a>");

        stringFormatSb.append("<br/>");
        stringFormatSb.append("<a class='category-name' href='/entry-filter/form?categoryId=");
        stringFormatSb.append("%s");
        stringFormatSb.append("'>");

        stringFormatSb.append("<sup>");

        stringFormatSb.append("%s");
        stringFormatSb.append("</sup>");

        stringFormatSb.append("</a>");

        stringFormatSb.append("</div>");

        long dateMillisYmd;
        if (date == null) {
            dateMillisYmd = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli();
        } else {
            dateMillisYmd = date.atStartOfDay(zoneId).toInstant().toEpochMilli();
        }

        List<Category> categories = categoryService.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Integer> categoryWeights = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        StringBuilder reportForPositiveWeight = new StringBuilder();
        StringBuilder reportForZeroWeight = new StringBuilder();
        StringBuilder reportForNegativeWeight = new StringBuilder();

        int totalCount = 0;

        int totalWeight = 0;

        for (Category c : categories) {
            //List<Entry> doneEntries = entryService.findDonesByCategory(c.getId(), dateMillisYmd);

            List<Entry> allEntriesByCategoryAndOneDay = entryService.findAllByCategoryAndOneDay(c.getId(), dateMillisYmd);


            List<Entry> weightedEntries = new ArrayList<>();
            for(Entry e : allEntriesByCategoryAndOneDay) {
                if(e.getTopic().getWeight() >= 0) {
                    weightedEntries.add(e);
                } else {
                    Topic topic = e.getTopic();

                    reportForNegativeWeight.append(String.format(stringFormatSb.toString(),
                            topic.getWeight(),
                            e.getStatus(),
                            topic.getId(),
                            topic.getName(),
                            topic.getCategory().getId(),
                            topic.getCategory().getName() )
                    );

                }
            }

            int categoryWeight = 0;

            if (!weightedEntries.isEmpty()) {
                labels.add(c.getName());
                counts.add(weightedEntries.size());
                ids.add(c.getId());
                totalCount += weightedEntries.size();
                for(Entry entry : weightedEntries) {

                    Topic topic = entry.getTopic();
                    if(topic.getWeight() > 0) {
                        if(entry.getStatus().equals(1)) {

                            totalWeight += topic.getWeight();
                            categoryWeight += topic.getWeight();
                        }

                        reportForPositiveWeight.append(String.format(stringFormatSb.toString(),
                                topic.getWeight(),
                                entry.getStatus(),
                                topic.getId(),
                                topic.getName(),
                                topic.getCategory().getId(),
                                topic.getCategory().getName() )
                        );


                    } else if(topic.getWeight() == 0) {
                        reportForZeroWeight.append(String.format(stringFormatSb.toString(),
                                topic.getWeight(),
                                entry.getStatus(),
                                topic.getId(),
                                topic.getName(),
                                topic.getCategory().getId(),
                                topic.getCategory().getName() )
                        );
                    } else {
                        // reportForNegativeWeight islemlerini burada degil, blok disinda yukarida hallettik zaten.
                    }
                }
                categoryWeights.add(categoryWeight);


            }

        }





        // burada senin prepareCategoryPieChartForDate() içindeki hesapları yapıyoruz
        Map<String, Object> result = new HashMap<>();
        result.put("totalWeight", totalWeight);
        result.put("categoryLabels", labels);
        result.put("categoryCounts", counts);
        result.put("categoryWeights", categoryWeights);
        //result.put("categoryIds", ids);
        result.put("categoryTotalCount", totalCount);
        result.put("categoryTotalCategories", labels.size());

        result.put("reportForPositiveWeight", reportForPositiveWeight.toString());
        result.put("reportForZeroWeight", reportForZeroWeight.toString());
        result.put("reportForNegativeWeight", reportForNegativeWeight.toString());

        return result;
    }



}

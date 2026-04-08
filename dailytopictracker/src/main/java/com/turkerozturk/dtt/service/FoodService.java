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
package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.dto.FoodEntryDto;
import com.turkerozturk.dtt.dto.FoodSummaryDto;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.helper.FoodParser;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    public FoodSummaryDto getDailyFoodSummary(Long dateMillis) {

        List<Topic> foodTopics = topicRepository.findFoodTopics();

        List<Long> topicIds = foodTopics.stream()
                .map(Topic::getId)
                .toList();

        List<Entry> entries = entryRepository
                .findFoodEntriesByDate(dateMillis, topicIds);

        List<FoodEntryDto> result = new ArrayList<>();

        double totalKcal = 0.0;
        double totalGram = 0.0;
        double totalGramFat = 0.0;
        double totalGramCarbonhydrate = 0.0;
        double totalGramProtein = 0.0;

        for (Entry entry : entries) {

            String noteContent = entry.getNote() != null
                    ? entry.getNote().getContent()
                    : null;
            Topic topic = entry.getTopic();
            String topicDescription = topic.getDescription();

            Double gram = FoodParser.extractGram(noteContent);
            Double kcalPer100g = FoodParser.extractKcalPer100g(
                    topicDescription
            );

            if (gram == null || kcalPer100g == null) continue;

            Double kcal = FoodParser.calculateKcal(gram, kcalPer100g);
            Double gramFat = FoodParser.calculateKcal(gram, FoodParser.extractFat(topicDescription));
            Double gramCarbonhydrate = FoodParser.calculateKcal(gram, FoodParser.extractCarbohydrate(topicDescription));
            Double gramProtein = FoodParser.calculateKcal(gram, FoodParser.extractProtein(topicDescription));

            FoodEntryDto dto = new FoodEntryDto();
            dto.setEntryId(entry.getId());
            dto.setDateMillis(dateMillis);
            dto.setTopicId(topic.getId());
            dto.setTopicName(topic.getName());
            dto.setTopicDescription(topicDescription);
            dto.setGram(gram);
            dto.setKcalPer100g(kcalPer100g);
            dto.setCalculatedKcal(kcal);
            dto.setCategoryId(topic.getCategory().getId());
            dto.setFat(gramFat);
            dto.setCarbohydrate(gramCarbonhydrate);
            dto.setProtein(gramProtein);

            result.add(dto);

            totalKcal += kcal;
            totalGram += gram;
            totalGramFat += dto.getFat();
            totalGramCarbonhydrate += dto.getCarbohydrate();
            totalGramProtein += dto.getProtein();
        }

        FoodSummaryDto summary = new FoodSummaryDto();
        // Ayni listeyi yerinde sirala (IN-PLACE)
        result.sort(Comparator.comparing(FoodEntryDto::getCalculatedKcal,
                Comparator.nullsLast(Comparator.reverseOrder())));
        summary.setItems(result);
        summary.setTotalKcal(totalKcal);
        summary.setTotalGram(totalGram);
        summary.setTotalGramFat(totalGramFat);
        summary.setTotalGramCarbohydrate(totalGramCarbonhydrate);
        summary.setTotalGramProtein(totalGramProtein);

        return summary;
    }
}

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
        double totalGramFiber = 0.0;
        double totalGramSodium = 0.0;
        double totalGramFatSaturated = 0.0;
        double totalGramSugar = 0.0;

        double totalGramFatKcal = 0.0;
        double totalGramCarbonhydrateKcal = 0.0;
        double totalGramProteinKcal = 0.0;
        double totalGramKcalFatCarbProtein = 0.0;


        double totalKcalByStatus = 0.0;
        double totalGramByStatus = 0.0;
        double totalGramFatByStatus = 0.0;
        double totalGramCarbohydrateByStatus = 0.0;
        double totalGramProteinByStatus = 0.0;

        double totalGramFiberByStatus = 0.0;
        double totalGramSodiumByStatus = 0.0;
        double totalGramFatSaturatedByStatus = 0.0;
        double totalGramSugarByStatus = 0.0;



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

            // Frontend'de topic description icerisine #food yazdiktan sonra, satirlara asagidaki siralama ile veri girilir:
            // #food
            Double kcal = FoodParser.calculateKcal(gram, kcalPer100g);
            Double gramFat = FoodParser.calculateKcal(gram, FoodParser.extractFat(topicDescription));
            Double gramCarbonhydrate = FoodParser.calculateKcal(gram, FoodParser.extractCarbohydrate(topicDescription));
            Double gramProtein = FoodParser.calculateKcal(gram, FoodParser.extractProtein(topicDescription));
            Double gramFiber = FoodParser.calculateKcal(gram, FoodParser.extractFiber(topicDescription));
            Double gramSodium = FoodParser.calculateKcal(gram, FoodParser.extractSodium(topicDescription));
            Double gramFatSaturated = FoodParser.calculateKcal(gram, FoodParser.extractFatSaturated(topicDescription));
            Double gramSugar = FoodParser.calculateKcal(gram, FoodParser.extractSugar(topicDescription));

            FoodEntryDto dto = new FoodEntryDto();
            dto.setEntryId(entry.getId());
            dto.setEntryStatus(entry.getStatus());
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
            dto.setFiber(gramFiber);
            dto.setSodium(gramSodium);
            dto.setFatSaturated(gramFatSaturated);
            dto.setSugar(gramSugar);

            dto.setGramPerKcal(100 / kcalPer100g);

            result.add(dto);

            totalKcal += kcal;
            totalGram += gram;

            totalGramFat += dto.getFat();
            totalGramCarbonhydrate += dto.getCarbohydrate();
            totalGramProtein += dto.getProtein();
            totalGramFiber += dto.getFiber();
            totalGramSodium += dto.getSodium();
            totalGramFatSaturated += dto.getFatSaturated();
            totalGramSugar += dto.getSugar();

            if(entry.getStatus() == 1) {
                totalKcalByStatus += kcal;
                totalGramByStatus += gram;
                totalGramFatByStatus += dto.getFat();
                totalGramCarbohydrateByStatus += dto.getCarbohydrate();
                totalGramProteinByStatus += dto.getProtein();
                totalGramFiberByStatus += dto.getFiber();
                totalGramSodiumByStatus += dto.getSodium();
                totalGramFatSaturatedByStatus += dto.getFatSaturated();
                totalGramSugarByStatus += dto.getSugar();

            }


            totalGramFatKcal += dto.getFat() * 4;
            totalGramCarbonhydrateKcal += dto.getCarbohydrate() * 4;
            totalGramProteinKcal += dto.getProtein() * 9;
            totalGramKcalFatCarbProtein = totalGramFatKcal + totalGramCarbonhydrateKcal + totalGramProteinKcal;
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
        summary.setTotalGramFiber(totalGramFiber);
        summary.setTotalGramSodium(totalGramSodium);
        summary.setTotalGramFatSaturated(totalGramFatSaturated);
        summary.setTotalGramSugar(totalGramSugar);


        summary.setTotalGramFatKcal(totalGramFatKcal);
        summary.setTotalGramCarbohydrateKcal(totalGramCarbonhydrateKcal);
        summary.setTotalGramProteinKcal(totalGramProteinKcal);
        summary.setTotalGramKcalFatCarbProtein(totalGramKcalFatCarbProtein);

        summary.setTotalKcalByStatus(totalKcalByStatus);
        summary.setTotalGramByStatus(totalGramByStatus);
        summary.setTotalGramFatByStatus(totalGramFatByStatus);
        summary.setTotalGramCarbohydrateByStatus(totalGramCarbohydrateByStatus);
        summary.setTotalGramProteinByStatus(totalGramProteinByStatus);
        summary.setTotalGramFiberByStatus(totalGramFiberByStatus);
        summary.setTotalGramSodiumByStatus(totalGramSodiumByStatus);
        summary.setTotalGramFatSaturatedByStatus(totalGramFatSaturatedByStatus);
        summary.setTotalGramSugarByStatus(totalGramSugarByStatus);

        return summary;
    }
}

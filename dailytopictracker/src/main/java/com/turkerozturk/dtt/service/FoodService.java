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

import com.turkerozturk.dtt.dto.DailyFoodSummaryDto;
import com.turkerozturk.dtt.dto.DateRangeFoodSummaryDto;
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


        double totalPercentFat = 0.0;
        double totalPercentCarbohydrate = 0.0;
        double totalPercentProtein = 0.0;


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

            totalPercentFat = (totalGramFatKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentCarbohydrate = (totalGramCarbonhydrateKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentProtein = (totalGramProteinKcal / totalGramKcalFatCarbProtein) * 100;

        }

        FoodSummaryDto summary = new FoodSummaryDto();
        // Ayni listeyi yerinde sirala (IN-PLACE)
        result.sort(Comparator.comparing(FoodEntryDto::getCalculatedKcal,
                Comparator.nullsLast(Comparator.reverseOrder())));
        summary.setItems(result);
        DailyFoodSummaryDto dailyFoodSummaryDto = new DailyFoodSummaryDto();
        dailyFoodSummaryDto.setTotalKcal(totalKcal);
        dailyFoodSummaryDto.setTotalGram(totalGram);

        dailyFoodSummaryDto.setTotalGramFat(totalGramFat);
        dailyFoodSummaryDto.setTotalGramCarbohydrate(totalGramCarbonhydrate);
        dailyFoodSummaryDto.setTotalGramProtein(totalGramProtein);
        dailyFoodSummaryDto.setTotalGramFiber(totalGramFiber);
        dailyFoodSummaryDto.setTotalGramSodium(totalGramSodium);
        dailyFoodSummaryDto.setTotalGramFatSaturated(totalGramFatSaturated);
        dailyFoodSummaryDto.setTotalGramSugar(totalGramSugar);


        dailyFoodSummaryDto.setTotalGramFatKcal(totalGramFatKcal);
        dailyFoodSummaryDto.setTotalGramCarbohydrateKcal(totalGramCarbonhydrateKcal);
        dailyFoodSummaryDto.setTotalGramProteinKcal(totalGramProteinKcal);
        dailyFoodSummaryDto.setTotalGramKcalFatCarbProtein(totalGramKcalFatCarbProtein);

        dailyFoodSummaryDto.setTotalPercentFat(totalPercentFat);
        dailyFoodSummaryDto.setTotalPercentCarbohydrate(totalPercentCarbohydrate);
        dailyFoodSummaryDto.setTotalPercentProtein(totalPercentProtein);

        dailyFoodSummaryDto.setTotalKcalByStatus(totalKcalByStatus);
        dailyFoodSummaryDto.setTotalGramByStatus(totalGramByStatus);
        dailyFoodSummaryDto.setTotalGramFatByStatus(totalGramFatByStatus);
        dailyFoodSummaryDto.setTotalGramCarbohydrateByStatus(totalGramCarbohydrateByStatus);
        dailyFoodSummaryDto.setTotalGramProteinByStatus(totalGramProteinByStatus);
        dailyFoodSummaryDto.setTotalGramFiberByStatus(totalGramFiberByStatus);
        dailyFoodSummaryDto.setTotalGramSodiumByStatus(totalGramSodiumByStatus);
        dailyFoodSummaryDto.setTotalGramFatSaturatedByStatus(totalGramFatSaturatedByStatus);
        dailyFoodSummaryDto.setTotalGramSugarByStatus(totalGramSugarByStatus);

        summary.setFsd(dailyFoodSummaryDto);

        return summary;
    }





    public DateRangeFoodSummaryDto getDateRangeSummary(Long startDateMillis, Long endDateMillis) {

        List<DailyFoodSummaryDto> dailyList = new ArrayList<>();

        DateRangeFoodSummaryDto rangeDto = new DateRangeFoodSummaryDto();

        // toplamlar
        double totalKcal = 0.0;
        double totalGram = 0.0;
        double totalFat = 0.0;
        double totalCarb = 0.0;
        double totalProtein = 0.0;
        double totalFiber = 0.0;
        double totalSodium = 0.0;
        double totalSugar = 0.0;
        double totalFatSat = 0.0;
        double totalWater = 0.0;

        // gün gün dolaş
        long oneDayMillis = 24 * 60 * 60 * 1000;

        for (long date = startDateMillis; date <= endDateMillis; date += oneDayMillis) {

            FoodSummaryDto daily = getDailyFoodSummary(date);

            DailyFoodSummaryDto dto = new DailyFoodSummaryDto();

            dto.setDateMillis(date);

            dto.setTotalKcal(nvl(daily.getFsd().getTotalKcal()));
            dto.setTotalGram(nvl(daily.getFsd().getTotalGram()));

            dto.setTotalGramFat(nvl(daily.getFsd().getTotalGramFat()));
            dto.setTotalGramCarbohydrate(nvl(daily.getFsd().getTotalGramCarbohydrate()));
            dto.setTotalGramProtein(nvl(daily.getFsd().getTotalGramProtein()));

            dto.setTotalGramFiber(nvl(daily.getFsd().getTotalGramFiber()));
            dto.setTotalGramSodium(nvl(daily.getFsd().getTotalGramSodium()));
            dto.setTotalGramSugar(nvl(daily.getFsd().getTotalGramSugar()));
            dto.setTotalGramFatSaturated(nvl(daily.getFsd().getTotalGramFatSaturated()));

            dto.setTotalGramWater(nvl(daily.getFsd().getTotalGramWater()));

            // listeye ekle
            dailyList.add(dto);

            // toplamları artır
            totalKcal += dto.getTotalKcal();
            totalGram += dto.getTotalGram();
            totalFat += dto.getTotalGramFat();
            totalCarb += dto.getTotalGramCarbohydrate();
            totalProtein += dto.getTotalGramProtein();
            totalFiber += dto.getTotalGramFiber();
            totalSodium += dto.getTotalGramSodium();
            totalSugar += dto.getTotalGramSugar();
            totalFatSat += dto.getTotalGramFatSaturated();
            totalWater += dto.getTotalGramWater();
        }

        rangeDto.setDays(dailyList);

        rangeDto.setTotalKcal(totalKcal);
        rangeDto.setTotalGram(totalGram);
        rangeDto.setTotalGramFat(totalFat);
        rangeDto.setTotalGramCarbohydrate(totalCarb);
        rangeDto.setTotalGramProtein(totalProtein);
        rangeDto.setTotalGramFiber(totalFiber);
        rangeDto.setTotalGramSodium(totalSodium);
        rangeDto.setTotalGramSugar(totalSugar);
        rangeDto.setTotalGramFatSaturated(totalFatSat);
        rangeDto.setTotalGramWater(totalWater);

        return rangeDto;
    }

    /**
     * null guvenligi helper
     * @param value
     * @return
     */
    private double nvl(Double value) {
        return value == null ? 0.0 : value;
    }

}

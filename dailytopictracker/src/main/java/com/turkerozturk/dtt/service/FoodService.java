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

import com.turkerozturk.dtt.dto.*;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.helper.FoodParser;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.*;

import static com.turkerozturk.dtt.helper.FoodParser.extractMealGrams;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final NutritionService nutritionService;
    private final PhysicalActivityService physicalActivityService;


    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    @Value("${app.locale:en}")
    private String appLocale;

    public FoodSummaryDto getDailyFoodSummary(Long dateMillis) {
        Locale locale = Locale.forLanguageTag(appLocale);
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY); // case-insensitive

        NutritionResultDto humanBody = nutritionService.calculate(dateMillis);


        List<Topic> foodTopics = topicRepository.findFoodTopics();

        List<Long> topicIds = foodTopics.stream()
                .map(Topic::getId)
                .toList();

        List<Entry> entries = entryRepository
                .findEntriesByADayForSelectedTopics(dateMillis, topicIds);

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

        double totalKcalDiff = 0.0;
        double totalGramDiff = 0.0;


        double totalKcalByStatus0 = 0.0;
        double totalGramByStatus0 = 0.0;
        double totalGramFatByStatus0 = 0.0;
        double totalGramCarbohydrateByStatus0 = 0.0;
        double totalGramProteinByStatus0 = 0.0;
        double totalGramFiberByStatus0 = 0.0;
        double totalGramSodiumByStatus0 = 0.0;
        double totalGramFatSaturatedByStatus0 = 0.0;
        double totalGramSugarByStatus0 = 0.0;

        double totalKcalByStatus1 = 0.0;
        double totalGramByStatus1 = 0.0;
        double totalGramFatByStatus1 = 0.0;
        double totalGramCarbohydrateByStatus1 = 0.0;
        double totalGramProteinByStatus1 = 0.0;
        double totalGramFiberByStatus1 = 0.0;
        double totalGramSodiumByStatus1 = 0.0;
        double totalGramFatSaturatedByStatus1 = 0.0;
        double totalGramSugarByStatus1 = 0.0;

        double totalKcalByStatus2 = 0.0;
        double totalGramByStatus2 = 0.0;
        double totalGramFatByStatus2 = 0.0;
        double totalGramCarbohydrateByStatus2 = 0.0;
        double totalGramProteinByStatus2 = 0.0;
        double totalGramFiberByStatus2 = 0.0;
        double totalGramSodiumByStatus2 = 0.0;
        double totalGramFatSaturatedByStatus2 = 0.0;
        double totalGramSugarByStatus2 = 0.0;

        Map<Character, MealGroupDto> mealMap = new LinkedHashMap<>();

        for (Entry entry : entries) {

            String noteContent = entry.getNote() != null
                    ? entry.getNote().getContent()
                    : null;
            Topic topic = entry.getTopic();

            String topicDescription = topic.getDescription();

            Double kcalPer100g = FoodParser.extractKcalPer100g(
                    topicDescription
            );

            if (kcalPer100g == null) continue;

            // Double gram = FoodParser.extractGram(noteContent); // eski parser
            Map<Character, Double> mealGrams = extractMealGrams(noteContent); // yeni parser (daha gelismis)


            Double gram = 0.0;

            for(Character mealCode : mealGrams.keySet()) {
                gram += mealGrams.get(mealCode);
            }


            for (Map.Entry<Character, Double> mg : mealGrams.entrySet()) {

                Character mealCode = mg.getKey();
                Double gramValue = mg.getValue();

                MealGroupDto mealGroup = mealMap.get(mealCode);

                if (mealGroup == null) {
                    mealGroup = new MealGroupDto();
                    mealGroup.setMealCode(mealCode);
                    mealGroup.setItems(new ArrayList<>());
                    mealGroup.setTotalCalories(0.0);
                    mealGroup.setTotalGramFat(0.0);
                    mealGroup.setTotalGramProtein(0.0);
                    mealGroup.setTotalGramCarbohydrate(0.0);
                    mealGroup.setTotalGramFiber(0.0);
                    mealGroup.setTotalGramSodium(0.0);
                    mealGroup.setTotalGramFatSaturated(0.0);
                    mealGroup.setTotalGramSugar(0.0);
                    
                    mealMap.put(mealCode, mealGroup);
                }

                // item ekle
                MealItemDto itemDto = new MealItemDto();
                itemDto.setFoodName(entry.getTopic().getName());
                itemDto.setGram(gramValue);

                mealGroup.getItems().add(itemDto);

                // kalori ekle (oransal!)
                Double kcal = FoodParser.calculateKcal(gramValue, kcalPer100g);
                Double gramFat = FoodParser.calculateKcal(gramValue, FoodParser.extractFat(topicDescription));
                Double gramCarbonhydrate = FoodParser.calculateKcal(gramValue, FoodParser.extractCarbohydrate(topicDescription));
                Double gramProtein = FoodParser.calculateKcal(gramValue, FoodParser.extractProtein(topicDescription));
                Double gramFiber = FoodParser.calculateKcal(gramValue, FoodParser.extractFiber(topicDescription));
                Double gramSodium = FoodParser.calculateKcal(gramValue, FoodParser.extractSodium(topicDescription));
                Double gramFatSaturated = FoodParser.calculateKcal(gramValue, FoodParser.extractFatSaturated(topicDescription));
                Double gramSugar = FoodParser.calculateKcal(gramValue, FoodParser.extractSugar(topicDescription));


                mealGroup.setTotalCalories(mealGroup.getTotalCalories() + kcal);
                mealGroup.setTotalGramFat(mealGroup.getTotalGramFat() + gramFat);
                mealGroup.setTotalGramCarbohydrate(mealGroup.getTotalGramCarbohydrate() + gramCarbonhydrate);
                mealGroup.setTotalGramProtein(mealGroup.getTotalGramProtein() + gramProtein);
                mealGroup.setTotalGramFiber(mealGroup.getTotalGramFiber() + gramFiber);
                mealGroup.setTotalGramSodium(mealGroup.getTotalGramSodium() + gramSodium);
                mealGroup.setTotalGramFatSaturated(mealGroup.getTotalGramFatSaturated() + gramFatSaturated);
                mealGroup.setTotalGramSugar(mealGroup.getTotalGramSugar() + gramSugar);
            }






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
            Category category = topic.getCategory();
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());
            dto.setFat(gramFat);
            dto.setCarbohydrate(gramCarbonhydrate);
            dto.setProtein(gramProtein);
            dto.setFiber(gramFiber);
            dto.setSodium(gramSodium);
            dto.setFatSaturated(gramFatSaturated);
            dto.setSugar(gramSugar);

            dto.setGramPerKcal(100 / kcalPer100g);

            dto.setMealGrams(mealGrams);

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

            if(entry.getStatus() == 0) {
                totalKcalByStatus0 += kcal;
                totalGramByStatus0 += gram;
                totalGramFatByStatus0 += dto.getFat();
                totalGramCarbohydrateByStatus0 += dto.getCarbohydrate();
                totalGramProteinByStatus0 += dto.getProtein();
                totalGramFiberByStatus0 += dto.getFiber();
                totalGramSodiumByStatus0 += dto.getSodium();
                totalGramFatSaturatedByStatus0 += dto.getFatSaturated();
                totalGramSugarByStatus0 += dto.getSugar();

            }

            if(entry.getStatus() == 1) {
                totalKcalByStatus1 += kcal;
                totalGramByStatus1 += gram;
                totalGramFatByStatus1 += dto.getFat();
                totalGramCarbohydrateByStatus1 += dto.getCarbohydrate();
                totalGramProteinByStatus1 += dto.getProtein();
                totalGramFiberByStatus1 += dto.getFiber();
                totalGramSodiumByStatus1 += dto.getSodium();
                totalGramFatSaturatedByStatus1 += dto.getFatSaturated();
                totalGramSugarByStatus1 += dto.getSugar();

            }

            if(entry.getStatus() == 2) {
                totalKcalByStatus2 += kcal;
                totalGramByStatus2 += gram;
                totalGramFatByStatus2 += dto.getFat();
                totalGramCarbohydrateByStatus2 += dto.getCarbohydrate();
                totalGramProteinByStatus2 += dto.getProtein();
                totalGramFiberByStatus2 += dto.getFiber();
                totalGramSodiumByStatus2 += dto.getSodium();
                totalGramFatSaturatedByStatus2 += dto.getFatSaturated();
                totalGramSugarByStatus2 += dto.getSugar();

            }


            totalGramFatKcal += dto.getFat() * 9;
            totalGramCarbonhydrateKcal += dto.getCarbohydrate() * 4;
            totalGramProteinKcal += dto.getProtein() * 4;
            totalGramKcalFatCarbProtein = totalGramFatKcal + totalGramCarbonhydrateKcal + totalGramProteinKcal;

            totalPercentFat = (totalGramFatKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentCarbohydrate = (totalGramCarbonhydrateKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentProtein = (totalGramProteinKcal / totalGramKcalFatCarbProtein) * 100;
            totalKcalDiff = totalKcal - humanBody.getTdee();
            totalGramDiff = totalKcalDiff / 7700;

        }

        // BASLA bu kisim spor aktiviteleri ile harcanan enerjiyi hesaplar.
        List<Topic> activityTopics = topicRepository.findActivityTopicsRaw();
        List<Long> activityTopicIds = activityTopics.stream()
                .map(Topic::getId)
                .toList();
        List<Entry> activityEntries = entryRepository
                .findEntriesByADayForSelectedTopics(dateMillis, activityTopicIds);
        double totalActivityKcal = 0.0;
        for (Entry activityEntry : activityEntries) {
            totalActivityKcal += physicalActivityService.calculate(activityEntry, humanBody.getWeightKg());
        }

        // gunun sonunda yedigimiz kalorilerden TDEE(gunluk kalori ihtiyacimizi) yi ve harcadigimiz kalorileri cikartiyoruz.
        double totalKcalDiffWithActivity = totalKcalDiff - totalActivityKcal;
        double totalGramDiffWithActivity = totalKcalDiffWithActivity / 7700;
        // BITTI bu kisim spor aktiviteleri ile harcanan enerjiyi hesaplar.


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

        dailyFoodSummaryDto.setTotalKcalDiff(totalKcalDiff);
        dailyFoodSummaryDto.setTotalGramDiff(totalGramDiff);


        dailyFoodSummaryDto.setTotalKcalByStatus0(totalKcalByStatus0);
        dailyFoodSummaryDto.setTotalGramByStatus0(totalGramByStatus0);
        dailyFoodSummaryDto.setTotalGramFatByStatus0(totalGramFatByStatus0);
        dailyFoodSummaryDto.setTotalGramCarbohydrateByStatus0(totalGramCarbohydrateByStatus0);
        dailyFoodSummaryDto.setTotalGramProteinByStatus0(totalGramProteinByStatus0);
        dailyFoodSummaryDto.setTotalGramFiberByStatus0(totalGramFiberByStatus0);
        dailyFoodSummaryDto.setTotalGramSodiumByStatus0(totalGramSodiumByStatus0);
        dailyFoodSummaryDto.setTotalGramFatSaturatedByStatus0(totalGramFatSaturatedByStatus0);
        dailyFoodSummaryDto.setTotalGramSugarByStatus0(totalGramSugarByStatus0);

        dailyFoodSummaryDto.setTotalKcalByStatus1(totalKcalByStatus1);
        dailyFoodSummaryDto.setTotalGramByStatus1(totalGramByStatus1);
        dailyFoodSummaryDto.setTotalGramFatByStatus1(totalGramFatByStatus1);
        dailyFoodSummaryDto.setTotalGramCarbohydrateByStatus1(totalGramCarbohydrateByStatus1);
        dailyFoodSummaryDto.setTotalGramProteinByStatus1(totalGramProteinByStatus1);
        dailyFoodSummaryDto.setTotalGramFiberByStatus1(totalGramFiberByStatus1);
        dailyFoodSummaryDto.setTotalGramSodiumByStatus1(totalGramSodiumByStatus1);
        dailyFoodSummaryDto.setTotalGramFatSaturatedByStatus1(totalGramFatSaturatedByStatus1);
        dailyFoodSummaryDto.setTotalGramSugarByStatus1(totalGramSugarByStatus1);



        dailyFoodSummaryDto.setTotalKcalByStatus2(totalKcalByStatus2);
        dailyFoodSummaryDto.setTotalGramByStatus2(totalGramByStatus2);
        dailyFoodSummaryDto.setTotalGramFatByStatus2(totalGramFatByStatus2);
        dailyFoodSummaryDto.setTotalGramCarbohydrateByStatus2(totalGramCarbohydrateByStatus2);
        dailyFoodSummaryDto.setTotalGramProteinByStatus2(totalGramProteinByStatus2);
        dailyFoodSummaryDto.setTotalGramFiberByStatus2(totalGramFiberByStatus2);
        dailyFoodSummaryDto.setTotalGramSodiumByStatus2(totalGramSodiumByStatus2);
        dailyFoodSummaryDto.setTotalGramFatSaturatedByStatus2(totalGramFatSaturatedByStatus2);
        dailyFoodSummaryDto.setTotalGramSugarByStatus2(totalGramSugarByStatus2);

        dailyFoodSummaryDto.setHumanBody(humanBody);

        dailyFoodSummaryDto.setTotalActivityKcal(totalActivityKcal);
        dailyFoodSummaryDto.setTotalKcalDiffWithActivity(totalKcalDiffWithActivity);
        dailyFoodSummaryDto.setTotalGramDiffWithActivity(totalGramDiffWithActivity);

        summary.setFsd(dailyFoodSummaryDto);




        // Meal icindeki food’lari sirala
        for (MealGroupDto meal : mealMap.values()) {
            meal.getItems().sort((a, b) ->
                    collator.compare(a.getFoodName(), b.getFoodName())
            );
        }
        // Meal’leri sirala (mealCode’a gore)
        List<MealGroupDto> sortedMeals = new ArrayList<>(mealMap.values());
        sortedMeals.sort((a, b) ->
                collator.compare(
                        String.valueOf(a.getMealCode()),
                        String.valueOf(b.getMealCode())
                )
        );

        summary.setMeals(sortedMeals); //sorted
        //summary.setMeals(new ArrayList<>(mealMap.values())); //unsorted


        return summary;
    }


    /**
     * Belirtilen tarih araligindaki her bir gune ait veriyi ve onlar uzerinden toplamlari ve ortalamalari icerir.
     * @param startDateMillis
     * @param endDateMillis
     * @return
     */
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
        double totalKcalDiff = 0.0;
        double totalGramDiff = 0.0;
        double realBodyWeightSumKg = 0.0;
        double totalActivityKcal = 0.0;
        double totalKcalTdee = 0.0;
        double totalDays = 0.0;
        double totalKcalDiffWithActivity = 0.0;
        double totalGramDiffWithActivity = 0.0;

        double totalGramFatKcal = 0.0;
        double totalGramCarbonhydrateKcal = 0.0;
        double totalGramProteinKcal = 0.0;
        double totalGramKcalFatCarbProtein = 0.0;
        double totalPercentFat = 0.0;
        double totalPercentCarbohydrate = 0.0;
        double totalPercentProtein = 0.0;




        // gün gün dolaş
        long oneDayMillis = 24 * 60 * 60 * 1000;

        for (long date = startDateMillis; date <= endDateMillis; date += oneDayMillis) {

            totalDays++;

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

            dto.setTotalKcalDiff(daily.getFsd().getTotalKcalDiff());
            dto.setTotalGramDiff(daily.getFsd().getTotalGramDiff());
            dto.setHumanBody(daily.getFsd().getHumanBody());

            dto.setTotalActivityKcal(daily.getFsd().getTotalActivityKcal());

            dto.setTotalKcalDiffWithActivity(daily.getFsd().getTotalKcalDiffWithActivity());
            dto.setTotalGramDiffWithActivity(daily.getFsd().getTotalGramDiffWithActivity());

            dto.setTotalGramFatKcal(daily.getFsd().getTotalGramFatKcal());
            dto.setTotalGramCarbohydrateKcal(daily.getFsd().getTotalGramCarbohydrateKcal());
            dto.setTotalGramProteinKcal(daily.getFsd().getTotalGramProteinKcal());
            dto.setTotalGramKcalFatCarbProtein(daily.getFsd().getTotalGramKcalFatCarbProtein());
            dto.setTotalPercentFat(daily.getFsd().getTotalPercentFat());
            dto.setTotalPercentCarbohydrate(daily.getFsd().getTotalPercentCarbohydrate());
            dto.setTotalPercentProtein(daily.getFsd().getTotalPercentProtein());

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
            totalKcalDiff += dto.getTotalKcalDiff();
            totalGramDiff += dto.getTotalGramDiff();
            realBodyWeightSumKg += dto.getHumanBody().getWeightKg();
            totalActivityKcal += dto.getTotalActivityKcal();
            totalKcalTdee += dto.getHumanBody().getTdee();
            totalKcalDiffWithActivity += dto.getTotalKcalDiffWithActivity();
            totalGramDiffWithActivity += dto.getTotalGramDiffWithActivity();

            totalGramFatKcal += dto.getTotalGramFatKcal();
            totalGramCarbonhydrateKcal += dto.getTotalGramCarbohydrateKcal();
            totalGramProteinKcal += dto.getTotalGramProteinKcal();
            totalGramKcalFatCarbProtein += dto.getTotalGramKcalFatCarbProtein();
            totalPercentFat += dto.getTotalPercentFat();
            totalPercentCarbohydrate += dto.getTotalPercentCarbohydrate();
            totalPercentProtein += dto.getTotalPercentProtein();
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
        rangeDto.setTotalKcalDiff(totalKcalDiff);
        rangeDto.setTotalGramDiff(totalGramDiff);
        rangeDto.setTotalActivityKcal(totalActivityKcal);
        rangeDto.setTotalKcalTdee(totalKcalTdee);
        rangeDto.setTotalDays(totalDays);
        rangeDto.setTotalKcalDiffWithActivity(totalKcalDiffWithActivity);
        rangeDto.setTotalGramDiffWithActivity(totalGramDiffWithActivity);

        rangeDto.setAverageKcal(totalKcal / totalDays);
        rangeDto.setAverageGram(totalGram / totalDays);
        rangeDto.setAverageGramFat(totalFat / totalDays);
        rangeDto.setAverageGramCarbohydrate(totalCarb / totalDays);
        rangeDto.setAverageGramProtein(totalProtein / totalDays);
        rangeDto.setAverageGramFiber(totalFiber / totalDays);
        rangeDto.setAverageGramSodium(totalSodium / totalDays);
        rangeDto.setAverageGramSugar(totalSugar / totalDays);
        rangeDto.setAverageGramFatSaturated(totalFatSat / totalDays);
        rangeDto.setAverageGramWater(totalWater / totalDays);
        rangeDto.setAverageKcalDiff(totalKcalDiff / totalDays);
        rangeDto.setAverageGramDiff(totalGramDiff / totalDays);
        rangeDto.setAverageWeightKg(realBodyWeightSumKg / totalDays);
        rangeDto.setAverageActivityKcal(totalActivityKcal / totalDays);
        rangeDto.setAverageKcalTdee(totalKcalTdee / totalDays);
        rangeDto.setAverageKcalDiffWithActivity(totalKcalDiffWithActivity / totalDays);
        rangeDto.setAverageGramDiffWithActivity(totalGramDiffWithActivity / totalDays);

        rangeDto.setAverageGramFatKcal(totalGramFatKcal / totalDays);
        rangeDto.setAverageGramCarbonhydrateKcal(totalGramCarbonhydrateKcal / totalDays);
        rangeDto.setAverageGramProteinKcal(totalGramProteinKcal / totalDays);
        rangeDto.setAverageGramKcalFatCarbProtein(totalGramKcalFatCarbProtein / totalDays);
        rangeDto.setAveragePercentFat(totalPercentFat / totalDays);
        rangeDto.setAveragePercentCarbohydrate(totalPercentCarbohydrate / totalDays);
        rangeDto.setAveragePercentProtein(totalPercentProtein / totalDays);




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



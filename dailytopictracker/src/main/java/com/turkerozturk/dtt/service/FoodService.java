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
import com.turkerozturk.dtt.helper.SettingHelper;
import com.turkerozturk.dtt.helper.datatrends.TrendDirection;
import com.turkerozturk.dtt.helper.datatrends.TrendUtils;
import com.turkerozturk.dtt.helper.usda.BodyMassIndexCategory;
import com.turkerozturk.dtt.helper.usda.NutrientLimit;
import com.turkerozturk.dtt.helper.usda.NutrientRequirementService;
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
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    private final SettingHelper settingHelper;
    private final NutrientRequirementService nutrientRequirementService;
    private final SleepService sleepService;
    private final NutritionService nutritionService;
    private final PhysicalActivityService physicalActivityService;

    private static final int THOUSAND_KCAL = 1000;
    private static final int THOUSAND = 1000;


    private static final double FIBER_REQ_PER_KCAL = (double) 14 / THOUSAND_KCAL;
    private static final double SATURATED_FAT_REQUIREMENT_PERCENT = 0.10;


    // TODO settingsden al
    private static final BodyMassIndexCategory BODY_MASS_INDEX_CATEGORY = BodyMassIndexCategory.NORMAL_WEIGHT;



    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    @Value("${app.locale:en}")
    private String appLocale;

    @Value("${human.gender}")
    private String genderProp;
    //private Gender genderProp;

    @Value("${human.age}")
    private int ageProp;;

    public FoodSummaryDto getDailyFoodSummary(Long dateMillis) {
        Locale locale = Locale.forLanguageTag(appLocale);
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY); // case-insensitive

        NutritionResultDto humanBody = nutritionService.calculate(dateMillis);
        SleepDurationDto sleepDurationDto = sleepService.calculate(dateMillis);


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
        double totalGramCarbohydrate = 0.0;
        double totalGramProtein = 0.0;
        double totalGramFiber = 0.0;
        double totalGramSodium = 0.0;
        double totalGramFatSaturated = 0.0;
        double totalGramSugar = 0.0;

        double totalGramFatKcal = 0.0;
        double totalGramCarbohydrateKcal = 0.0;
        double totalGramProteinKcal = 0.0;
        double totalGramKcalFatCarbProtein = 0.0;


        double totalPercentFat = 0.0;
        double totalPercentCarbohydrate = 0.0;
        double totalPercentProtein = 0.0;
        //double totalPercentSugar = 0.0;
        //double totalPercentFatSaturatedUpperLimit = 0.0;
        //double totalPercentSugarUpperLimit = 0.0;

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
            // Bir not iceriginde toplam ve carpim biciminde birden cok gramaj ve mealCode olabilir:
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
                    mealGroup.setTotalGram(0.0);

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
                Double gramCarbohydrate = FoodParser.calculateKcal(gramValue, FoodParser.extractCarbohydrate(topicDescription));
                Double gramProtein = FoodParser.calculateKcal(gramValue, FoodParser.extractProtein(topicDescription));
                Double gramFiber = FoodParser.calculateKcal(gramValue, FoodParser.extractFiber(topicDescription));
                Double gramSodium = FoodParser.calculateKcal(gramValue, FoodParser.extractSodium(topicDescription));
                Double gramFatSaturated = FoodParser.calculateKcal(gramValue, FoodParser.extractFatSaturated(topicDescription));
                Double gramSugar = FoodParser.calculateKcal(gramValue, FoodParser.extractSugar(topicDescription));


                mealGroup.setTotalGram(mealGroup.getTotalGram() + gramValue);
                mealGroup.setTotalCalories(mealGroup.getTotalCalories() + kcal);
                mealGroup.setTotalGramFat(mealGroup.getTotalGramFat() + gramFat);
                mealGroup.setTotalGramCarbohydrate(mealGroup.getTotalGramCarbohydrate() + gramCarbohydrate);
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
            Double gramCarbohydrate = FoodParser.calculateKcal(gram, FoodParser.extractCarbohydrate(topicDescription));
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
            dto.setCarbohydrate(gramCarbohydrate);
            dto.setProtein(gramProtein);
            dto.setFiber(gramFiber);
            dto.setSodium(gramSodium);
            dto.setFatSaturated(gramFatSaturated);
            dto.setSugar(gramSugar);
            StringBuilder mealCodesAsString = new StringBuilder();
            if(!mealGrams.isEmpty()) {
                for (Character c : mealGrams.keySet()) {
                    mealCodesAsString.append(c.toString());
                    //mealCodesAsString.append(mealGrams.get(c));
                    mealCodesAsString.append(", ");
                }
                //sondaki ", " karakterlerini kaldiriyoruz:
                mealCodesAsString.deleteCharAt(mealCodesAsString.length() - 1);
                mealCodesAsString.deleteCharAt(mealCodesAsString.length() - 1);
            }
            dto.setMealCodes(mealCodesAsString.toString());

            double gramPerKcal = 0.0;
            if(kcalPer100g > 0) {
                gramPerKcal = 100 / kcalPer100g;
            }
            dto.setGramPerKcal(gramPerKcal);

            dto.setMealGrams(mealGrams);

            result.add(dto);

            totalKcal += kcal;
            totalGram += gram;

            totalGramFat += dto.getFat();
            totalGramCarbohydrate += dto.getCarbohydrate();
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
            totalGramCarbohydrateKcal += dto.getCarbohydrate() * 4;
            totalGramProteinKcal += dto.getProtein() * 4;
            totalGramKcalFatCarbProtein = totalGramFatKcal + totalGramCarbohydrateKcal + totalGramProteinKcal;

            totalPercentFat = (totalGramFatKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentCarbohydrate = (totalGramCarbohydrateKcal / totalGramKcalFatCarbProtein) * 100;
            totalPercentProtein = (totalGramProteinKcal / totalGramKcalFatCarbProtein) * 100;
            totalKcalDiff = totalKcal - humanBody.getTdee();
            totalGramDiff = totalKcalDiff / 7700;

            //totalPercentFatSaturatedUpperLimit = totalPercentFat * 0.33; //TODO bunlari tekrar arastir.
            //totalPercentSugarUpperLimit = totalPercentCarbohydrate * 0.24; // 0.24 GEVSEK, 0.12 SIKI



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


        double usdaSodium = 0.0;
        double usdaProteinGram = 0.0;
        double usdaCarbohydrateGram = 0.0;
        double usdaFatRequirementMinPercent = 0.0;
        double usdaFatRequirementMaxPercent = 0.0;
        double usdaCarbohydrateRequirementMinPercent = 0.0;
        double usdaCarbohydrateRequirementMaxPercent = 0.0;
        double usdaProteinRequirementMinPercent = 0.0;
        double usdaProteinRequirementMaxPercent = 0.0;


        Integer age = settingHelper.getInt("human.age", ageProp);
        Gender gender = settingHelper.getEnum("human.gender", Gender.class, Gender.MALE);
        //String genderAsString = settingHelper.get("human.gender", genderProp);
        //Gender gender = Gender.valueOf(genderAsString);
        //System.out.println("**** age: " + age + ", gender: " + gender);

        List<NutrientLimit> nutrientLimits = nutrientRequirementService.getLimits(age, gender);
        for(NutrientLimit n : nutrientLimits) {
            switch (n.nutrientType()) {
                case SODIUM_MG -> usdaSodium = n.maxValue() / THOUSAND;
                case FAT_PERCENT -> {
                    usdaFatRequirementMinPercent = n.minValue();
                    usdaFatRequirementMaxPercent = n.maxValue();
                }
                case CARBOHYDRATE_GRAM -> usdaCarbohydrateGram = n.maxValue();
                case CARBOHYDRATE_PERCENT -> {
                    usdaCarbohydrateRequirementMinPercent = n.minValue();
                    usdaCarbohydrateRequirementMaxPercent = n.maxValue();
                }
                case PROTEIN_GRAM -> usdaProteinGram = n.maxValue();
                case PROTEIN_PERCENT -> {
                    usdaProteinRequirementMinPercent = n.minValue();
                    usdaProteinRequirementMaxPercent = n.maxValue();
                }
            }
        }

        dailyFoodSummaryDto.setSodiumRequirementGram(usdaSodium);

        dailyFoodSummaryDto.setFatRequirementMinPercent(usdaFatRequirementMinPercent);
        dailyFoodSummaryDto.setFatRequirementMaxPercent(usdaFatRequirementMaxPercent);

        dailyFoodSummaryDto.setCarbohydrateRequirementGram(usdaCarbohydrateGram);
        dailyFoodSummaryDto.setCarbohydrateRequirementMinPercent(usdaCarbohydrateRequirementMinPercent);
        dailyFoodSummaryDto.setCarbohydrateRequirementMaxPercent(usdaCarbohydrateRequirementMaxPercent);

        dailyFoodSummaryDto.setProteinRequirementGram(usdaProteinGram);
        dailyFoodSummaryDto.setProteinRequirementMinPercent(usdaProteinRequirementMinPercent);
        dailyFoodSummaryDto.setProteinRequirementMaxPercent(usdaProteinRequirementMaxPercent);

        dailyFoodSummaryDto.setFiberRequirementGram( FIBER_REQ_PER_KCAL * totalKcal);

        double totalPercentFatSaturated = (totalGramFatSaturated / totalGramKcalFatCarbProtein) * 100;

        dailyFoodSummaryDto.setFatSaturatedRequirementGram(totalPercentFatSaturated);
        dailyFoodSummaryDto.setFatSaturatedRequirementMinPercent(totalPercentFatSaturated + totalPercentFatSaturated - SATURATED_FAT_REQUIREMENT_PERCENT);
        dailyFoodSummaryDto.setFatSaturatedRequirementMaxPercent(totalPercentFatSaturated + totalPercentFatSaturated * SATURATED_FAT_REQUIREMENT_PERCENT );
        //System.out.println("+++++ " + totalGramKcalFatCarbProtein + " totalGramKcalFatCarbProtein");

        //System.out.println("***** " + totalPercentFatSaturated +" totalPercentFatSaturated");
        //System.out.println("-----" + totalGramFatSaturated + " " + "totalGramFatSaturated");


        dailyFoodSummaryDto.setTotalGram(totalGram);

        dailyFoodSummaryDto.setTotalGramFat(totalGramFat);
        dailyFoodSummaryDto.setTotalGramCarbohydrate(totalGramCarbohydrate);
        dailyFoodSummaryDto.setTotalGramProtein(totalGramProtein);
        dailyFoodSummaryDto.setTotalGramFiber(totalGramFiber);
        dailyFoodSummaryDto.setTotalGramSodium(totalGramSodium);
        dailyFoodSummaryDto.setTotalGramFatSaturated(totalGramFatSaturated);
        dailyFoodSummaryDto.setTotalGramSugar(totalGramSugar);


        dailyFoodSummaryDto.setTotalGramFatKcal(totalGramFatKcal);
        dailyFoodSummaryDto.setTotalGramCarbohydrateKcal(totalGramCarbohydrateKcal);
        dailyFoodSummaryDto.setTotalGramProteinKcal(totalGramProteinKcal);
        dailyFoodSummaryDto.setTotalGramKcalFatCarbProtein(totalGramKcalFatCarbProtein);

        dailyFoodSummaryDto.setTotalPercentFat(totalPercentFat);
        dailyFoodSummaryDto.setTotalPercentCarbohydrate(totalPercentCarbohydrate);
        dailyFoodSummaryDto.setTotalPercentProtein(totalPercentProtein);
        //dailyFoodSummaryDto.setTotalPercentFatSaturatedUpperLimit(totalPercentFatSaturatedUpperLimit);
        //dailyFoodSummaryDto.setTotalPercentSugarUpperLimit(totalPercentSugarUpperLimit);

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

        dailyFoodSummaryDto.setSleepDurationDto(sleepDurationDto);

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
        double totalGramDiffWithActivityAndFactor = 0.0;

        double totalGramFatKcal = 0.0;
        double totalGramCarbohydrateKcal = 0.0;
        double totalGramProteinKcal = 0.0;
        double totalGramKcalFatCarbProtein = 0.0;
        double totalPercentFat = 0.0;
        double totalPercentCarbohydrate = 0.0;
        double totalPercentProtein = 0.0;
        double totalPercentFatSaturatedUpperLimit = 0.0;
        double totalPercentSugarUpperLimit = 0.0;

        double totalSleepDuration = 0.0;
        SleepDurationDto totalSleepDurationDto = new SleepDurationDto();
        double averageSleepDurationSeconds;
        SleepDurationDto averageSleepDurationDto = new SleepDurationDto();



        // tarih araligindaki tartim sonuclarinin ilk ve sonuncusu arasindaki "realWeightDifference" degerini hesaplamak icin.
        double rangeStartDayWeight = 0.0;
        double rangeEndDayWeight = 0.0;



/*
        FoodSummaryDto dailyYesterday = getDailyFoodSummary(startDateMillis - ONE_DAY_MILLIS);
        double realWeightDiffKg;
        DataDirectionDto realWeights = new DataDirectionDto();
        Map<Balance, Integer> realWeightDirectionMap;
        LinkedList<Double> realWeightValues = new LinkedList<>();
        LinkedList<Double> zeroValues = new LinkedList<>();

        double calculatedWeightDiffKg;
        DataDirectionDto calculatedWeights = new DataDirectionDto();
        Map<Balance, Integer> calculatedWeightDirectionMap;
        LinkedList<Double> calculatedWeightValues = new LinkedList<>();
*/


        // gün gün dolaş
        for (long date = startDateMillis; date <= endDateMillis; date += ONE_DAY_MILLIS) {

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

            if(date == startDateMillis + 86400 * 1000) {
                rangeStartDayWeight = dto.getHumanBody().getWeightKg();
            }
            if(date == endDateMillis) {
                rangeEndDayWeight = dto.getHumanBody().getWeightKg();
            }

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
            //dto.setTotalPercentSugarUpperLimit(daily.);

            dto.setSleepDurationDto(daily.getFsd().getSleepDurationDto());

            dto.setFatRequirementMaxPercent(daily.getFsd().getFatRequirementMaxPercent());
            dto.setFatRequirementMinPercent(daily.getFsd().getFatRequirementMinPercent());
            dto.setFatRequirementGram(daily.getFsd().getFatRequirementGram());

            dto.setCarbohydrateRequirementMaxPercent(daily.getFsd().getCarbohydrateRequirementMaxPercent());
            dto.setCarbohydrateRequirementMinPercent(daily.getFsd().getCarbohydrateRequirementMinPercent());
            dto.setCarbohydrateRequirementGram(daily.getFsd().getCarbohydrateRequirementGram());


            dto.setProteinRequirementMaxPercent(daily.getFsd().getProteinRequirementMaxPercent());
            dto.setProteinRequirementMinPercent(daily.getFsd().getProteinRequirementMinPercent());
            dto.setProteinRequirementGram(daily.getFsd().getProteinRequirementGram());

            dto.setFiberRequirementGram(daily.getFsd().getFiberRequirementGram());
            dto.setSodiumRequirementGram(daily.getFsd().getSodiumRequirementGram());

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
            totalGramCarbohydrateKcal += dto.getTotalGramCarbohydrateKcal();
            totalGramProteinKcal += dto.getTotalGramProteinKcal();
            totalGramKcalFatCarbProtein += dto.getTotalGramKcalFatCarbProtein();
            totalPercentFat += dto.getTotalPercentFat();
            totalPercentCarbohydrate += dto.getTotalPercentCarbohydrate();
            totalPercentProtein += dto.getTotalPercentProtein();

            totalSleepDuration += dto.getSleepDurationDto().getSleepDurationSeconds();

            /*
            zeroValues.add(0.0);


            calculatedWeightDiffKg = dailyYesterday.getFsd().getTotalGramDiffWithActivity() - daily.getFsd().getTotalGramDiffWithActivity();
            calculatedWeightValues.add(calculatedWeightDiffKg);

            realWeightDiffKg = dailyYesterday.getFsd().getHumanBody().getWeightKg() - daily.getFsd().getHumanBody().getWeightKg();
            realWeightValues.add(realWeightDiffKg);

          


            dailyYesterday = daily;
            */
        }

        /*
        realWeightDirectionMap = DataDirectionDto.getBalanceMap(realWeightValues, zeroValues);
        realWeights.setDataValues(realWeightValues);
        realWeights.setBaseValues(zeroValues);
        realWeights.setMap(realWeightDirectionMap);

        calculatedWeightDirectionMap = DataDirectionDto.getBalanceMap(calculatedWeightValues, zeroValues);
        calculatedWeights.setDataValues(calculatedWeightValues);
        calculatedWeights.setBaseValues(zeroValues);
        calculatedWeights.setMap(calculatedWeightDirectionMap);
        */

        FoodSummaryDto beginDay = getDailyFoodSummary(startDateMillis);
        FoodSummaryDto endDay = getDailyFoodSummary(startDateMillis);

        double activityCorrectionFactor = settingHelper.getDouble("human.activityCorrectionFactor", 1.0);

        totalGramDiffWithActivityAndFactor = totalGramDiffWithActivity - endDay.getFsd().getTotalGramDiffWithActivity();
        totalGramDiffWithActivityAndFactor = totalGramDiffWithActivityAndFactor * activityCorrectionFactor;






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
        rangeDto.setTotalGramDiffWithActivityAndFactor(totalGramDiffWithActivityAndFactor);
        rangeDto.setActivityCorrectionFactor(activityCorrectionFactor);


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
        rangeDto.setAverageGramCarbohydrateKcal(totalGramCarbohydrateKcal / totalDays);
        rangeDto.setAverageGramProteinKcal(totalGramProteinKcal / totalDays);
        rangeDto.setAverageGramKcalFatCarbProtein(totalGramKcalFatCarbProtein / totalDays);
        rangeDto.setAveragePercentFat(totalPercentFat / totalDays);
        rangeDto.setAveragePercentCarbohydrate(totalPercentCarbohydrate / totalDays);
        rangeDto.setAveragePercentProtein(totalPercentProtein / totalDays);

        // gercekte ilk ve son tarih arasinda kac kilo alinip verildigini gosterir.
        rangeDto.setRealWeightDifference(rangeEndDayWeight - rangeStartDayWeight);

        double usdaSodium = 0.0;
        double usdaProteinGram = 0.0;
        double usdaCarbohydrateGram = 0.0;
        double usdaFatRequirementMinPercent = 0.0;
        double usdaFatRequirementMaxPercent = 0.0;
        double usdaCarbohydrateRequirementMinPercent = 0.0;
        double usdaCarbohydrateRequirementMaxPercent = 0.0;
        double usdaProteinRequirementMinPercent = 0.0;
        double usdaProteinRequirementMaxPercent = 0.0;
        Integer age = settingHelper.getInt("human.age", ageProp);
        Gender gender = settingHelper.getEnum("human.gender", Gender.class, Gender.MALE);
        List<NutrientLimit> nutrientLimits = nutrientRequirementService.getLimits(age, gender);
        for(NutrientLimit n : nutrientLimits) {
            switch (n.nutrientType()) {
                case SODIUM_MG -> usdaSodium = n.maxValue() / THOUSAND;
                case FAT_PERCENT -> {
                    usdaFatRequirementMinPercent = n.minValue();
                    usdaFatRequirementMaxPercent = n.maxValue();
                }
                case CARBOHYDRATE_GRAM -> usdaCarbohydrateGram = n.maxValue();
                case CARBOHYDRATE_PERCENT -> {
                    usdaCarbohydrateRequirementMinPercent = n.minValue();
                    usdaCarbohydrateRequirementMaxPercent = n.maxValue();
                }
                case PROTEIN_GRAM -> usdaProteinGram = n.maxValue();
                case PROTEIN_PERCENT -> {
                    usdaProteinRequirementMinPercent = n.minValue();
                    usdaProteinRequirementMaxPercent = n.maxValue();
                }
            }
        }

        rangeDto.setFatRequirementMaxPercent(usdaFatRequirementMaxPercent);
        rangeDto.setFatRequirementMinPercent(usdaFatRequirementMinPercent);
        //rangeDto.setFatRequirementGram(uf);

        rangeDto.setCarbohydrateRequirementMaxPercent(usdaCarbohydrateRequirementMaxPercent);
        rangeDto.setCarbohydrateRequirementMinPercent(usdaCarbohydrateRequirementMinPercent);
        rangeDto.setCarbohydrateRequirementGram(usdaCarbohydrateGram);


        rangeDto.setProteinRequirementMaxPercent(usdaProteinRequirementMaxPercent);
        rangeDto.setProteinRequirementMinPercent(usdaProteinRequirementMinPercent);
        rangeDto.setProteinRequirementGram(usdaProteinGram);

        rangeDto.setFiberRequirementGram( FIBER_REQ_PER_KCAL * rangeDto.getAverageKcal());

        rangeDto.setSodiumRequirementGram(usdaSodium);

        totalSleepDurationDto.setSleepDurationSeconds(totalSleepDuration);
        rangeDto.setTotalSleepDurationDto(totalSleepDurationDto);
        averageSleepDurationSeconds = totalSleepDuration / totalDays;
        averageSleepDurationDto.setSleepDurationSeconds(averageSleepDurationSeconds);
        rangeDto.setAverageSleepDurationDto(averageSleepDurationDto);

        //rangeDto.setCalculatedWeightDirections(calculatedWeights.getDirections());
        //rangeDto.setRealWeightDirections(realWeights.getDirections());


        List<TrendDirection> tdeeTrends = new ArrayList<>(); //1
        List<TrendDirection> kcalTrends = new ArrayList<>(); //2
        List<TrendDirection> kcalFoodDiffTrends = new ArrayList<>(); //3

        List<TrendDirection> kgFoodDiffTrends = new ArrayList<>(); //4
        List<TrendDirection> kcalActivityTrends = new ArrayList<>(); //5
        List<TrendDirection> kcalFoodDiffAndActivityTrends = new ArrayList<>(); //6

        List<TrendDirection> kgTheoraticallyDiffTrends = new ArrayList<>(); //7
        List<TrendDirection> kgBodyWeightTrends = new ArrayList<>(); //8
        List<TrendDirection> foodWeightTrends = new ArrayList<>(); //9

        List<TrendDirection> fatWeightTrends = new ArrayList<>(); //10
        List<TrendDirection> carbohydrateWeightTrends = new ArrayList<>(); //11
        List<TrendDirection> proteinWeightTrends = new ArrayList<>(); //12

        List<TrendDirection> fiberWeightTrends = new ArrayList<>(); //13
        List<TrendDirection> sodiumWeightTrends = new ArrayList<>(); //14
        List<TrendDirection> fatSaturatedWeightTrends = new ArrayList<>(); //15

        List<TrendDirection> sugarWeightTrends = new ArrayList<>(); //16
        List<TrendDirection> sleepDurationTrends = new ArrayList<>(); //17

        // average trends
        List<TrendDirection> avgTdeeTrends = new ArrayList<>(); //1
        List<TrendDirection> avgKcalTrends = new ArrayList<>(); //2
        List<TrendDirection> avgKcalFoodDiffTrends = new ArrayList<>(); //3

        List<TrendDirection> avgKgFoodDiffTrends = new ArrayList<>(); //4
        List<TrendDirection> avgKcalActivityTrends = new ArrayList<>(); //5
        List<TrendDirection> avgKcalFoodDiffAndActivityTrends = new ArrayList<>(); //6

        List<TrendDirection> avgKgTheoraticallyDiffTrends = new ArrayList<>(); //7
        List<TrendDirection> avgKgBodyWeightTrends = new ArrayList<>(); //8
        List<TrendDirection> avgFoodWeightTrends = new ArrayList<>(); //9

        List<TrendDirection> avgFatWeightTrends = new ArrayList<>(); //10
        List<TrendDirection> avgCarbohydrateWeightTrends = new ArrayList<>(); //11
        List<TrendDirection> avgProteinWeightTrends = new ArrayList<>(); //12

        List<TrendDirection> avgFiberWeightTrends = new ArrayList<>(); //13
        List<TrendDirection> avgSodiumWeightTrends = new ArrayList<>(); //14
        List<TrendDirection> avgFatSaturatedWeightTrends = new ArrayList<>(); //15

        List<TrendDirection> avgSugarWeightTrends = new ArrayList<>(); //16
        List<TrendDirection> avgSleepDurationTrends = new ArrayList<>(); //17

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // limit trends
        // TODO gunluk Aktivite limitleri, tarih araligindaki aktivite verilerinin dinamik ortalamasindan sabit katsayilar kadar uzaktadir. Simdilik boyle.
        double lmt5 = rangeDto.getAverageActivityKcal();
        double uLmtKcalActivity = lmt5 * 1.2;
        double dLmtKcalActivity = lmt5 * 0.8;

        double lmt9 = rangeDto.getAverageGram();
        double uLmtFoodWeight = lmt9 * 1.2;
        double dLmtFoodWeight = lmt9 * 0.8;

        List<TrendDirection> lmtTdeeTrends = new ArrayList<>(); //1
        List<TrendDirection> lmtKcalTrends = new ArrayList<>(); //2
        List<TrendDirection> lmtKcalFoodDiffTrends = new ArrayList<>(); //3

        List<TrendDirection> lmtKgFoodDiffTrends = new ArrayList<>(); //4
        List<TrendDirection> lmtKcalActivityTrends = new ArrayList<>(); //5
        List<TrendDirection> lmtKcalFoodDiffAndActivityTrends = new ArrayList<>(); //6

        List<TrendDirection> lmtKgTheoraticallyDiffTrends = new ArrayList<>(); //7
        List<TrendDirection> lmtKgBodyWeightTrends = new ArrayList<>(); //8
        List<TrendDirection> lmtFoodWeightTrends = new ArrayList<>(); //9

        List<TrendDirection> lmtFatWeightTrends = new ArrayList<>(); //10
        List<TrendDirection> lmtCarbohydrateWeightTrends = new ArrayList<>(); //11
        List<TrendDirection> lmtProteinWeightTrends = new ArrayList<>(); //12

        List<TrendDirection> lmtFiberWeightTrends = new ArrayList<>(); //13
        List<TrendDirection> lmtSodiumWeightTrends = new ArrayList<>(); //14
        List<TrendDirection> lmtFatSaturatedWeightTrends = new ArrayList<>(); //15

        List<TrendDirection> lmtSugarWeightTrends = new ArrayList<>(); //16
        List<TrendDirection> lmtSleepDurationTrends = new ArrayList<>(); //17

        FoodSummaryDto dailyYesterday = getDailyFoodSummary(startDateMillis - ONE_DAY_MILLIS);
        DailyFoodSummaryDto previous = dailyYesterday.getFsd();//null;
        for (DailyFoodSummaryDto current : dailyList) {

            if (previous == null) {
                current.setKcalTdeeTrend(TrendDirection.SAME); //1
                current.setKcalFoodTrend(TrendDirection.SAME); //2
                current.setKcalFoodDiffTrend(TrendDirection.SAME); //3

                current.setKgFoodDiffTrend(TrendDirection.SAME); //4
                current.setKcalActivityTrend(TrendDirection.SAME); //5
                current.setKcalVirtualWeightTrend(TrendDirection.SAME); //6

                current.setKgTheoreticalWeightTrend(TrendDirection.SAME); //7
                current.setBodyWeightTrend(TrendDirection.SAME); //8
                current.setFoodWeightTrend(TrendDirection.SAME); //9

                current.setFatWeightTrend(TrendDirection.SAME); //10
                current.setCarbohydrateWeightTrend(TrendDirection.SAME); //11
                current.setProteinWeightTrend(TrendDirection.SAME); //12

                current.setFiberWeightTrend(TrendDirection.SAME); //13
                current.setSodiumWeightTrend(TrendDirection.SAME); //14
                current.setFatSaturatedWeightTrend(TrendDirection.SAME); //15

                current.setSugarWeightTrend(TrendDirection.SAME); //16
                current.setSleepDurationTrend(TrendDirection.SAME); //17

            } else {
                current.setKcalTdeeTrend(TrendUtils.decideDirection(current.getHumanBody().getTdee(), previous.getHumanBody().getTdee()));
                tdeeTrends.add(current.getKcalTdeeTrend()); //1

                current.setKcalFoodTrend(TrendUtils.decideDirection(current.getTotalKcal(), previous.getTotalKcal()));
                kcalTrends.add(current.getKcalFoodTrend()); //2

                current.setKcalFoodDiffTrend(TrendUtils.decideDirection(current.getTotalKcalDiff(), previous.getTotalKcalDiff()));
                kcalFoodDiffTrends.add(current.getKcalFoodDiffTrend()); //3

                current.setKgFoodDiffTrend(TrendUtils.decideDirection(current.getTotalGramDiff(), previous.getTotalGramDiff()));
                kgFoodDiffTrends.add(current.getKgFoodDiffTrend()); //4

                current.setKcalActivityTrend(TrendUtils.decideDirection(current.getTotalActivityKcal(), previous.getTotalActivityKcal()));
                kcalActivityTrends.add(current.getKcalActivityTrend()); //5

                current.setKcalVirtualWeightTrend(TrendUtils.decideDirection(current.getTotalKcalDiffWithActivity(), previous.getTotalKcalDiffWithActivity()));
                kcalFoodDiffAndActivityTrends.add(current.getKcalVirtualWeightTrend()); //6

                current.setKgTheoreticalWeightTrend(TrendUtils.decideDirection(current.getTotalGramDiffWithActivity(), previous.getTotalGramDiffWithActivity()));
                kgTheoraticallyDiffTrends.add(current.getKcalVirtualWeightTrend()); //7

                current.setBodyWeightTrend(TrendUtils.decideDirection(current.getHumanBody().getWeightKg(), previous.getHumanBody().getWeightKg()));
                kgBodyWeightTrends.add(current.getBodyWeightTrend()); //8

                current.setFoodWeightTrend(TrendUtils.decideDirection(current.getTotalGram(), previous.getTotalGram()));
                foodWeightTrends.add(current.getFoodWeightTrend()); //9

                current.setFatWeightTrend(TrendUtils.decideDirection(current.getTotalGramFat(), previous.getTotalGramFat()));
                fatWeightTrends.add(current.getFatWeightTrend()); //10

                current.setCarbohydrateWeightTrend(TrendUtils.decideDirection(current.getTotalGramCarbohydrate(), previous.getTotalGramCarbohydrate()));
                carbohydrateWeightTrends.add(current.getCarbohydrateWeightTrend()); //11

                current.setProteinWeightTrend(TrendUtils.decideDirection(current.getTotalGramProtein(), previous.getTotalGramProtein()));
                proteinWeightTrends.add(current.getProteinWeightTrend()); //12

                current.setFiberWeightTrend(TrendUtils.decideDirection(current.getTotalGramFiber(), previous.getTotalGramFiber()));
                fiberWeightTrends.add(current.getFiberWeightTrend()); //13

                current.setSodiumWeightTrend(TrendUtils.decideDirection(current.getTotalGramSodium(), previous.getTotalGramSodium()));
                sodiumWeightTrends.add(current.getSodiumWeightTrend()); //14

                current.setFatSaturatedWeightTrend(TrendUtils.decideDirection(current.getTotalGramFatSaturated(), previous.getTotalGramFatSaturated()));
                fatSaturatedWeightTrends.add(current.getFatSaturatedWeightTrend()); //15

                current.setSugarWeightTrend(TrendUtils.decideDirection(current.getTotalGramSugar(), previous.getTotalGramSugar()));
                sugarWeightTrends.add(current.getSugarWeightTrend()); //16

                current.setSleepDurationTrend(TrendUtils.decideDirection(current.getSleepDurationDto().getSleepDurationSeconds(), previous.getSleepDurationDto().getSleepDurationSeconds()));
                sleepDurationTrends.add(current.getSleepDurationTrend()); //17


                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                //avg trends
                current.setAvgKcalTdeeTrend(TrendUtils.decideDirection(current.getHumanBody().getTdee(), rangeDto.getAverageKcalTdee()));
                avgTdeeTrends.add(current.getAvgKcalTdeeTrend()); //1

                current.setAvgKcalFoodTrend(TrendUtils.decideDirection(current.getTotalKcal(), rangeDto.getAverageKcal()));
                avgKcalTrends.add(current.getAvgKcalFoodTrend()); //2

                current.setAvgKcalFoodDiffTrend(TrendUtils.decideDirection(current.getTotalKcalDiff(), rangeDto.getAverageKcalDiff()));
                avgKcalFoodDiffTrends.add(current.getAvgKcalFoodDiffTrend()); //3

                current.setAvgKgFoodDiffTrend(TrendUtils.decideDirection(current.getTotalGramDiff(), rangeDto.getAverageGramDiff()));
                avgKgFoodDiffTrends.add(current.getAvgKgFoodDiffTrend()); //4

                current.setAvgKcalActivityTrend(TrendUtils.decideDirection(current.getTotalActivityKcal(), rangeDto.getAverageActivityKcal()));
                avgKcalActivityTrends.add(current.getAvgKcalActivityTrend()); //5

                current.setAvgKcalVirtualWeightTrend(TrendUtils.decideDirection(current.getTotalKcalDiffWithActivity(), rangeDto.getAverageKcalDiffWithActivity()));
                avgKcalFoodDiffAndActivityTrends.add(current.getAvgKcalVirtualWeightTrend()); //6

                current.setAvgKgTheoreticalWeightTrend(TrendUtils.decideDirection(current.getTotalGramDiffWithActivity(), rangeDto.getAverageGramDiffWithActivity()));
                avgKgTheoraticallyDiffTrends.add(current.getAvgKcalVirtualWeightTrend()); //7

                current.setAvgBodyWeightTrend(TrendUtils.decideDirection(current.getHumanBody().getWeightKg(), rangeDto.getAverageWeightKg()));
                avgKgBodyWeightTrends.add(current.getAvgBodyWeightTrend()); //8

                current.setAvgFoodWeightTrend(TrendUtils.decideDirection(current.getTotalGram(), rangeDto.getAverageGram()));
                avgFoodWeightTrends.add(current.getAvgFoodWeightTrend()); //9

                current.setAvgFatWeightTrend(TrendUtils.decideDirection(current.getTotalGramFat(), rangeDto.getAverageGramFat()));
                avgFatWeightTrends.add(current.getAvgFatWeightTrend()); //10

                current.setAvgCarbohydrateWeightTrend(TrendUtils.decideDirection(current.getTotalGramCarbohydrate(), rangeDto.getAverageGramCarbohydrate()));
                avgCarbohydrateWeightTrends.add(current.getAvgCarbohydrateWeightTrend()); //11

                current.setAvgProteinWeightTrend(TrendUtils.decideDirection(current.getTotalGramProtein(), rangeDto.getAverageGramProtein()));
                avgProteinWeightTrends.add(current.getAvgProteinWeightTrend()); //12

                current.setAvgFiberWeightTrend(TrendUtils.decideDirection(current.getTotalGramFiber(), rangeDto.getAverageGramFiber()));
                avgFiberWeightTrends.add(current.getAvgFiberWeightTrend()); //13

                current.setAvgSodiumWeightTrend(TrendUtils.decideDirection(current.getTotalGramSodium(), rangeDto.getAverageGramSodium()));
                avgSodiumWeightTrends.add(current.getAvgSodiumWeightTrend()); //14

                current.setAvgFatSaturatedWeightTrend(TrendUtils.decideDirection(current.getTotalGramFatSaturated(), rangeDto.getAverageGramFatSaturated()));
                avgFatSaturatedWeightTrends.add(current.getAvgFatSaturatedWeightTrend()); //15

                current.setAvgSugarWeightTrend(TrendUtils.decideDirection(current.getTotalGramSugar(), rangeDto.getAverageGramSugar()));
                avgSugarWeightTrends.add(current.getAvgSugarWeightTrend()); //16

                current.setAvgSleepDurationTrend(TrendUtils.decideDirection(current.getSleepDurationDto().getSleepDurationSeconds(), averageSleepDurationSeconds));
                avgSleepDurationTrends.add(current.getAvgSleepDurationTrend()); //17

                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                //limit trends
                double col1 = current.getHumanBody().getTdee();
                current.setLmtKcalTdeeTrend(TrendUtils.decideDirection2(col1, current.getHumanBody().getBmi(),col1 * 1.375));
                lmtTdeeTrends.add(current.getLmtKcalTdeeTrend()); //1

                double col2 = current.getTotalKcal();
                double uLmtKcalTdee = col1 * 1.2;
                double dLmtKcalTdee = col1 * 0.8;



                current.setLmtKcalFoodTrend(TrendUtils.decideDirection2(col2, dLmtKcalTdee,uLmtKcalTdee));
                lmtKcalTrends.add(current.getLmtKcalFoodTrend()); //2

                current.setLmtKcalFoodDiffTrend(TrendUtils.decideDirection(current.getTotalKcalDiff(), rangeDto.getAverageKcalDiff()));
                lmtKcalFoodDiffTrends.add(current.getLmtKcalFoodDiffTrend()); //3

                current.setLmtKgFoodDiffTrend(TrendUtils.decideDirection(current.getTotalGramDiff(), rangeDto.getAverageGramDiff()));
                lmtKgFoodDiffTrends.add(current.getLmtKgFoodDiffTrend()); //4



                current.setLmtKcalActivityTrend(TrendUtils.decideDirection2(current.getTotalActivityKcal(), dLmtKcalActivity, uLmtKcalActivity));
                lmtKcalActivityTrends.add(current.getLmtKcalActivityTrend()); //5

                current.setLmtKcalVirtualWeightTrend(TrendUtils.decideDirection(current.getTotalKcalDiffWithActivity(), rangeDto.getAverageKcalDiffWithActivity()));
                lmtKcalFoodDiffAndActivityTrends.add(current.getLmtKcalVirtualWeightTrend()); //6

                current.setLmtKgTheoreticalWeightTrend(TrendUtils.decideDirection(current.getTotalGramDiffWithActivity(), rangeDto.getAverageGramDiffWithActivity()));
                lmtKgTheoraticallyDiffTrends.add(current.getLmtKcalVirtualWeightTrend()); //7

                /*
                18,5 altı	Zayıf
                18,5 – 24,9	Normal kilolu
                25,0 – 29,9	Fazla kilolu
                30,0 – 34,9	Obez (Sınıf I)
                35,0 – 39,9	Obez (Sınıf II)
                40 ve üzeri	İleri derece obez
                 */
                double col8 = current.getHumanBody().getBmi();
                double uLmtKgBodyWeight = BODY_MASS_INDEX_CATEGORY.getMaxValue();
                double dLmtKgBodyWeight = BODY_MASS_INDEX_CATEGORY.getMinValue();
                current.setLmtBodyWeightTrend(TrendUtils.decideDirection2(col8, dLmtKgBodyWeight, uLmtKgBodyWeight));
                lmtKgBodyWeightTrends.add(current.getLmtBodyWeightTrend()); //8

                current.setLmtFoodWeightTrend(TrendUtils.decideDirection2(current.getTotalGram(), dLmtFoodWeight, uLmtFoodWeight));
                lmtFoodWeightTrends.add(current.getLmtFoodWeightTrend()); //9

                double col10 = current.getTotalPercentFat();
                current.setLmtFatWeightTrend(TrendUtils.decideDirection2(col10, rangeDto.getFatRequirementMinPercent(), rangeDto.getFatRequirementMaxPercent()));
                lmtFatWeightTrends.add(current.getLmtFatWeightTrend()); //10

                double col11 = current.getTotalPercentCarbohydrate();
                current.setLmtCarbohydrateWeightTrend(TrendUtils.decideDirection2(col11, rangeDto.getCarbohydrateRequirementMinPercent() ,rangeDto.getCarbohydrateRequirementMaxPercent()));
                lmtCarbohydrateWeightTrends.add(current.getLmtCarbohydrateWeightTrend()); //11

                double col12 = current.getTotalPercentProtein();
                current.setLmtProteinWeightTrend(TrendUtils.decideDirection2(col12, rangeDto.getProteinRequirementMinPercent(), rangeDto.getProteinRequirementMaxPercent()));
                lmtProteinWeightTrends.add(current.getLmtProteinWeightTrend()); //12

                current.setLmtFiberWeightTrend(TrendUtils.decideDirection(current.getTotalGramFiber(), rangeDto.getAverageGramFiber()));
                lmtFiberWeightTrends.add(current.getLmtFiberWeightTrend()); //13

                current.setLmtSodiumWeightTrend(TrendUtils.decideDirection(current.getTotalGramSodium(), rangeDto.getAverageGramSodium()));
                lmtSodiumWeightTrends.add(current.getLmtSodiumWeightTrend()); //14

                current.setLmtFatSaturatedWeightTrend(TrendUtils.decideDirection(current.getTotalGramFatSaturated(), rangeDto.getAverageGramFatSaturated()));
                lmtFatSaturatedWeightTrends.add(current.getLmtFatSaturatedWeightTrend()); //15

                current.setLmtSugarWeightTrend(TrendUtils.decideDirection(current.getTotalGramSugar(), rangeDto.getAverageGramSugar()));
                lmtSugarWeightTrends.add(current.getLmtSugarWeightTrend()); //16

                current.setLmtSleepDurationTrend(TrendUtils.decideDirection(current.getSleepDurationDto().getSleepDurationSeconds(), averageSleepDurationSeconds));
                lmtSleepDurationTrends.add(current.getLmtSleepDurationTrend()); //17

            }
            previous = current;
        }

        rangeDto.setTdeeTrendsSummary(TrendUtils.summarize(tdeeTrends)); //1 kcal ihtiyac
        rangeDto.setKcalTrendsSummary(TrendUtils.summarize(kcalTrends)); //2 kcal gida
        rangeDto.setKcalDiffTrendsSummary(TrendUtils.summarize(kcalFoodDiffTrends)); //3 kcal gida fark

        rangeDto.setKgDiffTrendsSummary(TrendUtils.summarize(kgFoodDiffTrends)); //4 kg gida fark
        rangeDto.setKcalActivityTrendsSummary(TrendUtils.summarize(kcalActivityTrends)); //5 kcal aktivite
        rangeDto.setKcalVirtualWeightTrendsSummary(TrendUtils.summarize(kcalFoodDiffAndActivityTrends)); // 6 kcal teorik fark

        rangeDto.setKgVirtualWeightTrendsSummary(TrendUtils.summarize(kgTheoraticallyDiffTrends)); //7
        rangeDto.setKgBodyWeightTrendsSummary(TrendUtils.summarize(kgBodyWeightTrends)); //8 kg gercek vucut agirligi
        rangeDto.setFoodWeightTrendsSummary(TrendUtils.summarize(foodWeightTrends)); //9 gr gramaj toplam gida agirligi

        rangeDto.setFatWeightTrendsSummary(TrendUtils.summarize(fatWeightTrends)); //10 gr yag
        rangeDto.setCarbohydrateWeightTrendsSummary(TrendUtils.summarize(carbohydrateWeightTrends)); //11 gr karbonhidrat
        rangeDto.setProteinWeightTrendsSummary(TrendUtils.summarize(proteinWeightTrends)); //12 gr protein

        rangeDto.setFiberWeightTrendsSummary(TrendUtils.summarize(fiberWeightTrends)); //13 gr lif
        rangeDto.setSodiumWeightTrendsSummary(TrendUtils.summarize(sodiumWeightTrends)); //14 gr sodyum
        rangeDto.setFatSaturatedWeightTrendsSummary(TrendUtils.summarize(fatSaturatedWeightTrends)); //15 doymus yag

        rangeDto.setSugarWeightTrendsSummary(TrendUtils.summarize(sugarWeightTrends)); //16 gr seker
        rangeDto.setSleepDurationTrendsSummary(TrendUtils.summarize(sleepDurationTrends)); //17 sn uyku

        // AVERAGE TRENDS
        rangeDto.setAvgTdeeTrendsSummary(TrendUtils.summarize(avgTdeeTrends)); //1 kcal ihtiyac
        rangeDto.setAvgKcalTrendsSummary(TrendUtils.summarize(avgKcalTrends)); //2 kcal gida
        rangeDto.setAvgKcalDiffTrendsSummary(TrendUtils.summarize(avgKcalFoodDiffTrends)); //3 kcal gida fark

        rangeDto.setAvgKgDiffTrendsSummary(TrendUtils.summarize(avgKgFoodDiffTrends)); //4 kg gida fark
        rangeDto.setAvgKcalActivityTrendsSummary(TrendUtils.summarize(avgKcalActivityTrends)); //5 kcal aktivite
        rangeDto.setAvgKcalVirtualWeightTrendsSummary(TrendUtils.summarize(avgKcalFoodDiffAndActivityTrends)); // 6 kcal teorik fark

        rangeDto.setAvgKgVirtualWeightTrendsSummary(TrendUtils.summarize(avgKgTheoraticallyDiffTrends)); //7
        rangeDto.setAvgKgBodyWeightTrendsSummary(TrendUtils.summarize(avgKgBodyWeightTrends)); //8 kg gercek vucut agirligi
        rangeDto.setAvgFoodWeightTrendsSummary(TrendUtils.summarize(avgFoodWeightTrends)); //9 gr gramaj toplam gida agirligi

        rangeDto.setAvgFatWeightTrendsSummary(TrendUtils.summarize(avgFatWeightTrends)); //10 gr yag
        rangeDto.setAvgCarbohydrateWeightTrendsSummary(TrendUtils.summarize(avgCarbohydrateWeightTrends)); //11 gr karbonhidrat
        rangeDto.setAvgProteinWeightTrendsSummary(TrendUtils.summarize(avgProteinWeightTrends)); //12 gr protein

        rangeDto.setAvgFiberWeightTrendsSummary(TrendUtils.summarize(avgFiberWeightTrends)); //13 gr lif
        rangeDto.setAvgSodiumWeightTrendsSummary(TrendUtils.summarize(avgSodiumWeightTrends)); //14 gr sodyum
        rangeDto.setAvgFatSaturatedWeightTrendsSummary(TrendUtils.summarize(avgFatSaturatedWeightTrends)); //15 doymus yag

        rangeDto.setAvgSugarWeightTrendsSummary(TrendUtils.summarize(avgSugarWeightTrends)); //16 gr seker
        rangeDto.setAvgSleepDurationTrendsSummary(TrendUtils.summarize(avgSleepDurationTrends)); //17 sn uyku

        // LIMIT TRENDS

        rangeDto.setULmtKcalActivity(uLmtKcalActivity);
        rangeDto.setDLmtKcalActivity(dLmtKcalActivity);

        rangeDto.setULmtFoodWeight(uLmtFoodWeight);
        rangeDto.setDLmtFoodWeight(dLmtFoodWeight);

        rangeDto.setLmtKcalTdeeTrendsSummary(TrendUtils.summarize(lmtTdeeTrends)); //1 kcal ihtiyac
        rangeDto.setLmtKcalTrendsSummary(TrendUtils.summarize(lmtKcalTrends)); //2 kcal gida
        rangeDto.setLmtKcalDiffTrendsSummary(TrendUtils.summarize(lmtKcalFoodDiffTrends)); //3 kcal gida fark

        rangeDto.setLmtKgDiffTrendsSummary(TrendUtils.summarize(lmtKgFoodDiffTrends)); //4 kg gida fark
        rangeDto.setLmtKcalActivityTrendsSummary(TrendUtils.summarize(lmtKcalActivityTrends)); //5 kcal aktivite
        rangeDto.setLmtKcalVirtualWeightTrendsSummary(TrendUtils.summarize(lmtKcalFoodDiffAndActivityTrends)); // 6 kcal teorik fark

        rangeDto.setLmtKgVirtualWeightTrendsSummary(TrendUtils.summarize(lmtKgTheoraticallyDiffTrends)); //7
        rangeDto.setLmtKgBodyWeightTrendsSummary(TrendUtils.summarize(lmtKgBodyWeightTrends)); //8 kg gercek vucut agirligi
        rangeDto.setLmtFoodWeightTrendsSummary(TrendUtils.summarize(lmtFoodWeightTrends)); //9 gr gramaj toplam gida agirligi

        rangeDto.setLmtFatWeightTrendsSummary(TrendUtils.summarize(lmtFatWeightTrends)); //10 gr yag
        rangeDto.setLmtCarbohydrateWeightTrendsSummary(TrendUtils.summarize(lmtCarbohydrateWeightTrends)); //11 gr karbonhidrat
        rangeDto.setLmtProteinWeightTrendsSummary(TrendUtils.summarize(lmtProteinWeightTrends)); //12 gr protein

        rangeDto.setLmtFiberWeightTrendsSummary(TrendUtils.summarize(lmtFiberWeightTrends)); //13 gr lif
        rangeDto.setLmtSodiumWeightTrendsSummary(TrendUtils.summarize(lmtSodiumWeightTrends)); //14 gr sodyum
        rangeDto.setLmtFatSaturatedWeightTrendsSummary(TrendUtils.summarize(lmtFatSaturatedWeightTrends)); //15 doymus yag

        rangeDto.setLmtSugarWeightTrendsSummary(TrendUtils.summarize(lmtSugarWeightTrends)); //16 gr seker
        rangeDto.setLmtSleepDurationTrendsSummary(TrendUtils.summarize(lmtSleepDurationTrends)); //17 sn uyku



        rangeDto.setDays(dailyList);

        List<FoodConsumptionDto> foodConsumptionDtosByCategory = getFoodListByDateRangeSummaryAndCategories(startDateMillis, endDateMillis);
        rangeDto.setFoodConsumptionDtosByCategory(foodConsumptionDtosByCategory);

        List<FoodConsumptionDto> foodConsumptionDtos = getFoodListByDateRangeSummary(startDateMillis, endDateMillis);
        rangeDto.setFoodConsumptionDtos(foodConsumptionDtos);

        // Data Descriptions
        String activityLevelString = settingHelper.get("human.activityLevel", "activity level is not defined");
        rangeDto.setKcalTdeeDataDescription(String.format("Günlük alınması gereken kalori(TDEE). BMR, Mifflin-St Jeor formülüne göre hesaplanır. Seçmiş olduğunuz aktivite seviyesi: %s", ActivityLevel.valueOf(activityLevelString)));
        rangeDto.setKcalConsumptionDataDescription(String.format("Günlük tüketilmiş kalori miktarı."));
        rangeDto.setKcalFoodDiffDataDescription(String.format("Günlük alınması gereken kalori ile gerçekte tüketilmiş kalori arasındaki fark."));
        rangeDto.setKgFoodDiffDataDescription(String.format("Gıda yoluyla teorik olarak ileride kaç kg eksik veya fazla çıkacağı."));
        rangeDto.setKcalActivityDataDescription(String.format("Normalin dışında, fazladan fiziksel aktiviteler yoluyla kaybedilen kalori miktarı."));
        rangeDto.setKcalTeoricalDiffDataDescription(String.format("Teorik olarak günlük gıda tüketimi ve aktiveler sonrasında hesaplanan kalorinin, günlük alınması gereken kaloriye göre farkı."));
        rangeDto.setKgTeoricalDiffDataDescription(String.format("Teorik olarak gıda tüketimine ek olarak aktiviteleri de dahil edince ileride kaç kg eksik veya fazla oluşacağı."));
        rangeDto.setBodyWeightDataDescription(String.format("Vücut kitle endeksi %s ile %s aralığında kaslı olmayanlar için %s sayılır.", BODY_MASS_INDEX_CATEGORY.getMinValue(), BODY_MASS_INDEX_CATEGORY.getMaxValue(), BODY_MASS_INDEX_CATEGORY.getDisplayName()));
        rangeDto.setFoodWeightDataDescription(String.format("Şimdilik %s ile %s gram arasında yenilebilir gıda gerektiğini varsayıyoruz.", 0, 0));
        rangeDto.setFatDataDescription(String.format("%%%s ile %%%s arasında yağ gerekiyor.", usdaFatRequirementMinPercent, usdaFatRequirementMaxPercent));
        rangeDto.setCarbohydrateDataDescription(String.format("%%%s ile %%%s arasında ve en az %s gram karbonhidrat gerekiyor.", usdaCarbohydrateRequirementMinPercent, usdaCarbohydrateRequirementMaxPercent, usdaCarbohydrateGram));
        rangeDto.setProteinDataDescription(String.format("%%%s ile %%%s arasında ve en az %s gram protein gerekiyor.", usdaProteinRequirementMinPercent, usdaProteinRequirementMaxPercent, usdaProteinGram));
        rangeDto.setFiberDataDescription("TODO: Fiber miktarı her 1000 kcal için 14 gr olmalıdır. Şimdilik yaşa göre olan miktar uygulanmamıştır.");
        rangeDto.setSodiumDataDescription("TODO: Şimdilik ikiye bölmek mantıklı olur çünkü sodyumu değil tuz miktarını gösteriyor çoğunlukla.");
        rangeDto.setFatSaturatedDataDescription("Doymuş yağ ile ilgili henüz mantıklı bir limit hesaplaması bulunmamaktadır. Ortalama değer üzerinden hardcode edilmiş bir katsayı kullanılmıştır.");
        rangeDto.setSugarDataDescription("Şeker gramajı ile ilgili henüz mantıklı bir hesaplama bulunmamaktadır. Ortalamanın üstü ve altı için hardcoded bir katsayı kullanılmıştır.");
        rangeDto.setSleepDataDescription("Uyku verisi için şimdilik limitler minimum 6 max 8 saat olarak hardcode edilmiştir.");

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


    public List<FoodConsumptionDto> getFoodListByDateRangeSummary(Long startDateMillis, Long endDateMillis) {

        Map<Long, Double> foodWeights = new HashMap<>();
        Map<Long, String> foodNames = new HashMap<>();
        Map<Long, String> categoryNames = new HashMap<>();
        Map<Long, Long> categoryIds = new HashMap<>();

        // gün gün dolaş
        for (long date = startDateMillis; date <= endDateMillis; date += ONE_DAY_MILLIS) {
            //totalDays++;
            FoodSummaryDto daily = getDailyFoodSummary(date);
            for (FoodEntryDto fsd : daily.getItems()) {
                long topicId = fsd.getTopicId();
                foodWeights.merge(topicId, fsd.getGram(), Double::sum);
                foodNames.computeIfAbsent(topicId, k -> fsd.getTopicName());
                categoryNames.computeIfAbsent(topicId, k -> fsd.getCategoryName());
                categoryIds.computeIfAbsent(topicId, k -> fsd.getCategoryId());
            }
        }

        List<FoodConsumptionDto> foodConsumptionDtos = new ArrayList<>();
        for (long topicId : foodWeights.keySet()) {
            FoodConsumptionDto fcd = new FoodConsumptionDto();
            fcd.setTopicId(topicId);
            fcd.setFoodWeight(foodWeights.get(topicId));
            fcd.setCategoryId(categoryIds.get(topicId));
            fcd.setCategoryName(categoryNames.get(topicId));
            fcd.setTopicName(foodNames.get(topicId));
            foodConsumptionDtos.add(fcd);
        }

        //kalici sort
        foodConsumptionDtos.sort(
                Comparator.comparing(FoodConsumptionDto::getFoodWeight).reversed()
        );

        // gecici sort ve konsola yazdirmak
        //foodConsumptionDtos.stream()
        //        .sorted(Comparator.comparing(FoodConsumptionDto::getFoodWeight).reversed())
        //        .forEach(System.out::println);


        return foodConsumptionDtos;
    }

    public List<FoodConsumptionDto> getFoodListByDateRangeSummaryAndCategories(Long startDateMillis, Long endDateMillis) {

        Map<Long, Double> foodWeights = new HashMap<>();
        Map<Long, Set<String>> foodNameSets = new HashMap<>(); // set kullanmamizin sebebi, topic isimlerinin yani food namelerin tekrar etmemesi.
        Map<Long, String> categoryNames = new HashMap<>();
        Map<Long, Long> topicIds = new HashMap<>();

        // gün gün dolaş
        for (long date = startDateMillis; date <= endDateMillis; date += ONE_DAY_MILLIS) {
            FoodSummaryDto daily = getDailyFoodSummary(date);

            for (FoodEntryDto fsd : daily.getItems()) {
                long categoryId = fsd.getCategoryId();

                foodWeights.merge(categoryId, fsd.getGram(), Double::sum);

                foodNameSets
                        .computeIfAbsent(categoryId, k -> new LinkedHashSet<>())
                        .add(fsd.getTopicName());

                categoryNames.computeIfAbsent(categoryId, k -> fsd.getCategoryName());
                topicIds.computeIfAbsent(categoryId, k -> fsd.getTopicId());
            }
        }

        List<FoodConsumptionDto> foodConsumptionDtos = new ArrayList<>();
        for (long categoryId : foodWeights.keySet()) {
            FoodConsumptionDto fcd = new FoodConsumptionDto();
            fcd.setTopicId(topicIds.get(categoryId));
            fcd.setFoodWeight(foodWeights.get(categoryId));
            fcd.setCategoryId(categoryId);
            fcd.setCategoryName(categoryNames.get(categoryId));
            fcd.setTopicName(foodNameSets.get(categoryId).toString());
            foodConsumptionDtos.add(fcd);
        }

        //kalici sort
        foodConsumptionDtos.sort(
                Comparator.comparing(FoodConsumptionDto::getFoodWeight).reversed()
        );

        // gecici sort ve konsola yazdirmak
        //foodConsumptionDtos.stream()
        //        .sorted(Comparator.comparing(FoodConsumptionDto::getFoodWeight).reversed())
        //        .forEach(System.out::println);


        return foodConsumptionDtos;
    }


}



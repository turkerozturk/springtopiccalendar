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
package com.turkerozturk.dtt.dto;


import com.turkerozturk.dtt.helper.datatrends.TrendDirection;
import lombok.Data;

@Data
public class DailyFoodSummaryDto {

    private Long dateMillis;

    private Double totalKcal;
    private Double totalGram;

    private Double totalGramFat;
    private Double totalGramCarbohydrate;
    private Double totalGramProtein;

    private Double totalGramFiber;
    private Double totalGramSodium;
    private Double totalGramFatSaturated;
    private Double totalGramSugar;

    private Double totalGramWater;

    private Double totalGramFatKcal;
    private Double totalGramCarbohydrateKcal;
    private Double totalGramProteinKcal;

    private Double totalGramKcalFatCarbProtein;


    private Double totalPercentFat;
    private Double totalPercentCarbohydrate;
    private Double totalPercentProtein;

    //private Double totalPercentFatSaturatedUpperLimit;
    //private Double totalPercentSugarUpperLimit;


    private Double totalKcalByStatus0;
    private Double totalGramByStatus0;
    private Double totalGramFatByStatus0;
    private Double totalGramCarbohydrateByStatus0;
    private Double totalGramProteinByStatus0;
    private Double totalGramFiberByStatus0;
    private Double totalGramSodiumByStatus0;
    private Double totalGramFatSaturatedByStatus0;
    private Double totalGramSugarByStatus0;

    private Double totalKcalByStatus1;
    private Double totalGramByStatus1;
    private Double totalGramFatByStatus1;
    private Double totalGramCarbohydrateByStatus1;
    private Double totalGramProteinByStatus1;
    private Double totalGramFiberByStatus1;
    private Double totalGramSodiumByStatus1;
    private Double totalGramFatSaturatedByStatus1;
    private Double totalGramSugarByStatus1;

    private Double totalKcalByStatus2;
    private Double totalGramByStatus2;
    private Double totalGramFatByStatus2;
    private Double totalGramCarbohydrateByStatus2;
    private Double totalGramProteinByStatus2;
    private Double totalGramFiberByStatus2;
    private Double totalGramSodiumByStatus2;
    private Double totalGramFatSaturatedByStatus2;
    private Double totalGramSugarByStatus2;

    private NutritionResultDto humanBody;

    private Double totalKcalDiff;
    private Double totalGramDiff;

    private Double totalActivityKcal;

    private Double totalKcalDiffWithActivity;
    private Double totalGramDiffWithActivity;

    private SleepDurationDto sleepDurationDto;

    private Double fiberRequirementGram;
    private Double sodiumRequirementGram;


    private Double fatRequirementGram;
    private Double fatRequirementMinPercent;
    private Double fatRequirementMaxPercent;

    private Double carbohydrateRequirementGram;
    private Double carbohydrateRequirementMinPercent;
    private Double carbohydrateRequirementMaxPercent;

    private Double proteinRequirementGram;
    private Double proteinRequirementMinPercent;
    private Double proteinRequirementMaxPercent;

    private Double fatSaturatedRequirementGram;
    private Double fatSaturatedRequirementMinPercent;
    private Double fatSaturatedRequirementMaxPercent;


    // TODO Artik varolan sutun verilerini de asagidaki yapilara aktaracagim:
    private TrendDirection kcalTdeeTrend;
    private TrendDirection kcalFoodTrend;
    private TrendDirection kcalFoodDiffTrend;
    private TrendDirection kgFoodDiffTrend;
    private TrendDirection kcalActivityTrend;
    private TrendDirection kcalVirtualWeightTrend;
    private TrendDirection kgTheoreticalWeightTrend;
    private TrendDirection bodyWeightTrend;
    private TrendDirection foodWeightTrend;
    private TrendDirection fatWeightTrend;
    private TrendDirection carbohydrateWeightTrend;
    private TrendDirection proteinWeightTrend;
    private TrendDirection fiberWeightTrend;
    private TrendDirection sodiumWeightTrend;
    private TrendDirection fatSaturatedWeightTrend;
    private TrendDirection sugarWeightTrend;
    private TrendDirection sleepDurationTrend;


}

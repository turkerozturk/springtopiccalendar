package com.turkerozturk.dtt.dto;

import com.turkerozturk.dtt.helper.datatrends.TrendSummaryDto;
import lombok.Data;

import java.util.List;

@Data
public class DateRangeFoodSummaryDto {

    private List<DailyFoodSummaryDto> days;

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

    private Double totalKcalDiff;
    private Double totalGramDiff;

    private Double totalBodyWeightDiff;

    private Double totalActivityKcal;
    private Double totalKcalTdee;

    private Double totalDays;

    private Double totalKcalDiffWithActivity;
    private Double totalGramDiffWithActivity;
    private Double totalGramDiffWithActivityAndFactor;
    private Double activityCorrectionFactor;



    private Double averageKcal;
    private Double averageGram;

    private Double averageGramFat;
    private Double averageGramCarbohydrate;
    private Double averageGramProtein;

    private Double averageGramFiber;
    private Double averageGramSodium;
    private Double averageGramFatSaturated;
    private Double averageGramSugar;

    private Double averageGramWater;

    private Double averageKcalDiff;
    private Double averageGramDiff;

    private Double averageWeightKg;
    private Double averageBodyWeightDiff;

    private Double averageActivityKcal;
    private Double averageKcalTdee;

    private Double averageKcalDiffWithActivity;
    private Double averageGramDiffWithActivity;

    private Double averageGramFatKcal;
    private Double averageGramCarbohydrateKcal;
    private Double averageGramProteinKcal;
    private Double averageGramKcalFatCarbProtein;
    private Double averagePercentFat;
    private Double averagePercentCarbohydrate;
    private Double averagePercentProtein;

    private Double realWeightDifference;

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

    private SleepDurationDto totalSleepDurationDto;
    private SleepDurationDto averageSleepDurationDto;

    // Trend Summarries
    private TrendSummaryDto tdeeTrendsSummary;
    private TrendSummaryDto kcalTrendsSummary;
    private TrendSummaryDto kcalDiffTrendsSummary;

    private TrendSummaryDto kgDiffTrendsSummary;
    private TrendSummaryDto kcalActivityTrendsSummary;
    private TrendSummaryDto kcalVirtualWeightTrendsSummary;

    private TrendSummaryDto kgVirtualWeightTrendsSummary;
    private TrendSummaryDto kgBodyWeightTrendsSummary;

    private TrendSummaryDto foodWeightTrendsSummary;
    private TrendSummaryDto fatWeightTrendsSummary;
    private TrendSummaryDto carbohydrateWeightTrendsSummary;

    private TrendSummaryDto proteinWeightTrendsSummary;
    private TrendSummaryDto fiberWeightTrendsSummary;
    private TrendSummaryDto sodiumWeightTrendsSummary;

    private TrendSummaryDto fatSaturatedWeightTrendsSummary;
    private TrendSummaryDto sugarWeightTrendsSummary;
    private TrendSummaryDto sleepDurationTrendsSummary;

    // Average Trend Summarries
    private TrendSummaryDto avgTdeeTrendsSummary;
    private TrendSummaryDto avgKcalTrendsSummary;
    private TrendSummaryDto avgKcalDiffTrendsSummary;

    private TrendSummaryDto avgKgDiffTrendsSummary;
    private TrendSummaryDto avgKcalActivityTrendsSummary;
    private TrendSummaryDto avgKcalVirtualWeightTrendsSummary;

    private TrendSummaryDto avgKgVirtualWeightTrendsSummary;
    private TrendSummaryDto avgKgBodyWeightTrendsSummary;

    private TrendSummaryDto avgFoodWeightTrendsSummary;
    private TrendSummaryDto avgFatWeightTrendsSummary;
    private TrendSummaryDto avgCarbohydrateWeightTrendsSummary;

    private TrendSummaryDto avgProteinWeightTrendsSummary;
    private TrendSummaryDto avgFiberWeightTrendsSummary;
    private TrendSummaryDto avgSodiumWeightTrendsSummary;

    private TrendSummaryDto avgFatSaturatedWeightTrendsSummary;
    private TrendSummaryDto avgSugarWeightTrendsSummary;
    private TrendSummaryDto avgSleepDurationTrendsSummary;

    // Limit Summaries
    private TrendSummaryDto lmtKcalTdeeTrendsSummary;
    private TrendSummaryDto lmtKcalTrendsSummary;
    private TrendSummaryDto lmtKcalDiffTrendsSummary;

    private TrendSummaryDto lmtKgDiffTrendsSummary;
    private TrendSummaryDto lmtKcalActivityTrendsSummary;
    private TrendSummaryDto lmtKcalVirtualWeightTrendsSummary;

    private TrendSummaryDto lmtKgVirtualWeightTrendsSummary;
    private TrendSummaryDto lmtKgBodyWeightTrendsSummary;

    private TrendSummaryDto lmtFoodWeightTrendsSummary;
    private TrendSummaryDto lmtFatWeightTrendsSummary;
    private TrendSummaryDto lmtCarbohydrateWeightTrendsSummary;

    private TrendSummaryDto lmtProteinWeightTrendsSummary;
    private TrendSummaryDto lmtFiberWeightTrendsSummary;
    private TrendSummaryDto lmtSodiumWeightTrendsSummary;

    private TrendSummaryDto lmtFatSaturatedWeightTrendsSummary;
    private TrendSummaryDto lmtSugarWeightTrendsSummary;
    private TrendSummaryDto lmtSleepDurationTrendsSummary;

    private double uLmtKcalActivity;
    private double dLmtKcalActivity;

    private double uLmtFoodWeight;
    private double dLmtFoodWeight;


    private String kcalTdeeDataDescription;
    private String kcalConsumptionDataDescription;
    private String kcalFoodDiffDataDescription;
    private String kgFoodDiffDataDescription;
    private String kcalActivityDataDescription;
    private String kcalTeoricalDiffDataDescription;
    private String kgTeoricalDiffDataDescription;
    private String bodyWeightDataDescription;
    private String foodWeightDataDescription;
    private String fatDataDescription;
    private String carbohydrateDataDescription;
    private String proteinDataDescription;
    private String fiberDataDescription;
    private String sodiumDataDescription;
    private String fatSaturatedDataDescription;
    private String sugarDataDescription;
    private String sleepDataDescription;



    List<FoodConsumptionDto> foodConsumptionDtos;
    List<FoodConsumptionDto> foodConsumptionDtosByCategory;



}

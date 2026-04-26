package com.turkerozturk.dtt.dto;

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


}

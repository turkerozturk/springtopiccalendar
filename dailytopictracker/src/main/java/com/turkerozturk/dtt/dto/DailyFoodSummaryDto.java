package com.turkerozturk.dtt.dto;


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

}

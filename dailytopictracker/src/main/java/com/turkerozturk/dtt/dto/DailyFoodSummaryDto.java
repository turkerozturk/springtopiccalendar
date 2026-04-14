package com.turkerozturk.dtt.dto;


import lombok.AllArgsConstructor;
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


    private Double totalKcalByStatus;
    private Double totalGramByStatus;
    private Double totalGramFatByStatus;
    private Double totalGramCarbohydrateByStatus;
    private Double totalGramProteinByStatus;
    private Double totalGramFiberByStatus;
    private Double totalGramSodiumByStatus;
    private Double totalGramFatSaturatedByStatus;
    private Double totalGramSugarByStatus;


}

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

    private Double averageWeightKg;
    private Double totalBodyWeightDiff;


}

package com.turkerozturk.dtt.dto;

import lombok.Data;

import java.util.List;

@Data
public class FoodConsumptionDtosByCategoryDto {

    private List<FoodConsumptionDto> foodConsumptionDtosByCategory;
    private int totalCategoriesCount;
    private int totalFoodsCount;
    private double totalFoodWeightsCount;
    private int totalDaysCount;




}

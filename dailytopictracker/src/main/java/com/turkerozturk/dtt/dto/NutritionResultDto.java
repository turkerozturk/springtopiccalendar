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

import lombok.Data;

@Data
public class NutritionResultDto {

    private double bmr;              // bazal metabolizma
    private double tdee;             // günlük kalori ihtiyacı
    private double weightLossCalories;
    private double weightGainCalories;

    private double proteinGrams;
    private double fatGrams;
    private double carbGrams;

    private double bmi;
    private String bmiCategory;

    public NutritionResultDto(double bmr, double tdee,
                              double weightLossCalories,
                              double weightGainCalories,
                              double proteinGrams,
                              double fatGrams,
                              double carbGrams,
                              double bmi,
                              String bmiCategory) {
        this.bmr = bmr;
        this.tdee = tdee;
        this.weightLossCalories = weightLossCalories;
        this.weightGainCalories = weightGainCalories;
        this.proteinGrams = proteinGrams;
        this.fatGrams = fatGrams;
        this.carbGrams = carbGrams;
        this.bmi = bmi;
        this.bmiCategory = bmiCategory;
    }

    // getter'lar
}

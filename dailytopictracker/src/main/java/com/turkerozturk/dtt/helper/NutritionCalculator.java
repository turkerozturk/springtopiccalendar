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
package com.turkerozturk.dtt.helper;

import com.turkerozturk.dtt.dto.ActivityLevel;
import com.turkerozturk.dtt.dto.Gender;
import com.turkerozturk.dtt.dto.NutritionResultDto;

public class NutritionCalculator {

    public static NutritionResultDto calculate(
            double weightKg,
            double heightCm,
            int age,
            Gender gender,
            ActivityLevel activityLevel
    ) {

        // 1) BMR (Mifflin-St Jeor)
        double bmr;
        if (gender == Gender.MALE) {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
        } else {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        }

        // 2) TDEE (günlük ihtiyaç)
        double tdee = bmr * activityLevel.getMultiplier();


        // 4) Makro dağılım (örnek: %25 protein, %30 yağ, %45 karbonhidrat)
        double proteinCalories = tdee * 0.25;
        double fatCalories = tdee * 0.30;
        double carbCalories = tdee * 0.45;

        double proteinGrams = proteinCalories / 4;
        double fatGrams = fatCalories / 9;
        double carbGrams = carbCalories / 4;

        double weightLossCalories = tdee - 550; // ~0.5 kg/hafta
        // 4) Makro dağılım (örnek: %25 protein, %30 yağ, %45 karbonhidrat)
        double proteinCaloriesForLoss = weightLossCalories * 0.25;
        double fatCaloriesForLoss = weightLossCalories * 0.30;
        double carbCaloriesForLoss = weightLossCalories * 0.45;

        double proteinGramsForLoss = proteinCaloriesForLoss / 4;
        double fatGramsForLoss = fatCaloriesForLoss / 9;
        double carbGramsForLoss = carbCaloriesForLoss / 4;

        double weightGainCalories = tdee + 550;

        double proteinCaloriesForGain = weightGainCalories * 0.25;
        double fatCaloriesForGain = weightGainCalories * 0.30;
        double carbCaloriesForGain = weightGainCalories * 0.45;

        double proteinGramsForGain = proteinCaloriesForGain / 4;
        double fatGramsForGain = fatCaloriesForGain / 9;
        double carbGramsForGain = carbCaloriesForGain / 4;

        double proteinCaloriesForBMR = bmr * 0.25;
        double fatCaloriesForBMR = bmr * 0.30;
        double carbCaloriesForBMR = bmr * 0.45;

        double proteinGramsForBMR = proteinCaloriesForBMR / 4;
        double fatGramsForBMR = fatCaloriesForBMR / 9;
        double carbGramsForBMR = carbCaloriesForBMR / 4;

        // 5) BMI
        double heightMeter = heightCm / 100.0;
        double bmi = weightKg / (heightMeter * heightMeter);

        String bmiCategory;
        if (bmi < 18.5) {
            bmiCategory = "Zayıf";
        } else if (bmi < 25) {
            bmiCategory = "Normal";
        } else if (bmi < 30) {
            bmiCategory = "Fazla Kilolu";
        } else {
            bmiCategory = "Şişman";
        }

        return new NutritionResultDto(
                round(bmr),
                round(tdee),
                round(weightLossCalories),
                round(weightGainCalories),
                round(proteinGrams),
                round(fatGrams),
                round(carbGrams),
                round(bmi),
                bmiCategory,
                round(proteinGramsForLoss),
                round(fatGramsForLoss),
                round(carbGramsForLoss),
                round(proteinGramsForGain),
                round(fatGramsForGain),
                round(carbGramsForGain),
                round(proteinGramsForBMR),
                round(fatGramsForBMR),
                round(carbGramsForBMR)
        );
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

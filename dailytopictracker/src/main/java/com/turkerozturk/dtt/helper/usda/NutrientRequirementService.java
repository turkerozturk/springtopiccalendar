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
package com.turkerozturk.dtt.helper.usda;

import com.turkerozturk.dtt.dto.Gender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NutrientRequirementService {

    private final List<NutritionProfile> profiles;

    public NutrientRequirementService() {

        // Table E3.1.A4. Nutritional goals for each age/sex group used in assessing adequacy of USDA Food Patterns at various calorie levels
        // 1888-1-6644-Appendix-E3-1-Table-A4.pdf
        profiles = List.of(

                new NutritionProfile(
                        new AgeRange(1, 3),
                        Gender.CHILD,
                        List.of(
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        30.0,
                                        40.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        13.0,
                                        13.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        5.0,
                                        20.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        14.0,
                                        14.0,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        1500.0,
                                        1500.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(4, 8),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        19.0,
                                        19.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        16.8,
                                        16.8,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        1900.0,
                                        1900.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(4, 8),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        19.0,
                                        19.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        19.6,
                                        19.6,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        1900.0,
                                        1900.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(9, 13),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        34.0,
                                        34.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        22.4,
                                        22.4,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        1900.0,
                                        1900.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(9, 13),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        34.0,
                                        34.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        25.2,
                                        25.2,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2200.0,
                                        2200.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(14, 18),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        46.0,
                                        46.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        25.2,
                                        25.2,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(14, 18),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        25.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        52.0,
                                        52.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        30.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        30.8,
                                        30.8,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(19, 30),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        46.0,
                                        46.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        28.0,
                                        28.0,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(19, 30),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        33.6,
                                        33.6,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(31, 50),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        46.0,
                                        46.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        25.2,
                                        25.2,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(31, 50),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        30.8,
                                        30.8,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(51, 200),
                        Gender.FEMALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        46.0,
                                        46.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        22.4,
                                        22.4,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                ),
                new NutritionProfile(
                        new AgeRange(51, 200),
                        Gender.MALE,
                        List.of(
                                /*
                                new NutrientLimit(
                                        NutrientType.FAT_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ), */
                                new NutrientLimit(
                                        NutrientType.FAT_PERCENT,
                                        20.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_GRAM,
                                        130.0,
                                        130.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.CARBOHYDRATE_PERCENT,
                                        45.0,
                                        65.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_GRAM,
                                        56.0,
                                        56.0,
                                        "g",
                                        "RDA"
                                ),
                                new NutrientLimit(
                                        NutrientType.PROTEIN_PERCENT,
                                        10.0,
                                        35.0,
                                        "%",
                                        "AMDR"
                                ),
                                new NutrientLimit(
                                        NutrientType.FIBER_GRAM,
                                        28.0,
                                        28.0,
                                        "g",
                                        "14g/1000kcal"
                                ),
                                new NutrientLimit(
                                        NutrientType.SODIUM_MG,
                                        2300.0,
                                        2300.0,
                                        "mg",
                                        "UL"
                                ),
                                new NutrientLimit(
                                        NutrientType.SATURATED_FAT_PERCENT,
                                        10.0,
                                        10.0,
                                        "%",
                                        "DG"
                                )
                        )
                )
        );
    }

    public List<NutrientLimit> getLimits(
            int age,
            Gender sex
    ) {

        return profiles.stream()
                .filter(p -> p.gender() == sex)
                .filter(p -> p.ageRange().contains(age))
                .findFirst()
                .map(NutritionProfile::limits)
                .orElse(List.of());
    }
}

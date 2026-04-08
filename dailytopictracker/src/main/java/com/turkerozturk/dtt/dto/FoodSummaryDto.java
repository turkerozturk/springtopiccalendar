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

import java.util.List;

public class FoodSummaryDto {
    private List<FoodEntryDto> items;
    private Double totalKcal;
    private Double totalGram;
    private Double totalGramFat;
    private Double totalGramCarbohydrate;
    private Double totalGramProtein;

    // getter/setter


    public List<FoodEntryDto> getItems() {
        return items;
    }

    public void setItems(List<FoodEntryDto> items) {
        this.items = items;
    }

    public Double getTotalKcal() {
        return totalKcal;
    }

    public void setTotalKcal(Double totalKcal) {
        this.totalKcal = totalKcal;
    }

    public Double getTotalGram() {
        return totalGram;
    }

    public void setTotalGram(Double totalGram) {
        this.totalGram = totalGram;
    }

    public Double getTotalGramFat() {
        return totalGramFat;
    }

    public void setTotalGramFat(Double totalGramFat) {
        this.totalGramFat = totalGramFat;
    }

    public Double getTotalGramCarbohydrate() {
        return totalGramCarbohydrate;
    }

    public void setTotalGramCarbohydrate(Double totalGramCarbohydrate) {
        this.totalGramCarbohydrate = totalGramCarbohydrate;
    }

    public Double getTotalGramProtein() {
        return totalGramProtein;
    }

    public void setTotalGramProtein(Double totalGramProtein) {
        this.totalGramProtein = totalGramProtein;
    }
}

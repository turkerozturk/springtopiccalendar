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

    private Double totalGramFatKcal;
    private Double totalGramCarbohydrateKcal;
    private Double totalGramProteinKcal;

    private Double totalGramKcalFatCarbProtein;


    private Double totalKcalByStatus;
    private Double totalGramByStatus;
    private Double totalGramFatByStatus;
    private Double totalGramCarbohydrateByStatus;
    private Double totalGramProteinByStatus;
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

    public Double getTotalGramFatKcal() {
        return totalGramFatKcal;
    }

    public void setTotalGramFatKcal(Double totalGramFatKcal) {
        this.totalGramFatKcal = totalGramFatKcal;
    }

    public Double getTotalGramCarbohydrateKcal() {
        return totalGramCarbohydrateKcal;
    }

    public void setTotalGramCarbohydrateKcal(Double totalGramCarbohydrateKcal) {
        this.totalGramCarbohydrateKcal = totalGramCarbohydrateKcal;
    }

    public Double getTotalGramProteinKcal() {
        return totalGramProteinKcal;
    }

    public void setTotalGramProteinKcal(Double totalGramProteinKcal) {
        this.totalGramProteinKcal = totalGramProteinKcal;
    }

    public Double getTotalGramKcalFatCarbProtein() {
        return totalGramKcalFatCarbProtein;
    }

    public void setTotalGramKcalFatCarbProtein(Double totalGramKcalFatCarbProtein) {
        this.totalGramKcalFatCarbProtein = totalGramKcalFatCarbProtein;
    }

    public Double getTotalKcalByStatus() {
        return totalKcalByStatus;
    }

    public void setTotalKcalByStatus(Double totalKcalByStatus) {
        this.totalKcalByStatus = totalKcalByStatus;
    }

    public Double getTotalGramByStatus() {
        return totalGramByStatus;
    }

    public void setTotalGramByStatus(Double totalGramByStatus) {
        this.totalGramByStatus = totalGramByStatus;
    }

    public Double getTotalGramFatByStatus() {
        return totalGramFatByStatus;
    }

    public void setTotalGramFatByStatus(Double totalGramFatByStatus) {
        this.totalGramFatByStatus = totalGramFatByStatus;
    }

    public Double getTotalGramCarbohydrateByStatus() {
        return totalGramCarbohydrateByStatus;
    }

    public void setTotalGramCarbohydrateByStatus(Double totalGramCarbohydrateByStatus) {
        this.totalGramCarbohydrateByStatus = totalGramCarbohydrateByStatus;
    }

    public Double getTotalGramProteinByStatus() {
        return totalGramProteinByStatus;
    }

    public void setTotalGramProteinByStatus(Double totalGramProteinByStatus) {
        this.totalGramProteinByStatus = totalGramProteinByStatus;
    }
}

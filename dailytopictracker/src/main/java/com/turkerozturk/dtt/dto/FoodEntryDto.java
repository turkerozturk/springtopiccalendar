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

public class FoodEntryDto {

    private Long dateMillis;
    private String topicName;
    private String topicDescription;


    private Double kcalPer100g;
    private Double gram;
    private Double calculatedKcal;

    private Double gramPerKcal;

    private Double carbohydrate;
    private Double fat;
    private Double protein;

    private Double water;

    private Double fiber;
    private Double sodium;
    private Double fatSaturated;
    private Double sugar;


    private Long entryId;
    private int entryStatus;
    private Long topicId;
    private String categoryName;
    private Long categoryId;



    // getter/setter


    public Long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(Long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicDescription() {
        return topicDescription;
    }

    public void setTopicDescription(String topicDescription) {
        this.topicDescription = topicDescription;
    }

    public Double getKcalPer100g() {
        return kcalPer100g;
    }

    public void setKcalPer100g(Double kcalPer100g) {
        this.kcalPer100g = kcalPer100g;
    }

    public Double getGram() {
        return gram;
    }

    public void setGram(Double gram) {
        this.gram = gram;
    }

    public Double getCalculatedKcal() {
        return calculatedKcal;
    }

    public void setCalculatedKcal(Double calculatedKcal) {
        this.calculatedKcal = calculatedKcal;
    }

    public Double getCarbohydrate() {
        return carbohydrate;
    }

    public void setCarbohydrate(Double carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public Double getFat() {
        return fat;
    }

    public void setFat(Double fat) {
        this.fat = fat;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public int getEntryStatus() {
        return entryStatus;
    }

    public void setEntryStatus(int entryStatus) {
        this.entryStatus = entryStatus;
    }

    public Double getWater() {
        return water;
    }

    public void setWater(Double water) {
        this.water = water;
    }

    public Double getFiber() {
        return fiber;
    }

    public void setFiber(Double fiber) {
        this.fiber = fiber;
    }

    public Double getSodium() {
        return sodium;
    }

    public void setSodium(Double sodium) {
        this.sodium = sodium;
    }

    public Double getFatSaturated() {
        return fatSaturated;
    }

    public void setFatSaturated(Double fatSaturated) {
        this.fatSaturated = fatSaturated;
    }

    public Double getSugar() {
        return sugar;
    }

    public void setSugar(Double sugar) {
        this.sugar = sugar;
    }

    public Double getGramPerKcal() {
        return gramPerKcal;
    }

    public void setGramPerKcal(Double gramPerKcal) {
        this.gramPerKcal = gramPerKcal;
    }
}
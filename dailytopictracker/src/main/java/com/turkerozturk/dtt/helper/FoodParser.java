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

public class FoodParser {

    public static Double extractGram(String noteContent) {
        if (noteContent == null || noteContent.isBlank()) return null;

        String firstLine = noteContent.split("\\R")[0].trim();

        if (firstLine.matches("\\d+(\\.\\d+)?")) {
            return Double.parseDouble(firstLine);
        }

        return null;
    }

    public static Double extractKcalPer100g(String description) {
        if (description == null) return null;

        String[] lines = description.split("\\R");

        if (lines.length < 2) return null;

        String secondLine = lines[1].trim();

        if (secondLine.matches("\\d+(\\.\\d+)?")) {
            return Double.parseDouble(secondLine);
        }

        return null;
    }

    public static Double calculateKcal(Double gram, Double kcalPer100g) {
        if (gram == null || kcalPer100g == null) return null;
        return (gram / 100.0) * kcalPer100g;
    }


}

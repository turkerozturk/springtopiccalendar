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

        return parseFlexibleDouble(firstLine, null);
    }

    public static Double extractKcalPer100g(String description) {
        if (description == null) return null;

        String[] lines = description.split("\\R");

        if (lines.length < 2) return null;

        return parseFlexibleDouble(lines[1], null);
    }

    // --- YENİLER ---

    public static Double extractFat(String description) {
        return extractLineValue(description, 2);
    }

    public static Double extractCarbohydrate(String description) {
        return extractLineValue(description, 3);
    }

    public static Double extractProtein(String description) {
        return extractLineValue(description, 4);
    }

    // --- ORTAK LOGIC ---

    private static Double extractLineValue(String description, int index) {
        if (description == null) return 0.0;

        String[] lines = description.split("\\R");

        if (lines.length <= index) return 0.0;

        return parseFlexibleDouble(lines[index], 0.0);
    }

    /**
     * Virgül ve nokta destekli parse:
     * "12.5" -> 12.5
     * "12,5" -> 12.5
     * "abc"  -> defaultValue
     */
    private static Double parseFlexibleDouble(String value, Double defaultValue) {
        if (value == null) return defaultValue;

        try {
            String normalized = value.trim()
                    .replace(",", "."); // virgül destek

            if (normalized.matches("\\d+(\\.\\d+)?")) {
                return Double.parseDouble(normalized);
            }

        } catch (Exception ignored) {}

        return defaultValue;
    }

    public static Double calculateKcal(Double gram, Double kcalPer100g) {
        if (gram == null || kcalPer100g == null) return null;
        return (gram / 100.0) * kcalPer100g;
    }
}
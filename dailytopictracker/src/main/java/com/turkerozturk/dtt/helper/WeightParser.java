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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeightParser {

    public static Optional<Double> extractWeight(String noteContent) {
        if (noteContent == null || noteContent.isBlank()) {
            return Optional.empty();
        }

        String firstLine = noteContent.split("\\R")[0].trim();

        // virgülü noktaya çevir
        firstLine = firstLine.replace(",", ".");

        // sadece sayı + decimal yakala
        Pattern pattern = Pattern.compile("([0-9]+(\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(firstLine);

        if (matcher.find()) {
            try {
                return Optional.of(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        return Optional.empty();
    }
}

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

import com.turkerozturk.dtt.dto.PhysicalActivityMeta;

public class PhysicalActivityParser {

    public PhysicalActivityMeta parseTopic(String description) {

        PhysicalActivityMeta meta = new PhysicalActivityMeta();

        if (description == null) {
            return meta;
        }

        String[] lines = description.split("\\r?\\n");

        for (String line : lines) {

            String normalized = line.trim().toLowerCase();

            // MET
            if (normalized.startsWith("met")) {
                Double value = extractNumber(normalized);
                meta.setMet(value);
            }

            // TYPE
            if (normalized.startsWith("type")) {

                if (normalized.contains("duration")) {
                    meta.setType(PhysicalActivityMeta.ActivityCalcType.DURATION);
                } else if (normalized.contains("repetition")) {
                    meta.setType(PhysicalActivityMeta.ActivityCalcType.REPETITION);
                } else {
                    meta.setType(PhysicalActivityMeta.ActivityCalcType.UNKNOWN);
                }
            }
        }

        return meta;
    }

    private Double extractNumber(String line) {
        try {
            String number = line.replaceAll("[^0-9,\\.]", "")
                    .replace(",", ".");
            return Double.parseDouble(number);
        } catch (Exception e) {
            return null;
        }
    }
}

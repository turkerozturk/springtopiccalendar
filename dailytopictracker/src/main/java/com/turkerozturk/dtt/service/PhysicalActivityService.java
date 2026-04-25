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
package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.dto.PhysicalActivityMeta;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.helper.ExpressionEvaluator;
import com.turkerozturk.dtt.helper.PhysicalActivityParser;
import org.springframework.stereotype.Service;

@Service
public class PhysicalActivityService {

    private final PhysicalActivityParser parser = new PhysicalActivityParser();

    public double calculate(Entry entry, double weightKg) {

        if (entry == null || entry.getNote() == null) return 0;

        PhysicalActivityMeta meta =
                parser.parseTopic(entry.getTopic().getDescription());

        if (meta.getMet() == null) return 0;

        String firstLine = extractFirstLine(entry.getNote().getContent());

        Double value = ExpressionEvaluator.evaluate(firstLine);

        if (value == null) return 0;

        switch (meta.getType()) {

            case DURATION:
                return meta.getMet() * weightKg * (value / 3600.0);

            case REPETITION:
                // fallback (zayıf model)
                return meta.getMet() * value * 0.1;

            default:
                return 0;
        }
    }

    private String extractFirstLine(String content) {
        if (content == null) return null;
        return content.split("\\r?\\n")[0];
    }
}

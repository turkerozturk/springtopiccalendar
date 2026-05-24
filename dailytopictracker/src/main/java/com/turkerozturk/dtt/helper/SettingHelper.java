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


import com.turkerozturk.dtt.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



import org.springframework.core.env.Environment;


@Component
@RequiredArgsConstructor
public class SettingHelper {

    private final SettingRepository settingRepository;

    private final Environment environment;

    public String get(String key, String defaultValue) {

        // 1. settings table
        var dbValue = settingRepository.findByKey(key)
                .map(s -> s.getValue())
                .orElse(null);

        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }

        // 2. application.properties
        return environment.getProperty(key, defaultValue);
    }

    public Integer getInt(String key, Integer defaultValue) {

        try {
            return Integer.parseInt(get(key, defaultValue.toString()));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Double getDouble(String key, Double defaultValue) {

        try {
            return Double.parseDouble(get(key, defaultValue.toString()));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {

        try {
            return Boolean.parseBoolean(get(key, defaultValue.toString()));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public <E extends Enum<E>> E getEnum(
            String key,
            Class<E> enumClass,
            E defaultValue
    ) {

        try {

            String value = get(key, defaultValue.name());

            return Enum.valueOf(enumClass, value.toUpperCase());

        } catch (Exception e) {

            return defaultValue;
        }
    }
}
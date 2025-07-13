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

import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtils {


    public void setZoneId(ZoneId zoneId) {
        this.zone = zoneId;
    }




    // private static String timeZone = "UTC";
    private ZoneId zone;
    public long getEpochMillis(int year, int month, int day) {
        // Create a LocalDate instance
        LocalDate date = LocalDate.of(year, month, day);

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public long getEpochMillisToday() {
        // Create a LocalDate instance
        LocalDate date = LocalDate.now();

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }


}

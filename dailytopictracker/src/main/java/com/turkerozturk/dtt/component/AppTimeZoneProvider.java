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
package com.turkerozturk.dtt.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class AppTimeZoneProvider {

    // bu alan Spring başlatılırken doldurulacak
    private static ZoneId ZONE;

    @Value("${app.timezone:UTC}")
    public void setZoneId(String timeZoneId) {
        ZONE = ZoneId.of(timeZoneId);
    }

    /** Diğer yerlerden bu metotla erişeceğiz */
    public static ZoneId getZone() {
        return ZONE;
    }

    /** retrofit-style erişim (injection gerekirse) */
    public ZoneId getZoneId() {
        return ZONE;
    }
}



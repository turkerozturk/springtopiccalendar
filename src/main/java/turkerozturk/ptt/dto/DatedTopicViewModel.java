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
package turkerozturk.ptt.dto;

import turkerozturk.ptt.entity.Topic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
public class DatedTopicViewModel {
    private Topic topic;
    private LocalDate dateLocal;
    private int status;

    public DatedTopicViewModel(Topic topic, Long dateMillis, int status, ZoneId zoneId) {
        this.topic = topic;
        this.dateLocal = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate();
        this.status = status;
    }

    public Topic getTopic() {
        return topic;
    }

    public LocalDate getDateLocal() {
        return dateLocal;
    }

    public int getStatus() {
        return status;
    }
}

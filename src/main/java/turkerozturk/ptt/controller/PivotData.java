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
package turkerozturk.ptt.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Pivot veri taşıyıcı:
 *  - dateRange: Filtre tarih aralığındaki günler (LocalDate listesi)
 *  - topicList: Filtre sonucunda gerçekten kullanılan Topic listesi (veya tüm Topic listesi)
 *  - pivotMap:  (topicId, localDate) -> List<Entry>
 *  - topicEntryCount: (topicId) -> o topic’in toplam entry sayısı
 */
@Data
@AllArgsConstructor
public class PivotData {
    private List<LocalDate> dateRange;
    private List<Topic> topicList;
    private Map<Long, Map<LocalDate, List<Entry>>> pivotMap;
    private Map<Long, Integer> topicEntryCount;
}

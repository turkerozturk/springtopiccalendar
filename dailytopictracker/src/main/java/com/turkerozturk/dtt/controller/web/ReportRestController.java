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
package com.turkerozturk.dtt.controller.web;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/api/report")
public class ReportRestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Tum veriler uzerinden verilen tarihler arasinda kategori gruplarindan olusan radar raporu gosterebilir.
     * @param periodType
     * @param refDate
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/radar")
    public Map<String, Object> getRadarReport(
            @RequestParam(defaultValue = "month") String periodType, // "day", "week", "month", "year", "all", "last7", "range"
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate refDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {

        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate now = LocalDate.now(zoneId);
        LocalDate start;
        LocalDate end;

        // --- tarih aralığını belirle ---
        switch (periodType.toLowerCase()) {
            case "day":
                start = refDate != null ? refDate : now;
                end = start;
                break;
            case "week":
                LocalDate ref = refDate != null ? refDate : now;
                start = ref.minusDays(ref.getDayOfWeek().getValue() - 1); // Pazartesi
                end = start.plusDays(6);
                break;
            case "month":
                ref = refDate != null ? refDate : now;
                start = ref.withDayOfMonth(1);
                end = start.plusMonths(1).minusDays(1);
                break;
            case "year":
                ref = refDate != null ? refDate : now;
                start = ref.withDayOfYear(1);
                end = ref.withMonth(12).withDayOfMonth(31);
                break;
            case "last7":
                end = now;
                start = now.minusDays(6);
                break;
            case "range":
                start = startDate;
                end = endDate;
                break;
            case "all":
            default:
                start = LocalDate.of(1970, 1, 1);
                end = now;
                break;
        }

        long startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long endMillis = end.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1;

        // --- SQL sorgusu ---
        String sql = """
            select count(*) as counter,
                   sum(topics.weight) as score,
                   category_groups.name as categoryGroupName
            from entries
            left join topics on entries.topic_id = topics.id
            left join categories on topics.category_id = categories.id
            left join category_groups on categories.category_group_number = category_groups.id
            where 
                entries.date_millis_ymd between ? and ? 
                and categories.is_archived = 0
                and topics.weight > 0
            group by categoryGroupName
            order by score desc, counter desc
        """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startMillis, endMillis);

        // --- chart data ---
        List<String> labels = new ArrayList<>();
        List<Integer> counters = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        int totalScore = 0, totalCount = 0;

        for (Map<String, Object> row : rows) {
            String groupName = (String) row.get("categoryGroupName");
            int counter = ((Number) row.get("counter")).intValue();
            int score = ((Number) row.get("score")).intValue();
            labels.add(groupName);
            counters.add(counter);
            scores.add(score);
            totalCount += counter;
            totalScore += score;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("counters", counters);
        result.put("scores", scores);
        result.put("totalCount", totalCount);
        result.put("totalScore", totalScore);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        result.put("periodType", periodType);

        return result;
    }
}
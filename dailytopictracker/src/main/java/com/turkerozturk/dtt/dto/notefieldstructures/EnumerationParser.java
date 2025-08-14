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
package com.turkerozturk.dtt.dto.notefieldstructures;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Note;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.text.Collator;

public class EnumerationParser implements NoteFieldStructure {


    private Locale locale;
    private ZoneId zoneId;

    private Map<String, GroupStats> statsMap = new LinkedHashMap<>();




    public EnumerationParser() {
        this.zoneId = AppTimeZoneProvider.getZone();
        this.locale = Locale.ENGLISH;

    }

    @Override
    public String getName() {
        return "Enumeration Parser";
    }

    @Override
    public String getDescription() {
        return "Groups entries by first line of note and calculates occurrence stats.";
    }

    @Override
    public void parseRawData(List<Entry> entries) {
        statsMap.clear();

        // 1) Filter + group
        Map<String, List<Entry>> grouped = entries.stream()
                .filter(e -> e.getStatus() == 1)
                .collect(Collectors.groupingBy(e -> {
                    Note entryNote = e.getNote();
                    String note = null;
                    if(entryNote != null) {
                        note = entryNote.getContent();
                    } else {
                        if (note == null || note.trim().isEmpty()) {
                            return "EMPTY";
                        }
                    }
                    String firstLine = note.split("\\R", 2)[0].trim();
                    return firstLine.isEmpty() ? "EMPTY" : firstLine;
                }));

        // 2) Sort keys by locale
        List<String> sortedKeys = new ArrayList<>(grouped.keySet());
        Collator collator = Collator.getInstance(locale);
        sortedKeys.sort(collator);
        // 3) Build stats
        LocalDate today = LocalDate.now(zoneId);

        for (String key : sortedKeys) {
            List<Entry> groupEntries = grouped.get(key);
            // Sort by date ascending
            List<LocalDate> dates = groupEntries.stream()
                    .map(e -> Instant.ofEpochMilli(e.getDateMillisYmd()).atZone(zoneId).toLocalDate())
                    .sorted()
                    .collect(Collectors.toList());

            int count = dates.size();
            LocalDate lastDate = dates.get(count - 1);

            double avgDays;
            if (count > 1) {
                long totalDiff = 0;
                for (int i = 1; i < count; i++) {
                    totalDiff += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
                }
                avgDays = (double) totalDiff / (count - 1);
            } else {
                avgDays = ChronoUnit.DAYS.between(dates.get(0), today);
            }

            statsMap.put(key, new GroupStats(count, lastDate, avgDays));
        }
    }

    @Override
    public String getParsedDataAsJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Map.Entry<String, GroupStats> e : statsMap.entrySet()) {
            if (!first) sb.append(",");
            GroupStats gs = e.getValue();
            sb.append("{")
                    .append("\"group\":\"").append(escapeJson(e.getKey())).append("\",")
                    .append("\"count\":").append(gs.count).append(",")
                    .append("\"lastDate\":\"").append(gs.lastDate.format(fmt)).append("\",")
                    .append("\"avgDays\":").append(String.format("%.2f", gs.avgDays))
                    .append("}");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr><th>Group</th><th>Count</th><th>Last Date</th><th>Avg Days</th></tr>");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Map.Entry<String, GroupStats> e : statsMap.entrySet()) {
            GroupStats gs = e.getValue();
            sb.append("<tr>")
                    .append("<td>").append(htmlEscape(e.getKey())).append("</td>")
                    .append("<td>").append(gs.count).append("</td>")
                    .append("<td>").append(gs.lastDate.format(fmt)).append("</td>")
                    .append("<td>").append(String.format("%.2f", gs.avgDays)).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static class GroupStats {
        int count;
        LocalDate lastDate;
        double avgDays;
        GroupStats(int count, LocalDate lastDate, double avgDays) {
            this.count = count;
            this.lastDate = lastDate;
            this.avgDays = avgDays;
        }
    }
}


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
import com.turkerozturk.dtt.entity.Topic;

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
        return "EnumerationParser";
    }

    @Override
    public String getDescription() {
        return "Groups entries by first line of note and calculates occurrence stats.";
    }

    @Override
    public void parseRawData(List<Entry> entries) {
        statsMap.clear();

        // Grup -> tarih listesi
        Map<String, List<LocalDate>> dateMap = new HashMap<>();

        for (Entry e : entries) {
            if (e.getStatus() != 1) continue;

            String note = e.getNote().getContent();
            String firstLine = (note == null ? "" : note.split("\\R", 2)[0]).trim();
            if (firstLine.isEmpty()) firstLine = "EMPTY";

            String[] parts = firstLine.split(",");
            LocalDate entryDate = Instant.ofEpochMilli(e.getDateMillisYmd())
                    .atZone(zoneId)
                    .toLocalDate();

            for (String p : parts) {
                String token = p.trim();
                if (token.isEmpty()) token = "EMPTY";

                // İlk harf büyük, geri kalanı küçük
                token = token.substring(0, 1).toUpperCase(locale) +
                        token.substring(1).toLowerCase(locale);

                dateMap.computeIfAbsent(token, k -> new ArrayList<>()).add(entryDate);
            }
        }

        LocalDate today = LocalDate.now(zoneId);

        // Locale'e göre sıralama
        List<String> sortedKeys = new ArrayList<>(dateMap.keySet());
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);
        sortedKeys.sort(collator);

        // Her grup için istatistik
        for (String key : sortedKeys) {
            List<LocalDate> dates = new ArrayList<>(dateMap.get(key));
            Collections.sort(dates);

            int originalCount = dates.size(); // gerçek entry sayısı
            boolean addedToday = false;

            if (!dates.isEmpty()) {
                // Konu (topic) sonlandırılmamışsa today ekle
                Topic topic = null;
                for (Entry e : entries) {
                    if (e.getStatus() == 1) {
                        String note = e.getNote().getContent();
                        String firstLine = (note == null ? "" : note.split("\\R", 2)[0]).trim();
                        if (firstLine.isEmpty()) firstLine = "EMPTY";
                        String[] parts = firstLine.split(",");
                        for (String p : parts) {
                            String token = p.trim();
                            if (token.isEmpty()) token = "EMPTY";
                            token = token.substring(0, 1).toUpperCase(locale) +
                                    token.substring(1).toLowerCase(locale);
                            if (token.equals(key)) {
                                topic = e.getTopic();
                                break;
                            }
                        }
                        if (topic != null) break;
                    }
                }
                if (topic != null && topic.getEndDate() == null) {
                    dates.add(today);
                    Collections.sort(dates);
                    addedToday = true;
                }
            }

            // Ortalama gün farkı hesapla
            double avgDays;
            if (dates.size() > 1) {
                long totalDiff = 0;
                for (int i = 1; i < dates.size(); i++) {
                    totalDiff += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
                }
                avgDays = (double) totalDiff / (dates.size() - 1);
            } else {
                avgDays = ChronoUnit.DAYS.between(dates.get(0), today);
            }

            // lastDate: today eklenmişse sondan bir önceki tarih
            LocalDate lastDate = addedToday
                    ? dates.get(dates.size() - 2)
                    : dates.get(dates.size() - 1);

            // count: today eklenmişse bile orijinal sayı
            statsMap.put(key, new GroupStats(originalCount, lastDate, avgDays));
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
        sb.append("<table class=\"table table-sm table-bordered\"><tr><th>Group</th><th>Count</th><th>Last Date</th><th>Avg Days</th></tr>");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        int totalCount = 0;
        for (Map.Entry<String, GroupStats> e : statsMap.entrySet()) {
            GroupStats gs = e.getValue();
            sb.append("<tr>")
                    .append("<td>").append(htmlEscape(e.getKey())).append("</td>")
                    .append("<td>").append(gs.count).append("</td>")
                    .append("<td>").append(gs.lastDate.format(fmt)).append("</td>")
                    .append("<td>").append(String.format("%.2f", gs.avgDays)).append("</td>")
                    .append("</tr>");
            totalCount += gs.count;
        }
        sb.append("<tr>")
                .append("<td colspan=\"4\">")
                .append("").append(totalCount).append(" items in ")
                .append(statsMap.size()).append(" different things,")
                .append("</td>")
                .append("</tr>");


        sb.append("</table>");

        sb.append("<span>").append("Parser: ")
                .append(getName());
        sb.append("</span>");
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


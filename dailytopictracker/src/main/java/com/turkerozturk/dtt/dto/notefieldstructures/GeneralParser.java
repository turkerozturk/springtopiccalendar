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
import com.turkerozturk.dtt.entity.Topic;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class GeneralParser implements NoteFieldStructure {

    private final Locale locale;
    private final ZoneId zoneId;

    private double generalAvg;
    private int generalCount;
    private long generalDaysRange;

    private List<SegmentResult> segmentResults = new ArrayList<>();

    public GeneralParser() {

        this.zoneId = AppTimeZoneProvider.getZone();
        this.locale = Locale.ENGLISH;
    }

    @Override
    public String getName() {
        return "General Parser";
    }

    @Override
    public String getDescription() {
        return "Calculates overall and segmented average repetition days for a topic.";
    }



    @Override
    public void parseRawData(List<Entry> entries) {
        if (entries.isEmpty()) return;

        // Tek topic
        Topic topic = entries.get(0).getTopic();

        // Sadece status=1 olan entry’lerin tarihleri
        List<LocalDate> dates = entries.stream()
                .filter(e -> e.getStatus() == 1)
                .map(e -> Instant.ofEpochMilli(e.getDateMillisYmd()).atZone(zoneId).toLocalDate())
                .sorted()
                .collect(Collectors.toList());

        if (dates.isEmpty()) return;

        LocalDate today = LocalDate.now(zoneId);
        LocalDate lastDate = dates.get(dates.size() - 1);

        // Genel ortalama
        boolean addToday = (topic != null && topic.getEndDate() == null && !lastDate.equals(today));
        this.generalAvg = calculateAverageDays(dates, addToday ? today : null);
        this.generalCount = dates.size();
        LocalDate endDate = addToday ? today : lastDate;
        this.generalDaysRange = ChronoUnit.DAYS.between(dates.get(0), endDate);

        // Bölme kararı
        int segments = decideSegmentCount(this.generalDaysRange);
        this.segmentResults.clear();

        if (segments > 1) {
            List<SegmentResult> segs = calculateSegmentAverages(dates, addToday ? today : null, segments);
            this.segmentResults.addAll(segs);
        }
    }

    private double calculateAverageDays(List<LocalDate> dates, LocalDate extraEnd) {
        List<LocalDate> temp = new ArrayList<>(dates);
        if (extraEnd != null) temp.add(extraEnd);
        Collections.sort(temp);

        if (temp.size() < 2) return 0.0;

        long totalDiff = 0;
        for (int i = 1; i < temp.size(); i++) {
            totalDiff += ChronoUnit.DAYS.between(temp.get(i - 1), temp.get(i));
        }
        return (double) totalDiff / (temp.size() - 1);
    }

    private int decideSegmentCount(long totalDays) {
        if (totalDays < 6) return 1; // çok kısa aralık bölünmez
        if (totalDays <= 15) return 3;
        if (totalDays <= 30) return 4;
        return 5; // daha uzun ise daha çok segment
    }

    private List<SegmentResult> calculateSegmentAverages(List<LocalDate> dates, LocalDate extraEnd, int segments) {
        List<SegmentResult> results = new ArrayList<>();
        LocalDate start = dates.get(0);
        LocalDate end = (extraEnd != null) ? extraEnd : dates.get(dates.size() - 1);
        long totalDays = ChronoUnit.DAYS.between(start, end);
        long segLength = totalDays / segments;

        for (int i = 0; i < segments; i++) {
            LocalDate segStart = start.plusDays(i * segLength);
            LocalDate segEnd = (i == segments - 1) ? end : segStart.plusDays(segLength);

            // Segment içindeki tarihleri bul
            List<LocalDate> segDates = dates.stream()
                    .filter(d -> !d.isBefore(segStart) && !d.isAfter(segEnd))
                    .collect(Collectors.toList());

            double avg = calculateAverageDays(segDates, segEnd);
            results.add(new SegmentResult(avg, segDates.size(), ChronoUnit.DAYS.between(segStart, segEnd)));
        }
        return results;
    }

    @Override
    public String getParsedDataAsJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"general\":{")
                .append("\"avgDays\":").append(String.format(Locale.US, "%.2f", generalAvg)).append(",")
                .append("\"count\":").append(generalCount).append(",")
                .append("\"daysRange\":").append(generalDaysRange)
                .append("},\"segments\":[");
        for (int i = 0; i < segmentResults.size(); i++) {
            SegmentResult s = segmentResults.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"avgDays\":").append(String.format(Locale.US, "%.2f", s.avgDays))
                    .append(",\"count\":").append(s.count)
                    .append(",\"daysRange\":").append(s.daysRange).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1'>");

        sb.append("<tr><th colspan='3'>Genel Ortalama</th></tr>")
                .append("<tr><th>Ortalama Gün</th><th>Entry Sayısı</th><th>Aralık (gün)</th></tr>")
                .append("<tr><td>").append(String.format(Locale.US, "%.2f", generalAvg))
                .append("</td><td>").append(generalCount)
                .append("</td><td>").append(generalDaysRange).append("</td></tr>");

        if (!segmentResults.isEmpty()) {
            sb.append("<tr><th colspan='3'>Bölmeli Ortalamalar</th></tr>")
                    .append("<tr><th>Ortalama Gün</th><th>Entry Sayısı</th><th>Aralık (gün)</th></tr>");
            for (SegmentResult s : segmentResults) {
                sb.append("<tr><td>").append(String.format(Locale.US, "%.2f", s.avgDays))
                        .append("</td><td>").append(s.count)
                        .append("</td><td>").append(s.daysRange).append("</td></tr>");
            }
        }

        sb.append("</table>");
        return sb.toString();
    }

    private static class SegmentResult {
        double avgDays;
        int count;
        long daysRange;

        SegmentResult(double avgDays, int count, long daysRange) {
            this.avgDays = avgDays;
            this.count = count;
            this.daysRange = daysRange;
        }
    }
}

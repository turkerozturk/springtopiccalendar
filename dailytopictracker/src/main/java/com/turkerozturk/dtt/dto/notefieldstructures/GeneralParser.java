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


import java.time.format.DateTimeFormatter;

public class GeneralParser implements NoteFieldStructure {

    private final Locale locale;
    private final ZoneId zoneId;
    private final int minSegmentDays; // heuristic for decideSegmentCount

    // General results
    private double generalAvgDays = 0.0; // days per occurrence (density)
    private int generalCount = 0;        // # of status==1 entries
    private long generalDaysRange = 0;   // inclusive day count
    private LocalDate generalStart = null;
    private LocalDate generalEnd = null; // inclusive end (topic.endDate or today)

    // Segmented results
    private List<SegmentResult> segmentResults = new ArrayList<>();

    private double segmentedAvgDays = 0.0; // butun segment ortalamalarinin ortalamasi.

    double stabilityIndex;
    String stabilityComment;

    public GeneralParser() {

        this.zoneId = AppTimeZoneProvider.getZone();
        this.locale = Locale.ENGLISH;
        int minSegmentDays = 7;
        this.minSegmentDays = Math.max(1, minSegmentDays);

    }


    @Override
    public String getName() {
        return "General Parser";
    }

    @Override
    public String getDescription() {
        return "Calculates overall and segmented density-based average recurrence (days per status==1) for a single topic.";
    }

    @Override
    public void parseRawData(List<Entry> entries) {
        // Reset
        generalAvgDays = 0.0;
        generalCount = 0;
        generalDaysRange = 0;
        generalStart = null;
        generalEnd = null;
        segmentResults.clear();

        if (entries == null || entries.isEmpty()) return;

        // Assumption: all entries belong to the same topic
        Topic topic = entries.get(0).getTopic();

        // Status==1 dates, sorted
        List<LocalDate> dates = entries.stream()
                .filter(e -> e.getStatus() == 1)
                .map(e -> Instant.ofEpochMilli(e.getDateMillisYmd()).atZone(zoneId).toLocalDate())
                .sorted()
                .collect(Collectors.toList());

        if (dates.isEmpty()) return; // nothing to compute


        // End = topic.endDate (inclusive) if present, else today (inclusive)
        LocalDate today = LocalDate.now(zoneId);
        LocalDate topicStart = null;
        LocalDate topicEnd = null; // Adjust here if your Topic end date type differs
        if (topic != null) {
            // If your model uses a different type (e.g., Long millis), convert it here instead.
            topicStart = topic.getBaseDate();
            topicEnd = topic.getEndDate(); // <-- assumes LocalDate, adapt if necessary
        }
        generalStart = (topicStart != null) ? topicStart : dates.get(0); // inclusive

        generalEnd = (topicEnd != null) ? topicEnd : today;

        // Inclusive day count in range [generalStart, generalEnd]
        generalDaysRange = ChronoUnit.DAYS.between(generalStart, generalEnd) + 1;

        // Count of status==1 entries
        generalCount = dates.size();

        // Density-based average: days per occurrence (general)
        generalAvgDays = (generalCount > 0) ? ((double) generalDaysRange / generalCount) : 0.0;

// Segments
        int segments = decideSegmentCount(generalDaysRange);
        if (segments > 1) {
            segmentResults = calculateSegmentAverages(dates, generalStart, generalEnd, segments);

            double segmentAvgSums = 0.0;
            int calculatedSize = segmentResults.size();

            for (SegmentResult s : segmentResults) {
                if (s.avgDays != null) {
                    // segmentte entry varsa hesaplanmış değeri kullan
                    segmentAvgSums += s.avgDays;
                } else {
                    // segmentte hiç entry yok → o segmentin tamamı boş
                    long segDays = (s.end.toEpochDay() - s.start.toEpochDay()) + 1;
                    segmentAvgSums += segDays;  // bu segmentin yoğunluğu = tüm günler boş
                }
            }

            segmentedAvgDays = segmentAvgSums / calculatedSize;

            // Stability index
            stabilityIndex = segmentedAvgDays - generalAvgDays;

            // Esnek eşik: genel ortalamanın %10'u
            double threshold = 0.1 * generalAvgDays;
            if (Math.abs(stabilityIndex) < threshold) {
                stabilityComment = "Very stable (almost no variation)";
            } else if (stabilityIndex > 0) {
                stabilityComment = "Entries are more evenly spread across segments (general average is lower)";
            } else {
                stabilityComment = "Entries are denser in some segments (general average is higher)";
            }
        }

    }

    /**
     * Decide how many equal segments to split the inclusive range into.
     * Heuristic: aim for segments of at least minSegmentDays, capped at 5 segments.
     */
    private int decideSegmentCount(long totalDaysInclusive) {
        if (totalDaysInclusive <= 0) return 1;
        int bySize = (int) (totalDaysInclusive / minSegmentDays);
        int segments = Math.max(1, Math.min(5, bySize));
        return segments;
    }

    /**
     * Split [start, end] inclusive into k nearly-equal segments and compute density-based averages per segment.
     * If a segment has zero entries, avgDays is null.
     */
    private List<SegmentResult> calculateSegmentAverages(List<LocalDate> sortedDates,
                                                         LocalDate start,
                                                         LocalDate end,
                                                         int k) {
        List<SegmentResult> results = new ArrayList<>();
        if (k <= 0) return results;

        long totalDays = ChronoUnit.DAYS.between(start, end) + 1; // inclusive length
        if (totalDays <= 0) return results;

        long baseLen = totalDays / k;           // at least 1 because we cap k in decide
        long remainder = totalDays % k;         // distribute +1 to the first 'remainder' segments

        // Two-pointer sweep on dates
        int idx = 0;
        int n = sortedDates.size();

        LocalDate segStart = start;
        for (int i = 0; i < k; i++) {
            long len = baseLen + (i < remainder ? 1 : 0);
            LocalDate segEnd = segStart.plusDays(len - 1); // inclusive

            // Count dates within [segStart, segEnd]
            int count = 0;
            while (idx < n && (sortedDates.get(idx).isBefore(segStart))) {
                idx++; // advance to first date >= segStart (safety if any)
            }
            int j = idx;
            while (j < n && !sortedDates.get(j).isAfter(segEnd)) {
                count++; j++;
            }
            idx = j; // next segment continues from here

            Double avgDays = (count > 0) ? ((double) len / count) : null;
            results.add(new SegmentResult(avgDays, count, len, segStart, segEnd));

            segStart = segEnd.plusDays(1);
        }
        return results;
    }

    @Override
    public String getParsedDataAsJSON() {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"general\":{")
                .append("\"avgDays\":").append(String.format(Locale.US, "%.2f", generalAvgDays)).append(",")
                .append("\"count\":").append(generalCount).append(",")
                .append("\"daysRange\":").append(generalDaysRange).append(",")
                .append("\"startDate\":\"").append(generalStart == null ? "" : generalStart.format(fmt)).append("\",")
                .append("\"endDate\":\"").append(generalEnd == null ? "" : generalEnd.format(fmt)).append("\"")
                .append("},\"segments\":[");

        for (int i = 0; i < segmentResults.size(); i++) {
            SegmentResult s = segmentResults.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"avgDays\":")
                    .append(s.avgDays == null ? "null" : String.format(Locale.US, "%.2f", s.avgDays)).append(",")
                    .append("\"count\":").append(s.count).append(",")
                    .append("\"daysRange\":").append(s.daysRange).append(",")
                    .append("\"startDate\":\"").append(s.start.format(fmt)).append("\",")
                    .append("\"endDate\":\"").append(s.end.format(fmt)).append("\"")
                    .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String getReport() {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"table table-sm table-bordered\">");

        // General
        sb.append("<tr><th colspan='3'>General Average</th></tr>")
                .append("<tr><th>Average Days</th><th>Entry Count</th><th>Range (days)</th></tr>");
        String generalTitle = (generalStart != null && generalEnd != null)
                ? (generalStart.format(fmt) + " → " + generalEnd.format(fmt))
                : "";
        sb.append("<tr>")
                .append("<td style=\"background-color:lightyellow;font-weight:bold;\">").append(String.format(Locale.US, "%.2f", generalAvgDays)).append("</td>")
                .append("<td style=\"background-color:lightgreen;\">").append(generalCount).append("</td>")
                .append("<td title='").append(escapeHtml(generalTitle)).append("' style=\"background-color:snow;\">")
                .append(generalDaysRange).append("</td>")
                .append("</tr>");

        // Segments
        if (!segmentResults.isEmpty()) {
            sb.append("<tr><th colspan='3'>Segmented Averages</th></tr>")
                    .append("<tr><th>Average Days</th><th>Entry Count</th><th>Range (days)</th></tr>");
            for (SegmentResult s : segmentResults) {
                String title = s.start.format(fmt) + " → " + s.end.format(fmt);
                sb.append("<tr>")
                        .append("<td>")
                        .append(s.avgDays == null ? "—" : String.format(Locale.US, "%.2f", s.avgDays))
                        .append("</td>")
                        .append("<td>").append(s.count).append("</td>")
                        .append("<td title='").append(escapeHtml(title)).append("'>")
                        .append(s.daysRange).append("</td>")
                        .append("</tr>");
            }

            sb.append("<tr><td colspan='2'>Average Over Segments</td>");
            sb.append("<td style=\"background-color:lightyellow;font-weight:bold;\">")
                    .append(String.format(Locale.US, "%.2f", segmentedAvgDays))
                    .append("</td></tr>");


            sb.append("<tr><td colspan='2' title='Segmented average days - General average days'>Stability Index</td>")
                    .append("<td  title='less than half is stable, negative is unstable, positive means more even.'>")
                    .append(String.format(Locale.US, "%.2f", stabilityIndex))
                    .append("</td></tr>");
            sb.append("<tr><td colspan='3'>").append(stabilityComment)
                    .append("</td></tr>");


        }

        sb.append("</table>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static class SegmentResult {
        final Double avgDays; // null if no entries in segment
        final int count;
        final long daysRange; // inclusive length
        final LocalDate start; // inclusive
        final LocalDate end;   // inclusive

        SegmentResult(Double avgDays, int count, long daysRange, LocalDate start, LocalDate end) {
            this.avgDays = avgDays;
            this.count = count;
            this.daysRange = daysRange;
            this.start = start;
            this.end = end;
        }
    }
}

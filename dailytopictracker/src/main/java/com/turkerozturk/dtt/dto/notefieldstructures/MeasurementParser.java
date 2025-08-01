package com.turkerozturk.dtt.dto.notefieldstructures;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Entry;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class MeasurementParser implements NoteFieldStructure {
    private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*(.*)$");

    private List<ParsedEntry> parsedEntries = new LinkedList<>();
    private String unit;
    private double total = 0;
    private double average = 0;

    @Override
    public String getName() {
        return "MeasurementParser";
    }

    @Override
    public String getDescription() {
        return "Parses numerical measurement and its unit from the first line of entry notes.";
    }

    @Override
    public void parseRawData(List<Entry> entries) {
        parsedEntries.clear();
        Map<String, Integer> unitCount = new HashMap<>();
        double sum = 0;
        int count = 0;

        for (Entry entry : entries) {
            if (entry.getStatus() != 1 || entry.getNote() == null) continue;

            String[] lines = entry.getNote().getContent().split("\\r?\\n");
            if (lines.length == 0) continue;

            String firstLine = lines[0].trim();
            Matcher matcher = LINE_PATTERN.matcher(firstLine);
            if (!matcher.matches()) continue;

            try {
                String rawNumber = matcher.group(1).replace(",", "."); // normalize decimal separator
                Double value = Double.parseDouble(rawNumber);
                String rawUnit = matcher.group(2).trim();

                parsedEntries.add(new ParsedEntry(entry.getDateMillisYmd(), value, rawUnit));
                sum += value;
                count++;

                if (!rawUnit.isEmpty()) {
                    unitCount.put(rawUnit, unitCount.getOrDefault(rawUnit, 0) + 1);
                }

            } catch (Exception e) {
                // geçersiz format, atla
            }
        }

        this.unit = unitCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        this.total = sum;
        this.average = count > 0 ? sum / count : 0;
    }

    @Override
    public String getParsedDataAsJSON() {
        try {
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("parser", getName());
            jsonMap.put("entryCount", parsedEntries.size());
            jsonMap.put("unit", unit);
            jsonMap.put("total", total);
            jsonMap.put("average", average);

            List<Map<String, Object>> dataList = new ArrayList<>();
            for (ParsedEntry pe : parsedEntries) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", formatDate(pe.dateMillisYmd));
                map.put("value", pe.value);
                map.put("unit", pe.unit);
                dataList.add(map);
            }
            jsonMap.put("data", dataList);

            if (!parsedEntries.isEmpty()) {
                List<ParsedEntry> sortedByDate = new ArrayList<>(parsedEntries);
                sortedByDate.sort(Comparator.comparingLong(ParsedEntry::getDateMillisYmd));
                ParsedEntry first = sortedByDate.get(0);
                ParsedEntry last = sortedByDate.get(sortedByDate.size() - 1);

                List<ParsedEntry> sortedByValue = new ArrayList<>(parsedEntries);
                sortedByValue.sort(Comparator.comparingDouble(ParsedEntry::getValue));
                ParsedEntry min = sortedByValue.get(0);
                ParsedEntry max = sortedByValue.get(sortedByValue.size() - 1);

                jsonMap.put("firstEntry", toEntryMap(first));
                jsonMap.put("lastEntry", toEntryMap(last));
                jsonMap.put("minValueEntry", toEntryMap(min));
                jsonMap.put("maxValueEntry", toEntryMap(max));
            }

            ObjectMapper mapper = new ObjectMapper();
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap));
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public String getReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("<table style='margin:auto;background-color:FloralWhite;'>");

        sb.append("<tr><td colspan='2'>");
        sb.append("Parser: ").append(getName()).append("\n");
        sb.append("</td></tr>");
        sb.append("<tr><td colspan='2'>");
        sb.append("Total Entries Parsed: ").append(parsedEntries.size()).append("\n");
        sb.append("</td></tr>");
        sb.append("<tr><td colspan='2'>");
        sb.append("Most Common Unit: ").append(unit).append("\n");
        sb.append("</td></tr>");
        sb.append("<tr><td colspan='2'>");
        sb.append("Total Value: ").append(String.format("%.2f", total)).append(" ").append(unit).append("\n");
        sb.append("</td></tr>");
        sb.append("<tr><td colspan='2'>");
        sb.append("Average Value: ").append(String.format("%.2f", average)).append(" ").append(unit).append("\n");
        sb.append("</td></tr>");


        if (!parsedEntries.isEmpty()) {
            // Sıralama için kopya liste
            List<ParsedEntry> sortedByDate = new ArrayList<>(parsedEntries);
            sortedByDate.sort(Comparator.comparingLong(ParsedEntry::getDateMillisYmd));

            ParsedEntry first = sortedByDate.get(0);
            ParsedEntry last = sortedByDate.get(sortedByDate.size() - 1);

            List<ParsedEntry> sortedByValue = new ArrayList<>(parsedEntries);
            sortedByValue.sort(Comparator.comparingDouble(ParsedEntry::getValue));

            ParsedEntry min = sortedByValue.get(0);
            ParsedEntry max = sortedByValue.get(sortedByValue.size() - 1);



            sb.append("<tr>");
            sb.append("<td>");
            sb.append("First Entry: ")
                    .append(formatDate(first.dateMillisYmd)).append(" → ")
                    .append("</td><td>")
                    .append(String.format("%.2f", first.value)).append(" ").append(first.unit).append("\n");
            sb.append("</td");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Last Entry: ")
                    .append(formatDate(last.dateMillisYmd)).append(" → ")
                    .append("</td><td>")
                    .append(String.format("%.2f", last.value)).append(" ").append(last.unit).append("\n");
            sb.append("</td");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Min Value: ")
                    .append(formatDate(min.dateMillisYmd)).append(" → ")
                    .append("</td><td>")
                    .append(String.format("%.2f", min.value)).append(" ").append(min.unit).append("\n");
            sb.append("</td");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Max Value: ")
                    .append(formatDate(max.dateMillisYmd)).append(" → ")
                    .append("</td><td>")
                    .append(String.format("%.2f", max.value)).append(" ").append(max.unit);
            sb.append("</td");
            sb.append("</tr>");

            sb.append("</table>");

        }

        return sb.toString();
    }

    @Data
    private static class ParsedEntry {
        private long dateMillisYmd;
        private double value;
        private String unit;

        public ParsedEntry(long dateMillisYmd, double value, String unit) {
            this.dateMillisYmd = dateMillisYmd;
            this.value = value;
            this.unit = unit;
        }
    }


    private String formatDate(long millisYmd) {
        AppTimeZoneProvider timeZoneProvider = new AppTimeZoneProvider();
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz

        return Instant.ofEpochMilli(millisYmd)
                .atZone(zoneId)
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private Map<String, Object> toEntryMap(ParsedEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", formatDate(entry.dateMillisYmd));
        map.put("value", entry.value);
        map.put("unit", entry.unit);
        return map;
    }


}

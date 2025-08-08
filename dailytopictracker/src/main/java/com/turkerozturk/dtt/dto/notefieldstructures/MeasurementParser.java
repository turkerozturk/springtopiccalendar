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
    private String measurementUnit;
    private double sumTotal = 0;
    private double average = 0;
    // TODO below:
    //private double firstEntryValue = 0;
    //private double lastEntryValue = 0;
    //private double minimumEntryValue = 0;
    //private double maximumEntryValue = 0;
    private double diffMinimumToAverage = 0;
    private double diffMaximumToAverage = 0;
    private double diffMaximumToMinimum = 0;

    private ParsedEntry first;
    private ParsedEntry last;
    private ParsedEntry min;
    private ParsedEntry max;

    private double diffSumBelowAverage = 0;
    private double diffSumAboveAverage = 0;
    private double sumAtAverage = 0;
    private int countBelowAverage = 0;
    private int countAboveAverage = 0;
    private int countEqualAverage = 0;

    int countTotal = 0;


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

        this.measurementUnit = unitCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        this.sumTotal = sum;
        this.average = count > 0 ? sum / count : 0;


        for (ParsedEntry parsedEntry : parsedEntries) {


            Double parsedValue = parsedEntry.value;
            if (parsedValue > average) {
                diffSumAboveAverage += parsedValue;
                countAboveAverage++;
            } else if (parsedValue < average) {
                diffSumBelowAverage += parsedValue;
                countBelowAverage++;
            } else {
                sumAtAverage += parsedValue;
                countEqualAverage++;
            }



        }




        if (!parsedEntries.isEmpty()) {
            // Sıralama için kopya liste
            List<ParsedEntry> sortedByDate = new ArrayList<>(parsedEntries);
            sortedByDate.sort(Comparator.comparingLong(ParsedEntry::getDateMillisYmd));

            first = sortedByDate.get(0);
            last = sortedByDate.get(sortedByDate.size() - 1);

            List<ParsedEntry> sortedByValue = new ArrayList<>(parsedEntries);
            sortedByValue.sort(Comparator.comparingDouble(ParsedEntry::getValue));

            min = sortedByValue.get(0);
            max = sortedByValue.get(sortedByValue.size() - 1);


            diffMaximumToAverage = max.value - average;
            diffMinimumToAverage = average - min.value;
            diffMaximumToMinimum = max.value - min.value;

        }





        countTotal = parsedEntries.size();






    }

    @Override
    public String getParsedDataAsJSON() {
        try {
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("parser", getName());
            jsonMap.put("entryCount", parsedEntries.size());
            jsonMap.put("unit", measurementUnit);
            jsonMap.put("total", sumTotal);
            jsonMap.put("average", average);

            if (!parsedEntries.isEmpty()) {

                List<ParsedEntry> sortedByDate = new ArrayList<>(parsedEntries);
                sortedByDate.sort(Comparator.comparingLong(ParsedEntry::getDateMillisYmd));
                ParsedEntry first = sortedByDate.get(0);
                ParsedEntry last = sortedByDate.get(sortedByDate.size() - 1);

                List<ParsedEntry> sortedByValue = new ArrayList<>(parsedEntries);
                sortedByValue.sort(Comparator.comparingDouble(ParsedEntry::getValue));
                ParsedEntry min = sortedByValue.get(0);
                ParsedEntry max = sortedByValue.get(sortedByValue.size() - 1);

                List<Map<String, Object>> dataList = new ArrayList<>();
            for (ParsedEntry pe : sortedByDate) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", formatDate(pe.dateMillisYmd));
                map.put("value", pe.value);
                map.put("unit", pe.unit);
                dataList.add(map);
            }
            jsonMap.put("data", dataList);



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
        if (!parsedEntries.isEmpty()) {

            sb.append("<table class='table table-sm' style='margin:auto;background-color:FloralWhite;'>");




            /*
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Parser: ")
                    .append("</td><td>")
                    .append(getName());
            sb.append("</td>");
            sb.append("</tr>");
            */





            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Average → ")
                    .append("</td><td>")
                    .append(String.format("%.2f", average))
                    .append("</td><td>")
                    .append(measurementUnit);

            sb.append("</td>");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td>");
            sb.append("First: ").append(" → ").append(formatDate(first.dateMillisYmd))
                    .append("</td><td>")
                    .append(String.format("%.2f", first.value))
                    .append("</td><td>")
                    .append(measurementUnit);
            sb.append("</td>");
            sb.append("</tr>");


            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Last: ").append(" → ").append(formatDate(last.dateMillisYmd))
                    .append("</td><td>")
                    .append(String.format("%.2f", last.value))
                    .append("</td><td>")
                    .append(measurementUnit);
            sb.append("</td>");
            sb.append("</tr>");


            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Min: ").append(" → ").append(formatDate(min.dateMillisYmd))
                    .append("</td><td>")
                    .append(String.format("%.2f", min.value))
                    .append("</td><td>")
                    .append(measurementUnit);
            sb.append("</td>");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Max: ").append(" → ").append(formatDate(max.dateMillisYmd))
                    .append("</td><td>")
                    .append(String.format("%.2f", max.value))
                    .append("</td><td>")
                    .append(measurementUnit);
            sb.append("</td>");
            sb.append("</tr>");




            sb.append("</table>");


            sb.append("<table class='table table-sm' style='margin:auto;background-color:FloralWhite;'>");






            sb.append("<tr>");
                sb.append("<td>");
                sb.append("&nbsp;");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("&gt; Avg");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("= Avg");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("&lt; Avg");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("Total");
                sb.append("</td>");
            sb.append("</tr>");



            sb.append("<tr>");
                sb.append("<td>");
                sb.append("Sum");
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", diffSumAboveAverage));
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", sumAtAverage));
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", diffSumBelowAverage));
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", sumTotal));
                sb.append("</td>");
            sb.append("</tr>");


            sb.append("<tr>");
                sb.append("<td>");
                sb.append("Count");
                sb.append("</td>");

                sb.append("<td>")
                        .append(countAboveAverage);
                sb.append("</td>");

                sb.append("<td>")
                        .append(countEqualAverage);
                sb.append("</td>");

                sb.append("<td>")
                        .append(countBelowAverage);
                sb.append("</td>");

                sb.append("<td>");
                sb.append(countTotal);
                sb.append("</td>");
            sb.append("</tr>");



            sb.append("<tr>");
                sb.append("<td>");
                sb.append("Diff");
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", diffMaximumToAverage));
                sb.append("</td>");

                sb.append("<td>");
                sb.append("-");
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", diffMinimumToAverage));
                sb.append("</td>");

                sb.append("<td>")
                        .append(String.format("%.2f", diffMaximumToMinimum));
                sb.append("</td>");
            sb.append("</tr>");


            /*
            yapilabilecekler:
            tum tarih araligi
            bos periyotlar haric
            acik sure
            asiri sure
            ikisinin mutlak degersiz toplami
             */






            sb.append("</table>");

            sb.append("<span>").append("Parser: ")
                    .append(getName());
            sb.append("</span>");

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

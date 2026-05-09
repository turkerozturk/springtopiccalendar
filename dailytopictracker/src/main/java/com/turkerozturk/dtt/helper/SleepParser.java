package com.turkerozturk.dtt.helper;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SleepParser {

    /**
     * İlk satırdaki uyku sürelerini parse eder ve toplam saniye listesi döner.
     *
     * Örnekler:
     *
     * 13                  -> [46800]
     * 13:25               -> [48300]
     * 13:25:42            -> [48342]
     * :25                 -> [1500]
     * :25:42              -> [1542]
     * ::                  -> [0]
     * :                   -> [0]
     * 0                   -> [0]
     *
     * 7:30+1:15           -> [27000, 4500]
     * 6+0:45+:20:30       -> [21600, 2700, 1230]
     */
    public static Optional<long[]> extractSleepSeconds(String noteContent) {

        if (noteContent == null || noteContent.isBlank()) {
            return Optional.of(new long[]{0});
        }

        String firstLine = noteContent.split("\\R")[0].trim();

        if (firstLine.isBlank()) {
            return Optional.of(new long[]{0});
        }

        try {

            String[] sleepParts = firstLine.split("\\+");

            List<Long> resultList = new ArrayList<>();

            for (String sleepText : sleepParts) {

                sleepText = sleepText.trim();

                long seconds = parseSingleSleep(sleepText);

                resultList.add(seconds);
            }

            long[] resultArray = new long[resultList.size()];

            for (int i = 0; i < resultList.size(); i++) {
                resultArray[i] = resultList.get(i);
            }

            return Optional.of(resultArray);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static long parseSingleSleep(String text) {

        if (text.isBlank()
                || text.equals(":")
                || text.equals("::")
                || text.equals("0")) {
            return 0L;
        }

        String[] parts = text.split(":", -1);

        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        if (parts.length == 1) {

            // 13
            hours = parsePart(parts[0]);

        } else if (parts.length == 2) {

            // 13:25
            // :25

            if (!parts[0].isBlank()) {
                hours = parsePart(parts[0]);
            }

            if (!parts[1].isBlank()) {
                minutes = parsePart(parts[1]);
            }

        } else if (parts.length == 3) {

            // 13:25:42
            // :25:42
            // 13::42

            if (!parts[0].isBlank()) {
                hours = parsePart(parts[0]);
            }

            if (!parts[1].isBlank()) {
                minutes = parsePart(parts[1]);
            }

            if (!parts[2].isBlank()) {
                seconds = parsePart(parts[2]);
            }

        } else {
            throw new IllegalArgumentException("Invalid sleep format");
        }

        return hours * 3600
                + minutes * 60
                + seconds;
    }

    private static long parsePart(String value) {
        return Long.parseLong(value.trim());
    }
}

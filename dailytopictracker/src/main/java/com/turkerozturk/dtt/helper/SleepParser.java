package com.turkerozturk.dtt.helper;

import java.util.Optional;

public class SleepParser {

    /**
     * İlk satırdaki süreyi parse eder ve toplam saniye olarak döner.
     *
     * Örnekler:
     * 13          -> 46800
     * 13:25       -> 48300
     * 13:25:42    -> 48342
     * :25         -> 1500
     * :25:42      -> 1542
     * ::          -> 0
     * :           -> 0
     * 0           -> 0
     */
    public static Optional<Long> extractSleepSeconds(String noteContent) {

        if (noteContent == null || noteContent.isBlank()) {
            return Optional.of(0L);
        }

        String firstLine = noteContent.split("\\R")[0].trim();

        if (firstLine.isBlank()
                || firstLine.equals(":")
                || firstLine.equals("::")
                || firstLine.equals("0")) {
            return Optional.of(0L);
        }

        try {

            String[] parts = firstLine.split(":", -1);

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
                return Optional.empty();
            }

            long totalSeconds =
                    hours * 3600 +
                            minutes * 60 +
                            seconds;

            return Optional.of(totalSeconds);

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static long parsePart(String value) {
        return Long.parseLong(value.trim());
    }
}



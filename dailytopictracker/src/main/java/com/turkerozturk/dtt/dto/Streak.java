package com.turkerozturk.dtt.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Streak {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayCount;
    private int percentage; // 1 ile 100 arası

    private int width; // 15 - 200 arasında
    private String color; // HTML hex color

    public Streak(LocalDate startDate, LocalDate endDate, int totalDays) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayCount = (int) (ChronoUnit.DAYS.between(startDate, endDate) + 1);

        int rawPercent = (int) Math.round((double) dayCount * 100 / totalDays);
        this.percentage = Math.max(rawPercent, 1); // en az 1

        this.width = calculateWidth(dayCount);
        this.color = ColorBand.fromPercentage(roundedPercentage()).getHex();
    }

    private int calculateWidth(int dayCount) {
        if (dayCount <= 10) {
            return 15 + (int) Math.round((dayCount - 1) * (35.0 / 9)); // 15–50
        } else if (dayCount <= 100) {
            return 51 + (int) Math.round((dayCount - 11) * (49.0 / 89)); // 51–100
        } else if (dayCount <= 1000) {
            return 101 + (int) Math.round((dayCount - 101) * (49.0 / 899)); // 101–150
        } else if (dayCount < 10000) {
            return 151 + (int) Math.round((dayCount - 1001) * (48.0 / 8999)); // 151–199
        } else {
            return 200;
        }
    }

    private int roundedPercentage() {
        return Math.min(((this.percentage + 9) / 10) * 10, 100);
    }


    enum ColorBand {
        P10(10, "#fa0268"),
        P20(20, "#d92e5a"),
        P30(30, "#b84a4c"),
        P40(40, "#97653d"),
        P50(50, "#768f2f"),
        P60(60, "#55b921"),
        P70(70, "#47d11b"),
        P80(80, "#39e815"),
        P90(90, "#2bfa0f"),
        P100(100, "#7efa02");


        private final int maxPercentage;
        private final String hex;

        ColorBand(int maxPercentage, String hex) {
            this.maxPercentage = maxPercentage;
            this.hex = hex;
        }

        public String getHex() {
            return hex;
        }

        public static ColorBand fromPercentage(int percentage) {
            for (ColorBand band : ColorBand.values()) {
                if (percentage <= band.maxPercentage) {
                    return band;
                }
            }
            return P100;
        }
    }

    // Getter'lar
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDayCount() { return dayCount; }
    public int getPercentage() { return percentage; }
    public int getWidth() { return width; }
    public String getColor() { return color; }
}

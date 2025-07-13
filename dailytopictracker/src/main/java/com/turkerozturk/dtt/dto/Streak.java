package com.turkerozturk.dtt.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Streak {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayCount;

    private int percentage; // 1 ile 100 arası


    public Streak(LocalDate startDate, LocalDate endDate, int totalDays) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayCount = (int) (ChronoUnit.DAYS.between(startDate, endDate) + 1);
        // percentage hesapla (1–100 arasında)
        int rawPercent = (int) Math.round((double) dayCount * 100 / totalDays);
        this.percentage = Math.max(rawPercent, 1); // en az 1
    }

    // Getter'lar
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDayCount() { return dayCount; }

    public int getPercentage() {
        return percentage;
    }


}

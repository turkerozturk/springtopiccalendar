package com.turkerozturk.dtt.dto;

import lombok.Data;

@Data
public class SleepDurationDto {

    private Double sleepDurationSeconds;
    private boolean sleepDurationUpdated;
    private Long sleepDurationTopicId;
    private Long sleepDurationCategoryId;
    private String sleepDurationAsString;


    public SleepDurationDto(Double sleepDurationSeconds, boolean sleepDurationUpdated, Long sleepDurationTopicId, Long sleepDurationCategoryId) {
        this.sleepDurationSeconds = sleepDurationSeconds;
        this.sleepDurationUpdated = sleepDurationUpdated;
        this.sleepDurationTopicId = sleepDurationTopicId;
        this.sleepDurationCategoryId = sleepDurationCategoryId;

        if (sleepDurationSeconds != null) {

            long totalSeconds = sleepDurationSeconds.longValue();

            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            this.sleepDurationAsString =
                    String.format("%02d:%02d:%02d", hours, minutes, seconds);

        } else {
            this.sleepDurationAsString = "00:00:00";
        }

    }
}

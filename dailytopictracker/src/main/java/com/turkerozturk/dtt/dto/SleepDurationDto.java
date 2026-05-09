package com.turkerozturk.dtt.dto;

import lombok.Data;

@Data
public class SleepDurationDto {

    private Double sleepDurationSeconds;
    private boolean sleepDurationUpdated;
    private Long sleepDurationTopicId;
    private Long sleepDurationCategoryId;

    public SleepDurationDto(Double sleepDurationSeconds, boolean sleepDurationUpdated, Long sleepDurationTopicId, Long sleepDurationCategoryId) {
        this.sleepDurationSeconds = sleepDurationSeconds;
        this.sleepDurationUpdated = sleepDurationUpdated;
        this.sleepDurationTopicId = sleepDurationTopicId;
        this.sleepDurationCategoryId = sleepDurationCategoryId;
    }
}

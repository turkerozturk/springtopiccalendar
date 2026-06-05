package com.turkerozturk.dtt.helper.datatrends;

import lombok.Data;

@Data
public class TrendSummaryDto {

    private long upCount;

    private long downCount;

    private long sameCount;

    public String getDisplayText() {
        return downCount + "↓, "
                + sameCount + ", " // ☐
                + upCount + "↑";
    }

    private double minValue;
    private double maxValue;
    private double averageValue;

    //streak
    //patternCounts


}

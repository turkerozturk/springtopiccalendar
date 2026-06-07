package com.turkerozturk.dtt.helper.datatrends;

import java.util.ArrayList;
import java.util.List;

public final class TrendUtils {

    private TrendUtils() {
    }

    public static List<TrendDirection> calculateDirections(List<Double> values) {

        List<TrendDirection> result = new ArrayList<>();

        if (values.isEmpty()) {
            return result;
        }

        result.add(TrendDirection.SAME);

        for (int i = 1; i < values.size(); i++) {

            double previous = values.get(i - 1);
            double current = values.get(i);

            if (current > previous) {
                result.add(TrendDirection.UP);
            } else if (current < previous) {
                result.add(TrendDirection.DOWN);
            } else {
                result.add(TrendDirection.SAME);
            }
        }

        return result;
    }

    public static TrendSummaryDto summarize(
            List<TrendDirection> directions) {

        TrendSummaryDto dto = new TrendSummaryDto();

        for (TrendDirection direction : directions) {

            switch (direction) {
                case UP ->
                        dto.setUpCount(dto.getUpCount() + 1);

                case DOWN ->
                        dto.setDownCount(dto.getDownCount() + 1);

                case SAME ->
                        dto.setSameCount(dto.getSameCount() + 1);
            }
        }

        return dto;
    }

    public static TrendDirection decideDirection(double value1, double value2) {
        if (value1 > value2) {
            return TrendDirection.UP;
        }
        else if (value1 < value2) {
            return TrendDirection.DOWN;
        }
        else {
            return TrendDirection.SAME;
        }
    }

    public static TrendDirection decideDirection2(double value, double lowerLimit, double upperLimit) {
        if (value > upperLimit) {
            return TrendDirection.UP;
        }
        else if (value < lowerLimit) {
            return TrendDirection.DOWN;
        }
        else {
            return TrendDirection.SAME;
        }
    }


}
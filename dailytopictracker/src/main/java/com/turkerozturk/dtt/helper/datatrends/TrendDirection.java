package com.turkerozturk.dtt.helper.datatrends;

import lombok.Getter;

public enum TrendDirection {

    UP("\uD83E\uDC65", 1),
    DOWN("\uD83E\uDC66", -1),
    SAME("=", 0);

    @Getter
    private final String symbol;
    @Getter
    private final int number;

    TrendDirection(String symbol, int number) {
        this.symbol = symbol;
        this.number = number;
    }

}
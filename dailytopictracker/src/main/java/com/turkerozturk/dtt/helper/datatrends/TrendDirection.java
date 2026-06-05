package com.turkerozturk.dtt.helper.datatrends;

import lombok.Getter;

public enum TrendDirection {

    UP("\uD83E\uDC65", "↑",1),
    DOWN("\uD83E\uDC66", "↓",-1),
    SAME("=", "=",0);

    @Getter
    private final String symbol;
    @Getter
    private final String symbol2;
    @Getter
    private final int number;

    TrendDirection(String symbol, String symbol2, int number) {
        this.symbol = symbol;
        this.symbol2 = symbol2;
        this.number = number;
    }

}
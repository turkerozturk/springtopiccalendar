package com.turkerozturk.dtt.helper.datatrends;

public class TrendValueDto<T extends Number> {

    private T value;

    private TrendDirection direction;

    public TrendValueDto() {
    }

    public TrendValueDto(T value, TrendDirection direction) {
        this.value = value;
        this.direction = direction;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public TrendDirection getDirection() {
        return direction;
    }

    public void setDirection(TrendDirection direction) {
        this.direction = direction;
    }
}
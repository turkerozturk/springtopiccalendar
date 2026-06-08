package com.turkerozturk.dtt.helper.usda;

public enum BodyMassIndexCategory {

    UNDERWEIGHT(0.0, 18.5, "Underweight"),
    NORMAL_WEIGHT(18.5, 25.0, "Normal Weight"),
    OVERWEIGHT(25.0, 30.0, "Overweight"),
    OBESE_CLASS_I(30.0, 35.0, "Obese Class I"),
    OBESE_CLASS_II(35.0, 40.0, "Obese Class II"),
    OBESE_CLASS_III(40.0, Double.MAX_VALUE, "Obese Class III");

    private final double minValue;
    private final double maxValue;
    private final String displayName;

    BodyMassIndexCategory(double minValue, double maxValue, String displayName) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayName = displayName;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BodyMassIndexCategory fromBmi(double bmi) {
        for (BodyMassIndexCategory category : values()) {
            if (bmi >= category.minValue && bmi < category.maxValue) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid BMI value: " + bmi);
    }
}
package com.pulsetrack.nutrition.model;

/** Standard total daily energy expenditure multipliers applied to the BMR. */
public enum ActivityLevel {

    SEDENTARY("Sedentary", 1.20),
    LIGHT("Lightly active", 1.375),
    MODERATE("Moderately active", 1.55),
    HIGH("Very active", 1.725),
    ATHLETE("Athlete", 1.90);

    private final String label;
    private final double multiplier;

    ActivityLevel(String label, double multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }

    public String getLabel() {
        return label;
    }

    public double getMultiplier() {
        return multiplier;
    }
}

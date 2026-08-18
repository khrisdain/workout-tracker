package com.fitnesstracker.workouttracker.model;

/**
 * The categories of training a member can log. Each constant carries the
 * presentation metadata the UI needs (label, icon, colour) plus the MET value
 * used by {@code WorkoutService} to estimate calories when a member leaves the
 * calories field blank.
 */
public enum WorkoutType {

    CARDIO("Cardio", "bi-heart-pulse", "danger", 7.0,
            "Steady state running, cycling, rowing and swimming."),
    STRENGTH("Strength", "bi-trophy", "primary", 5.0,
            "Resistance training with barbells, dumbbells or machines."),
    FLEXIBILITY("Flexibility", "bi-flower1", "success", 2.5,
            "Yoga, mobility drills and post session stretching."),
    HIIT("HIIT", "bi-lightning-charge", "warning", 9.0,
            "Short maximal intervals separated by incomplete recovery."),
    SPORTS("Sports", "bi-dribbble", "info", 6.5,
            "Court and field sport, pickup games and league play.");

    private final String label;
    private final String icon;
    private final String colour;
    private final double metValue;
    private final String description;

    WorkoutType(String label, String icon, String colour, double metValue, String description) {
        this.label = label;
        this.icon = icon;
        this.colour = colour;
        this.metValue = metValue;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getColour() {
        return colour;
    }

    public double getMetValue() {
        return metValue;
    }

    public String getDescription() {
        return description;
    }
}

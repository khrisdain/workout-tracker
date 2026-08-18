package com.fitnesstracker.workouttracker.model;

/**
 * How hard a session was relative to the member's own capacity. The intensity
 * factor scales the calorie estimate produced by {@code WorkoutService}.
 */
public enum DifficultyLevel {

    BEGINNER("Beginner", "success", 0.85, "Conversational effort, plenty left in the tank."),
    INTERMEDIATE("Intermediate", "warning", 1.0, "Challenging but repeatable week after week."),
    ADVANCED("Advanced", "danger", 1.2, "Near maximal effort that needs real recovery.");

    private final String label;
    private final String colour;
    private final double intensityFactor;
    private final String description;

    DifficultyLevel(String label, String colour, double intensityFactor, String description) {
        this.label = label;
        this.colour = colour;
        this.intensityFactor = intensityFactor;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getColour() {
        return colour;
    }

    public double getIntensityFactor() {
        return intensityFactor;
    }

    public String getDescription() {
        return description;
    }
}

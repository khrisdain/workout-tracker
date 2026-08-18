package com.fitnesstracker.workouttracker.model;

/** Mirror of the microservice enum. Multipliers live on the service side. */
public enum ActivityLevel {

    SEDENTARY("Sedentary", "Desk bound, little planned training."),
    LIGHT("Lightly active", "One to three sessions a week."),
    MODERATE("Moderately active", "Three to five sessions a week."),
    HIGH("Very active", "Six or more sessions a week."),
    ATHLETE("Athlete", "Twice daily training or physical work.");

    private final String label;
    private final String description;

    ActivityLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}

package com.fitnesstracker.workouttracker.model;

/**
 * Used only as an input to the Mifflin-St Jeor resting metabolic rate formula
 * that the nutrition microservice applies. {@code UNSPECIFIED} falls back to the
 * midpoint constant so the field can stay optional.
 */
public enum BiologicalSex {

    FEMALE("Female"),
    MALE("Male"),
    UNSPECIFIED("Prefer not to say");

    private final String label;

    BiologicalSex(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.fitnesstracker.workouttracker.model;

/**
 * Mirror of the enum owned by the nutrition microservice. Duplicating a small
 * contract like this is intentional: the two services stay independently
 * deployable and exchange plain JSON rather than a shared jar.
 */
public enum NutritionGoal {

    CUT("Cut", "Lose fat while holding on to muscle."),
    MAINTAIN("Maintain", "Hold current bodyweight and support training."),
    BULK("Bulk", "Add size with a controlled calorie surplus.");

    private final String label;
    private final String description;

    NutritionGoal(String label, String description) {
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

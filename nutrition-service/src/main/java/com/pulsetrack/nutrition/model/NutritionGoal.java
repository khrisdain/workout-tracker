package com.pulsetrack.nutrition.model;

/**
 * What the member is trying to do with their bodyweight, and the numbers the
 * calculator derives from that choice.
 *
 * @param calorieMultiplier applied to maintenance calories
 * @param proteinPerKg      grams of protein per kilogram of bodyweight
 * @param fatShare          share of daily calories that comes from fat
 */
public enum NutritionGoal {

    CUT("Cut", 0.82, 2.2, 0.25),
    MAINTAIN("Maintain", 1.00, 1.8, 0.28),
    BULK("Bulk", 1.12, 2.0, 0.25);

    private final String label;
    private final double calorieMultiplier;
    private final double proteinPerKg;
    private final double fatShare;

    NutritionGoal(String label, double calorieMultiplier, double proteinPerKg, double fatShare) {
        this.label = label;
        this.calorieMultiplier = calorieMultiplier;
        this.proteinPerKg = proteinPerKg;
        this.fatShare = fatShare;
    }

    public String getLabel() {
        return label;
    }

    public double getCalorieMultiplier() {
        return calorieMultiplier;
    }

    public double getProteinPerKg() {
        return proteinPerKg;
    }

    public double getFatShare() {
        return fatShare;
    }
}

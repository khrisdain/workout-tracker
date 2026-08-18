package com.fitnesstracker.workouttracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** Aggregate figures the microservice exposes for the admin dashboard. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NutritionStats(
        long totalPlans,
        long distinctMembers,
        int averageDailyCalories,
        int averageProteinGrams,
        Map<String, Long> plansByGoal) {

    public static NutritionStats empty() {
        return new NutritionStats(0, 0, 0, 0, Map.of());
    }
}

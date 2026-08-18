package com.pulsetrack.nutrition.dto;

import java.util.Map;

/** Aggregate figures for PulseTrack's admin dashboard. */
public record NutritionStats(
        long totalPlans,
        long distinctMembers,
        int averageDailyCalories,
        int averageProteinGrams,
        Map<String, Long> plansByGoal) {
}

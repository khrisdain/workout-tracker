package com.pulsetrack.nutrition.dto;

import com.pulsetrack.nutrition.model.ActivityLevel;
import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;

import java.time.LocalDateTime;

/** The JSON representation of a stored plan. */
public record MealPlanResponse(
        Long id,
        String memberUsername,
        String memberName,
        NutritionGoal goal,
        ActivityLevel activityLevel,
        Double weightKg,
        Integer heightCm,
        Integer age,
        Integer weeklyTrainingMinutes,
        Integer basalMetabolicRate,
        Integer maintenanceCalories,
        Integer dailyCalories,
        Integer proteinGrams,
        Integer carbGrams,
        Integer fatGrams,
        Double hydrationLitres,
        String summary,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MealPlanResponse from(MealPlan plan) {
        return new MealPlanResponse(
                plan.getId(),
                plan.getMemberUsername(),
                plan.getMemberName(),
                plan.getGoal(),
                plan.getActivityLevel(),
                plan.getWeightKg(),
                plan.getHeightCm(),
                plan.getAge(),
                plan.getWeeklyTrainingMinutes(),
                plan.getBasalMetabolicRate(),
                plan.getMaintenanceCalories(),
                plan.getDailyCalories(),
                plan.getProteinGrams(),
                plan.getCarbGrams(),
                plan.getFatGrams(),
                plan.getHydrationLitres(),
                plan.getSummary(),
                plan.getNotes(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }
}

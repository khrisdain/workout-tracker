package com.fitnesstracker.workouttracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitnesstracker.workouttracker.model.ActivityLevel;
import com.fitnesstracker.workouttracker.model.NutritionGoal;

import java.time.LocalDateTime;

/**
 * A meal plan as returned by the microservice. Unknown fields are ignored so the
 * services can evolve their contract independently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MealPlanResponse(
        Long id,
        String memberUsername,
        String memberName,
        NutritionGoal goal,
        ActivityLevel activityLevel,
        Integer basalMetabolicRate,
        Integer maintenanceCalories,
        Integer dailyCalories,
        Integer proteinGrams,
        Integer carbGrams,
        Integer fatGrams,
        Double hydrationLitres,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public int proteinCalories() {
        return proteinGrams == null ? 0 : proteinGrams * 4;
    }

    public int carbCalories() {
        return carbGrams == null ? 0 : carbGrams * 4;
    }

    public int fatCalories() {
        return fatGrams == null ? 0 : fatGrams * 9;
    }

    public int proteinPercent() {
        return percentOf(proteinCalories());
    }

    public int carbPercent() {
        return percentOf(carbCalories());
    }

    public int fatPercent() {
        return percentOf(fatCalories());
    }

    private int percentOf(int part) {
        int total = proteinCalories() + carbCalories() + fatCalories();
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }
}

package com.fitnesstracker.workouttracker.dto;

import com.fitnesstracker.workouttracker.model.ActivityLevel;
import com.fitnesstracker.workouttracker.model.BiologicalSex;
import com.fitnesstracker.workouttracker.model.NutritionGoal;

/** Payload POSTed to the nutrition microservice. */
public record MealPlanRequest(
        String memberUsername,
        String memberName,
        NutritionGoal goal,
        ActivityLevel activityLevel,
        Double weightKg,
        Integer heightCm,
        Integer age,
        BiologicalSex sex,
        Integer weeklyTrainingMinutes,
        String notes) {
}

package com.pulsetrack.nutrition.dto;

import com.pulsetrack.nutrition.model.ActivityLevel;
import com.pulsetrack.nutrition.model.BiologicalSex;
import com.pulsetrack.nutrition.model.NutritionGoal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The request body for POST and PUT. Validated at the boundary so a bad payload
 * comes back as 400 with field level detail rather than a 500.
 */
public record MealPlanRequest(

        @NotBlank(message = "memberUsername is required")
        @Size(max = 40, message = "memberUsername cannot exceed 40 characters")
        String memberUsername,

        @Size(max = 120, message = "memberName cannot exceed 120 characters")
        String memberName,

        @NotNull(message = "goal is required (CUT, MAINTAIN or BULK)")
        NutritionGoal goal,

        @NotNull(message = "activityLevel is required")
        ActivityLevel activityLevel,

        @NotNull(message = "weightKg is required")
        @DecimalMin(value = "30.0", message = "weightKg must be at least 30")
        @DecimalMax(value = "300.0", message = "weightKg cannot exceed 300")
        Double weightKg,

        @NotNull(message = "heightCm is required")
        @Min(value = 100, message = "heightCm must be at least 100")
        @Max(value = 250, message = "heightCm cannot exceed 250")
        Integer heightCm,

        @NotNull(message = "age is required")
        @Min(value = 13, message = "age must be at least 13")
        @Max(value = 100, message = "age cannot exceed 100")
        Integer age,

        BiologicalSex sex,

        @Min(value = 0, message = "weeklyTrainingMinutes cannot be negative")
        @Max(value = 2000, message = "weeklyTrainingMinutes cannot exceed 2000")
        Integer weeklyTrainingMinutes,

        @Size(max = 300, message = "notes cannot exceed 300 characters")
        String notes) {
}

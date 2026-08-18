package com.fitnesstracker.workouttracker.dto;

import com.fitnesstracker.workouttracker.model.ActivityLevel;
import com.fitnesstracker.workouttracker.model.BiologicalSex;
import com.fitnesstracker.workouttracker.model.NutritionGoal;
import com.fitnesstracker.workouttracker.model.User;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The form a member fills in to have the microservice build them a plan. */
public class NutritionPlanForm {

    @NotNull(message = "Choose a goal")
    private NutritionGoal goal = NutritionGoal.MAINTAIN;

    @NotNull(message = "Choose an activity level")
    private ActivityLevel activityLevel = ActivityLevel.MODERATE;

    @NotNull(message = "Bodyweight is required")
    @DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    private Double weightKg;

    @NotNull(message = "Height is required")
    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height cannot exceed 250 cm")
    private Integer heightCm;

    @NotNull(message = "Age is required")
    @Min(value = 13, message = "You must be at least 13")
    @Max(value = 100, message = "Age cannot exceed 100")
    private Integer age;

    @NotNull(message = "Choose an option")
    private BiologicalSex sex = BiologicalSex.UNSPECIFIED;

    @Size(max = 300, message = "Notes cannot exceed 300 characters")
    private String notes;

    /** Pre-fills the form from whatever the member already saved on their profile. */
    public static NutritionPlanForm prefilledFor(User user) {
        NutritionPlanForm form = new NutritionPlanForm();
        form.weightKg = user.getWeightKg();
        form.heightCm = user.getHeightCm();
        form.age = user.getAge();
        form.sex = user.getSex() == null ? BiologicalSex.UNSPECIFIED : user.getSex();
        return form;
    }

    public MealPlanRequest toRequest(User user, int weeklyTrainingMinutes) {
        return new MealPlanRequest(user.getUsername(), user.getFullName(), goal, activityLevel,
                weightKg, heightCm, age, sex, weeklyTrainingMinutes, notes);
    }

    public NutritionGoal getGoal() {
        return goal;
    }

    public void setGoal(NutritionGoal goal) {
        this.goal = goal;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public BiologicalSex getSex() {
        return sex;
    }

    public void setSex(BiologicalSex sex) {
        this.sex = sex;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

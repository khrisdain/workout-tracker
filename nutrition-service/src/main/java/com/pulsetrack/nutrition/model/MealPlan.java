package com.pulsetrack.nutrition.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * The resource this microservice owns. The inputs are stored alongside the
 * computed outputs so a plan can be explained and recalculated later, and so the
 * service never has to call back into PulseTrack for context.
 */
@Entity
@Table(name = "meal_plans", indexes = {
        @Index(name = "idx_meal_plans_member", columnList = "member_username"),
        @Index(name = "idx_meal_plans_goal", columnList = "goal")
})
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- inputs supplied by the caller ----

    @Column(name = "member_username", nullable = false, length = 40)
    private String memberUsername;

    @Column(name = "member_name", length = 120)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NutritionGoal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    private ActivityLevel activityLevel;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BiologicalSex sex;

    @Column(name = "weekly_training_minutes")
    private Integer weeklyTrainingMinutes;

    @Column(length = 300)
    private String notes;

    // ---- outputs computed by MacroCalculator ----

    @Column(name = "basal_metabolic_rate", nullable = false)
    private Integer basalMetabolicRate;

    @Column(name = "maintenance_calories", nullable = false)
    private Integer maintenanceCalories;

    @Column(name = "daily_calories", nullable = false)
    private Integer dailyCalories;

    @Column(name = "protein_grams", nullable = false)
    private Integer proteinGrams;

    @Column(name = "carb_grams", nullable = false)
    private Integer carbGrams;

    @Column(name = "fat_grams", nullable = false)
    private Integer fatGrams;

    @Column(name = "hydration_litres")
    private Double hydrationLitres;

    @Column(length = 400)
    private String summary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- getters / setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberUsername() {
        return memberUsername;
    }

    public void setMemberUsername(String memberUsername) {
        this.memberUsername = memberUsername;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
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

    public Integer getWeeklyTrainingMinutes() {
        return weeklyTrainingMinutes;
    }

    public void setWeeklyTrainingMinutes(Integer weeklyTrainingMinutes) {
        this.weeklyTrainingMinutes = weeklyTrainingMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getBasalMetabolicRate() {
        return basalMetabolicRate;
    }

    public void setBasalMetabolicRate(Integer basalMetabolicRate) {
        this.basalMetabolicRate = basalMetabolicRate;
    }

    public Integer getMaintenanceCalories() {
        return maintenanceCalories;
    }

    public void setMaintenanceCalories(Integer maintenanceCalories) {
        this.maintenanceCalories = maintenanceCalories;
    }

    public Integer getDailyCalories() {
        return dailyCalories;
    }

    public void setDailyCalories(Integer dailyCalories) {
        this.dailyCalories = dailyCalories;
    }

    public Integer getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(Integer proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public Integer getCarbGrams() {
        return carbGrams;
    }

    public void setCarbGrams(Integer carbGrams) {
        this.carbGrams = carbGrams;
    }

    public Integer getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(Integer fatGrams) {
        this.fatGrams = fatGrams;
    }

    public Double getHydrationLitres() {
        return hydrationLitres;
    }

    public void setHydrationLitres(Double hydrationLitres) {
        this.hydrationLitres = hydrationLitres;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

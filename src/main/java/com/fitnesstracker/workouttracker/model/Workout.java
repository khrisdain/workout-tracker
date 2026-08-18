package com.fitnesstracker.workouttracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single training session logged by a member. This is the primary entity of
 * the application: the public form creates it, the list view filters and sorts
 * it, and the dashboards aggregate it.
 */
@Entity
@Table(name = "workouts", indexes = {
        @Index(name = "idx_workouts_date", columnList = "date_logged"),
        @Index(name = "idx_workouts_type", columnList = "type")
})
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Give the session a name so you can find it later")
    @Size(min = 3, max = 80, message = "Session name must be between 3 and 80 characters")
    @Column(nullable = false, length = 80)
    private String name;

    @NotNull(message = "Choose a workout type")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutType type;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 300, message = "Duration cannot exceed 300 minutes")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @NotNull(message = "Choose a difficulty level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Min(value = 0, message = "Calories burned cannot be negative")
    @Max(value = 5000, message = "Calories burned cannot exceed 5000")
    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @NotNull(message = "Pick the date you trained")
    @PastOrPresent(message = "You cannot log a session in the future")
    @Column(name = "date_logged", nullable = false)
    private LocalDate dateLogged;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    /**
     * The member the session belongs to. Fetched eagerly on purpose: the app runs
     * with {@code spring.jpa.open-in-view: false}, so a lazy proxy would blow up
     * the moment a Thymeleaf template asked for the owner's name.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_workouts_owner"))
    private User owner;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Workout() {
    }

    public Workout(String name, WorkoutType type, Integer durationMinutes, DifficultyLevel difficulty,
                   Integer caloriesBurned, LocalDate dateLogged) {
        this.name = name;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.difficulty = difficulty;
        this.caloriesBurned = caloriesBurned;
        this.dateLogged = dateLogged;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Training load in minutes weighted by how hard the session felt. */
    public int getTrainingLoad() {
        if (durationMinutes == null || difficulty == null) {
            return 0;
        }
        return (int) Math.round(durationMinutes * difficulty.getIntensityFactor());
    }

    public String getOwnerName() {
        return owner == null ? "PulseTrack demo" : owner.getFullName();
    }

    // ---- getters / setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkoutType getType() {
        return type;
    }

    public void setType(WorkoutType type) {
        this.type = type;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(Integer caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public LocalDate getDateLogged() {
        return dateLogged;
    }

    public void setDateLogged(LocalDate dateLogged) {
        this.dateLogged = dateLogged;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

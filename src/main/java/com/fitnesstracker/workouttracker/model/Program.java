package com.fitnesstracker.workouttracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A multi week training block authored by a coach. Members enrol in a program;
 * coaches and administrators are the only roles allowed to create or edit one,
 * which is where the role model earns its keep.
 */
@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Program name is required")
    @Size(min = 3, max = 80, message = "Program name must be between 3 and 80 characters")
    @Column(nullable = false, length = 80)
    private String name;

    @NotBlank(message = "Describe what this program is for")
    @Size(min = 20, max = 600, message = "Description must be between 20 and 600 characters")
    @Column(nullable = false, length = 600)
    private String description;

    @NotNull(message = "Choose the training focus")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutType focus;

    @NotNull(message = "Choose a difficulty level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @NotNull(message = "Length in weeks is required")
    @Min(value = 1, message = "A program runs for at least 1 week")
    @Max(value = 52, message = "A program cannot run longer than 52 weeks")
    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @NotNull(message = "Sessions per week is required")
    @Min(value = 1, message = "At least 1 session per week")
    @Max(value = 14, message = "At most 14 sessions per week")
    @Column(name = "sessions_per_week", nullable = false)
    private Integer sessionsPerWeek;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_programs_coach"))
    private User coach;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "program_members",
            joinColumns = @JoinColumn(name = "program_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public int getTotalSessions() {
        if (durationWeeks == null || sessionsPerWeek == null) {
            return 0;
        }
        return durationWeeks * sessionsPerWeek;
    }

    public boolean isEnrolled(String username) {
        return members.stream().anyMatch(m -> m.getUsername().equals(username));
    }

    public String getCoachName() {
        return coach == null ? "PulseTrack staff" : coach.getFullName();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkoutType getFocus() {
        return focus;
    }

    public void setFocus(WorkoutType focus) {
        this.focus = focus;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(Integer durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public Integer getSessionsPerWeek() {
        return sessionsPerWeek;
    }

    public void setSessionsPerWeek(Integer sessionsPerWeek) {
        this.sessionsPerWeek = sessionsPerWeek;
    }

    public User getCoach() {
        return coach;
    }

    public void setCoach(User coach) {
        this.coach = coach;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

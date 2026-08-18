package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.config.PulseTrackProperties;
import com.fitnesstracker.workouttracker.model.ActivityLevel;
import com.fitnesstracker.workouttracker.model.BiologicalSex;
import com.fitnesstracker.workouttracker.model.DifficultyLevel;
import com.fitnesstracker.workouttracker.model.NutritionGoal;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.List;

/**
 * Model attributes every template can rely on: branding, the signed in account,
 * the enum option lists that populate the dropdowns, and the active profile that
 * the footer badge displays.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final PulseTrackProperties properties;
    private final Environment environment;

    public GlobalModelAdvice(PulseTrackProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @ModelAttribute("brand")
    public PulseTrackProperties.Branding brand() {
        return properties.getBranding();
    }

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal User user) {
        return user;
    }

    @ModelAttribute("workoutTypes")
    public WorkoutType[] workoutTypes() {
        return WorkoutType.values();
    }

    @ModelAttribute("difficultyLevels")
    public DifficultyLevel[] difficultyLevels() {
        return DifficultyLevel.values();
    }

    @ModelAttribute("allRoles")
    public Role[] allRoles() {
        return Role.values();
    }

    @ModelAttribute("nutritionGoals")
    public NutritionGoal[] nutritionGoals() {
        return NutritionGoal.values();
    }

    @ModelAttribute("activityLevels")
    public ActivityLevel[] activityLevels() {
        return ActivityLevel.values();
    }

    @ModelAttribute("sexOptions")
    public BiologicalSex[] sexOptions() {
        return BiologicalSex.values();
    }

    @ModelAttribute("activeProfiles")
    public List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? List.of("default") : Arrays.asList(profiles);
    }
}

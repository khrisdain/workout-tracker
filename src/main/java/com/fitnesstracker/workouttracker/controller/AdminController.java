package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.client.NutritionServiceClient;
import com.fitnesstracker.workouttracker.dto.MealPlanResponse;
import com.fitnesstracker.workouttracker.dto.NutritionStats;
import com.fitnesstracker.workouttracker.dto.RemoteResult;
import com.fitnesstracker.workouttracker.dto.WorkoutFilter;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.service.DashboardService;
import com.fitnesstracker.workouttracker.service.ProgramService;
import com.fitnesstracker.workouttracker.service.UserService;
import com.fitnesstracker.workouttracker.service.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The administrator console. Every route here sits behind
 * {@code hasRole("ADMIN")} in {@link com.fitnesstracker.workouttracker.config.SecurityConfig}.
 *
 * <p>The landing page is the one screen in the system that reads from both the
 * local database and the remote nutrition microservice at the same time.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService users;
    private final WorkoutService workouts;
    private final ProgramService programs;
    private final DashboardService dashboard;
    private final NutritionServiceClient nutrition;

    public AdminController(UserService users, WorkoutService workouts, ProgramService programs,
                           DashboardService dashboard, NutritionServiceClient nutrition) {
        this.users = users;
        this.workouts = workouts;
        this.programs = programs;
        this.dashboard = dashboard;
        this.nutrition = nutrition;
    }

    /** Local database figures on the left, live microservice figures on the right. */
    @GetMapping
    public String console(Model model) {
        RemoteResult<NutritionStats> remote = nutrition.stats();

        model.addAttribute("summary", dashboard.global());
        model.addAttribute("userCount", users.count());
        model.addAttribute("adminCount", users.countByRole(Role.ADMIN));
        model.addAttribute("coachCount", users.countByRole(Role.COACH));
        model.addAttribute("memberCount", users.countByRole(Role.MEMBER));
        model.addAttribute("programCount", programs.count());
        model.addAttribute("recentUsers", users.findAll().stream().limit(5).toList());
        model.addAttribute("nutritionStats", remote.data() == null ? NutritionStats.empty() : remote.data());
        model.addAttribute("serviceStatus", remote.status());
        model.addAttribute("pageTitle", "Admin console");
        return "admin/console";
    }

    // ------------------------------------------------------------------
    // Users
    // ------------------------------------------------------------------

    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("pageTitle", "Manage users");
        return "admin/users";
    }

    @PostMapping("/users/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(name = "roles", required = false) List<Role> roles,
                              RedirectAttributes redirectAttributes) {
        try {
            Set<Role> requested = (roles == null || roles.isEmpty())
                    ? EnumSet.noneOf(Role.class)
                    : EnumSet.copyOf(roles);
            User updated = users.replaceRoles(id, requested);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Roles for " + updated.getUsername() + " updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String toggleStatus(@PathVariable Long id,
                               @RequestParam boolean enabled,
                               RedirectAttributes redirectAttributes) {
        try {
            User updated = users.setEnabled(id, enabled);
            redirectAttributes.addFlashAttribute("successMessage",
                    updated.getUsername() + (enabled ? " can sign in again." : " is now suspended."));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            users.delete(id, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Account deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ------------------------------------------------------------------
    // Workouts
    // ------------------------------------------------------------------

    @GetMapping("/workouts")
    public String workoutAdmin(@ModelAttribute("filter") WorkoutFilter filter, Model model) {
        filter.setSize(15);
        model.addAttribute("page", workouts.search(filter, null));
        model.addAttribute("sortableFields", WorkoutFilter.SORTABLE_FIELDS);
        model.addAttribute("pageTitle", "Manage workouts");
        return "admin/workouts";
    }

    @DeleteMapping("/workouts/{id}")
    public String deleteWorkout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        workouts.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Session deleted.");
        return "redirect:/admin/workouts";
    }

    // ------------------------------------------------------------------
    // Programs
    // ------------------------------------------------------------------

    @GetMapping("/programs")
    public String programAdmin(Model model) {
        model.addAttribute("programs", programs.findAll());
        model.addAttribute("pageTitle", "Manage programs");
        return "admin/programs";
    }

    // ------------------------------------------------------------------
    // Remote nutrition service
    // ------------------------------------------------------------------

    @GetMapping("/nutrition")
    public String nutritionAdmin(@RequestParam(required = false) String member,
                                 @RequestParam(required = false) String goal,
                                 @RequestParam(required = false) Integer minCalories,
                                 @RequestParam(required = false) Integer maxCalories,
                                 Model model) {
        RemoteResult<List<MealPlanResponse>> plans =
                nutrition.search(member, goal, minCalories, maxCalories);
        RemoteResult<NutritionStats> stats = nutrition.stats();

        model.addAttribute("plans", plans.data() == null ? List.of() : plans.data());
        model.addAttribute("serviceStatus", plans.status());
        model.addAttribute("nutritionStats", stats.data() == null ? NutritionStats.empty() : stats.data());
        model.addAttribute("member", member);
        model.addAttribute("goal", goal);
        model.addAttribute("minCalories", minCalories);
        model.addAttribute("maxCalories", maxCalories);
        model.addAttribute("pageTitle", "Nutrition service");
        return "admin/nutrition";
    }

    @DeleteMapping("/nutrition/{id}")
    public String deleteRemotePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RemoteResult<Boolean> result = nutrition.delete(id);
        if (result.isAvailable()) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Plan " + id + " deleted from the nutrition service.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.status().message());
        }
        return "redirect:/admin/nutrition";
    }
}

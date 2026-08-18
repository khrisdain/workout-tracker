package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.client.NutritionServiceClient;
import com.fitnesstracker.workouttracker.dto.MealPlanResponse;
import com.fitnesstracker.workouttracker.dto.NutritionPlanForm;
import com.fitnesstracker.workouttracker.dto.RemoteResult;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.repository.WorkoutRepository;
import com.fitnesstracker.workouttracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * The member facing half of the microservice integration: this controller reads
 * and writes meal plans that live in a completely separate Spring Boot
 * application and database, reached over HTTP with Basic Auth.
 *
 * <p>Nothing here throws when the remote service is down. Every action inspects
 * {@link RemoteResult#isAvailable()} and renders the page in a degraded state
 * with an explanation instead.
 */
@Controller
@RequestMapping("/nutrition")
public class NutritionController {

    private final NutritionServiceClient nutrition;
    private final UserService users;
    private final WorkoutRepository workouts;

    public NutritionController(NutritionServiceClient nutrition, UserService users, WorkoutRepository workouts) {
        this.nutrition = nutrition;
        this.users = users;
        this.workouts = workouts;
    }

    @GetMapping
    public String plan(@AuthenticationPrincipal User currentUser, Model model) {
        User account = users.require(currentUser.getUsername());
        RemoteResult<MealPlanResponse> latest = nutrition.latestFor(account.getUsername());

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", NutritionPlanForm.prefilledFor(account));
        }
        model.addAttribute("plan", latest.data());
        model.addAttribute("serviceStatus", latest.status());
        model.addAttribute("weeklyMinutes", weeklyMinutes(account.getUsername()));
        model.addAttribute("pageTitle", "Nutrition plan");
        return "nutrition";
    }

    @PostMapping
    public String requestPlan(@Valid @ModelAttribute("form") NutritionPlanForm form,
                              BindingResult binding,
                              @AuthenticationPrincipal User currentUser,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User account = users.require(currentUser.getUsername());

        if (binding.hasErrors()) {
            RemoteResult<MealPlanResponse> latest = nutrition.latestFor(account.getUsername());
            model.addAttribute("plan", latest.data());
            model.addAttribute("serviceStatus", latest.status());
            model.addAttribute("weeklyMinutes", weeklyMinutes(account.getUsername()));
            model.addAttribute("pageTitle", "Nutrition plan");
            return "nutrition";
        }

        // Remember what the member typed so the profile and the next request agree.
        users.rememberMetrics(account.getUsername(), form.getWeightKg(), form.getHeightCm(),
                form.getAge(), form.getSex());

        RemoteResult<MealPlanResponse> created =
                nutrition.create(form.toRequest(account, weeklyMinutes(account.getUsername())));

        if (created.isAvailable() && created.data() != null) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "The nutrition service built you a " + created.data().dailyCalories()
                            + " kcal plan for your " + form.getGoal().getLabel().toLowerCase() + " phase.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", created.status().message());
        }
        return "redirect:/nutrition";
    }

    @DeleteMapping("/{id}")
    public String deletePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RemoteResult<Boolean> result = nutrition.delete(id);
        if (result.isAvailable()) {
            redirectAttributes.addFlashAttribute("successMessage", "Plan deleted from the nutrition service.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.status().message());
        }
        return "redirect:/nutrition";
    }

    /** Training volume over the last seven days, sent to the service as context. */
    private int weeklyMinutes(String username) {
        return (int) workouts.minutesForUserSince(username, LocalDate.now().minusDays(6));
    }
}

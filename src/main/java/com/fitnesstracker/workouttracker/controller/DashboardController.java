package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.dto.ProfileForm;
import com.fitnesstracker.workouttracker.exception.DuplicateAccountException;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.service.DashboardService;
import com.fitnesstracker.workouttracker.service.ProgramService;
import com.fitnesstracker.workouttracker.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The signed in member's home: personal stats, recent sessions, and profile editing. */
@Controller
public class DashboardController {

    private final DashboardService dashboard;
    private final WorkoutService workouts;
    private final ProgramService programs;
    private final com.fitnesstracker.workouttracker.service.UserService users;

    public DashboardController(DashboardService dashboard, WorkoutService workouts, ProgramService programs,
                               com.fitnesstracker.workouttracker.service.UserService users) {
        this.dashboard = dashboard;
        this.workouts = workouts;
        this.programs = programs;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("summary", dashboard.forMember(currentUser.getUsername()));
        model.addAttribute("recentWorkouts", workouts.recentFor(currentUser.getUsername()));
        model.addAttribute("myPrograms", programs.findEnrolled(currentUser.getUsername()));
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User currentUser, Model model) {
        User fresh = users.require(currentUser.getUsername());
        model.addAttribute("form", ProfileForm.from(fresh));
        model.addAttribute("account", fresh);
        model.addAttribute("pageTitle", "My profile");
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("form") ProfileForm form,
                                BindingResult binding,
                                @AuthenticationPrincipal User currentUser,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!binding.hasErrors()) {
            try {
                users.updateProfile(currentUser.getUsername(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Profile saved.");
                return "redirect:/profile";
            } catch (DuplicateAccountException e) {
                binding.addError(new FieldError("form", e.getField(), form.getEmail(), false,
                        null, null, e.getMessage()));
            }
        }
        model.addAttribute("account", users.require(currentUser.getUsername()));
        model.addAttribute("pageTitle", "My profile");
        return "profile";
    }
}

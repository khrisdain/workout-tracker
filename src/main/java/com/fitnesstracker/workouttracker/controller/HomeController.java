package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.model.DifficultyLevel;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import com.fitnesstracker.workouttracker.service.DashboardService;
import com.fitnesstracker.workouttracker.service.ProgramService;
import com.fitnesstracker.workouttracker.service.UserService;
import com.fitnesstracker.workouttracker.service.WorkoutService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The three public informational pages: landing, about, and the training guide. */
@Controller
public class HomeController {

    private final WorkoutService workouts;
    private final ProgramService programs;
    private final UserService users;
    private final DashboardService dashboard;

    public HomeController(WorkoutService workouts, ProgramService programs, UserService users,
                          DashboardService dashboard) {
        this.workouts = workouts;
        this.programs = programs;
        this.users = users;
        this.dashboard = dashboard;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("summary", dashboard.global());
        model.addAttribute("recentWorkouts", workouts.recentFor(null));
        model.addAttribute("featuredPrograms", programs.findPublished().stream().limit(3).toList());
        model.addAttribute("memberCount", users.count());
        model.addAttribute("pageTitle", "Train with intent");
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("workoutCount", workouts.count());
        model.addAttribute("programCount", programs.count());
        model.addAttribute("memberCount", users.count());
        model.addAttribute("coachCount", users.countByRole(Role.COACH));
        model.addAttribute("pageTitle", "About PulseTrack");
        return "about";
    }

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("types", WorkoutType.values());
        model.addAttribute("levels", DifficultyLevel.values());
        model.addAttribute("pageTitle", "Training guide");
        return "guide";
    }
}

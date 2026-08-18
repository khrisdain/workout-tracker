package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.dto.WorkoutFilter;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.model.Workout;
import com.fitnesstracker.workouttracker.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * The primary entity's CRUD surface.
 *
 * <p>Browsing is public; creating, editing and deleting require a session and,
 * for members, ownership of the row. Filtering, sorting and paging are handed
 * straight to Spring Data through {@link WorkoutFilter}.
 */
@Controller
@RequestMapping("/workouts")
public class WorkoutController {

    private final WorkoutService workouts;

    public WorkoutController(WorkoutService workouts) {
        this.workouts = workouts;
    }

    /** Filterable, sortable, paged list. {@code mine=true} narrows it to the signed in member. */
    @GetMapping
    public String list(@ModelAttribute("filter") WorkoutFilter filter,
                       @RequestParam(defaultValue = "false") boolean mine,
                       @AuthenticationPrincipal User currentUser,
                       Model model) {

        String owner = (mine && currentUser != null) ? currentUser.getUsername() : null;
        Page<Workout> page = workouts.search(filter, owner);

        model.addAttribute("page", page);
        model.addAttribute("mine", mine);
        model.addAttribute("sortableFields", WorkoutFilter.SORTABLE_FIELDS);
        model.addAttribute("pageTitle", mine ? "My training log" : "Training log");
        return "workouts/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Workout workout = new Workout();
        workout.setDateLogged(LocalDate.now());
        model.addAttribute("workout", workout);
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "Log a workout");
        return "workouts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("workout") Workout workout,
                         BindingResult binding,
                         @AuthenticationPrincipal User currentUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "Log a workout");
            return "workouts/form";
        }
        Workout saved = workouts.create(workout, currentUser);
        redirectAttributes.addFlashAttribute("successMessage",
                "\"" + saved.getName() + "\" is in the log.");
        return "redirect:/workouts/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal User currentUser,
                         Model model) {
        Workout workout = workouts.get(id);
        model.addAttribute("workout", workout);
        model.addAttribute("canModify", workouts.canModify(workout, currentUser));
        model.addAttribute("pageTitle", workout.getName());
        return "workouts/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal User currentUser,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Workout workout = workouts.get(id);
        if (!workouts.canModify(workout, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "That session belongs to another member, so you cannot edit it.");
            return "redirect:/workouts/" + id;
        }
        model.addAttribute("workout", workout);
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit " + workout.getName());
        return "workouts/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("workout") Workout workout,
                         BindingResult binding,
                         @AuthenticationPrincipal User currentUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Workout existing = workouts.get(id);
        if (!workouts.canModify(existing, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "That session belongs to another member, so you cannot edit it.");
            return "redirect:/workouts/" + id;
        }
        if (binding.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit session");
            return "workouts/form";
        }
        workouts.update(id, workout);
        redirectAttributes.addFlashAttribute("successMessage", "Session updated.");
        return "redirect:/workouts/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        Workout workout = workouts.get(id);
        if (!workouts.canModify(workout, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "That session belongs to another member, so you cannot delete it.");
            return "redirect:/workouts/" + id;
        }
        workouts.delete(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "\"" + workout.getName() + "\" was removed from the log.");
        return "redirect:/workouts" + (currentUser != null && !currentUser.hasRole(Role.ADMIN) ? "?mine=true" : "");
    }
}

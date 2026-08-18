package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.model.Program;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.service.ProgramService;
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

/**
 * Training programs. Browsing is public, enrolling needs an account, and
 * authoring is restricted to COACH and ADMIN by both the filter chain and the
 * ownership check in {@link ProgramService#canModify}.
 */
@Controller
@RequestMapping("/programs")
public class ProgramController {

    private final ProgramService programs;
    private final UserService users;

    public ProgramController(ProgramService programs, UserService users) {
        this.programs = programs;
        this.users = users;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("programs", programs.findPublished());
        model.addAttribute("myPrograms", currentUser == null
                ? java.util.List.of()
                : programs.findEnrolled(currentUser.getUsername()));
        model.addAttribute("pageTitle", "Training programs");
        return "programs/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("program", new Program());
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "New program");
        return "programs/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("program") Program program,
                         BindingResult binding,
                         @AuthenticationPrincipal User currentUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "New program");
            return "programs/form";
        }
        Program saved = programs.create(program, users.require(currentUser.getUsername()));
        redirectAttributes.addFlashAttribute("successMessage", "Program \"" + saved.getName() + "\" published.");
        return "redirect:/programs/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal User currentUser, Model model) {
        Program program = programs.get(id);
        model.addAttribute("program", program);
        model.addAttribute("canModify", programs.canModify(program, currentUser));
        model.addAttribute("enrolled", currentUser != null && program.isEnrolled(currentUser.getUsername()));
        model.addAttribute("pageTitle", program.getName());
        return "programs/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                           Model model, RedirectAttributes redirectAttributes) {
        Program program = programs.get(id);
        if (!programs.canModify(program, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only the coach who wrote this program, or an administrator, can edit it.");
            return "redirect:/programs/" + id;
        }
        model.addAttribute("program", program);
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit " + program.getName());
        return "programs/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("program") Program program,
                         BindingResult binding,
                         @AuthenticationPrincipal User currentUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Program existing = programs.get(id);
        if (!programs.canModify(existing, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only the coach who wrote this program, or an administrator, can edit it.");
            return "redirect:/programs/" + id;
        }
        if (binding.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit program");
            return "programs/form";
        }
        programs.update(id, program);
        redirectAttributes.addFlashAttribute("successMessage", "Program updated.");
        return "redirect:/programs/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        Program program = programs.get(id);
        if (!programs.canModify(program, currentUser)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only the coach who wrote this program, or an administrator, can delete it.");
            return "redirect:/programs/" + id;
        }
        programs.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Program removed.");
        return "redirect:/programs";
    }

    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                         RedirectAttributes redirectAttributes) {
        User member = users.require(currentUser.getUsername());
        boolean added = programs.enrol(id, member);
        redirectAttributes.addFlashAttribute("successMessage",
                added ? "You are enrolled. The program now shows on your dashboard."
                      : "You were already enrolled in that program.");
        return "redirect:/programs/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                           RedirectAttributes redirectAttributes) {
        User member = users.require(currentUser.getUsername());
        programs.withdraw(id, member);
        redirectAttributes.addFlashAttribute("infoMessage", "You have left that program.");
        return "redirect:/programs/" + id;
    }
}

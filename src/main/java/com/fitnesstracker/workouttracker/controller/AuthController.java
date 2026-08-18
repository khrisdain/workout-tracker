package com.fitnesstracker.workouttracker.controller;

import com.fitnesstracker.workouttracker.dto.RegistrationForm;
import com.fitnesstracker.workouttracker.exception.DuplicateAccountException;
import com.fitnesstracker.workouttracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Login page, self service registration and the access denied page. */
@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered,
                        Authentication authentication,
                        Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (error != null) {
            model.addAttribute("errorMessage",
                    "That username and password combination did not match an active account.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been signed out. See you at the next session.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created. Sign in to log your first workout.");
        }
        model.addAttribute("pageTitle", "Sign in");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute("pageTitle", "Create your account");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult binding,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (!form.passwordsMatch()) {
            binding.rejectValue("confirmPassword", "passwords.mismatch", "The two passwords do not match");
        }
        if (!binding.hasErrors()) {
            try {
                users.register(form);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Welcome to PulseTrack, " + form.getFullName() + ". Sign in to get started.");
                return "redirect:/login?registered";
            } catch (DuplicateAccountException e) {
                binding.addError(new FieldError("form", e.getField(), form.getUsername(), false,
                        null, null, e.getMessage()));
            }
        }
        model.addAttribute("pageTitle", "Create your account");
        return "auth/register";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Not your area");
        return "error/access-denied";
    }
}

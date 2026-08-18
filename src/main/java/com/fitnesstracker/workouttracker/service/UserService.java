package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.dto.ProfileForm;
import com.fitnesstracker.workouttracker.dto.RegistrationForm;
import com.fitnesstracker.workouttracker.exception.DuplicateAccountException;
import com.fitnesstracker.workouttracker.exception.ResourceNotFoundException;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Account lifecycle plus the {@link UserDetailsService} Spring Security
 * authenticates against. Passwords are BCrypt encoded on the way in and never
 * read back out in plain text.
 */
@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + username));
    }

    public User require(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + username));
    }

    public User requireById(Long id) {
        return users.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    /** Self service registration. New accounts always start as MEMBER. */
    @Transactional
    public User register(RegistrationForm form) {
        if (users.existsByUsernameIgnoreCase(form.getUsername())) {
            throw new DuplicateAccountException("username",
                    "That username is already taken, pick another one");
        }
        if (users.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateAccountException("email",
                    "An account already exists for that email address");
        }

        User user = new User(
                form.getUsername().trim(),
                form.getEmail().trim().toLowerCase(),
                form.getFullName().trim(),
                passwordEncoder.encode(form.getPassword()),
                EnumSet.of(Role.MEMBER));
        return users.save(user);
    }

    @Transactional
    public User updateProfile(String username, ProfileForm form) {
        User user = require(username);
        String newEmail = form.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateAccountException("email", "Another account already uses that email address");
        }
        user.setFullName(form.getFullName().trim());
        user.setEmail(newEmail);
        user.setHeightCm(form.getHeightCm());
        user.setWeightKg(form.getWeightKg());
        user.setAge(form.getAge());
        user.setSex(form.getSex());
        return users.save(user);
    }

    /** Keeps a member's saved metrics in step with what they typed into the nutrition form. */
    @Transactional
    public void rememberMetrics(String username, Double weightKg, Integer heightCm, Integer age,
                                com.fitnesstracker.workouttracker.model.BiologicalSex sex) {
        User user = require(username);
        user.setWeightKg(weightKg);
        user.setHeightCm(heightCm);
        user.setAge(age);
        user.setSex(sex);
        users.save(user);
    }

    // ---- administration ----

    public List<User> findAll() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    public List<User> findMembers() {
        return users.findByRole(Role.MEMBER);
    }

    public List<User> findCoaches() {
        return users.findByRole(Role.COACH);
    }

    public long countByRole(Role role) {
        return users.countByRole(role);
    }

    public long count() {
        return users.count();
    }

    @Transactional
    public User replaceRoles(Long userId, Set<Role> roles) {
        User user = requireById(userId);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("An account must keep at least one role");
        }
        if (user.hasRole(Role.ADMIN) && !roles.contains(Role.ADMIN) && countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("This is the last administrator, its ADMIN role cannot be removed");
        }
        user.setRoles(EnumSet.copyOf(roles));
        return users.save(user);
    }

    @Transactional
    public User setEnabled(Long userId, boolean enabled) {
        User user = requireById(userId);
        if (!enabled && user.hasRole(Role.ADMIN) && countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("The last administrator cannot be disabled");
        }
        user.setEnabled(enabled);
        return users.save(user);
    }

    @Transactional
    public void delete(Long userId, String actingUsername) {
        User user = requireById(userId);
        if (user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalArgumentException("You cannot delete the account you are signed in with");
        }
        if (user.hasRole(Role.ADMIN) && countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("The last administrator cannot be deleted");
        }
        users.delete(user);
    }
}

package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.dto.RegistrationForm;
import com.fitnesstracker.workouttracker.exception.DuplicateAccountException;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegistrationForm validForm() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegistrationForm form = new RegistrationForm();
        form.setUsername("user" + unique);
        form.setFullName("New Member");
        form.setEmail(unique + "@test.local");
        form.setPassword("Passw0rdd");
        form.setConfirmPassword("Passw0rdd");
        return form;
    }

    @Test
    @DisplayName("registration stores a BCrypt hash, never the plain password")
    void encodesPassword() {
        RegistrationForm form = validForm();

        User saved = users.register(form);

        assertThat(saved.getPassword()).isNotEqualTo("Passw0rdd");
        assertThat(saved.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("Passw0rdd", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("new accounts get the MEMBER role and nothing more")
    void defaultsToMemberRole() {
        User saved = users.register(validForm());

        assertThat(saved.getRoles()).containsExactly(Role.MEMBER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("a duplicate username is rejected with a message the form can show")
    void rejectsDuplicateUsername() {
        RegistrationForm form = validForm();
        form.setUsername("jordan");

        assertThatThrownBy(() -> users.register(form))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    @DisplayName("a duplicate email is rejected too")
    void rejectsDuplicateEmail() {
        RegistrationForm form = validForm();
        form.setEmail("jordan@pulsetrack.app");

        assertThatThrownBy(() -> users.register(form))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("the seeded accounts load as Spring Security principals with the right authorities")
    void loadsSeededAdmin() {
        User admin = (User) users.loadUserByUsername("admin");

        assertThat(admin.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMIN", "ROLE_COACH", "ROLE_MEMBER");
    }

    @Test
    @DisplayName("the last administrator cannot have their ADMIN role removed")
    void protectsLastAdmin() {
        User admin = users.require("admin");

        assertThatThrownBy(() -> users.replaceRoles(admin.getId(), java.util.EnumSet.of(Role.MEMBER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last administrator");
    }
}

package com.fitnesstracker.workouttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The password encoder lives in its own configuration class on purpose.
 * {@code UserService} needs it, and {@code SecurityConfig} needs
 * {@code UserService}; declaring the bean here keeps that from becoming a
 * circular dependency.
 */
@Configuration
public class PasswordConfig {

    /** Strength 10, matching the hashes in the profile seed scripts. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

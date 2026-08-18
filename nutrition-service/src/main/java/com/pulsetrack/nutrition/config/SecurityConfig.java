package com.pulsetrack.nutrition.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic protection for the whole API, backed by an in-memory user store
 * that is completely independent of PulseTrack's {@code users} table. Two
 * accounts:
 *
 * <ul>
 *   <li><strong>service account</strong> ({@code ROLE_SERVICE}) - the identity
 *       PulseTrack authenticates with. Full CRUD on meal plans.</li>
 *   <li><strong>admin account</strong> ({@code ROLE_ADMIN}) - the same, plus the
 *       actuator endpoints, for operators poking at the service directly.</li>
 * </ul>
 *
 * The API is stateless, so CSRF protection is switched off deliberately: there
 * is no browser session or cookie for an attacker to ride.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(NutritionProperties properties, PasswordEncoder encoder) {
        NutritionProperties.Account service = properties.getSecurity().getServiceAccount();
        NutritionProperties.Account admin = properties.getSecurity().getAdminAccount();

        UserDetails serviceUser = User.withUsername(service.getUsername())
                .password(encoder.encode(service.getPassword()))
                .roles("SERVICE")
                .build();

        UserDetails adminUser = User.withUsername(admin.getUsername())
                .password(encoder.encode(admin.getPassword()))
                .roles("SERVICE", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(serviceUser, adminUser);
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**", "/actuator/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("SERVICE", "ADMIN"))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

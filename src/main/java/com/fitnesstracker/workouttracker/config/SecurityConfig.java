package com.fitnesstracker.workouttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * The application's security model.
 *
 * <p>Public surface: the marketing pages, the read only workout and program
 * listings, registration and login. Everything that creates, edits or deletes
 * data requires an authenticated session, and three roles divide the rest:
 *
 * <ul>
 *   <li>{@code MEMBER} - log and manage their own sessions, enrol in programs,
 *       request a nutrition plan.</li>
 *   <li>{@code COACH} - all of the above, plus authoring programs and reviewing
 *       the full roster of logged sessions.</li>
 *   <li>{@code ADMIN} - the {@code /admin} console: users, roles, every workout
 *       and the remote nutrition service.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Authenticates against the {@code users} table through
     * {@link com.fitnesstracker.workouttracker.service.UserService}, comparing the
     * submitted password with the stored BCrypt hash. Both collaborators arrive as
     * method parameters so this class never has to hold a field reference to the
     * service layer.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ---- static assets and infrastructure ----
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/error", "/actuator/health", "/actuator/info").permitAll()

                // ---- public informational pages ----
                .requestMatchers("/", "/about", "/guide", "/login", "/register", "/access-denied").permitAll()

                // ---- read only browsing is open, writing is not ----
                .requestMatchers(HttpMethod.GET, "/workouts", "/workouts/{id:[0-9]+}").permitAll()
                .requestMatchers(HttpMethod.GET, "/programs", "/programs/{id:[0-9]+}").permitAll()

                // ---- administrator only ----
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ---- coach and administrator only: authoring programs ----
                .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/programs/new", "/programs/*/edit").hasAnyRole("COACH", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/programs").hasAnyRole("COACH", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/programs/{id:[0-9]+}").hasAnyRole("COACH", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/programs/**").hasAnyRole("COACH", "ADMIN")

                // ---- everything else needs a signed in account ----
                .anyRequest().authenticated())

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", false)
                .failureUrl("/login?error")
                .permitAll())

            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())

            .exceptionHandling(handling -> handling
                .accessDeniedPage("/access-denied"))

            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                .maximumSessions(2))

            .authenticationProvider(authenticationProvider);

        return http.build();
    }
}

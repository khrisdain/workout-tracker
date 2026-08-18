package com.pulsetrack.nutrition;

import com.pulsetrack.nutrition.config.NutritionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * PulseTrack Nutrition Service.
 *
 * <p>A standalone Spring Boot application with its own database, its own users
 * and its own domain logic. It knows nothing about PulseTrack's schema; the two
 * communicate only over the REST API in
 * {@link com.pulsetrack.nutrition.controller.MealPlanController}, secured with
 * HTTP Basic.
 *
 * <p>Profiles: {@code dev} runs on in-memory H2, {@code qa} on the PostgreSQL
 * container from {@code docker-compose.yml}.
 */
@SpringBootApplication
@EnableConfigurationProperties(NutritionProperties.class)
public class NutritionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionServiceApplication.class, args);
    }
}

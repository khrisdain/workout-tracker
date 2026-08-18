package com.pulsetrack.nutrition;

import com.pulsetrack.nutrition.config.NutritionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NutritionServiceApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private NutritionProperties properties;

    @Test
    @DisplayName("the service starts on the dev profile with H2")
    void contextLoads() {
        assertThat(environment.getActiveProfiles()).contains("dev");
        assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
    }

    @Test
    @DisplayName("the Basic Auth accounts are bound from YAML, not hard coded")
    void credentialsComeFromConfiguration() {
        assertThat(properties.getSecurity().getServiceAccount().getUsername()).isEqualTo("pulsetrack-app");
        assertThat(properties.getSecurity().getAdminAccount().getUsername()).isEqualTo("nutrition-admin");
    }
}

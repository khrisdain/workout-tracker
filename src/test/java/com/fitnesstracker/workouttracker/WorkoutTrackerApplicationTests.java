package com.fitnesstracker.workouttracker;

import com.fitnesstracker.workouttracker.config.PulseTrackProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkoutTrackerApplicationTests {

    @Autowired
    private PulseTrackProperties properties;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("the context starts on the default dev profile")
    void contextLoads() {
        assertThat(environment.getActiveProfiles()).contains("dev");
    }

    @Test
    @DisplayName("pulsetrack.* YAML settings bind to PulseTrackProperties")
    void propertiesBindFromYaml() {
        assertThat(properties.getBranding().getName()).isEqualTo("PulseTrack");
        assertThat(properties.getWorkout().getWeeklyMinutesGoal()).isEqualTo(150);
        assertThat(properties.getNutritionService().getBaseUrl()).isEqualTo("http://localhost:8081");
    }
}

package com.fitnesstracker.workouttracker;

import com.fitnesstracker.workouttracker.config.PulseTrackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * PulseTrack - the primary web application.
 *
 * <p>Runs under one of two profiles selected entirely from YAML:
 * {@code dev} (H2 in memory) or {@code prod} (MySQL). See
 * {@code src/main/resources/application*.yml} and the README.
 */
@SpringBootApplication
@EnableConfigurationProperties(PulseTrackProperties.class)
public class WorkoutTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkoutTrackerApplication.class, args);
    }
}

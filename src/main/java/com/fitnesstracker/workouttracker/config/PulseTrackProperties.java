package com.fitnesstracker.workouttracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type safe binding for the {@code pulsetrack.*} block of the YAML files. Every
 * value that differs between environments is reached through this class rather
 * than a hard coded constant, which is what lets the profiles swap behaviour
 * without a source change.
 */
@ConfigurationProperties(prefix = "pulsetrack")
public class PulseTrackProperties {

    private final Branding branding = new Branding();
    private final Workout workout = new Workout();
    private final NutritionService nutritionService = new NutritionService();

    public Branding getBranding() {
        return branding;
    }

    public Workout getWorkout() {
        return workout;
    }

    public NutritionService getNutritionService() {
        return nutritionService;
    }

    public static class Branding {
        private String name = "PulseTrack";
        private String tagline = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTagline() {
            return tagline;
        }

        public void setTagline(String tagline) {
            this.tagline = tagline;
        }
    }

    public static class Workout {
        private int maxDurationMinutes = 300;
        private int weeklyMinutesGoal = 150;

        public int getMaxDurationMinutes() {
            return maxDurationMinutes;
        }

        public void setMaxDurationMinutes(int maxDurationMinutes) {
            this.maxDurationMinutes = maxDurationMinutes;
        }

        public int getWeeklyMinutesGoal() {
            return weeklyMinutesGoal;
        }

        public void setWeeklyMinutesGoal(int weeklyMinutesGoal) {
            this.weeklyMinutesGoal = weeklyMinutesGoal;
        }
    }

    public static class NutritionService {
        private boolean enabled = true;
        private String baseUrl = "http://localhost:8081";
        private String username = "pulsetrack-app";
        private String password = "app-secret";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 4000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}

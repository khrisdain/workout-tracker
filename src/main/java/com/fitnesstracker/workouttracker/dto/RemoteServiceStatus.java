package com.fitnesstracker.workouttracker.dto;

/**
 * Whether the nutrition microservice answered, and what to tell the user if it
 * did not. Every page that consumes remote data renders one of these instead of
 * failing, which is the graceful degradation the deliverable asks for.
 */
public record RemoteServiceStatus(boolean available, String baseUrl, String message) {

    public static RemoteServiceStatus up(String baseUrl) {
        return new RemoteServiceStatus(true, baseUrl, "Connected");
    }

    public static RemoteServiceStatus down(String baseUrl, String message) {
        return new RemoteServiceStatus(false, baseUrl, message);
    }

    public static RemoteServiceStatus disabled(String baseUrl) {
        return new RemoteServiceStatus(false, baseUrl,
                "The nutrition service is switched off for this environment (pulsetrack.nutrition-service.enabled=false).");
    }
}

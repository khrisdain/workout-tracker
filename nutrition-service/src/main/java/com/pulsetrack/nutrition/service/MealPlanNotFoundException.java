package com.pulsetrack.nutrition.service;

/** Mapped to HTTP 404 by the API exception handler. */
public class MealPlanNotFoundException extends RuntimeException {

    public MealPlanNotFoundException(Long id) {
        super("No meal plan exists with id " + id);
    }
}

package com.fitnesstracker.workouttracker.exception;

/** Thrown when a URL references an id that is not in the database. Mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " " + id + " does not exist");
    }
}

package com.fitnesstracker.workouttracker.dto;

import java.util.List;
import java.util.Optional;

/**
 * The outcome of a call to the nutrition microservice: the payload when the call
 * succeeded, and always a {@link RemoteServiceStatus} the view can render.
 */
public record RemoteResult<T>(T data, RemoteServiceStatus status) {

    public static <T> RemoteResult<T> ok(T data, String baseUrl) {
        return new RemoteResult<>(data, RemoteServiceStatus.up(baseUrl));
    }

    public static <T> RemoteResult<T> failed(RemoteServiceStatus status) {
        return new RemoteResult<>(null, status);
    }

    public boolean isAvailable() {
        return status.available();
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(data);
    }

    /** Convenience for templates: an empty list instead of null when the call failed. */
    @SuppressWarnings("unchecked")
    public List<?> dataOrEmptyList() {
        return data instanceof List<?> list ? list : List.of();
    }
}

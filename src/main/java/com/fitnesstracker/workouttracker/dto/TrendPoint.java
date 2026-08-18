package com.fitnesstracker.workouttracker.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** A single day on the training volume trend line. */
public record TrendPoint(LocalDate date, long minutes) {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM d");

    public String label() {
        return date.format(LABEL);
    }
}

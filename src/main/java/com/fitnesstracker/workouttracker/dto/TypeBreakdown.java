package com.fitnesstracker.workouttracker.dto;

import com.fitnesstracker.workouttracker.model.WorkoutType;

/** One slice of the "where the training time goes" chart. */
public record TypeBreakdown(WorkoutType type, long sessions, long minutes, long calories, int sharePercent) {
}

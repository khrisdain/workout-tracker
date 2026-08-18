package com.fitnesstracker.workouttracker.dto;

import java.util.List;

/**
 * Everything the member dashboard renders, assembled once in the service layer
 * so the template stays free of query logic.
 */
public record DashboardSummary(
        long sessions,
        long minutes,
        long calories,
        long minutesThisWeek,
        int weeklyGoalMinutes,
        long enrolledPrograms,
        List<TypeBreakdown> byType,
        List<TrendPoint> trend) {

    public int weeklyGoalPercent() {
        if (weeklyGoalMinutes <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(minutesThisWeek * 100.0 / weeklyGoalMinutes));
    }

    public long hours() {
        return minutes / 60;
    }

    public int averageSessionMinutes() {
        return sessions == 0 ? 0 : (int) Math.round((double) minutes / sessions);
    }

    // ---- chart payloads -------------------------------------------------
    // Rendered straight into data- attributes so pulsetrack.js can read them
    // without an extra request. Labels come from enum constants and formatted
    // dates, so they are safe to quote directly.

    public String trendLabelsJson() {
        return trend.stream()
                .map(point -> "\"" + point.label() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    public String trendValuesJson() {
        return trend.stream()
                .map(point -> String.valueOf(point.minutes()))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    public String typeLabelsJson() {
        return byType.stream()
                .map(row -> "\"" + row.type().getLabel() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    public String typeSessionsJson() {
        return byType.stream()
                .map(row -> String.valueOf(row.sessions()))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}

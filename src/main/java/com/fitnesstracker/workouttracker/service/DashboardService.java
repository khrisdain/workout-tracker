package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.config.PulseTrackProperties;
import com.fitnesstracker.workouttracker.dto.DashboardSummary;
import com.fitnesstracker.workouttracker.dto.TrendPoint;
import com.fitnesstracker.workouttracker.dto.TypeBreakdown;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import com.fitnesstracker.workouttracker.repository.ProgramRepository;
import com.fitnesstracker.workouttracker.repository.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the grouped repository queries into the shape the dashboards render.
 * Every figure comes from a database aggregate, not from streaming entities
 * into memory.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TREND_DAYS = 30;

    private final WorkoutRepository workouts;
    private final ProgramRepository programs;
    private final PulseTrackProperties properties;

    public DashboardService(WorkoutRepository workouts, ProgramRepository programs,
                            PulseTrackProperties properties) {
        this.workouts = workouts;
        this.programs = programs;
        this.properties = properties;
    }

    /** Personal dashboard for one member. */
    public DashboardSummary forMember(String username) {
        long sessions = workouts.countByOwnerUsername(username);
        long minutes = workouts.totalMinutesForUser(username);
        long calories = workouts.totalCaloriesForUser(username);
        long weekMinutes = workouts.minutesForUserSince(username, startOfWeek());
        List<TypeBreakdown> byType = toBreakdown(workouts.aggregateByTypeForUser(username));
        List<TrendPoint> trend = toTrend(workouts.dailyMinutesForUserSince(username, trendStart()));

        return new DashboardSummary(sessions, minutes, calories, weekMinutes,
                properties.getWorkout().getWeeklyMinutesGoal(),
                programs.countEnrolled(username), byType, trend);
    }

    /** Whole-gym view used by the home page and the admin console. */
    public DashboardSummary global() {
        long sessions = workouts.count();
        long minutes = workouts.totalMinutes();
        long calories = workouts.totalCalories();
        List<TypeBreakdown> byType = toBreakdown(workouts.aggregateByType());
        List<TrendPoint> trend = toTrend(workouts.dailyMinutesSince(trendStart()));

        return new DashboardSummary(sessions, minutes, calories, 0,
                properties.getWorkout().getWeeklyMinutesGoal(),
                programs.count(), byType, trend);
    }

    private static LocalDate startOfWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private static LocalDate trendStart() {
        return LocalDate.now().minusDays(TREND_DAYS - 1L);
    }

    private List<TypeBreakdown> toBreakdown(List<Object[]> rows) {
        long totalSessions = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<TypeBreakdown> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            long sessions = ((Number) row[1]).longValue();
            int share = totalSessions == 0 ? 0 : (int) Math.round(sessions * 100.0 / totalSessions);
            result.add(new TypeBreakdown((WorkoutType) row[0], sessions,
                    ((Number) row[2]).longValue(), ((Number) row[3]).longValue(), share));
        }
        return result;
    }

    /** Fills the gaps so the trend line has one point per day, including rest days. */
    private List<TrendPoint> toTrend(List<Object[]> rows) {
        Map<LocalDate, Long> byDate = new LinkedHashMap<>();
        for (Object[] row : rows) {
            byDate.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }
        List<TrendPoint> points = new ArrayList<>(TREND_DAYS);
        LocalDate cursor = trendStart();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            points.add(new TrendPoint(cursor, byDate.getOrDefault(cursor, 0L)));
            cursor = cursor.plusDays(1);
        }
        return points;
    }
}

package com.fitnesstracker.workouttracker.repository;

import com.fitnesstracker.workouttracker.model.DifficultyLevel;
import com.fitnesstracker.workouttracker.model.Workout;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Composable JPA criteria fragments. Each returns {@code null} when its input is
 * absent so callers can chain them unconditionally with
 * {@code Specification.where(a).and(b)}.
 */
public final class WorkoutSpecifications {

    private WorkoutSpecifications() {
    }

    public static Specification<Workout> hasType(WorkoutType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Workout> hasDifficulty(DifficultyLevel difficulty) {
        return difficulty == null ? null : (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Workout> nameContains(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern));
    }

    public static Specification<Workout> loggedFrom(LocalDate from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateLogged"), from);
    }

    public static Specification<Workout> loggedUntil(LocalDate to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateLogged"), to);
    }

    public static Specification<Workout> minDuration(Integer minutes) {
        return minutes == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("durationMinutes"), minutes);
    }

    public static Specification<Workout> ownedBy(String username) {
        return username == null ? null : (root, query, cb) -> cb.equal(root.get("owner").get("username"), username);
    }
}

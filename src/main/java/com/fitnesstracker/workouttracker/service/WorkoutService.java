package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.dto.WorkoutFilter;
import com.fitnesstracker.workouttracker.exception.ResourceNotFoundException;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.model.Workout;
import com.fitnesstracker.workouttracker.repository.WorkoutRepository;
import com.fitnesstracker.workouttracker.repository.WorkoutSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All workout reads and writes go through here. Filtering, sorting and paging
 * are pushed into the database via {@link Specification} plus {@link Pageable} -
 * no list is ever loaded into memory and trimmed afterwards.
 */
@Service
@Transactional(readOnly = true)
public class WorkoutService {

    private final WorkoutRepository workouts;

    public WorkoutService(WorkoutRepository workouts) {
        this.workouts = workouts;
    }

    /**
     * @param filter        the toolbar selections
     * @param restrictOwner username to scope results to, or {@code null} for everyone
     */
    public Page<Workout> search(WorkoutFilter filter, String restrictOwner) {
        Specification<Workout> spec = Specification
                .where(WorkoutSpecifications.nameContains(filter.getQ()))
                .and(WorkoutSpecifications.hasType(filter.getType()))
                .and(WorkoutSpecifications.hasDifficulty(filter.getDifficulty()))
                .and(WorkoutSpecifications.loggedFrom(filter.getFrom()))
                .and(WorkoutSpecifications.loggedUntil(filter.getTo()))
                .and(WorkoutSpecifications.minDuration(filter.getMinDuration()))
                .and(WorkoutSpecifications.ownedBy(restrictOwner));

        return workouts.findAll(spec, toPageable(filter));
    }

    private Pageable toPageable(WorkoutFilter filter) {
        Sort.Direction direction = filter.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Secondary sort on id keeps paging stable when the primary key ties.
        Sort sort = Sort.by(direction, filter.getSafeSort()).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(filter.getPage(), filter.getSize(), sort);
    }

    public Workout get(Long id) {
        return workouts.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Workout", id));
    }

    public List<Workout> recentFor(String username) {
        return username == null
                ? workouts.findTop5ByOrderByDateLoggedDescIdDesc()
                : workouts.findTop5ByOwnerUsernameOrderByDateLoggedDescIdDesc(username);
    }

    @Transactional
    public Workout create(Workout workout, User owner) {
        workout.setId(null);
        workout.setOwner(owner);
        applyCalorieEstimate(workout, owner);
        return workouts.save(workout);
    }

    @Transactional
    public Workout update(Long id, Workout edited) {
        Workout existing = get(id);
        existing.setName(edited.getName());
        existing.setType(edited.getType());
        existing.setDurationMinutes(edited.getDurationMinutes());
        existing.setDifficulty(edited.getDifficulty());
        existing.setCaloriesBurned(edited.getCaloriesBurned());
        existing.setDateLogged(edited.getDateLogged());
        existing.setNotes(edited.getNotes());
        applyCalorieEstimate(existing, existing.getOwner());
        return workouts.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Workout workout = get(id);
        workouts.delete(workout);
    }

    /**
     * A member may only touch their own sessions; coaches and administrators may
     * touch any of them. Called by the controllers before an edit or delete.
     */
    public boolean canModify(Workout workout, User actor) {
        if (actor == null) {
            return false;
        }
        if (actor.hasRole(Role.ADMIN) || actor.hasRole(Role.COACH)) {
            return true;
        }
        return workout.getOwner() != null && workout.getOwner().getId().equals(actor.getId());
    }

    /**
     * Fills in a calorie figure when the member left the field blank, using the
     * MET formula: kcal = MET x weight(kg) x hours, scaled by how hard the
     * session felt. Falls back to a 70 kg reference when we have no bodyweight.
     */
    void applyCalorieEstimate(Workout workout, User owner) {
        if (workout.getCaloriesBurned() != null || workout.getDurationMinutes() == null
                || workout.getType() == null || workout.getDifficulty() == null) {
            return;
        }
        double weightKg = (owner != null && owner.getWeightKg() != null) ? owner.getWeightKg() : 70.0;
        double hours = workout.getDurationMinutes() / 60.0;
        double kcal = workout.getType().getMetValue() * weightKg * hours
                * workout.getDifficulty().getIntensityFactor();
        workout.setCaloriesBurned((int) Math.round(kcal));
    }

    public long count() {
        return workouts.count();
    }
}
